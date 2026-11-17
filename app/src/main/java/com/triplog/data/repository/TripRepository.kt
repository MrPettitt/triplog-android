package com.triplog.data.repository

import com.triplog.data.local.dao.TripDao
import com.triplog.data.local.entity.TripEntity
import com.triplog.data.remote.ApiService
import com.triplog.data.remote.dto.CreateTripRequest
import com.triplog.domain.model.Trip
import com.triplog.sync.SyncOutcome
import com.triplog.sync.SyncResult
import com.triplog.sync.SyncScheduler
import com.triplog.sync.outcomeForDeleteCode
import com.triplog.sync.outcomeForHttpCode
import com.triplog.sync.toResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val apiService: ApiService,
    private val syncScheduler: SyncScheduler
) {
    private val syncMutex = Mutex()

    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips().map { entities ->
        entities.map { it.toDomain() }
    }

    val pendingSyncCount: Flow<Int> = tripDao.getPendingSyncCount()
    val totalDistanceKm: Flow<Double?> = tripDao.getTotalDistanceKm()
    val totalTripCount: Flow<Int> = tripDao.getTotalTripCount()

    suspend fun getTrip(id: Long): Trip? = tripDao.getTripById(id)?.toDomain()

    suspend fun saveTrip(trip: Trip): Long {
        val id = tripDao.insertTrip(TripEntity.fromDomain(trip))
        syncScheduler.requestSync()
        return id
    }

    /** Edits a trip. If it was already synced it goes back to Pending until the change is uploaded. */
    suspend fun updateTrip(trip: Trip) {
        tripDao.updateDetails(
            id = trip.id,
            fromLocation = trip.fromLocation,
            toLocation = trip.toLocation,
            dateTime = trip.dateTime,
            distanceKm = trip.distanceKm,
            purpose = trip.purpose.name,
            notes = trip.notes
        )
        syncScheduler.requestSync()
    }

    /** Hides the trip at once; the server copy (if any) is deleted on the next sync. */
    suspend fun deleteTrip(id: Long) {
        tripDao.markDeleted(id)
        syncScheduler.requestSync()
    }

    /** Pushes every pending create, edit and delete. Only one pass runs at a time. */
    suspend fun syncPendingTrips(): SyncResult = syncMutex.withLock {
        var result = SyncResult()
        for (entity in tripDao.getUnsyncedTrips()) {
            result += syncTrip(entity).toResult()
        }
        result
    }

    private suspend fun syncTrip(entity: TripEntity): SyncOutcome {
        return try {
            val remoteId = entity.remoteId
            when {
                entity.isDeleted -> deleteRemote(entity)
                remoteId == null -> createRemote(entity)
                else -> updateRemote(entity, remoteId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            SyncOutcome.Retryable
        } catch (e: Exception) {
            // Unreadable response body and similar: the record stays pending for the next pass.
            SyncOutcome.Rejected
        }
    }

    private suspend fun createRemote(entity: TripEntity): SyncOutcome {
        val response = apiService.createTrip(entity.clientId, entity.toRequest())
        val outcome = outcomeForHttpCode(response.code())
        if (outcome != SyncOutcome.Success) return outcome

        val remoteId = response.body()?.data?.id ?: return SyncOutcome.Rejected
        // Always remember the server id, even if the user edited the trip meanwhile.
        tripDao.setRemoteId(entity.id, remoteId)
        tripDao.markAsSynced(entity.id, remoteId, entity.revision)
        return SyncOutcome.Success
    }

    private suspend fun updateRemote(entity: TripEntity, remoteId: String): SyncOutcome {
        val response = apiService.updateTrip(remoteId, entity.toRequest())
        val outcome = outcomeForHttpCode(response.code())
        if (outcome != SyncOutcome.Success) return outcome

        tripDao.markAsSynced(entity.id, remoteId, entity.revision)
        return SyncOutcome.Success
    }

    private suspend fun deleteRemote(entity: TripEntity): SyncOutcome {
        val remoteId = entity.remoteId
        if (remoteId == null) {
            // Never reached the server, so there is nothing to delete there.
            tripDao.deleteById(entity.id)
            return SyncOutcome.Success
        }
        val outcome = outcomeForDeleteCode(apiService.deleteTrip(remoteId).code())
        if (outcome == SyncOutcome.Success) tripDao.deleteById(entity.id)
        return outcome
    }

    private fun TripEntity.toRequest() = CreateTripRequest(
        clientId = clientId,
        fromLocation = fromLocation,
        toLocation = toLocation,
        dateTime = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        distanceKm = distanceKm,
        purpose = purpose,
        notes = notes
    )
}

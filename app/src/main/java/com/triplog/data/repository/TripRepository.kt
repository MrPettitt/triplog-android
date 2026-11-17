package com.triplog.data.repository

import com.triplog.data.local.dao.TripDao
import com.triplog.data.local.entity.TripEntity
import com.triplog.data.remote.ApiService
import com.triplog.data.remote.dto.CreateTripRequest
import com.triplog.domain.model.Trip
import com.triplog.sync.SyncOutcome
import com.triplog.sync.SyncResult
import com.triplog.sync.SyncScheduler
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

    suspend fun saveTrip(trip: Trip): Long {
        val id = tripDao.insertTrip(TripEntity.fromDomain(trip))
        syncScheduler.requestSync()
        return id
    }

    /** Uploads every unsynced trip. Only one pass runs at a time. */
    suspend fun syncPendingTrips(): SyncResult = syncMutex.withLock {
        var result = SyncResult()
        for (entity in tripDao.getUnsyncedTrips()) {
            result += syncTrip(entity).toResult()
        }
        result
    }

    private suspend fun syncTrip(entity: TripEntity): SyncOutcome = try {
        val request = CreateTripRequest(
            clientId = entity.clientId,
            fromLocation = entity.fromLocation,
            toLocation = entity.toLocation,
            dateTime = entity.dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            distanceKm = entity.distanceKm,
            purpose = entity.purpose,
            notes = entity.notes
        )
        val response = apiService.createTrip(entity.clientId, request)
        val outcome = outcomeForHttpCode(response.code())
        if (outcome != SyncOutcome.Success) {
            outcome
        } else {
            val remoteId = response.body()?.data?.id
            if (remoteId == null) {
                SyncOutcome.Rejected
            } else {
                tripDao.markAsSynced(entity.id, remoteId)
                SyncOutcome.Success
            }
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

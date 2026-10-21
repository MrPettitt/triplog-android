package com.triplog.data.repository

import com.triplog.data.local.dao.TripDao
import com.triplog.data.local.entity.TripEntity
import com.triplog.data.remote.ApiService
import com.triplog.data.remote.dto.CreateTripRequest
import com.triplog.domain.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val apiService: ApiService
) {
    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips().map { entities ->
        entities.map { it.toDomain() }
    }

    val pendingSyncCount: Flow<Int> = tripDao.getPendingSyncCount()
    val totalDistanceKm: Flow<Double?> = tripDao.getTotalDistanceKm()
    val totalTripCount: Flow<Int> = tripDao.getTotalTripCount()

    suspend fun saveTrip(trip: Trip): Long {
        val entity = TripEntity.fromDomain(trip)
        return tripDao.insertTrip(entity)
    }

    suspend fun syncPendingTrips() {
        val unsynced = tripDao.getUnsyncedTrips()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        for (entity in unsynced) {
            try {
                val request = CreateTripRequest(
                    fromLocation = entity.fromLocation,
                    toLocation = entity.toLocation,
                    dateTime = entity.dateTime.format(formatter),
                    distanceKm = entity.distanceKm,
                    purpose = entity.purpose,
                    notes = entity.notes
                )
                val response = apiService.createTrip(request)
                if (response.isSuccessful) {
                    val remoteId = response.body()?.data?.id ?: continue
                    tripDao.markAsSynced(entity.id, remoteId)
                }
            } catch (e: Exception) {
                // Network error - will retry on next sync
            }
        }
    }
}

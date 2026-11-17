package com.triplog.data.repository

import com.triplog.data.local.dao.TripDao
import com.triplog.data.local.entity.TripEntity
import com.triplog.domain.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao
) {
    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips().map { entities ->
        entities.map { it.toDomain() }
    }

    val totalDistanceKm: Flow<Double?> = tripDao.getTotalDistanceKm()
    val totalTripCount: Flow<Int> = tripDao.getTotalTripCount()

    suspend fun getTrip(id: Long): Trip? = tripDao.getTripById(id)?.toDomain()

    suspend fun saveTrip(trip: Trip): Long = tripDao.insertTrip(TripEntity.fromDomain(trip))

    /** Replaces the stored trip with the same id. Does nothing if it no longer exists. */
    suspend fun updateTrip(trip: Trip) {
        val existing = tripDao.getTripById(trip.id) ?: return
        tripDao.updateTrip(TripEntity.fromDomain(trip).copy(createdAt = existing.createdAt))
    }

    suspend fun deleteTrip(id: Long) = tripDao.deleteById(id)
}

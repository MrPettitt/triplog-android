package com.triplog.data.local.dao

import androidx.room.*
import com.triplog.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY dateTime DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedTrips(): List<TripEntity>

    @Query("SELECT COUNT(*) FROM trips WHERE isSynced = 0")
    fun getPendingSyncCount(): Flow<Int>

    @Query("SELECT SUM(distanceKm) FROM trips")
    fun getTotalDistanceKm(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM trips")
    fun getTotalTripCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("UPDATE trips SET isSynced = 1, remoteId = :remoteId WHERE id = :localId")
    suspend fun markAsSynced(localId: Long, remoteId: String)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)
}

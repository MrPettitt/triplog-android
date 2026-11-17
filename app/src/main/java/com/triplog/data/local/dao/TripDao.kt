package com.triplog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.triplog.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TripDao {

    @Query("SELECT * FROM trips WHERE isDeleted = 0 ORDER BY dateTime DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id AND isDeleted = 0")
    suspend fun getTripById(id: Long): TripEntity?

    /** Everything that still needs to reach the server, including deletions. */
    @Query("SELECT * FROM trips WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedTrips(): List<TripEntity>

    @Query("SELECT COUNT(*) FROM trips WHERE isSynced = 0")
    fun getPendingSyncCount(): Flow<Int>

    @Query("SELECT SUM(distanceKm) FROM trips WHERE isDeleted = 0")
    fun getTotalDistanceKm(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM trips WHERE isDeleted = 0")
    fun getTotalTripCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    /** Targeted update so a concurrent sync that just stored remoteId is not overwritten. */
    @Query(
        "UPDATE trips SET fromLocation = :fromLocation, toLocation = :toLocation, " +
            "dateTime = :dateTime, distanceKm = :distanceKm, purpose = :purpose, notes = :notes, " +
            "isSynced = 0, revision = revision + 1 WHERE id = :id AND isDeleted = 0"
    )
    suspend fun updateDetails(
        id: Long,
        fromLocation: String,
        toLocation: String,
        dateTime: LocalDateTime,
        distanceKm: Double,
        purpose: String,
        notes: String
    )

    @Query("UPDATE trips SET isDeleted = 1, isSynced = 0, revision = revision + 1 WHERE id = :id")
    suspend fun markDeleted(id: Long)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE trips SET remoteId = :remoteId WHERE id = :localId")
    suspend fun setRemoteId(localId: Long, remoteId: String)

    /** Only succeeds if nothing changed locally since [revision] was read. */
    @Query("UPDATE trips SET isSynced = 1, remoteId = :remoteId WHERE id = :localId AND revision = :revision")
    suspend fun markAsSynced(localId: Long, remoteId: String, revision: Int)
}

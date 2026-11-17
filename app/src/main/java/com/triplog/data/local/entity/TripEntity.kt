package com.triplog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import java.time.LocalDateTime

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(defaultValue = "")
    val clientId: String,
    val fromLocation: String,
    val toLocation: String,
    val dateTime: LocalDateTime,
    val distanceKm: Double,
    val purpose: String,
    val notes: String,
    val isSynced: Boolean,
    val remoteId: String?,
    val createdAt: LocalDateTime,
    /** Bumped on every local change so a finished upload never marks newer edits as synced. */
    @ColumnInfo(defaultValue = "0")
    val revision: Int = 0,
    /** Tombstone: hidden from the UI, removed locally once the server has deleted it. */
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false
) {
    fun toDomain(): Trip = Trip(
        id = id,
        clientId = clientId,
        fromLocation = fromLocation,
        toLocation = toLocation,
        dateTime = dateTime,
        distanceKm = distanceKm,
        purpose = TripPurpose.valueOf(purpose),
        notes = notes,
        isSynced = isSynced,
        remoteId = remoteId,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(trip: Trip): TripEntity = TripEntity(
            id = trip.id,
            clientId = trip.clientId,
            fromLocation = trip.fromLocation,
            toLocation = trip.toLocation,
            dateTime = trip.dateTime,
            distanceKm = trip.distanceKm,
            purpose = trip.purpose.name,
            notes = trip.notes,
            isSynced = trip.isSynced,
            remoteId = trip.remoteId,
            createdAt = trip.createdAt
        )
    }
}

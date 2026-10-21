package com.triplog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import java.time.LocalDateTime

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromLocation: String,
    val toLocation: String,
    val dateTime: LocalDateTime,
    val distanceKm: Double,
    val purpose: String,
    val notes: String,
    val isSynced: Boolean,
    val remoteId: String?,
    val createdAt: LocalDateTime
) {
    fun toDomain(): Trip = Trip(
        id = id,
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

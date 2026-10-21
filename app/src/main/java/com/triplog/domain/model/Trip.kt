package com.triplog.domain.model

import java.time.LocalDateTime

data class Trip(
    val id: Long = 0,
    val fromLocation: String,
    val toLocation: String,
    val dateTime: LocalDateTime,
    val distanceKm: Double,
    val purpose: TripPurpose,
    val notes: String = "",
    val isSynced: Boolean = false,
    val remoteId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class TripPurpose(val label: String) {
    CLIENT_VISIT("Client Visit"),
    SITE_INSPECTION("Site Inspection"),
    MEETING("Meeting"),
    DELIVERY("Delivery"),
    OTHER("Other")
}

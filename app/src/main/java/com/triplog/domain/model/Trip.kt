package com.triplog.domain.model

import androidx.annotation.StringRes
import com.triplog.R
import java.time.LocalDateTime

data class Trip(
    val id: Long = 0,
    val fromLocation: String,
    val toLocation: String,
    val dateTime: LocalDateTime,
    val distanceKm: Double,
    val purpose: TripPurpose,
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class TripPurpose(@StringRes val labelRes: Int) {
    CLIENT_VISIT(R.string.purpose_client_visit),
    SITE_INSPECTION(R.string.purpose_site_inspection),
    MEETING(R.string.purpose_meeting),
    DELIVERY(R.string.purpose_delivery),
    OTHER(R.string.purpose_other)
}

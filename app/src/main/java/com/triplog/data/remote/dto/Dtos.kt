package com.triplog.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TripDto(
    @Json(name = "id") val id: String,
    @Json(name = "from_location") val fromLocation: String,
    @Json(name = "to_location") val toLocation: String,
    @Json(name = "date_time") val dateTime: String,
    @Json(name = "distance_km") val distanceKm: Double,
    @Json(name = "purpose") val purpose: String,
    @Json(name = "notes") val notes: String,
    @Json(name = "employee_id") val employeeId: String
)

@JsonClass(generateAdapter = true)
data class CreateTripRequest(
    @Json(name = "from_location") val fromLocation: String,
    @Json(name = "to_location") val toLocation: String,
    @Json(name = "date_time") val dateTime: String,
    @Json(name = "distance_km") val distanceKm: Double,
    @Json(name = "purpose") val purpose: String,
    @Json(name = "notes") val notes: String
)

@JsonClass(generateAdapter = true)
data class FuelExpenseDto(
    @Json(name = "id") val id: String,
    @Json(name = "date") val date: String,
    @Json(name = "litres") val litres: Double,
    @Json(name = "amount_rands") val amountRands: Double,
    @Json(name = "odometer_km") val odometerKm: Int,
    @Json(name = "fuel_station") val fuelStation: String,
    @Json(name = "receipt_url") val receiptUrl: String?
)

@JsonClass(generateAdapter = true)
data class CreateExpenseRequest(
    @Json(name = "date") val date: String,
    @Json(name = "litres") val litres: Double,
    @Json(name = "amount_rands") val amountRands: Double,
    @Json(name = "odometer_km") val odometerKm: Int,
    @Json(name = "fuel_station") val fuelStation: String
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: T?,
    @Json(name = "message") val message: String?
)

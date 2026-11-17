package com.triplog.data.local

import com.triplog.data.local.entity.FuelExpenseEntity
import com.triplog.data.local.entity.TripEntity
import com.triplog.domain.model.FuelExpense
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EntityMappingTest {

    @Test
    fun trip_roundTripsThroughEntity() {
        val trip = Trip(
            id = 7,
            fromLocation = "Head Office",
            toLocation = "Client Site",
            dateTime = LocalDateTime.of(2026, 5, 1, 8, 30),
            distanceKm = 42.5,
            purpose = TripPurpose.SITE_INSPECTION,
            notes = "Bring hard hat",
            createdAt = LocalDateTime.of(2026, 5, 1, 8, 31)
        )
        assertEquals(trip, TripEntity.fromDomain(trip).toDomain())
    }

    @Test
    fun trip_purposeIsStoredByName() {
        val trip = Trip(
            fromLocation = "A",
            toLocation = "B",
            dateTime = LocalDateTime.of(2026, 1, 1, 0, 0),
            distanceKm = 1.0,
            purpose = TripPurpose.DELIVERY
        )
        assertEquals("DELIVERY", TripEntity.fromDomain(trip).purpose)
    }

    @Test
    fun fuelExpense_roundTripsThroughEntity() {
        val expense = FuelExpense(
            id = 3,
            date = LocalDate.of(2026, 5, 2),
            litres = 38.2,
            amountRands = 812.4,
            odometerKm = 84320,
            fuelStation = "Engen Sandton",
            receiptImagePath = "/data/receipt.jpg",
            createdAt = LocalDateTime.of(2026, 5, 2, 17, 0)
        )
        assertEquals(expense, FuelExpenseEntity.fromDomain(expense).toDomain())
    }
}

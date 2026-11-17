package com.triplog.util

import com.triplog.domain.model.FuelExpense
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ListFiltersTest {

    private val today = LocalDate.of(2026, 11, 17)

    private fun trip(
        from: String = "Head Office",
        to: String = "Client Site",
        notes: String = "",
        purpose: TripPurpose = TripPurpose.MEETING,
        date: LocalDate = today
    ) = Trip(
        fromLocation = from,
        toLocation = to,
        dateTime = date.atTime(9, 0),
        distanceKm = 10.0,
        purpose = purpose,
        notes = notes
    )

    private fun expense(station: String = "Engen Sandton", date: LocalDate = today) = FuelExpense(
        date = date,
        litres = 10.0,
        amountRands = 200.0,
        odometerKm = 1000,
        fuelStation = station
    )

    // --- date ranges ---

    @Test
    fun allRange_acceptsEverything() {
        assertTrue(DateRange.ALL.contains(LocalDate.of(1999, 1, 1), today))
    }

    @Test
    fun thisMonth_onlyMatchesTheCurrentMonth() {
        assertTrue(DateRange.THIS_MONTH.contains(LocalDate.of(2026, 11, 1), today))
        assertTrue(DateRange.THIS_MONTH.contains(LocalDate.of(2026, 11, 30), today))
        assertFalse(DateRange.THIS_MONTH.contains(LocalDate.of(2026, 10, 31), today))
        assertFalse(DateRange.THIS_MONTH.contains(LocalDate.of(2025, 11, 17), today))
    }

    @Test
    fun last30Days_includesTodayAndTheTwentyNineDaysBeforeIt() {
        assertTrue(DateRange.LAST_30_DAYS.contains(today, today))
        assertTrue(DateRange.LAST_30_DAYS.contains(today.minusDays(29), today))
        assertFalse(DateRange.LAST_30_DAYS.contains(today.minusDays(30), today))
        assertFalse(DateRange.LAST_30_DAYS.contains(today.plusDays(1), today))
    }

    @Test
    fun thisYear_onlyMatchesTheCurrentYear() {
        assertTrue(DateRange.THIS_YEAR.contains(LocalDate.of(2026, 1, 1), today))
        assertFalse(DateRange.THIS_YEAR.contains(LocalDate.of(2025, 12, 31), today))
    }

    // --- query matching ---

    @Test
    fun query_isCaseInsensitiveAndMatchesAnywhere() {
        assertTrue(matchesQuery("MIDRAND", "Midrand Warehouse"))
        assertTrue(matchesQuery("ware", "Midrand Warehouse"))
        assertFalse(matchesQuery("pretoria", "Midrand Warehouse"))
    }

    @Test
    fun query_requiresEveryWordButInAnyField() {
        assertTrue(matchesQuery("rose  midrand", "Rosebank", "Midrand Warehouse"))
        assertFalse(matchesQuery("rose sandton", "Rosebank", "Midrand Warehouse"))
    }

    @Test
    fun blankQuery_matchesEverything() {
        assertTrue(matchesQuery("", "anything"))
        assertTrue(matchesQuery("   ", "anything"))
    }

    // --- trips ---

    @Test
    fun tripFilter_withNothingSet_returnsEveryTripAndIsInactive() {
        val trips = listOf(trip(), trip(from = "Depot"))

        assertEquals(trips, TripFilter().apply(trips, today))
        assertFalse(TripFilter().isActive)
    }

    @Test
    fun tripFilter_searchesStartEndAndNotes() {
        val trips = listOf(
            trip(from = "Rosebank", to = "Depot"),
            trip(from = "Sandton", to = "Warehouse"),
            trip(from = "A", to = "B", notes = "collect parcels at the warehouse")
        )

        val found = TripFilter(query = "warehouse").apply(trips, today)

        assertEquals(listOf("Sandton", "A"), found.map { it.fromLocation })
    }

    @Test
    fun tripFilter_byPurpose() {
        val trips = listOf(
            trip(from = "one", purpose = TripPurpose.DELIVERY),
            trip(from = "two", purpose = TripPurpose.MEETING)
        )

        assertEquals(listOf("one"), TripFilter(purpose = TripPurpose.DELIVERY).apply(trips, today).map { it.fromLocation })
    }

    @Test
    fun tripFilter_combinesAllConditions() {
        val trips = listOf(
            trip(from = "match", purpose = TripPurpose.DELIVERY, date = today.minusDays(3)),
            trip(from = "match", purpose = TripPurpose.MEETING, date = today.minusDays(3)),
            trip(from = "match", purpose = TripPurpose.DELIVERY, date = today.minusDays(60)),
            trip(from = "other", purpose = TripPurpose.DELIVERY, date = today.minusDays(3))
        )
        val filter = TripFilter(query = "match", purpose = TripPurpose.DELIVERY, range = DateRange.LAST_30_DAYS)

        val found = filter.apply(trips, today)

        assertEquals(1, found.size)
        assertTrue(filter.isActive)
    }

    @Test
    fun tripFilter_usesTheTripDateNotTheTime() {
        val lateNight = trip().copy(dateTime = LocalDateTime.of(2026, 11, 17, 23, 59))

        assertEquals(1, TripFilter(range = DateRange.LAST_30_DAYS).apply(listOf(lateNight), today).size)
    }

    // --- expenses ---

    @Test
    fun expenseFilter_searchesTheStation() {
        val expenses = listOf(expense("Engen Sandton"), expense("Shell Rosebank"))

        assertEquals(listOf("Shell Rosebank"), ExpenseFilter(query = "shell").apply(expenses, today).map { it.fuelStation })
    }

    @Test
    fun expenseFilter_byRange() {
        val expenses = listOf(
            expense("recent", today.minusDays(5)),
            expense("old", today.minusDays(400))
        )

        assertEquals(listOf("recent"), ExpenseFilter(range = DateRange.THIS_YEAR).apply(expenses, today).map { it.fuelStation })
        assertTrue(ExpenseFilter(range = DateRange.THIS_YEAR).isActive)
        assertFalse(ExpenseFilter().isActive)
    }
}

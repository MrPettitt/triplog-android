package com.triplog.util

import com.triplog.domain.model.FuelExpense
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class MonthlySummaryTest {

    private fun trip(at: LocalDateTime, km: Double) = Trip(
        fromLocation = "A",
        toLocation = "B",
        dateTime = at,
        distanceKm = km,
        purpose = TripPurpose.MEETING
    )

    private fun expense(date: LocalDate, litres: Double, amount: Double) = FuelExpense(
        date = date,
        litres = litres,
        amountRands = amount,
        odometerKm = 1000,
        fuelStation = "Engen"
    )

    @Test
    fun noData_givesNoMonths() {
        assertTrue(summarizeByMonth(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun totalsAreGroupedPerCalendarMonth() {
        val trips = listOf(
            trip(LocalDateTime.of(2026, 11, 3, 8, 0), 10.5),
            trip(LocalDateTime.of(2026, 11, 20, 17, 0), 20.0),
            trip(LocalDateTime.of(2026, 10, 30, 9, 0), 5.0)
        )
        val expenses = listOf(
            expense(LocalDate.of(2026, 11, 5), 40.0, 900.0),
            expense(LocalDate.of(2026, 11, 25), 30.5, 700.25)
        )

        val months = summarizeByMonth(trips, expenses)

        assertEquals(2, months.size)
        val november = months[0]
        assertEquals(YearMonth.of(2026, 11), november.month)
        assertEquals(2, november.tripCount)
        assertEquals(30.5, november.distanceKm, 0.0)
        assertEquals(2, november.fillUpCount)
        assertEquals(70.5, november.litres, 0.0)
        assertEquals(1600.25, november.fuelSpendRands, 0.0)

        val october = months[1]
        assertEquals(YearMonth.of(2026, 10), october.month)
        assertEquals(1, october.tripCount)
        assertEquals(5.0, october.distanceKm, 0.0)
        assertEquals(0, october.fillUpCount)
        assertEquals(0.0, october.fuelSpendRands, 0.0)
    }

    @Test
    fun monthsAreNewestFirstAcrossYears() {
        val trips = listOf(
            trip(LocalDateTime.of(2025, 12, 31, 23, 59), 1.0),
            trip(LocalDateTime.of(2026, 1, 1, 0, 0), 1.0),
            trip(LocalDateTime.of(2025, 3, 15, 12, 0), 1.0)
        )

        assertEquals(
            listOf(YearMonth.of(2026, 1), YearMonth.of(2025, 12), YearMonth.of(2025, 3)),
            summarizeByMonth(trips, emptyList()).map { it.month }
        )
    }

    @Test
    fun aMonthWithOnlyFuelStillAppears_andViceVersa() {
        val months = summarizeByMonth(
            trips = listOf(trip(LocalDateTime.of(2026, 5, 2, 8, 0), 12.0)),
            expenses = listOf(expense(LocalDate.of(2026, 6, 2), 10.0, 200.0))
        )

        assertEquals(listOf(YearMonth.of(2026, 6), YearMonth.of(2026, 5)), months.map { it.month })
        assertEquals(0, months[0].tripCount)
        assertEquals(1, months[0].fillUpCount)
        assertEquals(1, months[1].tripCount)
        assertEquals(0, months[1].fillUpCount)
    }

    @Test
    fun tripsOnTheLastAndFirstInstantOfAMonthLandInTheRightMonth() {
        val months = summarizeByMonth(
            listOf(
                trip(LocalDateTime.of(2026, 2, 28, 23, 59, 59), 1.0),
                trip(LocalDateTime.of(2026, 3, 1, 0, 0, 0), 2.0)
            ),
            emptyList()
        )

        assertEquals(2.0, months.first { it.month == YearMonth.of(2026, 3) }.distanceKm, 0.0)
        assertEquals(1.0, months.first { it.month == YearMonth.of(2026, 2) }.distanceKm, 0.0)
    }

    @Test
    fun csv_hasOneRowPerMonthWithDotDecimals() {
        val months = summarizeByMonth(
            listOf(trip(LocalDateTime.of(2026, 11, 3, 8, 0), 10.55)),
            listOf(expense(LocalDate.of(2026, 11, 5), 40.0, 900.5))
        )

        assertEquals(
            CsvExport.BYTE_ORDER_MARK +
                "Month,Trips,Distance (km),Fuel fill-ups,Litres,Fuel spend (R)\r\n" +
                "2026-11,1,10.6,1,40.00,900.50\r\n",
            CsvExport.monthlySummary(months)
        )
    }
}

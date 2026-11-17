package com.triplog.util

import com.triplog.domain.model.FuelExpense
import com.triplog.domain.model.Trip
import java.time.YearMonth

/** Totals for one calendar month. A month appears only if it has at least one trip or expense. */
data class MonthSummary(
    val month: YearMonth,
    val tripCount: Int,
    val distanceKm: Double,
    val fillUpCount: Int,
    val litres: Double,
    val fuelSpendRands: Double
)

/** Groups trips and fuel expenses by calendar month, newest month first. */
fun summarizeByMonth(trips: List<Trip>, expenses: List<FuelExpense>): List<MonthSummary> {
    val tripsByMonth = trips.groupBy { YearMonth.from(it.dateTime) }
    val expensesByMonth = expenses.groupBy { YearMonth.from(it.date) }

    return (tripsByMonth.keys + expensesByMonth.keys)
        .sortedDescending()
        .map { month ->
            val monthTrips = tripsByMonth[month].orEmpty()
            val monthExpenses = expensesByMonth[month].orEmpty()
            MonthSummary(
                month = month,
                tripCount = monthTrips.size,
                distanceKm = monthTrips.sumOf { it.distanceKm },
                fillUpCount = monthExpenses.size,
                litres = monthExpenses.sumOf { it.litres },
                fuelSpendRands = monthExpenses.sumOf { it.amountRands }
            )
        }
}

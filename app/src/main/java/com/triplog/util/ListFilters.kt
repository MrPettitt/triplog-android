package com.triplog.util

import com.triplog.domain.model.FuelExpense
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import java.time.LocalDate
import java.time.YearMonth

/** How far back a list is limited. Ranges are relative to the day the list is viewed. */
enum class DateRange {
    ALL,
    THIS_MONTH,
    LAST_30_DAYS,
    THIS_YEAR;

    fun contains(date: LocalDate, today: LocalDate): Boolean = when (this) {
        ALL -> true
        THIS_MONTH -> YearMonth.from(date) == YearMonth.from(today)
        LAST_30_DAYS -> !date.isAfter(today) && !date.isBefore(today.minusDays(29))
        THIS_YEAR -> date.year == today.year
    }
}

/** Case-insensitive match of every word of [query] against [fields]. A blank query matches. */
internal fun matchesQuery(query: String, vararg fields: String): Boolean {
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val haystack = fields.joinToString(" ").lowercase()
    return words.all { it in haystack }
}

data class TripFilter(
    val query: String = "",
    val purpose: TripPurpose? = null,
    val range: DateRange = DateRange.ALL
) {
    val isActive: Boolean get() = query.isNotBlank() || purpose != null || range != DateRange.ALL

    fun apply(trips: List<Trip>, today: LocalDate = LocalDate.now()): List<Trip> = trips.filter { trip ->
        (purpose == null || trip.purpose == purpose) &&
            range.contains(trip.dateTime.toLocalDate(), today) &&
            matchesQuery(query, trip.fromLocation, trip.toLocation, trip.notes)
    }
}

data class ExpenseFilter(
    val query: String = "",
    val range: DateRange = DateRange.ALL
) {
    val isActive: Boolean get() = query.isNotBlank() || range != DateRange.ALL

    fun apply(expenses: List<FuelExpense>, today: LocalDate = LocalDate.now()): List<FuelExpense> =
        expenses.filter { expense ->
            range.contains(expense.date, today) && matchesQuery(query, expense.fuelStation)
        }
}

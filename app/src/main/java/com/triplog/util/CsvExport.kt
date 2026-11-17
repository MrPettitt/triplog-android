package com.triplog.util

import com.triplog.domain.model.FuelExpense
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Builds CSV text for trips and fuel expenses (RFC 4180: comma separated, CRLF line endings,
 * quotes doubled). Dates are ISO (2026-03-14), numbers always use a dot so any spreadsheet
 * reads them the same way. A byte order mark is included so Excel detects UTF-8.
 */
object CsvExport {

    val BYTE_ORDER_MARK: String = 0xFEFF.toChar().toString()
    private const val NEWLINE = "\r\n"

    private val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun trips(trips: List<Trip>, purposeLabel: (TripPurpose) -> String): String = build(
        header = listOf("Date", "Time", "Start location", "End location", "Distance (km)", "Purpose", "Notes"),
        rows = trips.map { trip ->
            listOf(
                trip.dateTime.format(dateFormat),
                trip.dateTime.format(timeFormat),
                text(trip.fromLocation),
                text(trip.toLocation),
                number(trip.distanceKm, 1),
                text(purposeLabel(trip.purpose)),
                text(trip.notes)
            )
        }
    )

    fun expenses(expenses: List<FuelExpense>): String = build(
        header = listOf("Date", "Fuel station", "Litres", "Amount (R)", "Odometer (km)", "Receipt photo"),
        rows = expenses.map { expense ->
            listOf(
                expense.date.format(dateFormat),
                text(expense.fuelStation),
                number(expense.litres, 2),
                number(expense.amountRands, 2),
                expense.odometerKm.toString(),
                if (expense.receiptImagePath != null) "Yes" else "No"
            )
        }
    )

    private fun build(header: List<String>, rows: List<List<String>>): String =
        buildString {
            append(BYTE_ORDER_MARK)
            append(header.joinToString(",") { escape(it) })
            append(NEWLINE)
            rows.forEach { row ->
                append(row.joinToString(",") { escape(it) })
                append(NEWLINE)
            }
        }

    private fun number(value: Double, decimals: Int): String =
        String.format(Locale.ROOT, "%.${decimals}f", value)

    /**
     * Free text typed by the user. A cell that starts with = + - @ (or a tab or carriage return)
     * would be run as a formula by Excel and similar tools, so it gets a leading apostrophe.
     */
    internal fun text(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.isNotEmpty() && trimmed[0] in FORMULA_TRIGGERS) "'$trimmed" else trimmed
    }

    internal fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private val FORMULA_TRIGGERS = charArrayOf('=', '+', '-', '@', '\t', '\r')
}

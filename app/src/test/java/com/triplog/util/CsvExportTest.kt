package com.triplog.util

import com.triplog.domain.model.FuelExpense
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

class CsvExportTest {

    private val bom = CsvExport.BYTE_ORDER_MARK
    private val label: (TripPurpose) -> String = { it.name.lowercase().replaceFirstChar(Char::uppercase) }

    private fun trip(
        from: String = "Head Office",
        to: String = "Client Site",
        notes: String = "",
        km: Double = 42.5
    ) = Trip(
        fromLocation = from,
        toLocation = to,
        dateTime = LocalDateTime.of(2026, 3, 14, 9, 5),
        distanceKm = km,
        purpose = TripPurpose.MEETING,
        notes = notes
    )

    private fun expense(station: String = "Engen Sandton", receipt: String? = "/r.jpg") = FuelExpense(
        date = LocalDate.of(2026, 3, 2),
        litres = 38.2,
        amountRands = 812.4,
        odometerKm = 84320,
        fuelStation = station,
        receiptImagePath = receipt
    )

    @Test
    fun trips_haveAHeaderAndOneLinePerTrip() {
        val csv = CsvExport.trips(listOf(trip(), trip(from = "Depot")), label)

        assertEquals(
            bom + "Date,Time,Start location,End location,Distance (km),Purpose,Notes\r\n" +
                "2026-03-14,09:05,Head Office,Client Site,42.5,Meeting,\r\n" +
                "2026-03-14,09:05,Depot,Client Site,42.5,Meeting,\r\n",
            csv
        )
    }

    @Test
    fun emptyList_givesOnlyTheHeader() {
        assertEquals(
            bom + "Date,Fuel station,Litres,Amount (R),Odometer (km),Receipt photo\r\n",
            CsvExport.expenses(emptyList())
        )
    }

    @Test
    fun expenses_useDotDecimalsAndReceiptFlag() {
        val csv = CsvExport.expenses(listOf(expense(), expense(station = "Shell", receipt = null)))

        assertEquals(
            bom + "Date,Fuel station,Litres,Amount (R),Odometer (km),Receipt photo\r\n" +
                "2026-03-02,Engen Sandton,38.20,812.40,84320,Yes\r\n" +
                "2026-03-02,Shell,38.20,812.40,84320,No\r\n",
            csv
        )
    }

    @Test
    fun numbers_ignoreTheDeviceLocale() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        try {
            val csv = CsvExport.expenses(listOf(expense()))
            assertTrue(csv, csv.contains("38.20,812.40"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun cellsWithCommasQuotesAndNewlines_areQuoted() {
        val csv = CsvExport.trips(
            listOf(trip(from = "A, B", notes = "said \"hi\"\nthen left")),
            label
        )

        assertTrue(csv, csv.contains("\"A, B\""))
        assertTrue(csv, csv.contains("\"said \"\"hi\"\"\nthen left\""))
    }

    @Test
    fun cellsThatLookLikeFormulas_areDefused() {
        val csv = CsvExport.trips(
            listOf(trip(from = "=HYPERLINK(\"http://x\")", to = "+1", notes = "-2 and @me")),
            label
        )

        assertTrue(csv, csv.contains("\"'=HYPERLINK(\"\"http://x\"\")\""))
        assertTrue(csv, csv.contains(",'+1,"))
        assertTrue(csv, csv.contains(",'-2 and @me\r\n"))
    }

    @Test
    fun ordinaryTextIsNotChanged_andIsTrimmed() {
        assertEquals("Midrand Warehouse", CsvExport.text("  Midrand Warehouse "))
        assertEquals("", CsvExport.text("   "))
        assertEquals("a-b", CsvExport.text("a-b"))
    }

    @Test
    fun escape_leavesPlainValuesAlone() {
        assertEquals("plain", CsvExport.escape("plain"))
        assertEquals("\"a,b\"", CsvExport.escape("a,b"))
    }
}

package com.triplog.data.repository

import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import com.triplog.testutil.FakeApiService
import com.triplog.testutil.FakeTripDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime

class TripRepositoryTest {

    private val dao = FakeTripDao()
    private val api = FakeApiService()
    private val repository = TripRepository(dao, api)

    private fun trip(from: String = "A") = Trip(
        fromLocation = from,
        toLocation = "B",
        dateTime = LocalDateTime.of(2026, 1, 1, 8, 0),
        distanceKm = 12.5,
        purpose = TripPurpose.MEETING,
        notes = "n"
    )

    @Test
    fun saveTrip_storesUnsyncedTrip() = runTest {
        val id = repository.saveTrip(trip())

        val stored = dao.trips.value.single()
        assertEquals(id, stored.id)
        assertFalse(stored.isSynced)
    }

    @Test
    fun sync_marksTripSyncedAndSendsExpectedRequest() = runTest {
        repository.saveTrip(trip(from = "Head Office"))

        repository.syncPendingTrips()

        val stored = dao.trips.value.single()
        assertTrue(stored.isSynced)
        assertEquals("remote-trip", stored.remoteId)

        val request = api.tripRequests.single()
        assertEquals("Head Office", request.fromLocation)
        assertEquals("MEETING", request.purpose)
        assertEquals("2026-01-01T08:00:00", request.dateTime)
        assertEquals(12.5, request.distanceKm, 0.0)
    }

    @Test
    fun sync_leavesTripPendingOnServerError() = runTest {
        api.createTripBehaviour = { FakeApiService.serverError() }
        repository.saveTrip(trip())

        repository.syncPendingTrips()

        assertFalse(dao.trips.value.single().isSynced)
    }

    @Test
    fun sync_leavesTripPendingOnNetworkException() = runTest {
        api.createTripBehaviour = { throw IOException("offline") }
        repository.saveTrip(trip())

        repository.syncPendingTrips()

        assertFalse(dao.trips.value.single().isSynced)
    }

    @Test
    fun sync_continuesAfterOneTripFails() = runTest {
        var calls = 0
        api.createTripBehaviour = {
            calls++
            if (calls == 1) throw IOException("flaky")
            else retrofit2.Response.success(
                com.triplog.data.remote.dto.ApiResponse(true, FakeApiService.tripDto("ok"), null)
            )
        }
        repository.saveTrip(trip(from = "first"))
        repository.saveTrip(trip(from = "second"))

        repository.syncPendingTrips()

        val byFrom = dao.trips.value.associateBy { it.fromLocation }
        assertFalse(byFrom.getValue("first").isSynced)
        assertTrue(byFrom.getValue("second").isSynced)
    }

    @Test
    fun sync_skipsAlreadySyncedTrips() = runTest {
        repository.saveTrip(trip())
        repository.syncPendingTrips()
        api.tripRequests.clear()

        repository.syncPendingTrips()

        assertTrue(api.tripRequests.isEmpty())
    }
}

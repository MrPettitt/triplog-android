package com.triplog.data.repository

import com.triplog.data.remote.dto.ApiResponse
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import com.triplog.sync.SyncResult
import com.triplog.testutil.FakeApiService
import com.triplog.testutil.FakeSyncScheduler
import com.triplog.testutil.FakeTripDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDateTime

class TripRepositoryTest {

    private val dao = FakeTripDao()
    private val api = FakeApiService()
    private val scheduler = FakeSyncScheduler()
    private val repository = TripRepository(dao, api, scheduler)

    private fun trip(from: String = "A") = Trip(
        fromLocation = from,
        toLocation = "B",
        dateTime = LocalDateTime.of(2026, 1, 1, 8, 0),
        distanceKm = 12.5,
        purpose = TripPurpose.MEETING,
        notes = "n"
    )

    @Test
    fun saveTrip_storesUnsyncedTripAndRequestsSync() = runTest {
        val id = repository.saveTrip(trip())

        val stored = dao.trips.value.single()
        assertEquals(id, stored.id)
        assertFalse(stored.isSynced)
        assertEquals(1, scheduler.requests)
    }

    @Test
    fun sync_marksTripSyncedAndSendsExpectedRequest() = runTest {
        repository.saveTrip(trip(from = "Head Office"))

        val result = repository.syncPendingTrips()

        assertEquals(SyncResult(synced = 1), result)
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
    fun sync_sendsTheSameClientIdAsIdempotencyKeyOnEveryAttempt() = runTest {
        api.createTripBehaviour = { throw IOException("offline") }
        repository.saveTrip(trip())

        repository.syncPendingTrips()
        repository.syncPendingTrips()

        val clientId = dao.trips.value.single().clientId
        assertTrue(clientId.isNotBlank())
        assertEquals(listOf(clientId, clientId), api.idempotencyKeys)
        assertEquals(clientId, api.tripRequests.first().clientId)
    }

    @Test
    fun sync_serverError_isRetryableAndLeavesTripPending() = runTest {
        api.createTripBehaviour = { FakeApiService.serverError() }
        repository.saveTrip(trip())

        val result = repository.syncPendingTrips()

        assertEquals(SyncResult(retryable = 1), result)
        assertTrue(result.shouldRetry)
        assertFalse(dao.trips.value.single().isSynced)
    }

    @Test
    fun sync_clientError_isRejectedNotRetried() = runTest {
        api.createTripBehaviour = { FakeApiService.httpError(422) }
        repository.saveTrip(trip())

        val result = repository.syncPendingTrips()

        assertEquals(SyncResult(rejected = 1), result)
        assertFalse(result.shouldRetry)
        assertFalse(dao.trips.value.single().isSynced)
    }

    @Test
    fun sync_tooManyRequests_isRetryable() = runTest {
        api.createTripBehaviour = { FakeApiService.httpError(429) }
        repository.saveTrip(trip())

        assertTrue(repository.syncPendingTrips().shouldRetry)
    }

    @Test
    fun sync_networkException_isRetryableAndLeavesTripPending() = runTest {
        api.createTripBehaviour = { throw IOException("offline") }
        repository.saveTrip(trip())

        val result = repository.syncPendingTrips()

        assertEquals(SyncResult(retryable = 1), result)
        assertFalse(dao.trips.value.single().isSynced)
    }

    @Test
    fun sync_successWithoutRemoteId_isRejectedAndStaysPending() = runTest {
        api.createTripBehaviour = { Response.success(ApiResponse(true, null, null)) }
        repository.saveTrip(trip())

        val result = repository.syncPendingTrips()

        assertEquals(SyncResult(rejected = 1), result)
        assertFalse(dao.trips.value.single().isSynced)
    }

    @Test
    fun sync_continuesAfterOneTripFails() = runTest {
        var calls = 0
        api.createTripBehaviour = {
            calls++
            if (calls == 1) throw IOException("flaky")
            else Response.success(ApiResponse(true, FakeApiService.tripDto("ok"), null))
        }
        repository.saveTrip(trip(from = "first"))
        repository.saveTrip(trip(from = "second"))

        val result = repository.syncPendingTrips()

        assertEquals(SyncResult(synced = 1, retryable = 1), result)
        val byFrom = dao.trips.value.associateBy { it.fromLocation }
        assertFalse(byFrom.getValue("first").isSynced)
        assertTrue(byFrom.getValue("second").isSynced)
    }

    @Test
    fun sync_skipsAlreadySyncedTrips() = runTest {
        repository.saveTrip(trip())
        repository.syncPendingTrips()
        api.tripRequests.clear()

        val result = repository.syncPendingTrips()

        assertEquals(SyncResult(), result)
        assertTrue(api.tripRequests.isEmpty())
    }
}

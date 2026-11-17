package com.triplog.data.repository

import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import com.triplog.sync.SyncResult
import com.triplog.testutil.FakeApiService
import com.triplog.testutil.FakeSyncScheduler
import com.triplog.testutil.FakeTripDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime

class TripEditDeleteTest {

    private val dao = FakeTripDao()
    private val api = FakeApiService()
    private val scheduler = FakeSyncScheduler()
    private val repository = TripRepository(dao, api, scheduler)

    private fun trip(from: String = "A") = Trip(
        fromLocation = from,
        toLocation = "B",
        dateTime = LocalDateTime.of(2026, 1, 1, 8, 0),
        distanceKm = 10.0,
        purpose = TripPurpose.MEETING
    )

    private suspend fun savedAndSynced(): Trip {
        val id = repository.saveTrip(trip())
        repository.syncPendingTrips()
        return repository.getTrip(id)!!
    }

    // --- edit ---

    @Test
    fun update_changesFieldsAndMarksSyncedTripPending() = runTest {
        val synced = savedAndSynced()
        assertTrue(synced.isSynced)

        repository.updateTrip(synced.copy(fromLocation = "Depot", distanceKm = 99.5, purpose = TripPurpose.DELIVERY))

        val edited = repository.getTrip(synced.id)!!
        assertEquals("Depot", edited.fromLocation)
        assertEquals(99.5, edited.distanceKm, 0.0)
        assertEquals(TripPurpose.DELIVERY, edited.purpose)
        assertFalse(edited.isSynced)
        assertEquals("remote-trip", edited.remoteId)
        assertEquals(2, scheduler.requests)
    }

    @Test
    fun sync_sendsEditedTripAsUpdateNotCreate() = runTest {
        val synced = savedAndSynced()
        repository.updateTrip(synced.copy(toLocation = "Warehouse"))

        val result = repository.syncPendingTrips()

        assertEquals(SyncResult(synced = 1), result)
        assertEquals(1, api.tripRequests.size)
        val (remoteId, body) = api.tripUpdates.single()
        assertEquals("remote-trip", remoteId)
        assertEquals("Warehouse", body.toLocation)
        assertTrue(repository.getTrip(synced.id)!!.isSynced)
    }

    @Test
    fun update_ofUnsyncedTrip_isStillACreateOnSync() = runTest {
        api.createTripBehaviour = { throw IOException("offline") }
        val id = repository.saveTrip(trip())
        repository.syncPendingTrips()
        repository.updateTrip(repository.getTrip(id)!!.copy(fromLocation = "Edited"))

        api.createTripBehaviour = { retrofit2.Response.success(
            com.triplog.data.remote.dto.ApiResponse(true, FakeApiService.tripDto("r1"), null)
        ) }
        repository.syncPendingTrips()

        assertTrue(api.tripUpdates.isEmpty())
        assertEquals("Edited", api.tripRequests.last().fromLocation)
        assertTrue(repository.getTrip(id)!!.isSynced)
    }

    @Test
    fun edit_whileUploadIsInFlight_isNotMarkedSynced() = runTest {
        val id = repository.saveTrip(trip())
        api.createTripBehaviour = {
            // The user saves an edit while the create request is still running.
            repository.updateTrip(repository.getTrip(id)!!.copy(notes = "late edit"))
            retrofit2.Response.success(
                com.triplog.data.remote.dto.ApiResponse(true, FakeApiService.tripDto("r1"), null)
            )
        }

        repository.syncPendingTrips()

        val afterFirst = repository.getTrip(id)!!
        assertFalse("the late edit must still be pending", afterFirst.isSynced)
        assertEquals("r1", afterFirst.remoteId)

        repository.syncPendingTrips()

        assertEquals("late edit", api.tripUpdates.single().second.notes)
        assertEquals(1, api.tripRequests.size)
        assertTrue(repository.getTrip(id)!!.isSynced)
    }

    @Test
    fun failedUpdate_staysPendingAndIsRetried() = runTest {
        val synced = savedAndSynced()
        repository.updateTrip(synced.copy(notes = "x"))
        api.updateTripBehaviour = { throw IOException("offline") }

        val result = repository.syncPendingTrips()

        assertEquals(SyncResult(retryable = 1), result)
        assertFalse(repository.getTrip(synced.id)!!.isSynced)
    }

    // --- delete ---

    @Test
    fun delete_hidesTripFromListsAndTotalsImmediately() = runTest {
        val id = repository.saveTrip(trip())

        repository.deleteTrip(id)

        assertNull(repository.getTrip(id))
        assertTrue(repository.allTrips.first().isEmpty())
        assertEquals(0, repository.totalTripCount.first())
        assertNull(repository.totalDistanceKm.first())
    }

    @Test
    fun delete_ofNeverSyncedTrip_isRemovedWithoutCallingTheServer() = runTest {
        api.createTripBehaviour = { throw IOException("offline") }
        val id = repository.saveTrip(trip())
        repository.deleteTrip(id)

        val result = repository.syncPendingTrips()

        assertEquals(SyncResult(synced = 1), result)
        assertTrue(dao.trips.value.isEmpty())
        assertTrue(api.tripRequests.isEmpty())
        assertTrue(api.tripDeletes.isEmpty())
    }

    @Test
    fun delete_ofSyncedTrip_callsDeleteThenRemovesLocalRow() = runTest {
        val synced = savedAndSynced()
        repository.deleteTrip(synced.id)
        assertEquals("tombstone counts as pending", 1, repository.pendingSyncCount.first())

        repository.syncPendingTrips()

        assertEquals(listOf("remote-trip"), api.tripDeletes)
        assertTrue(dao.trips.value.isEmpty())
        assertEquals(0, repository.pendingSyncCount.first())
    }

    @Test
    fun delete_whenServerAlreadyLostIt_countsAsDone() = runTest {
        val synced = savedAndSynced()
        repository.deleteTrip(synced.id)
        api.deleteTripBehaviour = { FakeApiService.httpError(404) }

        repository.syncPendingTrips()

        assertTrue(dao.trips.value.isEmpty())
    }

    @Test
    fun delete_serverError_keepsTombstoneForRetry() = runTest {
        val synced = savedAndSynced()
        repository.deleteTrip(synced.id)
        api.deleteTripBehaviour = { FakeApiService.serverError() }

        val result = repository.syncPendingTrips()

        assertEquals(SyncResult(retryable = 1), result)
        assertNotNull(dao.trips.value.singleOrNull())
        assertNull(repository.getTrip(synced.id))

        api.deleteTripBehaviour = { retrofit2.Response.success(Unit) }
        repository.syncPendingTrips()

        assertTrue(dao.trips.value.isEmpty())
    }

    @Test
    fun delete_whileCreateIsInFlight_stillDeletesTheServerCopy() = runTest {
        val id = repository.saveTrip(trip())
        api.createTripBehaviour = {
            repository.deleteTrip(id)
            retrofit2.Response.success(
                com.triplog.data.remote.dto.ApiResponse(true, FakeApiService.tripDto("r1"), null)
            )
        }

        repository.syncPendingTrips()
        repository.syncPendingTrips()

        assertEquals(listOf("r1"), api.tripDeletes)
        assertTrue(dao.trips.value.isEmpty())
    }
}

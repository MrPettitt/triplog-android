package com.triplog.data.repository

import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import com.triplog.testutil.FakeTripDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TripRepositoryTest {

    private val dao = FakeTripDao()
    private val repository = TripRepository(dao)

    private fun trip(from: String = "A", at: LocalDateTime = LocalDateTime.of(2026, 1, 1, 8, 0), km: Double = 10.0) =
        Trip(
            fromLocation = from,
            toLocation = "B",
            dateTime = at,
            distanceKm = km,
            purpose = TripPurpose.MEETING,
            notes = "n"
        )

    @Test
    fun saveTrip_storesItAndReturnsTheId() = runTest {
        val id = repository.saveTrip(trip("Head Office"))

        val stored = repository.getTrip(id)!!
        assertEquals("Head Office", stored.fromLocation)
        assertEquals(TripPurpose.MEETING, stored.purpose)
    }

    @Test
    fun allTrips_areNewestFirst() = runTest {
        repository.saveTrip(trip("old", at = LocalDateTime.of(2026, 1, 1, 8, 0)))
        repository.saveTrip(trip("new", at = LocalDateTime.of(2026, 3, 1, 8, 0)))
        repository.saveTrip(trip("middle", at = LocalDateTime.of(2026, 2, 1, 8, 0)))

        assertEquals(listOf("new", "middle", "old"), repository.allTrips.first().map { it.fromLocation })
    }

    @Test
    fun totals_followTheStoredTrips() = runTest {
        assertEquals(0, repository.totalTripCount.first())
        assertNull(repository.totalDistanceKm.first())

        repository.saveTrip(trip(km = 12.5))
        repository.saveTrip(trip(km = 7.5))

        assertEquals(2, repository.totalTripCount.first())
        assertEquals(20.0, repository.totalDistanceKm.first()!!, 0.0)
    }

    @Test
    fun updateTrip_changesFieldsButKeepsIdAndCreationTime() = runTest {
        val id = repository.saveTrip(trip("Before"))
        val original = repository.getTrip(id)!!

        repository.updateTrip(original.copy(fromLocation = "After", distanceKm = 99.5, createdAt = LocalDateTime.MAX))

        val edited = repository.getTrip(id)!!
        assertEquals("After", edited.fromLocation)
        assertEquals(99.5, edited.distanceKm, 0.0)
        assertEquals(original.createdAt, edited.createdAt)
        assertEquals(1, dao.trips.value.size)
    }

    @Test
    fun updateTrip_ofAMissingTrip_doesNothing() = runTest {
        repository.updateTrip(trip().copy(id = 42))

        assertTrue(dao.trips.value.isEmpty())
    }

    @Test
    fun deleteTrip_removesItFromListsAndTotals() = runTest {
        val id = repository.saveTrip(trip(km = 10.0))
        repository.saveTrip(trip("keep", km = 5.0))

        repository.deleteTrip(id)

        assertNull(repository.getTrip(id))
        assertEquals(listOf("keep"), repository.allTrips.first().map { it.fromLocation })
        assertEquals(5.0, repository.totalDistanceKm.first()!!, 0.0)
    }

    @Test
    fun deleteTrip_ofAMissingTrip_isHarmless() = runTest {
        repository.saveTrip(trip())

        repository.deleteTrip(999)

        assertEquals(1, dao.trips.value.size)
    }
}

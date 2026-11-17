package com.triplog.testutil

import com.triplog.data.local.dao.FuelExpenseDao
import com.triplog.data.local.dao.TripDao
import com.triplog.data.local.entity.FuelExpenseEntity
import com.triplog.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [TripDao] that mirrors the SQL semantics of the real one. */
class FakeTripDao : TripDao {
    val trips = MutableStateFlow<List<TripEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllTrips(): Flow<List<TripEntity>> =
        trips.map { list -> list.sortedByDescending { it.dateTime } }

    override suspend fun getTripById(id: Long): TripEntity? = trips.value.firstOrNull { it.id == id }

    override fun getTotalDistanceKm(): Flow<Double?> = trips.map { list ->
        if (list.isEmpty()) null else list.sumOf { it.distanceKm }
    }

    override fun getTotalTripCount(): Flow<Int> = trips.map { it.size }

    override suspend fun insertTrip(trip: TripEntity): Long {
        val id = if (trip.id == 0L) nextId++ else trip.id
        trips.value = trips.value.filterNot { it.id == id } + trip.copy(id = id)
        return id
    }

    override suspend fun updateTrip(trip: TripEntity) {
        trips.value = trips.value.map { if (it.id == trip.id) trip else it }
    }

    override suspend fun deleteById(id: Long) {
        trips.value = trips.value.filterNot { it.id == id }
    }
}

/** In-memory [FuelExpenseDao] that mirrors the SQL semantics of the real one. */
class FakeFuelExpenseDao : FuelExpenseDao {
    val expenses = MutableStateFlow<List<FuelExpenseEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllExpenses(): Flow<List<FuelExpenseEntity>> =
        expenses.map { list -> list.sortedWith(compareByDescending<FuelExpenseEntity> { it.date }.thenByDescending { it.createdAt }) }

    override suspend fun getExpenseById(id: Long): FuelExpenseEntity? = expenses.value.firstOrNull { it.id == id }

    override fun getTotalExpenseRands(): Flow<Double?> = expenses.map { list ->
        if (list.isEmpty()) null else list.sumOf { it.amountRands }
    }

    override suspend fun insertExpense(expense: FuelExpenseEntity): Long {
        val id = if (expense.id == 0L) nextId++ else expense.id
        expenses.value = expenses.value.filterNot { it.id == id } + expense.copy(id = id)
        return id
    }

    override suspend fun updateExpense(expense: FuelExpenseEntity) {
        expenses.value = expenses.value.map { if (it.id == expense.id) expense else it }
    }

    override suspend fun deleteById(id: Long) {
        expenses.value = expenses.value.filterNot { it.id == id }
    }
}

package com.triplog.testutil

import com.triplog.data.local.dao.FuelExpenseDao
import com.triplog.data.local.dao.TripDao
import com.triplog.data.local.entity.FuelExpenseEntity
import com.triplog.data.local.entity.TripEntity
import com.triplog.data.remote.ApiService
import com.triplog.data.remote.dto.ApiResponse
import com.triplog.data.remote.dto.CreateExpenseRequest
import com.triplog.data.remote.dto.CreateTripRequest
import com.triplog.data.remote.dto.FuelExpenseDto
import com.triplog.data.remote.dto.TripDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class FakeTripDao : TripDao {
    val trips = MutableStateFlow<List<TripEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllTrips(): Flow<List<TripEntity>> = trips
    override suspend fun getUnsyncedTrips(): List<TripEntity> = trips.value.filter { !it.isSynced }
    override fun getPendingSyncCount(): Flow<Int> = trips.map { list -> list.count { !it.isSynced } }
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

    override suspend fun markAsSynced(localId: Long, remoteId: String) {
        trips.value = trips.value.map {
            if (it.id == localId) it.copy(isSynced = true, remoteId = remoteId) else it
        }
    }

    override suspend fun deleteTrip(trip: TripEntity) {
        trips.value = trips.value.filterNot { it.id == trip.id }
    }
}

class FakeFuelExpenseDao : FuelExpenseDao {
    val expenses = MutableStateFlow<List<FuelExpenseEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllExpenses(): Flow<List<FuelExpenseEntity>> = expenses
    override suspend fun getUnsyncedExpenses(): List<FuelExpenseEntity> =
        expenses.value.filter { !it.isSynced }

    override fun getTotalExpenseRands(): Flow<Double?> = expenses.map { list ->
        if (list.isEmpty()) null else list.sumOf { it.amountRands }
    }

    override fun getPendingSyncCount(): Flow<Int> = expenses.map { list -> list.count { !it.isSynced } }

    override suspend fun insertExpense(expense: FuelExpenseEntity): Long {
        val id = if (expense.id == 0L) nextId++ else expense.id
        expenses.value = expenses.value.filterNot { it.id == id } + expense.copy(id = id)
        return id
    }

    override suspend fun updateExpense(expense: FuelExpenseEntity) {
        expenses.value = expenses.value.map { if (it.id == expense.id) expense else it }
    }

    override suspend fun setRemoteId(localId: Long, remoteId: String) {
        expenses.value = expenses.value.map { if (it.id == localId) it.copy(remoteId = remoteId) else it }
    }

    override suspend fun markAsSynced(localId: Long, remoteId: String) {
        expenses.value = expenses.value.map {
            if (it.id == localId) it.copy(isSynced = true, remoteId = remoteId) else it
        }
    }

    override suspend fun deleteExpense(expense: FuelExpenseEntity) {
        expenses.value = expenses.value.filterNot { it.id == expense.id }
    }
}

class FakeSyncScheduler : com.triplog.sync.SyncScheduler {
    var requests = 0
        private set

    override fun requestSync() {
        requests++
    }
}

/** Scriptable [ApiService]: set the behaviours you need and inspect what was sent. */
class FakeApiService : ApiService {
    var createTripBehaviour: suspend (CreateTripRequest) -> Response<ApiResponse<TripDto>> = {
        Response.success(ApiResponse(true, tripDto("remote-trip"), null))
    }
    var createExpenseBehaviour: suspend (CreateExpenseRequest) -> Response<ApiResponse<FuelExpenseDto>> = {
        Response.success(ApiResponse(true, expenseDto("remote-expense"), null))
    }

    var receiptBehaviour: suspend (String) -> Response<ApiResponse<FuelExpenseDto>> = { id ->
        Response.success(ApiResponse(true, expenseDto(id), null))
    }

    val tripRequests = mutableListOf<CreateTripRequest>()
    val expenseRequests = mutableListOf<CreateExpenseRequest>()
    val receiptUploads = mutableListOf<String>()

    val idempotencyKeys = mutableListOf<String>()

    override suspend fun createTrip(
        idempotencyKey: String,
        request: CreateTripRequest
    ): Response<ApiResponse<TripDto>> {
        idempotencyKeys += idempotencyKey
        tripRequests += request
        return createTripBehaviour(request)
    }

    override suspend fun getTrips(): Response<ApiResponse<List<TripDto>>> =
        Response.success(ApiResponse(true, emptyList(), null))

    override suspend fun createExpense(
        idempotencyKey: String,
        request: CreateExpenseRequest
    ): Response<ApiResponse<FuelExpenseDto>> {
        idempotencyKeys += idempotencyKey
        expenseRequests += request
        return createExpenseBehaviour(request)
    }

    override suspend fun uploadReceipt(
        expenseId: String,
        receipt: MultipartBody.Part,
        expenseIdPart: RequestBody
    ): Response<ApiResponse<FuelExpenseDto>> {
        receiptUploads += expenseId
        return receiptBehaviour(expenseId)
    }

    override suspend fun getExpenses(): Response<ApiResponse<List<FuelExpenseDto>>> =
        Response.success(ApiResponse(true, emptyList(), null))

    companion object {
        fun tripDto(id: String) = TripDto(
            id = id, fromLocation = "A", toLocation = "B", dateTime = "2026-01-01T08:00:00",
            distanceKm = 10.0, purpose = "OTHER", notes = "", employeeId = "e1"
        )

        fun expenseDto(id: String) = FuelExpenseDto(
            id = id, date = "2026-01-01", litres = 10.0, amountRands = 200.0,
            odometerKm = 1000, fuelStation = "Engen", receiptUrl = null
        )

        fun <T> httpError(code: Int): Response<T> =
            Response.error(code, "".toResponseBody(null))

        fun <T> serverError(): Response<T> =
            Response.error(500, "".toResponseBody(null))
    }
}

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
import com.triplog.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalDateTime

/** In-memory [TripDao] that mirrors the SQL semantics of the real one. */
class FakeTripDao : TripDao {
    val trips = MutableStateFlow<List<TripEntity>>(emptyList())
    private var nextId = 1L

    private val visible get() = trips.value.filter { !it.isDeleted }

    override fun getAllTrips(): Flow<List<TripEntity>> = trips.map { list -> list.filter { !it.isDeleted } }
    override suspend fun getTripById(id: Long): TripEntity? = visible.firstOrNull { it.id == id }
    override suspend fun getUnsyncedTrips(): List<TripEntity> = trips.value.filter { !it.isSynced }
    override fun getPendingSyncCount(): Flow<Int> = trips.map { list -> list.count { !it.isSynced } }
    override fun getTotalDistanceKm(): Flow<Double?> = trips.map { all ->
        val list = all.filter { !it.isDeleted }
        if (list.isEmpty()) null else list.sumOf { it.distanceKm }
    }

    override fun getTotalTripCount(): Flow<Int> = trips.map { list -> list.count { !it.isDeleted } }

    override suspend fun insertTrip(trip: TripEntity): Long {
        val id = if (trip.id == 0L) nextId++ else trip.id
        trips.value = trips.value.filterNot { it.id == id } + trip.copy(id = id)
        return id
    }

    override suspend fun updateDetails(
        id: Long,
        fromLocation: String,
        toLocation: String,
        dateTime: LocalDateTime,
        distanceKm: Double,
        purpose: String,
        notes: String
    ) = change(id) {
        if (it.isDeleted) it else it.copy(
            fromLocation = fromLocation, toLocation = toLocation, dateTime = dateTime,
            distanceKm = distanceKm, purpose = purpose, notes = notes,
            isSynced = false, revision = it.revision + 1
        )
    }

    override suspend fun markDeleted(id: Long) =
        change(id) { it.copy(isDeleted = true, isSynced = false, revision = it.revision + 1) }

    override suspend fun deleteById(id: Long) {
        trips.value = trips.value.filterNot { it.id == id }
    }

    override suspend fun setRemoteId(localId: Long, remoteId: String) =
        change(localId) { it.copy(remoteId = remoteId) }

    override suspend fun markAsSynced(localId: Long, remoteId: String, revision: Int) =
        change(localId) {
            if (it.revision == revision) it.copy(isSynced = true, remoteId = remoteId) else it
        }

    private fun change(id: Long, transform: (TripEntity) -> TripEntity) {
        trips.value = trips.value.map { if (it.id == id) transform(it) else it }
    }
}

/** In-memory [FuelExpenseDao] that mirrors the SQL semantics of the real one. */
class FakeFuelExpenseDao : FuelExpenseDao {
    val expenses = MutableStateFlow<List<FuelExpenseEntity>>(emptyList())
    private var nextId = 1L

    private val visible get() = expenses.value.filter { !it.isDeleted }

    override fun getAllExpenses(): Flow<List<FuelExpenseEntity>> =
        expenses.map { list -> list.filter { !it.isDeleted } }

    override suspend fun getExpenseById(id: Long): FuelExpenseEntity? = visible.firstOrNull { it.id == id }
    override suspend fun getUnsyncedExpenses(): List<FuelExpenseEntity> =
        expenses.value.filter { !it.isSynced }

    override fun getTotalExpenseRands(): Flow<Double?> = expenses.map { all ->
        val list = all.filter { !it.isDeleted }
        if (list.isEmpty()) null else list.sumOf { it.amountRands }
    }

    override fun getPendingSyncCount(): Flow<Int> = expenses.map { list -> list.count { !it.isSynced } }

    override suspend fun insertExpense(expense: FuelExpenseEntity): Long {
        val id = if (expense.id == 0L) nextId++ else expense.id
        expenses.value = expenses.value.filterNot { it.id == id } + expense.copy(id = id)
        return id
    }

    override suspend fun updateDetails(
        id: Long,
        date: LocalDate,
        litres: Double,
        amountRands: Double,
        odometerKm: Int,
        fuelStation: String,
        receiptImagePath: String?,
        receiptUploaded: Boolean
    ) = change(id) {
        if (it.isDeleted) it else it.copy(
            date = date, litres = litres, amountRands = amountRands, odometerKm = odometerKm,
            fuelStation = fuelStation, receiptImagePath = receiptImagePath,
            receiptUploaded = receiptUploaded, isSynced = false, revision = it.revision + 1
        )
    }

    override suspend fun markDeleted(id: Long) =
        change(id) { it.copy(isDeleted = true, isSynced = false, revision = it.revision + 1) }

    override suspend fun deleteById(id: Long) {
        expenses.value = expenses.value.filterNot { it.id == id }
    }

    override suspend fun setRemoteId(localId: Long, remoteId: String) =
        change(localId) { it.copy(remoteId = remoteId) }

    override suspend fun markReceiptUploaded(localId: Long, revision: Int) =
        change(localId) { if (it.revision == revision) it.copy(receiptUploaded = true) else it }

    override suspend fun markAsSynced(localId: Long, remoteId: String, revision: Int) =
        change(localId) {
            if (it.revision == revision) it.copy(isSynced = true, remoteId = remoteId) else it
        }

    private fun change(id: Long, transform: (FuelExpenseEntity) -> FuelExpenseEntity) {
        expenses.value = expenses.value.map { if (it.id == id) transform(it) else it }
    }
}

class FakeSyncScheduler : SyncScheduler {
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
    var updateTripBehaviour: suspend (String) -> Response<ApiResponse<TripDto>> = { id ->
        Response.success(ApiResponse(true, tripDto(id), null))
    }
    var deleteTripBehaviour: suspend (String) -> Response<Unit> = { Response.success(Unit) }

    var createExpenseBehaviour: suspend (CreateExpenseRequest) -> Response<ApiResponse<FuelExpenseDto>> = {
        Response.success(ApiResponse(true, expenseDto("remote-expense"), null))
    }
    var updateExpenseBehaviour: suspend (String) -> Response<ApiResponse<FuelExpenseDto>> = { id ->
        Response.success(ApiResponse(true, expenseDto(id), null))
    }
    var deleteExpenseBehaviour: suspend (String) -> Response<Unit> = { Response.success(Unit) }
    var receiptBehaviour: suspend (String) -> Response<ApiResponse<FuelExpenseDto>> = { id ->
        Response.success(ApiResponse(true, expenseDto(id), null))
    }

    val tripRequests = mutableListOf<CreateTripRequest>()
    val tripUpdates = mutableListOf<Pair<String, CreateTripRequest>>()
    val tripDeletes = mutableListOf<String>()
    val expenseRequests = mutableListOf<CreateExpenseRequest>()
    val expenseUpdates = mutableListOf<Pair<String, CreateExpenseRequest>>()
    val expenseDeletes = mutableListOf<String>()
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

    override suspend fun updateTrip(tripId: String, request: CreateTripRequest): Response<ApiResponse<TripDto>> {
        tripUpdates += tripId to request
        return updateTripBehaviour(tripId)
    }

    override suspend fun deleteTrip(tripId: String): Response<Unit> {
        tripDeletes += tripId
        return deleteTripBehaviour(tripId)
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

    override suspend fun updateExpense(
        expenseId: String,
        request: CreateExpenseRequest
    ): Response<ApiResponse<FuelExpenseDto>> {
        expenseUpdates += expenseId to request
        return updateExpenseBehaviour(expenseId)
    }

    override suspend fun deleteExpense(expenseId: String): Response<Unit> {
        expenseDeletes += expenseId
        return deleteExpenseBehaviour(expenseId)
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

        fun <T> serverError(): Response<T> = httpError(500)
    }
}

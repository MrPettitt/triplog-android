package com.triplog.data.remote

import com.triplog.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Trips ---
    @POST("trips")
    suspend fun createTrip(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateTripRequest
    ): Response<ApiResponse<TripDto>>

    @PUT("trips/{id}")
    suspend fun updateTrip(
        @Path("id") tripId: String,
        @Body request: CreateTripRequest
    ): Response<ApiResponse<TripDto>>

    @DELETE("trips/{id}")
    suspend fun deleteTrip(@Path("id") tripId: String): Response<Unit>

    @GET("trips")
    suspend fun getTrips(): Response<ApiResponse<List<TripDto>>>

    // --- Fuel Expenses ---
    @POST("fuel-expenses")
    suspend fun createExpense(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateExpenseRequest
    ): Response<ApiResponse<FuelExpenseDto>>

    @PUT("fuel-expenses/{id}")
    suspend fun updateExpense(
        @Path("id") expenseId: String,
        @Body request: CreateExpenseRequest
    ): Response<ApiResponse<FuelExpenseDto>>

    @DELETE("fuel-expenses/{id}")
    suspend fun deleteExpense(@Path("id") expenseId: String): Response<Unit>

    @Multipart
    @POST("fuel-expenses/{id}/receipt")
    suspend fun uploadReceipt(
        @Path("id") expenseId: String,
        @Part receipt: MultipartBody.Part,
        @Part("expense_id") expenseIdPart: RequestBody
    ): Response<ApiResponse<FuelExpenseDto>>

    @GET("fuel-expenses")
    suspend fun getExpenses(): Response<ApiResponse<List<FuelExpenseDto>>>
}

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
        @Body request: CreateTripRequest
    ): Response<ApiResponse<TripDto>>

    @GET("trips")
    suspend fun getTrips(): Response<ApiResponse<List<TripDto>>>

    // --- Fuel Expenses ---
    @POST("fuel-expenses")
    suspend fun createExpense(
        @Body request: CreateExpenseRequest
    ): Response<ApiResponse<FuelExpenseDto>>

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

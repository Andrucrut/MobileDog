package com.example.dogapp.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("health")
    suspend fun health(): Map<String, Any>
    @POST("account/auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    @POST("account/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): TokenResponseDto

    @POST("account/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): TokenResponseDto

    @GET("account/user/me")
    suspend fun me(@Header("Authorization") token: String): UserDto

    @PATCH("account/user/me")
    suspend fun updateMe(
        @Header("Authorization") token: String,
        @Body body: UserUpdateDto,
    ): UserDto

    @GET("booking/dogs/")
    suspend fun dogs(@Header("Authorization") token: String): List<DogDto>

    @POST("booking/dogs/")
    suspend fun createDog(@Header("Authorization") token: String, @Body body: DogCreateDto): DogDto

    @PATCH("booking/dogs/{id}")
    suspend fun updateDog(
        @retrofit2.http.Path("id") dogId: String,
        @Header("Authorization") token: String,
        @Body body: DogUpdateDto,
    ): DogDto

    @GET("booking/walkers/search")
    suspend fun searchWalkers(
        @Header("Authorization") token: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius_km") radiusKm: Double? = null,
        @Query("min_rating") minRating: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
    ): List<WalkerDto>

    @POST("booking/bookings/")
    suspend fun createBooking(@Header("Authorization") token: String, @Body body: BookingCreateDto): BookingDto

    @GET("booking/bookings/me/owner")
    suspend fun ownerBookings(@Header("Authorization") token: String): List<BookingDto>

    @GET("booking/bookings/me/walker")
    suspend fun walkerBookings(@Header("Authorization") token: String): List<BookingDto>

    @PATCH("booking/bookings/{id}/status")
    suspend fun updateBookingStatus(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
        @Body body: BookingStatusUpdateDto,
    ): BookingDto

    @POST("tracking/walk-sessions/start")
    suspend fun startSession(@Header("Authorization") token: String, @Body body: WalkSessionStartDto): WalkSessionDto

    @GET("tracking/walk-sessions/by-booking/{id}")
    suspend fun sessionByBooking(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
    ): WalkSessionDto?

    @GET("tracking/walk-sessions/{id}/points")
    suspend fun sessionPoints(
        @retrofit2.http.Path("id") sessionId: String,
        @Header("Authorization") token: String,
    ): List<TrackPointDto>

    @POST("tracking/walk-sessions/{id}/points")
    suspend fun addPoint(
        @retrofit2.http.Path("id") sessionId: String,
        @Header("Authorization") token: String,
        @Body body: TrackPointInDto,
    ): TrackPointDto

    @POST("tracking/walk-sessions/{id}/finish")
    suspend fun finishSession(
        @retrofit2.http.Path("id") sessionId: String,
        @Header("Authorization") token: String,
    ): WalkSessionDto

    @POST("review/reviews/")
    suspend fun createReview(@Header("Authorization") token: String, @Body body: ReviewCreateDto): ReviewDto

    @GET("review/reviews/me")
    suspend fun myReviews(@Header("Authorization") token: String): List<ReviewDto>

    @POST("payment/payments/intents")
    suspend fun paymentIntent(@Header("Authorization") token: String, @Body body: PaymentIntentCreateDto): PaymentDto

    @POST("payment/payments/{id}/confirm")
    suspend fun confirmPayment(
        @retrofit2.http.Path("id") paymentId: String,
        @Header("Authorization") token: String,
    ): PaymentDto

    @GET("payment/payments/me")
    suspend fun myPayments(@Header("Authorization") token: String): List<PaymentDto>

    @GET("notification/notifications/")
    suspend fun notifications(@Header("Authorization") token: String): List<NotificationDto>
}

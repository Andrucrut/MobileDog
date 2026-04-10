package com.example.dogapp.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.Response

interface ApiService {
    @GET("health")
    suspend fun health(): Map<String, Any>
    @POST("account/api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    @POST("account/api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): TokenResponseDto

    @POST("account/api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): TokenResponseDto

    @GET("account/api/v1/user/me")
    suspend fun me(@Header("Authorization") token: String): UserDto

    @PATCH("account/api/v1/user/me")
    suspend fun updateMe(
        @Header("Authorization") token: String,
        @Body body: UserUpdateDto,
    ): UserDto

    @GET("booking/api/v1/dogs/")
    suspend fun dogs(@Header("Authorization") token: String): List<DogDto>

    @POST("booking/api/v1/dogs/")
    suspend fun createDog(@Header("Authorization") token: String, @Body body: DogCreateDto): DogDto

    @PATCH("booking/api/v1/dogs/{id}")
    suspend fun updateDog(
        @retrofit2.http.Path("id") dogId: String,
        @Header("Authorization") token: String,
        @Body body: DogUpdateDto,
    ): DogDto

    @GET("booking/api/v1/walkers/search")
    suspend fun searchWalkers(
        @Header("Authorization") token: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius_km") radiusKm: Double? = null,
        @Query("min_rating") minRating: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
    ): List<WalkerDto>

    @GET("booking/api/v1/walkers/me")
    suspend fun myWalkerProfile(@Header("Authorization") token: String): WalkerDto

    @GET("booking/api/v1/walkers/{id}")
    suspend fun walkerById(
        @retrofit2.http.Path("id") walkerId: String,
        @Header("Authorization") token: String,
    ): WalkerDto

    @POST("booking/api/v1/walkers/me")
    suspend fun createMyWalkerProfile(
        @Header("Authorization") token: String,
        @Body body: WalkerCreateMeDto,
    ): WalkerDto

    @POST("booking/api/v1/bookings/")
    suspend fun createBooking(@Header("Authorization") token: String, @Body body: BookingCreateDto): BookingDto

    @GET("booking/api/v1/bookings/me/owner")
    suspend fun ownerBookings(@Header("Authorization") token: String): List<BookingDto>

    @GET("booking/api/v1/bookings/me/walker")
    suspend fun walkerBookings(@Header("Authorization") token: String): List<BookingDto>

    @GET("booking/api/v1/bookings/open")
    suspend fun openBookings(@Header("Authorization") token: String): List<BookingDto>

    @POST("booking/api/v1/bookings/{id}/accept")
    suspend fun acceptBooking(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
    ): BookingDto

    @PATCH("booking/api/v1/bookings/{id}/status")
    suspend fun updateBookingStatus(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
        @Body body: BookingStatusUpdateDto,
    ): BookingDto

    @POST("booking/api/v1/bookings/{id}/owner-settle")
    suspend fun ownerSettleBooking(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
    ): BookingDto

    @GET("payment/api/v1/wallets/me")
    suspend fun myWallet(@Header("Authorization") token: String): WalletDto

    @POST("payment/api/v1/wallets/topup")
    suspend fun topUpWallet(
        @Header("Authorization") token: String,
        @Body body: WalletTopUpDto,
    ): WalletDto

    @POST("payment/api/v1/withdrawals/")
    suspend fun createWithdrawal(
        @Header("Authorization") token: String,
        @Body body: WithdrawalCreateDto,
    ): WithdrawalDto

    @GET("payment/api/v1/withdrawals/me")
    suspend fun myWithdrawals(@Header("Authorization") token: String): List<WithdrawalDto>

    @POST("tracking/api/v1/walk-sessions/start")
    suspend fun startSession(@Header("Authorization") token: String, @Body body: WalkSessionStartDto): WalkSessionDto

    /** Тело может быть JSON null, пока сессии нет — только через Response, иначе падает адаптер Retrofit. */
    @GET("tracking/api/v1/walk-sessions/by-booking/{id}")
    suspend fun sessionByBooking(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
    ): Response<WalkSessionDto>

    @GET("tracking/api/v1/walk-sessions/{id}/points")
    suspend fun sessionPoints(
        @retrofit2.http.Path("id") sessionId: String,
        @Header("Authorization") token: String,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 200,
    ): TrackPointPageDto

    @GET("tracking/api/v1/walk-sessions/{id}/route")
    suspend fun sessionRoute(
        @retrofit2.http.Path("id") sessionId: String,
        @Header("Authorization") token: String,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 500,
    ): Response<WalkRouteResponseDto>

    @GET("tracking/api/v1/walk-sessions/by-booking/{id}/route")
    suspend fun sessionRouteByBooking(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 500,
    ): Response<WalkRouteResponseDto>

    @POST("tracking/api/v1/walk-sessions/{id}/points")
    suspend fun addPoint(
        @retrofit2.http.Path("id") sessionId: String,
        @Header("Authorization") token: String,
        @Body body: TrackPointInDto,
    ): TrackPointDto

    @POST("tracking/api/v1/walk-sessions/{id}/finish")
    suspend fun finishSession(
        @retrofit2.http.Path("id") sessionId: String,
        @Header("Authorization") token: String,
    ): WalkSessionDto

    @POST("review/api/v1/reviews/")
    suspend fun createReview(@Header("Authorization") token: String, @Body body: ReviewCreateDto): ReviewDto

    @GET("review/api/v1/reviews/me")
    suspend fun myReviews(@Header("Authorization") token: String): List<ReviewDto>

    @GET("review/api/v1/reviews/walkers/{id}")
    suspend fun walkerReviews(
        @retrofit2.http.Path("id") walkerProfileId: String,
        @Header("Authorization") token: String,
    ): List<WalkerReviewDto>

    @POST("payment/api/v1/payments/intents")
    suspend fun paymentIntent(@Header("Authorization") token: String, @Body body: PaymentIntentCreateDto): PaymentDto

    @POST("payment/api/v1/payments/{id}/confirm")
    suspend fun confirmPayment(
        @retrofit2.http.Path("id") paymentId: String,
        @Header("Authorization") token: String,
    ): PaymentDto

    @GET("payment/api/v1/payments/me")
    suspend fun myPayments(@Header("Authorization") token: String): List<PaymentDto>

    @GET("notification/api/v1/notifications/")
    suspend fun notifications(@Header("Authorization") token: String): List<NotificationDto>

    @GET("booking/api/v1/bookings/{id}/applications/")
    suspend fun bookingApplications(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
    ): List<BookingApplicationDto>

    @POST("booking/api/v1/bookings/{id}/applications/")
    suspend fun createBookingApplication(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
    ): BookingApplicationDto

    @POST("booking/api/v1/bookings/{id}/applications/choose")
    suspend fun chooseBookingApplication(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
        @Body body: ChooseApplicationBodyDto,
    ): BookingApplicationDto

    @POST("booking/api/v1/bookings/{bookingId}/applications/{applicationId}/reject")
    suspend fun rejectBookingApplication(
        @retrofit2.http.Path("bookingId") bookingId: String,
        @retrofit2.http.Path("applicationId") applicationId: String,
        @Header("Authorization") token: String,
    ): BookingApplicationDto

    @POST("booking/api/v1/bookings/{id}/applications/withdraw")
    suspend fun withdrawBookingApplication(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
    ): BookingApplicationDto

    @GET("booking/api/v1/conversations/me/summary")
    suspend fun conversations(
        @Header("Authorization") token: String,
    ): List<ConversationSummaryDto>

    @GET("booking/api/v1/conversations/by-booking/{id}")
    suspend fun conversationByBooking(
        @retrofit2.http.Path("id") bookingId: String,
        @Header("Authorization") token: String,
    ): ConversationDto?

    @GET("booking/api/v1/conversations/{id}/messages")
    suspend fun conversationMessages(
        @retrofit2.http.Path("id") conversationId: String,
        @Header("Authorization") token: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 30,
    ): ChatMessagesPageDto

    @POST("booking/api/v1/conversations/{id}/messages")
    suspend fun sendConversationMessage(
        @retrofit2.http.Path("id") conversationId: String,
        @Header("Authorization") token: String,
        @Body body: ChatMessageCreateDto,
    ): ChatMessageDto

    @POST("booking/api/v1/conversations/{id}/read")
    suspend fun markConversationMessagesReadSlash(
        @retrofit2.http.Path("id") conversationId: String,
        @Header("Authorization") token: String,
    ): Map<String, Any>?
}

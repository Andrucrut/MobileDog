package com.example.dogapp.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogapp.data.api.*
import com.example.dogapp.data.repository.AppRepository
import com.example.dogapp.presentation.screens.UserRoleUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.cos

data class MainState(
    val startupChecked: Boolean = false,
    val serverReady: Boolean = false,
    val authorized: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val user: UserDto? = null,
    val dogs: List<DogDto> = emptyList(),
    val walkers: List<WalkerDto> = emptyList(),
    val ownerBookings: List<BookingDto> = emptyList(),
    val walkerBookings: List<BookingDto> = emptyList(),
    val notifications: List<NotificationDto> = emptyList(),
    val reviews: List<ReviewDto> = emptyList(),
    val payments: List<PaymentDto> = emptyList(),
    val wallet: WalletDto? = null,
    val withdrawals: List<WithdrawalDto> = emptyList(),
    /** После успешной оплаты заказа — показать модалку отзыва для этого booking id */
    val reviewPromptBookingId: String? = null,
    val activeSession: WalkSessionDto? = null,
    val activeWalkBookingId: String? = null,
    val trackPoints: List<TrackPointDto> = emptyList(),
    val routeByBooking: Map<String, WalkRouteResponseDto?> = emptyMap(),
    val applicationsByBooking: Map<String, List<BookingApplicationDto>> = emptyMap(),
    val walkerProfileById: Map<String, WalkerDto> = emptyMap(),
    val walkerReviewsById: Map<String, List<WalkerReviewDto>> = emptyMap(),
    val conversations: List<ConversationDto> = emptyList(),
    val chatMessagesByConversation: Map<String, List<ChatMessageDto>> = emptyMap(),
    val chatCursorByConversation: Map<String, String?> = emptyMap(),
    val chatHasMoreByConversation: Map<String, Boolean> = emptyMap(),
    val darkThemeEnabled: Boolean = false,
    val compactUiEnabled: Boolean = false,
    val hideNotificationsPreview: Boolean = false,
    /** Локальные URI фото питомцев (dogId → file://…), не с сервера. */
    val dogLocalPhotos: Map<String, String> = emptyMap(),
)

class MainViewModel(private val repository: AppRepository) : ViewModel() {
    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    /** Последовательная отправка тестовых точек с джойстика, без глобального loading. */
    private val simulatedPointMutex = Mutex()

    init {
        viewModelScope.launch { startupCheck() }
    }

    fun retryStartup() = viewModelScope.launch { startupCheck() }

    private suspend fun startupCheck() {
        _state.value = _state.value.copy(loading = true, error = null, startupChecked = false, serverReady = false)
        runCatching { repository.pingGateway() }
            .onFailure {
                _state.value = _state.value.copy(
                    loading = false,
                    error = it.message ?: "Не удалось подключиться к серверу",
                    startupChecked = true,
                    serverReady = false,
                    authorized = false,
                )
                return
            }

        val auth = repository.isAuthorized()
        _state.value = _state.value.copy(
            loading = false,
            startupChecked = true,
            serverReady = true,
            authorized = auth,
            darkThemeEnabled = repository.darkThemeEnabled(),
            compactUiEnabled = repository.compactUiEnabled(),
            hideNotificationsPreview = repository.hideNotificationsPreviewEnabled(),
        )
        if (auth) loadAll()
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        runCatchingLoading {
            repository.login(email, password)
            _state.value = _state.value.copy(authorized = true)
            loadAll()
        }
    }

    fun register(firstName: String, lastName: String, email: String, password: String, role: UserRoleUi) = viewModelScope.launch {
        runCatchingLoading {
            repository.register(firstName, lastName, email, password, if (role == UserRoleUi.WALKER) "walker" else "owner")
            _state.value = _state.value.copy(authorized = true)
            loadAll()
        }
    }

    fun logout() = viewModelScope.launch {
        repository.logout()
        _state.value = MainState(authorized = false)
    }

    fun loadAll() = viewModelScope.launch {
        runCatchingLoading {
            val me = repository.me()
            if (me.role?.key.equals("walker", ignoreCase = true)) {
                runCatching { repository.ensureWalkerProfileExistsForCurrentUser() }
            }
            val dogs = repository.dogs()
            val walkers = repository.walkers(59.9343, 30.3351)
            val ownerBookings = repository.ownerBookingsWithCoordinates()
            val walkerBookings = if (me.role?.key.equals("walker", ignoreCase = true)) {
                val open = repository.openBookingsWithCoordinates()
                val mine = repository.walkerBookings().map { repository.enrichBookingCoordinates(it) }
                (open + mine).distinctBy { it.id }
            } else {
                repository.walkerBookings()
            }
            val notifications = repository.notifications()
            val reviews = repository.reviews()
            val payments = repository.payments()
            val wallet = runCatching { repository.myWallet() }.getOrNull()
            val withdrawals = if (me.role?.key.equals("walker", ignoreCase = true)) {
                runCatching { repository.myWithdrawals() }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
            val conversations = runCatching { repository.conversations() }.getOrDefault(emptyList())
            val walkerAppsPrefetch =
                if (me.role?.key.equals("walker", ignoreCase = true)) {
                    val pendingIds = walkerBookings
                        .filter { it.status.equals("PENDING", ignoreCase = true) }
                        .map { it.id }
                        .distinct()
                    prefetchWalkerOwnApplicationsForPendingBookings(pendingIds)
                } else {
                    emptyMap()
                }
            _state.value = _state.value.copy(
                user = me,
                dogs = dogs,
                walkers = walkers,
                ownerBookings = ownerBookings,
                walkerBookings = walkerBookings,
                notifications = notifications,
                reviews = reviews,
                payments = payments,
                wallet = wallet,
                withdrawals = withdrawals,
                dogLocalPhotos = repository.dogPhotoUriMap(),
                conversations = conversations,
                applicationsByBooking = if (walkerAppsPrefetch.isEmpty()) {
                    _state.value.applicationsByBooking
                } else {
                    _state.value.applicationsByBooking + walkerAppsPrefetch
                },
            )
        }
    }

    /** Обновление списков заказов без сброса error/notice (после завершения прогулки). */
    private suspend fun refreshBookingsAndWalletKeepingFeedback() {
        val keepError = _state.value.error
        val keepNotice = _state.value.notice
        runCatching {
            val me = repository.me()
            if (me.role?.key.equals("walker", ignoreCase = true)) {
                runCatching { repository.ensureWalkerProfileExistsForCurrentUser() }
            }
            val ownerBookings = repository.ownerBookingsWithCoordinates()
            val walkerBookings = if (me.role?.key.equals("walker", ignoreCase = true)) {
                val open = repository.openBookingsWithCoordinates()
                val mine = repository.walkerBookings().map { repository.enrichBookingCoordinates(it) }
                (open + mine).distinctBy { it.id }
            } else {
                repository.walkerBookings()
            }
            val wallet = runCatching { repository.myWallet() }.getOrNull()
            _state.value = _state.value.copy(
                user = me,
                ownerBookings = ownerBookings,
                walkerBookings = walkerBookings,
                wallet = wallet,
                error = keepError,
                notice = keepNotice,
            )
        }
    }

    private suspend fun prefetchWalkerOwnApplicationsForPendingBookings(
        bookingIds: List<String>,
    ): Map<String, List<BookingApplicationDto>> {
        if (bookingIds.isEmpty()) return emptyMap()
        return supervisorScope {
            bookingIds
                .map { id ->
                    async(Dispatchers.IO) {
                        id to runCatching { repository.bookingApplications(id) }.getOrDefault(emptyList())
                    }
                }
                .awaitAll()
                .toMap()
        }
    }

    fun addDog(
        name: String,
        breed: String?,
        birthDate: String?,
        weightKg: Double?,
        gender: String?,
        isVaccinated: Boolean,
        isSterilized: Boolean,
        isAggressive: Boolean,
        behaviorNotes: String?,
        medicalNotes: String?,
    ) = viewModelScope.launch {
        runCatchingLoading {
            repository.createDog(
                name,
                breed,
                birthDate,
                weightKg,
                gender,
                isVaccinated,
                isSterilized,
                isAggressive,
                behaviorNotes,
                medicalNotes,
            )
            _state.value = _state.value.copy(
                dogs = repository.dogs(),
                dogLocalPhotos = repository.dogPhotoUriMap(),
            )
        }
    }

    fun saveDogLocalPhoto(dogId: String, sourceUri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        repository.saveDogPhotoFromPicker(dogId, sourceUri)
        val map = repository.dogPhotoUriMap()
        withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(dogLocalPhotos = map)
        }
    }

    fun clearDogLocalPhoto(dogId: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.clearDogPhoto(dogId)
        val map = repository.dogPhotoUriMap()
        withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(dogLocalPhotos = map)
        }
    }

    fun createBooking(
        dogId: String,
        durationMinutes: Int,
        addressCountry: String,
        addressCity: String,
        addressStreet: String,
        addressHouse: String?,
        addressApartment: String?,
        meetingLat: Double?,
        meetingLng: Double?,
        desiredPrice: String,
        extraParams: String,
    ) = viewModelScope.launch {
        runCatchingLoading {
            val dt = Instant.now().plusSeconds(3600).atOffset(ZoneOffset.UTC).toString()
            val booking = repository.createBooking(
                dogId = dogId,
                durationMinutes = durationMinutes,
                scheduledAtIso = dt,
                addressCountry = addressCountry,
                addressCity = addressCity,
                addressStreet = addressStreet,
                addressHouse = addressHouse,
                addressApartment = addressApartment,
                meetingLat = meetingLat,
                meetingLng = meetingLng,
                desiredPrice = desiredPrice,
                extraParams = extraParams,
            )
            _state.value = _state.value.copy(ownerBookings = repository.ownerBookingsWithCoordinates())
        }
    }

    suspend fun suggestStreet(country: String, city: String, streetQuery: String): List<NominatimPlaceDto> {
        return repository.suggestStreet(country, city, streetQuery)
    }

    fun updateDog(
        dogId: String,
        name: String?,
        breed: String?,
        birthDate: String?,
        weightKg: Double?,
        gender: String?,
        isVaccinated: Boolean?,
        isSterilized: Boolean?,
        isAggressive: Boolean?,
        behaviorNotes: String?,
        medicalNotes: String?,
    ) = viewModelScope.launch {
        runCatchingLoading {
            repository.updateDog(
                dogId = dogId,
                name = name,
                breed = breed,
                birthDate = birthDate,
                weightKg = weightKg,
                gender = gender,
                isVaccinated = isVaccinated,
                isSterilized = isSterilized,
                isAggressive = isAggressive,
                behaviorNotes = behaviorNotes,
                medicalNotes = medicalNotes,
            )
            _state.value = _state.value.copy(
                dogs = repository.dogs(),
                dogLocalPhotos = repository.dogPhotoUriMap(),
            )
        }
    }

    /** Устаревший /accept на бэкенде отключён; для открытых заявок — отклик, для назначенного выгульщика — PATCH CONFIRMED. */
    fun applyAsWalker(bookingId: String) = viewModelScope.launch {
        runCatchingLoading {
            val b = (_state.value.walkerBookings + _state.value.ownerBookings).firstOrNull { it.id == bookingId }
            if (!b?.walker_id.isNullOrBlank()) {
                repository.updateBookingStatus(bookingId, "CONFIRMED")
            } else {
                repository.createBookingApplication(bookingId, null)
            }
            _state.value = _state.value.copy(
                walkerBookings = (repository.openBookingsWithCoordinates() + repository.walkerBookings().map { repository.enrichBookingCoordinates(it) }).distinctBy { it.id },
                ownerBookings = repository.ownerBookingsWithCoordinates(),
            )
        }
    }

    fun walkerConfirmAssignedBooking(bookingId: String) = viewModelScope.launch {
        runCatchingLoading {
            repository.updateBookingStatus(bookingId, "CONFIRMED")
            _state.value = _state.value.copy(
                walkerBookings = (repository.openBookingsWithCoordinates() + repository.walkerBookings().map { repository.enrichBookingCoordinates(it) }).distinctBy { it.id },
                ownerBookings = repository.ownerBookingsWithCoordinates(),
            )
        }
    }

    fun loadApplications(bookingId: String) = viewModelScope.launch {
        runCatchingLoading {
            val apps = repository.bookingApplications(bookingId)
            _state.value = _state.value.copy(applicationsByBooking = _state.value.applicationsByBooking + (bookingId to apps))
        }
    }

    fun submitApplication(bookingId: String, message: String?) = viewModelScope.launch {
        runCatchingLoading {
            val created = repository.createBookingApplication(bookingId, message)
            val apps = runCatching { repository.bookingApplications(bookingId) }
                .getOrElse {
                    val existing = _state.value.applicationsByBooking[bookingId].orEmpty()
                    (existing + created).distinctBy { it.id }
                }
            _state.value = _state.value.copy(
                applicationsByBooking = _state.value.applicationsByBooking + (bookingId to apps),
                notice = "Отклик отправлен",
            )
            runCatching { _state.value = _state.value.copy(conversations = repository.conversations()) }
        }
    }

    fun withdrawApplication(bookingId: String, applicationId: String) = viewModelScope.launch {
        runCatchingLoading {
            val withdrawn = repository.withdrawBookingApplication(bookingId)
            val apps = runCatching { repository.bookingApplications(bookingId) }
                .getOrElse {
                    val existing = _state.value.applicationsByBooking[bookingId].orEmpty()
                    existing.map { app ->
                        if (app.id == withdrawn.id) withdrawn else app
                    }
                }
            _state.value = _state.value.copy(applicationsByBooking = _state.value.applicationsByBooking + (bookingId to apps))
        }
    }

    fun chooseApplication(
        bookingId: String,
        applicationId: String,
        onSuccessConversationId: (String?) -> Unit = {},
    ) = viewModelScope.launch {
        runCatchingLoading {
            val chosen = repository.chooseBookingApplication(bookingId, applicationId)
            _state.value = _state.value.copy(
                ownerBookings = repository.ownerBookingsWithCoordinates(),
                walkerBookings = (repository.openBookingsWithCoordinates() + repository.walkerBookings().map { repository.enrichBookingCoordinates(it) }).distinctBy { it.id },
                notice = "Заявка принята",
            )
            val apps = runCatching { repository.bookingApplications(bookingId) }.getOrDefault(emptyList())
            val conv = runCatching { repository.conversations() }.getOrDefault(_state.value.conversations)
            _state.value = _state.value.copy(
                applicationsByBooking = _state.value.applicationsByBooking + (bookingId to apps),
                conversations = conv,
                chatCursorByConversation = if (chosen.conversation_id != null) {
                    _state.value.chatCursorByConversation + (chosen.conversation_id to null)
                } else {
                    _state.value.chatCursorByConversation
                },
            )
            onSuccessConversationId(chosen.conversation_id)
        }
    }

    fun rejectApplication(bookingId: String, applicationId: String) = viewModelScope.launch {
        runCatchingLoading {
            val rejected = repository.rejectBookingApplication(bookingId, applicationId)
            val current = _state.value.applicationsByBooking[bookingId].orEmpty()
            val merged = if (current.isEmpty()) {
                listOf(rejected)
            } else {
                current.map { if (it.id == rejected.id) rejected else it }
            }
            _state.value = _state.value.copy(
                applicationsByBooking = _state.value.applicationsByBooking + (bookingId to merged),
                notice = "Заявка отклонена",
                error = null,
            )
        }
    }

    fun loadWalkerProfile(walkerId: String) = viewModelScope.launch {
        runCatchingLoading {
            val profile = repository.walkerById(walkerId)
            val reviews = runCatching { repository.walkerReviews(walkerId) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(
                walkerProfileById = _state.value.walkerProfileById + (walkerId to profile),
                walkerReviewsById = _state.value.walkerReviewsById + (walkerId to reviews),
            )
        }
    }

    fun refreshConversations() = viewModelScope.launch {
        runCatchingLoading {
            _state.value = _state.value.copy(conversations = repository.conversations())
        }
    }

    fun loadChatMessages(conversationId: String, reset: Boolean = false) = viewModelScope.launch {
        runCatchingLoading {
            val cursor = if (reset) null else _state.value.chatCursorByConversation[conversationId]
            val page = repository.conversationMessages(conversationId, cursor = cursor, limit = 30)
            val old = if (reset) emptyList() else _state.value.chatMessagesByConversation[conversationId].orEmpty()
            _state.value = _state.value.copy(
                chatMessagesByConversation = _state.value.chatMessagesByConversation + (conversationId to (old + page.items)),
                chatCursorByConversation = _state.value.chatCursorByConversation + (conversationId to page.next_cursor),
                chatHasMoreByConversation = _state.value.chatHasMoreByConversation + (conversationId to page.has_more),
            )
        }
    }

    fun sendChatMessage(conversationId: String, text: String) = viewModelScope.launch {
        runCatchingLoading {
            val msg = repository.sendConversationMessage(conversationId, text)
            val old = _state.value.chatMessagesByConversation[conversationId].orEmpty()
            _state.value = _state.value.copy(chatMessagesByConversation = _state.value.chatMessagesByConversation + (conversationId to (old + msg)))
            runCatching { _state.value = _state.value.copy(conversations = repository.conversations()) }
        }
    }

    fun markChatRead(conversationId: String) = viewModelScope.launch {
        runCatchingLoading {
            repository.markConversationMessagesRead(conversationId)
            runCatching { _state.value = _state.value.copy(conversations = repository.conversations()) }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    /** Подгрузка маршрутов для списка завершённых прогулок (без глобального loading). */
    fun prefetchCompletedWalkRoutes() = viewModelScope.launch {
        val isWalker = _state.value.user?.role?.key.equals("walker", ignoreCase = true)
        val list = if (isWalker) _state.value.walkerBookings else _state.value.ownerBookings
        for (b in list.filter { it.status.equals("COMPLETED", ignoreCase = true) }) {
            if (_state.value.routeByBooking.containsKey(b.id)) continue
            runCatching {
                val route = repository.routeByBooking(b.id, offset = 0, limit = 500)
                _state.value = _state.value.copy(routeByBooking = _state.value.routeByBooking + (b.id to route))
            }
        }
    }

    fun loadRouteByBooking(bookingId: String, withGlobalLoading: Boolean = true) = viewModelScope.launch {
        suspend fun fetchAndStore() {
            val route = repository.routeByBooking(bookingId, offset = 0, limit = 500)
            _state.value = _state.value.copy(routeByBooking = _state.value.routeByBooking + (bookingId to route))
        }
        if (withGlobalLoading) {
            runCatchingLoading { fetchAndStore() }
        } else {
            runCatching { fetchAndStore() }
                .onFailure { throwable ->
                    if (throwable is HttpException && throwable.code() == 401) {
                        repository.logout()
                        _state.value = MainState(
                            authorized = false,
                            darkThemeEnabled = _state.value.darkThemeEnabled,
                            compactUiEnabled = _state.value.compactUiEnabled,
                            hideNotificationsPreview = _state.value.hideNotificationsPreview,
                            error = humanError(throwable),
                        )
                    } else {
                        _state.value = _state.value.copy(error = humanError(throwable), notice = null)
                    }
                }
        }
    }

    fun startTracking(bookingId: String) = viewModelScope.launch {
        runCatchingLoading {
            val existing = repository.sessionByBooking(bookingId)
            val session = existing ?: repository.startSession(bookingId)
            val points = repository.points(session.id)
            val booking = _state.value.walkerBookings.firstOrNull { it.id == bookingId }
                ?: _state.value.ownerBookings.firstOrNull { it.id == bookingId }
            if (booking?.status?.equals("CONFIRMED", ignoreCase = true) == true) {
                runCatching { repository.updateBookingStatus(bookingId, "IN_PROGRESS") }
            }
            _state.value = _state.value.copy(
                activeSession = session,
                activeWalkBookingId = bookingId,
                trackPoints = points,
            )
            loadAll()
            loadRouteByBooking(bookingId, withGlobalLoading = false)
        }
    }

    fun startTracking() = viewModelScope.launch {
        val booking = _state.value.walkerBookings.firstOrNull { it.status == "CONFIRMED" || it.status == "IN_PROGRESS" }
            ?: return@launch
        startTracking(booking.id)
    }

    fun addTrackPoint(lat: Double, lng: Double) = viewModelScope.launch {
        runCatchingLoading {
            val s = _state.value.activeSession ?: error("Сессия не запущена")
            repository.addPoint(s.id, lat, lng)
            _state.value = _state.value.copy(trackPoints = repository.points(s.id))
            _state.value.activeWalkBookingId?.let { loadRouteByBooking(it) }
        }
    }

    /**
     * Шаг симуляции прогулки: следующая точка от последней в треке (или точки встречи заявки).
     * [unitNorth]/[unitEast] — нормированное направление (север/восток), [speedFactor] 0..1.
     */
    fun simulatedWalkStep(
        bookingId: String,
        unitNorth: Double,
        unitEast: Double,
        speedFactor: Double,
    ) = viewModelScope.launch(Dispatchers.IO) {
        simulatedPointMutex.withLock {
            val s = _state.value.activeSession ?: return@withLock
            if (_state.value.activeWalkBookingId != bookingId) return@withLock
            val booking =
                _state.value.walkerBookings.firstOrNull { it.id == bookingId }
                    ?: _state.value.ownerBookings.firstOrNull { it.id == bookingId }
            val last = _state.value.trackPoints.lastOrNull()
            val baseLat = last?.latitude ?: booking?.meeting_latitude ?: 59.9343
            val baseLng = last?.longitude ?: booking?.meeting_longitude ?: 30.3351
            val len = kotlin.math.hypot(unitNorth, unitEast)
            if (len < 1e-6) return@withLock
            val n = unitNorth / len
            val e = unitEast / len
            val meters = 5.0 * speedFactor.coerceIn(0.2, 1.0)
            val dLat = n * meters / 111_320.0
            val rad = Math.toRadians(baseLat)
            val denom = 111_320.0 * cos(rad)
            val dLng = if (kotlin.math.abs(denom) < 1e-6) 0.0 else e * meters / denom
            val newLat = baseLat + dLat
            val newLng = baseLng + dLng
            runCatching {
                repository.addPoint(s.id, newLat, newLng)
                val pts = repository.points(s.id)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(trackPoints = pts)
                }
            }.onFailure { throwable ->
                withContext(Dispatchers.Main) {
                    if (throwable is HttpException && throwable.code() == 401) {
                        repository.logout()
                        _state.value = MainState(
                            authorized = false,
                            darkThemeEnabled = _state.value.darkThemeEnabled,
                            compactUiEnabled = _state.value.compactUiEnabled,
                            hideNotificationsPreview = _state.value.hideNotificationsPreview,
                            error = humanError(throwable),
                        )
                    } else {
                        _state.value = _state.value.copy(error = humanError(throwable), notice = null)
                    }
                }
            }
        }
    }

    fun addFakePoint() = viewModelScope.launch {
        val baseLat = 59.9343
        val baseLng = 30.3351
        addTrackPoint(baseLat + Math.random() / 100, baseLng + Math.random() / 100)
    }

    fun finishTracking() = viewModelScope.launch {
        runCatchingLoading {
            val s = _state.value.activeSession ?: error("Сессия не запущена")
            repository.finishSession(s.id)
            val finishedBookingId = _state.value.activeWalkBookingId
            if (finishedBookingId != null) {
                pushBookingToAwaitingOwnerPayment(finishedBookingId)
            }
            _state.value = _state.value.copy(activeSession = null, activeWalkBookingId = null, trackPoints = emptyList())
            refreshBookingsAndWalletKeepingFeedback()
            if (finishedBookingId != null) {
                runCatching {
                    val route = repository.routeByBooking(finishedBookingId, offset = 0, limit = 500)
                    _state.value = _state.value.copy(
                        routeByBooking = _state.value.routeByBooking + (finishedBookingId to route),
                    )
                }
            }
        }
    }

    /**
     * Бэкенд: AWAITING_OWNER_PAYMENT только из IN_PROGRESS.
     * Раньше прогулка могла идти в статусе CONFIRMED — тогда добиваем IN_PROGRESS и повторяем.
     * Для уже «зависших» заказов — кнопка выгульщика на экране заказа.
     */
    fun walkerPushPaymentToOwner(bookingId: String) = viewModelScope.launch {
        runCatchingLoading {
            pushBookingToAwaitingOwnerPayment(bookingId)
            refreshBookingsAndWalletKeepingFeedback()
        }
    }

    private suspend fun pushBookingToAwaitingOwnerPayment(bookingId: String) {
        val st = (_state.value.walkerBookings + _state.value.ownerBookings)
            .firstOrNull { it.id == bookingId }?.status?.uppercase()
        val direct = runCatching { repository.updateBookingStatus(bookingId, "AWAITING_OWNER_PAYMENT") }
        if (direct.isSuccess) return

        if (st == "CONFIRMED") {
            runCatching { repository.updateBookingStatus(bookingId, "IN_PROGRESS") }
        }
        runCatching { repository.updateBookingStatus(bookingId, "AWAITING_OWNER_PAYMENT") }
            .onFailure { t ->
                _state.value = _state.value.copy(error = humanError(t))
            }
    }

    fun ownerSettleBooking(bookingId: String) = viewModelScope.launch {
        runCatchingLoading {
            repository.ownerSettleBooking(bookingId)
            val me = repository.me()
            val ownerBookings = repository.ownerBookingsWithCoordinates()
            val wallet = runCatching { repository.myWallet() }.getOrNull()
            _state.value = _state.value.copy(
                user = me,
                ownerBookings = ownerBookings,
                wallet = wallet,
                reviewPromptBookingId = bookingId,
            )
        }
    }

    fun topUpWallet(amount: Double) = viewModelScope.launch {
        runCatchingLoading {
            val w = repository.topUpWallet(amount)
            _state.value = _state.value.copy(wallet = w)
        }
    }

    fun clearReviewPrompt() {
        _state.value = _state.value.copy(reviewPromptBookingId = null)
    }

    fun refreshWithdrawals() = viewModelScope.launch {
        runCatchingLoading {
            _state.value = _state.value.copy(
                withdrawals = repository.myWithdrawals(),
                wallet = runCatching { repository.myWallet() }.getOrNull(),
            )
        }
    }

    fun requestWithdrawal(amount: Double) = viewModelScope.launch {
        runCatchingLoading {
            repository.createWithdrawal(amount)
            _state.value = _state.value.copy(
                withdrawals = repository.myWithdrawals(),
                wallet = runCatching { repository.myWallet() }.getOrNull(),
            )
        }
    }

    fun reviewBooking(bookingId: String, rating: Int, comment: String) = viewModelScope.launch {
        runCatchingLoading {
            repository.createReview(bookingId, rating, comment)
            _state.value = _state.value.copy(
                reviews = repository.reviews(),
                reviewPromptBookingId =
                    if (_state.value.reviewPromptBookingId == bookingId) null
                    else _state.value.reviewPromptBookingId,
            )
        }
    }

    fun updateProfile(
        telegram: String?,
        firstName: String?,
        lastName: String?,
        middleName: String?,
        gender: String?,
        country: String?,
        city: String?,
    ) = viewModelScope.launch {
        runCatchingLoading {
            val user = repository.updateMe(
                telegram = telegram,
                firstName = firstName,
                lastName = lastName,
                middleName = middleName,
                gender = gender,
                country = country,
                city = city,
            )
            _state.value = _state.value.copy(user = user)
        }
    }

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch {
        repository.setDarkTheme(enabled)
        _state.value = _state.value.copy(darkThemeEnabled = enabled)
    }

    fun setCompactUi(enabled: Boolean) = viewModelScope.launch {
        repository.setCompactUi(enabled)
        _state.value = _state.value.copy(compactUiEnabled = enabled)
    }

    fun setHideNotificationsPreview(enabled: Boolean) = viewModelScope.launch {
        repository.setHideNotificationsPreview(enabled)
        _state.value = _state.value.copy(hideNotificationsPreview = enabled)
    }

    private suspend fun runCatchingLoading(block: suspend () -> Unit) {
        _state.value = _state.value.copy(loading = true, error = null, notice = null)
        runCatching { block() }
            .onFailure { throwable ->
                if (throwable is HttpException && throwable.code() == 401) {
                    repository.logout()
                    _state.value = MainState(
                        authorized = false,
                        darkThemeEnabled = _state.value.darkThemeEnabled,
                        compactUiEnabled = _state.value.compactUiEnabled,
                        hideNotificationsPreview = _state.value.hideNotificationsPreview,
                        error = humanError(throwable),
                    )
                } else {
                    _state.value = _state.value.copy(error = humanError(throwable), notice = null)
                }
            }
        _state.value = _state.value.copy(loading = false)
    }

    private fun humanError(throwable: Throwable): String {
        if (throwable is HttpException) {
            val body = throwable.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                runCatching {
                    val detail = JSONObject(body).opt("detail")
                    if (detail is String && detail.isNotBlank()) {
                        if (throwable.code() == 402 || detail == "insufficient_balance") {
                            return "Недостаточно средств на балансе. Пополните кошелёк и повторите оплату."
                        }
                        return mapApiDetailToRu(detail)
                    }
                }
            }
            if (throwable.code() == 402) {
                return "Недостаточно средств на балансе. Пополните кошелёк и повторите оплату."
            }
            return "Ошибка сервера: ${throwable.code()}"
        }
        return throwable.message ?: "Неизвестная ошибка"
    }

    private fun mapApiDetailToRu(detail: String): String = when (detail) {
        "no_walker_assigned" ->
            "К заказу не назначен выгульщик. Владелец должен выбрать исполнителя среди откликов."
        "invalid_booking_status" ->
            "Сейчас нельзя начать прогулку: заказ должен быть «Подтверждён» или «В процессе». Сначала подтвердите заказ или дождитесь выбора владельца."
        "booking_service_unavailable" ->
            "Сервис заказов временно недоступен (tracking не достучался до booking). Проверьте, что все микросервисы запущены."
        "invalid_transition" ->
            "Такое изменение статуса сейчас недопустимо."
        "deprecated_use_applications_choose" ->
            "Это действие устарело: откликнитесь на заявку или подтвердите назначенный заказ."
        "forbidden" -> "Нет прав на это действие (войдите как назначенный выгульщик)."
        else -> detail
    }
}

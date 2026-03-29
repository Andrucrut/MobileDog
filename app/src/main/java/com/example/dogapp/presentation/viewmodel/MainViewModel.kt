package com.example.dogapp.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogapp.data.api.*
import com.example.dogapp.data.repository.AppRepository
import com.example.dogapp.presentation.screens.UserRoleUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.time.Instant
import java.time.ZoneOffset

data class MainState(
    val startupChecked: Boolean = false,
    val serverReady: Boolean = false,
    val authorized: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val user: UserDto? = null,
    val dogs: List<DogDto> = emptyList(),
    val walkers: List<WalkerDto> = emptyList(),
    val ownerBookings: List<BookingDto> = emptyList(),
    val walkerBookings: List<BookingDto> = emptyList(),
    val notifications: List<NotificationDto> = emptyList(),
    val reviews: List<ReviewDto> = emptyList(),
    val payments: List<PaymentDto> = emptyList(),
    val activeSession: WalkSessionDto? = null,
    val trackPoints: List<TrackPointDto> = emptyList(),
    val darkThemeEnabled: Boolean = false,
    val compactUiEnabled: Boolean = false,
    val hideNotificationsPreview: Boolean = false,
    /** Локальные URI фото питомцев (dogId → file://…), не с сервера. */
    val dogLocalPhotos: Map<String, String> = emptyMap(),
)

class MainViewModel(private val repository: AppRepository) : ViewModel() {
    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

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
            val dogs = repository.dogs()
            val walkers = repository.walkers(59.9343, 30.3351)
            val ownerBookings = repository.ownerBookingsWithCoordinates()
            val walkerBookings = repository.walkerBookings()
            val notifications = repository.notifications()
            val reviews = repository.reviews()
            val payments = repository.payments()
            _state.value = _state.value.copy(
                user = me,
                dogs = dogs,
                walkers = walkers,
                ownerBookings = ownerBookings,
                walkerBookings = walkerBookings,
                notifications = notifications,
                reviews = reviews,
                payments = payments,
                dogLocalPhotos = repository.dogPhotoUriMap(),
            )
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
            _state.value = _state.value.copy(dogs = repository.dogs())
        }
    }

    fun startTracking() = viewModelScope.launch {
        runCatchingLoading {
            val booking = _state.value.walkerBookings.firstOrNull { it.status == "CONFIRMED" || it.status == "IN_PROGRESS" }
                ?: error("Нет подтвержденной прогулки для старта")
            val session = repository.startSession(booking.id)
            _state.value = _state.value.copy(activeSession = session, trackPoints = repository.points(session.id))
        }
    }

    fun addFakePoint() = viewModelScope.launch {
        runCatchingLoading {
            val s = _state.value.activeSession ?: error("Сессия не запущена")
            val baseLat = 59.9343
            val baseLng = 30.3351
            repository.addPoint(s.id, baseLat + Math.random() / 100, baseLng + Math.random() / 100)
            _state.value = _state.value.copy(trackPoints = repository.points(s.id))
        }
    }

    fun finishTracking() = viewModelScope.launch {
        runCatchingLoading {
            val s = _state.value.activeSession ?: error("Сессия не запущена")
            repository.finishSession(s.id)
            _state.value = _state.value.copy(activeSession = null, trackPoints = emptyList())
            loadAll()
        }
    }

    fun confirmAndPay(bookingId: String) = viewModelScope.launch {
        runCatchingLoading {
            repository.createAndConfirmPayment(bookingId)
            _state.value = _state.value.copy(payments = repository.payments())
        }
    }

    fun reviewBooking(bookingId: String, rating: Int, comment: String) = viewModelScope.launch {
        runCatchingLoading {
            repository.createReview(bookingId, rating, comment)
            _state.value = _state.value.copy(reviews = repository.reviews())
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
        _state.value = _state.value.copy(loading = true, error = null)
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
                    _state.value = _state.value.copy(error = humanError(throwable))
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
                    if (detail is String && detail.isNotBlank()) return detail
                }
            }
            return "Ошибка сервера: ${throwable.code()}"
        }
        return throwable.message ?: "Неизвестная ошибка"
    }
}

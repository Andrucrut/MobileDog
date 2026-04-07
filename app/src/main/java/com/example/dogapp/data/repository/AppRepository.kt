package com.example.dogapp.data.repository

import android.net.Uri
import com.example.dogapp.data.api.*
import com.example.dogapp.data.local.DogPhotoStorage
import retrofit2.HttpException
import com.example.dogapp.data.local.SettingsStore
import com.example.dogapp.data.local.TokenStore
import kotlinx.coroutines.flow.first
import java.util.LinkedHashMap

class AppRepository(
    private val api: ApiService,
    private val geoApi: GeoApiService,
    private val tokenStore: TokenStore,
    private val settingsStore: SettingsStore,
    private val dogPhotoStorage: DogPhotoStorage,
) {
    fun dogPhotoUriMap(): Map<String, String> = dogPhotoStorage.uriMap()

    fun saveDogPhotoFromPicker(dogId: String, sourceUri: Uri): Uri? =
        dogPhotoStorage.savePhotoFromPicker(dogId, sourceUri)

    fun clearDogPhoto(dogId: String) = dogPhotoStorage.clear(dogId)
    suspend fun pingGateway() {
        api.health()
    }
    private suspend fun bearer(): String {
        val token = tokenStore.accessTokenFlow.first() ?: error("Не авторизован")
        return "Bearer $token"
    }

    private suspend fun refreshTokensOrThrow() {
        val refresh = tokenStore.refreshTokenFlow.first() ?: error("Не авторизован")
        try {
            val tokens = api.refresh(RefreshRequestDto(refresh_token = refresh))
            tokenStore.saveTokens(tokens.access_token, tokens.refresh_token)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                tokenStore.clear()
            }
            throw e
        }
    }

    private suspend fun <T> withAuthRetry(block: suspend (bearer: String) -> T): T {
        val token1 = bearer()
        return try {
            block(token1)
        } catch (e: retrofit2.HttpException) {
            if (e.code() != 401) throw e
            refreshTokensOrThrow()
            try {
                block(bearer())
            } catch (e2: retrofit2.HttpException) {
                if (e2.code() == 401) tokenStore.clear()
                throw e2
            }
        }
    }

    suspend fun login(email: String, password: String) {
        val tokens = api.login(LoginRequestDto(email = email, password = password))
        tokenStore.saveTokens(tokens.access_token, tokens.refresh_token)
    }

    suspend fun register(firstName: String, lastName: String, email: String, password: String, roleKey: String) {
        val tokens = api.register(
            RegisterRequestDto(
                first_name = firstName,
                last_name = lastName,
                email = email,
                password = password,
                role_key = roleKey,
            )
        )
        tokenStore.saveTokens(tokens.access_token, tokens.refresh_token)
    }

    suspend fun isAuthorized(): Boolean = tokenStore.accessTokenFlow.first() != null
    suspend fun logout() = tokenStore.clear()

    suspend fun me() = withAuthRetry { api.me(it) }
    suspend fun updateMe(
        telegram: String?,
        firstName: String?,
        lastName: String?,
        middleName: String?,
        gender: String?,
        country: String?,
        city: String?,
    ) = withAuthRetry { token ->
        api.updateMe(
            token,
        UserUpdateDto(
            telegram = telegram?.takeIf { it.isNotBlank() },
            first_name = firstName?.takeIf { it.isNotBlank() },
            last_name = lastName?.takeIf { it.isNotBlank() },
            middle_name = middleName?.takeIf { it.isNotBlank() },
            gender = gender?.takeIf { it.isNotBlank() },
            country = country?.takeIf { it.isNotBlank() },
            city = city?.takeIf { it.isNotBlank() },
        ),
        )
    }
    suspend fun dogs() = withAuthRetry { api.dogs(it) }
    suspend fun createDog(
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
    ) = withAuthRetry { token ->
        api.createDog(
            token,
        DogCreateDto(
            name = name,
            breed = breed?.takeIf { it.isNotBlank() },
            birth_date = birthDate?.takeIf { it.isNotBlank() },
            weight_kg = weightKg,
            gender = gender?.takeIf { it.isNotBlank() },
            is_vaccinated = isVaccinated,
            is_sterilized = isSterilized,
            is_aggressive = isAggressive,
            behavior_notes = behaviorNotes?.takeIf { it.isNotBlank() },
            medical_notes = medicalNotes?.takeIf { it.isNotBlank() },
        ),
        )
    }

    suspend fun updateDog(
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
    ) = withAuthRetry { token ->
        api.updateDog(
            dogId,
            token,
            DogUpdateDto(
                name = name?.takeIf { it.isNotBlank() },
                breed = breed?.takeIf { it.isNotBlank() },
                birth_date = birthDate?.takeIf { it.isNotBlank() },
                weight_kg = weightKg,
                gender = gender?.takeIf { it.isNotBlank() },
                is_vaccinated = isVaccinated,
                is_sterilized = isSterilized,
                is_aggressive = isAggressive,
                behavior_notes = behaviorNotes?.takeIf { it.isNotBlank() },
                medical_notes = medicalNotes?.takeIf { it.isNotBlank() },
            ),
        )
    }
    suspend fun walkers(lat: Double?, lng: Double?) = withAuthRetry { api.searchWalkers(it, lat = lat, lng = lng, radiusKm = 25.0) }
    suspend fun walkerById(walkerId: String) = withAuthRetry { api.walkerById(walkerId, it) }
    suspend fun walkerReviews(walkerId: String) = withAuthRetry { api.walkerReviews(walkerId, it) }
    suspend fun ensureWalkerProfileExistsForCurrentUser(defaultPricePerHour: Double = 500.0) = withAuthRetry { token ->
        runCatching { api.myWalkerProfile(token) }
            .recoverCatching { err ->
                if (err is retrofit2.HttpException && err.code() == 404) {
                    api.createMyWalkerProfile(
                        token,
                        WalkerCreateMeDto(
                            price_per_hour = defaultPricePerHour,
                            service_radius_km = 5.0,
                        ),
                    )
                } else {
                    throw err
                }
            }
            .getOrThrow()
    }
    suspend fun ownerBookings() = withAuthRetry { api.ownerBookings(it) }
    suspend fun walkerBookings() = withAuthRetry { api.walkerBookings(it) }
    suspend fun openBookings() = withAuthRetry { api.openBookings(it) }
    suspend fun acceptBooking(bookingId: String) = withAuthRetry { api.acceptBooking(bookingId, it) }
    suspend fun updateBookingStatus(bookingId: String, status: String, cancelReason: String? = null) =
        withAuthRetry { api.updateBookingStatus(bookingId, it, BookingStatusUpdateDto(status = status, cancel_reason = cancelReason)) }
    suspend fun notifications() = withAuthRetry { api.notifications(it) }
    suspend fun reviews() = withAuthRetry { api.myReviews(it) }
    suspend fun payments() = withAuthRetry { api.myPayments(it) }
    suspend fun bookingApplications(bookingId: String): List<BookingApplicationDto> = withAuthRetry { token ->
        api.bookingApplications(bookingId, token)
    }
    suspend fun createBookingApplication(bookingId: String, message: String?) =
        withAuthRetry { token ->
            api.createBookingApplication(bookingId, token)
        }
    suspend fun chooseBookingApplication(bookingId: String, applicationId: String) = withAuthRetry { token ->
        api.chooseBookingApplication(bookingId, token, ChooseApplicationBodyDto(application_id = applicationId))
    }
    suspend fun rejectBookingApplication(bookingId: String, applicationId: String) = withAuthRetry { token ->
        api.rejectBookingApplication(bookingId, applicationId, token)
    }
    suspend fun withdrawBookingApplication(bookingId: String) = withAuthRetry { token ->
        api.withdrawBookingApplication(bookingId, token)
    }
    suspend fun conversations(): List<ConversationDto> = withAuthRetry { token ->
        api.conversations(token).map { it.conversation.copy(last_message = it.last_message?.text, unread_count = it.unread_count) }
    }
    suspend fun conversationByBooking(bookingId: String): ConversationDto? = withAuthRetry { token ->
        api.conversationByBooking(bookingId, token)
    }
    suspend fun conversationMessages(conversationId: String, cursor: String? = null, limit: Int = 30) =
        withAuthRetry { token ->
            api.conversationMessages(conversationId, token, cursor = cursor, limit = limit)
        }
    suspend fun sendConversationMessage(conversationId: String, text: String) =
        withAuthRetry { token ->
            api.sendConversationMessage(conversationId, token, ChatMessageCreateDto(body = text))
        }
    suspend fun markConversationMessagesRead(conversationId: String) =
        withAuthRetry { token ->
            api.markConversationMessagesReadSlash(conversationId, token)
        }

    suspend fun createBooking(
        dogId: String,
        durationMinutes: Int,
        scheduledAtIso: String,
        addressCountry: String,
        addressCity: String,
        addressStreet: String,
        addressHouse: String?,
        addressApartment: String?,
        meetingLat: Double?,
        meetingLng: Double?,
        desiredPrice: String,
        extraParams: String,
    ): BookingDto {
        val notes = buildString {
            if (desiredPrice.isNotBlank()) append("Желаемая цена: ").append(desiredPrice)
            if (extraParams.isNotBlank()) {
                if (isNotEmpty()) append("; ")
                append("Параметры: ").append(extraParams)
            }
        }
        return withAuthRetry { token ->
            api.createBooking(
                token,
            BookingCreateDto(
                dog_id = dogId,
                scheduled_at = scheduledAtIso,
                duration_minutes = durationMinutes,
                address_country = addressCountry,
                address_city = addressCity,
                address_street = addressStreet,
                address_house = addressHouse?.takeIf { it.isNotBlank() },
                address_apartment = addressApartment?.takeIf { it.isNotBlank() },
                meeting_latitude = meetingLat,
                meeting_longitude = meetingLng,
                owner_notes = notes,
            )
            )
        }
    }

    suspend fun suggestStreet(country: String, city: String, streetQuery: String): List<NominatimPlaceDto> {
        val canonical = canonicalRuCityMoscowOrSpb(city) ?: return emptyList()
        val word = streetQuery.trim()
        if (word.isEmpty()) return emptyList()
        val viewbox = viewBoxForRuCity(canonical)

        suspend fun searchBiased(q: String): List<NominatimPlaceDto> = runCatching {
            geoApi.search(
                query = q,
                countrycodes = "ru",
                limit = 25,
                viewbox = viewbox,
                bounded = null,
            )
        }.getOrDefault(emptyList())

        suspend fun searchStrictBox(q: String): List<NominatimPlaceDto> = runCatching {
            geoApi.search(
                query = q,
                countrycodes = "ru",
                limit = 25,
                viewbox = viewbox,
                bounded = 1,
            )
        }.getOrDefault(emptyList())

        suspend fun searchWide(q: String): List<NominatimPlaceDto> = runCatching {
            geoApi.search(query = q, countrycodes = "ru", limit = 25)
        }.getOrDefault(emptyList())

        val queries = listOf(
            "$word, $canonical, Россия",
            "улица $word, $canonical, Россия",
            "$word улица, $canonical",
            "переулок $word, $canonical",
            "проспект $word, $canonical",
            "набережная $word, $canonical",
            "шоссе $word, $canonical",
            "бульвар $word, $canonical",
            "$word, $canonical",
        )

        val merged = LinkedHashMap<String, NominatimPlaceDto>()
        fun putAll(batch: List<NominatimPlaceDto>) {
            for (item in batch) {
                merged.putIfAbsent("${item.lat},${item.lon}", item)
            }
        }

        for (q in queries) {
            putAll(searchBiased(q))
            if (merged.size >= 45) break
        }
        if (merged.size < 8) {
            for (q in queries) {
                putAll(searchWide(q))
                if (merged.size >= 45) break
            }
        }
        if (merged.size < 5) {
            for (q in queries) {
                putAll(searchStrictBox(q))
                if (merged.size >= 45) break
            }
        }
        if (merged.isEmpty()) {
            putAll(
                runCatching {
                    geoApi.searchStructured(
                        street = word,
                        city = canonical,
                        country = "Россия",
                        countrycodes = "ru",
                        limit = 20,
                    )
                }.getOrDefault(emptyList()),
            )
        }

        val pool = merged.values.filter { likelyInChosenCity(it, canonical) }
            .ifEmpty { merged.values.toList() }
        val qn = normalizeForMatch(word)
        return filterAndRankStreetMatches(pool, qn).take(12)
    }

    private fun likelyInChosenCity(item: NominatimPlaceDto, canonical: String): Boolean {
        val d = item.display_name.lowercase()
        return when (canonical) {
            "Москва" -> d.contains("москва") || d.contains("москов")
            else -> d.contains("петербург") || d.contains("ленинград") || d.contains("спб")
        }
    }

    private fun nominatimFirstSegment(value: String): String {
        val t = value.trim()
        if (t.isEmpty()) return t
        val i = t.indexOf(',')
        return if (i > 0) t.substring(0, i).trim() else t
    }

    private fun canonicalRuCityMoscowOrSpb(city: String): String? {
        val t = nominatimFirstSegment(city).trim().lowercase().replace(".", "")
        val compact = t.replace("-", "").replace(" ", "")
        return when (compact) {
            "москва", "мск" -> "Москва"
            "санктпетербург", "спб", "питер", "питербург", "ленинград" -> "Санкт-Петербург"
            else -> when (t) {
                "санкт-петербург", "с-пб" -> "Санкт-Петербург"
                else -> null
            }
        }
    }

    private fun viewBoxForRuCity(canonical: String): String = when (canonical) {
        "Москва" -> "37.32,55.49,37.84,55.96"
        else -> "29.42,59.69,30.67,60.09"
    }

    private fun normalizeForMatch(s: String): String =
        s.lowercase()
            .replace('ё', 'е')
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    private fun filterAndRankStreetMatches(items: List<NominatimPlaceDto>, queryNormalized: String): List<NominatimPlaceDto> {
        if (queryNormalized.length < 2) return items
        fun hay(item: NominatimPlaceDto): String {
            val road = item.address?.road.orEmpty()
            return normalizeForMatch("$road ${item.display_name}")
        }
        val matched = items.filter { hay(it).contains(queryNormalized) }
        val base = if (matched.isNotEmpty()) matched else items
        return base.sortedWith(
            compareBy<NominatimPlaceDto> { item ->
                val road = item.address?.road?.let { normalizeForMatch(it) }.orEmpty()
                when {
                    road.startsWith(queryNormalized) -> 0
                    road.contains(queryNormalized) -> 1
                    normalizeForMatch(item.display_name).contains(queryNormalized) -> 2
                    else -> 3
                }
            }.thenBy { it.display_name.length },
        )
    }

    suspend fun ownerBookingsWithCoordinates(): List<BookingDto> {
        val list = ownerBookings()
        return list.map { enrichBookingCoordinates(it) }
    }

    suspend fun openBookingsWithCoordinates(): List<BookingDto> {
        val list = openBookings()
        return list.map { enrichBookingCoordinates(it) }
    }

    suspend fun enrichBookingCoordinates(b: BookingDto): BookingDto {
        if (b.meeting_latitude != null && b.meeting_longitude != null) return b
        val p = geocodeAddress(
            b.address_country,
            b.address_city,
            b.address_street,
            b.address_house,
        ) ?: return b
        return b.copy(meeting_latitude = p.first, meeting_longitude = p.second)
    }

    suspend fun geocodeAddress(
        country: String?,
        city: String?,
        street: String?,
        house: String?,
    ): Pair<Double, Double>? {
        val cityLine = city?.trim()?.takeIf { it.isNotEmpty() }?.let { c ->
            canonicalRuCityMoscowOrSpb(c) ?: c
        }
        val parts = buildList {
            street?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
            house?.trim()?.takeIf { it.isNotEmpty() }?.let { add("д. $it") }
            cityLine?.let { add(it) }
            country?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        }
        if (parts.isEmpty()) return null
        val q = parts.joinToString(", ")
        return runCatching {
            val items = geoApi.search(
                query = q,
                countrycodes = country?.let { countryCodesFor(it) },
                limit = 3,
            )
            val first = items.firstOrNull() ?: return null
            val lat = first.lat.toDoubleOrNull() ?: return null
            val lon = first.lon.toDoubleOrNull() ?: return null
            lat to lon
        }.getOrNull()
    }

    private fun countryCodesFor(country: String): String? {
        val c = country.trim().lowercase()
        return when {
            c.contains("росс") || c == "ru" || c.contains("russia") -> "ru"
            c.contains("украин") || c.contains("ukraine") -> "ua"
            c.contains("беларус") || c.contains("belarus") -> "by"
            c.contains("казах") || c.contains("kazakhstan") -> "kz"
            else -> null
        }
    }

    suspend fun startSession(bookingId: String) = withAuthRetry { api.startSession(it, WalkSessionStartDto(bookingId)) }
    suspend fun sessionByBooking(bookingId: String): WalkSessionDto? = withAuthRetry { token ->
        val response = api.sessionByBooking(bookingId, token)
        if (!response.isSuccessful) throw HttpException(response)
        response.body()
    }
    suspend fun addPoint(sessionId: String, lat: Double, lng: Double) = withAuthRetry { api.addPoint(sessionId, it, TrackPointInDto(lat, lng)) }
    suspend fun points(sessionId: String, pageSize: Int = 200): List<TrackPointDto> = withAuthRetry { token ->
        val out = mutableListOf<TrackPointDto>()
        var offset = 0
        var hasMore = true
        while (hasMore) {
            val page = api.sessionPoints(sessionId, token, offset = offset, limit = pageSize)
            out += page.items
            hasMore = page.has_more
            offset += page.items.size
            if (page.items.isEmpty()) break
        }
        out
    }
    suspend fun routeBySession(sessionId: String, offset: Int = 0, limit: Int = 500) =
        withAuthRetry { token ->
            val response = api.sessionRoute(sessionId, token, offset = offset, limit = limit)
            if (response.isSuccessful) response.body() else throw retrofit2.HttpException(response)
        }
    suspend fun routeByBooking(bookingId: String, offset: Int = 0, limit: Int = 500) =
        withAuthRetry { token ->
            val response = api.sessionRouteByBooking(bookingId, token, offset = offset, limit = limit)
            if (response.isSuccessful) response.body() else throw retrofit2.HttpException(response)
        }
    suspend fun finishSession(sessionId: String) = withAuthRetry { api.finishSession(sessionId, it) }

    suspend fun createReview(bookingId: String, rating: Int, comment: String) = withAuthRetry { api.createReview(it, ReviewCreateDto(bookingId, rating, comment)) }

    suspend fun myWallet() = withAuthRetry { api.myWallet(it) }

    suspend fun topUpWallet(amount: Double) = withAuthRetry { api.topUpWallet(it, WalletTopUpDto(amount)) }

    suspend fun ownerSettleBooking(bookingId: String) = withAuthRetry { api.ownerSettleBooking(bookingId, it) }

    suspend fun myWithdrawals() = withAuthRetry { api.myWithdrawals(it) }

    suspend fun createWithdrawal(amount: Double) = withAuthRetry { api.createWithdrawal(it, WithdrawalCreateDto(amount)) }

    suspend fun darkThemeEnabled() = settingsStore.darkThemeFlow.first()
    suspend fun compactUiEnabled() = settingsStore.compactUiFlow.first()
    suspend fun hideNotificationsPreviewEnabled() = settingsStore.hideNotificationsPreviewFlow.first()
    suspend fun setDarkTheme(enabled: Boolean) = settingsStore.setDarkTheme(enabled)
    suspend fun setCompactUi(enabled: Boolean) = settingsStore.setCompactUi(enabled)
    suspend fun setHideNotificationsPreview(enabled: Boolean) = settingsStore.setHideNotificationsPreview(enabled)
}

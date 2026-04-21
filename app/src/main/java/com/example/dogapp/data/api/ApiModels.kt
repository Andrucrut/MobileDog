package com.example.dogapp.data.api

import com.google.gson.annotations.SerializedName

data class TokenResponseDto(
    val access_token: String,
    val refresh_token: String,
    val token_type: String,
)

data class LoginRequestDto(
    val email: String? = null,
    val phone: String? = null,
    val password: String,
)

data class RefreshRequestDto(
    val refresh_token: String,
)

data class RegisterRequestDto(
    val email: String? = null,
    val phone: String? = null,
    val telegram: String? = null,
    val first_name: String,
    val last_name: String,
    val middle_name: String? = null,
    val birth_date: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val city: String? = null,
    val consent_personal_data: Boolean = true,
    val consent_privacy_policy: Boolean = true,
    val password: String,
    val role_key: String,
)

data class UserDto(
    val id: String,
    val avatar: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val telegram: String? = null,
    val first_name: String,
    val last_name: String,
    val middle_name: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val city: String? = null,
    val role: RoleDto? = null,
)

data class RoleDto(
    val name: String,
    val key: String,
)

data class UserUpdateDto(
    val telegram: String? = null,
    val first_name: String? = null,
    val last_name: String? = null,
    val middle_name: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val city: String? = null,
)

data class DogDto(
    val id: String,
    val name: String,
    val breed: String? = null,
    val birth_date: String? = null,
    val weight_kg: Double? = null,
    val gender: String? = null,
    val is_vaccinated: Boolean = false,
    val is_sterilized: Boolean = false,
    val is_aggressive: Boolean = false,
    val behavior_notes: String? = null,
    val medical_notes: String? = null,
)

data class DogCreateDto(
    val name: String,
    val breed: String? = null,
    val birth_date: String? = null,
    val weight_kg: Double? = null,
    val gender: String? = null,
    val is_vaccinated: Boolean = false,
    val is_sterilized: Boolean = false,
    val is_aggressive: Boolean = false,
    val behavior_notes: String? = null,
    val medical_notes: String? = null,
)

data class DogUpdateDto(
    val name: String? = null,
    val breed: String? = null,
    val birth_date: String? = null,
    val weight_kg: Double? = null,
    val gender: String? = null,
    val is_vaccinated: Boolean? = null,
    val is_sterilized: Boolean? = null,
    val is_aggressive: Boolean? = null,
    val behavior_notes: String? = null,
    val medical_notes: String? = null,
)

data class WalkerDto(
    val id: String,
    val user_id: String,
    val bio: String? = null,
    val experience_years: Int,
    val price_per_hour: Double,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val service_radius_km: Double,
    val is_verified: Boolean,
    val is_available: Boolean,
    val rating: Double,
    val reviews_count: Int,
)

data class WalkerCreateMeDto(
    val bio: String? = null,
    val experience_years: Int? = 0,
    val price_per_hour: Double,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val service_radius_km: Double = 5.0,
)

data class BookingDto(
    val id: String,
    val owner_id: String,
    val walker_id: String? = null,
    val dog_id: String,
    val scheduled_at: String,
    val duration_minutes: Int,
    val price: Double,
    val status: String,
    val address_country: String? = null,
    val address_city: String? = null,
    val address_street: String? = null,
    val address_house: String? = null,
    val address_apartment: String? = null,
    val meeting_latitude: Double? = null,
    val meeting_longitude: Double? = null,
    val owner_notes: String? = null,
)

private val desiredPriceFromNotesRe =
    Regex("""Желаемая\s+цена:\s*([0-9]+(?:[.,][0-9]+)?)""", RegexOption.IGNORE_CASE)

/** Цена в списках и карточке: из API или из текста «Желаемая цена» в заметках, если сервер вернул 0. */
fun BookingDto.displayPriceRub(): Double {
    if (price > 0) return price
    return desiredPriceFromNotesRe.find(owner_notes.orEmpty())?.groupValues?.get(1)
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?: 0.0
}

data class BookingCreateDto(
    val dog_id: String,
    val scheduled_at: String,
    val duration_minutes: Int,
    val address_country: String,
    val address_city: String,
    val address_street: String,
    val address_house: String? = null,
    val address_apartment: String? = null,
    val meeting_latitude: Double? = null,
    val meeting_longitude: Double? = null,
    /** Сумма заказа в рублях; бэкенд booking-сервиса должен сохранять в поле price. */
    val price: Double? = null,
    val owner_notes: String? = null,
)

data class NominatimAddressDto(
    val road: String? = null,
    val house_number: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val municipality: String? = null,
    val city_district: String? = null,
    val suburb: String? = null,
    val neighbourhood: String? = null,
    val quarter: String? = null,
    val state: String? = null,
    val country: String? = null,
)

data class NominatimPlaceDto(
    val display_name: String,
    val lat: String,
    val lon: String,
    val address: NominatimAddressDto? = null,
)

fun NominatimPlaceDto.streetNameForField(): String {
    val road = address?.road?.trim()
    if (!road.isNullOrBlank()) return road
    return display_name.substringBefore(",").trim().ifBlank { display_name }
}

fun NominatimPlaceDto.houseNumberFromSuggestion(): String? =
    address?.house_number?.trim()?.takeIf { it.isNotEmpty() }

fun NominatimPlaceDto.streetLabelForForm(): String = streetNameForField()

fun NominatimPlaceDto.cityLabelForForm(): String {
    return address?.city
        ?: address?.town
        ?: address?.municipality
        ?: address?.village
        ?: display_name
}

fun NominatimPlaceDto.shortStreetSuggestionLabel(): String {
    val a = address
    val house = a?.house_number?.trim()?.takeIf { it.isNotEmpty() }
    fun withHouse(line: String): String =
        if (house != null) "$line, д. $house" else line
    if (a != null) {
        val district = sequenceOf(
            a.city_district,
            a.suburb,
            a.neighbourhood,
            a.quarter,
        )
            .mapNotNull { it?.trim()?.takeIf { s -> s.isNotEmpty() } }
            .firstOrNull()
        val road = a.road?.trim()?.takeIf { it.isNotEmpty() }
        when {
            district != null && road != null -> return withHouse("$district · $road")
            road != null -> return withHouse(road)
            district != null -> return withHouse(district)
        }
    }
    return withHouse(displayNameShortForSuggestion(display_name))
}

private fun displayNameShortForSuggestion(displayName: String): String {
    val noIndex = displayName
        .replace(Regex("\\b\\d{6}\\b"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    val parts = noIndex.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return displayName
    if (parts.size == 1) return parts[0]
    return "${parts[0]} · ${parts[1]}"
}

data class BookingStatusUpdateDto(
    val status: String,
    val cancel_reason: String? = null,
)

data class WalkSessionDto(
    val id: String,
    val booking_id: String,
    val owner_id: String,
    val walker_user_id: String,
    val status: String,
    val started_at: String,
    val ended_at: String? = null,
)

data class WalkSessionStartDto(val booking_id: String)

data class TrackPointDto(
    val id: String,
    val session_id: String,
    val latitude: Double,
    val longitude: Double,
    val recorded_at: String,
)

data class TrackPointPageDto(
    val items: List<TrackPointDto>,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val has_more: Boolean,
)

data class TrackPointInDto(
    val latitude: Double,
    val longitude: Double,
    val accuracy_m: Int? = null,
)

data class RouteSummaryBBoxDto(
    val min_lat: Double,
    val min_lng: Double,
    val max_lat: Double,
    val max_lng: Double,
)

data class WalkRouteSummaryDto(
    val points_count: Int,
    val total_points: Int? = null,
    val returned_points: Int? = null,
    val offset: Int? = null,
    val limit: Int? = null,
    val has_more: Boolean? = null,
    val total_distance_m: Double? = null,
    val duration_seconds: Int? = null,
    val bbox: RouteSummaryBBoxDto? = null,
)

data class WalkRouteResponseDto(
    val points: List<TrackPointDto>,
    val summary: WalkRouteSummaryDto,
)

data class ReviewDto(
    val id: String,
    val booking_id: String,
    val rating: Int,
    val comment: String? = null,
)

data class WalkerReviewDto(
    val id: String,
    val booking_id: String,
    val rating: Int,
    val comment: String? = null,
    val created_at: String? = null,
)

data class ReviewCreateDto(
    val booking_id: String,
    val rating: Int,
    val comment: String? = null,
)

data class PaymentDto(
    val id: String,
    val booking_id: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val payer_owner_id: String? = null,
    val beneficiary_walker_user_id: String? = null,
)

data class PaymentIntentCreateDto(val booking_id: String)

data class NotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val created_at: String,
    val read_at: String? = null,
)

data class BookingApplicationDto(
    val id: String,
    val booking_id: String,
    val walker_id: String? = null,
    val walker_user_id: String? = null,
    val walker_first_name: String? = null,
    val walker_last_name: String? = null,
    val walker_avatar: String? = null,
    val walker_city: String? = null,
    val status: String? = null,
    val message: String? = null,
    val created_at: String? = null,
    val walker_rating: Double? = null,
    val walker_reviews_count: Int? = null,
    val walker_price_per_hour: Double? = null,
    val conversation_id: String? = null,
)

data class BookingApplicationCreateDto(
    val message: String? = null,
)

data class ChooseApplicationBodyDto(
    val application_id: String,
)

data class ConversationDto(
    val id: String,
    val booking_id: String? = null,
    val owner_id: String? = null,
    val walker_user_id: String? = null,
    val last_message: String? = null,
    val unread_count: Int? = null,
    val updated_at: String? = null,
)

data class ConversationSummaryDto(
    val conversation: ConversationDto,
    val last_message: ChatMessageDto? = null,
    val unread_count: Int = 0,
)

data class ChatMessageDto(
    val id: String,
    val conversation_id: String,
    val sender_user_id: String? = null,
    @SerializedName("body")
    val body: String? = null,
    val text: String? = null,
    val created_at: String? = null,
    val read_at: String? = null,
)

data class ChatMessageCreateDto(
    val body: String,
)

data class ChatMessagesPageDto(
    val items: List<ChatMessageDto>,
    val next_cursor: String? = null,
    val has_more: Boolean = false,
)

data class WalletDto(
    val user_id: String,
    val balance: Double,
    val currency: String,
)

data class WalletTopUpDto(
    val amount: Double,
)

data class WithdrawalDto(
    val id: String,
    val user_id: String,
    val amount: Double,
    val status: String,
    val moderator_note: String? = null,
    val created_at: String,
)

data class WithdrawalCreateDto(
    val amount: Double,
)

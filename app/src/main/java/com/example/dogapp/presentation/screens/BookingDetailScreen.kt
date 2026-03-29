package com.example.dogapp.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dogapp.data.api.BookingDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    booking: BookingDto?,
    dogName: String?,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заказ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (booking == null) {
            Text("Заявка не найдена", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Питомец: ${dogName ?: "—"}")
            Text("Статус: ${bookingStatusRu(booking.status)}")
            Text("Когда: ${booking.scheduled_at}")
            Text("Длительность: ${booking.duration_minutes} мин")
            Text("Цена: ${booking.price}")
            Text(
                buildBookingAddressLine(
                    booking.address_country,
                    booking.address_city,
                    booking.address_street,
                    booking.address_house,
                    booking.address_apartment,
                ),
            )
            booking.owner_notes?.takeIf { it.isNotBlank() }?.let { Text("Примечание: $it") }
        }
    }
}

private fun bookingStatusRu(status: String): String = when (status.uppercase()) {
    "PENDING" -> "Ожидает подтверждения"
    "CONFIRMED" -> "Подтверждено"
    "IN_PROGRESS" -> "В процессе"
    "COMPLETED" -> "Завершено"
    "CANCELLED" -> "Отменено"
    else -> status
}

private fun buildBookingAddressLine(
    country: String?,
    city: String?,
    street: String?,
    house: String?,
    apartment: String?,
): String {
    val parts = listOfNotNull(
        country?.takeIf { it.isNotBlank() },
        city?.takeIf { it.isNotBlank() },
        street?.takeIf { it.isNotBlank() },
        house?.takeIf { it.isNotBlank() }?.let { "д. $it" },
        apartment?.takeIf { it.isNotBlank() }?.let { "кв. $it" },
    )
    return if (parts.isEmpty()) "Адрес не указан" else "Адрес: ${parts.joinToString(", ")}"
}

package com.example.dogapp.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dogapp.data.api.BookingApplicationDto
import com.example.dogapp.data.api.BookingDto
import com.example.dogapp.data.api.WalkRouteResponseDto
import com.example.dogapp.ui.theme.PetProfileColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    booking: BookingDto?,
    dogName: String?,
    route: WalkRouteResponseDto?,
    applications: List<BookingApplicationDto>,
    isWalker: Boolean,
    isOwner: Boolean,
    hasConversation: Boolean,
    canOpenWalk: Boolean,
    feedbackText: String?,
    feedbackIsError: Boolean,
    onBack: () -> Unit,
    onRefreshRoute: () -> Unit,
    onOpenWalk: () -> Unit,
    onRefreshApplications: () -> Unit,
    onSubmitApplication: (String?) -> Unit,
    onWithdrawApplication: (String) -> Unit,
    onOpenApplication: (applicationId: String) -> Unit,
    onOpenChat: () -> Unit,
) {
    var applicationMessage by remember { mutableStateOf("") }
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
                .background(PetProfileColors.ScreenBg)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(PetProfileColors.CardTeal, PetProfileColors.CardTealDark),
                        ),
                    )
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(alpha = 0.22f)) {
                            Text(
                                text = bookingStatusRu(booking.status),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (isOwner) {
                            Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(alpha = 0.16f)) {
                                Text(
                                    text = "Роль: владелец",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                    Text(
                        text = dogName ?: "Питомец",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "Заказ №${booking.id.take(8)}",
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BookingDetailRow(Icons.Outlined.CalendarMonth, "Когда", booking.scheduled_at)
                    BookingDetailRow(Icons.Outlined.Schedule, "Длительность", "${booking.duration_minutes} мин")
                    BookingDetailRow(Icons.Outlined.Pets, "Стоимость", "${"%.0f".format(booking.price)} ₽")
                    BookingDetailRow(
                        Icons.Outlined.Place,
                        "Адрес",
                        buildBookingAddressLine(
                            booking.address_country,
                            booking.address_city,
                            booking.address_street,
                            booking.address_house,
                            booking.address_apartment,
                        ),
                        maxLines = 3,
                    )
                    booking.owner_notes?.takeIf { it.isNotBlank() }?.let {
                        Text("Примечание: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (feedbackText != null) {
                Text(
                    text = feedbackText,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = if (feedbackIsError) MaterialTheme.colorScheme.error else PetProfileColors.CardTealDark,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Отклики на заявку", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = onRefreshApplications) { Text("Обновить отклики") }
                    val canSubmitApplication = booking.status.equals("PENDING", ignoreCase = true)
                    if (isWalker && canSubmitApplication) {
                        OutlinedTextField(
                            value = applicationMessage,
                            onValueChange = { applicationMessage = it },
                            label = { Text("Сообщение владельцу") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                        Button(
                            onClick = { onSubmitApplication(applicationMessage) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetProfileColors.CardTeal,
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Отправить отклик") }
                    }
                    if (applications.isEmpty()) {
                        Text(if (isOwner) "Пока нет откликов от выгульщиков" else "Отклик ещё не отображён")
                    } else {
                        applications.forEach { app ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = PetProfileColors.ScreenBg),
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val fullName = listOfNotNull(
                                        app.walker_first_name?.takeIf { it.isNotBlank() },
                                        app.walker_last_name?.takeIf { it.isNotBlank() },
                                    ).joinToString(" ").ifBlank { null }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column {
                                            Text(
                                                text = fullName ?: "Выгульщик",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Text(
                                                text = "Открыть отклик",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        IconButton(onClick = { onOpenApplication(app.id) }) {
                                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Открыть отклик")
                                        }
                                    }
                                    Text("Отклик #${app.id.take(8)}", fontWeight = FontWeight.SemiBold)
                                    app.message?.let { Text(it) }
                                    Text("Статус: ${app.status ?: "—"}")
                                    val meta = buildList {
                                        app.walker_price_per_hour?.let { add("Цена/час: ${"%.0f".format(it)} ₽") }
                                        app.walker_rating?.let { add("Рейтинг: ${"%.1f".format(it)}") }
                                        app.walker_reviews_count?.let { add("Отзывы: $it") }
                                    }
                                    if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                    if (isWalker) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { onWithdrawApplication(app.id) }) { Text("Отозвать") }
                                        }
                                    }
                                    if (isOwner) {
                                        val walkerLabel = when {
                                            !app.walker_city.isNullOrBlank() -> "Город: ${app.walker_city}"
                                            app.walker_user_id != null -> "ID: #${app.walker_user_id.take(8)}"
                                            else -> "Профиль выгульщика"
                                        }
                                        Text(
                                            text = walkerLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (hasConversation) {
                Button(
                    onClick = onOpenChat,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PetProfileColors.CardTealDark,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Chat, contentDescription = null)
                        Text("Открыть чат")
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Route, contentDescription = null, tint = PetProfileColors.CardTeal)
                        Text("Маршрут и аналитика", style = MaterialTheme.typography.titleMedium)
                    }
                    if (route == null) {
                        Text("Маршрут ещё не сформирован")
                    } else {
                        BookingDetailRow(
                            icon = Icons.Outlined.Analytics,
                            title = "Точек всего",
                            value = "${route.summary.total_points ?: route.summary.points_count}",
                        )
                        route.summary.total_distance_m?.let { BookingDetailRow(Icons.Outlined.Route, "Дистанция", "${"%.0f".format(it)} м") }
                        route.summary.duration_seconds?.let { BookingDetailRow(Icons.Outlined.Schedule, "Длительность", "${it / 60} мин") }
                        Text(
                            "Пагинация: offset=${route.summary.offset ?: 0}, limit=${route.summary.limit ?: route.points.size}, has_more=${route.summary.has_more ?: false}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedButton(onClick = onRefreshRoute) { Text("Обновить маршрут") }
                    if (canOpenWalk) {
                        Button(
                            onClick = onOpenWalk,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetProfileColors.CardTeal,
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Открыть экран прогулки") }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BookingDetailRow(
    icon: ImageVector,
    title: String,
    value: String,
    maxLines: Int = 1,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = PetProfileColors.CardTeal)
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
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

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dogapp.data.api.BookingApplicationDto
import com.example.dogapp.data.api.BookingDto
import com.example.dogapp.presentation.viewmodel.MainState
import com.example.dogapp.ui.theme.PetProfileColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BookingScreen(
    state: MainState,
    onRefresh: () -> Unit,
    onReview: (String, Int, String) -> Unit,
    onAcceptAsWalker: (String) -> Unit,
    onOpenBooking: (String) -> Unit,
    onOpenWalk: (String) -> Unit,
) {
    val isWalker = state.user?.role?.key.equals("walker", ignoreCase = true)
    val bookings = (if (isWalker) state.walkerBookings else state.ownerBookings)
        .filter { !it.status.equals("COMPLETED", ignoreCase = true) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PetProfileColors.ScreenBg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            PetProfileColors.CardTeal,
                            PetProfileColors.CardTealDark,
                        ),
                    ),
                )
                .padding(horizontal = 8.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isWalker) "Заявки на прогулку" else "Мои заявки",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isWalker) "Доступные и активные прогулки" else "Созданные заявки и отклики выгульщиков",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                }
                IconButton(onClick = onRefresh) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Обновить",
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }

        if (bookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🐕",
                        style = MaterialTheme.typography.displayMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Пока нет бронирований",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PetProfileColors.CardTealDark,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isWalker) "Когда владелец создаст заявку, она появится здесь" else "Создайте заявку на карте или в карточке питомца",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(bookings, key = { it.id }) { b ->
                    val dogName = state.dogs.firstOrNull { it.id == b.dog_id }?.name ?: "Питомец"
                    var reviewText by remember(b.id) { mutableStateOf("Отличная прогулка") }
                    val apps = state.applicationsByBooking[b.id].orEmpty()
                    val hasWalkerActiveApplication =
                        isWalker && walkerHasActiveOwnApplication(apps, state.user?.id)
                    BookingCard(
                        booking = b,
                        isWalker = isWalker,
                        hasWalkerActiveApplication = hasWalkerActiveApplication,
                        dogName = dogName,
                        reviewText = reviewText,
                        onReviewTextChange = { reviewText = it },
                        onReview = { onReview(b.id, 5, reviewText) },
                        onAcceptAsWalker = { onAcceptAsWalker(b.id) },
                        onOpenBooking = { onOpenBooking(b.id) },
                        onOpenWalk = { onOpenWalk(b.id) },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: BookingDto,
    isWalker: Boolean,
    hasWalkerActiveApplication: Boolean,
    dogName: String,
    reviewText: String,
    onReviewTextChange: (String) -> Unit,
    onReview: () -> Unit,
    onAcceptAsWalker: () -> Unit,
    onOpenBooking: () -> Unit,
    onOpenWalk: () -> Unit,
) {
    val statusUi = bookingStatusUi(booking.status)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusUi.container,
                    ) {
                        Text(
                            text = statusUi.labelRu,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = statusUi.onContainer,
                        )
                    }
                    if (isWalker && booking.status.equals("PENDING", ignoreCase = true) && hasWalkerActiveApplication) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE0F2F1),
                        ) {
                            Text(
                                text = "Вы откликнулись",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PetProfileColors.CardTealDark,
                            )
                        }
                    }
                }
                Text(
                    text = "%.0f ₽".format(booking.price),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PetProfileColors.CardTealDark,
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Pets,
                    contentDescription = null,
                    tint = PetProfileColors.CardTeal,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = dogName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(10.dp))

            BookingMetaRow(
                icon = Icons.Outlined.CalendarMonth,
                text = formatScheduledRu(booking.scheduled_at),
            )
            Spacer(Modifier.height(6.dp))
            BookingMetaRow(
                icon = Icons.Outlined.Schedule,
                text = "Длительность: ${booking.duration_minutes} мин",
            )
            Spacer(Modifier.height(6.dp))
            BookingMetaRow(
                icon = Icons.Outlined.Place,
                text = formatAddress(booking),
                maxLines = 3,
            )

            Spacer(Modifier.height(16.dp))

            Spacer(Modifier.height(12.dp))
            if (isWalker) {
                when {
                    booking.status.equals("PENDING", ignoreCase = true) && hasWalkerActiveApplication -> {
                        OutlinedButton(
                            onClick = onOpenBooking,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PetProfileColors.CardTeal),
                        ) { Text("Детали") }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onOpenBooking,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PetProfileColors.CardTeal),
                            ) { Text("Детали") }
                            if (booking.status.equals("PENDING", true)) {
                                Button(
                                    onClick = onOpenBooking,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PetProfileColors.CardTeal,
                                        contentColor = Color.White,
                                    ),
                                ) { Text("Откликнуться") }
                            } else {
                                Button(
                                    onClick = onOpenWalk,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PetProfileColors.CardTeal,
                                        contentColor = Color.White,
                                    ),
                                ) { Text("Прогулка") }
                            }
                        }
                    }
                }
            } else {
                if (booking.status.equals("AWAITING_OWNER_PAYMENT", ignoreCase = true)) {
                    Text(
                        text = "Ожидает оплаты на главном экране — завершите оплату, чтобы продолжить.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onOpenBooking,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PetProfileColors.CardTeal),
                    ) { Text("Детали заказа") }
                } else if (booking.status.equals("COMPLETED", ignoreCase = true)) {
                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = onReviewTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Текст отзыва") },
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenBooking,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PetProfileColors.CardTeal),
                        ) { Text("Детали") }
                        Button(
                            onClick = onReview,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetProfileColors.CardTeal,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("Отзыв", fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenBooking,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PetProfileColors.CardTeal),
                    ) { Text("Детали") }
                }
            }
        }
    }
}

@Composable
private fun BookingMetaRow(
    icon: ImageVector,
    text: String,
    maxLines: Int = 2,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PetProfileColors.CardTeal.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class StatusUi(
    val labelRu: String,
    val container: Color,
    val onContainer: Color,
)

private fun bookingStatusUi(status: String): StatusUi {
    return when (status.uppercase()) {
        "PENDING" -> StatusUi(
            labelRu = "Ожидает",
            container = Color(0xFFFFF3E0),
            onContainer = Color(0xFFE65100),
        )
        "CONFIRMED" -> StatusUi(
            labelRu = "Подтверждено",
            container = Color(0xFFE0F2F1),
            onContainer = PetProfileColors.CardTealDark,
        )
        "IN_PROGRESS" -> StatusUi(
            labelRu = "Идёт прогулка",
            container = Color(0xFFE3F2FD),
            onContainer = Color(0xFF1565C0),
        )
        "AWAITING_OWNER_PAYMENT" -> StatusUi(
            labelRu = "Ожидает оплаты",
            container = Color(0xFFFFF8E1),
            onContainer = Color(0xFFF57F17),
        )
        "COMPLETED" -> StatusUi(
            labelRu = "Завершено",
            container = Color(0xFFE8F5E9),
            onContainer = Color(0xFF2E7D32),
        )
        "CANCELLED" -> StatusUi(
            labelRu = "Отменено",
            container = Color(0xFFFFEBEE),
            onContainer = Color(0xFFC62828),
        )
        else -> StatusUi(
            labelRu = status,
            container = PetProfileColors.HeroGradientTop,
            onContainer = PetProfileColors.CardTealDark,
        )
    }
}

private fun formatScheduledRu(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val z = instant.atZone(ZoneId.systemDefault())
        val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru-RU"))
        z.format(fmt)
    } catch (_: Exception) {
        iso
    }
}

private fun walkerHasActiveOwnApplication(
    applications: List<BookingApplicationDto>,
    userId: String?,
): Boolean {
    if (userId == null) return false
    return applications.any { app ->
        app.walker_user_id == userId &&
            app.status?.uppercase() in setOf("PENDING", "ACCEPTED")
    }
}

private fun formatAddress(b: BookingDto): String {
    val parts = buildList {
        b.address_street?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        b.address_house?.trim()?.takeIf { it.isNotEmpty() }?.let { add("д. $it") }
        b.address_apartment?.trim()?.takeIf { it.isNotEmpty() }?.let { add("кв. $it") }
        b.address_city?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        b.address_country?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
    }
    return parts.joinToString(", ").ifBlank { "Адрес уточняется" }
}

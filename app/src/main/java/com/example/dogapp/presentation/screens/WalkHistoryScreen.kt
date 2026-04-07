package com.example.dogapp.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dogapp.data.api.BookingDto
import com.example.dogapp.data.api.WalkRouteResponseDto
import com.example.dogapp.presentation.viewmodel.MainState
import com.example.dogapp.ui.theme.PetProfileColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WalkHistoryScreen(
    state: MainState,
    onRefresh: () -> Unit,
    onOpenBooking: (String) -> Unit,
    onPrefetchRoutes: () -> Unit,
) {
    val isWalker = state.user?.role?.key.equals("walker", ignoreCase = true)
    val all = if (isWalker) state.walkerBookings else state.ownerBookings
    val completed = all
        .filter { it.status.equals("COMPLETED", ignoreCase = true) }
        .sortedByDescending { b -> parseScheduledInstant(b.scheduled_at) }

    LaunchedEffect(Unit) {
        onPrefetchRoutes()
    }

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
                        text = "История прогулок",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Завершённые заказы: цена, маршрут, адрес",
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

        if (completed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🐾", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Пока нет завершённых прогулок",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PetProfileColors.CardTealDark,
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
                items(completed, key = { it.id }) { b ->
                    val route = state.routeByBooking[b.id]
                    val dogName = state.dogs.firstOrNull { it.id == b.dog_id }?.name ?: "Питомец"
                    HistoryWalkCard(
                        booking = b,
                        route = route,
                        dogName = dogName,
                        onClick = { onOpenBooking(b.id) },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryWalkCard(
    booking: BookingDto,
    route: WalkRouteResponseDto?,
    dogName: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE8F5E9),
                    ) {
                        Text(
                            text = "Завершено",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32),
                        )
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

            HistoryMetaRow(
                icon = Icons.Outlined.CalendarMonth,
                text = formatHistoryDateTime(booking.scheduled_at),
            )
            Spacer(Modifier.height(6.dp))
            HistoryMetaRow(
                icon = Icons.Outlined.Schedule,
                text = historyDurationLine(booking.duration_minutes, route),
            )
            Spacer(Modifier.height(6.dp))
            HistoryMetaRow(
                icon = Icons.Outlined.Place,
                text = formatHistoryAddress(booking),
                maxLines = 3,
            )
            val distText = formatHistoryDistance(route)
            if (distText != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = distText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Подробнее о заказе →",
                style = MaterialTheme.typography.labelLarge,
                color = PetProfileColors.CardTeal,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HistoryMetaRow(
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

private fun parseScheduledInstant(iso: String): Instant =
    runCatching { Instant.parse(iso) }.getOrElse { Instant.EPOCH }

private fun formatHistoryDateTime(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val z = instant.atZone(ZoneId.systemDefault())
        val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru-RU"))
        z.format(fmt)
    } catch (_: Exception) {
        iso
    }
}

private fun formatHistoryAddress(b: BookingDto): String {
    val parts = buildList {
        b.address_street?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        b.address_house?.trim()?.takeIf { it.isNotEmpty() }?.let { add("д. $it") }
        b.address_apartment?.trim()?.takeIf { it.isNotEmpty() }?.let { add("кв. $it") }
        b.address_city?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        b.address_country?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
    }
    return parts.joinToString(", ").ifBlank { "Адрес уточняется" }
}

private fun historyDurationLine(plannedMinutes: Int, route: WalkRouteResponseDto?): String {
    val planned = "План: $plannedMinutes мин"
    val sec = route?.summary?.duration_seconds ?: return planned
    val actualMin = (sec + 59) / 60
    return "$planned · по треку ~ $actualMin мин"
}

private fun formatHistoryDistance(route: WalkRouteResponseDto?): String? {
    val m = route?.summary?.total_distance_m ?: return null
    return if (m >= 1000) {
        "Расстояние: %.2f км".format(m / 1000.0)
    } else {
        "Расстояние: %.0f м".format(m)
    }
}

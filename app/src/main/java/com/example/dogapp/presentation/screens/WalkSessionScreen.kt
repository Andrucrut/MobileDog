package com.example.dogapp.presentation.screens

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dogapp.data.api.BookingDto
import com.example.dogapp.data.api.TrackPointDto
import com.example.dogapp.data.api.WalkRouteResponseDto
import com.example.dogapp.ui.theme.PetProfileColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WalkSessionScreen(
    booking: BookingDto?,
    route: WalkRouteResponseDto?,
    trackPoints: List<TrackPointDto>,
    activeForBooking: Boolean,
    loading: Boolean,
    onBack: () -> Unit,
    onStartOrResume: () -> Unit,
    onAddPoint: (Double, Double) -> Unit,
    onAddFakePoint: () -> Unit,
    onFinish: () -> Unit,
    onRefreshRoute: () -> Unit,
) {
    if (booking == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Назад") }
            Text("Заявка не найдена")
        }
        return
    }
    val context = LocalContext.current
    val locationError = remember { mutableStateOf<String?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            requestSingleLocation(context,
                onSuccess = { lat, lng -> onAddPoint(lat, lng) },
                onError = { msg -> locationError.value = msg },
            )
        } else {
            locationError.value = "Нет разрешения на геолокацию"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PetProfileColors.ScreenBg)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(PetProfileColors.CardTeal, PetProfileColors.CardTealDark)))
                .padding(horizontal = 8.dp, vertical = 14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.18f)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Прогулка", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "Статус: ${booking.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
                IconButton(onClick = onRefreshRoute) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.18f)) {
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

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Дата прогулки: ${formatScheduledRu(booking.scheduled_at)}")
                    Text("Длительность: ${booking.duration_minutes} мин")
                    Text("Точек в сессии: ${trackPoints.size}")
                    route?.summary?.total_distance_m?.let { Text("Дистанция: ${"%.0f".format(it)} м") }
                    route?.summary?.duration_seconds?.let { Text("Время в пути: ${it / 60} мин") }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStartOrResume,
                    enabled = !loading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PetProfileColors.CardTeal, contentColor = Color.White),
                ) { Text(if (activeForBooking) "Сессия активна" else "Начать/продолжить") }
                OutlinedButton(
                    onClick = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    enabled = !loading && activeForBooking,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PetProfileColors.CardTeal),
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("GPS точка")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onAddFakePoint,
                    enabled = !loading && activeForBooking,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PetProfileColors.CardTeal),
                ) { Text("Тестовая точка") }
                Button(
                    onClick = onFinish,
                    enabled = !loading && activeForBooking,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PetProfileColors.CardTealDark, contentColor = Color.White),
                ) { Text("Завершить прогулку") }
            }
            locationError.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            route?.let { r ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Анализ маршрута", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Всего точек: ${r.summary.total_points ?: r.summary.points_count}")
                        Text("Загружено точек: ${r.summary.returned_points ?: r.points.size}")
                        Text("Пагинация: offset=${r.summary.offset ?: 0}, limit=${r.summary.limit ?: r.points.size}, has_more=${r.summary.has_more ?: false}")
                        r.summary.bbox?.let {
                            Text("BBox: ${it.min_lat}, ${it.min_lng} .. ${it.max_lat}, ${it.max_lng}")
                        }
                    }
                }
            }
        }
    }
}

private fun requestSingleLocation(
    context: Context,
    onSuccess: (Double, Double) -> Unit,
    onError: (String) -> Unit,
) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return onError("LocationManager недоступен")
    val gps = runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
    val net = runCatching { manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
    val best = listOfNotNull(gps, net).maxByOrNull { it.time }
    if (best != null) {
        onSuccess(best.latitude, best.longitude)
        return
    }
    onError("Не удалось получить текущую геопозицию")
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

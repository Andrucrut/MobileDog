package com.example.dogapp.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dogapp.data.api.BookingDto
import com.example.dogapp.data.api.WalkRouteResponseDto
import com.example.dogapp.data.api.WalletDto
import com.example.dogapp.ui.theme.PetProfileColors

/**
 * Подтверждение прогулки владельцем и оплата.
 * @param embeddedInCard true — белая карточка на экране заказа; false — текст на градиенте (модалка).
 */
@Composable
fun OwnerWalkSettleForm(
    booking: BookingDto,
    dogName: String,
    wallet: WalletDto?,
    route: WalkRouteResponseDto?,
    loading: Boolean,
    errorText: String?,
    onTopUp: (Double) -> Unit,
    onSettle: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshRoute: () -> Unit,
    embeddedInCard: Boolean,
    modifier: Modifier = Modifier,
) {
    val topUpAmount = remember { mutableStateOf("500") }
    var walkAcknowledged by rememberSaveable(booking.id) { mutableStateOf(false) }

    val titleColor = if (embeddedInCard) MaterialTheme.colorScheme.onSurface else Color.White
    val bodyColor = if (embeddedInCard) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.92f)
    val secondaryLineColor = if (embeddedInCard) MaterialTheme.colorScheme.onSurface else Color.White

    Column(
        modifier = modifier
            .then(
                if (embeddedInCard) {
                    Modifier
                } else {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(PetProfileColors.CardTeal, PetProfileColors.CardTealDark),
                            ),
                            shape = RoundedCornerShape(22.dp),
                        )
                },
            )
            .padding(if (embeddedInCard) 0.dp else 20.dp)
            .then(if (embeddedInCard) Modifier else Modifier.verticalScroll(rememberScrollState())),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Прогулка завершена",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
        Text(
            text = "Сначала нажмите «Подтвердить», если всё прошло хорошо. Ниже — маршрут. Затем оплатите заказ.",
            style = MaterialTheme.typography.bodyMedium,
            color = bodyColor,
        )
        Text(
            text = "$dogName · ${"%.0f".format(booking.price)} ₽",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = secondaryLineColor,
        )

        if (!walkAcknowledged) {
            Button(
                onClick = { walkAcknowledged = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = if (embeddedInCard) {
                    ButtonDefaults.buttonColors(
                        containerColor = PetProfileColors.CardTeal,
                        contentColor = Color.White,
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = PetProfileColors.CardTealDark,
                    )
                },
            ) {
                Text("Подтвердить: заказ выполнен, всё хорошо", fontWeight = FontWeight.SemiBold)
            }
        }

        WalkRouteMapPreview(
            route = route,
            meetingLatitude = booking.meeting_latitude,
            meetingLongitude = booking.meeting_longitude,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )

        OutlinedButton(
            onClick = onRefreshRoute,
            shape = RoundedCornerShape(12.dp),
            colors = if (embeddedInCard) {
                ButtonDefaults.outlinedButtonColors(contentColor = PetProfileColors.CardTealDark)
            } else {
                ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            },
        ) {
            Text("Обновить маршрут")
        }

        WalkStatsBlock(route = route, embeddedInCard = embeddedInCard)

        AnimatedVisibility(visible = walkAcknowledged) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalDivider(
                    color = if (embeddedInCard) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    else Color.White.copy(alpha = 0.35f),
                )
                Text(
                    text = "Оплата",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                )
                Text(
                    text = "Баланс: ${"%.0f".format(wallet?.balance ?: 0.0)} ${wallet?.currency ?: "RUB"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (embeddedInCard) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.9f),
                )
                errorText?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = if (embeddedInCard) MaterialTheme.colorScheme.error else Color(0xFFFFE082),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedTextField(
                    value = topUpAmount.value,
                    onValueChange = { topUpAmount.value = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Сумма пополнения") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedButton(
                    onClick = {
                        val v = topUpAmount.value.replace(',', '.').toDoubleOrNull() ?: return@OutlinedButton
                        if (v > 0) onTopUp(v)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !loading,
                    colors = if (embeddedInCard) {
                        ButtonDefaults.outlinedButtonColors(contentColor = PetProfileColors.CardTealDark)
                    } else {
                        ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    },
                ) {
                    Text("Пополнить баланс")
                }
                Button(
                    onClick = onSettle,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (embeddedInCard) {
                        ButtonDefaults.buttonColors(
                            containerColor = PetProfileColors.CardTealDark,
                            contentColor = Color.White,
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = PetProfileColors.CardTealDark,
                        )
                    },
                    enabled = !loading,
                ) {
                    Text("Оплатить заказ", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    shape = RoundedCornerShape(12.dp),
                    colors = if (embeddedInCard) {
                        ButtonDefaults.outlinedButtonColors(contentColor = PetProfileColors.CardTealDark)
                    } else {
                        ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    },
                ) {
                    Text("Обновить данные")
                }
            }
        }

        if (loading) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(
                color = if (embeddedInCard) PetProfileColors.CardTeal else Color.White,
            )
        }
    }
}

@Composable
private fun WalkStatsBlock(route: WalkRouteResponseDto?, embeddedInCard: Boolean) {
    val mainColor = if (embeddedInCard) MaterialTheme.colorScheme.onSurface else Color.White
    val subtleColor = if (embeddedInCard) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.85f)
    val distKm = route?.summary?.total_distance_m?.let { it / 1000.0 }
    val durationSec = route?.summary?.duration_seconds
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Дистанция: ${formatDistanceKm(distKm)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = mainColor,
        )
        Text(
            text = "Время в движении: ${formatWalkDurationRu(durationSec)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = mainColor,
        )
        route?.summary?.let { s ->
            val totalPts = s.total_points ?: s.points_count
            Text(
                text = "Точек трека: $totalPts",
                style = MaterialTheme.typography.bodySmall,
                color = subtleColor,
            )
        }
    }
}

private fun formatDistanceKm(km: Double?): String {
    if (km == null || km <= 0) return "нет данных"
    return if (km < 0.1) "${"%.0f".format(km * 1000)} м" else "${"%.2f".format(km)} км"
}

private fun formatWalkDurationRu(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return "нет данных"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "$h ч $m мин"
        m > 0 -> "$m мин $s с"
        else -> "$s с"
    }
}

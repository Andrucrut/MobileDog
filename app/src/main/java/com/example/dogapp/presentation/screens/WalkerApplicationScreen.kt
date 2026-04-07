package com.example.dogapp.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dogapp.data.api.BookingApplicationDto
import com.example.dogapp.data.api.WalkerReviewDto
import com.example.dogapp.data.api.WalkerDto
import com.example.dogapp.ui.theme.PetProfileColors
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkerApplicationScreen(
    walker: WalkerDto?,
    application: BookingApplicationDto?,
    reviews: List<WalkerReviewDto>,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль выгульщика") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PetProfileColors.ScreenBg)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(PetProfileColors.CardTeal, PetProfileColors.CardTealDark),
                        ),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .padding(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Column {
                        Text(
                            text = "Кандидат на заявку",
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        Text(
                            text = "ID: ${walker?.id?.take(8) ?: "—"}",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    }
                }
            }

            item {
                Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Данные выгульщика", style = MaterialTheme.typography.titleMedium)
                    Text("Рейтинг: ${walker?.rating?.let { "%.1f".format(it) } ?: "—"}")
                    Text("Отзывов: ${maxOf(walker?.reviews_count ?: 0, reviews.size)}")
                    Text("Опыт: ${walker?.experience_years ?: 0} лет")
                    Text("Цена за час: ${walker?.price_per_hour?.let { "%.0f ₽".format(it) } ?: "—"}")
                    Text("Радиус: ${walker?.service_radius_km?.let { "%.1f км".format(it) } ?: "—"}")
                    if (!walker?.bio.isNullOrBlank()) {
                        Text("О себе: ${walker?.bio}")
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("Статус отклика: ${application?.status ?: "PENDING"}")
                    if (!application?.message.isNullOrBlank()) {
                        Text("Сообщение: ${application?.message}")
                    }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Отзывы", style = MaterialTheme.typography.titleMedium)
                        if (reviews.isEmpty()) {
                            Text("Пока нет отзывов")
                        } else {
                            reviews.take(5).forEach { review ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = PetProfileColors.ScreenBg),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text("Оценка: ${review.rating}/5", fontWeight = FontWeight.SemiBold)
                                        if (!review.comment.isNullOrBlank()) {
                                            Text(review.comment)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Назад")
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Отклонить")
                    }
                }
            }
            item {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PetProfileColors.CardTeal,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Принять заявку")
                }
            }
        }
    }
}

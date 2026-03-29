package com.example.dogapp.presentation.screens

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dogapp.R
import com.example.dogapp.data.api.DogDto
import com.example.dogapp.ui.theme.PetProfileColors

@Composable
fun DogDetailScreen(
    dog: DogDto?,
    localPhotoUri: String?,
    ownerCity: String?,
    ownerCountry: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCreateBooking: () -> Unit,
) {
    if (dog == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PetProfileColors.ScreenBg)
                .padding(24.dp),
        ) {
            TextButton(onClick = onBack) { Text("Назад") }
            Spacer(Modifier.height(24.dp))
            Text(
                "Питомец не найден",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        return
    }

    val favorite = remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val photoUri = localPhotoUri?.let { Uri.parse(it) }
    val (genderLabel, _) = genderRu(dog.gender)
    val ageText = dogAgeLabel(dog.birth_date) ?: "—"
    val birthFormatted = formatBirthDateRu(dog.birth_date)
    val weightText = dog.weight_kg?.let { "%.1f кг".format(it) } ?: "—"
    val locationText = buildLocationLine(ownerCity, ownerCountry) ?: "—"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PetProfileColors.ScreenBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    PetProfileColors.CardTeal,
                                    PetProfileColors.CardTealDark,
                                ),
                            ),
                        ),
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.minDimension * 0.45f,
                        center = Offset(size.width * 0.85f, size.height * 0.2f),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = size.minDimension * 0.35f,
                        center = Offset(size.width * 0.15f, size.height * 0.75f),
                    )
                    val wave = Path().apply {
                        val w = size.width
                        val h = size.height
                        moveTo(0f, h * 0.55f)
                        quadraticTo(w * 0.3f, h * 0.45f, w * 0.55f, h * 0.58f)
                        quadraticTo(w * 0.8f, h * 0.72f, w, h * 0.52f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = wave, color = Color.White.copy(alpha = 0.06f))
                }
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.95f),
                        shadowElevation = 3.dp,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.padding(8.dp),
                            tint = PetProfileColors.CardTealDark,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-56).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier.size(112.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(PetProfileColors.HeroGradientTop),
                        ) {
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.dog_placeholder),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                    Surface(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = 4.dp, y = 4.dp),
                        shape = CircleShape,
                        color = PetProfileColors.CardTeal,
                        shadowElevation = 4.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Изменить фото",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = dog.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dog.breed?.takeIf { it.isNotBlank() } ?: "Порода не указана",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PetInfoTile(
                    icon = { Icon(Icons.Outlined.CalendarToday, null, tint = PetProfileColors.CardTeal) },
                    label = "Возраст",
                    value = ageText,
                )
                PetInfoTile(
                    icon = { Icon(Icons.Outlined.CalendarToday, null, tint = PetProfileColors.CardTeal) },
                    label = "Дата рождения",
                    value = birthFormatted,
                )
                PetInfoTile(
                    icon = { Icon(Icons.Outlined.MonitorWeight, null, tint = PetProfileColors.CardTeal) },
                    label = "Вес",
                    value = weightText,
                )
                PetInfoTile(
                    icon = { Icon(Icons.Outlined.Wc, null, tint = PetProfileColors.CardTeal) },
                    label = "Пол",
                    value = genderLabel,
                )
                PetSwitchTile(
                    icon = { Icon(Icons.Outlined.Vaccines, null, tint = PetProfileColors.CardTeal) },
                    label = "Прививки",
                    checked = dog.is_vaccinated,
                )
                PetSwitchTile(
                    icon = { Icon(Icons.Outlined.MedicalServices, null, tint = PetProfileColors.CardTeal) },
                    label = "Стерилизация",
                    checked = dog.is_sterilized,
                )
                PetSwitchTile(
                    icon = { Icon(Icons.Outlined.Pets, null, tint = PetProfileColors.CardTeal) },
                    label = "Может проявлять агрессию",
                    checked = dog.is_aggressive,
                )
                PetInfoTile(
                    icon = { Icon(Icons.Outlined.LocationOn, null, tint = PetProfileColors.CardTeal) },
                    label = "Регион хозяина",
                    value = locationText,
                )
                PetMultilineTile(
                    icon = { Icon(Icons.Outlined.EditNote, null, tint = PetProfileColors.CardTeal) },
                    label = "Заметки о поведении",
                    text = dog.behavior_notes?.trim().takeUnless { it.isNullOrEmpty() }
                        ?: "Нет описания — добавьте в редактировании профиля.",
                )
                PetMultilineTile(
                    icon = { Icon(Icons.Outlined.LocalHospital, null, tint = PetProfileColors.CardTeal) },
                    label = "Медицинские заметки",
                    text = dog.medical_notes?.trim().takeUnless { it.isNullOrEmpty() }
                        ?: "Нет заметок.",
                )
                Card(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "Редактировать все данные",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PetProfileColors.CardTeal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(120.dp))
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 10.dp,
            color = Color.White,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .size(52.dp)
                        .clickable { favorite.value = !favorite.value },
                    shape = RoundedCornerShape(14.dp),
                    color = PetProfileColors.CardTeal,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (favorite.value) "♥" else "♡",
                            color = Color.White,
                            fontSize = 22.sp,
                        )
                    }
                }
                Button(
                    onClick = onCreateBooking,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PetProfileColors.CardTeal,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        "Создать заявку",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PetInfoTile(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PetProfileColors.HeroGradientTop),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1C1E),
                )
            }
        }
    }
}

@Composable
private fun PetSwitchTile(
    icon: @Composable () -> Unit,
    label: String,
    checked: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PetProfileColors.HeroGradientTop),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = Color(0xFF1A1C1E),
            )
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = false,
                colors = SwitchDefaults.colors(
                    disabledCheckedThumbColor = Color.White,
                    disabledCheckedTrackColor = PetProfileColors.CardTeal,
                    disabledUncheckedThumbColor = Color.White,
                    disabledUncheckedTrackColor = Color.LightGray.copy(alpha = 0.6f),
                    disabledUncheckedBorderColor = Color.Transparent,
                    disabledCheckedBorderColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun PetMultilineTile(
    icon: @Composable () -> Unit,
    label: String,
    text: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PetProfileColors.HeroGradientTop),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1A1C1E),
                lineHeight = 22.sp,
                overflow = TextOverflow.Visible,
            )
        }
    }
}

private fun genderRu(gender: String?): Pair<String, String> {
    val g = gender?.trim()?.lowercase().orEmpty()
    return when {
        g.isEmpty() -> "—" to ""
        g == "male" || g == "m" || g == "мужской" || g == "самец" -> "Мальчик" to "♂"
        g == "female" || g == "f" || g == "женский" || g == "самка" -> "Девочка" to "♀"
        else -> (gender ?: "—") to ""
    }
}

private fun dogAgeLabel(birthDate: String?): String? {
    if (birthDate.isNullOrBlank()) return null
    val year = birthDate.take(4).toIntOrNull() ?: return null
    val cal = java.util.Calendar.getInstance()
    val nowYear = cal.get(java.util.Calendar.YEAR)
    val age = nowYear - year
    if (age < 0) return null
    return when {
        age == 0 -> "Меньше года"
        age == 1 -> "1 год"
        age in 2..4 -> "$age года"
        else -> "$age лет"
    }
}

private fun formatBirthDateRu(birthDate: String?): String {
    if (birthDate.isNullOrBlank()) return "—"
    val p = birthDate.take(10).split("-")
    if (p.size == 3) {
        val (y, m, d) = p
        if (y.length == 4 && m.length == 2 && d.length == 2) return "$d.$m.$y"
    }
    return birthDate
}

private fun buildLocationLine(city: String?, country: String?): String? {
    val parts = buildList {
        city?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        country?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
    }
    if (parts.isEmpty()) return null
    return parts.joinToString(", ")
}

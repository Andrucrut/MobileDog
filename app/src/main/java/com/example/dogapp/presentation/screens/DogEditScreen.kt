package com.example.dogapp.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dogapp.R
import com.example.dogapp.data.api.DogDto
import com.example.dogapp.ui.theme.PetProfileColors

@Composable
fun DogEditScreen(
    dog: DogDto?,
    localPhotoUri: String?,
    onBack: () -> Unit,
    onSave: (String, String?, String?, Double?, String?, Boolean?, Boolean?, Boolean?, String?, String?) -> Unit,
    onPickPhoto: (Uri) -> Unit,
    onClearPhoto: () -> Unit,
) {
    if (dog == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PetProfileColors.ScreenBg)
                .padding(24.dp),
        ) {
            TextButton(onClick = onBack) { Text("Назад") }
            Spacer(Modifier.height(16.dp))
            Text("Питомец не найден", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    val showConfirm = remember { mutableStateOf(false) }
    val name = remember { mutableStateOf(dog.name) }
    val breed = remember { mutableStateOf(dog.breed ?: "") }
    val birthDate = remember { mutableStateOf(dog.birth_date ?: "") }
    val weight = remember { mutableStateOf(dog.weight_kg?.toString() ?: "") }
    val gender = remember { mutableStateOf(dog.gender ?: "") }
    val behavior = remember { mutableStateOf(dog.behavior_notes ?: "") }
    val medical = remember { mutableStateOf(dog.medical_notes ?: "") }
    val vaccinated = remember { mutableStateOf(dog.is_vaccinated) }
    val sterilized = remember { mutableStateOf(dog.is_sterilized) }
    val aggressive = remember { mutableStateOf(dog.is_aggressive) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPickPhoto(uri)
    }

    if (showConfirm.value) {
        AlertDialog(
            onDismissRequest = { showConfirm.value = false },
            title = { Text("Сохранение") },
            text = { Text("Точно сохранить изменения?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm.value = false
                    onSave(
                        name.value,
                        breed.value.takeIf { it.isNotBlank() },
                        birthDate.value.takeIf { it.isNotBlank() },
                        weight.value.toDoubleOrNull(),
                        gender.value.takeIf { it.isNotBlank() },
                        vaccinated.value,
                        sterilized.value,
                        aggressive.value,
                        behavior.value.takeIf { it.isNotBlank() },
                        medical.value.takeIf { it.isNotBlank() },
                    )
                }) { Text("Да") }
            },
            dismissButton = { TextButton(onClick = { showConfirm.value = false }) { Text("Нет") } },
        )
    }

    val scroll = rememberScrollState()
    val parsedPhoto = localPhotoUri?.let { Uri.parse(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PetProfileColors.ScreenBg)
            .verticalScroll(scroll),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(PetProfileColors.CardTeal, PetProfileColors.CardTealDark),
                    ),
                )
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
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
                Text(
                    text = "Редактирование питомца",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.size(48.dp))
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = Color.White,
            shadowElevation = 5.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = "Фото",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PetProfileColors.CardTealDark,
                )
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Surface(
                        modifier = Modifier.size(140.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = PetProfileColors.HeroGradientTop,
                        shadowElevation = 4.dp,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White),
                        ) {
                            if (parsedPhoto != null) {
                                AsyncImage(
                                    model = parsedPhoto,
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
                                        .padding(16.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                    Surface(
                        onClick = { picker.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 6.dp)
                            .size(44.dp),
                        shape = CircleShape,
                        color = PetProfileColors.CardTeal,
                        shadowElevation = 4.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Выбрать фото",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { picker.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PetProfileColors.CardTeal,
                        ),
                    ) {
                        Text("Галерея", fontWeight = FontWeight.Medium)
                    }
                    if (localPhotoUri != null) {
                        OutlinedButton(
                            onClick = onClearPhoto,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Убрать фото")
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PetProfileColors.ScreenBg),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = name.value,
                            onValueChange = { name.value = it },
                            label = { Text("Имя") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        )
                        OutlinedTextField(
                            value = breed.value,
                            onValueChange = { breed.value = it },
                            label = { Text("Порода") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        )
                        OutlinedTextField(
                            value = birthDate.value,
                            onValueChange = { birthDate.value = it },
                            label = { Text("Дата рождения (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        )
                        OutlinedTextField(
                            value = weight.value,
                            onValueChange = { weight.value = it },
                            label = { Text("Вес, кг") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        )
                        Text("Пол", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = gender.value.equals("male", ignoreCase = true),
                                onClick = { gender.value = "male" },
                                label = { Text("Мальчик") },
                            )
                            FilterChip(
                                selected = gender.value.equals("female", ignoreCase = true),
                                onClick = { gender.value = "female" },
                                label = { Text("Девочка") },
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SwitchRow("Вакцинирован", vaccinated.value) { vaccinated.value = it }
                        SwitchRow("Стерилизован", sterilized.value) { sterilized.value = it }
                        SwitchRow("Может агрессировать", aggressive.value) { aggressive.value = it }
                    }
                }

                OutlinedTextField(
                    value = behavior.value,
                    onValueChange = { behavior.value = it },
                    label = { Text("Заметки о поведении") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = medical.value,
                    onValueChange = { medical.value = it },
                    label = { Text("Медицинские заметки") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                )

                Button(
                    onClick = { showConfirm.value = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PetProfileColors.CardTeal,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Сохранить изменения", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PetProfileColors.CardTeal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.6f),
            ),
        )
    }
}

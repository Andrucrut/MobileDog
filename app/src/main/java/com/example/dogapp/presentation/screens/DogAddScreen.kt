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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dogapp.R
import com.example.dogapp.ui.theme.PetProfileColors

@Composable
fun DogAddScreen(
    onBack: () -> Unit,
    onAdd: (String, String?, String?, Double?, String?, Boolean, Boolean, Boolean, String?, String?) -> Unit,
) {
    val name = remember { mutableStateOf("") }
    val breed = remember { mutableStateOf("") }
    val birthDate = remember { mutableStateOf("") }
    val weight = remember { mutableStateOf("") }
    val gender = remember { mutableStateOf("") }
    val behaviorNotes = remember { mutableStateOf("") }
    val medicalNotes = remember { mutableStateOf("") }
    val isVaccinated = remember { mutableStateOf(false) }
    val isSterilized = remember { mutableStateOf(false) }
    val isAggressive = remember { mutableStateOf(false) }
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoUri.value = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PetProfileColors.ScreenBg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Новый питомец", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Добавьте данные и фото", color = Color.White.copy(alpha = 0.9f))
                }
                Spacer(Modifier.size(48.dp))
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(top = 10.dp, bottom = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PetProfileColors.HeroGradientTop,
                        modifier = Modifier.size(130.dp),
                    ) {
                        if (photoUri.value != null) {
                            AsyncImage(model = photoUri.value, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Image(painter = painterResource(R.drawable.dog_placeholder), contentDescription = null, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("Добавить фото") }
                    if (photoUri.value != null) {
                        OutlinedButton(onClick = { photoUri.value = null }, modifier = Modifier.weight(1f)) { Text("Убрать") }
                    }
                }
                OutlinedTextField(value = name.value, onValueChange = { name.value = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = breed.value, onValueChange = { breed.value = it }, label = { Text("Порода") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = birthDate.value, onValueChange = { birthDate.value = it }, label = { Text("Дата рождения YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = weight.value, onValueChange = { weight.value = it }, label = { Text("Вес (кг)") }, modifier = Modifier.fillMaxWidth())
                Text("Пол", style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = gender.value.equals("male", true), onClick = { gender.value = "male" }, label = { Text("Мальчик") })
                    FilterChip(selected = gender.value.equals("female", true), onClick = { gender.value = "female" }, label = { Text("Девочка") })
                }
                OutlinedTextField(value = behaviorNotes.value, onValueChange = { behaviorNotes.value = it }, label = { Text("Поведение") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = medicalNotes.value, onValueChange = { medicalNotes.value = it }, label = { Text("Мед. заметки") }, modifier = Modifier.fillMaxWidth())
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isVaccinated.value, onClick = { isVaccinated.value = !isVaccinated.value }, label = { Text("Вакцинирован") })
                    FilterChip(selected = isSterilized.value, onClick = { isSterilized.value = !isSterilized.value }, label = { Text("Стерилизован") })
                    FilterChip(selected = isAggressive.value, onClick = { isAggressive.value = !isAggressive.value }, label = { Text("Агрессивный") })
                }
                Button(
                    onClick = {
                        onAdd(
                            name.value,
                            breed.value,
                            birthDate.value,
                            weight.value.toDoubleOrNull(),
                            gender.value,
                            isVaccinated.value,
                            isSterilized.value,
                            isAggressive.value,
                            behaviorNotes.value,
                            medicalNotes.value,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PetProfileColors.CardTeal, contentColor = Color.White),
                ) { Text("Сохранить", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}


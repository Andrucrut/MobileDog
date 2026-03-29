package com.example.dogapp.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DogAddScreen(
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Добавление питомца")
        OutlinedTextField(value = name.value, onValueChange = { name.value = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = breed.value, onValueChange = { breed.value = it }, label = { Text("Порода") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = birthDate.value, onValueChange = { birthDate.value = it }, label = { Text("Дата рождения YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = weight.value, onValueChange = { weight.value = it }, label = { Text("Вес (кг)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = gender.value, onValueChange = { gender.value = it }, label = { Text("Пол") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = behaviorNotes.value, onValueChange = { behaviorNotes.value = it }, label = { Text("Поведение") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = medicalNotes.value, onValueChange = { medicalNotes.value = it }, label = { Text("Мед. заметки") }, modifier = Modifier.fillMaxWidth())
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = isVaccinated.value, onCheckedChange = { isVaccinated.value = it })
            Text("Вакцинирован")
        }
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = isSterilized.value, onCheckedChange = { isSterilized.value = it })
            Text("Стерилизован")
        }
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = isAggressive.value, onCheckedChange = { isAggressive.value = it })
            Text("Агрессивный")
        }
        Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Добавить фото") }
        photoUri.value?.let { Text("Фото выбрано") }
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
            modifier = Modifier.fillMaxWidth()
        ) { Text("Сохранить") }
    }
}


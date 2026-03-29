package com.example.dogapp.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dogapp.data.api.DogDto
import com.example.dogapp.data.api.NominatimPlaceDto
import com.example.dogapp.data.api.houseNumberFromSuggestion
import com.example.dogapp.data.api.shortStreetSuggestionLabel
import com.example.dogapp.data.api.streetNameForField
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SUGGEST_DEBOUNCE_MS = 320L
private const val MIN_STREET_QUERY_LEN = 1

private const val CITY_MSK = "Москва"
private const val CITY_SPB = "Санкт-Петербург"

@Composable
fun BookingCreateScreen(
    dog: DogDto?,
    onSuggestStreet: suspend (country: String, city: String, streetQuery: String) -> List<NominatimPlaceDto>,
    onCreate: (
        dogId: String,
        durationMinutes: Int,
        addressCountry: String,
        addressCity: String,
        addressStreet: String,
        addressHouse: String?,
        addressApartment: String?,
        meetingLat: Double?,
        meetingLng: Double?,
        desiredPrice: String,
        extraParams: String,
    ) -> Unit,
) {
    if (dog == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { Text("Питомец не найден") }
        return
    }
    val country = remember { mutableStateOf("Россия") }
    val selectedCity = remember { mutableStateOf<String?>(null) }
    val street = remember { mutableStateOf("") }
    val house = remember { mutableStateOf("") }
    val apartment = remember { mutableStateOf("") }
    val desiredPrice = remember { mutableStateOf("") }
    val duration = remember { mutableStateOf("60") }
    val extra = remember { mutableStateOf("") }
    val meetingLat = remember { mutableStateOf<Double?>(null) }
    val meetingLng = remember { mutableStateOf<Double?>(null) }
    val streetChosenFromSuggestions = remember { mutableStateOf(false) }
    val formError = remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val submitting = remember { mutableStateOf(false) }

    val streetSuggestions = remember { mutableStateOf<List<NominatimPlaceDto>>(emptyList()) }
    val streetSuggestJob = remember { mutableStateOf<Job?>(null) }

    val scroll = rememberScrollState()
    val canSearchStreet = selectedCity.value != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Создание заявки")
        Text("Питомец: ${dog.name}")
        Text("Страна: Россия", style = MaterialTheme.typography.bodyMedium)
        Text("Город", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedCity.value == CITY_SPB,
                onClick = {
                    selectedCity.value = CITY_SPB
                    streetChosenFromSuggestions.value = false
                    meetingLat.value = null
                    meetingLng.value = null
                    streetSuggestions.value = emptyList()
                },
                label = { Text(CITY_SPB) },
            )
            FilterChip(
                selected = selectedCity.value == CITY_MSK,
                onClick = {
                    selectedCity.value = CITY_MSK
                    streetChosenFromSuggestions.value = false
                    meetingLat.value = null
                    meetingLng.value = null
                    streetSuggestions.value = emptyList()
                },
                label = { Text(CITY_MSK) },
            )
        }

        OutlinedTextField(
            value = street.value,
            onValueChange = {
                street.value = it
                streetChosenFromSuggestions.value = false
                meetingLat.value = null
                meetingLng.value = null
                streetSuggestJob.value?.cancel()
                val c = selectedCity.value
                if (c == null) {
                    streetSuggestions.value = emptyList()
                } else {
                    streetSuggestJob.value = scope.launch {
                        delay(SUGGEST_DEBOUNCE_MS)
                        if (it.length < MIN_STREET_QUERY_LEN) {
                            streetSuggestions.value = emptyList()
                            return@launch
                        }
                        val items = runCatching {
                            onSuggestStreet("Россия", c, it)
                        }.getOrDefault(emptyList())
                        streetSuggestions.value = items
                    }
                }
            },
            label = { Text("Улица") },
            supportingText = {
                Text(
                    if (canSearchStreet) {
                        "Введите название и выберите улицу из списка"
                    } else {
                        "Сначала выберите город: Москва или Санкт-Петербург"
                    },
                )
            },
            enabled = canSearchStreet,
            modifier = Modifier.fillMaxWidth(),
        )
        AddressSuggestionList(
            suggestions = if (canSearchStreet) streetSuggestions.value else emptyList(),
            onPick = { item ->
                street.value = item.streetNameForField()
                item.houseNumberFromSuggestion()?.let { house.value = it }
                val la = item.lat.toDoubleOrNull()
                val lo = item.lon.toDoubleOrNull()
                meetingLat.value = la
                meetingLng.value = lo
                streetChosenFromSuggestions.value = true
                streetSuggestions.value = emptyList()
            },
        )

        OutlinedTextField(value = house.value, onValueChange = { house.value = it }, label = { Text("Дом") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = apartment.value, onValueChange = { apartment.value = it }, label = { Text("Квартира") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desiredPrice.value, onValueChange = { desiredPrice.value = it }, label = { Text("Цена") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = duration.value, onValueChange = { duration.value = it }, label = { Text("Длительность (мин)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = extra.value, onValueChange = { extra.value = it }, label = { Text("Параметры") }, modifier = Modifier.fillMaxWidth())

        formError.value?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                if (submitting.value) return@Button
                formError.value = null
                val c = selectedCity.value
                if (c == null) {
                    formError.value = "Выберите город"
                    return@Button
                }
                if (!streetChosenFromSuggestions.value || meetingLat.value == null || meetingLng.value == null) {
                    formError.value = "Выберите улицу из списка"
                    return@Button
                }
                submitting.value = true
                scope.launch {
                    onCreate(
                        dog.id,
                        duration.value.toIntOrNull() ?: 60,
                        country.value,
                        c,
                        street.value,
                        house.value,
                        apartment.value,
                        meetingLat.value,
                        meetingLng.value,
                        desiredPrice.value,
                        extra.value,
                    )
                    submitting.value = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !submitting.value,
        ) {
            Text(if (submitting.value) "Создание..." else "Создать заявку")
        }
    }
}

@Composable
private fun AddressSuggestionList(
    suggestions: List<NominatimPlaceDto>,
    onPick: (NominatimPlaceDto) -> Unit,
) {
    if (suggestions.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            suggestions.forEach { item ->
                TextButton(
                    onClick = { onPick(item) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = item.shortStreetSuggestionLabel(),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

package com.example.dogapp.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StartupScreen(
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("DogApp", style = MaterialTheme.typography.headlineMedium)
        if (loading) {
            CircularProgressIndicator()
            Text("Подключение к серверу…")
            Text(
                "Если сервер был неактивен, его запуск может занять до минуты.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(error ?: "Сервер недоступен")
            Text(
                "Нажмите «Повторить» через несколько секунд.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Повторить") }
        }
    }
}


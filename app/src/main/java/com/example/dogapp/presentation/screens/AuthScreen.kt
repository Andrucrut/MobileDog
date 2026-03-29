package com.example.dogapp.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class UserRoleUi { OWNER, WALKER }
private enum class AuthStep { ROLE, LOGIN, REGISTER }

@Composable
fun AuthScreen(
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String, UserRoleUi) -> Unit,
) {
    val role = remember { mutableStateOf<UserRoleUi?>(null) }
    val step = remember { mutableStateOf(AuthStep.ROLE) }
    val firstName = remember { mutableStateOf("Андрей") }
    val lastName = remember { mutableStateOf("Якунин") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    LaunchedEffect(role.value, step.value) {
        if (step.value != AuthStep.ROLE && role.value == null) {
            step.value = AuthStep.ROLE
        }
    }

    BackHandler(enabled = step.value != AuthStep.ROLE) {
        when (step.value) {
            AuthStep.LOGIN -> {
                step.value = AuthStep.ROLE
                role.value = null
            }
            AuthStep.REGISTER -> {
                step.value = AuthStep.LOGIN
            }
            AuthStep.ROLE -> Unit
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (step.value) {
                AuthStep.ROLE -> {
                    Text("Выберите роль", style = MaterialTheme.typography.headlineSmall)
                    Button(
                        onClick = { role.value = UserRoleUi.OWNER; step.value = AuthStep.LOGIN },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Я владелец собаки") }
                    Button(
                        onClick = { role.value = UserRoleUi.WALKER; step.value = AuthStep.LOGIN },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Я выгульщик") }
                }
                AuthStep.LOGIN -> {
                    Text(
                        if (role.value == UserRoleUi.WALKER) "Вход для выгульщика" else "Вход для владельца",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    OutlinedTextField(
                        value = email.value,
                        onValueChange = { email.value = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password.value,
                        onValueChange = { password.value = it },
                        label = { Text("Пароль") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (role.value != null) {
                                onLogin(email.value.trim(), password.value)
                            }
                        },
                        enabled = !loading && role.value != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Войти") }
                    HorizontalDivider()
                    TextButton(onClick = { step.value = AuthStep.REGISTER }) {
                        Text("Нет аккаунта? Зарегистрироваться")
                    }
                    TextButton(onClick = {
                        step.value = AuthStep.ROLE
                        role.value = null
                    }) {
                        Text("Назад к выбору роли")
                    }
                }
                AuthStep.REGISTER -> {
                    Text(
                        if (role.value == UserRoleUi.WALKER) "Регистрация выгульщика" else "Регистрация владельца",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    OutlinedTextField(
                        value = firstName.value,
                        onValueChange = { firstName.value = it },
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lastName.value,
                        onValueChange = { lastName.value = it },
                        label = { Text("Фамилия") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email.value,
                        onValueChange = { email.value = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password.value,
                        onValueChange = { password.value = it },
                        label = { Text("Пароль") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val r = role.value ?: return@Button
                            onRegister(
                                firstName.value.trim(),
                                lastName.value.trim(),
                                email.value.trim(),
                                password.value,
                                r,
                            )
                        },
                        enabled = !loading && role.value != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Зарегистрироваться") }
                    TextButton(onClick = { step.value = AuthStep.LOGIN }) {
                        Text("Уже есть аккаунт? Войти")
                    }
                    TextButton(onClick = {
                        step.value = AuthStep.ROLE
                        role.value = null
                    }) {
                        Text("Назад к выбору роли")
                    }
                }
            }
            if (loading) CircularProgressIndicator()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

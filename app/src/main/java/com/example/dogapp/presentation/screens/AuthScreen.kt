package com.example.dogapp.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.dogapp.ui.theme.PetProfileColors

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
    val passwordVisible = remember { mutableStateOf(false) }

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
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        PetProfileColors.HeroGradientTop.copy(alpha = 0.6f),
                        PetProfileColors.ScreenBg,
                        Color.White,
                    ),
                ),
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 430.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(PetProfileColors.CardTeal.copy(alpha = 0.16f))
                        .border(1.dp, PetProfileColors.CardTeal.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Pets,
                        contentDescription = null,
                        tint = PetProfileColors.CardTealDark,
                        modifier = Modifier.size(34.dp),
                    )
                }

                Text(
                    text = "DogApp",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PetProfileColors.CardTealDark,
                )

                when (step.value) {
                    AuthStep.ROLE -> {
                        Text("Выберите роль", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = { role.value = UserRoleUi.OWNER; step.value = AuthStep.LOGIN },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetProfileColors.CardTeal,
                                contentColor = Color.White,
                            ),
                        ) { Text("Я владелец собаки") }
                        Button(
                            onClick = { role.value = UserRoleUi.WALKER; step.value = AuthStep.LOGIN },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetProfileColors.CardTealDark,
                                contentColor = Color.White,
                            ),
                        ) { Text("Я выгульщик") }
                    }
                    AuthStep.LOGIN -> {
                        Text(
                            if (role.value == UserRoleUi.WALKER) "Вход для выгульщика" else "Вход для владельца",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = email.value,
                            onValueChange = { email.value = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = password.value,
                            onValueChange = { password.value = it },
                            label = { Text("Пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                                    Icon(
                                        imageVector = if (passwordVisible.value) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible.value) "Скрыть пароль" else "Показать пароль",
                                    )
                                }
                            },
                        )
                        Button(
                            onClick = {
                                if (role.value != null) {
                                    onLogin(email.value.trim(), password.value)
                                }
                            },
                            enabled = !loading && role.value != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetProfileColors.CardTeal,
                                contentColor = Color.White,
                            ),
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = firstName.value,
                            onValueChange = { firstName.value = it },
                            label = { Text("Имя") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = lastName.value,
                            onValueChange = { lastName.value = it },
                            label = { Text("Фамилия") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = email.value,
                            onValueChange = { email.value = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = password.value,
                            onValueChange = { password.value = it },
                            label = { Text("Пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                                    Icon(
                                        imageVector = if (passwordVisible.value) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible.value) "Скрыть пароль" else "Показать пароль",
                                    )
                                }
                            },
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
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetProfileColors.CardTeal,
                                contentColor = Color.White,
                            ),
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
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Загрузка...")
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

package com.example.dogapp.presentation.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dogapp.R
import com.example.dogapp.presentation.viewmodel.MainState
import com.example.dogapp.ui.theme.PetProfileColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: MainState,
    onUpdateProfile: (String?, String?, String?, String?, String?, String?, String?) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onTopUpWallet: (Double) -> Unit = {},
    onOpenWithdrawals: () -> Unit = {},
) {
    val editing = remember { mutableStateOf(false) }
    val showConfirm = remember { mutableStateOf(false) }
    val showTopUp = remember { mutableStateOf(false) }
    val topUpAmount = remember { mutableStateOf("500") }
    val telegram = remember { mutableStateOf(state.user?.telegram ?: "") }
    val firstName = remember { mutableStateOf(state.user?.first_name ?: "") }
    val lastName = remember { mutableStateOf(state.user?.last_name ?: "") }
    val middleName = remember { mutableStateOf(state.user?.middle_name ?: "") }
    val gender = remember { mutableStateOf(state.user?.gender ?: "") }
    val country = remember { mutableStateOf(state.user?.country ?: "") }
    val city = remember { mutableStateOf(state.user?.city ?: "") }

    LaunchedEffect(state.user) {
        val u = state.user ?: return@LaunchedEffect
        telegram.value = u.telegram.orEmpty()
        firstName.value = u.first_name
        lastName.value = u.last_name
        middleName.value = u.middle_name.orEmpty()
        gender.value = u.gender.orEmpty()
        country.value = u.country.orEmpty()
        city.value = u.city.orEmpty()
    }

    if (showTopUp.value) {
        AlertDialog(
            onDismissRequest = { showTopUp.value = false },
            title = { Text("Пополнение баланса") },
            text = {
                OutlinedTextField(
                    value = topUpAmount.value,
                    onValueChange = { topUpAmount.value = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Сумма (₽)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = topUpAmount.value.replace(',', '.').toDoubleOrNull() ?: return@TextButton
                    if (v > 0) {
                        onTopUpWallet(v)
                        showTopUp.value = false
                    }
                }) { Text("Пополнить") }
            },
            dismissButton = {
                TextButton(onClick = { showTopUp.value = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showConfirm.value) {
        AlertDialog(
            onDismissRequest = { showConfirm.value = false },
            title = { Text(stringResource(R.string.profile_save_title)) },
            text = { Text(stringResource(R.string.profile_save_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm.value = false
                    editing.value = false
                    onUpdateProfile(
                        telegram.value,
                        firstName.value,
                        lastName.value,
                        middleName.value,
                        gender.value,
                        country.value,
                        city.value,
                    )
                }) { Text(stringResource(R.string.common_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm.value = false }) { Text(stringResource(R.string.common_no)) }
            },
        )
    }

    val user = state.user
    val scroll = rememberScrollState()
    val initials = remember(user) {
        val a = user?.first_name?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        val b = user?.last_name?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        (a + b).ifBlank { "?" }
    }
    val subtitle = remember(user) {
        when (user?.role?.key?.lowercase()) {
            "walker" -> "Выгульщик"
            "owner" -> "Владелец собаки"
            else -> user?.role?.name?.takeIf { it.isNotBlank() }
        } ?: when (user?.role?.key?.lowercase()) {
                "walker" -> "Выгульщик"
                "owner" -> "Владелец собаки"
                else -> "Участник DogApp"
            }
    }
    val fullName = remember(user) {
        listOfNotNull(user?.first_name, user?.last_name)
            .joinToString(" ")
            .trim()
            .ifBlank { "—" }
    }
    val isWalkerRole = remember(user) { user?.role?.key.equals("walker", ignoreCase = true) }
    /** Для владельца — отзывы, которые он написал; для выгульщика — отзывы о нём (из профиля walker). */
    val profileReviewsCount = remember(isWalkerRole, state.myWalkerProfile, state.reviews) {
        if (isWalkerRole) state.myWalkerProfile?.reviews_count ?: 0 else state.reviews.size
    }
    val profilePaymentsCount = remember(isWalkerRole, state.payments, user?.id) {
        val uid = user?.id
        if (uid.isNullOrBlank()) return@remember state.payments.size
        val hasActorIds = state.payments.any { it.payer_owner_id != null || it.beneficiary_walker_user_id != null }
        if (!hasActorIds) return@remember state.payments.size
        if (isWalkerRole) {
            state.payments.count { it.beneficiary_walker_user_id?.equals(uid, ignoreCase = true) == true }
        } else {
            state.payments.count { it.payer_owner_id?.equals(uid, ignoreCase = true) == true }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PetProfileColors.ScreenBg)
            .verticalScroll(scroll),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PetProfileColors.CardTeal,
                                PetProfileColors.CardTealDark,
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(800f, 400f),
                        ),
                    ),
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.09f),
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.88f, size.height * 0.15f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.07f),
                    radius = size.minDimension * 0.32f,
                    center = Offset(size.width * 0.12f, size.height * 0.82f),
                )
                val wave = Path().apply {
                    val w = size.width
                    val h = size.height
                    moveTo(0f, h * 0.5f)
                    quadraticTo(w * 0.35f, h * 0.42f, w * 0.6f, h * 0.55f)
                    quadraticTo(w * 0.85f, h * 0.68f, w, h * 0.48f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = wave, color = Color.White.copy(alpha = 0.07f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
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
                                contentDescription = stringResource(R.string.profile_back_content_description),
                                modifier = Modifier.padding(8.dp),
                                tint = PetProfileColors.CardTealDark,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.profile_title),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    IconButton(onClick = onOpenSettings) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.95f),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.profile_settings),
                                    tint = PetProfileColors.CardTeal,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(96.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = PetProfileColors.CardTealDark,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
                .offset(y = (-28).dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                if (!editing.value && user == null) {
                    Text(
                        text = stringResource(R.string.profile_data_unavailable),
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (!editing.value && user != null) {
                    ProfileInfoRow(
                        icon = Icons.Outlined.Email,
                        label = stringResource(R.string.profile_label_email),
                        value = user.email?.takeIf { it.isNotBlank() } ?: "—",
                    )
                    ProfileInfoRow(
                        icon = Icons.AutoMirrored.Outlined.Send,
                        label = stringResource(R.string.profile_telegram),
                        value = user.telegram?.takeIf { it.isNotBlank() } ?: "—",
                    )
                    ProfileInfoRow(
                        icon = Icons.Outlined.Badge,
                        label = stringResource(R.string.profile_role),
                        value = when (user.role?.key?.lowercase()) {
                            "walker" -> "Выгульщик"
                            "owner" -> "Владелец собаки"
                            else -> user.role?.name?.takeIf { it.isNotBlank() } ?: "—"
                        },
                    )
                    ProfileInfoRow(
                        icon = Icons.Outlined.Wc,
                        label = stringResource(R.string.profile_gender),
                        value = genderDisplayRu(user.gender),
                    )
                    ProfileInfoRow(
                        icon = Icons.Outlined.Public,
                        label = stringResource(R.string.profile_country),
                        value = user.country?.takeIf { it.isNotBlank() } ?: "—",
                    )
                    ProfileInfoRow(
                        icon = Icons.Outlined.LocationCity,
                        label = stringResource(R.string.profile_city),
                        value = user.city?.takeIf { it.isNotBlank() } ?: "—",
                    )
                    ProfileInfoRow(
                        icon = Icons.Outlined.Notifications,
                        label = stringResource(R.string.profile_notifications),
                        value = stringResource(R.string.profile_count_format, state.notifications.size),
                    )
                    state.wallet?.let { w ->
                        ProfileInfoRow(
                            icon = Icons.Outlined.Payments,
                            label = "Кошелёк",
                            value = "%.0f %s".format(w.balance, w.currency),
                        )
                    }
                    ProfileInfoRow(
                        icon = Icons.Outlined.Payments,
                        label = stringResource(R.string.profile_payments),
                        value = stringResource(R.string.profile_count_format, profilePaymentsCount),
                    )
                    ProfileInfoRow(
                        icon = Icons.Outlined.RateReview,
                        label = stringResource(R.string.profile_reviews),
                        value = stringResource(R.string.profile_count_format, profileReviewsCount),
                        isLast = true,
                    )
                } else if (editing.value) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.profile_edit_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = firstName.value,
                            onValueChange = { firstName.value = it },
                            label = { Text(stringResource(R.string.profile_first_name)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = lastName.value,
                            onValueChange = { lastName.value = it },
                            label = { Text(stringResource(R.string.profile_last_name)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = telegram.value,
                            onValueChange = { telegram.value = it },
                            label = { Text(stringResource(R.string.profile_telegram)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(stringResource(R.string.profile_gender))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = gender.value.equals("male", ignoreCase = true),
                                onClick = { gender.value = "male" },
                                label = { Text(stringResource(R.string.profile_gender_male)) },
                            )
                            FilterChip(
                                selected = gender.value.equals("female", ignoreCase = true),
                                onClick = { gender.value = "female" },
                                label = { Text(stringResource(R.string.profile_gender_female)) },
                            )
                        }
                        OutlinedTextField(
                            value = country.value,
                            onValueChange = { country.value = it },
                            label = { Text(stringResource(R.string.profile_country)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = city.value,
                            onValueChange = { city.value = it },
                            label = { Text(stringResource(R.string.profile_city)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (!editing.value && user != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (isWalkerRole) {
                            OutlinedButton(
                                onClick = onOpenWithdrawals,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) { Text("Вывод средств") }
                        } else {
                            OutlinedButton(
                                onClick = { showTopUp.value = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) { Text("Пополнить баланс") }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (editing.value) {
                        OutlinedButton(
                            onClick = { editing.value = false },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.common_cancel)) }
                        Button(
                            onClick = { showConfirm.value = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetProfileColors.CardTeal,
                                contentColor = Color.White,
                            ),
                        ) { Text(stringResource(R.string.common_save)) }
                    } else {
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.profile_logout)) }
                        Button(
                            onClick = { editing.value = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetProfileColors.CardTeal,
                                contentColor = Color.White,
                            ),
                        ) { Text(stringResource(R.string.profile_update_data)) }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isLast: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PetProfileColors.CardTeal,
                modifier = Modifier.size(26.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1C1B1F),
                )
            }
        }
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 40.dp, top = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )
        }
    }
}

private fun genderDisplayRu(gender: String?): String {
    val g = gender?.trim()?.lowercase().orEmpty()
    if (g.isEmpty()) return "—"
    return when {
        g == "male" || g == "m" || g == "мужской" -> "Мужской"
        g == "female" || g == "f" || g == "женский" -> "Женский"
        else -> gender ?: "—"
    }
}

package com.example.dogapp.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dogapp.data.api.WalletDto
import com.example.dogapp.data.api.WithdrawalDto
import com.example.dogapp.ui.theme.PetProfileColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkerWithdrawalsScreen(
    wallet: WalletDto?,
    withdrawals: List<WithdrawalDto>,
    loading: Boolean,
    feedback: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSubmitWithdrawal: (Double) -> Unit,
) {
    val amount = remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вывод средств") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PetProfileColors.ScreenBg)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Баланс", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${"%.0f".format(wallet?.balance ?: 0.0)} ${wallet?.currency ?: "RUB"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PetProfileColors.CardTealDark,
                    )
                }
            }
            feedback?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(
                value = amount.value,
                onValueChange = { amount.value = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Сумма вывода") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Button(
                onClick = {
                    val v = amount.value.replace(',', '.').toDoubleOrNull() ?: return@Button
                    if (v > 0) onSubmitWithdrawal(v)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PetProfileColors.CardTeal, contentColor = Color.White),
            ) {
                Text("Запросить вывод")
            }
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PetProfileColors.CardTealDark, contentColor = Color.White),
            ) {
                Text("Обновить список")
            }
            Text("История заявок", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(withdrawals, key = { it.id }) { w ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("${"%.0f".format(w.amount)} ₽", fontWeight = FontWeight.SemiBold)
                                Text(statusRu(w.status), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun statusRu(s: String): String = when (s.uppercase()) {
    "PENDING_MODERATION" -> "На модерации"
    "IN_PROGRESS" -> "В процессе"
    "COMPLETED" -> "Выполнено"
    "REJECTED" -> "Отклонено"
    else -> s
}

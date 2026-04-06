package com.example.dogapp.presentation.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dogapp.data.api.ChatMessageDto
import com.example.dogapp.ui.theme.PetProfileColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    currentUserId: String?,
    peerTitle: String,
    messages: List<ChatMessageDto>,
    hasMore: Boolean,
    loading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val orderedMessages = remember(messages) {
        messages.sortedBy { it.created_at ?: "" }
    }
    val listState = rememberLazyListState()
    val initials = remember(peerTitle) {
        peerTitle
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.take(1).uppercase() }
            .ifBlank { "?" }
    }
    Scaffold(
        topBar = {
            Surface(shadowElevation = 6.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    PetProfileColors.CardTeal,
                                    PetProfileColors.CardTealDark,
                                ),
                            ),
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                    ) {
                        Text(
                            text = peerTitle.ifBlank { "Чат" },
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "онлайн",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f),
                        )
                    }
                    Button(
                        onClick = onRefresh,
                        enabled = !loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.22f),
                            contentColor = Color.White,
                        ),
                    ) { Text("Обновить") }
                }
            }
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            PetProfileColors.ScreenBg,
                            Color(0xFFF4FBFA),
                        ),
                    ),
                ),
        ) {
            Spacer(Modifier.height(4.dp))
            if (hasMore) {
                OutlinedButton(
                    onClick = onLoadMore,
                    enabled = !loading,
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) { Text("Загрузить старые") }
            }
            LaunchedEffect(orderedMessages.size) {
                if (orderedMessages.isNotEmpty()) {
                    listState.scrollToItem(orderedMessages.lastIndex)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                        ) {
                            Text(
                                text = "Напишите первое сообщение",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                items(orderedMessages, key = { it.id }) { m ->
                    val mine = currentUserId != null && m.sender_user_id == currentUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 300.dp),
                            shape = if (mine) {
                                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
                            } else {
                                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (mine) PetProfileColors.CardTealDark else Color.White,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    m.text.orEmpty().ifBlank { m.body.orEmpty() },
                                    color = if (mine) Color.White else Color.Black,
                                )
                                Text(
                                    formatChatTime(m.created_at),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (mine) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Сообщение") },
                        shape = RoundedCornerShape(18.dp),
                    )
                }
                Button(
                    onClick = {
                        val trimmed = text.trim()
                        if (trimmed.isNotEmpty()) {
                            onSend(trimmed)
                            text = ""
                        }
                    },
                    enabled = !loading,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = PetProfileColors.CardTealDark,
                        contentColor = Color.White,
                    ),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                ) {
                    Text(
                        text = ">",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

private fun formatChatTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "--:--"
    return try {
        val instant = Instant.parse(iso)
        val zdt = instant.atZone(ZoneId.systemDefault())
        val fmt = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("ru-RU"))
        zdt.format(fmt)
    } catch (_: Exception) {
        iso.take(5)
    }
}

package com.example.dogapp.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.dogapp.presentation.viewmodel.MainState
import com.example.dogapp.ui.theme.PetProfileColors

@Composable
fun BookingsHubScreen(
    state: MainState,
    onRefresh: () -> Unit,
    onReview: (String, Int, String) -> Unit,
    onAcceptAsWalker: (String) -> Unit,
    onOpenBooking: (String) -> Unit,
    onOpenWalk: (String) -> Unit,
    onPrefetchHistoryRoutes: () -> Unit,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PetProfileColors.ScreenBg),
    ) {
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = PetProfileColors.CardTeal,
            contentColor = Color.White,
        ) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = {
                    Text(
                        "Заявки",
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = {
                    Text(
                        "История",
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
            )
        }
        when (tabIndex) {
            0 -> BookingScreen(
                state = state,
                onRefresh = onRefresh,
                onReview = onReview,
                onAcceptAsWalker = onAcceptAsWalker,
                onOpenBooking = onOpenBooking,
                onOpenWalk = onOpenWalk,
            )
            1 -> WalkHistoryScreen(
                state = state,
                onRefresh = onRefresh,
                onOpenBooking = onOpenBooking,
                onPrefetchRoutes = onPrefetchHistoryRoutes,
            )
        }
    }
}

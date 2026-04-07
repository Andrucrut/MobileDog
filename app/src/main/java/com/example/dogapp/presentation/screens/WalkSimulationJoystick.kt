package com.example.dogapp.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.dogapp.ui.theme.PetProfileColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Условный джойстик для тестов: периодически вызывает [onSimulatedTick] с направлением (север/восток).
 * Показывать только в debug-сборке.
 */
@Composable
fun WalkSimulationJoystick(
    enabled: Boolean,
    onSimulatedTick: (unitNorth: Double, unitEast: Double, speedFactor: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val baseRadiusPx = remember(density) { with(density) { 52.dp.toPx() } }
    val knobRadiusPx = remember(density) { with(density) { 22.dp.toPx() } }

    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var knobOffsetY by remember { mutableFloatStateOf(0f) }
    var steerX by remember { mutableFloatStateOf(0f) }
    var steerY by remember { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()
    var tickJob by remember { mutableStateOf<Job?>(null) }

    fun cancelTicks() {
        tickJob?.cancel()
        tickJob = null
    }

    fun startTicksIfNeeded() {
        if (tickJob?.isActive == true) return
        tickJob = scope.launch {
            while (isActive && enabled) {
                delay(420)
                val len = hypot(steerX, steerY)
                if (len < 0.08f) continue
                val nx = steerX / len
                val ny = steerY / len
                val speed = len.coerceIn(0.25f, 1f)
                // Вверх на экране = север (положительный unitNorth)
                val unitNorth = (-ny).toDouble()
                val unitEast = nx.toDouble()
                onSimulatedTick(unitNorth, unitEast, speed.toDouble())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { cancelTicks() }
    }

    Column(modifier = modifier) {
        Text(
            "Тест: движение",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = PetProfileColors.CardTealDark,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Удерживайте и отклоняйте круг — точки уходят на сервер, как при реальной прогулке.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    color = PetProfileColors.CardTeal.copy(alpha = 0.15f),
                    shape = CircleShape,
                )
                .pointerInput(enabled, baseRadiusPx) {
                    detectDragGestures(
                        onDragStart = {
                            if (enabled) startTicksIfNeeded()
                        },
                        onDragEnd = {
                            knobOffsetX = 0f
                            knobOffsetY = 0f
                            steerX = 0f
                            steerY = 0f
                            cancelTicks()
                        },
                        onDragCancel = {
                            knobOffsetX = 0f
                            knobOffsetY = 0f
                            steerX = 0f
                            steerY = 0f
                            cancelTicks()
                        },
                        onDrag = { _, dragAmount ->
                            if (!enabled) return@detectDragGestures
                            val maxR = baseRadiusPx - knobRadiusPx * 0.6f
                            var nx = knobOffsetX + dragAmount.x
                            var ny = knobOffsetY + dragAmount.y
                            val d = hypot(nx, ny)
                            if (d > maxR && d > 0f) {
                                nx *= maxR / d
                                ny *= maxR / d
                            }
                            knobOffsetX = nx
                            knobOffsetY = ny
                            steerX = (nx / maxR).coerceIn(-1f, 1f)
                            steerY = (ny / maxR).coerceIn(-1f, 1f)
                            if (tickJob?.isActive != true) startTicksIfNeeded()
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .offset {
                        IntOffset(
                            knobOffsetX.roundToInt(),
                            knobOffsetY.roundToInt(),
                        )
                    }
                    .background(PetProfileColors.CardTeal, CircleShape),
            )
        }
    }
}

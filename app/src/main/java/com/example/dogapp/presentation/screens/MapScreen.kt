package com.example.dogapp.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.example.dogapp.data.api.BookingDto
import com.example.dogapp.presentation.viewmodel.MainState
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MapScreen(
    state: MainState,
    onStartTracking: () -> Unit,
    onAddFakePoint: () -> Unit,
    onFinishTracking: () -> Unit,
    onOpenBookingDetail: (String) -> Unit,
) {
    val context = LocalContext.current
    val bookingMarkerIcon = remember(context) { dogEmojiMapIcon(context) }
    Configuration.getInstance().userAgentValue = context.packageName

    val clusterBookings = remember { mutableStateOf<List<BookingDto>?>(null) }
    val zoomIn = remember { mutableStateOf(false) }
    val zoomOut = remember { mutableStateOf(false) }

    val bookingsOnMap = state.ownerBookings.filter {
        it.meeting_latitude != null && it.meeting_longitude != null
    }
    val groupedByLocation = bookingsOnMap.groupBy { b ->
        geoGroupKey(b.meeting_latitude!!, b.meeting_longitude!!)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                MapView(it).apply {
                    controller.setZoom(12.0)
                    controller.setCenter(GeoPoint(59.9343, 30.3351))
                    setMultiTouchControls(true)
                }
            },
            update = { map ->
                if (zoomIn.value) {
                    map.controller.zoomIn()
                    zoomIn.value = false
                }
                if (zoomOut.value) {
                    map.controller.zoomOut()
                    zoomOut.value = false
                }
                map.overlays.clear()

                val events = MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        clusterBookings.value = null
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                })
                map.overlays.add(events)

                state.walkers.forEach { walker ->
                    if (walker.latitude != null && walker.longitude != null) {
                        val m = Marker(map)
                        m.position = GeoPoint(walker.latitude, walker.longitude)
                        m.title = "Рейтинг ${walker.rating}, ${walker.price_per_hour} руб/ч"
                        map.overlays.add(m)
                    }
                }
                state.trackPoints.forEach { p ->
                    val m = Marker(map)
                    m.position = GeoPoint(p.latitude, p.longitude)
                    m.title = "Точка маршрута"
                    map.overlays.add(m)
                }
                if (state.trackPoints.isNotEmpty()) {
                    val polyline = Polyline().apply {
                        setPoints(state.trackPoints.map { GeoPoint(it.latitude, it.longitude) })
                        outlinePaint.color = android.graphics.Color.BLUE
                    }
                    map.overlays.add(polyline)
                }

                groupedByLocation.forEach { (_, list) ->
                    if (list.isEmpty()) return@forEach
                    val first = list.first()
                    val lat = first.meeting_latitude!!
                    val lng = first.meeting_longitude!!
                    val marker = Marker(map)
                    marker.position = GeoPoint(lat, lng)
                    marker.icon = bookingMarkerIcon
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    marker.title = if (list.size > 1) "${list.size} заявок" else first.id
                    marker.setOnMarkerClickListener { _, _ ->
                        clusterBookings.value = list
                        true
                    }
                    map.overlays.add(marker)
                }

                map.invalidate()
            },
        )

        Card(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.CenterStart)
                .padding(start = 12.dp),
        ) {
            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { zoomIn.value = true }) { Text("+") }
                Button(onClick = { zoomOut.value = true }) { Text("-") }
            }
        }

        clusterBookings.value?.let { list ->
            AlertDialog(
                onDismissRequest = { clusterBookings.value = null },
                title = {
                    Text(if (list.size > 1) "Заявки (${list.size})" else "Заявка")
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(list, key = { it.id }) { b ->
                            val dogName = state.dogs.firstOrNull { it.id == b.dog_id }?.name ?: "—"
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text("$dogName · ${bookingStatusRuShort(b.status)}")
                                    Text(
                                        b.scheduled_at,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    TextButton(
                                        onClick = {
                                            clusterBookings.value = null
                                            onOpenBookingDetail(b.id)
                                        },
                                    ) {
                                        Text("Подробнее")
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { clusterBookings.value = null }) {
                        Text("Закрыть")
                    }
                },
            )
        }
    }
}

private fun geoGroupKey(lat: Double, lng: Double): String =
    "${String.format("%.5f", lat)},${String.format("%.5f", lng)}"

private fun bookingStatusRuShort(status: String): String = when (status.uppercase()) {
    "PENDING" -> "ожидает"
    "CONFIRMED" -> "подтверждено"
    "IN_PROGRESS" -> "идёт"
    "COMPLETED" -> "завершено"
    "CANCELLED" -> "отменено"
    else -> status
}

private fun dogEmojiMapIcon(context: android.content.Context): Drawable {
    val d = context.resources.displayMetrics.density
    // Крупная метка заявок (видно поверх тайлов карты)
    val px = (96f * d).toInt().coerceIn(88, 160)
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textSize = px * 0.62f
        textAlign = Paint.Align.CENTER
    }
    val emoji = "🐶"
    val x = px / 2f
    val fm = paint.fontMetrics
    val y = px / 2f - (fm.ascent + fm.descent) / 2f
    canvas.drawText(emoji, x, y, paint)
    return BitmapDrawable(context.resources, bmp)
}

package com.example.dogapp.presentation.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.dogapp.data.api.WalkRouteResponseDto
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun WalkRouteMapPreview(
    route: WalkRouteResponseDto?,
    meetingLatitude: Double?,
    meetingLongitude: Double?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Configuration.getInstance().userAgentValue = context.packageName
    val points = route?.points.orEmpty()
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        factory = { ctx ->
            MapView(ctx).apply {
                setMultiTouchControls(true)
            }
        },
        update = { map ->
            map.overlays.clear()
            when {
                points.isNotEmpty() -> {
                    val polyline = Polyline().apply {
                        setPoints(points.map { GeoPoint(it.latitude, it.longitude) })
                        outlinePaint.color = android.graphics.Color.parseColor("#0F766E")
                        outlinePaint.strokeWidth = 10f
                    }
                    map.overlays.add(polyline)
                    val bb = route?.summary?.bbox
                    if (bb != null) {
                        runCatching {
                            map.zoomToBoundingBox(
                                BoundingBox(bb.max_lat, bb.max_lng, bb.min_lat, bb.min_lng),
                                true,
                                72,
                            )
                        }
                    } else {
                        val cLat = points.map { it.latitude }.average()
                        val cLng = points.map { it.longitude }.average()
                        map.controller.setZoom(16.0)
                        map.controller.setCenter(GeoPoint(cLat, cLng))
                    }
                }
                meetingLatitude != null && meetingLongitude != null -> {
                    map.controller.setZoom(15.0)
                    map.controller.setCenter(GeoPoint(meetingLatitude, meetingLongitude))
                    val m = Marker(map)
                    m.position = GeoPoint(meetingLatitude, meetingLongitude)
                    map.overlays.add(m)
                }
                else -> {
                    map.controller.setZoom(12.0)
                    map.controller.setCenter(GeoPoint(59.9343, 30.3351))
                }
            }
            map.invalidate()
        },
    )
}

package com.gusur.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusur.app.theme.GusurColors
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

data class MapMarker(
    val id: String,
    val lat: Double,
    val lon: Double,
    val highlighted: Boolean = false,
    val emoji: String = "🧖"
)

private const val TILE_SIZE = 256
private const val MIN_ZOOM = 4
private const val MAX_ZOOM = 17

private fun lonToWorldX(lon: Double, zoom: Int): Double {
    val n = 1 shl zoom
    return (lon + 180.0) / 360.0 * n * TILE_SIZE
}

private fun latToWorldY(lat: Double, zoom: Int): Double {
    val n = 1 shl zoom
    val rad = lat * PI / 180.0
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * n * TILE_SIZE
}

private fun worldXToLon(x: Double, zoom: Int): Double {
    val n = 1 shl zoom
    return x / (n * TILE_SIZE) * 360.0 - 180.0
}

private fun worldYToLat(y: Double, zoom: Int): Double {
    val n = 1 shl zoom
    val frac = y / (n * TILE_SIZE)
    val rad = atan(sinh(PI * (1.0 - 2.0 * frac)))
    return rad * 180.0 / PI
}

@Composable
fun OsmMapView(
    centerLat: Double,
    centerLon: Double,
    markers: List<MapMarker>,
    onMarkerClick: (MapMarker) -> Unit,
    modifier: Modifier = Modifier,
    initialZoom: Int = 11
) {
    var zoom by remember { mutableStateOf(initialZoom) }
    var center by remember { mutableStateOf(centerLat to centerLon) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(centerLat, centerLon) {
        center = centerLat to centerLon
    }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .background(Color(0xFFE3F2FD))
            .onSizeChanged { size = it }
            .pointerInput(zoom) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val cx = lonToWorldX(center.second, zoom)
                    val cy = latToWorldY(center.first, zoom)
                    val newX = cx - dragAmount.x
                    val newY = cy - dragAmount.y
                    center = worldYToLat(newY, zoom) to worldXToLon(newX, zoom)
                }
            }
    ) {
        if (size.width > 0 && size.height > 0) {
            val centerWorldX = lonToWorldX(center.second, zoom)
            val centerWorldY = latToWorldY(center.first, zoom)
            val halfW = size.width / 2.0
            val halfH = size.height / 2.0
            val totalTiles = 1 shl zoom

            val minTileX = ((centerWorldX - halfW) / TILE_SIZE).toInt()
            val maxTileX = ((centerWorldX + halfW) / TILE_SIZE).toInt()
            val minTileY = ((centerWorldY - halfH) / TILE_SIZE).toInt().coerceAtLeast(0)
            val maxTileY = ((centerWorldY + halfH) / TILE_SIZE).toInt().coerceAtMost(totalTiles - 1)

            for (tx in minTileX..maxTileX) {
                val wrappedTx = ((tx % totalTiles) + totalTiles) % totalTiles
                for (ty in minTileY..maxTileY) {
                    val tileWorldX = tx * TILE_SIZE
                    val tileWorldY = ty * TILE_SIZE
                    val screenX = (tileWorldX - centerWorldX + halfW)
                    val screenY = (tileWorldY - centerWorldY + halfH)
                    val url = "https://tile.openstreetmap.org/$zoom/$wrappedTx/$ty.png"
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(density) { screenX.toInt().toDp() },
                                y = with(density) { screenY.toInt().toDp() }
                            )
                            .size(with(density) { TILE_SIZE.toDp() })
                    ) {
                        KamelImage(
                            resource = asyncPainterResource(url),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            markers.forEach { marker ->
                val mx = lonToWorldX(marker.lon, zoom)
                val my = latToWorldY(marker.lat, zoom)
                val sx = mx - centerWorldX + halfW
                val sy = my - centerWorldY + halfH
                if (sx in -32.0..(size.width + 32.0) && sy in -32.0..(size.height + 32.0)) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(density) { sx.toInt().toDp() } - 14.dp,
                                y = with(density) { sy.toInt().toDp() } - 14.dp
                            )
                            .size(28.dp)
                            .clickable { onMarkerClick(marker) }
                    ) {
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(2.dp, GusurColors.Ink),
                            color = if (marker.highlighted) GusurColors.Ember else GusurColors.Gold,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(marker.emoji, fontSize = 14.sp, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 32.dp)) {
            ZoomButton(zoomIn = true) { zoom = (zoom + 1).coerceAtMost(MAX_ZOOM) }
            Spacer(Modifier.height(6.dp))
            ZoomButton(zoomIn = false) { zoom = (zoom - 1).coerceAtLeast(MIN_ZOOM) }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            color = Color.White.copy(alpha = 0.85f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                "© OpenStreetMap contributors",
                fontSize = 9.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                color = Color.DarkGray
            )
        }
    }
}

@Composable
private fun ZoomButton(zoomIn: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(36.dp).clickable(onClick = onClick),
        shape = CircleShape,
        border = BorderStroke(2.dp, GusurColors.Ink),
        color = GusurColors.Paper,
        elevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (zoomIn) Icons.Default.Add else Icons.Default.Remove,
                contentDescription = if (zoomIn) "Zoom in" else "Zoom out",
                tint = GusurColors.Ink,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

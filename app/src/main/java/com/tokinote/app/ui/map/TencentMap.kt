package com.tokinote.app.ui.map

import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.JournalLocation
import com.tokinote.app.data.Note
import com.tokinote.app.ui.notes.dayLabel
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TILE_SIZE = 256
private const val MIN_ZOOM = 3
private const val MAX_ZOOM = 18

data class MapPoint(val lat: Double, val lng: Double)

private fun lngToWorldX(lng: Double, zoom: Int): Double =
    (lng + 180.0) / 360.0 * TILE_SIZE * (1 shl zoom)

private fun latToWorldY(lat: Double, zoom: Int): Double {
    val clamped = lat.coerceIn(-85.0511, 85.0511)
    val rad = clamped * PI / 180.0
    return (0.5 - ln((1 + sin(rad)) / (1 - sin(rad))) / (4 * PI)) * TILE_SIZE * (1 shl zoom)
}

private fun worldXToLng(x: Double, zoom: Int): Double =
    x / (TILE_SIZE * (1 shl zoom)) * 360.0 - 180.0

private fun worldYToLat(y: Double, zoom: Int): Double {
    val n = PI - 2 * PI * y / (TILE_SIZE * (1 shl zoom))
    return 180.0 / PI * atan(0.5 * (exp(n) - exp(-n)))
}

/**
 * 腾讯地图瓦片地图。坐标使用 GCJ-02（与腾讯瓦片一致）。
 * 不依赖 SDK / Key，直接取 rt0-3.map.gtimg.com 的瓦片。
 */
@Composable
fun TencentMap(
    notes: List<Note>,
    modifier: Modifier = Modifier,
    onOpenNote: (String) -> Unit = {},
    pickMode: Boolean = false,
    onPick: (MapPoint) -> Unit = {},
    initialCenter: MapPoint? = null,
    initialZoom: Int = 12,
) {
    val firstLocation = notes.firstNotNullOfOrNull { it.location }
    var center by remember {
        mutableStateOf(
            initialCenter ?: firstLocation?.let { MapPoint(it.lat, it.lng) }
                ?: MapPoint(39.9042, 116.4074),
        )
    }
    var zoom by remember { mutableIntStateOf(initialZoom) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var picked by remember { mutableStateOf<MapPoint?>(null) }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }

    val tiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val loading = remember { mutableStateMapOf<String, Boolean>() }
    val textMeasurer = rememberTextMeasurer()

    val visibleTiles = remember(center, zoom, viewSize) {
        if (viewSize.width == 0) emptyList()
        else {
            val centerX = lngToWorldX(center.lng, zoom)
            val centerY = latToWorldY(center.lat, zoom)
            val left = centerX - viewSize.width / 2.0
            val top = centerY - viewSize.height / 2.0
            val startX = floor(left / TILE_SIZE).toInt()
            val endX = floor((left + viewSize.width) / TILE_SIZE).toInt()
            val startY = floor(top / TILE_SIZE).toInt()
            val endY = floor((top + viewSize.height) / TILE_SIZE).toInt()
            buildList {
                for (x in startX..endX) {
                    for (y in startY..endY) {
                        add(Triple(zoom, x, y))
                    }
                }
            }
        }
    }

    LaunchedEffect(visibleTiles) {
        visibleTiles.forEach { (z, x, y) ->
            val key = "$z/$x/$y"
            if (tiles[key] == null && loading[key] != true) {
                loading[key] = true
                launch {
                    val bitmap = loadTencentTile(z, x, y)
                    if (bitmap != null) tiles[key] = bitmap
                    loading[key] = false
                }
            }
        }
    }

    val onColor = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.surfaceContainer

    Box(modifier = modifier.background(container)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewSize = it }
                .pointerInput(zoom) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val centerX = lngToWorldX(center.lng, zoom) - drag.x
                        val centerY = latToWorldY(center.lat, zoom) - drag.y
                        center = MapPoint(worldYToLat(centerY, zoom), worldXToLng(centerX, zoom))
                    }
                }
                .pointerInput(zoom, pickMode) {
                    detectTapGestures { offset ->
                        val worldX = lngToWorldX(center.lng, zoom) + (offset.x - viewSize.width / 2.0)
                        val worldY = latToWorldY(center.lat, zoom) + (offset.y - viewSize.height / 2.0)
                        val point = MapPoint(worldYToLat(worldY, zoom), worldXToLng(worldX, zoom))
                        if (pickMode) {
                            picked = point
                            onPick(point)
                        } else {
                            val hit = notes.firstOrNull { note ->
                                val loc = note.location ?: return@firstOrNull false
                                val noteX = lngToWorldX(loc.lng, zoom) - lngToWorldX(center.lng, zoom) + viewSize.width / 2.0
                                val noteY = latToWorldY(loc.lat, zoom) - latToWorldY(center.lat, zoom) + viewSize.height / 2.0
                                val dx = noteX - offset.x
                                val dy = noteY - offset.y
                                dx * dx + dy * dy <= (28 * density) * (28 * density)
                            }
                            selectedNoteId = hit?.id
                        }
                    }
                },
        ) {
            val centerX = lngToWorldX(center.lng, zoom)
            val centerY = latToWorldY(center.lat, zoom)

            visibleTiles.forEach { (z, x, y) ->
                val bitmap = tiles["$z/$x/$y"] ?: return@forEach
                val screenX = x * TILE_SIZE - centerX + viewSize.width / 2.0
                val screenY = y * TILE_SIZE - centerY + viewSize.height / 2.0
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(screenX.toInt(), screenY.toInt()),
                    dstSize = IntSize(TILE_SIZE, TILE_SIZE),
                )
            }

            if (tiles.isEmpty()) {
                drawRect(color = container)
            }

            notes.forEach { note ->
                val loc = note.location ?: return@forEach
                val sx = (lngToWorldX(loc.lng, zoom) - centerX + viewSize.width / 2.0).toFloat()
                val sy = (latToWorldY(loc.lat, zoom) - centerY + viewSize.height / 2.0).toFloat()
                drawCircle(color = Color.White, radius = 11.dp.toPx(), center = Offset(sx, sy))
                drawCircle(color = primary, radius = 8.dp.toPx(), center = Offset(sx, sy))
            }

            picked?.let { point ->
                val sx = (lngToWorldX(point.lng, zoom) - centerX + viewSize.width / 2.0).toFloat()
                val sy = (latToWorldY(point.lat, zoom) - centerY + viewSize.height / 2.0).toFloat()
                drawCircle(color = Color.Black.copy(alpha = 0.2f), radius = 14.dp.toPx(), center = Offset(sx, sy))
                drawCircle(color = primary, radius = 10.dp.toPx(), center = Offset(sx, sy + 2.dp.toPx()))
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(sx, sy + 2.dp.toPx()))
            }

            if (tiles.isEmpty()) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = "正在加载腾讯地图…",
                    topLeft = Offset(size.width / 2 - 60.dp.toPx(), size.height / 2),
                    style = TextStyle(color = onColor, fontSize = 14.sp),
                )
            }
        }

        Text(
            text = stringResource(R.string.map_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalIconButton(onClick = { zoom = (zoom + 1).coerceAtMost(MAX_ZOOM) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.zoom_in))
            }
            FilledTonalIconButton(onClick = { zoom = (zoom - 1).coerceAtLeast(MIN_ZOOM) }) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.zoom_out))
            }
        }

        val selected = notes.firstOrNull { it.id == selectedNoteId }
        if (selected != null && !pickMode) {
            Surface(
                onClick = { onOpenNote(selected.id) },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = selected.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = selected.title.ifBlank { stringResource(R.string.note_untitled) },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = selected.journalDate.dayLabel(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private val density: Float
    get() = android.content.res.Resources.getSystem().displayMetrics.density

private suspend fun loadTencentTile(z: Int, x: Int, y: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    val url = "https://rt${(x + y) and 3}.map.gtimg.com/tile?z=$z&x=$x&y=$y&styleid=1&version=2"
    runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
        connection.setRequestProperty("Referer", "https://map.qq.com/")
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.inputStream.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    }.getOrNull()
}

/** 位置选择页：在腾讯地图上点选坐标，可命名。 */
@Composable
fun LocationPickerScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onConfirm: (JournalLocation) -> Unit,
) {
    var point by remember { mutableStateOf<MapPoint?>(null) }
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(contentPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = stringResource(R.string.location_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(R.string.location_pick_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        TencentMap(
            notes = emptyList(),
            modifier = Modifier.weight(1f).padding(12.dp),
            pickMode = true,
            onPick = { point = it },
            initialZoom = 14,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.location_name)) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = {
                    val p = point ?: return@Button
                    onConfirm(
                        JournalLocation(
                            lat = p.lat,
                            lng = p.lng,
                            name = name.ifBlank { "自定义位置" },
                        ),
                    )
                },
                enabled = point != null,
                shape = CircleShape,
            ) {
                Text(stringResource(R.string.location_confirm))
            }
        }
    }
}

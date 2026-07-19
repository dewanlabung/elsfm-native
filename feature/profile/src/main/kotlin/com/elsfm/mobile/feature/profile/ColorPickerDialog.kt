package com.elsfm.mobile.feature.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ComposeShader
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    val hsv = remember {
        val arr = floatArrayOf(0f, 0f, 0f)
        AndroidColor.colorToHSV(initialColor.toArgb(), arr)
        mutableStateOf(Triple(arr[0], arr[1], arr[2]))
    }
    val previewColor by remember(hsv.value) {
        mutableStateOf(Color.hsv(hsv.value.first, hsv.value.second, hsv.value.third))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SatValPanel(hue = hsv.value.first) { sat, value ->
                    hsv.value = Triple(hsv.value.first, sat, value)
                }

                Spacer(modifier = Modifier.height(20.dp))

                HueBar { hue ->
                    hsv.value = Triple(hue, hsv.value.second, hsv.value.third)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(previewColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    )
                    Text(
                        text = "#%06X".format(previewColor.toArgb() and 0xFFFFFF),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorSelected(previewColor)
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SatValPanel(hue: Float, setSatVal: (Float, Float) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val pressOffset = remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = Modifier
            .size(280.dp)
            .emitDragGesture(interactionSource, scope) { offset ->
                pressOffset.value = offset
                val sat = (offset.x / 280.dp.value).coerceIn(0f, 1f)
                val value = 1f - (offset.y / 280.dp.value).coerceIn(0f, 1f)
                setSatVal(sat, value)
            }
            .clip(RoundedCornerShape(12.dp)),
    ) {
        val w = size.width.toInt()
        val h = size.height.toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val panel = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val rgb = AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f))
        val satShader = android.graphics.LinearGradient(
            panel.left, panel.top, panel.right, panel.top,
            -0x1, rgb, Shader.TileMode.CLAMP,
        )
        val valShader = android.graphics.LinearGradient(
            panel.left, panel.top, panel.left, panel.bottom,
            -0x1, -0x1000000, Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(
            panel, 12f, 12f,
            Paint().apply {
                shader = ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY)
            },
        )
        drawBitmap(bitmap, panel)
        drawCircle(Color.White, 10.dp.toPx(), pressOffset.value, style = Stroke(2.dp.toPx()))
        drawCircle(Color.White, 3.dp.toPx(), pressOffset.value)
    }
}

@Composable
private fun HueBar(setColor: (Float) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val pressOffset = remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = Modifier
            .height(36.dp)
            .width(280.dp)
            .emitDragGesture(interactionSource, scope) { offset ->
                val x = offset.x.coerceIn(0f, 280.dp.value)
                pressOffset.value = Offset(x, 0f)
                setColor(x * 360f / 280.dp.value)
            }
            .clip(RoundedCornerShape(50)),
    ) {
        val w = size.width.toInt()
        val h = size.height.toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val hueCanvas = Canvas(bitmap)
        val panel = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val linePaint = Paint().apply { strokeWidth = 0f }
        for (i in 0 until w) {
            linePaint.color = AndroidColor.HSVToColor(floatArrayOf(i * 360f / w, 1f, 1f))
            hueCanvas.drawLine(i.toFloat(), 0f, i.toFloat(), panel.bottom, linePaint)
        }
        drawBitmap(bitmap, panel)
        drawCircle(
            Color.White,
            size.height / 2,
            Offset(pressOffset.value.x, size.height / 2),
            style = Stroke(2.dp.toPx()),
        )
    }
}

private fun Modifier.emitDragGesture(
    interactionSource: MutableInteractionSource,
    scope: CoroutineScope,
    onPosition: (Offset) -> Unit,
): Modifier = composed {
    pointerInput(Unit) {
        detectDragGestures { input, _ ->
            scope.launch {
                interactionSource.emit(PressInteraction.Press(input.position))
                onPosition(input.position)
            }
        }
    }.clickable(interactionSource, null) {}
}

private fun DrawScope.drawBitmap(bitmap: Bitmap, panel: RectF) {
    drawIntoCanvas { it.nativeCanvas.drawBitmap(bitmap, null, panel.toRect(), null) }
}

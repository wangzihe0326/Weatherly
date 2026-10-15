package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class WeatherGlyph { SUN, CLOUD, UMBRELLA, DROPLET, LOCATION, THERMOMETER, SETTINGS, REFRESH, CHECK, ARROW_RIGHT, CLOCK }

/** One outlined icon family. The enclosing labelled control supplies semantics. */
@Composable
fun WeatherIcon(
    glyph: WeatherGlyph,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier.size(24.dp)) {
        val stroke = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(x1, y1), Offset(x2, y2), 1.7f, StrokeCap.Round)
        fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), tint, style = stroke)
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            when (glyph) {
                WeatherGlyph.SUN -> {
                    drawCircle(tint, 4.1f, Offset(12f, 12f), style = stroke)
                    repeat(8) { i ->
                        val angle = i * Math.PI / 4
                        line(12f + 7f * cos(angle).toFloat(), 12f + 7f * sin(angle).toFloat(),
                            12f + 9.2f * cos(angle).toFloat(), 12f + 9.2f * sin(angle).toFloat())
                    }
                }
                WeatherGlyph.CLOUD -> path {
                    moveTo(6f, 18f); cubicTo(0.5f, 18f, 1f, 10f, 6f, 10f)
                    cubicTo(6f, 2f, 18f, 2f, 18f, 11f)
                    cubicTo(24f, 10.5f, 24f, 18f, 18f, 18f); close()
                }
                WeatherGlyph.UMBRELLA -> {
                    path {
                        moveTo(3f, 12f); cubicTo(3f, 1f, 21f, 1f, 21f, 12f)
                        quadraticTo(18.7f, 9.5f, 16.5f, 12f)
                        quadraticTo(14.2f, 9.5f, 12f, 12f)
                        quadraticTo(9.7f, 9.5f, 7.5f, 12f)
                        quadraticTo(5.2f, 9.5f, 3f, 12f)
                    }
                    path { moveTo(12f, 12f); lineTo(12f, 19f); cubicTo(12f, 23f, 17f, 23f, 17f, 19f) }
                    line(12f, 2f, 12f, 4f)
                }
                WeatherGlyph.DROPLET -> path {
                    moveTo(12f, 2f); cubicTo(9f, 7f, 5f, 10.5f, 5f, 14.5f)
                    cubicTo(5f, 23.5f, 19f, 23.5f, 19f, 14.5f)
                    cubicTo(19f, 10.5f, 15f, 7f, 12f, 2f); close()
                }
                WeatherGlyph.LOCATION -> {
                    path {
                        moveTo(12f, 22f); cubicTo(8f, 17f, 4f, 13f, 4f, 9f)
                        cubicTo(4f, -1f, 20f, -1f, 20f, 9f)
                        cubicTo(20f, 13f, 16f, 17f, 12f, 22f); close()
                    }
                    drawCircle(tint, 2.5f, Offset(12f, 9f), style = stroke)
                }
                WeatherGlyph.THERMOMETER -> {
                    path {
                        moveTo(9f, 14f); lineTo(9f, 5f); cubicTo(9f, 1f, 15f, 1f, 15f, 5f)
                        lineTo(15f, 14f); cubicTo(21f, 20f, 3f, 25f, 7f, 16f); close()
                    }
                    line(12f, 8f, 12f, 18f)
                    drawCircle(tint, 1.6f, Offset(12f, 18f))
                }
                WeatherGlyph.SETTINGS -> {
                    listOf(6f, 12f, 18f).forEachIndexed { index, y ->
                        val x = if (index == 1) 15f else 8f
                        line(3f, y, x - 2f, y); line(x + 2f, y, 21f, y)
                        drawCircle(tint, 2f, Offset(x, y), style = stroke)
                    }
                }
                WeatherGlyph.REFRESH -> {
                    drawArc(tint, 35f, 285f, false, Offset(4f, 4f), Size(16f, 16f), style = stroke)
                    path { moveTo(20f, 3f); lineTo(20f, 9f); lineTo(14f, 9f) }
                }
                WeatherGlyph.CHECK -> path { moveTo(5f, 12f); lineTo(10f, 17f); lineTo(20f, 6f) }
                WeatherGlyph.ARROW_RIGHT -> {
                    line(4f, 12f, 20f, 12f)
                    path { moveTo(14f, 6f); lineTo(20f, 12f); lineTo(14f, 18f) }
                }
                WeatherGlyph.CLOCK -> {
                    drawCircle(tint, 9f, Offset(12f, 12f), style = stroke)
                    path { moveTo(12f, 6f); lineTo(12f, 12f); lineTo(16f, 14f) }
                }
            }
        }
    }
}

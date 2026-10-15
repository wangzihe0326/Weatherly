package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Original decorative weather symbols; the enclosing forecast supplies the spoken description. */
@Composable
fun ForecastConditionIcon(
    weatherCode: Int?,
    modifier: Modifier = Modifier,
    isDay: Boolean? = null
) {
    val outline = MaterialTheme.colorScheme.onSurface
    val cloudFill = MaterialTheme.colorScheme.surfaceContainerHigh
    val sun = MaterialTheme.colorScheme.tertiary
    val sunFill = MaterialTheme.colorScheme.tertiaryContainer
    val rain = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(40.dp).clearAndSetSemantics {}) {
        withTransform({ scale(size.width / 48f, size.height / 48f, pivot = Offset.Zero) }) {
            when (weatherCode) {
                0 -> forecastSky(Offset(24f, 24f), 10f, isDay, sun, sunFill, outline)
                1, 2 -> {
                    forecastSky(Offset(32f, 15f), 7f, isDay, sun, sunFill, outline)
                    forecastCloud(Offset(3f, 15f), 0.94f, cloudFill, outline)
                }
                3 -> {
                    forecastCloud(Offset(10f, 7f), 0.88f, cloudFill, outline.copy(alpha = 0.6f))
                    forecastCloud(Offset(2f, 17f), 1f, cloudFill, outline)
                }
                45, 48 -> {
                    forecastCloud(Offset(3f, 7f), 1f, cloudFill, outline)
                    repeat(3) { line ->
                        val inset = if (line == 1) 0f else 4f
                        drawLine(outline, Offset(7f + inset, 32f + line * 5f), Offset(41f - inset, 32f + line * 5f), 1.8f, StrokeCap.Round)
                    }
                }
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> {
                    forecastCloud(Offset(3f, 5f), 1f, cloudFill, outline)
                    repeat(3) { drop ->
                        val x = 14f + drop * 10f
                        drawLine(rain, Offset(x, 31f), Offset(x - 3f, 39f), 2.2f, StrokeCap.Round)
                    }
                }
                71, 73, 75, 77, 85, 86 -> {
                    forecastCloud(Offset(3f, 5f), 1f, cloudFill, outline)
                    repeat(2) { flake ->
                        val center = Offset(16f + flake * 17f, 36f)
                        repeat(3) { arm ->
                            val angle = arm * Math.PI / 3
                            val delta = Offset(cos(angle).toFloat(), sin(angle).toFloat()) * 4f
                            drawLine(rain, center - delta, center + delta, 1.6f, StrokeCap.Round)
                        }
                    }
                }
                95, 96, 97, 99 -> {
                    forecastCloud(Offset(3f, 4f), 1f, cloudFill, outline)
                    val bolt = Path().apply {
                        moveTo(26f, 28f); lineTo(18f, 38f); lineTo(25f, 38f)
                        lineTo(22f, 46f); lineTo(34f, 33f); lineTo(27f, 33f); close()
                    }
                    drawPath(bolt, sunFill)
                    drawPath(bolt, sun, style = Stroke(1.6f, join = StrokeJoin.Round))
                }
                else -> {
                    drawCircle(outline.copy(alpha = 0.65f), 13f, Offset(24f, 24f), style = Stroke(1.8f))
                    drawLine(outline, Offset(18f, 24f), Offset(30f, 24f), 2f, StrokeCap.Round)
                }
            }
        }
    }
}

private fun DrawScope.forecastSky(
    center: Offset,
    radius: Float,
    isDay: Boolean?,
    sun: Color,
    fill: Color,
    outline: Color
) {
    if (isDay == false) {
        val moon = Path().apply {
            moveTo(center.x + radius * 0.55f, center.y - radius)
            cubicTo(center.x - radius * 1.8f, center.y - radius, center.x - radius * 1.3f, center.y + radius * 1.5f, center.x + radius * 0.5f, center.y + radius)
            cubicTo(center.x - radius * 0.3f, center.y + radius * 0.5f, center.x - radius * 0.5f, center.y - radius * 0.2f, center.x + radius * 0.55f, center.y - radius)
            close()
        }
        drawPath(moon, fill)
        drawPath(moon, outline, style = Stroke(1.7f, join = StrokeJoin.Round))
    } else {
        drawCircle(fill, radius, center)
        drawCircle(sun, radius, center, style = Stroke(1.8f))
        repeat(8) { ray ->
            val angle = ray * Math.PI / 4
            val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(sun, center + direction * (radius + 3f), center + direction * (radius + 6f), 1.8f, StrokeCap.Round)
        }
    }
}

private fun DrawScope.forecastCloud(origin: Offset, cloudScale: Float, fill: Color, outline: Color) {
    withTransform({ translate(origin.x, origin.y); scale(cloudScale, cloudScale, pivot = Offset.Zero) }) {
        val cloud = Path().apply {
            moveTo(10f, 22f)
            cubicTo(0f, 22f, 0f, 10f, 9f, 10f)
            cubicTo(11f, -2f, 27f, -2f, 29f, 10f)
            cubicTo(40f, 7f, 45f, 22f, 33f, 22f)
            close()
        }
        drawPath(cloud, fill)
        drawPath(cloud, outline, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

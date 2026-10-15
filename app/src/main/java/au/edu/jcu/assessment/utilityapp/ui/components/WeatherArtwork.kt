package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import au.edu.jcu.assessment.utilityapp.ui.theme.LocalWeatherColors
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private enum class ArtworkKind { CLEAR, PARTLY_CLOUDY, CLOUD, RAIN, STORM, SNOW, FOG, UNKNOWN }

/** A decorative vector companion to the adjacent, authoritative weather description. */
@Composable
fun WeatherArtwork(description: String, modifier: Modifier = Modifier) {
    val colors = LocalWeatherColors.current
    val normalized = description.lowercase(Locale.ROOT)
    // Snow grains contains "rain": test specific conditions before generic substrings.
    val kind = when {
        "thunder" in normalized || "hail" in normalized -> ArtworkKind.STORM
        "snow" in normalized || "ice" in normalized -> ArtworkKind.SNOW
        "fog" in normalized -> ArtworkKind.FOG
        "rain" in normalized || "drizzle" in normalized -> ArtworkKind.RAIN
        "partly" in normalized || "mainly clear" in normalized -> ArtworkKind.PARTLY_CLOUDY
        "cloud" in normalized || "overcast" in normalized -> ArtworkKind.CLOUD
        "clear" in normalized -> ArtworkKind.CLEAR
        else -> ArtworkKind.UNKNOWN
    }

    Canvas(modifier.clearAndSetSemantics {}) {
        withTransform({ scale(size.width / 200f, size.height / 200f, pivot = Offset.Zero) }) {
            drawCircle(colors.onHero.copy(alpha = 0.035f), 88f, Offset(105f, 98f))
            drawCircle(colors.onHero.copy(alpha = 0.07f), 72f, Offset(105f, 98f), style = Stroke(0.8f))

            if (kind == ArtworkKind.CLEAR || kind == ArtworkKind.PARTLY_CLOUDY) {
                val center = if (kind == ArtworkKind.CLEAR) Offset(100f, 97f) else Offset(137f, 65f)
                val radius = if (kind == ArtworkKind.CLEAR) 43f else 34f
                drawCircle(
                    Brush.radialGradient(
                        listOf(colors.sun.copy(alpha = 0.21f), colors.sun.copy(alpha = 0f)),
                        center = center,
                        radius = radius * 2f
                    ),
                    radius * 2f,
                    center
                )
                repeat(8) { index ->
                    val angle = index * Math.PI / 4
                    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    drawLine(
                        colors.sun.copy(alpha = 0.7f),
                        center + direction * (radius + 12f),
                        center + direction * (radius + 20f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    Brush.linearGradient(
                        listOf(colors.cloud, colors.sun),
                        start = center - Offset(radius, radius),
                        end = center + Offset(radius, radius)
                    ),
                    radius,
                    center
                )
            }

            if (kind != ArtworkKind.CLEAR && kind != ArtworkKind.UNKNOWN) {
                drawWeatherCloud(Offset(56f, 50f), 0.78f, colors.cloudShadow.copy(alpha = 0.75f), colors.cloudShadow)
                drawWeatherCloud(Offset(21f, 70f), 1f, colors.cloud, colors.cloudShadow)
            }

            when (kind) {
                ArtworkKind.RAIN -> repeat(3) { index ->
                    val x = 67f + index * 31f
                    val top = 143f + if (index == 1) 6f else 0f
                    drawLine(
                        Brush.linearGradient(listOf(colors.rain, colors.rain.copy(alpha = 0.45f))),
                        Offset(x, top), Offset(x - 9f, top + 22f),
                        strokeWidth = 5f, cap = StrokeCap.Round
                    )
                }
                ArtworkKind.STORM -> {
                    val bolt = Path().apply {
                        moveTo(107f, 133f)
                        lineTo(87f, 162f)
                        lineTo(104f, 162f)
                        lineTo(96f, 185f)
                        lineTo(126f, 150f)
                        lineTo(108f, 150f)
                        close()
                    }
                    drawPath(bolt, colors.sun)
                    drawLine(colors.rain, Offset(65f, 144f), Offset(58f, 161f), 4f, StrokeCap.Round)
                    drawLine(colors.rain, Offset(143f, 143f), Offset(137f, 160f), 4f, StrokeCap.Round)
                }
                ArtworkKind.SNOW -> repeat(3) { index ->
                    val center = Offset(65f + index * 34f, 156f + if (index == 1) 12f else 0f)
                    repeat(3) { arm ->
                        val angle = arm * Math.PI / 3
                        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat()) * 7f
                        drawLine(colors.cloud, center - direction, center + direction, 2f, StrokeCap.Round)
                    }
                }
                ArtworkKind.FOG -> repeat(3) { index ->
                    val inset = if (index == 1) 0f else 12f
                    drawLine(
                        colors.cloud.copy(alpha = 0.75f),
                        Offset(38f + inset, 148f + index * 13f),
                        Offset(165f - inset, 148f + index * 13f),
                        4f,
                        StrokeCap.Round
                    )
                }
                ArtworkKind.UNKNOWN -> {
                    drawCircle(colors.heroMuted.copy(alpha = 0.4f), 45f, Offset(100f, 100f), style = Stroke(2f))
                    drawLine(colors.heroMuted, Offset(81f, 100f), Offset(119f, 100f), 4f, StrokeCap.Round)
                }
                else -> Unit
            }
        }
    }
}

private fun DrawScope.drawWeatherCloud(origin: Offset, cloudScale: Float, top: Color, bottom: Color) {
    withTransform({ translate(origin.x, origin.y); scale(cloudScale, cloudScale, pivot = Offset.Zero) }) {
        val cloud = Path().apply {
            moveTo(30f, 62f)
            cubicTo(10f, 62f, 5f, 49f, 9f, 37f)
            cubicTo(12f, 26f, 23f, 22f, 33f, 24f)
            cubicTo(35f, 7f, 47f, 0f, 63f, 2f)
            cubicTo(78f, 3f, 87f, 13f, 89f, 26f)
            cubicTo(99f, 15f, 116f, 20f, 120f, 33f)
            cubicTo(140f, 32f, 148f, 45f, 141f, 55f)
            cubicTo(138f, 62f, 130f, 64f, 118f, 64f)
            lineTo(30f, 64f)
            close()
        }
        drawPath(cloud, Brush.linearGradient(listOf(top, bottom), Offset(30f, 0f), Offset(100f, 78f)))
        drawRoundRect(top.copy(alpha = 0.2f), Offset(26f, 54f), Size(94f, 2f), CornerRadius(1f))
    }
}

package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import au.edu.jcu.assessment.utilityapp.ui.theme.LocalWeatherColors
import au.edu.jcu.assessment.utilityapp.ui.theme.WeatherColors
import kotlin.math.cos
import kotlin.math.sin

internal enum class WeatherArtworkVariant {
    CLEAR_DAY, CLEAR_NIGHT, CLEAR_NEUTRAL,
    PARTLY_CLOUDY_DAY, PARTLY_CLOUDY_NIGHT, PARTLY_CLOUDY_NEUTRAL,
    CLOUD, RAIN, STORM, SNOW, FOG, UNKNOWN
}

/** Select from structured current conditions; missing daylight never implies daytime. */
internal fun weatherArtworkVariant(weatherCode: Int?, isDay: Boolean?): WeatherArtworkVariant =
    when (weatherCode) {
        0 -> when (isDay) {
            true -> WeatherArtworkVariant.CLEAR_DAY
            false -> WeatherArtworkVariant.CLEAR_NIGHT
            null -> WeatherArtworkVariant.CLEAR_NEUTRAL
        }
        1, 2 -> when (isDay) {
            true -> WeatherArtworkVariant.PARTLY_CLOUDY_DAY
            false -> WeatherArtworkVariant.PARTLY_CLOUDY_NIGHT
            null -> WeatherArtworkVariant.PARTLY_CLOUDY_NEUTRAL
        }
        3 -> WeatherArtworkVariant.CLOUD
        45, 48 -> WeatherArtworkVariant.FOG
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherArtworkVariant.RAIN
        71, 73, 75, 77, 85, 86 -> WeatherArtworkVariant.SNOW
        95, 96, 97, 99 -> WeatherArtworkVariant.STORM
        else -> WeatherArtworkVariant.UNKNOWN
    }

/** A decorative vector companion to the adjacent, authoritative weather description. */
@Composable
fun WeatherArtwork(weatherCode: Int?, isDay: Boolean?, modifier: Modifier = Modifier) {
    val colors = LocalWeatherColors.current
    val variant = weatherArtworkVariant(weatherCode, isDay)

    Canvas(modifier.clearAndSetSemantics {}) {
        withTransform({ scale(size.width / 200f, size.height / 200f, pivot = Offset.Zero) }) {
            drawCircle(colors.onHero.copy(alpha = 0.035f), 88f, Offset(105f, 98f))
            drawCircle(colors.onHero.copy(alpha = 0.07f), 72f, Offset(105f, 98f), style = Stroke(0.8f))

            when (variant) {
                WeatherArtworkVariant.CLEAR_DAY -> drawWeatherSun(Offset(100f, 97f), 43f, colors)
                WeatherArtworkVariant.PARTLY_CLOUDY_DAY -> drawWeatherSun(Offset(137f, 65f), 34f, colors)
                WeatherArtworkVariant.CLEAR_NIGHT -> {
                    drawWeatherMoon(Offset(99f, 97f), 44f, colors)
                    drawWeatherStar(Offset(145f, 53f), 7f, colors.moon)
                    drawWeatherStar(Offset(157f, 104f), 4f, colors.heroMuted)
                    drawWeatherStar(Offset(52f, 52f), 3f, colors.heroMuted.copy(alpha = 0.7f))
                }
                WeatherArtworkVariant.PARTLY_CLOUDY_NIGHT -> {
                    drawWeatherMoon(Offset(137f, 48f), 34f, colors)
                    drawWeatherStar(Offset(89f, 36f), 5f, colors.moon)
                    drawWeatherStar(Offset(174f, 91f), 3f, colors.heroMuted)
                }
                else -> Unit
            }

            if (variant != WeatherArtworkVariant.CLEAR_DAY &&
                variant != WeatherArtworkVariant.CLEAR_NIGHT &&
                variant != WeatherArtworkVariant.CLEAR_NEUTRAL &&
                variant != WeatherArtworkVariant.UNKNOWN
            ) {
                drawWeatherCloud(Offset(56f, 50f), 0.78f, colors.cloudShadow.copy(alpha = 0.75f), colors.cloudShadow)
                drawWeatherCloud(Offset(21f, 70f), 1f, colors.cloud, colors.cloudShadow)
            }

            when (variant) {
                WeatherArtworkVariant.RAIN -> repeat(3) { index ->
                    val x = 67f + index * 31f
                    val top = 143f + if (index == 1) 6f else 0f
                    drawLine(
                        Brush.linearGradient(listOf(colors.rain, colors.rain.copy(alpha = 0.45f))),
                        Offset(x, top), Offset(x - 9f, top + 22f),
                        strokeWidth = 5f, cap = StrokeCap.Round
                    )
                }
                WeatherArtworkVariant.STORM -> {
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
                WeatherArtworkVariant.SNOW -> repeat(3) { index ->
                    val center = Offset(65f + index * 34f, 156f + if (index == 1) 12f else 0f)
                    repeat(3) { arm ->
                        val angle = arm * Math.PI / 3
                        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat()) * 7f
                        drawLine(colors.cloud, center - direction, center + direction, 2f, StrokeCap.Round)
                    }
                }
                WeatherArtworkVariant.FOG -> repeat(3) { index ->
                    val inset = if (index == 1) 0f else 12f
                    drawLine(
                        colors.cloud.copy(alpha = 0.75f),
                        Offset(38f + inset, 148f + index * 13f),
                        Offset(165f - inset, 148f + index * 13f),
                        4f,
                        StrokeCap.Round
                    )
                }
                WeatherArtworkVariant.CLEAR_NEUTRAL, WeatherArtworkVariant.UNKNOWN -> {
                    // A neutral placeholder avoids inventing a sun or moon when data is absent.
                    drawCircle(colors.heroMuted.copy(alpha = 0.4f), 45f, Offset(100f, 100f), style = Stroke(2f))
                    drawLine(colors.heroMuted, Offset(81f, 100f), Offset(119f, 100f), 4f, StrokeCap.Round)
                }
                else -> Unit
            }
        }
    }
}

private fun DrawScope.drawWeatherSun(center: Offset, radius: Float, colors: WeatherColors) {
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

private fun DrawScope.drawWeatherMoon(center: Offset, radius: Float, colors: WeatherColors) {
    drawCircle(
        Brush.radialGradient(
            listOf(colors.moonShadow.copy(alpha = 0.16f), colors.moonShadow.copy(alpha = 0f)),
            center = center,
            radius = radius * 1.8f
        ),
        radius * 1.8f,
        center
    )
    // Subtract geometry so the crescent's cutout preserves the hero's underlying gradient.
    val disc = Path().apply { addOval(Rect(center, radius)) }
    val cutout = Path().apply { addOval(Rect(center + Offset(radius * 0.45f, -radius * 0.3f), radius)) }
    val crescent = Path.combine(PathOperation.Difference, disc, cutout)
    drawPath(
        crescent,
        Brush.linearGradient(
            listOf(colors.moon, colors.moonShadow),
            center - Offset(radius, radius),
            center + Offset(radius, radius)
        )
    )
}

private fun DrawScope.drawWeatherStar(center: Offset, radius: Float, color: Color) {
    val star = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + radius * 0.15f, center.y - radius * 0.15f, center.x + radius, center.y)
        quadraticTo(center.x + radius * 0.15f, center.y + radius * 0.15f, center.x, center.y + radius)
        quadraticTo(center.x - radius * 0.15f, center.y + radius * 0.15f, center.x - radius, center.y)
        quadraticTo(center.x - radius * 0.15f, center.y - radius * 0.15f, center.x, center.y - radius)
        close()
    }
    drawPath(star, color)
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

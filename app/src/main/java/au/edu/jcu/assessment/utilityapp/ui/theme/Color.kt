package au.edu.jcu.assessment.utilityapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// An atmospheric blue hero and warm paper surfaces give the screens one identity.
val SkyBlue = Color(0xFF235A77)
val PaleSky = Color(0xFFE1EEF3)
val InkBlue = Color(0xFF172D3C)
val NightBlue = Color(0xFF203C50)
val LightSkyBlue = Color(0xFFA3D4EF)
val CloudWhite = Color(0xFFF7F6F2)
val DarkSurface = Color(0xFF101C25)
val Amber = Color(0xFF785000)
val PaleAmber = Color(0xFFFFE8AD)
val DarkAmber = Color(0xFF493813)
val LightAmber = Color(0xFFFFD781)

/** Colours for illustrated weather, separate from semantic Material controls. */
@Immutable
data class WeatherColors(
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val heroMuted: Color,
    val sun: Color,
    val cloud: Color,
    val cloudShadow: Color,
    val rain: Color
)

val LightWeatherColors = WeatherColors(
    heroStart = Color(0xFF12334C),
    heroEnd = Color(0xFF2B5871),
    onHero = Color(0xFFFFFFFF),
    heroMuted = Color(0xFFD5E6EF),
    sun = Color(0xFFFFD47B),
    cloud = Color(0xFFF6FBFF),
    cloudShadow = Color(0xFFB8D7E8),
    rain = Color(0xFF9BD5F4)
)

val DarkWeatherColors = LightWeatherColors.copy(
    heroStart = Color(0xFF182E43),
    heroEnd = Color(0xFF24475F),
    heroMuted = Color(0xFFCBDEE9)
)

val LocalWeatherColors = staticCompositionLocalOf { LightWeatherColors }

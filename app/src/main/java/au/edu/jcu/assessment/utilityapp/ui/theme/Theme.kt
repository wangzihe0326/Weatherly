package au.edu.jcu.assessment.utilityapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = PaleSky,
    onPrimaryContainer = InkBlue,
    secondary = Color(0xFF4C666F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F0ED),
    onSecondaryContainer = InkBlue,
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = PaleAmber,
    onTertiaryContainer = Color(0xFF493307),
    background = CloudWhite,
    onBackground = InkBlue,
    surface = Color.White,
    onSurface = InkBlue,
    surfaceVariant = Color(0xFFEAF0F2),
    onSurfaceVariant = Color(0xFF52636C),
    surfaceContainer = Color(0xFFEEF2F3),
    surfaceContainerLow = Color(0xFFF8FAFB),
    surfaceContainerHigh = Color(0xFFE5ECEF),
    outline = Color(0xFF73818A),
    outlineVariant = Color(0xFFD6E1E5),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFCE7E5),
    onErrorContainer = Color(0xFF601410)
)

private val DarkColorScheme = darkColorScheme(
    primary = LightSkyBlue,
    onPrimary = Color(0xFF003354),
    primaryContainer = NightBlue,
    onPrimaryContainer = Color(0xFFD6EAFF),
    secondary = Color(0xFFB4C9DC),
    onSecondary = Color(0xFF203446),
    secondaryContainer = Color(0xFF30495F),
    onSecondaryContainer = Color(0xFFD7E8F7),
    tertiary = LightAmber,
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = DarkAmber,
    onTertiaryContainer = Color(0xFFFFE6AA),
    background = DarkSurface,
    onBackground = Color(0xFFE3EAF0),
    surface = Color(0xFF1A2A35),
    onSurface = Color(0xFFE3EAF0),
    surfaceVariant = Color(0xFF35414D),
    onSurfaceVariant = Color(0xFFC0CDD8),
    surfaceContainer = Color(0xFF202D39),
    surfaceContainerLow = Color(0xFF18222C),
    surfaceContainerHigh = Color(0xFF2B3845),
    outline = Color(0xFF8F9EAB),
    outlineVariant = Color(0xFF4A5967),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5A211E),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MobileTech_UtilityAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    CompositionLocalProvider(
        LocalWeatherColors provides if (darkTheme) DarkWeatherColors else LightWeatherColors
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}

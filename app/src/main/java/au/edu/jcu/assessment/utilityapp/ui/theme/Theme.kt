package au.edu.jcu.assessment.utilityapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = PaleSky,
    onPrimaryContainer = InkBlue,
    secondary = Color(0xFF48627A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8F3),
    onSecondaryContainer = InkBlue,
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = PaleAmber,
    onTertiaryContainer = Color(0xFF382700),
    background = CloudWhite,
    onBackground = Color(0xFF17212B),
    surface = Color.White,
    onSurface = Color(0xFF17212B),
    surfaceVariant = Color(0xFFE7EDF3),
    onSurfaceVariant = Color(0xFF48535E),
    surfaceContainer = Color(0xFFF0F4F8),
    surfaceContainerLow = Color(0xFFF6F9FC),
    surfaceContainerHigh = Color(0xFFE7EDF3),
    outline = Color(0xFF6F7A85),
    outlineVariant = Color(0xFFC4CED8),
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
    surface = Color(0xFF18222C),
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
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

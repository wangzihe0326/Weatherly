package au.edu.jcu.assessment.utilityapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The device sans-serif family works offline and follows system text scaling.
private fun typeStyle(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) =
    TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = weight, fontSize = size.sp,
        lineHeight = line.sp, letterSpacing = tracking.sp)

val Typography = Typography(
    displayLarge = typeStyle(78, 84, FontWeight.Light, -4f),
    displayMedium = typeStyle(56, 64, FontWeight.Light, -2f),
    displaySmall = typeStyle(36, 44, FontWeight.Medium, -1f),
    headlineLarge = typeStyle(30, 36, FontWeight.SemiBold, -1f),
    headlineMedium = typeStyle(26, 32, FontWeight.SemiBold, -0.5f),
    headlineSmall = typeStyle(24, 30, FontWeight.SemiBold),
    titleLarge = typeStyle(20, 26, FontWeight.SemiBold, -0.3f),
    titleMedium = typeStyle(16, 24, FontWeight.SemiBold),
    titleSmall = typeStyle(14, 20, FontWeight.SemiBold),
    bodyLarge = typeStyle(16, 24),
    bodyMedium = typeStyle(14, 21),
    bodySmall = typeStyle(12, 18),
    labelLarge = typeStyle(14, 20, FontWeight.Medium),
    labelMedium = typeStyle(12, 18, FontWeight.Medium, 0.3f),
    labelSmall = typeStyle(12, 16, FontWeight.Medium, 1.2f)
)

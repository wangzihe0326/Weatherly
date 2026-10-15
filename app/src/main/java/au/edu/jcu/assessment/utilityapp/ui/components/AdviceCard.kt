package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.AdviceKind
import au.edu.jcu.assessment.utilityapp.data.UmbrellaAdvice

@Composable
fun AdviceCard(advice: UmbrellaAdvice) {
    val peak = advice.peakProbability
    val kind = if (peak == null) AdviceKind.UNAVAILABLE else advice.kind
    val isAlert = kind == AdviceKind.BRING_UMBRELLA
    val title = when (kind) {
        AdviceKind.BRING_UMBRELLA -> R.string.advice_bring
        AdviceKind.BELOW_THRESHOLD -> R.string.advice_below
        AdviceKind.UNAVAILABLE -> R.string.advice_unavailable
    }
    val reason = when (kind) {
        AdviceKind.BRING_UMBRELLA -> stringResource(
            R.string.advice_bring_reason,
            probabilityLabel(requireNotNull(peak)),
            advice.thresholdPercent
        )
        AdviceKind.BELOW_THRESHOLD -> stringResource(
            R.string.advice_below_reason,
            probabilityLabel(requireNotNull(peak)),
            advice.thresholdPercent
        )
        AdviceKind.UNAVAILABLE -> stringResource(R.string.advice_unavailable_reason)
    }
    val container = if (isAlert) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val content = if (isAlert) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
    val accent = if (isAlert) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth().testTag("umbrella_advice"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content)
    ) {
        BoxWithConstraints(Modifier.padding(16.dp)) {
            val stacked = LocalDensity.current.fontScale >= 1.3f || maxWidth < 260.dp
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stacked) {
                    AdviceHeading(title, content, compact = true)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdviceSymbol(kind, accent)
                        Column(Modifier.weight(1f)) { AdviceHeading(title, content, compact = false) }
                    }
                }
                Text(reason, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AdviceHeading(title: Int, content: androidx.compose.ui.graphics.Color, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.visual_umbrella_check),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
            color = content
        )
        Text(
            stringResource(title),
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }.testTag("umbrella_decision")
        )
    }
}

@Composable
private fun AdviceSymbol(kind: AdviceKind, accent: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier.size(40.dp).background(accent.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        WeatherIcon(
            glyph = if (kind == AdviceKind.BELOW_THRESHOLD) WeatherGlyph.CHECK else WeatherGlyph.UMBRELLA,
            modifier = Modifier.size(28.dp),
            tint = accent
        )
    }
}

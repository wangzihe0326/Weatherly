package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
    Card(
        modifier = Modifier.fillMaxWidth().testTag("umbrella_advice"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (isAlert) MaterialTheme.colorScheme.onTertiaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.umbrella_heading), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(reason, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

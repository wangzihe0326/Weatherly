package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod

@Composable
fun RainForecastCard(periods: List<RainPeriod>, timezoneId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeading(stringResource(R.string.rain_heading))
            Text(
                stringResource(R.string.rain_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.city_time_zone, timezoneId),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (periods.isEmpty()) {
                Text(stringResource(R.string.rain_empty), style = MaterialTheme.typography.bodyLarge)
            } else {
                periods.forEachIndexed { index, period ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RainPeriodRow(period, timezoneId, index)
                }
                if (periods.size < 3 || periods.any { it.probabilityPercent == null }) {
                    Text(
                        stringResource(R.string.rain_partial),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RainPeriodRow(period: RainPeriod, timezoneId: String, index: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("rain_period_$index"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(periodLabel(period, timezoneId), style = MaterialTheme.typography.bodyLarge)
        val probability = period.probabilityPercent
        if (probability == null) {
            Text(
                stringResource(R.string.rain_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val label = probabilityLabel(probability)
            val accessibleLabel = stringResource(R.string.rain_probability_accessibility, label)
            Text(
                label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = accessibleLabel }
            )
            LinearProgressIndicator(
                progress = { (probability / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

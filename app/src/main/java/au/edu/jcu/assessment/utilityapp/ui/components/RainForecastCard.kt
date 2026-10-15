package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RainForecastCard(periods: List<RainPeriod>, timezoneId: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = LocalDensity.current.fontScale >= 1.3f || maxWidth < 300.dp
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.visual_hourly_forecast),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.3.sp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeatherIcon(WeatherGlyph.DROPLET, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    SectionHeading(stringResource(R.string.visual_rain_heading))
                }
                Text(
                    stringResource(R.string.city_time_zone, timezoneId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (periods.isEmpty()) {
                Text(stringResource(R.string.rain_empty), style = MaterialTheme.typography.bodyLarge)
            } else if (stacked) {
                periods.forEachIndexed { index, period ->
                    RainPeriodTile(period, timezoneId, index, compact = false)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    periods.forEachIndexed { index, period ->
                        RainPeriodTile(
                            period, timezoneId, index, compact = true,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.rain_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (periods.isNotEmpty() && (periods.size < 3 || periods.any { it.probabilityPercent == null })) {
                Text(
                    stringResource(R.string.rain_partial),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RainPeriodTile(
    period: RainPeriod,
    timezoneId: String,
    index: Int,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.of(timezoneId)
    val start = period.start.atZone(zone)
    val end = period.end.atZone(zone)
    val date = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    val time = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val dateLabel = if (start.toLocalDate() == end.toLocalDate()) start.format(date) else {
        stringResource(R.string.visual_interval_dates, start.format(date), end.format(date))
    }
    Card(
        modifier = modifier.testTag("rain_period_$index"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (index == 0) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
            contentColor = if (index == 0) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(if (compact) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (compact) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = periodLabel(period, timezoneId)
                    }
                ) {
                    Text(dateLabel, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    Text(start.format(time), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.visual_interval_end, end.format(time)), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(periodLabel(period, timezoneId), style = MaterialTheme.typography.bodyLarge)
            }
            val probability = period.probabilityPercent
            if (probability == null) {
                WeatherIcon(WeatherGlyph.DROPLET, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.rain_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = if (compact) TextAlign.Center else TextAlign.Start
                )
            } else {
                val label = probabilityLabel(probability)
                val accessibleLabel = stringResource(R.string.rain_probability_accessibility, label)
                if (compact) RainMeter(probability, vertical = true)
                Text(
                    label,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.semantics { contentDescription = accessibleLabel }
                )
                if (!compact) RainMeter(probability, vertical = false)
            }
        }
    }
}

/** The numeric label is authoritative; this redundant graphic has no accessibility node. */
@Composable
private fun RainMeter(probability: Double, vertical: Boolean) {
    val track = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val fill = MaterialTheme.colorScheme.primary
    val fraction = (probability / 100.0).toFloat().coerceIn(0f, 1f)
    Canvas(
        if (vertical) Modifier.width(28.dp).height(48.dp).clearAndSetSemantics {}
        else Modifier.fillMaxWidth().height(8.dp).clearAndSetSemantics {}
    ) {
        val corner = CornerRadius(if (vertical) 10.dp.toPx() else 4.dp.toPx())
        drawRoundRect(track, cornerRadius = corner)
        if (fraction > 0f) {
            val fillSize = if (vertical) Size(size.width, size.height * fraction) else Size(size.width * fraction, size.height)
            val origin = if (vertical) Offset(0f, size.height - fillSize.height) else Offset.Zero
            drawRoundRect(
                Brush.verticalGradient(listOf(fill.copy(alpha = 0.65f), fill)),
                topLeft = origin,
                size = fillSize,
                cornerRadius = corner
            )
        }
    }
}

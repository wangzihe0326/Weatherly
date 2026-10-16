package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.DailyForecast
import au.edu.jcu.assessment.utilityapp.ui.TemperatureUnit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A passive overview: the surrounding Weather screen owns scrolling and refresh. */
@Composable
fun DailyForecastCard(days: List<DailyForecast>, temperatureUnit: TemperatureUnit, today: LocalDate) {
    val lastDate = today.plusDays(6)
    val visibleDays = days
        .filter { !it.date.isBefore(today) && !it.date.isAfter(lastDate) }
        .sortedBy { it.date }
        .distinctBy { it.date }
    val validRanges = visibleDays.mapNotNull { it.validTemperatureRange() }
    val scaleMinimum = validRanges.minOfOrNull { it.start }
    val scaleMaximum = validRanges.maxOfOrNull { it.endInclusive }
    val hasMissingTemperatures = visibleDays.any {
        it.validTemperatureRange() == null
    }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("daily_forecast"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.daily_forecast_eyebrow),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.3.sp)
            )
            SectionHeading(stringResource(R.string.daily_forecast_heading))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (visibleDays.isEmpty()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.daily_forecast_empty),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.daily_forecast_empty_helper),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
                    val stacked = LocalDensity.current.fontScale >= 1.3f || maxWidth < 320.dp
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        visibleDays.forEachIndexed { index, day ->
                            DailyForecastRow(
                                day = day,
                                index = index,
                                today = today,
                                temperatureUnit = temperatureUnit,
                                scaleMinimum = scaleMinimum,
                                scaleMaximum = scaleMaximum,
                                stacked = stacked
                            )
                            if (index < visibleDays.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (visibleDays.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (visibleDays.size < 7) {
                    Text(
                        stringResource(R.string.daily_forecast_partial, visibleDays.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (hasMissingTemperatures) {
                    Text(
                        stringResource(R.string.daily_forecast_missing_temperature),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    stringResource(R.string.daily_forecast_probability_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailyForecastRow(
    day: DailyForecast,
    index: Int,
    today: LocalDate,
    temperatureUnit: TemperatureUnit,
    scaleMinimum: Double?,
    scaleMaximum: Double?,
    stacked: Boolean
) {
    val (low, high) = day.validTemperatures()
    val fullDate = day.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
    val dateDescription = when (day.date) {
        today, today.plusDays(1) -> stringResource(R.string.daily_forecast_date_description, dailyDateTitle(day.date, today), fullDate)
        else -> fullDate
    }
    val lowDescription = if (low == null) stringResource(R.string.daily_forecast_low_unavailable)
        else stringResource(R.string.daily_forecast_low_description, temperatureLabel(low, temperatureUnit))
    val highDescription = if (high == null) stringResource(R.string.daily_forecast_high_unavailable)
        else stringResource(R.string.daily_forecast_high_description, temperatureLabel(high, temperatureUnit))
    val probability = day.maxPrecipitationProbability?.takeIf { it.isFinite() && it in 0.0..100.0 }
    val rainDescription = if (probability == null) stringResource(R.string.daily_forecast_rain_unavailable)
        else stringResource(R.string.daily_forecast_peak_rain_description, probabilityLabel(probability))
    val description = stringResource(
        R.string.daily_forecast_description, dateDescription, day.weatherDescription,
        lowDescription, highDescription, rainDescription
    )
    // Today is contextual emphasis only. These rows deliberately have no click or selection semantics.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_day_$index")
            .clearAndSetSemantics { contentDescription = description }
            .background(
                if (day.date == today) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(if (stacked) 12.dp else 8.dp)
    ) {
        if (stacked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DailyDate(day.date, today)
                    Text(
                        day.weatherDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DailyPeakRain(day.maxPrecipitationProbability)
                }
                ForecastConditionIcon(
                    weatherCode = day.weatherCode,
                    isDay = null,
                    modifier = Modifier.size(40.dp).clearAndSetSemantics {}
                )
            }
            DailyTemperatures(day, index, temperatureUnit, scaleMinimum, scaleMaximum, Modifier.fillMaxWidth())
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DailyDate(day.date, today, Modifier.weight(1f))
                ForecastConditionIcon(
                    weatherCode = day.weatherCode,
                    isDay = null,
                    modifier = Modifier.size(32.dp).clearAndSetSemantics {}
                )
                DailyTemperatures(day, index, temperatureUnit, scaleMinimum, scaleMaximum, Modifier.weight(1.8f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    day.weatherDescription,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DailyPeakRain(day.maxPrecipitationProbability)
            }
        }
    }
}

@Composable
private fun DailyDate(date: LocalDate, today: LocalDate, modifier: Modifier = Modifier) {
    val locale = Locale.getDefault()
    val dayTitle = dailyDateTitle(date, today)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            dayTitle,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (date == today) FontWeight.SemiBold else FontWeight.Medium,
            color = if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            date.format(DateTimeFormatter.ofPattern("d MMM", locale)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun dailyDateTitle(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.daily_forecast_today)
    today.plusDays(1) -> stringResource(R.string.daily_forecast_tomorrow)
    else -> date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
}

@Composable
private fun DailyPeakRain(probability: Double?) {
    val validProbability = probability?.takeIf { it.isFinite() && it in 0.0..100.0 }
    Text(
        if (validProbability == null) stringResource(R.string.daily_forecast_rain_unavailable)
        else stringResource(R.string.daily_forecast_peak_rain, probabilityLabel(validProbability)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun DailyTemperatures(
    day: DailyForecast,
    index: Int,
    unit: TemperatureUnit,
    scaleMinimum: Double?,
    scaleMaximum: Double?,
    modifier: Modifier
) {
    val (validLow, validHigh) = day.validTemperatures()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DailyTemperatureValue(validLow, unit, isHigh = false, index = index)
        TemperatureRangeBar(
            range = day.validTemperatureRange(),
            scaleMinimum = scaleMinimum,
            scaleMaximum = scaleMaximum,
            modifier = Modifier.weight(1f).height(8.dp)
        )
        DailyTemperatureValue(validHigh, unit, isHigh = true, index = index)
    }
}

@Composable
private fun DailyTemperatureValue(celsius: Double?, unit: TemperatureUnit, isHigh: Boolean, index: Int) {
    val value = temperatureLabel(celsius, unit)
    val accessibleLabel = if (celsius == null) {
        stringResource(if (isHigh) R.string.daily_forecast_high_unavailable else R.string.daily_forecast_low_unavailable)
    } else {
        stringResource(if (isHigh) R.string.daily_forecast_high_description else R.string.daily_forecast_low_description, value)
    }
    Column(
        modifier = Modifier.widthIn(min = 40.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = if (isHigh) Alignment.End else Alignment.Start
    ) {
        Text(
            stringResource(if (isHigh) R.string.daily_forecast_high else R.string.daily_forecast_low),
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            modifier = Modifier
                .testTag(if (isHigh) "daily_high_$index" else "daily_low_$index")
                .semantics { contentDescription = accessibleLabel },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isHigh) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isHigh) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Decorative comparison only; every value is also printed with an explicit low/high label. */
@Composable
private fun TemperatureRangeBar(
    range: ClosedFloatingPointRange<Double>?,
    scaleMinimum: Double?,
    scaleMaximum: Double?,
    modifier: Modifier
) {
    val visualModifier = modifier.clearAndSetSemantics {}
    if (range == null || scaleMinimum == null || scaleMaximum == null) {
        Box(visualModifier)
        return
    }
    val span = scaleMaximum - scaleMinimum
    if (!span.isFinite() || span < 0) {
        Box(visualModifier)
        return
    }
    val startFraction = if (span == 0.0) 0.5f else ((range.start - scaleMinimum) / span).toFloat().coerceIn(0f, 1f)
    val endFraction = if (span == 0.0) 0.5f else ((range.endInclusive - scaleMinimum) / span).toFloat().coerceIn(0f, 1f)
    val track = MaterialTheme.colorScheme.outlineVariant
    val cool = MaterialTheme.colorScheme.primary
    val warm = MaterialTheme.colorScheme.tertiary
    Canvas(visualModifier) {
        val stroke = 6.dp.toPx().coerceAtMost(size.height)
        val inset = stroke / 2f
        val trackWidth = (size.width - stroke).coerceAtLeast(0f)
        val centerY = size.height / 2f
        drawLine(track, Offset(inset, centerY), Offset(size.width - inset, centerY), stroke, StrokeCap.Round)
        val start = Offset(inset + trackWidth * startFraction, centerY)
        val end = Offset(inset + trackWidth * endFraction, centerY)
        if (start == end) {
            drawCircle(cool, radius = inset, center = start)
        } else {
            drawLine(Brush.horizontalGradient(listOf(cool, warm)), start, end, stroke, StrokeCap.Round)
        }
    }
}

private fun DailyForecast.validTemperatureRange(): ClosedFloatingPointRange<Double>? {
    val (low, high) = validTemperatures()
    return if (low != null && high != null) low..high else null
}

private fun DailyForecast.validTemperatures(): Pair<Double?, Double?> {
    val low = temperatureMinCelsius?.takeIf { it.isFinite() }
    val high = temperatureMaxCelsius?.takeIf { it.isFinite() }
    return if (low != null && high != null && low > high) null to null else low to high
}

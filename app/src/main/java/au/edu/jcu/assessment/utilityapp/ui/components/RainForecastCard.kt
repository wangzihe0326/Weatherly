package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.ui.TemperatureUnit
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun RainForecastCard(
    periods: List<RainPeriod>,
    timezoneId: String,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    browsingState: HourlyBrowsingState = rememberHourlyBrowsingState(periods, timezoneId)
) {
    // A timestamp follows the same hour when refreshing advances the window.
    val selectedIndex = periods.indexOfFirst { it.start.epochSecond == browsingState.selectedStart }.coerceAtLeast(0)
    val selectedPeriod = periods.getOrNull(selectedIndex)
    val listState = browsingState.listState
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.hourly_forecast_eyebrow),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.3.sp)
            )
            SectionHeading(stringResource(R.string.hourly_forecast_heading))
            Text(
                stringResource(R.string.city_time_zone, timezoneId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (periods.isNotEmpty()) {
                Text(
                    stringResource(R.string.hourly_forecast_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (periods.isEmpty()) {
            Text(stringResource(R.string.hourly_forecast_empty), style = MaterialTheme.typography.bodyLarge)
        } else {
            val fontScale = LocalDensity.current.fontScale
            val tileWidth = 104.dp * fontScale.coerceIn(1f, 2f)
            LazyRow(
                modifier = Modifier.fillMaxWidth().testTag("hourly_list").selectableGroup(),
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(periods, key = { _, period -> period.start.epochSecond }) { index, period ->
                    HourlyPeriodTile(
                        period = period,
                        timezoneId = timezoneId,
                        temperatureUnit = temperatureUnit,
                        index = index,
                        selected = index == selectedIndex,
                        onSelect = { browsingState.selectedStart = period.start.epochSecond },
                        modifier = Modifier.width(tileWidth)
                    )
                }
            }
            HourlyNavigation(
                selected = selectedIndex + 1,
                count = periods.size,
                canGoBack = selectedIndex > 0,
                canGoForward = selectedIndex < periods.lastIndex,
                onPrevious = {
                    val index = selectedIndex - 1
                    browsingState.selectedStart = periods[index].start.epochSecond
                    scope.launch { listState.scrollToItem(index) }
                },
                onNext = {
                    val index = selectedIndex + 1
                    browsingState.selectedStart = periods[index].start.epochSecond
                    scope.launch { listState.scrollToItem(index) }
                }
            )
        }
        if (selectedPeriod != null) SelectedHourDetails(selectedPeriod, timezoneId, temperatureUnit)
        Text(
            stringResource(R.string.hourly_forecast_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (periods.isNotEmpty() && periods.size < 24) {
            Text(
                stringResource(R.string.hourly_forecast_short, periods.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (periods.any { it.probabilityPercent == null || it.temperatureCelsius == null || it.weatherCode == null }) {
            Text(
                stringResource(R.string.hourly_forecast_partial),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HourlyNavigation(
    selected: Int,
    count: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val previousLabel = stringResource(R.string.hourly_previous_accessibility)
    val nextLabel = stringResource(R.string.hourly_next_accessibility)
    val rangeLabel = stringResource(R.string.hourly_range_accessibility, selected, count)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.hourly_range, selected, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().testTag("hourly_range").semantics { contentDescription = rangeLabel },
            textAlign = TextAlign.Center
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = canGoBack,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("hourly_previous")
                    .semantics { contentDescription = previousLabel }
            ) {
                WeatherIcon(WeatherGlyph.ARROW_RIGHT, Modifier.size(18.dp).rotate(180f))
                Text(stringResource(R.string.hourly_previous_label), Modifier.padding(start = 6.dp))
            }
            OutlinedButton(
                onClick = onNext,
                enabled = canGoForward,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("hourly_next")
                    .semantics { contentDescription = nextLabel }
            ) {
                Text(stringResource(R.string.hourly_next_label), Modifier.padding(end = 6.dp))
                WeatherIcon(WeatherGlyph.ARROW_RIGHT, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun HourlyPeriodTile(
    period: RainPeriod,
    timezoneId: String,
    temperatureUnit: TemperatureUnit,
    index: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val start = period.start.atZone(ZoneId.of(timezoneId))
    val date = start.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    val time = start.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    val interval = periodLabel(period, timezoneId)
    val temperature = temperatureLabel(period.temperatureCelsius, temperatureUnit)
    val conditionDescription = weatherDescription(period.condition)
    val temperatureDescription = if (period.temperatureCelsius == null) {
        stringResource(R.string.hourly_temperature_missing)
    } else stringResource(R.string.hourly_temperature, temperature)
    val rainDescription = period.probabilityPercent?.let {
        stringResource(R.string.rain_probability_accessibility, probabilityLabel(it))
    } ?: stringResource(R.string.rain_unavailable)
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier = modifier.testTag("rain_period_$index").clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        shape = shape,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 216.dp).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clearAndSetSemantics { contentDescription = interval }
            ) {
                Text(date, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                Text(time, style = MaterialTheme.typography.titleMedium)
            }
            Box(Modifier.semantics { contentDescription = conditionDescription }) {
                ForecastConditionIcon(period.weatherCode, Modifier.size(42.dp), isDay = period.isDay)
            }
            Text(
                temperature,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.testTag("hourly_temperature_$index").semantics { contentDescription = temperatureDescription }
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WeatherIcon(WeatherGlyph.DROPLET, Modifier.size(14.dp))
                Text(
                    period.probabilityPercent?.let { probabilityLabel(it) } ?: stringResource(R.string.temperature_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { contentDescription = rainDescription }
                )
            }
            Text(
                stringResource(if (selected) R.string.rain_period_selected else R.string.rain_period_view),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clearAndSetSemantics {}
            )
        }
    }
}

@Composable
private fun SelectedHourDetails(period: RainPeriod, timezoneId: String, temperatureUnit: TemperatureUnit) {
    val conditionDescription = weatherDescription(period.condition)
    val temperatureMissingLabel = stringResource(R.string.hourly_temperature_missing)
    val temperature = temperatureLabel(period.temperatureCelsius, temperatureUnit)
    val temperatureDescription = if (period.temperatureCelsius == null) temperatureMissingLabel
        else stringResource(R.string.hourly_temperature, temperature)
    val probabilityDescription = period.probabilityPercent?.let {
        stringResource(R.string.rain_selected_probability, probabilityLabel(it))
    } ?: stringResource(R.string.rain_selected_unavailable)
    val description = stringResource(
        R.string.hourly_selected_description,
        periodLabel(period, timezoneId),
        conditionDescription,
        temperatureDescription,
        probabilityDescription
    )
    Card(
        modifier = Modifier.fillMaxWidth().testTag("hourly_details").clearAndSetSemantics {
            contentDescription = description
            liveRegion = LiveRegionMode.Polite
        },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.hourly_selected_heading), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                periodLabel(period, timezoneId),
                modifier = Modifier.testTag("rain_selected_period"),
                style = MaterialTheme.typography.titleMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ForecastConditionIcon(period.weatherCode, Modifier.size(36.dp), isDay = period.isDay)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(conditionDescription, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        temperature,
                        modifier = Modifier.testTag("rain_selected_temperature").semantics {
                            if (period.temperatureCelsius == null) contentDescription = temperatureMissingLabel
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            Text(
                probabilityDescription,
                modifier = Modifier.testTag("rain_selected_probability"),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

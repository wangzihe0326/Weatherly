package au.edu.jcu.assessment.utilityapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.ui.UtilityUiState
import au.edu.jcu.assessment.utilityapp.ui.components.AdviceCard
import au.edu.jcu.assessment.utilityapp.ui.components.DailyForecastCard
import au.edu.jcu.assessment.utilityapp.ui.components.RainForecastCard
import au.edu.jcu.assessment.utilityapp.ui.components.ScreenContent
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherSummaryCard
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherGlyph
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherIcon
import au.edu.jcu.assessment.utilityapp.ui.components.cityLabel
import au.edu.jcu.assessment.utilityapp.ui.components.modelTimeLabel
import java.time.ZoneId

@Composable
fun UtilityScreen(state: UtilityUiState, onRefresh: () -> Unit) {
    ScreenContent(tag = "weather_screen") {
        val refreshLabel = stringResource(R.string.refresh_weather)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.weather_heading),
                style = if (LocalDensity.current.fontScale >= 1.3f) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            IconButton(
                onClick = onRefresh,
                enabled = !state.isLoading,
                modifier = Modifier.size(48.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .semantics { contentDescription = refreshLabel }
                    .testTag("refresh_button")
            ) {
                WeatherIcon(
                    WeatherGlyph.REFRESH,
                    tint = if (state.isLoading) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary
                )
            }
        }

        val weather = state.weather
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(
                        if (weather == null) R.string.loading_title else R.string.refreshing_weather
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                if (weather == null) {
                    Text(
                        stringResource(R.string.loading_body, cityLabel(state.settings.city)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
        if (state.errorMessage != null) {
            StatusCard(
                title = stringResource(
                    if (weather == null) R.string.error_title else R.string.refresh_error_title
                ),
                message = state.errorMessage,
                extraMessage = if (weather != null) stringResource(R.string.refresh_error_body) else null,
                isError = true,
                showRetry = !state.isLoading,
                onRetry = onRefresh
            )
        }
        if (weather == null && !state.isLoading && state.errorMessage == null) {
            StatusCard(
                title = stringResource(R.string.empty_title),
                message = stringResource(R.string.empty_body, cityLabel(state.settings.city)),
                showRetry = true,
                onRetry = onRefresh
            )
        }
        if (weather != null) {
            if (state.isStale) {
                StatusCard(
                    title = stringResource(R.string.stale_title),
                    message = stringResource(R.string.stale_body)
                )
            }
            WeatherSummaryCard(weather = weather, temperatureUnit = state.settings.temperatureUnit)
            if (state.settings.umbrellaEnabled) AdviceCard(state.advice)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.fetched_at, modelTimeLabel(weather.fetchedAt, weather.timezoneId)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("forecast_fetched_at")
                )
                Text(
                    weather.observationTime?.let {
                        stringResource(R.string.model_time, modelTimeLabel(it, weather.timezoneId))
                    } ?: stringResource(R.string.model_time_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            key(weather.city) {
                RainForecastCard(
                    periods = state.hourlyPeriods,
                    timezoneId = weather.timezoneId,
                    temperatureUnit = state.settings.temperatureUnit
                )
            }
            DailyForecastCard(
                days = state.dailyForecast,
                temperatureUnit = state.settings.temperatureUnit,
                today = state.now.atZone(ZoneId.of(weather.timezoneId)).toLocalDate()
            )
        }
        DataAttribution()
    }
}

@Composable
private fun StatusCard(
    title: String,
    message: String,
    extraMessage: String? = null,
    isError: Boolean = false,
    showRetry: Boolean = false,
    onRetry: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WeatherIcon(if (isError) WeatherGlyph.CLOUD else WeatherGlyph.CLOCK)
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(message, style = MaterialTheme.typography.bodyLarge)
            if (extraMessage != null) Text(extraMessage, style = MaterialTheme.typography.bodyMedium)
            if (showRetry) {
                Button(onClick = onRetry, modifier = Modifier.testTag("retry_button")) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
private fun DataAttribution() {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.forecast_source_eyebrow),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            stringResource(R.string.data_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val provider: @Composable () -> Unit = {
            TextButton(onClick = { uriHandler.openUri("https://open-meteo.com/") }) {
                Text(stringResource(R.string.source_provider_label))
            }
        }
        val licence: @Composable () -> Unit = {
            TextButton(onClick = { uriHandler.openUri("https://creativecommons.org/licenses/by/4.0/") }) {
                Text(stringResource(R.string.source_licence_label))
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 320.dp || LocalDensity.current.fontScale > 1.3f) {
                Column { provider(); licence() }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { provider(); licence() }
            }
        }
    }
}

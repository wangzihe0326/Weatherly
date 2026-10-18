package au.edu.jcu.assessment.utilityapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
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
import au.edu.jcu.assessment.utilityapp.ui.components.ForecastScreenContent
import au.edu.jcu.assessment.utilityapp.ui.components.rememberHourlyBrowsingState
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherSummaryCard
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherGlyph
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherIcon
import au.edu.jcu.assessment.utilityapp.ui.components.cityLabel
import au.edu.jcu.assessment.utilityapp.ui.components.modelTimeLabel
import java.time.ZoneId
import java.time.Duration

@Composable
fun UtilityScreen(state: UtilityUiState, onRefresh: () -> Unit) {
    val weather = state.weather
    val hourlyBrowsing = rememberHourlyBrowsingState(state.hourlyPeriods, weather?.timezoneId ?: "UTC")
    ForecastScreenContent {
        item(key = "header") {
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
        }
        if (state.isLoading || state.errorMessage != null || state.isStale) {
            item(key = "status") { RefreshStatus(state, onRefresh) }
        }
        if (weather == null && !state.isLoading && state.errorMessage == null) {
            item(key = "empty") {
                StatusCard(
                    title = stringResource(R.string.empty_title),
                    message = stringResource(R.string.empty_body, cityLabel(state.settings.city)),
                    showRetry = true,
                    onRetry = onRefresh
                )
            }
        }
        if (weather != null) {
            item(key = "summary") {
                WeatherSummaryCard(weather = weather, temperatureUnit = state.settings.temperatureUnit)
            }
            if (state.settings.umbrellaEnabled) {
                item(key = "advice") { AdviceCard(state.advice, weather.timezoneId) }
            }
            item(key = "timestamps") {
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
            }
            item(key = "hourly") {
                Box(Modifier.testTag("weather_hourly_section")) {
                    RainForecastCard(
                        periods = state.hourlyPeriods,
                        timezoneId = weather.timezoneId,
                        temperatureUnit = state.settings.temperatureUnit,
                        browsingState = hourlyBrowsing
                    )
                }
            }
            item(key = "daily") {
                Box(Modifier.testTag("weather_daily_section")) {
                    DailyForecastCard(
                        days = state.dailyForecast,
                        temperatureUnit = state.settings.temperatureUnit,
                        today = state.now.atZone(ZoneId.of(weather.timezoneId)).toLocalDate()
                    )
                }
            }
        }
        item(key = "attribution") {
            Box(Modifier.testTag("weather_attribution_section")) { DataAttribution() }
        }
    }
}

@Composable
private fun RefreshStatus(state: UtilityUiState, onRefresh: () -> Unit) {
    val weather = state.weather
    val error = state.errorMessage
    val title = stringResource(when {
        state.isLoading -> if (weather == null) R.string.loading_title else R.string.refreshing_weather
        error != null -> if (weather == null) R.string.error_title else R.string.refresh_error_title
        else -> R.string.stale_title
    })
    val message = when {
        state.isLoading && weather == null -> stringResource(R.string.loading_body, cityLabel(state.settings.city))
        state.isLoading -> stringResource(R.string.refresh_keep_browsing)
        error != null -> error
        else -> stringResource(R.string.stale_body)
    }
    val recovery = when {
        state.isLoading -> null
        state.automaticRetriesExhausted -> stringResource(R.string.refresh_retries_paused)
        state.nextRetryAt != null -> {
            val minutes = ((Duration.between(state.now, state.nextRetryAt).seconds.coerceAtLeast(0) + 59) / 60)
                .coerceAtLeast(1)
            stringResource(R.string.refresh_retry_scheduled, minutes)
        }
        else -> null
    }
    StatusCard(
        modifier = Modifier.testTag("refresh_status"),
        title = title,
        message = message,
        extraMessage = listOfNotNull(
            if (weather != null && error != null) stringResource(
                if (state.isStale) R.string.refresh_old_forecast else R.string.refresh_error_body
            ) else null,
            recovery
        ).takeIf { it.isNotEmpty() }?.joinToString("\n\n"),
        isError = error != null && !state.isLoading,
        showRetry = !state.isLoading,
        isLoading = state.isLoading,
        onRetry = onRefresh
    )
}

@Composable
private fun StatusCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    extraMessage: String? = null,
    isError: Boolean = false,
    showRetry: Boolean = false,
    isLoading: Boolean = false,
    onRetry: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherIcon(if (isLoading) WeatherGlyph.REFRESH else if (isError) WeatherGlyph.CLOUD else WeatherGlyph.CLOCK)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
            }
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (extraMessage != null) Text(extraMessage, style = MaterialTheme.typography.bodyMedium)
            if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
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

package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.ui.TemperatureUnit

@Composable
fun WeatherSummaryCard(weather: WeatherSnapshot, temperatureUnit: TemperatureUnit) {
    val unavailable = stringResource(R.string.temperature_unavailable_description)
    Card(
        modifier = Modifier.fillMaxWidth().testTag("weather_summary"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.current_conditions), style = MaterialTheme.typography.labelLarge)
            Text(
                cityLabel(weather.city),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }.testTag("weather_city")
            )
            // A single column lets long descriptions and large system text wrap naturally.
            Text(
                temperatureLabel(weather.temperatureCelsius, temperatureUnit),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if (LocalDensity.current.fontScale >= 1.3f) 42.sp else 60.sp,
                    lineHeight = if (LocalDensity.current.fontScale >= 1.3f) 50.sp else 68.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-2).sp
                ),
                modifier = Modifier.testTag("current_temperature").semantics {
                    if (weather.temperatureCelsius == null) contentDescription = unavailable
                }
            )
            Text(weather.weatherDescription, style = MaterialTheme.typography.titleLarge)
            Text(
                if (weather.apparentTemperatureCelsius == null) {
                    stringResource(R.string.feels_like_unavailable)
                } else {
                    stringResource(
                        R.string.feels_like,
                        temperatureLabel(weather.apparentTemperatureCelsius, temperatureUnit)
                    )
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("feels_like_temperature")
            )
            Text(
                weather.observationTime?.let {
                    stringResource(R.string.model_time, modelTimeLabel(it, weather.timezoneId))
                } ?: stringResource(R.string.model_time_unavailable),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

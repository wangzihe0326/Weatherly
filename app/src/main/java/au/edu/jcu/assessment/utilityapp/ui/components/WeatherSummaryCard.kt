package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.ui.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.ui.theme.LocalWeatherColors

@Composable
fun WeatherSummaryCard(weather: WeatherSnapshot, temperatureUnit: TemperatureUnit) {
    val palette = LocalWeatherColors.current
    Card(
        modifier = Modifier.fillMaxWidth().testTag("weather_summary"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = palette.heroStart, contentColor = palette.onHero)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd)))
                .drawBehind {
                    val center = Offset(size.width * 0.92f, size.height * 0.38f)
                    drawCircle(palette.onHero.copy(alpha = 0.025f), size.width * 0.63f, center)
                    drawCircle(palette.onHero.copy(alpha = 0.045f), size.width * 0.81f, center, style = Stroke(1f))
                    drawCircle(palette.onHero.copy(alpha = 0.035f), size.width * 0.97f, center, style = Stroke(1f))
                }
                .padding(24.dp)
        ) {
            val contentWidth = maxWidth
            val stacked = LocalDensity.current.fontScale >= 1.3f || contentWidth < 280.dp
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.visual_current_conditions),
                        color = palette.heroMuted,
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeatherIcon(WeatherGlyph.LOCATION, Modifier.size(20.dp), tint = palette.heroMuted)
                        Text(
                            cityLabel(weather.city),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.weight(1f).semantics { heading() }.testTag("weather_city")
                        )
                    }
                }
                if (stacked) {
                    WeatherArtwork(weather.weatherDescription, Modifier.size(116.dp).align(Alignment.CenterHorizontally))
                    TemperatureAndCondition(weather, temperatureUnit, compact = true)
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            TemperatureAndCondition(weather, temperatureUnit, compact = contentWidth < 330.dp)
                        }
                        WeatherArtwork(weather.weatherDescription, Modifier.size(if (contentWidth < 330.dp) 122.dp else 148.dp))
                    }
                }
                HorizontalDivider(color = palette.onHero.copy(alpha = 0.16f))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeatherIcon(WeatherGlyph.THERMOMETER, Modifier.size(18.dp), tint = palette.heroMuted)
                        Text(
                            if (weather.apparentTemperatureCelsius == null) {
                                stringResource(R.string.feels_like_unavailable)
                            } else {
                                stringResource(R.string.feels_like, temperatureLabel(weather.apparentTemperatureCelsius, temperatureUnit))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("feels_like_temperature")
                        )
                    }
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeatherIcon(WeatherGlyph.CLOCK, Modifier.size(18.dp), tint = palette.heroMuted)
                        Text(
                            weather.observationTime?.let {
                                stringResource(R.string.model_time, modelTimeLabel(it, weather.timezoneId))
                            } ?: stringResource(R.string.model_time_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.heroMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemperatureAndCondition(weather: WeatherSnapshot, temperatureUnit: TemperatureUnit, compact: Boolean) {
    val palette = LocalWeatherColors.current
    val unavailable = stringResource(R.string.temperature_unavailable_description)
    val temperature = temperatureLabel(weather.temperatureCelsius, temperatureUnit)
    val numberEnd = temperature.indexOf('°').takeIf { it >= 0 } ?: temperature.length
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            buildAnnotatedString {
                append(temperature.substring(0, numberEnd))
                withStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)) {
                    append(temperature.substring(numberEnd))
                }
            },
            color = palette.onHero,
            style = if (compact) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
            modifier = Modifier.testTag("current_temperature").semantics {
                if (weather.temperatureCelsius == null) contentDescription = unavailable
            }
        )
        Text(weather.weatherDescription, style = MaterialTheme.typography.titleMedium, color = palette.onHero)
    }
}

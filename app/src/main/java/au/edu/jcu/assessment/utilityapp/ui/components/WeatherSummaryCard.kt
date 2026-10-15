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
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Card(
        modifier = Modifier.fillMaxWidth().testTag("weather_summary"),
        shape = RoundedCornerShape(28.dp),
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
                .padding(if (largeText) 16.dp else 20.dp)
        ) {
            val showArtwork = !largeText && maxWidth >= 280.dp
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeatherIcon(WeatherGlyph.LOCATION, Modifier.size(20.dp), tint = palette.heroMuted)
                    Text(
                        cityLabel(weather.city),
                        style = if (largeText) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f).semantics { heading() }.testTag("weather_city")
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        TemperatureAndCondition(weather, temperatureUnit, largeText)
                    }
                    // Decorative artwork yields space to the decision-making text as fonts grow.
                    if (showArtwork) {
                        WeatherArtwork(
                            weather.weatherDescription,
                            Modifier.size(96.dp).testTag("summary_artwork")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemperatureAndCondition(weather: WeatherSnapshot, temperatureUnit: TemperatureUnit, largeText: Boolean) {
    val palette = LocalWeatherColors.current
    val unavailable = stringResource(R.string.temperature_unavailable_description)
    val temperature = temperatureLabel(weather.temperatureCelsius, temperatureUnit)
    val numberEnd = temperature.indexOf('°').takeIf { it >= 0 } ?: temperature.length
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            buildAnnotatedString {
                append(temperature.substring(0, numberEnd))
                withStyle(SpanStyle(fontSize = if (largeText) 22.sp else 26.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)) {
                    append(temperature.substring(numberEnd))
                }
            },
            color = palette.onHero,
            style = if (largeText) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium,
            modifier = Modifier.testTag("current_temperature").semantics {
                if (weather.temperatureCelsius == null) contentDescription = unavailable
            }
        )
        Text(weather.weatherDescription, style = MaterialTheme.typography.titleMedium, color = palette.onHero)
        Text(
            if (weather.apparentTemperatureCelsius == null) {
                stringResource(R.string.feels_like_unavailable)
            } else {
                stringResource(R.string.feels_like, temperatureLabel(weather.apparentTemperatureCelsius, temperatureUnit))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = palette.heroMuted,
            modifier = Modifier.testTag("feels_like_temperature")
        )
    }
}

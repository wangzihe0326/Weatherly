package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.ui.TemperatureUnit
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun cityLabel(city: City): String = stringResource(
    when (city) {
        City.SINGAPORE -> R.string.city_singapore
        City.TOWNSVILLE -> R.string.city_townsville
        City.CAIRNS -> R.string.city_cairns
    }
)

@Composable
fun temperatureLabel(celsius: Double?, unit: TemperatureUnit): String {
    if (celsius == null || !celsius.isFinite()) {
        return stringResource(R.string.temperature_unavailable)
    }
    val temperature = unit.fromCelsius(celsius)
    val number = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 0 }
        .format(temperature)
    return stringResource(
        if (unit == TemperatureUnit.CELSIUS) R.string.temperature_celsius
        else R.string.temperature_fahrenheit,
        number
    )
}

@Composable
fun probabilityLabel(probability: Double): String = stringResource(
    R.string.rain_percent,
    NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(probability)
)

fun modelTimeLabel(time: Instant, timezoneId: String): String =
    DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.getDefault())
        .withZone(ZoneId.of(timezoneId))
        .format(time)

fun periodLabel(period: RainPeriod, timezoneId: String): String {
    val zone = ZoneId.of(timezoneId)
    val start = period.start.atZone(zone)
    val end = period.end.atZone(zone)
    val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    return if (start.toLocalDate() == end.toLocalDate()) {
        "${start.format(dateFormat)} · ${start.format(timeFormat)}–${end.format(timeFormat)}"
    } else {
        "${start.format(dateFormat)}, ${start.format(timeFormat)}–" +
            "${end.format(dateFormat)}, ${end.format(timeFormat)}"
    }
}

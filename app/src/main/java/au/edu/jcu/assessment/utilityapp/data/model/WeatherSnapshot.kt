package au.edu.jcu.assessment.utilityapp.data.model

import java.time.Instant
import java.time.LocalDate

/** Probability covers [start]–[end]; temperature and conditions are valid at [start]. */
data class RainPeriod(
    val start: Instant,
    val end: Instant,
    val probabilityPercent: Double? = null,
    val temperatureCelsius: Double? = null,
    val weatherCode: Int? = null,
    val weatherDescription: String = "Weather unavailable",
    val isDay: Boolean? = null
)

data class DailyForecast(
    val date: LocalDate,
    val temperatureMinCelsius: Double? = null,
    val temperatureMaxCelsius: Double? = null,
    val weatherCode: Int? = null,
    val weatherDescription: String = "Weather unavailable",
    val maxPrecipitationProbability: Double? = null
)

/** Temperatures remain in Celsius so changing display units needs no network request. */
data class WeatherSnapshot(
    val city: City,
    val temperatureCelsius: Double? = null,
    val apparentTemperatureCelsius: Double? = null,
    val weatherDescription: String = "Weather unavailable",
    val observationTime: Instant? = null,
    val fetchedAt: Instant,
    val timezoneId: String,
    val forecastPeriods: List<RainPeriod> = emptyList(),
    val dailyForecast: List<DailyForecast> = emptyList(),
    val weatherCode: Int? = null,
    val isDay: Boolean? = null
)

/** A partial hourly or daily forecast is useful even when current conditions are missing. */
fun WeatherSnapshot.hasMeaningfulData(): Boolean =
    temperatureCelsius.isUsableTemperature() || apparentTemperatureCelsius.isUsableTemperature() ||
        weatherDescription.isUsableDescription() ||
        forecastPeriods.any {
            it.temperatureCelsius.isUsableTemperature() || it.probabilityPercent.isUsableProbability() ||
                it.weatherCode != null || it.weatherDescription.isUsableDescription()
        } || dailyForecast.any {
            it.temperatureMinCelsius.isUsableTemperature() || it.temperatureMaxCelsius.isUsableTemperature() ||
                it.maxPrecipitationProbability.isUsableProbability() ||
                it.weatherCode != null || it.weatherDescription.isUsableDescription()
        }

private fun Double?.isUsableTemperature() = this != null && isFinite()
private fun Double?.isUsableProbability() = this != null && isFinite() && this in 0.0..100.0
private fun String.isUsableDescription() = isNotBlank() && this != "Weather unavailable"

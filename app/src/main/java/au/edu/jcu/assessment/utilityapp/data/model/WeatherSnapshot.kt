package au.edu.jcu.assessment.utilityapp.data.model

import java.time.Instant
import java.time.LocalDate

/** Probability covers [start]–[end]; temperature and conditions are valid at [start]. */
data class RainPeriod(
    val start: Instant,
    val end: Instant,
    val probabilityPercent: Double? = null,
    val temperatureCelsius: Double? = null,
    val condition: WeatherCondition = WeatherCondition.UNAVAILABLE,
    val isDay: Boolean? = null
) {
    val weatherCode: Int? get() = condition.wmoCode
}

data class DailyForecast(
    val date: LocalDate,
    val temperatureMinCelsius: Double? = null,
    val temperatureMaxCelsius: Double? = null,
    val condition: WeatherCondition = WeatherCondition.UNAVAILABLE,
    val maxPrecipitationProbability: Double? = null
) {
    val weatherCode: Int? get() = condition.wmoCode
}

/** Temperatures remain in Celsius so changing display units needs no network request. */
data class WeatherSnapshot(
    val city: City,
    val temperatureCelsius: Double? = null,
    val apparentTemperatureCelsius: Double? = null,
    val condition: WeatherCondition = WeatherCondition.UNAVAILABLE,
    val observationTime: Instant? = null,
    val fetchedAt: Instant,
    val timezoneId: String,
    val forecastPeriods: List<RainPeriod> = emptyList(),
    val dailyForecast: List<DailyForecast> = emptyList(),
    val isDay: Boolean? = null
) {
    val weatherCode: Int? get() = condition.wmoCode
}

/** A partial hourly or daily forecast is useful even when current conditions are missing. */
fun WeatherSnapshot.hasMeaningfulData(): Boolean =
    temperatureCelsius.isUsableTemperature() || apparentTemperatureCelsius.isUsableTemperature() ||
        condition != WeatherCondition.UNAVAILABLE ||
        forecastPeriods.any {
            it.temperatureCelsius.isUsableTemperature() || it.probabilityPercent.isUsableProbability() ||
                it.condition != WeatherCondition.UNAVAILABLE
        } || dailyForecast.any {
            it.temperatureMinCelsius.isUsableTemperature() || it.temperatureMaxCelsius.isUsableTemperature() ||
                it.maxPrecipitationProbability.isUsableProbability() ||
                it.condition != WeatherCondition.UNAVAILABLE
        }

private fun Double?.isUsableTemperature() = this != null && isFinite()
private fun Double?.isUsableProbability() = this != null && isFinite() && this in 0.0..100.0

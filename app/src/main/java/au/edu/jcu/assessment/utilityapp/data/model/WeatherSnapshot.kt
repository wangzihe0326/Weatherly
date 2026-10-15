package au.edu.jcu.assessment.utilityapp.data.model

import java.time.Instant

/** The probability covers the hourly interval ending at [end]. */
data class RainPeriod(
    val start: Instant,
    val end: Instant,
    val probabilityPercent: Double? = null
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
    val forecastPeriods: List<RainPeriod> = emptyList()
)

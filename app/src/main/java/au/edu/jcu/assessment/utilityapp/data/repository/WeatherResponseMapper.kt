package au.edu.jcu.assessment.utilityapp.data.repository

import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.data.remote.WeatherResponse
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId

/**
 * Maps a response without depending on the device timezone or dropping old periods.
 * The UI selects upcoming periods against its own current clock as time advances.
 */
fun mapWeatherResponse(
    response: WeatherResponse,
    city: City,
    fetchedAt: Instant
): WeatherSnapshot {
    val timezone = response.timezone
        ?: throw InvalidWeatherResponseException("The weather response has no timezone.")
    val timezoneId = try {
        ZoneId.of(timezone).id
    } catch (exception: DateTimeException) {
        throw InvalidWeatherResponseException("The weather response has an invalid timezone.", exception)
    }

    val current = response.current
    val temperature = current?.temperatureCelsius?.takeIf { it.isFinite() }
    val apparentTemperature = current?.apparentTemperatureCelsius?.takeIf { it.isFinite() }
    val description = weatherDescription(current?.weatherCode)
    val hourly = response.hourly

    // Pair by the ORIGINAL index: filtering invalid timestamps must not shift probabilities.
    val periods = hourly?.time.orEmpty().mapIndexedNotNull { index, timestamp ->
        val end = timestamp.toWeatherInstant() ?: return@mapIndexedNotNull null
        val probability = hourly?.precipitationProbability?.getOrNull(index)
            ?.takeIf { it.isFinite() && it in 0.0..100.0 }
        // Open-Meteo's probability at T covers the preceding hour, not the following hour.
        RainPeriod(start = end.minusSeconds(3600), end = end, probabilityPercent = probability)
    }.sortedBy { it.end }.distinctBy { it.end }

    if (temperature == null && apparentTemperature == null && description == null &&
        periods.none { it.probabilityPercent != null }
    ) {
        throw EmptyWeatherException()
    }

    return WeatherSnapshot(
        city = city,
        temperatureCelsius = temperature,
        apparentTemperatureCelsius = apparentTemperature,
        weatherDescription = description ?: "Weather unavailable",
        observationTime = current?.time.toWeatherInstant(),
        fetchedAt = fetchedAt,
        timezoneId = timezoneId,
        forecastPeriods = periods
    )
}

private fun Long?.toWeatherInstant(): Instant? {
    if (this == null || this <= 0) return null
    return try {
        Instant.ofEpochSecond(this)
    } catch (_: DateTimeException) {
        null
    }
}

/** Descriptions from https://open-meteo.com/en/docs; unknown codes remain unavailable. */
private fun weatherDescription(code: Int?): String? = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45 -> "Fog"
    48 -> "Depositing rime fog"
    51 -> "Light drizzle"
    53 -> "Moderate drizzle"
    55 -> "Dense drizzle"
    56 -> "Light freezing drizzle"
    57 -> "Dense freezing drizzle"
    61 -> "Slight rain"
    63 -> "Moderate rain"
    65 -> "Heavy rain"
    66 -> "Light freezing rain"
    67 -> "Heavy freezing rain"
    71 -> "Slight snowfall"
    73 -> "Moderate snowfall"
    75 -> "Heavy snowfall"
    77 -> "Snow grains"
    80 -> "Slight rain showers"
    81 -> "Moderate rain showers"
    82 -> "Violent rain showers"
    85 -> "Slight snow showers"
    86 -> "Heavy snow showers"
    95 -> "Thunderstorm"
    96 -> "Thunderstorm with slight hail"
    97 -> "Heavy thunderstorm"
    99 -> "Thunderstorm with heavy hail"
    else -> null
}

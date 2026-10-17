package au.edu.jcu.assessment.utilityapp.data.repository

import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.DailyForecast
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.data.model.hasMeaningfulData
import au.edu.jcu.assessment.utilityapp.data.remote.DailyWeatherResponse
import au.edu.jcu.assessment.utilityapp.data.remote.HourlyWeatherResponse
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
    val zone = try {
        ZoneId.of(timezone)
    } catch (exception: DateTimeException) {
        throw InvalidWeatherResponseException("The weather response has an invalid timezone.", exception)
    }

    val current = response.current
    val temperature = current?.temperatureCelsius?.takeIf { it.isFinite() }
    val apparentTemperature = current?.apparentTemperatureCelsius?.takeIf { it.isFinite() }
    val code = current?.weatherCode.knownCodeOrNull()
    val description = weatherDescription(code)

    val snapshot = WeatherSnapshot(
        city = city,
        temperatureCelsius = temperature,
        apparentTemperatureCelsius = apparentTemperature,
        weatherDescription = description ?: "Weather unavailable",
        observationTime = current?.time.toWeatherInstant(),
        fetchedAt = fetchedAt,
        timezoneId = zone.id,
        forecastPeriods = mapHourlyPeriods(response.hourly),
        dailyForecast = mapDailyForecast(response.daily, zone),
        weatherCode = code,
        isDay = current?.isDay.daylightOrNull()
    )
    if (!snapshot.hasMeaningfulData()) throw EmptyWeatherException()
    return snapshot
}

private fun mapHourlyPeriods(hourly: HourlyWeatherResponse?): List<RainPeriod> {
    if (hourly == null) return emptyList()
    // Retain each ORIGINAL index. A missing timestamp must not shift any parallel array.
    val indices = linkedMapOf<Instant, Int>()
    hourly.time.orEmpty().forEachIndexed { index, timestamp ->
        timestamp.toWeatherInstant()?.let { indices.putIfAbsent(it, index) }
    }
    val ends = indices.keys.toMutableSet()
    indices.forEach { (start, index) ->
        val hasCondition = hourly.temperatureCelsius?.getOrNull(index).finiteOrNull() != null ||
            hourly.weatherCode?.getOrNull(index).knownCodeOrNull() != null ||
            hourly.isDay?.getOrNull(index).daylightOrNull() != null
        if (hasCondition) {
            // Keep a terminal temperature even when its following rain probability is absent.
            try {
                ends += start.plusSeconds(3600)
            } catch (_: DateTimeException) {
                // An impossible terminal interval cannot be represented.
            }
        }
    }
    return ends.sorted().map { end ->
        val start = end.minusSeconds(3600)
        val conditionIndex = indices[start]
        val probabilityIndex = indices[end]
        val code = conditionIndex?.let { hourly.weatherCode?.getOrNull(it) }.knownCodeOrNull()
        RainPeriod(
            start = start,
            end = end,
            probabilityPercent = probabilityIndex?.let { hourly.precipitationProbability?.getOrNull(it) }
                .probabilityOrNull(),
            temperatureCelsius = conditionIndex?.let { hourly.temperatureCelsius?.getOrNull(it) }.finiteOrNull(),
            weatherCode = code,
            weatherDescription = weatherDescription(code) ?: "Weather unavailable",
            isDay = conditionIndex?.let { hourly.isDay?.getOrNull(it) }.daylightOrNull()
        )
    }
}

private fun mapDailyForecast(daily: DailyWeatherResponse?, zone: ZoneId): List<DailyForecast> {
    if (daily == null) return emptyList()
    return daily.time.orEmpty().mapIndexedNotNull { index, timestamp ->
        val date = timestamp.toWeatherInstant()?.atZone(zone)?.toLocalDate()
            ?: return@mapIndexedNotNull null
        var minimum = daily.temperatureMinCelsius?.getOrNull(index).finiteOrNull()
        var maximum = daily.temperatureMaxCelsius?.getOrNull(index).finiteOrNull()
        if (minimum != null && maximum != null && minimum > maximum) {
            minimum = null
            maximum = null
        }
        val code = daily.weatherCode?.getOrNull(index).knownCodeOrNull()
        DailyForecast(
            date = date,
            temperatureMinCelsius = minimum,
            temperatureMaxCelsius = maximum,
            weatherCode = code,
            weatherDescription = weatherDescription(code) ?: "Weather unavailable",
            maxPrecipitationProbability = daily.maxPrecipitationProbability?.getOrNull(index).probabilityOrNull()
        )
    }.sortedBy { it.date }.distinctBy { it.date }
}

private fun Double?.finiteOrNull(): Double? = this?.takeIf { it.isFinite() }
private fun Double?.probabilityOrNull(): Double? = this?.takeIf { it.isFinite() && it in 0.0..100.0 }
private fun Int?.knownCodeOrNull(): Int? = this?.takeIf { weatherDescription(it) != null }
private fun Int?.daylightOrNull(): Boolean? = when (this) { 0 -> false; 1 -> true; else -> null }

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

package au.edu.jcu.assessment.utilityapp.data.repository

import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.DailyForecast
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherCondition
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

    val snapshot = WeatherSnapshot(
        city = city,
        temperatureCelsius = temperature,
        apparentTemperatureCelsius = apparentTemperature,
        condition = WeatherCondition.fromCode(current?.weatherCode),
        observationTime = current?.time.toWeatherInstant(),
        fetchedAt = fetchedAt,
        timezoneId = zone.id,
        forecastPeriods = mapHourlyPeriods(response.hourly),
        dailyForecast = mapDailyForecast(response.daily, zone),
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
            WeatherCondition.fromCode(hourly.weatherCode?.getOrNull(index)) != WeatherCondition.UNAVAILABLE ||
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
        RainPeriod(
            start = start,
            end = end,
            probabilityPercent = probabilityIndex?.let { hourly.precipitationProbability?.getOrNull(it) }
                .probabilityOrNull(),
            temperatureCelsius = conditionIndex?.let { hourly.temperatureCelsius?.getOrNull(it) }.finiteOrNull(),
            condition = WeatherCondition.fromCode(conditionIndex?.let { hourly.weatherCode?.getOrNull(it) }),
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
        DailyForecast(
            date = date,
            temperatureMinCelsius = minimum,
            temperatureMaxCelsius = maximum,
            condition = WeatherCondition.fromCode(daily.weatherCode?.getOrNull(index)),
            maxPrecipitationProbability = daily.maxPrecipitationProbability?.getOrNull(index).probabilityOrNull()
        )
    }.sortedBy { it.date }.distinctBy { it.date }
}

private fun Double?.finiteOrNull(): Double? = this?.takeIf { it.isFinite() }
private fun Double?.probabilityOrNull(): Double? = this?.takeIf { it.isFinite() && it in 0.0..100.0 }
private fun Int?.daylightOrNull(): Boolean? = when (this) { 0 -> false; 1 -> true; else -> null }

private fun Long?.toWeatherInstant(): Instant? {
    if (this == null || this <= 0) return null
    return try {
        Instant.ofEpochSecond(this)
    } catch (_: DateTimeException) {
        null
    }
}

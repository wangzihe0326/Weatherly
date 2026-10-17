package au.edu.jcu.assessment.utilityapp.data.repository

import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.remote.CurrentWeatherResponse
import au.edu.jcu.assessment.utilityapp.data.remote.DailyWeatherResponse
import au.edu.jcu.assessment.utilityapp.data.remote.HourlyWeatherResponse
import au.edu.jcu.assessment.utilityapp.data.remote.WeatherResponse
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherResponseMapperTest {
    private val fetchedAt = Instant.parse("2026-10-15T02:30:00Z")
    private val firstEnd = Instant.parse("2026-10-15T02:00:00Z")

    @Test
    fun precipitationUsesPrecedingHourAndKeepsAllEightPeriods() {
        val times = (0L..7L).map { firstEnd.plusSeconds(it * 3600).epochSecond }
        val response = baseResponse().copy(
            hourly = HourlyWeatherResponse(times, List(8) { it * 10.0 })
        )

        val snapshot = mapWeatherResponse(response, City.SINGAPORE, fetchedAt)

        assertEquals(8, snapshot.forecastPeriods.size)
        assertEquals(firstEnd.minusSeconds(3600), snapshot.forecastPeriods.first().start)
        assertEquals(firstEnd, snapshot.forecastPeriods.first().end)
        assertTrue(snapshot.forecastPeriods.first().end.isBefore(fetchedAt))
        assertEquals(fetchedAt, snapshot.fetchedAt)
        assertEquals(City.SINGAPORE, snapshot.city)
    }

    @Test
    fun unixTimeIsNotShiftedAgainByTimezoneOffset() {
        val snapshot = mapWeatherResponse(baseResponse(), City.SINGAPORE, fetchedAt)

        assertEquals(fetchedAt, snapshot.observationTime)
        assertEquals(10, snapshot.observationTime!!.atZone(ZoneId.of(snapshot.timezoneId)).hour)
        assertEquals("Asia/Singapore", snapshot.timezoneId)
    }

    @Test
    fun missingProbabilityEntriesStayNullWithoutShiftingOtherEntries() {
        val response = baseResponse().copy(
            hourly = HourlyWeatherResponse(
                time = listOf(firstEnd.epochSecond, null, firstEnd.plusSeconds(7200).epochSecond,
                    firstEnd.plusSeconds(10800).epochSecond),
                precipitationProbability = listOf(10.0, 90.0, 30.0)
            )
        )

        val periods = mapWeatherResponse(response, City.SINGAPORE, fetchedAt).forecastPeriods

        assertEquals(3, periods.size)
        assertEquals(listOf(10.0, 30.0, null), periods.map { it.probabilityPercent })
        assertEquals(firstEnd.plusSeconds(7200), periods[1].end)
    }

    @Test
    fun surplusProbabilitiesWithoutTimestampsAreNotTurnedIntoForecasts() {
        val response = baseResponse().copy(
            hourly = HourlyWeatherResponse(listOf(firstEnd.epochSecond), listOf(20.0, 90.0, 95.0))
        )

        val periods = mapWeatherResponse(response, City.CAIRNS, fetchedAt).forecastPeriods

        assertEquals(1, periods.size)
        assertEquals(20.0, periods.single().probabilityPercent!!, 0.0)
    }

    @Test
    fun invalidProbabilitiesRemainUnknownWhileZeroAndHundredAreRetained() {
        val probabilities = listOf(0.0, 100.0, -1.0, 101.0, Double.NaN, Double.POSITIVE_INFINITY, null)
        val response = baseResponse().copy(
            hourly = HourlyWeatherResponse(
                probabilities.indices.map { firstEnd.plusSeconds(it * 3600L).epochSecond },
                probabilities
            )
        )

        val periods = mapWeatherResponse(response, City.TOWNSVILLE, fetchedAt).forecastPeriods

        assertEquals(listOf(0.0, 100.0, null, null, null, null, null),
            periods.map { it.probabilityPercent })
    }

    @Test
    fun invalidTimestampsAreSkippedAndDuplicateHoursCannotInflateCoverage() {
        val response = baseResponse().copy(
            hourly = HourlyWeatherResponse(
                listOf(Long.MAX_VALUE, -1L, 0L, firstEnd.epochSecond, firstEnd.epochSecond),
                listOf(99.0, 99.0, 99.0, 25.0, 25.0)
            )
        )

        val periods = mapWeatherResponse(response, City.SINGAPORE, fetchedAt).forecastPeriods

        assertEquals(1, periods.size)
        assertEquals(25.0, periods.single().probabilityPercent!!, 0.0)
    }

    @Test
    fun missingJsonValuesDoNotBecomeZeroOrClearWeather() {
        val response = Gson().fromJson(
            """{
                "timezone":"Asia/Singapore",
                "current":{"temperature_2m":28.5},
                "hourly":{"time":[${firstEnd.epochSecond}],"precipitation_probability":[null]}
            }""",
            WeatherResponse::class.java
        )

        val snapshot = mapWeatherResponse(response, City.SINGAPORE, fetchedAt)

        assertEquals(28.5, snapshot.temperatureCelsius!!, 0.0)
        assertNull(snapshot.apparentTemperatureCelsius)
        assertNull(snapshot.observationTime)
        assertEquals("Weather unavailable", snapshot.weatherDescription)
        assertNull(snapshot.weatherCode)
        assertNull(snapshot.isDay)
        assertNull(snapshot.forecastPeriods.single().probabilityPercent)
    }

    @Test
    fun currentDaylightJsonMapsOnlyZeroAndOneWithoutInferringFromTheObservationHour() {
        for ((wireValue, expected) in listOf(0 to false, 1 to true, -1 to null, 2 to null)) {
            val response = Gson().fromJson(
                """{
                    "timezone":"Asia/Singapore",
                    "current":{"time":${fetchedAt.epochSecond},"weather_code":0,"is_day":$wireValue}
                }""",
                WeatherResponse::class.java
            )

            val snapshot = mapWeatherResponse(response, City.SINGAPORE, fetchedAt)

            assertEquals(expected, snapshot.isDay)
            assertEquals(0, snapshot.weatherCode)
            assertEquals("Clear sky", snapshot.weatherDescription)
        }
    }

    @Test
    fun missingOrExplicitlyNullCurrentDaylightDoesNotBorrowTheHourlyFlag() {
        for (currentFields in listOf("\"temperature_2m\":28.5", "\"temperature_2m\":28.5,\"is_day\":null")) {
            val response = Gson().fromJson(
                """{
                    "timezone":"Asia/Singapore",
                    "current":{$currentFields},
                    "hourly":{"time":[${firstEnd.epochSecond}],"weather_code":[61],"is_day":[1]}
                }""",
                WeatherResponse::class.java
            )

            val snapshot = mapWeatherResponse(response, City.SINGAPORE, fetchedAt)

            assertNull(snapshot.isDay)
            assertNull(snapshot.weatherCode)
            assertEquals("Weather unavailable", snapshot.weatherDescription)
            assertEquals(true, snapshot.forecastPeriods.single { it.start == firstEnd }.isDay)
        }
    }

    @Test
    fun nonFiniteTemperatureDoesNotHideOtherwiseUsableData() {
        val response = baseResponse().copy(current = CurrentWeatherResponse(
            temperatureCelsius = Double.NaN,
            apparentTemperatureCelsius = 31.5,
            weatherCode = 999
        ))

        val snapshot = mapWeatherResponse(response, City.CAIRNS, fetchedAt)

        assertNull(snapshot.temperatureCelsius)
        assertEquals(31.5, snapshot.apparentTemperatureCelsius!!, 0.0)
        assertEquals("Weather unavailable", snapshot.weatherDescription)
        assertNull(snapshot.weatherCode)
    }

    @Test
    fun forecastOnlyResponseIsUsableWithoutInventingCurrentConditions() {
        val response = WeatherResponse(
            timezone = "Australia/Brisbane",
            hourly = HourlyWeatherResponse(listOf(firstEnd.epochSecond), listOf(0.0))
        )

        val snapshot = mapWeatherResponse(response, City.TOWNSVILLE, fetchedAt)

        assertNull(snapshot.temperatureCelsius)
        assertNull(snapshot.observationTime)
        assertEquals("Weather unavailable", snapshot.weatherDescription)
        assertEquals(0.0, snapshot.forecastPeriods.single().probabilityPercent!!, 0.0)
    }

    @Test
    fun weatherCodeZeroIsValidEvenWhenTemperaturesAreMissing() {
        val response = WeatherResponse(
            timezone = "Asia/Singapore",
            current = CurrentWeatherResponse(weatherCode = 0)
        )

        assertEquals("Clear sky", mapWeatherResponse(response, City.SINGAPORE, fetchedAt).weatherDescription)
    }

    @Test
    fun heavyThunderstormCodeIsRecognized() {
        val response = baseResponse().copy(current = CurrentWeatherResponse(weatherCode = 97))

        assertEquals("Heavy thunderstorm",
            mapWeatherResponse(response, City.SINGAPORE, fetchedAt).weatherDescription)
    }

    @Test(expected = InvalidWeatherResponseException::class)
    fun unknownTimezoneIsAnExplicitErrorInsteadOfSilentlyBecomingUtc() {
        mapWeatherResponse(baseResponse().copy(timezone = "Mars/Olympus"), City.CAIRNS, fetchedAt)
    }

    @Test(expected = InvalidWeatherResponseException::class)
    fun missingTimezoneIsAnExplicitError() {
        mapWeatherResponse(baseResponse().copy(timezone = null), City.CAIRNS, fetchedAt)
    }

    @Test(expected = EmptyWeatherException::class)
    fun completelyMissingWeatherIsAnEmptyResponse() {
        mapWeatherResponse(WeatherResponse(timezone = "Asia/Singapore"), City.SINGAPORE, fetchedAt)
    }

    @Test(expected = EmptyWeatherException::class)
    fun invalidCurrentValuesAndMissingProbabilitiesCannotCreateSuccess() {
        val response = WeatherResponse(
            timezone = "Asia/Singapore",
            current = CurrentWeatherResponse(
                temperatureCelsius = Double.POSITIVE_INFINITY,
                apparentTemperatureCelsius = Double.NaN,
                weatherCode = 999
            ),
            hourly = HourlyWeatherResponse(listOf(firstEnd.epochSecond), listOf(null))
        )

        mapWeatherResponse(response, City.SINGAPORE, fetchedAt)
    }

    @Test
    fun hourlyConditionsJoinTheIntervalStartByTimestampRatherThanArrayPosition() {
        val start = firstEnd
        val response = baseResponse().copy(hourly = HourlyWeatherResponse(
            time = listOf(start.epochSecond, start.plusSeconds(7200).epochSecond, start.plusSeconds(3600).epochSecond),
            precipitationProbability = listOf(5.0, 90.0, 30.0),
            temperatureCelsius = listOf(20.0, 30.0, 25.0),
            weatherCode = listOf(0, 61, 3),
            isDay = listOf(0, 1, 1)
        ))

        val periods = mapWeatherResponse(response, City.SINGAPORE, fetchedAt).forecastPeriods
        val firstHour = periods.single { it.start == start }
        val secondHour = periods.single { it.start == start.plusSeconds(3600) }

        assertEquals(20.0, firstHour.temperatureCelsius!!, 0.0)
        assertEquals(30.0, firstHour.probabilityPercent!!, 0.0)
        assertEquals(0, firstHour.weatherCode)
        assertEquals("Clear sky", firstHour.weatherDescription)
        assertEquals(false, firstHour.isDay)
        assertEquals(25.0, secondHour.temperatureCelsius!!, 0.0)
        assertEquals(90.0, secondHour.probabilityPercent!!, 0.0)
        assertEquals(3, secondHour.weatherCode)
        assertEquals(true, secondHour.isDay)
    }

    @Test
    fun missingTimestampCannotAttachANeighboursConditionsToAnHour() {
        val response = baseResponse().copy(hourly = HourlyWeatherResponse(
            time = listOf(firstEnd.epochSecond, null, firstEnd.plusSeconds(7200).epochSecond),
            precipitationProbability = listOf(10.0, 99.0, 60.0),
            temperatureCelsius = listOf(20.0, 999.0, 30.0),
            weatherCode = listOf(0, 95, 3),
            isDay = listOf(0, 1, 1)
        ))

        val periods = mapWeatherResponse(response, City.SINGAPORE, fetchedAt).forecastPeriods
        val missingStart = periods.single { it.start == firstEnd.plusSeconds(3600) }
        val missingEnd = periods.single { it.start == firstEnd }

        assertNull(missingStart.temperatureCelsius)
        assertNull(missingStart.weatherCode)
        assertNull(missingStart.isDay)
        assertEquals(60.0, missingStart.probabilityPercent!!, 0.0)
        assertEquals(20.0, missingEnd.temperatureCelsius!!, 0.0)
        assertNull(missingEnd.probabilityPercent)
    }

    @Test
    fun shortOrInvalidConditionArraysRemainUnknown() {
        val response = baseResponse().copy(hourly = HourlyWeatherResponse(
            time = listOf(firstEnd.epochSecond, firstEnd.plusSeconds(3600).epochSecond, firstEnd.plusSeconds(7200).epochSecond),
            precipitationProbability = listOf(10.0, 20.0, 30.0),
            temperatureCelsius = listOf(21.0, Double.NaN),
            weatherCode = listOf(999),
            isDay = listOf(2, 1)
        ))

        val periods = mapWeatherResponse(response, City.SINGAPORE, fetchedAt).forecastPeriods
        val firstHour = periods.single { it.start == firstEnd }
        val secondHour = periods.single { it.start == firstEnd.plusSeconds(3600) }

        assertEquals(21.0, firstHour.temperatureCelsius!!, 0.0)
        assertNull(firstHour.weatherCode)
        assertEquals("Weather unavailable", firstHour.weatherDescription)
        assertNull(firstHour.isDay)
        assertNull(secondHour.temperatureCelsius)
        assertNull(secondHour.weatherCode)
        assertEquals(true, secondHour.isDay)
        assertEquals(30.0, secondHour.probabilityPercent!!, 0.0)
    }

    @Test
    fun aTerminalHourlyTemperatureIsUsefulWithoutCurrentConditionsOrRain() {
        val response = WeatherResponse(
            timezone = "Asia/Singapore",
            hourly = HourlyWeatherResponse(time = listOf(firstEnd.epochSecond), temperatureCelsius = listOf(23.5))
        )

        val snapshot = mapWeatherResponse(response, City.SINGAPORE, fetchedAt)
        val hour = snapshot.forecastPeriods.single { it.start == firstEnd }

        assertNull(snapshot.temperatureCelsius)
        assertEquals(firstEnd.plusSeconds(3600), hour.end)
        assertEquals(23.5, hour.temperatureCelsius!!, 0.0)
        assertNull(hour.probabilityPercent)
    }

    @Test
    fun dailyDatesUseTheCityTimezoneAndDailyOnlyTemperatureIsUseful() {
        val midnightInSingapore = Instant.parse("2026-10-14T16:00:00Z")
        val response = WeatherResponse(
            timezone = "Asia/Singapore",
            daily = DailyWeatherResponse(
                time = listOf(midnightInSingapore.epochSecond),
                temperatureMinCelsius = listOf(23.0)
            )
        )

        val snapshot = mapWeatherResponse(response, City.SINGAPORE, fetchedAt)
        val day = snapshot.dailyForecast.single()

        assertEquals(LocalDate.parse("2026-10-15"), day.date)
        assertEquals(23.0, day.temperatureMinCelsius!!, 0.0)
        assertNull(day.temperatureMaxCelsius)
        assertNull(day.maxPrecipitationProbability)
        assertNull(snapshot.temperatureCelsius)
    }

    @Test
    fun dailyMissingTimestampAndUnequalArraysDoNotShiftValues() {
        val midnight = Instant.parse("2026-10-14T16:00:00Z")
        val response = baseResponse().copy(daily = DailyWeatherResponse(
            time = listOf(midnight.epochSecond, null, midnight.plusSeconds(172800).epochSecond),
            temperatureMinCelsius = listOf(20.0, 999.0, 22.0),
            temperatureMaxCelsius = listOf(30.0),
            weatherCode = listOf(0, 95, 61),
            maxPrecipitationProbability = listOf(10.0, 99.0, 30.0)
        ))

        val days = mapWeatherResponse(response, City.SINGAPORE, fetchedAt).dailyForecast

        assertEquals(listOf(LocalDate.parse("2026-10-15"), LocalDate.parse("2026-10-17")), days.map { it.date })
        assertEquals(22.0, days[1].temperatureMinCelsius!!, 0.0)
        assertNull(days[1].temperatureMaxCelsius)
        assertEquals(61, days[1].weatherCode)
        assertEquals("Slight rain", days[1].weatherDescription)
        assertEquals(30.0, days[1].maxPrecipitationProbability!!, 0.0)
    }

    @Test
    fun invalidDailyValuesStayUnknownInsteadOfLookingLikeAValidForecast() {
        val response = baseResponse().copy(daily = DailyWeatherResponse(
            time = listOf(firstEnd.epochSecond, firstEnd.plusSeconds(86400).epochSecond),
            temperatureMinCelsius = listOf(32.0, Double.NaN),
            temperatureMaxCelsius = listOf(20.0, Double.POSITIVE_INFINITY),
            weatherCode = listOf(999, null),
            maxPrecipitationProbability = listOf(-1.0, 101.0)
        ))

        val days = mapWeatherResponse(response, City.SINGAPORE, fetchedAt).dailyForecast

        days.forEach { day ->
            assertNull(day.temperatureMinCelsius)
            assertNull(day.temperatureMaxCelsius)
            assertNull(day.weatherCode)
            assertNull(day.maxPrecipitationProbability)
            assertEquals("Weather unavailable", day.weatherDescription)
        }
    }

    @Test(expected = EmptyWeatherException::class)
    fun daylightFlagAloneDoesNotPretendToBeWeatherData() {
        mapWeatherResponse(
            WeatherResponse(timezone = "Asia/Singapore",
                hourly = HourlyWeatherResponse(time = listOf(firstEnd.epochSecond), isDay = listOf(1))),
            City.SINGAPORE,
            fetchedAt
        )
    }

    @Test(expected = EmptyWeatherException::class)
    fun currentDaylightFlagAloneDoesNotPretendToBeWeatherData() {
        mapWeatherResponse(
            WeatherResponse(timezone = "Asia/Singapore", current = CurrentWeatherResponse(isDay = 1)),
            City.SINGAPORE,
            fetchedAt
        )
    }

    private fun baseResponse() = WeatherResponse(
        timezone = "Asia/Singapore",
        utcOffsetSeconds = 28800,
        current = CurrentWeatherResponse(
            time = fetchedAt.epochSecond,
            temperatureCelsius = 28.0,
            apparentTemperatureCelsius = 32.0,
            weatherCode = 2
        )
    )
}

package au.edu.jcu.assessment.utilityapp.data.repository

import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.remote.CurrentWeatherResponse
import au.edu.jcu.assessment.utilityapp.data.remote.HourlyWeatherResponse
import au.edu.jcu.assessment.utilityapp.data.remote.WeatherResponse
import com.google.gson.Gson
import java.time.Instant
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
        assertNull(snapshot.forecastPeriods.single().probabilityPercent)
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

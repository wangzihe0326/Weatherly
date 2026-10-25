package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.DailyForecast
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherCondition
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.data.model.hasMeaningfulData
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherSnapshotTest {
    private val now = Instant.parse("2026-10-15T02:30:00Z")
    private val day = LocalDate.parse("2026-10-15")
    private val empty = WeatherSnapshot(City.SINGAPORE, fetchedAt = now, timezoneId = "Asia/Singapore")

    @Test
    fun knownConditionAloneIsUsefulForCurrentHourlyAndDailyForecasts() {
        for (condition in WeatherCondition.entries.filter { it != WeatherCondition.UNAVAILABLE }) {
            assertTrue(empty.copy(condition = condition).hasMeaningfulData())
            assertTrue(empty.copy(forecastPeriods = listOf(
                RainPeriod(now, now.plusSeconds(3600), condition = condition)
            )).hasMeaningfulData())
            assertTrue(empty.copy(dailyForecast = listOf(
                DailyForecast(day, condition = condition)
            )).hasMeaningfulData())
        }
    }

    @Test
    fun unavailableConditionsAndDaylightAloneDoNotCountAsWeather() {
        assertFalse(empty.hasMeaningfulData())
        assertFalse(empty.copy(
            isDay = true,
            forecastPeriods = listOf(RainPeriod(now, now.plusSeconds(3600), isDay = false)),
            dailyForecast = listOf(DailyForecast(day))
        ).hasMeaningfulData())
    }

    @Test
    fun nonFiniteTemperaturesAndOutOfRangeProbabilitiesCannotCreateUsefulData() {
        for (invalidProbability in listOf(-1.0, 101.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertFalse(empty.copy(
                temperatureCelsius = Double.NEGATIVE_INFINITY,
                apparentTemperatureCelsius = Double.NaN,
                forecastPeriods = listOf(RainPeriod(now, now.plusSeconds(3600),
                    probabilityPercent = invalidProbability, temperatureCelsius = Double.NaN)),
                dailyForecast = listOf(DailyForecast(day,
                    temperatureMinCelsius = Double.NaN,
                    temperatureMaxCelsius = Double.POSITIVE_INFINITY,
                    maxPrecipitationProbability = invalidProbability))
            ).hasMeaningfulData())
        }
    }

    @Test
    fun zeroValuesAndBoundaryProbabilitiesRemainUsefulWithoutACondition() {
        assertTrue(empty.copy(temperatureCelsius = 0.0).hasMeaningfulData())
        assertTrue(empty.copy(apparentTemperatureCelsius = 0.0).hasMeaningfulData())
        for (probability in listOf(0.0, 100.0)) {
            assertTrue(empty.copy(forecastPeriods = listOf(
                RainPeriod(now, now.plusSeconds(3600), probabilityPercent = probability)
            )).hasMeaningfulData())
            assertTrue(empty.copy(dailyForecast = listOf(
                DailyForecast(day, maxPrecipitationProbability = probability)
            )).hasMeaningfulData())
        }
    }
}

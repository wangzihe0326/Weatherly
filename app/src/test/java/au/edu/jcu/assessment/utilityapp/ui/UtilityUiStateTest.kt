package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.AdviceKind
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.DailyForecast
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class UtilityUiStateTest {
    private val now = Instant.parse("2026-10-15T06:20:00Z")
    private val periods = (0L..5L).map {
        val end = Instant.parse("2026-10-15T06:00:00Z").plusSeconds(it * 3600)
        RainPeriod(end.minusSeconds(3600), end, 60.0)
    }
    private val weather = WeatherSnapshot(
        city = City.SINGAPORE, fetchedAt = now, timezoneId = "Asia/Singapore", forecastPeriods = periods
    )

    @Test fun selectsOngoingAndFollowingTwoPeriodsNotPastHour() {
        val state = UtilityUiState(weather = weather, now = now)
        assertEquals(periods.subList(1, 4), state.displayedPeriods)
        assertEquals(AdviceKind.BRING_UMBRELLA, state.advice.kind)
    }

    @Test fun expiredBoundaryIsExcluded() {
        val state = UtilityUiState(weather = weather, now = periods[1].end)
        assertEquals(periods[2], state.displayedPeriods.first())
    }

    @Test fun staleBoundaryDisablesAdvice() {
        val fresh = UtilityUiState(weather = weather, now = now.plusSeconds(1799))
        val stale = fresh.copy(now = now.plusSeconds(1800))
        assertFalse(fresh.isStale)
        assertTrue(stale.isStale)
        assertEquals(AdviceKind.UNAVAILABLE, stale.advice.kind)
    }

    @Test fun farFutureFetchTimeIsNotTreatedAsFresh() {
        assertTrue(UtilityUiState(weather = weather.copy(fetchedAt = now.plusSeconds(61)), now = now).isStale)
    }

    @Test fun gapInUpcomingForecastCannotProduceAdvice() {
        val withGap = weather.copy(forecastPeriods = periods.filterIndexed { i, _ -> i != 2 })
        assertEquals(AdviceKind.UNAVAILABLE, UtilityUiState(weather = withGap, now = now).advice.kind)
    }

    @Test fun futureOnlyForecastCannotDescribeCurrentPeriod() {
        val futureOnly = weather.copy(forecastPeriods = periods.drop(2))
        assertEquals(AdviceKind.UNAVAILABLE, UtilityUiState(weather = futureOnly, now = now).advice.kind)
    }

    @Test fun temperatureConversionHasKnownReferencePoints() {
        assertEquals(32.0, TemperatureUnit.FAHRENHEIT.fromCelsius(0.0), 0.001)
        assertEquals(212.0, TemperatureUnit.FAHRENHEIT.fromCelsius(100.0), 0.001)
        assertEquals(-40.0, TemperatureUnit.FAHRENHEIT.fromCelsius(-40.0), 0.001)
    }

    @Test fun browsingIncludesTwentyFourUnfinishedHoursButAdviceStillUsesThree() {
        val extended = (0L..30L).map { offset ->
            val end = Instant.parse("2026-10-15T06:00:00Z").plusSeconds(offset * 3600)
            RainPeriod(end.minusSeconds(3600), end, 10.0, temperatureCelsius = 20.0 + offset)
        }
        val state = UtilityUiState(weather = weather.copy(forecastPeriods = extended), now = now)

        assertEquals(extended.subList(1, 25), state.hourlyPeriods)
        assertEquals(extended.subList(1, 4), state.displayedPeriods)
        assertEquals(24, state.hourlyPeriods.size)
        assertEquals(3, state.displayedPeriods.size)
    }

    @Test fun rainAfterTheThirdHourOrInTheDailyForecastCannotTriggerNearTermAdvice() {
        val extended = (1L..30L).map { offset ->
            val end = Instant.parse("2026-10-15T06:00:00Z").plusSeconds(offset * 3600)
            RainPeriod(end.minusSeconds(3600), end, if (offset <= 3) 10.0 else 99.0)
        }
        val state = UtilityUiState(weather = weather.copy(
            forecastPeriods = extended,
            dailyForecast = listOf(DailyForecast(LocalDate.parse("2026-10-15"), maxPrecipitationProbability = 100.0))
        ), now = now)

        assertEquals(99.0, state.hourlyPeriods[3].probabilityPercent!!, 0.0)
        assertEquals(100.0, state.dailyForecast.single().maxPrecipitationProbability!!, 0.0)
        assertEquals(AdviceKind.BELOW_THRESHOLD, state.advice.kind)
        assertEquals(10.0, state.advice.peakProbability!!, 0.0)
    }

    @Test fun missingEarlyHoursCannotPullPeriodsBeyondTwentyFourHoursIntoTheWindow() {
        val firstHour = Instant.parse("2026-10-15T06:00:00Z")
        val laterHours = (10L..40L).map { offset ->
            val start = firstHour.plusSeconds(offset * 3600)
            RainPeriod(start, start.plusSeconds(3600), 20.0)
        }
        val exactlyAtLimit = RainPeriod(now.plusSeconds(86400), now.plusSeconds(90000), 99.0)
        val state = UtilityUiState(weather = weather.copy(
            forecastPeriods = laterHours + exactlyAtLimit
        ), now = now)

        assertEquals(laterHours.take(15), state.hourlyPeriods)
        assertTrue(state.hourlyPeriods.all { it.start < now.plusSeconds(86400) })
        assertFalse(exactlyAtLimit in state.hourlyPeriods)
        assertEquals(AdviceKind.UNAVAILABLE, state.advice.kind)
    }

    @Test fun dailyWindowStartsOnTheCityDateAndContainsAtMostSevenCalendarDays() {
        val clockTime = Instant.parse("2026-10-15T15:30:00Z")
        val firstDate = LocalDate.parse("2026-10-14")
        val days = (0L..10L).map { DailyForecast(firstDate.plusDays(it), temperatureMaxCelsius = 30.0) }
        val singapore = UtilityUiState(weather = weather.copy(
            fetchedAt = clockTime, dailyForecast = days, timezoneId = "Asia/Singapore"
        ), now = clockTime)
        val townsville = singapore.copy(weather = singapore.weather!!.copy(
            city = City.TOWNSVILLE, timezoneId = "Australia/Brisbane"
        ))

        assertEquals(LocalDate.parse("2026-10-15"), singapore.dailyForecast.first().date)
        assertEquals(LocalDate.parse("2026-10-21"), singapore.dailyForecast.last().date)
        assertEquals(LocalDate.parse("2026-10-16"), townsville.dailyForecast.first().date)
        assertEquals(LocalDate.parse("2026-10-22"), townsville.dailyForecast.last().date)
        assertEquals(7, singapore.dailyForecast.size)
        assertEquals(7, townsville.dailyForecast.size)
    }

    @Test fun missingDaysAreNotInventedOrReplacedByDatesBeyondTheSevenDayWindow() {
        val today = LocalDate.parse("2026-10-15")
        val state = UtilityUiState(weather = weather.copy(dailyForecast = listOf(
            DailyForecast(today.minusDays(1)),
            DailyForecast(today),
            DailyForecast(today.plusDays(2)),
            DailyForecast(today.plusDays(7))
        )), now = now)

        assertEquals(listOf(today, today.plusDays(2)), state.dailyForecast.map { it.date })
    }

    @Test fun displayUnitDoesNotConvertStoredHourlyOrDailyCelsiusOrAlterAdvice() {
        val snapshot = weather.copy(
            forecastPeriods = periods.map { it.copy(temperatureCelsius = 25.0) },
            dailyForecast = listOf(DailyForecast(LocalDate.parse("2026-10-15"), 20.0, 30.0))
        )
        val celsius = UtilityUiState(weather = snapshot, now = now)
        val fahrenheit = celsius.copy(settings = celsius.settings.copy(temperatureUnit = TemperatureUnit.FAHRENHEIT))

        assertEquals(celsius.hourlyPeriods, fahrenheit.hourlyPeriods)
        assertEquals(celsius.dailyForecast, fahrenheit.dailyForecast)
        assertEquals(celsius.advice, fahrenheit.advice)
        assertEquals(25.0, fahrenheit.hourlyPeriods.first().temperatureCelsius!!, 0.0)
        assertEquals(20.0, fahrenheit.dailyForecast.single().temperatureMinCelsius!!, 0.0)
        assertEquals(30.0, fahrenheit.dailyForecast.single().temperatureMaxCelsius!!, 0.0)
    }
}

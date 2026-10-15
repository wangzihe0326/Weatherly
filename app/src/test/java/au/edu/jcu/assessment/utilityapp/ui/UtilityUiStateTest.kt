package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.AdviceKind
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

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
}

package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.DailyForecast
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.data.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class UtilityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = MutableClock(Instant.parse("2026-10-15T06:20:00Z"))
    private val repository = ControlledRepository()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun initialRequestHasLoadingThenRealRepositoryResult() = runTest {
        val vm = UtilityViewModel(repository, clock)
        assertTrue(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.weather)
        runCurrent()
        assertEquals(City.SINGAPORE, repository.requests.single().city)
        repository.succeed(0, snapshot(City.SINGAPORE))
        runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(28.0, vm.uiState.value.weather!!.temperatureCelsius!!, 0.01)
    }

    @Test fun sameCityRefreshFailureKeepsItsOldWeather() = runTest {
        val vm = UtilityViewModel(repository, clock)
        runCurrent()
        val original = snapshot(City.SINGAPORE)
        repository.succeed(0, original)
        runCurrent()
        vm.refresh()
        runCurrent()
        repository.fail(1)
        runCurrent()
        assertEquals(original, vm.uiState.value.weather)
        assertNotNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun changingCityImmediatelyClearsPreviousCityAndFailureDoesNotRestoreIt() = runTest {
        val vm = UtilityViewModel(repository, clock)
        runCurrent()
        repository.succeed(0, snapshot(City.SINGAPORE))
        runCurrent()
        vm.selectCity(City.CAIRNS)
        assertNull(vm.uiState.value.weather)
        assertEquals(City.CAIRNS, vm.uiState.value.settings.city)
        runCurrent()
        repository.fail(1)
        runCurrent()
        assertNull(vm.uiState.value.weather)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test fun lateUncancellableSuccessCannotOverwriteNewCity() = runTest {
        val vm = UtilityViewModel(repository, clock)
        runCurrent()
        vm.selectCity(City.TOWNSVILLE)
        runCurrent()
        repository.succeed(1, snapshot(City.TOWNSVILLE))
        runCurrent()
        repository.succeed(0, snapshot(City.SINGAPORE))
        runCurrent()
        assertEquals(City.TOWNSVILLE, vm.uiState.value.weather!!.city)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test fun lateUncancellableFailureCannotReplaceNewCitySuccess() = runTest {
        val vm = UtilityViewModel(repository, clock)
        runCurrent()
        vm.selectCity(City.CAIRNS)
        runCurrent()
        repository.succeed(1, snapshot(City.CAIRNS))
        runCurrent()
        repository.fail(0)
        runCurrent()
        assertEquals(City.CAIRNS, vm.uiState.value.weather!!.city)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test fun presentationSettingsDoNotFetchWeatherAgain() = runTest {
        val vm = UtilityViewModel(repository, clock)
        runCurrent()
        repository.succeed(0, snapshot(City.SINGAPORE))
        runCurrent()
        vm.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        vm.setRainThreshold(80)
        vm.setUmbrellaEnabled(false)
        vm.selectCity(City.SINGAPORE)
        vm.setRainThreshold(999)
        runCurrent()
        assertEquals(1, repository.requests.size)
        assertEquals(80, vm.uiState.value.settings.rainThresholdPercent)
        assertEquals(TemperatureUnit.FAHRENHEIT, vm.uiState.value.settings.temperatureUnit)
        assertFalse(vm.uiState.value.settings.umbrellaEnabled)
    }

    @Test fun foregroundDoesNotDuplicateLoadingOrFreshRequestsButRefreshesAtThirtyMinutes() = runTest {
        val vm = UtilityViewModel(repository, clock)
        vm.onForeground()
        runCurrent()
        assertEquals(1, repository.requests.size)
        repository.succeed(0, snapshot(City.SINGAPORE))
        runCurrent()
        vm.onForeground()
        runCurrent()
        assertEquals(1, repository.requests.size)
        clock.value = clock.value.plusSeconds(1800)
        vm.onForeground()
        runCurrent()
        assertEquals(2, repository.requests.size)
        assertTrue(vm.uiState.value.isStale)
        repository.succeed(1, snapshot(City.SINGAPORE))
        runCurrent()
        assertFalse(vm.uiState.value.isStale)
    }

    @Test fun retryRecoversAfterInitialNetworkFailure() = runTest {
        val vm = UtilityViewModel(repository, clock)
        runCurrent()
        repository.fail(0)
        runCurrent()
        assertNull(vm.uiState.value.weather)
        vm.refresh()
        assertNull(vm.uiState.value.errorMessage)
        runCurrent()
        repository.succeed(1, snapshot(City.SINGAPORE))
        runCurrent()
        assertNotNull(vm.uiState.value.weather)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test fun dailyOnlyTemperatureIsAcceptedAndNearTermCoverageStillControlsForegroundRefresh() = runTest {
        val vm = UtilityViewModel(repository, clock)
        runCurrent()
        val partial = WeatherSnapshot(
            city = City.SINGAPORE, fetchedAt = clock.instant(), timezoneId = "Asia/Singapore",
            dailyForecast = listOf(DailyForecast(LocalDate.parse("2026-10-15"), temperatureMaxCelsius = 31.0))
        )
        repository.succeed(0, partial)
        runCurrent()

        assertEquals(partial, vm.uiState.value.weather)
        assertNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
        vm.onForeground()
        runCurrent()
        assertEquals(2, repository.requests.size)
        repository.succeed(1, partial)
        runCurrent()
    }

    @Test fun hourlyOnlyTemperatureIsAcceptedWithoutCreatingRainAdviceOrRefetchingForUnits() = runTest {
        val vm = UtilityViewModel(repository, clock)
        runCurrent()
        val partial = WeatherSnapshot(
            city = City.SINGAPORE, fetchedAt = clock.instant(), timezoneId = "Asia/Singapore",
            forecastPeriods = snapshot(City.SINGAPORE).forecastPeriods.map {
                it.copy(probabilityPercent = null, temperatureCelsius = 26.0)
            }
        )
        repository.succeed(0, partial)
        runCurrent()
        vm.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        vm.onForeground()
        runCurrent()

        assertEquals(partial, vm.uiState.value.weather)
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(au.edu.jcu.assessment.utilityapp.data.AdviceKind.UNAVAILABLE, vm.uiState.value.advice.kind)
        assertEquals(1, repository.requests.size)
        assertEquals(26.0, vm.uiState.value.hourlyPeriods.first().temperatureCelsius!!, 0.0)
    }

    @Test fun fewerThanTwentyFourHoursDoesNotRefreshWhenTheThreeHourReminderWindowIsComplete() = runTest {
        val vm = UtilityViewModel(repository, clock)
        runCurrent()
        val shortForecast = snapshot(City.SINGAPORE).copy(
            forecastPeriods = snapshot(City.SINGAPORE).forecastPeriods.take(3)
        )
        repository.succeed(0, shortForecast)
        runCurrent()
        vm.onForeground()
        runCurrent()

        assertEquals(3, vm.uiState.value.hourlyPeriods.size)
        assertEquals(1, repository.requests.size)
    }

    @Test fun aCompletelyEmptyRefreshDoesNotReplaceTheLastUsefulForecast() = runTest {
        val vm = UtilityViewModel(repository, clock)
        runCurrent()
        val original = snapshot(City.SINGAPORE)
        repository.succeed(0, original)
        runCurrent()
        vm.refresh()
        runCurrent()
        repository.succeed(1, WeatherSnapshot(
            city = City.SINGAPORE, fetchedAt = clock.instant(), timezoneId = "Asia/Singapore"
        ))
        runCurrent()

        assertEquals(original, vm.uiState.value.weather)
        assertEquals("No usable weather data was returned for this city. Please try again.", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    private fun snapshot(city: City) = WeatherSnapshot(
        city = city, temperatureCelsius = 28.0, fetchedAt = clock.instant(),
        timezoneId = "Asia/Singapore",
        forecastPeriods = (1L..8L).map {
            val end = Instant.parse("2026-10-15T06:00:00Z").plusSeconds(it * 3600)
            RainPeriod(end.minusSeconds(3600), end, 60.0)
        }
    )

    private class ControlledRepository : WeatherRepository {
        data class Request(val city: City, val continuation: Continuation<WeatherSnapshot>)
        val requests = mutableListOf<Request>()
        // Deliberately ignores cancellation to exercise the request-version guard.
        override suspend fun fetchWeather(city: City): WeatherSnapshot = suspendCoroutine {
            requests += Request(city, it)
        }
        fun succeed(index: Int, weather: WeatherSnapshot) = requests[index].continuation.resume(weather)
        fun fail(index: Int) = requests[index].continuation.resumeWithException(IOException("test network failure"))
    }

    private class MutableClock(var value: Instant) : Clock() {
        override fun instant(): Instant = value
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }
}

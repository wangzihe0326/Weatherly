package au.edu.jcu.assessment.utilityapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.hasMeaningfulData
import au.edu.jcu.assessment.utilityapp.data.repository.EmptyWeatherException
import au.edu.jcu.assessment.utilityapp.data.repository.InvalidWeatherResponseException
import au.edu.jcu.assessment.utilityapp.data.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant

class UtilityViewModel(
    private val repository: WeatherRepository,
    private val clock: Clock = Clock.systemUTC()
) : ViewModel() {
    private val mutableState = MutableStateFlow(UtilityUiState(now = clock.instant()))
    val uiState = mutableState.asStateFlow()
    private var request: Job? = null
    private var requestVersion = 0L
    private var isForeground = false
    private var automaticRetriesStarted = 0
    private var nextAutomaticRefreshAt: Instant? = null

    init {
        refresh()
    }

    fun refresh() {
        if (mutableState.value.isLoading) return
        automaticRetriesStarted = 0
        startRequest()
    }

    private fun startRequest() {
        val version = ++requestVersion
        val city = mutableState.value.settings.city
        request?.cancel()
        mutableState.update {
            it.copy(
                isLoading = true, errorMessage = null, nextRetryAt = null,
                automaticRetriesExhausted = false, now = clock.instant()
            )
        }
        request = viewModelScope.launch {
            try {
                val weather = repository.fetchWeather(city)
                check(weather.city == city) { "Repository returned a different city" }
                if (!weather.hasMeaningfulData()) throw EmptyWeatherException()
                // Cancellation is cooperative; also reject late, uncancellable results.
                if (version == requestVersion) {
                    val receivedAt = clock.instant()
                    automaticRetriesStarted = 0
                    // A useful but incomplete response must not trigger a request every minute.
                    nextAutomaticRefreshAt = receivedAt.plus(AUTOMATIC_REFRESH_INTERVAL)
                    mutableState.update {
                        it.copy(
                            weather = weather, isLoading = false, errorMessage = null,
                            nextRetryAt = null, automaticRetriesExhausted = false, now = receivedAt
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (version == requestVersion) {
                    val failedAt = clock.instant()
                    val nextRetry = RETRY_DELAYS.getOrNull(automaticRetriesStarted)?.let(failedAt::plus)
                    mutableState.update {
                        it.copy(
                            isLoading = false, errorMessage = messageFor(failure),
                            nextRetryAt = nextRetry, automaticRetriesExhausted = nextRetry == null,
                            now = failedAt
                        )
                    }
                }
            }
        }
    }

    fun selectCity(city: City) {
        if (city == mutableState.value.settings.city) return
        mutableState.update {
            it.copy(settings = it.settings.copy(city = city), weather = null, errorMessage = null)
        }
        automaticRetriesStarted = 0
        nextAutomaticRefreshAt = null
        // A city change supersedes even a request that is already in flight.
        startRequest()
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        mutableState.update { it.copy(settings = it.settings.copy(temperatureUnit = unit)) }
    }

    fun setUmbrellaEnabled(enabled: Boolean) {
        mutableState.update { it.copy(settings = it.settings.copy(umbrellaEnabled = enabled)) }
    }

    fun setRainThreshold(percent: Int) {
        if (percent !in setOf(30, 60, 80)) return
        mutableState.update { it.copy(settings = it.settings.copy(rainThresholdPercent = percent)) }
    }

    fun updateTime() {
        val now = clock.instant()
        val previous = mutableState.value.now
        val clockMovedBackBy = Duration.between(now, previous).takeIf { !it.isNegative }
        if (clockMovedBackBy != null) {
            // Preserve the remaining wait if the device clock is corrected backwards.
            nextAutomaticRefreshAt = nextAutomaticRefreshAt?.minus(clockMovedBackBy)
        }
        mutableState.update {
            it.copy(
                now = now,
                nextRetryAt = if (clockMovedBackBy == null) it.nextRetryAt
                    else it.nextRetryAt?.minus(clockMovedBackBy)
            )
        }
    }

    fun onForeground() {
        isForeground = true
        onForegroundTick()
    }

    fun onBackground() {
        isForeground = false
    }

    fun onForegroundTick() {
        if (!isForeground) return
        updateTime()
        val state = mutableState.value
        if (state.isLoading) return

        // Re-entering the foreground preserves both the backoff and its retry limit.
        if (state.errorMessage != null) {
            val retryAt = state.nextRetryAt ?: return
            if (state.now >= retryAt) {
                automaticRetriesStarted++
                startRequest()
            }
            return
        }

        if (nextAutomaticRefreshAt?.let { state.now < it } == true) return
        if (state.weather == null || state.isStale || state.displayedPeriods.size < 3) {
            startRequest()
        }
    }

    private companion object {
        val AUTOMATIC_REFRESH_INTERVAL: Duration = Duration.ofMinutes(30)
        val RETRY_DELAYS: List<Duration> = listOf(1L, 2L, 5L).map(Duration::ofMinutes)
    }

    private fun messageFor(failure: Exception): String = when (failure) {
        is HttpException -> if (failure.code() == 429) {
            "The weather service is busy. Please wait a moment and try again."
        } else {
            "The weather service is unavailable. Please try again."
        }
        is EmptyWeatherException -> "No usable weather data was returned for this city. Please try again."
        is InvalidWeatherResponseException -> "Some weather data could not be read. Please try again."
        is IOException -> "Could not reach the weather service. Check your connection and try again."
        else -> "Weather could not be loaded. Please try again."
    }
}

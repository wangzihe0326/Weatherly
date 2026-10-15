package au.edu.jcu.assessment.utilityapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.assessment.utilityapp.data.model.City
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

class UtilityViewModel(
    private val repository: WeatherRepository,
    private val clock: Clock = Clock.systemUTC()
) : ViewModel() {
    private val mutableState = MutableStateFlow(UtilityUiState(now = clock.instant()))
    val uiState = mutableState.asStateFlow()
    private var request: Job? = null
    private var requestVersion = 0L

    init {
        refresh()
    }

    fun refresh() {
        val version = ++requestVersion
        val city = mutableState.value.settings.city
        request?.cancel()
        mutableState.update { it.copy(isLoading = true, errorMessage = null, now = clock.instant()) }
        request = viewModelScope.launch {
            try {
                val weather = repository.fetchWeather(city)
                check(weather.city == city) { "Repository returned a different city" }
                // Cancellation is cooperative; also reject late, uncancellable results.
                if (version == requestVersion) {
                    mutableState.update {
                        it.copy(weather = weather, isLoading = false, errorMessage = null, now = clock.instant())
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (version == requestVersion) {
                    mutableState.update {
                        it.copy(isLoading = false, errorMessage = messageFor(failure), now = clock.instant())
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
        refresh()
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
        mutableState.update { it.copy(now = clock.instant()) }
    }

    fun onForeground() {
        updateTime()
        val state = mutableState.value
        if (!state.isLoading && (state.weather == null || state.isStale || state.displayedPeriods.size < 3)) {
            refresh()
        }
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

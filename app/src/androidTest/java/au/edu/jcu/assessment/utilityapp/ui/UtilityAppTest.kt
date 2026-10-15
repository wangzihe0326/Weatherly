package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** UI-only fixtures: no network or production Activity is launched by these tests. */
@RunWith(AndroidJUnit4::class)
class UtilityAppTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var currentState: MutableState<UtilityUiState>
    private var refreshCount = 0
    private val now = Instant.parse("2026-10-15T05:20:00Z")

    @Test
    fun unitsApplyToBothTemperaturesAndSurviveTabChanges() {
        showApp()
        compose.onNodeWithTag("current_temperature").assertTextEquals("25°C")
        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("unit_FAHRENHEIT").performScrollTo().performClick()
        compose.onNodeWithTag("weather_tab").performClick()
        compose.onNodeWithTag("current_temperature").assertTextEquals("77°F")
        compose.onNodeWithTag("feels_like_temperature").assertTextEquals("Feels like 86°F")
        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("unit_FAHRENHEIT").performScrollTo().assertIsSelected()
    }

    @Test
    fun cityThresholdAndToggleDeliverSettingsChanges() {
        showApp()
        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("city_TOWNSVILLE").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(City.TOWNSVILLE, currentState.value.settings.city) }
        compose.onNodeWithTag("threshold_30").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(30, currentState.value.settings.rainThresholdPercent) }
        compose.onNodeWithTag("umbrella_toggle").performScrollTo().performClick().assertIsOff()
        compose.onNodeWithTag("threshold_60").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { assertFalse(currentState.value.settings.umbrellaEnabled) }
    }

    @Test
    fun disablingAdviceHidesItWhileKeepingWeather() {
        showApp()
        compose.onNodeWithTag("umbrella_advice").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("umbrella_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("weather_tab").performClick()
        compose.onNodeWithTag("current_temperature").assertTextEquals("25°C")
        compose.onNodeWithTag("umbrella_advice").assertDoesNotExist()
    }

    @Test
    fun initialFailureShowsRetryWithoutInventingWeather() {
        showApp(UtilityUiState(errorMessage = "Connection unavailable. Please try again.", now = now))
        compose.onNodeWithText("Weather could not be loaded").assertIsDisplayed()
        compose.onNodeWithTag("current_temperature").assertDoesNotExist()
        compose.onNodeWithTag("retry_button").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, refreshCount) }
    }

    @Test
    fun missingProbabilityShowsUnavailableAdviceAndKeepsTheOtherPeriods() {
        val weather = sampleWeather().copy(
            forecastPeriods = sampleWeather().forecastPeriods.mapIndexed { index, period ->
                if (index == 1) period.copy(probabilityPercent = null) else period
            }
        )
        showApp(UtilityUiState(weather = weather, now = now))
        compose.onNodeWithText("Not enough current data for advice").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rain probability unavailable").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("rain_period_2").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun darkThemeAndLargeTextKeepTheLastSettingReachable() {
        showApp(darkTheme = true, fontScale = 1.5f)
        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("threshold_80").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("weather_tab").performClick()
        compose.onNodeWithTag("current_temperature").assertTextEquals("25°C")
        compose.onNodeWithText("Below your reminder threshold").performScrollTo().assertIsDisplayed()
    }

    private fun showApp(
        initialState: UtilityUiState = UtilityUiState(weather = sampleWeather(), now = now),
        darkTheme: Boolean = false,
        fontScale: Float = 1f
    ) {
        currentState = mutableStateOf(initialState)
        refreshCount = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MobileTech_UtilityAppTheme(darkTheme = darkTheme) {
                    UtilityApp(
                        state = currentState.value,
                        onRefresh = { refreshCount++ },
                        onCityChanged = { city ->
                            currentState.value = currentState.value.copy(
                                settings = currentState.value.settings.copy(city = city),
                                weather = null
                            )
                        },
                        onUnitChanged = { unit ->
                            currentState.value = currentState.value.copy(
                                settings = currentState.value.settings.copy(temperatureUnit = unit)
                            )
                        },
                        onUmbrellaEnabledChanged = { enabled ->
                            currentState.value = currentState.value.copy(
                                settings = currentState.value.settings.copy(umbrellaEnabled = enabled)
                            )
                        },
                        onThresholdChanged = { threshold ->
                            currentState.value = currentState.value.copy(
                                settings = currentState.value.settings.copy(rainThresholdPercent = threshold)
                            )
                        }
                    )
                }
            }
        }
    }

    private fun sampleWeather(): WeatherSnapshot {
        val start = Instant.parse("2026-10-15T05:00:00Z")
        return WeatherSnapshot(
            city = City.SINGAPORE,
            temperatureCelsius = 25.0,
            apparentTemperatureCelsius = 30.0,
            weatherDescription = "Cloudy",
            observationTime = now,
            fetchedAt = now,
            timezoneId = "Asia/Singapore",
            forecastPeriods = listOf(70.0, 40.0, 20.0).mapIndexed { index, probability ->
                RainPeriod(
                    start = start.plusSeconds(index * 3600L),
                    end = start.plusSeconds((index + 1) * 3600L),
                    probabilityPercent = probability
                )
            }
        )
    }
}

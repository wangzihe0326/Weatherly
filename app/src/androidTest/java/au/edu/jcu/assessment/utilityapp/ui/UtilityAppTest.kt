package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.DailyForecast
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.ui.components.periodLabel
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

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
        scrollWeatherTo("umbrella_advice")
        compose.onNodeWithTag("umbrella_advice").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("umbrella_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("weather_tab").performClick()
        scrollWeatherTo("current_temperature")
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
        scrollWeatherTo("umbrella_advice")
        compose.onNodeWithText("Not enough current data for advice").performScrollTo().assertIsDisplayed()
        selectVisibleHour(1)
        assertOnlyRainPeriodSelected(1)
        compose.onNodeWithTag("rain_period_1")
            .assertContentDescriptionContains(periodLabel(weather.forecastPeriods[1], weather.timezoneId), substring = true)
            .assertContentDescriptionContains("Rain probability unavailable", substring = true)
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(weather.forecastPeriods[1], weather.timezoneId))
        compose.onNodeWithTag("rain_selected_probability", useUnmergedTree = true).performScrollTo()
            .assertTextEquals("Rain probability is unavailable for this hour.")
        scrollWeatherTo("umbrella_advice")
        compose.onNodeWithText("Not enough current data for advice").performScrollTo().assertIsDisplayed()
        selectVisibleHour(2)
        compose.onNodeWithTag("rain_period_2").assertIsSelected()
        compose.runOnIdle { assertEquals(0, refreshCount) }
    }

    @Test
    fun choosingRainHoursUpdatesDetailsWithoutRefreshingOrChangingOverallAdvice() {
        val weather = sampleWeather()
        val originalState = UtilityUiState(weather = weather, now = now)
        showApp(originalState)
        scrollWeatherTo("weather_hourly_section")
        assertOnlyRainPeriodSelected(0)

        selectVisibleHour(1)
        assertOnlyRainPeriodSelected(1)
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(weather.forecastPeriods[1], weather.timezoneId))
        compose.onNodeWithTag("rain_selected_probability", useUnmergedTree = true).performScrollTo()
            .assertTextEquals("40% chance of rain during this hour.")

        selectVisibleHour(2)
        assertOnlyRainPeriodSelected(2)
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(weather.forecastPeriods[2], weather.timezoneId))
        compose.onNodeWithTag("rain_selected_probability", useUnmergedTree = true).performScrollTo()
            .assertTextEquals("20% chance of rain during this hour.")

        // Inspecting a lower-probability hour must not replace the three-hour advice.
        scrollWeatherTo("umbrella_advice")
        compose.onNodeWithText("Consider taking an umbrella").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Below your reminder threshold").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, refreshCount)
            assertEquals(originalState, currentState.value)
        }
    }

    @Test
    fun forecastUpdatesKeepTheChosenHourAndFallBackWhenItExpires() {
        val weather = sampleWeather()
        showApp(UtilityUiState(weather = weather, now = now))
        selectVisibleHour(1)
        assertOnlyRainPeriodSelected(1)

        // A fresh forecast advances the window: the chosen hour is now the first tile.
        val nextNow = now.plusSeconds(3600)
        val chosenHourUpdated = weather.forecastPeriods[1].copy(probabilityPercent = 45.0)
        val newLastHour = RainPeriod(
            start = weather.forecastPeriods.last().end,
            end = weather.forecastPeriods.last().end.plusSeconds(3600),
            probabilityPercent = 10.0
        )
        val nextWeather = weather.copy(
            fetchedAt = nextNow,
            observationTime = nextNow,
            forecastPeriods = listOf(chosenHourUpdated, weather.forecastPeriods[2], newLastHour)
        )
        compose.runOnIdle {
            currentState.value = currentState.value.copy(weather = nextWeather, now = nextNow)
        }

        assertOnlyRainPeriodSelected(0)
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(chosenHourUpdated, weather.timezoneId))
        compose.onNodeWithTag("rain_selected_probability", useUnmergedTree = true).performScrollTo()
            .assertTextEquals("45% chance of rain during this hour.")

        // Once that hour is no longer in the forecast, select the first available hour.
        val followingNow = nextNow.plusSeconds(3600)
        val followingWeather = nextWeather.copy(
            fetchedAt = followingNow,
            observationTime = followingNow,
            forecastPeriods = listOf(
                weather.forecastPeriods[2],
                newLastHour,
                RainPeriod(newLastHour.end, newLastHour.end.plusSeconds(3600), 5.0)
            )
        )
        compose.runOnIdle {
            currentState.value = currentState.value.copy(weather = followingWeather, now = followingNow)
        }

        assertOnlyRainPeriodSelected(0)
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(followingWeather.forecastPeriods.first(), weather.timezoneId))
        compose.onNodeWithTag("rain_selected_probability", useUnmergedTree = true).performScrollTo()
            .assertTextEquals("20% chance of rain during this hour.")
        compose.runOnIdle { assertEquals(0, refreshCount) }
    }

    @Test
    fun darkThemeAndDoubleTextSizeKeepTheThirdRainHourSelectable() {
        val weather = sampleWeather()
        showApp(UtilityUiState(weather = weather, now = now), darkTheme = true, fontScale = 2f)

        selectVisibleHour(2)
        compose.onNodeWithTag("rain_period_2").assertIsSelected()
            .assertContentDescriptionContains(periodLabel(weather.forecastPeriods[2], weather.timezoneId), substring = true)
            .assertContentDescriptionContains("Rain probability 20%", substring = true)
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true).performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals(periodLabel(weather.forecastPeriods[2], weather.timezoneId))
        compose.onNodeWithTag("rain_selected_probability", useUnmergedTree = true).performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("20% chance of rain during this hour.")
    }

    @Test
    fun darkThemeAndLargeTextKeepTheLastSettingReachable() {
        showApp(darkTheme = true, fontScale = 1.5f)
        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("threshold_80").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("weather_tab").performClick()
        compose.onNodeWithTag("current_temperature").assertTextEquals("25°C")
        scrollWeatherTo("umbrella_advice")
        compose.onNodeWithText("Below your reminder threshold").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun hourlyControlsReachHour24WithoutChangingNearTermAdvice() {
        val weather = sampleWeather()
        val start = weather.forecastPeriods.first().start
        val extended = weather.copy(forecastPeriods = (0 until 24).map { index ->
            RainPeriod(
                start = start.plusSeconds(index * 3600L),
                end = start.plusSeconds((index + 1) * 3600L),
                probabilityPercent = if (index < 3) 20.0 else 95.0,
                temperatureCelsius = 25.0,
                weatherCode = 2,
                weatherDescription = "Partly cloudy",
                isDay = true
            )
        })
        showApp(UtilityUiState(weather = extended, now = now))
        scrollWeatherTo("weather_hourly_section")
        compose.onNodeWithTag("hourly_previous").performScrollTo().assertIsNotEnabled()
        repeat(23) { compose.onNodeWithTag("hourly_next").performScrollTo().performClick() }
        compose.onNodeWithTag("rain_period_23").assertIsSelected()
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(extended.forecastPeriods.last(), extended.timezoneId))
        compose.onNodeWithTag("rain_selected_probability", useUnmergedTree = true).performScrollTo()
            .assertTextEquals("95% chance of rain during this hour.")
        compose.onNodeWithTag("hourly_next").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("hourly_previous").performClick()
        compose.onNodeWithTag("rain_period_22").assertIsSelected()
        scrollWeatherTo("umbrella_advice")
        compose.onNodeWithText("Below your reminder threshold").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Consider taking an umbrella").assertDoesNotExist()
        scrollWeatherTo("weather_daily_section")
        compose.onNodeWithTag("daily_day_6").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, refreshCount) }
    }

    @Test
    fun fahrenheitAppliesToHourlyAndDailyTemperatures() {
        showApp()
        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("unit_FAHRENHEIT").performScrollTo().performClick()
        compose.onNodeWithTag("weather_tab").performClick()
        scrollWeatherTo("weather_hourly_section")
        compose.onNodeWithTag("rain_selected_temperature", useUnmergedTree = true).performScrollTo().assertTextEquals("77°F")
        scrollWeatherTo("weather_daily_section")
        compose.onNodeWithTag("daily_low_0", useUnmergedTree = true)
            .performScrollTo().assertTextEquals("68°F")
        compose.onNodeWithTag("daily_high_0", useUnmergedTree = true).assertTextEquals("86°F")
        compose.onNodeWithTag("daily_day_6").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, refreshCount) }
    }

    @Test
    fun missingDailyTemperaturesStayUnavailableAndDaySevenRemainsReadable() {
        val weather = sampleWeather()
        showApp(
            UtilityUiState(weather = weather.copy(dailyForecast = weather.dailyForecast.mapIndexed { index, day ->
                if (index == 0) day.copy(temperatureMinCelsius = null, temperatureMaxCelsius = null) else day
            }), now = now),
            darkTheme = true,
            fontScale = 2f
        )
        scrollWeatherTo("weather_daily_section")
        compose.onNodeWithTag("daily_low_0", useUnmergedTree = true)
            .performScrollTo().assertTextEquals("—")
        compose.onNodeWithTag("daily_high_0", useUnmergedTree = true).assertTextEquals("—")
        compose.onNodeWithTag("daily_day_6").performScrollTo().assertIsDisplayed()
    }

    private fun selectVisibleHour(indexInPage: Int) {
        scrollWeatherTo("weather_hourly_section")
        compose.onNodeWithTag("hourly_list").performScrollTo().performScrollToIndex(indexInPage)
        compose.onNodeWithTag("rain_period_$indexInPage").performScrollTo().performClick()
    }

    private fun scrollWeatherTo(tag: String) {
        compose.onNodeWithTag("weather_screen").performScrollToNode(hasTestTag(tag))
    }

    private fun assertOnlyRainPeriodSelected(selectedIndex: Int) {
        repeat(3) { index ->
            val tile = compose.onNodeWithTag("rain_period_$index").assertHasClickAction()
            if (index == selectedIndex) tile.assertIsSelected() else tile.assertIsNotSelected()
        }
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
                    probabilityPercent = probability,
                    temperatureCelsius = 25.0 + index,
                    weatherCode = 3,
                    weatherDescription = "Overcast",
                    isDay = true
                )
            },
            dailyForecast = (0 until 7).map { index ->
                DailyForecast(
                    date = LocalDate.of(2026, 10, 15).plusDays(index.toLong()),
                    temperatureMinCelsius = 20.0 + index,
                    temperatureMaxCelsius = 30.0 + index,
                    weatherCode = 2,
                    weatherDescription = "Partly cloudy",
                    maxPrecipitationProbability = 30.0 + index
                )
            }
        )
    }
}

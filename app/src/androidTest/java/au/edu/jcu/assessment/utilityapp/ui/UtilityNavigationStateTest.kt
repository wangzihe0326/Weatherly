package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.DailyForecast
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.ui.components.periodLabel
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** Exercises navigation with deterministic weather; no network or production Activity is launched. */
@RunWith(AndroidJUnit4::class)
class UtilityNavigationStateTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: MutableState<UtilityUiState>
    private val now = Instant.parse("2026-10-15T05:20:00Z")

    @Test
    fun changingUnitsPreservesChosenHourAndBothScreensScrollPositions() {
        showApp()
        chooseHour(19)
        // Browsing is independent of selection: returning must not snap the row back to hour 20.
        compose.onNodeWithTag("hourly_list").performScrollToIndex(17)
        val weatherScroll = verticalScroll("weather_screen")
        val hourlyScroll = horizontalScroll()
        assertTrue(weatherScroll > 0f)

        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("unit_FAHRENHEIT").performScrollTo().performClick()
        compose.onNodeWithTag("threshold_80").performScrollTo()
        val settingsScroll = verticalScroll("settings_screen")
        assertTrue(settingsScroll > 0f)
        compose.onNodeWithTag("weather_tab").performClick()

        assertEquals(weatherScroll, verticalScroll("weather_screen"), 1f)
        assertEquals(hourlyScroll, horizontalScroll(), 0.001f)
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(state.value.weather!!.forecastPeriods[19], "Asia/Singapore"))
        compose.onNodeWithTag("rain_selected_temperature", useUnmergedTree = true).assertTextEquals("77°F")
        compose.onNodeWithTag("rain_period_19").assertIsSelected()
        compose.onNodeWithTag("settings_tab").performClick()
        assertEquals(settingsScroll, verticalScroll("settings_screen"), 1f)
        compose.onNodeWithTag("threshold_80").assertIsDisplayed()
    }

    @Test
    fun savedStateRestorationWhileInSettingsKeepsTheInactiveWeatherPage() {
        val restoration = StateRestorationTester(compose)
        showApp(restoration = restoration)
        chooseHour(19)
        compose.onNodeWithTag("hourly_list").performScrollToIndex(17)
        val weatherScroll = verticalScroll("weather_screen")
        val hourlyScroll = horizontalScroll()
        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("threshold_80").performScrollTo()
        val settingsScroll = verticalScroll("settings_screen")

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("settings_tab").assertIsSelected()
        assertEquals(settingsScroll, verticalScroll("settings_screen"), 1f)
        compose.onNodeWithTag("weather_tab").performClick()
        compose.onNodeWithTag("rain_period_19").assertIsSelected()
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(state.value.weather!!.forecastPeriods[19], "Asia/Singapore"))
        assertEquals(hourlyScroll, horizontalScroll(), 0.001f)
        assertEquals(weatherScroll, verticalScroll("weather_screen"), 1f)
    }

    @Test
    fun changingCitiesDoesNotReuseAnotherCitysSelectionOrScroll() {
        showApp(City.TOWNSVILLE)
        chooseHour(19)
        compose.onNodeWithTag("settings_tab").performClick()
        // Townsville and Cairns share a time zone, so the city must identify the saved state.
        compose.onNodeWithTag("city_CAIRNS").performScrollTo().performClick()
        compose.onNodeWithTag("weather_tab").performClick()

        compose.onNodeWithTag("current_temperature").assertIsDisplayed()
        assertEquals(0f, verticalScroll("weather_screen"), 1f)
        compose.onNodeWithTag("weather_screen").performScrollToNode(hasTestTag("weather_hourly_section"))
        assertEquals(0f, horizontalScroll(), 0.001f)
        compose.onNodeWithTag("rain_period_0").assertIsSelected()
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(state.value.weather!!.forecastPeriods.first(), "Australia/Brisbane"))
    }

    @Test
    fun returningToAVisitedCityRestoresBrowsingAfterItsLoadingPage() {
        showApp(deferCityLoads = true)
        chooseHour(19)
        compose.onNodeWithTag("hourly_list").performScrollToIndex(17)
        val weatherScroll = verticalScroll("weather_screen")
        val hourlyScroll = horizontalScroll()
        assertTrue(weatherScroll > 0f)

        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("city_CAIRNS").performScrollTo().performClick()
        compose.onNodeWithTag("weather_tab").performClick()
        compose.onNodeWithTag("current_temperature").assertDoesNotExist()
        compose.runOnIdle {
            state.value = state.value.copy(weather = sampleWeather(City.CAIRNS), isLoading = false)
        }
        compose.onNodeWithTag("current_temperature").assertIsDisplayed()

        compose.onNodeWithTag("settings_tab").performClick()
        compose.onNodeWithTag("city_SINGAPORE").performScrollTo().performClick()
        compose.onNodeWithTag("weather_tab").performClick()
        // Let the shorter loading page complete layout before delivering the response.
        compose.onNodeWithTag("current_temperature").assertDoesNotExist()
        assertEquals(0f, verticalScroll("weather_screen"), 1f)
        compose.runOnIdle {
            val fetchedAt = now.plusSeconds(30)
            state.value = state.value.copy(
                weather = sampleWeather(City.SINGAPORE).copy(fetchedAt = fetchedAt),
                isLoading = false,
                now = fetchedAt
            )
        }

        assertEquals(weatherScroll, verticalScroll("weather_screen"), 1f)
        assertEquals(hourlyScroll, horizontalScroll(), 0.001f)
        compose.onNodeWithTag("rain_period_19").assertIsSelected()
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(state.value.weather!!.forecastPeriods[19], "Asia/Singapore"))
    }

    @Test
    fun anHourThatExpiresWhileInSettingsFallsBackToTheFirstAvailableHour() {
        showApp()
        chooseHour(1)
        compose.onNodeWithTag("hourly_list").performScrollToIndex(17)
        compose.onNodeWithTag("settings_tab").performClick()
        compose.runOnIdle {
            val later = now.plusSeconds(7200)
            state.value = state.value.copy(
                now = later,
                weather = state.value.weather!!.copy(fetchedAt = later, observationTime = later)
            )
        }
        compose.onNodeWithTag("weather_tab").performClick()

        assertEquals(0f, horizontalScroll(), 0.001f)
        compose.onNodeWithTag("rain_period_0").assertIsSelected()
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(state.value.hourlyPeriods.first(), "Asia/Singapore"))
    }

    private fun chooseHour(index: Int) {
        compose.onNodeWithTag("weather_screen").performScrollToNode(hasTestTag("weather_hourly_section"))
        compose.onNodeWithTag("hourly_list").performScrollTo().performScrollToIndex(index)
        compose.onNodeWithTag("rain_period_$index").performScrollTo().performClick().assertIsSelected()
    }

    private fun verticalScroll(tag: String): Float = compose.onNodeWithTag(tag)
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    private fun horizontalScroll(): Float = compose.onNodeWithTag("hourly_list")
        .fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value()

    private fun showApp(
        city: City = City.SINGAPORE,
        restoration: StateRestorationTester? = null,
        deferCityLoads: Boolean = false
    ) {
        state = mutableStateOf(
            UtilityUiState(settings = AppSettings(city = city), weather = sampleWeather(city), now = now)
        )
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            MobileTech_UtilityAppTheme {
                UtilityApp(
                    state = state.value,
                    onRefresh = {},
                    onCityChanged = { selectedCity ->
                        state.value = state.value.copy(
                            settings = state.value.settings.copy(city = selectedCity),
                            weather = if (deferCityLoads) null else sampleWeather(selectedCity),
                            isLoading = deferCityLoads
                        )
                    },
                    onUnitChanged = { unit ->
                        state.value = state.value.copy(settings = state.value.settings.copy(temperatureUnit = unit))
                    },
                    onUmbrellaEnabledChanged = { enabled ->
                        state.value = state.value.copy(settings = state.value.settings.copy(umbrellaEnabled = enabled))
                    },
                    onThresholdChanged = { threshold ->
                        state.value = state.value.copy(settings = state.value.settings.copy(rainThresholdPercent = threshold))
                    }
                )
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
    }

    private fun sampleWeather(city: City): WeatherSnapshot {
        val start = Instant.parse("2026-10-15T05:00:00Z")
        return WeatherSnapshot(
            city = city,
            temperatureCelsius = 25.0,
            apparentTemperatureCelsius = 30.0,
            weatherDescription = "Cloudy",
            observationTime = now,
            fetchedAt = now,
            timezoneId = if (city == City.SINGAPORE) "Asia/Singapore" else "Australia/Brisbane",
            forecastPeriods = (0 until 24).map { index ->
                RainPeriod(
                    start = start.plusSeconds(index * 3600L),
                    end = start.plusSeconds((index + 1) * 3600L),
                    probabilityPercent = 70.0,
                    temperatureCelsius = 25.0,
                    weatherCode = 3,
                    weatherDescription = "Overcast",
                    isDay = true
                )
            },
            dailyForecast = (0 until 7).map { index ->
                DailyForecast(
                    date = LocalDate.of(2026, 10, 15).plusDays(index.toLong()),
                    temperatureMinCelsius = 20.0,
                    temperatureMaxCelsius = 30.0,
                    weatherCode = 2,
                    weatherDescription = "Partly cloudy",
                    maxPrecipitationProbability = 30.0
                )
            }
        )
    }
}

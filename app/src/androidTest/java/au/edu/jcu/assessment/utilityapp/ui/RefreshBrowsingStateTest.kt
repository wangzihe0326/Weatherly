package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.DailyForecast
import au.edu.jcu.assessment.utilityapp.data.model.WeatherCondition
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.ui.components.periodLabel
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** Deterministic state transitions exercise rendering without waiting for real forecast expiry. */
@RunWith(AndroidJUnit4::class)
class RefreshBrowsingStateTest {
    @get:Rule val compose = createComposeRule()

    private val now = Instant.parse("2026-10-18T05:20:00Z")
    private lateinit var state: MutableState<UtilityUiState>
    private var refreshCount = 0

    @Test
    fun loadingFailureAndRecoveryPreserveVisibleHourAndScreenPosition() {
        showApp()
        chooseHourAndBrowseElsewhere()
        val rowBefore = bounds("hourly_list")
        val visibleHourBefore = bounds("rain_period_17")
        val selected = state.value.hourlyPeriods[19]

        compose.runOnIdle { state.value = state.value.copy(isLoading = true) }
        assertBrowsingUnchanged(rowBefore, visibleHourBefore, visibleIndex = 17, selectedIndex = 19, selected)

        compose.runOnIdle {
            state.value = state.value.copy(
                isLoading = false,
                error = WeatherLoadError.NETWORK,
                nextRetryAt = now.plusSeconds(60)
            )
        }
        assertBrowsingUnchanged(rowBefore, visibleHourBefore, visibleIndex = 17, selectedIndex = 19, selected)

        compose.runOnIdle {
            state.value = state.value.copy(isLoading = true, error = null, nextRetryAt = null)
        }
        assertBrowsingUnchanged(rowBefore, visibleHourBefore, visibleIndex = 17, selectedIndex = 19, selected)

        deliverNextHourForecast()
        assertBrowsingUnchanged(rowBefore, visibleHourBefore, visibleIndex = 16, selectedIndex = 18, selected)
        compose.onNodeWithTag("rain_selected_probability", useUnmergedTree = true)
            .assertTextEquals("65% chance of rain during this hour.")
        compose.onNodeWithTag("rain_selected_temperature", useUnmergedTree = true).assertTextEquals("26°C")
    }

    @Test
    fun forecastAdvancingWhileHourlyIsOffscreenKeepsItsTimestampAnchorOnReturn() {
        showApp()
        chooseHourAndBrowseElsewhere()
        val visibleHourLeft = bounds("rain_period_17").left
        val selected = state.value.hourlyPeriods[19]
        showSection("daily")
        compose.onNodeWithTag("daily_day_6").performScrollTo().assertIsDisplayed()
        val dayBefore = bounds("daily_day_6")

        compose.runOnIdle { state.value = state.value.copy(isLoading = true) }
        assertBoundsEqual(dayBefore, bounds("daily_day_6"))
        deliverNextHourForecast()
        assertBoundsEqual(dayBefore, bounds("daily_day_6"))

        showSection("hourly")
        compose.onNodeWithTag("hourly_list").performScrollTo()
        compose.onNodeWithTag("rain_period_18").assertIsSelected()
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(selected, "Asia/Singapore"))
        assertEquals(visibleHourLeft, bounds("rain_period_16").left, 1f)
    }

    @Test
    fun expiredSelectionSafelyReturnsToTheCurrentHourWithoutMovingThePage() {
        showApp()
        showSection("hourly")
        compose.onNodeWithTag("hourly_list").performScrollTo().performScrollToIndex(1)
        compose.onNodeWithTag("rain_period_1").performClick().assertIsSelected()
        compose.onNodeWithTag("hourly_list").performScrollToIndex(17)
        val rowBefore = bounds("hourly_list")

        compose.runOnIdle {
            val later = now.plusSeconds(7200)
            state.value = state.value.copy(
                now = later,
                weather = state.value.weather!!.copy(fetchedAt = later, observationTime = later)
            )
        }

        compose.onNodeWithTag("rain_period_0").assertIsSelected().assertIsDisplayed()
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(state.value.hourlyPeriods.first(), "Asia/Singapore"))
        assertBoundsEqual(rowBefore, bounds("hourly_list"))
    }

    @Test
    fun failedFirstLoadOffersAnAccessibleRetryAndNeverInventsWeather() {
        showApp(UtilityUiState(isLoading = true, now = now))
        compose.onNodeWithTag("refresh_status")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.runOnIdle {
            state.value = state.value.copy(
                isLoading = false,
                error = WeatherLoadError.NETWORK,
                nextRetryAt = now.plusSeconds(60)
            )
        }
        compose.onNodeWithTag("current_temperature").assertDoesNotExist()
        compose.onNodeWithTag("refresh_status")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.runOnIdle {
            state.value = state.value.copy(nextRetryAt = null, automaticRetriesExhausted = true)
        }
        compose.onNodeWithTag("retry_button").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, refreshCount) }
    }

    private fun chooseHourAndBrowseElsewhere() {
        showSection("hourly")
        compose.onNodeWithTag("hourly_list").performScrollTo().performScrollToIndex(19)
        compose.onNodeWithTag("rain_period_19").performClick().assertIsSelected()
        // A visible-hour anchor must be separate from the selected-hour timestamp.
        compose.onNodeWithTag("hourly_list").performScrollToIndex(17)
        compose.onNodeWithTag("rain_period_17").assertIsDisplayed()
    }

    private fun deliverNextHourForecast() {
        compose.runOnIdle {
            val later = now.plusSeconds(3600)
            state.value = state.value.copy(
                isLoading = false,
                error = null,
                nextRetryAt = null,
                now = later,
                weather = state.value.weather!!.copy(
                    fetchedAt = later,
                    observationTime = later,
                    forecastPeriods = state.value.weather!!.forecastPeriods.map {
                        it.copy(probabilityPercent = 65.0, temperatureCelsius = 26.0)
                    }
                )
            )
        }
    }

    private fun assertBrowsingUnchanged(
        rowBefore: Rect,
        visibleBefore: Rect,
        visibleIndex: Int,
        selectedIndex: Int,
        selected: RainPeriod
    ) {
        assertBoundsEqual(rowBefore, bounds("hourly_list"))
        assertBoundsEqual(visibleBefore, bounds("rain_period_$visibleIndex"))
        compose.onNodeWithTag("rain_period_$selectedIndex").assertIsSelected()
        compose.onNodeWithTag("rain_selected_period", useUnmergedTree = true)
            .assertTextEquals(periodLabel(selected, "Asia/Singapore"))
    }

    private fun assertBoundsEqual(expected: Rect, actual: Rect) {
        assertEquals("Left edge moved", expected.left, actual.left, 1f)
        assertEquals("Top edge moved", expected.top, actual.top, 1f)
        assertEquals("Right edge moved", expected.right, actual.right, 1f)
        assertEquals("Bottom edge moved", expected.bottom, actual.bottom, 1f)
    }

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun showSection(key: String) {
        compose.onNodeWithTag("weather_screen").performScrollToKey(key)
    }

    private fun showApp(initial: UtilityUiState = UtilityUiState(weather = weather(), now = now)) {
        state = mutableStateOf(initial)
        compose.setContent {
            MobileTech_UtilityAppTheme {
                UtilityApp(
                    state = state.value,
                    onRefresh = { refreshCount++ },
                    onCityChanged = {},
                    onUnitChanged = {},
                    onUmbrellaEnabledChanged = {},
                    onThresholdChanged = {}
                )
            }
        }
    }

    private fun weather(): WeatherSnapshot {
        val start = Instant.parse("2026-10-18T05:00:00Z")
        return WeatherSnapshot(
            city = City.SINGAPORE,
            temperatureCelsius = 25.0,
            apparentTemperatureCelsius = 30.0,
            condition = WeatherCondition.OVERCAST,
            observationTime = now,
            fetchedAt = now,
            timezoneId = "Asia/Singapore",
            forecastPeriods = (0 until 24).map { index ->
                RainPeriod(
                    start = start.plusSeconds(index * 3600L),
                    end = start.plusSeconds((index + 1) * 3600L),
                    probabilityPercent = 70.0,
                    temperatureCelsius = 25.0,
                    condition = WeatherCondition.OVERCAST,
                    isDay = true
                )
            },
            dailyForecast = (0 until 7).map { index ->
                DailyForecast(
                    date = LocalDate.of(2026, 10, 18).plusDays(index.toLong()),
                    temperatureMinCelsius = 20.0,
                    temperatureMaxCelsius = 30.0,
                    condition = WeatherCondition.PARTLY_CLOUDY,
                    maxPrecipitationProbability = 30.0
                )
            }
        )
    }
}

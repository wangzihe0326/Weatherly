package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.assessment.utilityapp.data.model.DailyForecast
import au.edu.jcu.assessment.utilityapp.data.model.WeatherCondition
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.ui.components.DailyForecastCard
import au.edu.jcu.assessment.utilityapp.ui.components.RainForecastCard
import au.edu.jcu.assessment.utilityapp.ui.components.periodLabel
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ForecastAccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 15)
    private val hour = RainPeriod(
        start = Instant.parse("2026-10-15T05:00:00Z"),
        end = Instant.parse("2026-10-15T06:00:00Z"),
        probabilityPercent = 30.0,
        temperatureCelsius = 25.0,
        condition = WeatherCondition.PARTLY_CLOUDY
    )
    private val day = DailyForecast(
        date = today,
        temperatureMinCelsius = 20.0,
        temperatureMaxCelsius = 30.0,
        condition = WeatherCondition.PARTLY_CLOUDY,
        maxPrecipitationProbability = 60.0
    )

    @Test
    fun laterUpdatesOneCompletePoliteAnnouncementWithoutAddingAnAction() {
        val next = hour.copy(
            start = hour.end, end = hour.end.plusSeconds(3600),
            temperatureCelsius = 26.0, probabilityPercent = 70.0, condition = WeatherCondition.MODERATE_RAIN
        )
        showForecast(periods = listOf(hour, next))
        compose.onNodeWithTag("rain_period_0")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
        compose.onNodeWithTag("rain_period_1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsNotSelected()
        val details = compose.onNodeWithTag("hourly_details")
        details.assertContentDescriptionEquals(
            "Selected hour. ${periodLabel(hour, "Asia/Singapore")}. Partly cloudy. Temperature 25°C. 30% chance of rain during this hour."
        )
        compose.onNodeWithTag("hourly_next").performScrollTo().performClick()
        compose.onNodeWithTag("rain_period_1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
        details.assertContentDescriptionEquals(
            "Selected hour. ${periodLabel(next, "Asia/Singapore")}. Moderate rain. Temperature 26°C. 70% chance of rain during this hour."
        ).assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assertHasNoClickAction()
        assertTrue(details.fetchSemanticsNode().children.isEmpty())
    }

    @Test
    fun selectedHourDescribesMissingValuesInsteadOfReadingADashOrZero() {
        val missing = hour.copy(temperatureCelsius = null, probabilityPercent = null, condition = WeatherCondition.UNAVAILABLE)
        showForecast(periods = listOf(missing))
        compose.onNodeWithTag("hourly_details").assertContentDescriptionEquals(
            "Selected hour. ${periodLabel(missing, "Asia/Singapore")}. Weather unavailable. Temperature unavailable. Rain probability is unavailable for this hour."
        )
        compose.onNodeWithTag("rain_selected_temperature", useUnmergedTree = true).assertTextEquals("—")
    }

    @Test
    fun eachDayIsOnePassiveElementWithDateConditionAndQualifiedValuesInOrder() {
        showForecast(days = listOf(day, day.copy(date = today.plusDays(1))))
        listOf("Today" to today, "Tomorrow" to today.plusDays(1)).forEachIndexed { index, (label, date) ->
            val fullDate = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
            val row = compose.onNodeWithTag("daily_day_$index")
            row.assertContentDescriptionEquals(
                "$label, $fullDate. Partly cloudy. Low temperature 20°C. High temperature 30°C. Peak hourly rain probability 60%."
            ).assertHasNoClickAction()
            assertTrue(row.fetchSemanticsNode().children.isEmpty())
        }
    }

    @Test
    fun dailyDescriptionsPreservePartialDataAndRejectInvertedOrNonFiniteValues() {
        val days = listOf(
            day.copy(temperatureMaxCelsius = null, maxPrecipitationProbability = null),
            day.copy(date = today.plusDays(1), temperatureMinCelsius = 30.0, temperatureMaxCelsius = 10.0, maxPrecipitationProbability = 200.0),
            day.copy(date = today.plusDays(2), temperatureMinCelsius = Double.NaN, temperatureMaxCelsius = Double.POSITIVE_INFINITY)
        )
        showForecast(days = days)
        val summaries = (0..2).map { index ->
            compose.onNodeWithTag("daily_day_$index").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
        }
        assertTrue(summaries[0].contains("Low temperature 20°C. High temperature unavailable. Peak rain unavailable."))
        assertTrue(summaries[1].contains("Low temperature unavailable. High temperature unavailable. Peak rain unavailable."))
        assertTrue(summaries[2].contains("Low temperature unavailable. High temperature unavailable. Peak hourly rain probability 60%."))
        compose.onNodeWithTag("daily_low_0", useUnmergedTree = true).assertTextEquals("20°C")
        compose.onNodeWithTag("daily_low_1", useUnmergedTree = true).assertTextEquals("—")
        compose.onNodeWithTag("daily_high_1", useUnmergedTree = true).assertTextEquals("—")
    }

    @Test
    fun largeTextAndUnitChangesKeepTheSpokenSummaryConsistentWithTheDisplay() {
        val unit = mutableStateOf(TemperatureUnit.CELSIUS)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MobileTech_UtilityAppTheme {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        RainForecastCard(listOf(hour), "Asia/Singapore", unit.value)
                        DailyForecastCard(listOf(day), unit.value, today)
                    }
                }
            }
        }
        compose.runOnIdle { unit.value = TemperatureUnit.FAHRENHEIT }
        compose.onNodeWithTag("hourly_details").assertContentDescriptionEquals(
            "Selected hour. ${periodLabel(hour, "Asia/Singapore")}. Partly cloudy. Temperature 77°F. 30% chance of rain during this hour."
        )
        val fullDate = today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
        compose.onNodeWithTag("daily_day_0").assertContentDescriptionEquals(
            "Today, $fullDate. Partly cloudy. Low temperature 68°F. High temperature 86°F. Peak hourly rain probability 60%."
        )
        compose.onNodeWithTag("daily_low_0", useUnmergedTree = true).assertTextEquals("68°F")
        compose.onNodeWithTag("daily_high_0", useUnmergedTree = true).assertTextEquals("86°F")
    }

    private fun showForecast(periods: List<RainPeriod> = emptyList(), days: List<DailyForecast> = emptyList()) {
        compose.setContent {
            MobileTech_UtilityAppTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (periods.isNotEmpty()) RainForecastCard(periods, "Asia/Singapore")
                    if (days.isNotEmpty()) DailyForecastCard(days, TemperatureUnit.CELSIUS, today)
                }
            }
        }
    }
}

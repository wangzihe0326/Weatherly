package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.remote.CurrentWeatherResponse
import au.edu.jcu.assessment.utilityapp.data.remote.DailyWeatherResponse
import au.edu.jcu.assessment.utilityapp.data.remote.HourlyWeatherResponse
import au.edu.jcu.assessment.utilityapp.data.remote.WeatherResponse
import au.edu.jcu.assessment.utilityapp.data.repository.mapWeatherResponse
import au.edu.jcu.assessment.utilityapp.ui.components.DailyForecastCard
import au.edu.jcu.assessment.utilityapp.ui.components.RainForecastCard
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherSummaryCard
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

/** Exercises provider mapping and the actual cards, including their accessibility descriptions. */
@RunWith(AndroidJUnit4::class)
class WeatherPresentationTest {
    @get:Rule val compose = createComposeRule()

    private val now = Instant.parse("2026-10-15T05:20:00Z")
    private val hourStart = Instant.parse("2026-10-15T05:00:00Z")
    private val today = now.atZone(ZoneId.of("Asia/Singapore")).toLocalDate()

    @Test
    fun providerConditionsStayConsistentAcrossCurrentHourlyDailyAndSpokenText() {
        val weather = mutableStateOf(mappedWeather(63))
        compose.setContent {
            MobileTech_UtilityAppTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    WeatherSummaryCard(weather.value, TemperatureUnit.CELSIUS)
                    RainForecastCard(
                        weather.value.forecastPeriods.filter { it.start == hourStart },
                        weather.value.timezoneId,
                        TemperatureUnit.CELSIUS
                    )
                    DailyForecastCard(weather.value.dailyForecast, TemperatureUnit.CELSIUS, today)
                }
            }
        }

        // Unknown provider codes must remain unavailable, while usable temperatures survive.
        listOf(
            63 to "Moderate rain",
            96 to "Thunderstorm with slight hail",
            999 to "Weather unavailable"
        ).forEach { (code, description) ->
            compose.runOnIdle { weather.value = mappedWeather(code) }
            listOf("weather_summary", "hourly_details", "daily_day_0").forEach { parentTag ->
                compose.onNode(
                    hasText(description) and hasAnyAncestor(hasTestTag(parentTag)),
                    useUnmergedTree = true
                ).performScrollTo().assertIsDisplayed().assertTextEquals(description)
            }
            compose.onNodeWithTag("hourly_details")
                .assertContentDescriptionContains("$description. Temperature 25°C.", substring = true)
            compose.onNodeWithTag("daily_day_0")
                .assertContentDescriptionContains("$description. Low temperature 20°C. High temperature 30°C.", substring = true)
            compose.onNodeWithTag("current_temperature").assertTextEquals("25°C")
            compose.onNodeWithTag("rain_selected_temperature", useUnmergedTree = true).assertTextEquals("25°C")
            compose.onNodeWithTag("daily_low_0", useUnmergedTree = true).assertTextEquals("20°C")
            compose.onNodeWithTag("daily_high_0", useUnmergedTree = true).assertTextEquals("30°C")
        }
    }

    private fun mappedWeather(code: Int) = mapWeatherResponse(
        response = WeatherResponse(
            timezone = "Asia/Singapore",
            current = CurrentWeatherResponse(
                time = now.epochSecond,
                temperatureCelsius = 25.0,
                apparentTemperatureCelsius = 30.0,
                weatherCode = code,
                isDay = 1
            ),
            hourly = HourlyWeatherResponse(
                time = listOf(hourStart.epochSecond, hourStart.plusSeconds(3600).epochSecond),
                temperatureCelsius = listOf(25.0, 26.0),
                precipitationProbability = listOf(20.0, 60.0),
                weatherCode = listOf(code, code),
                isDay = listOf(1, 1)
            ),
            daily = DailyWeatherResponse(
                time = listOf(today.atStartOfDay(ZoneId.of("Asia/Singapore")).toEpochSecond()),
                temperatureMinCelsius = listOf(20.0),
                temperatureMaxCelsius = listOf(30.0),
                weatherCode = listOf(code),
                maxPrecipitationProbability = listOf(60.0)
            )
        ),
        city = City.SINGAPORE,
        fetchedAt = now
    )
}

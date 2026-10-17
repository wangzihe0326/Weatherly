package au.edu.jcu.assessment.utilityapp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.ui.components.AdviceCard
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherSummaryCard
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.util.Locale

/** Deterministic UI fixtures; the production app continues to use live weather. */
@RunWith(AndroidJUnit4::class)
class OverviewDetailsTest {
    @get:Rule val compose = createComposeRule()

    private val now = Instant.parse("2026-10-16T14:20:00Z")
    private val start = Instant.parse("2026-10-16T14:00:00Z")
    private lateinit var originalLocale: Locale

    @Before fun useEnglishFixtureDates() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @After fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun triggerNamesTheFirstQualifyingHourAcrossMidnightAndUpdatesWithThreshold() {
        val state = mutableStateOf(UtilityUiState(weather = weather(), now = now))
        compose.setContent {
            MobileTech_UtilityAppTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    AdviceCard(state.value.advice, "Asia/Singapore")
                }
            }
        }
        compose.onNodeWithTag("umbrella_trigger").assertContentDescriptionEquals(
            "First hour at or above your 60% threshold. Fri 16 Oct, 23:00–Sat 17 Oct, 00:00. " +
                "Hourly rain probability 60%. Times shown in Asia/Singapore."
        ).assertHasNoClickAction()
        compose.onNodeWithTag("umbrella_trigger_probability", useUnmergedTree = true)
            .assertTextEquals("60% hourly rain chance · city time")
        assertTrue(compose.onNodeWithTag("umbrella_trigger").fetchSemanticsNode().children.isEmpty())

        compose.runOnIdle {
            state.value = state.value.copy(settings = state.value.settings.copy(rainThresholdPercent = 80))
        }
        compose.onNodeWithTag("umbrella_trigger").assertContentDescriptionEquals(
            "First hour at or above your 80% threshold. Sat 17 Oct · 00:00–01:00. " +
                "Hourly rain probability 90%. Times shown in Asia/Singapore."
        )
        compose.runOnIdle {
            state.value = state.value.copy(now = now.plusSeconds(1800))
        }
        compose.onNodeWithTag("umbrella_trigger").assertDoesNotExist()
        compose.onNodeWithText("Not enough current data for advice").assertIsDisplayed()
    }

    @Test
    fun darkLargeTextKeepsTheWholeTriggerReadableWithoutAClippedSingleLine() {
        val state = UtilityUiState(weather = weather(), now = now)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MobileTech_UtilityAppTheme(darkTheme = true) {
                    Column(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("UI test fixture · large text", color = MaterialTheme.colorScheme.onBackground)
                        AdviceCard(state.advice, "Asia/Singapore")
                    }
                }
            }
        }
        compose.onNodeWithTag("umbrella_trigger_interval", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
            .assertTextEquals("Fri 16 Oct, 23:00–Sat 17 Oct, 00:00")
        compose.onNodeWithTag("umbrella_trigger_probability", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        captureIfRequested("trigger-dark-large-text")
    }

    @Test
    fun currentArtworkRespondsToProviderDaylightInBothThemes() {
        val current = mutableStateOf(weather().copy(weatherCode = 0, weatherDescription = "Clear sky", isDay = true))
        val dark = mutableStateOf(false)
        val label = mutableStateOf("UI test fixture · clear day")
        compose.setContent {
            MobileTech_UtilityAppTheme(darkTheme = dark.value) {
                Column(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(label.value, color = MaterialTheme.colorScheme.onBackground)
                    WeatherSummaryCard(current.value, TemperatureUnit.CELSIUS)
                    AdviceCard(UtilityUiState(weather = current.value, now = now).advice, "Asia/Singapore")
                }
            }
        }
        for (darkTheme in listOf(false, true)) {
            compose.runOnIdle { dark.value = darkTheme }
            for (code in listOf(0, 2)) {
                val captures = listOf(true, false, null).map { daylight ->
                    val name = "${if (darkTheme) "dark" else "light"}-${if (code == 0) "clear" else "partly-cloudy"}-${daylight ?: "unknown"}"
                    compose.runOnIdle {
                        current.value = current.value.copy(
                            weatherCode = code, isDay = daylight,
                            weatherDescription = if (code == 0) "Clear sky" else "Partly cloudy"
                        )
                        label.value = "UI test fixture · $name"
                    }
                    val artwork = compose.onNodeWithTag("summary_artwork", useUnmergedTree = true)
                        .assertIsDisplayed().captureToImage()
                    val pixels = IntArray(artwork.width * artwork.height)
                    artwork.readPixels(pixels)
                    captureIfRequested(name)
                    pixels
                }
                assertFalse("Day and night must render differently", captures[0].contentEquals(captures[1]))
                assertFalse("Missing daylight must not imply daytime", captures[0].contentEquals(captures[2]))
                assertFalse("Missing daylight must not imply nighttime", captures[1].contentEquals(captures[2]))
            }
        }
    }

    private fun weather() = WeatherSnapshot(
        city = City.SINGAPORE,
        temperatureCelsius = 27.0,
        apparentTemperatureCelsius = 30.0,
        weatherDescription = "Clear sky",
        observationTime = now,
        fetchedAt = now,
        timezoneId = "Asia/Singapore",
        weatherCode = 0,
        isDay = false,
        forecastPeriods = listOf(20.0, 60.0, 90.0).mapIndexed { index, probability ->
            RainPeriod(start.plusSeconds(index * 3600L), start.plusSeconds((index + 1) * 3600L), probability)
        }
    )

    private fun captureIfRequested(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureScreenshots") != "true") return
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "overview-test-screenshots")
        check(directory.isDirectory || directory.mkdirs())
        val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(directory, "$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            screenshot.recycle()
        }
    }
}

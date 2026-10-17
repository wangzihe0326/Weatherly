package au.edu.jcu.assessment.utilityapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherArtworkTest {
    @Test
    fun clearAndPartlyCloudyConditionsUseTheReportedDaylight() {
        assertEquals(WeatherArtworkVariant.CLEAR_DAY, weatherArtworkVariant(0, true))
        assertEquals(WeatherArtworkVariant.CLEAR_NIGHT, weatherArtworkVariant(0, false))
        for (code in listOf(1, 2)) {
            assertEquals(WeatherArtworkVariant.PARTLY_CLOUDY_DAY, weatherArtworkVariant(code, true))
            assertEquals(WeatherArtworkVariant.PARTLY_CLOUDY_NIGHT, weatherArtworkVariant(code, false))
        }
    }

    @Test
    fun missingDaylightDoesNotInventASunOrMoon() {
        assertEquals(WeatherArtworkVariant.CLEAR_NEUTRAL, weatherArtworkVariant(0, null))
        for (code in listOf(1, 2)) {
            assertEquals(WeatherArtworkVariant.PARTLY_CLOUDY_NEUTRAL, weatherArtworkVariant(code, null))
        }
    }

    @Test
    fun precipitationAndObscuredSkyKeepTheirConditionAcrossDaylightStates() {
        val conditions = mapOf(
            WeatherArtworkVariant.CLOUD to listOf(3),
            WeatherArtworkVariant.FOG to listOf(45, 48),
            WeatherArtworkVariant.RAIN to listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82),
            WeatherArtworkVariant.SNOW to listOf(71, 73, 75, 77, 85, 86),
            WeatherArtworkVariant.STORM to listOf(95, 96, 97, 99)
        )
        for ((variant, codes) in conditions) {
            for (code in codes) {
                for (isDay in listOf(true, false, null)) {
                    assertEquals("Weather code $code, daylight $isDay", variant, weatherArtworkVariant(code, isDay))
                }
            }
        }
    }

    @Test
    fun missingAndUnrecognisedWeatherRemainUnknownEvenWithDaylight() {
        for (code in listOf(null, -1, 4, 50, 98, 999)) {
            for (isDay in listOf(true, false, null)) {
                assertEquals(WeatherArtworkVariant.UNKNOWN, weatherArtworkVariant(code, isDay))
            }
        }
    }
}

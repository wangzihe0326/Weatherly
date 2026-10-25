package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.data.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherConditionTest {
    @Test
    fun everyRecognizedCodeHasItsOwnTypedCondition() {
        val expected = mapOf(
            0 to WeatherCondition.CLEAR_SKY,
            1 to WeatherCondition.MAINLY_CLEAR,
            2 to WeatherCondition.PARTLY_CLOUDY,
            3 to WeatherCondition.OVERCAST,
            45 to WeatherCondition.FOG,
            48 to WeatherCondition.DEPOSITING_RIME_FOG,
            51 to WeatherCondition.LIGHT_DRIZZLE,
            53 to WeatherCondition.MODERATE_DRIZZLE,
            55 to WeatherCondition.DENSE_DRIZZLE,
            56 to WeatherCondition.LIGHT_FREEZING_DRIZZLE,
            57 to WeatherCondition.DENSE_FREEZING_DRIZZLE,
            61 to WeatherCondition.SLIGHT_RAIN,
            63 to WeatherCondition.MODERATE_RAIN,
            65 to WeatherCondition.HEAVY_RAIN,
            66 to WeatherCondition.LIGHT_FREEZING_RAIN,
            67 to WeatherCondition.HEAVY_FREEZING_RAIN,
            71 to WeatherCondition.SLIGHT_SNOWFALL,
            73 to WeatherCondition.MODERATE_SNOWFALL,
            75 to WeatherCondition.HEAVY_SNOWFALL,
            77 to WeatherCondition.SNOW_GRAINS,
            80 to WeatherCondition.SLIGHT_RAIN_SHOWERS,
            81 to WeatherCondition.MODERATE_RAIN_SHOWERS,
            82 to WeatherCondition.VIOLENT_RAIN_SHOWERS,
            85 to WeatherCondition.SLIGHT_SNOW_SHOWERS,
            86 to WeatherCondition.HEAVY_SNOW_SHOWERS,
            95 to WeatherCondition.THUNDERSTORM,
            96 to WeatherCondition.THUNDERSTORM_WITH_SLIGHT_HAIL,
            97 to WeatherCondition.HEAVY_THUNDERSTORM,
            99 to WeatherCondition.THUNDERSTORM_WITH_HEAVY_HAIL
        )

        expected.forEach { (code, condition) ->
            assertEquals("Code $code", condition, WeatherCondition.fromCode(code))
            assertEquals(code, condition.wmoCode)
        }
    }

    @Test
    fun missingAndUnknownCodesStayUnavailableInsteadOfBecomingClear() {
        for (code in listOf(null, -1, 4, 44, 50, 98, 100, Int.MAX_VALUE)) {
            val condition = WeatherCondition.fromCode(code)
            assertEquals("Code $code", WeatherCondition.UNAVAILABLE, condition)
            assertNull(condition.wmoCode)
        }
    }
}

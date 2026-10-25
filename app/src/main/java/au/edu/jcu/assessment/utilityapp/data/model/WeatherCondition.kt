package au.edu.jcu.assessment.utilityapp.data.model

/** Weather conditions supported by the app; unknown codes never imply clear weather. */
enum class WeatherCondition(val wmoCode: Int?) {
    UNAVAILABLE(null),
    CLEAR_SKY(0),
    MAINLY_CLEAR(1),
    PARTLY_CLOUDY(2),
    OVERCAST(3),
    FOG(45),
    DEPOSITING_RIME_FOG(48),
    LIGHT_DRIZZLE(51),
    MODERATE_DRIZZLE(53),
    DENSE_DRIZZLE(55),
    LIGHT_FREEZING_DRIZZLE(56),
    DENSE_FREEZING_DRIZZLE(57),
    SLIGHT_RAIN(61),
    MODERATE_RAIN(63),
    HEAVY_RAIN(65),
    LIGHT_FREEZING_RAIN(66),
    HEAVY_FREEZING_RAIN(67),
    SLIGHT_SNOWFALL(71),
    MODERATE_SNOWFALL(73),
    HEAVY_SNOWFALL(75),
    SNOW_GRAINS(77),
    SLIGHT_RAIN_SHOWERS(80),
    MODERATE_RAIN_SHOWERS(81),
    VIOLENT_RAIN_SHOWERS(82),
    SLIGHT_SNOW_SHOWERS(85),
    HEAVY_SNOW_SHOWERS(86),
    THUNDERSTORM(95),
    THUNDERSTORM_WITH_SLIGHT_HAIL(96),
    HEAVY_THUNDERSTORM(97),
    THUNDERSTORM_WITH_HEAVY_HAIL(99);

    companion object {
        fun fromCode(code: Int?): WeatherCondition =
            entries.firstOrNull { it.wmoCode == code } ?: UNAVAILABLE
    }
}

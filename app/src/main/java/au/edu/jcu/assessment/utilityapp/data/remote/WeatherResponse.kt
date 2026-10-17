package au.edu.jcu.assessment.utilityapp.data.remote

import com.google.gson.annotations.SerializedName

/** Nullable wire fields keep missing JSON distinct from real zero values. */
data class WeatherResponse(
    val timezone: String? = null,
    @SerializedName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val current: CurrentWeatherResponse? = null,
    val hourly: HourlyWeatherResponse? = null,
    val daily: DailyWeatherResponse? = null
)

data class CurrentWeatherResponse(
    val time: Long? = null,
    @SerializedName("temperature_2m") val temperatureCelsius: Double? = null,
    @SerializedName("apparent_temperature") val apparentTemperatureCelsius: Double? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
    @SerializedName("is_day") val isDay: Int? = null
)

data class HourlyWeatherResponse(
    val time: List<Long?>? = null,
    @SerializedName("precipitation_probability")
    val precipitationProbability: List<Double?>? = null,
    @SerializedName("temperature_2m") val temperatureCelsius: List<Double?>? = null,
    @SerializedName("weather_code") val weatherCode: List<Int?>? = null,
    @SerializedName("is_day") val isDay: List<Int?>? = null
)

data class DailyWeatherResponse(
    val time: List<Long?>? = null,
    @SerializedName("temperature_2m_min") val temperatureMinCelsius: List<Double?>? = null,
    @SerializedName("temperature_2m_max") val temperatureMaxCelsius: List<Double?>? = null,
    @SerializedName("weather_code") val weatherCode: List<Int?>? = null,
    @SerializedName("precipitation_probability_max")
    val maxPrecipitationProbability: List<Double?>? = null
)

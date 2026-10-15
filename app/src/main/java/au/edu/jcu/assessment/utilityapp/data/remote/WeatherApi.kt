package au.edu.jcu.assessment.utilityapp.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/** Public, non-commercial Open-Meteo forecast endpoint; no API key is required. */
interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "temperature_2m,apparent_temperature,weather_code",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,is_day,precipitation_probability",
        @Query("daily") daily: String =
            "weather_code,temperature_2m_min,temperature_2m_max,precipitation_probability_max",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("timeformat") timeFormat: String = "unixtime",
        @Query("timezone") timezone: String = "auto",
        @Query("temperature_unit") temperatureUnit: String = "celsius"
    ): WeatherResponse
}

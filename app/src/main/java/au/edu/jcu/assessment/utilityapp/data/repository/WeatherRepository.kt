package au.edu.jcu.assessment.utilityapp.data.repository

import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.data.remote.WeatherApi
import com.google.gson.JsonParseException
import java.time.Clock
import java.time.Instant

interface WeatherRepository {
    suspend fun fetchWeather(city: City): WeatherSnapshot
}

class DefaultWeatherRepository(
    private val api: WeatherApi,
    private val clock: Clock = Clock.systemUTC()
) : WeatherRepository {
    override suspend fun fetchWeather(city: City): WeatherSnapshot {
        val response = try {
            api.getForecast(city.latitude, city.longitude)
        } catch (failure: JsonParseException) {
            throw InvalidWeatherResponseException("The weather service returned unreadable JSON.", failure)
        }
        return mapWeatherResponse(response, city, Instant.now(clock))
    }
}

class EmptyWeatherException : Exception("The weather service returned no usable weather data.")

class InvalidWeatherResponseException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

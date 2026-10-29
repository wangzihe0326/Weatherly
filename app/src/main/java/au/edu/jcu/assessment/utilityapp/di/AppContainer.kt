package au.edu.jcu.assessment.utilityapp.di

import au.edu.jcu.assessment.utilityapp.data.remote.WeatherApi
import au.edu.jcu.assessment.utilityapp.data.remote.WeatherResponseConverterFactory
import au.edu.jcu.assessment.utilityapp.data.repository.DefaultWeatherRepository
import au.edu.jcu.assessment.utilityapp.data.repository.WeatherRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/** Application-scoped dependencies, passed explicitly to the ViewModel factory. */
class AppContainer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(WeatherResponseConverterFactory())
        .build()
        .create(WeatherApi::class.java)

    val weatherRepository: WeatherRepository = DefaultWeatherRepository(api)
}

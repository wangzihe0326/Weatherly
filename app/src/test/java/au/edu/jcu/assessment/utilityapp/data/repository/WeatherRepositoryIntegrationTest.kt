package au.edu.jcu.assessment.utilityapp.data.repository

import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.remote.WeatherApi
import java.net.InetAddress
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Uses synthetic JSON fixtures served only on loopback; never calls a public API. */
class WeatherRepositoryIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: WeatherRepository
    private val fetchedAt = Instant.parse("2025-01-01T00:15:00Z")

    @Before
    fun setUp() {
        server = MockWebServer().apply {
            start(InetAddress.getByName("127.0.0.1"), 0)
        }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
        repository = DefaultWeatherRepository(api, Clock.fixed(fetchedAt, ZoneOffset.UTC))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun realRetrofitAndGsonRequestTheSelectedCityAndPreserveUtcIntervals() = runBlocking {
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(FORECAST_FIXTURE))

        val snapshot = repository.fetchWeather(City.TOWNSVILLE)
        val request = requireNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        val url = requireNotNull(request.requestUrl)

        assertEquals("GET", request.method)
        assertEquals("/v1/forecast", url.encodedPath)
        assertEquals(-19.26639, requireNotNull(url.queryParameter("latitude")).toDouble(), 0.000001)
        assertEquals(146.80570, requireNotNull(url.queryParameter("longitude")).toDouble(), 0.000001)
        assertEquals("temperature_2m,apparent_temperature,weather_code", url.queryParameter("current"))
        assertEquals("precipitation_probability", url.queryParameter("hourly"))
        assertEquals("8", url.queryParameter("forecast_hours"))
        assertEquals("unixtime", url.queryParameter("timeformat"))
        assertEquals("auto", url.queryParameter("timezone"))
        assertEquals("celsius", url.queryParameter("temperature_unit"))
        assertNull(url.queryParameter("apikey"))

        assertEquals(City.TOWNSVILLE, snapshot.city)
        assertEquals(28.4, snapshot.temperatureCelsius!!, 0.0)
        assertEquals(33.7, snapshot.apparentTemperatureCelsius!!, 0.0)
        assertEquals("Slight rain showers", snapshot.weatherDescription)
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), snapshot.observationTime)
        assertEquals(fetchedAt, snapshot.fetchedAt)
        assertEquals("Australia/Brisbane", snapshot.timezoneId)
        assertEquals(8, snapshot.forecastPeriods.size)
        assertEquals(Instant.parse("2024-12-31T23:00:00Z"), snapshot.forecastPeriods.first().start)
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), snapshot.forecastPeriods.first().end)
        assertEquals(15.0, snapshot.forecastPeriods.first().probabilityPercent!!, 0.0)
        assertNull(snapshot.forecastPeriods[1].probabilityPercent)
        assertEquals(45.0, snapshot.forecastPeriods[2].probabilityPercent!!, 0.0)
    }

    @Test
    fun httpFailureRetainsStatusAndErrorBodyForTheViewModel() = runBlocking {
        val errorFixture = """{"error":true,"reason":"Synthetic test fixture: unavailable"}"""
        server.enqueue(MockResponse()
            .setResponseCode(503)
            .setHeader("Content-Type", "application/json")
            .setBody(errorFixture))

        val error = try {
            repository.fetchWeather(City.CAIRNS)
            throw AssertionError("HTTP 503 must reach the caller instead of producing weather data.")
        } catch (exception: HttpException) {
            exception
        }

        assertEquals(503, error.code())
        assertEquals(errorFixture, error.response()?.errorBody()?.string())
        val request = requireNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        val url = requireNotNull(request.requestUrl)
        assertEquals(-16.92366, requireNotNull(url.queryParameter("latitude")).toDouble(), 0.000001)
        assertEquals(145.76613, requireNotNull(url.queryParameter("longitude")).toDouble(), 0.000001)
        assertEquals(1, server.requestCount)
    }

    private companion object {
        // Fixed synthetic test data, not an observed forecast or an application fallback.
        val FORECAST_FIXTURE = """
            {
              "latitude": -19.25,
              "longitude": 146.875,
              "generationtime_ms": 0.25,
              "utc_offset_seconds": 36000,
              "timezone": "Australia/Brisbane",
              "timezone_abbreviation": "GMT+10",
              "current_units": {
                "time": "unixtime",
                "temperature_2m": "°C",
                "apparent_temperature": "°C",
                "weather_code": "wmo code"
              },
              "current": {
                "time": 1735689600,
                "interval": 900,
                "temperature_2m": 28.4,
                "apparent_temperature": 33.7,
                "weather_code": 80
              },
              "hourly_units": {"time":"unixtime","precipitation_probability":"%"},
              "hourly": {
                "time": [1735689600,1735693200,1735696800,1735700400,
                         1735704000,1735707600,1735711200,1735714800],
                "precipitation_probability": [15,null,45,55,30,20,10,5]
              }
            }
        """.trimIndent()
    }
}

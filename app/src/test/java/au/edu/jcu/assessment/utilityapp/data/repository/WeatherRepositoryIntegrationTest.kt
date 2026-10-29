package au.edu.jcu.assessment.utilityapp.data.repository

import androidx.lifecycle.ViewModelStore
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.data.model.WeatherCondition
import au.edu.jcu.assessment.utilityapp.data.remote.WeatherApi
import au.edu.jcu.assessment.utilityapp.data.remote.WeatherResponseConverterFactory
import au.edu.jcu.assessment.utilityapp.ui.UtilityViewModel
import au.edu.jcu.assessment.utilityapp.ui.WeatherLoadError
import java.io.EOFException
import java.io.IOException
import java.net.InetAddress
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit

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
            .addConverterFactory(WeatherResponseConverterFactory())
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
        assertEquals("temperature_2m,apparent_temperature,weather_code,is_day", url.queryParameter("current"))
        assertEquals("temperature_2m,weather_code,is_day,precipitation_probability", url.queryParameter("hourly"))
        assertEquals("weather_code,temperature_2m_min,temperature_2m_max,precipitation_probability_max", url.queryParameter("daily"))
        assertEquals("7", url.queryParameter("forecast_days"))
        assertNull(url.queryParameter("forecast_hours"))
        assertEquals("unixtime", url.queryParameter("timeformat"))
        assertEquals("auto", url.queryParameter("timezone"))
        assertEquals("celsius", url.queryParameter("temperature_unit"))
        assertNull(url.queryParameter("apikey"))

        assertEquals(City.TOWNSVILLE, snapshot.city)
        assertEquals(28.4, snapshot.temperatureCelsius!!, 0.0)
        assertEquals(33.7, snapshot.apparentTemperatureCelsius!!, 0.0)
        assertEquals(WeatherCondition.SLIGHT_RAIN_SHOWERS, snapshot.condition)
        assertEquals(80, snapshot.weatherCode)
        assertEquals(true, snapshot.isDay)
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

    @Test
    fun malformedJsonIsReportedAsInvalidWeatherData() = runBlocking {
        assertInvalidResponse("""{"current":@}""")
    }

    @Test
    fun wrongFieldTypeIsReportedAsInvalidWeatherData() = runBlocking {
        assertInvalidResponse("""{"current":{"temperature_2m":[]}}""")
    }

    @Test
    fun invalidNumericTextIsReportedAsInvalidWeatherData() = runBlocking {
        assertInvalidResponse("""{"current":{"temperature_2m":"warm"}}""")
    }

    @Test
    fun fullyDeliveredButUnfinishedJsonIsReportedAsInvalidWeatherData() = runBlocking {
        assertInvalidResponse("""{"current":""")
    }

    @Test
    fun interruptedHttpBodiesRemainNetworkFailures() = runBlocking {
        val body = """{"current":{"temperature_2m":25}}""".padEnd(128)
        for (chunked in listOf(false, true)) {
            val response = MockResponse().setHeader("Content-Type", "application/json")
            if (chunked) response.setChunkedBody(body, 64) else response.setBody(body)
            server.enqueue(response.setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))

            val error = try {
                repository.fetchWeather(City.SINGAPORE)
                throw AssertionError("An interrupted HTTP body must remain a network failure.")
            } catch (exception: IOException) {
                exception
            }
            // The second chunk's header is cut short: this is transport EOF, not JSON EOF.
            if (chunked) assertTrue(error is EOFException)
        }
        assertEquals(2, server.requestCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun invalidRefreshKeepsWeatherAndRetryRecoversThroughTheRealParser() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val store = ViewModelStore()
        try {
            server.enqueue(MockResponse().setBody(FORECAST_FIXTURE))
            val vm = UtilityViewModel(repository, Clock.fixed(fetchedAt, ZoneOffset.UTC))
            store.put("weather", vm)
            val original = withTimeout(5_000) { vm.uiState.first { !it.isLoading } }
            assertNotNull(original.weather)
            assertNull(original.error)

            server.enqueue(MockResponse().setBody("""{"current":"""))
            vm.refresh()
            val failed = withTimeout(5_000) { vm.uiState.first { !it.isLoading } }
            assertEquals(WeatherLoadError.INVALID_RESPONSE, failed.error)
            assertSame(original.weather, failed.weather)
            assertEquals(fetchedAt.plusSeconds(60), failed.nextRetryAt)

            server.enqueue(MockResponse().setBody(FORECAST_FIXTURE))
            vm.refresh()
            val recovered = withTimeout(5_000) { vm.uiState.first { !it.isLoading } }
            assertNull(recovered.error)
            assertNull(recovered.nextRetryAt)
            assertNotNull(recovered.weather)
            assertEquals(3, server.requestCount)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private suspend fun assertInvalidResponse(body: String) {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body))
        val error = try {
            repository.fetchWeather(City.SINGAPORE)
            throw AssertionError("Unreadable JSON must be classified as invalid weather data.")
        } catch (exception: InvalidWeatherResponseException) {
            exception
        }
        assertNotNull(error.cause)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun extendedJsonProvidesHourlyConditionsAndSevenLocalCalendarDays() = runBlocking {
        // Synthetic extended fixture: hourly conditions and end-of-hour rain use different timestamps.
        val hourlyStart = Instant.parse("2025-01-01T00:00:00Z")
        val localMidnight = Instant.parse("2024-12-31T14:00:00Z")
        val fixture = """
            {
              "timezone":"Australia/Brisbane",
              "utc_offset_seconds":36000,
              "hourly": {
                "time":[${(0L..25L).joinToString { hourlyStart.plusSeconds(it * 3600).epochSecond.toString() }}],
                "temperature_2m":[${(0..25).joinToString { (20.0 + it * 0.5).toString() }}],
                "weather_code":[${(0..25).joinToString { if (it % 2 == 0) "0" else "61" }}],
                "is_day":[${(0..25).joinToString { if (it in 6..17) "1" else "0" }}],
                "precipitation_probability":[${(0..25).joinToString { (it * 3).toString() }}]
              },
              "daily": {
                "time":[${(0L..6L).joinToString { localMidnight.plusSeconds(it * 86400).epochSecond.toString() }}],
                "temperature_2m_min":[20,21,22,23,24,25,26],
                "temperature_2m_max":[30,31,32,33,34,35,36],
                "weather_code":[0,2,3,61,80,95,45],
                "precipitation_probability_max":[10,20,30,40,50,60,70]
              }
            }
        """.trimIndent()
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(fixture))

        val snapshot = repository.fetchWeather(City.CAIRNS)
        val firstHour = snapshot.forecastPeriods.single { it.start == hourlyStart }
        val lastHour = snapshot.forecastPeriods.single { it.start == hourlyStart.plusSeconds(25L * 3600) }

        assertEquals(City.CAIRNS, snapshot.city)
        assertNull(snapshot.temperatureCelsius)
        assertNull(snapshot.weatherCode)
        assertNull(snapshot.isDay)
        assertEquals(20.0, firstHour.temperatureCelsius!!, 0.0)
        assertEquals(3.0, firstHour.probabilityPercent!!, 0.0)
        assertEquals(0, firstHour.weatherCode)
        assertEquals(false, firstHour.isDay)
        assertEquals(32.5, lastHour.temperatureCelsius!!, 0.0)
        assertNull(lastHour.probabilityPercent)
        assertEquals(7, snapshot.dailyForecast.size)
        assertEquals(LocalDate.parse("2025-01-01"), snapshot.dailyForecast.first().date)
        assertEquals(LocalDate.parse("2025-01-07"), snapshot.dailyForecast.last().date)
        assertEquals(20.0, snapshot.dailyForecast.first().temperatureMinCelsius!!, 0.0)
        assertEquals(36.0, snapshot.dailyForecast.last().temperatureMaxCelsius!!, 0.0)
        assertEquals(70.0, snapshot.dailyForecast.last().maxPrecipitationProbability!!, 0.0)
        assertEquals(WeatherCondition.FOG, snapshot.dailyForecast.last().condition)
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
                "weather_code": "wmo code",
                "is_day": ""
              },
              "current": {
                "time": 1735689600,
                "interval": 900,
                "temperature_2m": 28.4,
                "apparent_temperature": 33.7,
                "weather_code": 80,
                "is_day": 1
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

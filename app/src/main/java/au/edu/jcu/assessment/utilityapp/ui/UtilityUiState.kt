package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.UmbrellaAdvice
import au.edu.jcu.assessment.utilityapp.data.calculateUmbrellaAdvice
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.DailyForecast
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

data class UtilityUiState(
    val settings: AppSettings = AppSettings(),
    val weather: WeatherSnapshot? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val now: Instant = Instant.now()
) {
    val isStale: Boolean
        get() = weather?.let {
            Duration.between(it.fetchedAt, now) >= Duration.ofMinutes(30) ||
                it.fetchedAt.isAfter(now.plusSeconds(60))
        } ?: false

    val hourlyPeriods: List<RainPeriod>
        get() = weather?.forecastPeriods.orEmpty().filter { it.end > now && it.start < now.plusSeconds(86400) }
            .sortedBy { it.start }.distinctBy { it.start }.take(24)

    /** The near-term reminder remains independent of the longer browsing forecast. */
    val displayedPeriods: List<RainPeriod>
        get() = hourlyPeriods.take(3)

    val dailyForecast: List<DailyForecast>
        get() = weather?.let { snapshot ->
            val today = now.atZone(ZoneId.of(snapshot.timezoneId)).toLocalDate()
            snapshot.dailyForecast.filter { it.date >= today && it.date < today.plusDays(7) }
                .sortedBy { it.date }.distinctBy { it.date }.take(7)
        }.orEmpty()

    val advice: UmbrellaAdvice
        get() {
            val periods = displayedPeriods
            val coversCurrentHour = periods.firstOrNull()?.start?.let { it <= now } == true
            val hasNoGaps = periods.zipWithNext().all { (first, next) -> first.end == next.start }
            return calculateUmbrellaAdvice(
                periods = periods,
                thresholdPercent = settings.rainThresholdPercent,
                isStale = isStale || !coversCurrentHour || !hasNoGaps
            )
        }
}

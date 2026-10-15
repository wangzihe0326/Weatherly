package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.UmbrellaAdvice
import au.edu.jcu.assessment.utilityapp.data.calculateUmbrellaAdvice
import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import au.edu.jcu.assessment.utilityapp.data.model.WeatherSnapshot
import java.time.Duration
import java.time.Instant

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

    val displayedPeriods: List<RainPeriod>
        get() = weather?.forecastPeriods.orEmpty().filter { it.end > now }.take(3)

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

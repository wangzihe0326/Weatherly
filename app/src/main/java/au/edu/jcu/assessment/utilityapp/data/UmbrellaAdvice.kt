package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod

enum class AdviceKind {
    BRING_UMBRELLA,
    BELOW_THRESHOLD,
    UNAVAILABLE
}

data class UmbrellaAdvice(
    val kind: AdviceKind,
    val peakProbability: Double? = null,
    val thresholdPercent: Int
)

/**
 * The caller supplies the upcoming three hourly periods. A missing forecast is not 0%.
 * This compares hourly probabilities; it does not estimate the chance of rain at any
 * point across the whole period, which would need additional model information.
 */
fun calculateUmbrellaAdvice(
    periods: List<RainPeriod>,
    thresholdPercent: Int,
    isStale: Boolean
): UmbrellaAdvice {
    require(thresholdPercent in 0..100) { "The rain threshold must be between 0 and 100." }

    val probabilities = periods.map { it.probabilityPercent }
    if (isStale || periods.size < 3 || probabilities.any {
            it == null || !it.isFinite() || it !in 0.0..100.0
        }
    ) {
        return UmbrellaAdvice(AdviceKind.UNAVAILABLE, thresholdPercent = thresholdPercent)
    }

    val peak = probabilities.filterNotNull().maxOrNull()
        ?: return UmbrellaAdvice(AdviceKind.UNAVAILABLE, thresholdPercent = thresholdPercent)
    return UmbrellaAdvice(
        kind = if (peak >= thresholdPercent) AdviceKind.BRING_UMBRELLA else AdviceKind.BELOW_THRESHOLD,
        peakProbability = peak,
        thresholdPercent = thresholdPercent
    )
}

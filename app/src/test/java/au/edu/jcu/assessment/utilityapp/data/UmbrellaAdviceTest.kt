package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.data.model.RainPeriod
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UmbrellaAdviceTest {
    @Test
    fun peakEqualToThresholdSuggestsBringingAnUmbrella() {
        val advice = calculateUmbrellaAdvice(periods(12.0, 50.0, 20.0), 50, false)

        assertEquals(AdviceKind.BRING_UMBRELLA, advice.kind)
        assertEquals(50.0, advice.peakProbability!!, 0.0)
        assertEquals(50, advice.thresholdPercent)
    }

    @Test
    fun allThreeBelowThresholdProducesBelowThresholdAdvice() {
        val advice = calculateUmbrellaAdvice(periods(49.0, 12.0, 30.0), 50, false)

        assertEquals(AdviceKind.BELOW_THRESHOLD, advice.kind)
        assertEquals(49.0, advice.peakProbability!!, 0.0)
    }

    @Test
    fun changingThresholdChangesAdviceWithoutFetchingAgain() {
        val forecast = periods(30.0, 60.0, 40.0)

        assertEquals(AdviceKind.BRING_UMBRELLA, calculateUmbrellaAdvice(forecast, 60, false).kind)
        assertEquals(AdviceKind.BELOW_THRESHOLD, calculateUmbrellaAdvice(forecast, 70, false).kind)
    }

    @Test
    fun aSingleMissingProbabilityMakesAdviceUnavailableEvenIfAnotherIsHigh() {
        val advice = calculateUmbrellaAdvice(periods(90.0, null, 20.0), 50, false)

        assertEquals(AdviceKind.UNAVAILABLE, advice.kind)
        assertNull(advice.peakProbability)
    }

    @Test
    fun fewerThanThreePeriodsCannotBePresentedAsACompleteForecast() {
        val advice = calculateUmbrellaAdvice(periods(20.0, 30.0), 50, false)

        assertEquals(AdviceKind.UNAVAILABLE, advice.kind)
        assertNull(advice.peakProbability)
    }

    @Test
    fun staleForecastNeverProducesCurrentAdvice() {
        val advice = calculateUmbrellaAdvice(periods(10.0, 20.0, 30.0), 50, true)

        assertEquals(AdviceKind.UNAVAILABLE, advice.kind)
        assertNull(advice.peakProbability)
    }

    @Test
    fun invalidNumericProbabilityCannotGiveFalseReassurance() {
        for (invalid in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 101.0)) {
            assertEquals(AdviceKind.UNAVAILABLE,
                calculateUmbrellaAdvice(periods(10.0, invalid, 30.0), 50, false).kind)
        }
    }

    @Test
    fun zeroPercentIsRealDataAndThresholdEndpointsAreInclusive() {
        assertEquals(AdviceKind.BELOW_THRESHOLD,
            calculateUmbrellaAdvice(periods(0.0, 0.0, 0.0), 50, false).kind)
        assertEquals(AdviceKind.BRING_UMBRELLA,
            calculateUmbrellaAdvice(periods(0.0, 0.0, 0.0), 0, false).kind)
        assertEquals(AdviceKind.BRING_UMBRELLA,
            calculateUmbrellaAdvice(periods(0.0, 100.0, 0.0), 100, false).kind)
    }

    @Test(expected = IllegalArgumentException::class)
    fun thresholdOutsideProbabilityRangeIsRejected() {
        calculateUmbrellaAdvice(periods(10.0, 20.0, 30.0), 101, false)
    }

    private fun periods(vararg probabilities: Double?): List<RainPeriod> {
        val start = Instant.parse("2026-10-15T02:00:00Z")
        return probabilities.mapIndexed { index, probability ->
            RainPeriod(
                start = start.plusSeconds(index * 3600L),
                end = start.plusSeconds((index + 1) * 3600L),
                probabilityPercent = probability
            )
        }
    }
}

package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.model.City

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F");

    fun fromCelsius(value: Double): Double =
        if (this == CELSIUS) value else value * 9.0 / 5.0 + 32.0
}

/** Session preferences: the assignment does not require storage across app restarts. */
data class AppSettings(
    val city: City = City.SINGAPORE,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val umbrellaEnabled: Boolean = true,
    val rainThresholdPercent: Int = 60
)

package au.edu.jcu.assessment.utilityapp.ui

/** Recoverable loading failures; the UI owns their localized presentation. */
enum class WeatherLoadError {
    NETWORK,
    SERVICE_BUSY,
    SERVICE_UNAVAILABLE,
    EMPTY_RESPONSE,
    INVALID_RESPONSE,
    UNKNOWN
}

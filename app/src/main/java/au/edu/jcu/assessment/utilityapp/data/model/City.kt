package au.edu.jcu.assessment.utilityapp.data.model

/** Preset city centres verified with the Open-Meteo Geocoding API. */
enum class City(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
) {
    SINGAPORE("Singapore", 1.28967, 103.85007),
    TOWNSVILLE("Townsville", -19.26639, 146.80570),
    CAIRNS("Cairns", -16.92366, 145.76613)
}

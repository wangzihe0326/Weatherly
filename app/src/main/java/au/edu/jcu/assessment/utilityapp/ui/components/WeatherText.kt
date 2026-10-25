package au.edu.jcu.assessment.utilityapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.WeatherCondition
import au.edu.jcu.assessment.utilityapp.ui.WeatherLoadError

/** Visible conditions and spoken summaries share the same resource-backed description. */
@Composable
fun weatherDescription(condition: WeatherCondition): String = stringResource(
    when (condition) {
        WeatherCondition.UNAVAILABLE -> R.string.weather_condition_unavailable
        WeatherCondition.CLEAR_SKY -> R.string.weather_condition_clear_sky
        WeatherCondition.MAINLY_CLEAR -> R.string.weather_condition_mainly_clear
        WeatherCondition.PARTLY_CLOUDY -> R.string.weather_condition_partly_cloudy
        WeatherCondition.OVERCAST -> R.string.weather_condition_overcast
        WeatherCondition.FOG -> R.string.weather_condition_fog
        WeatherCondition.DEPOSITING_RIME_FOG -> R.string.weather_condition_depositing_rime_fog
        WeatherCondition.LIGHT_DRIZZLE -> R.string.weather_condition_light_drizzle
        WeatherCondition.MODERATE_DRIZZLE -> R.string.weather_condition_moderate_drizzle
        WeatherCondition.DENSE_DRIZZLE -> R.string.weather_condition_dense_drizzle
        WeatherCondition.LIGHT_FREEZING_DRIZZLE -> R.string.weather_condition_light_freezing_drizzle
        WeatherCondition.DENSE_FREEZING_DRIZZLE -> R.string.weather_condition_dense_freezing_drizzle
        WeatherCondition.SLIGHT_RAIN -> R.string.weather_condition_slight_rain
        WeatherCondition.MODERATE_RAIN -> R.string.weather_condition_moderate_rain
        WeatherCondition.HEAVY_RAIN -> R.string.weather_condition_heavy_rain
        WeatherCondition.LIGHT_FREEZING_RAIN -> R.string.weather_condition_light_freezing_rain
        WeatherCondition.HEAVY_FREEZING_RAIN -> R.string.weather_condition_heavy_freezing_rain
        WeatherCondition.SLIGHT_SNOWFALL -> R.string.weather_condition_slight_snowfall
        WeatherCondition.MODERATE_SNOWFALL -> R.string.weather_condition_moderate_snowfall
        WeatherCondition.HEAVY_SNOWFALL -> R.string.weather_condition_heavy_snowfall
        WeatherCondition.SNOW_GRAINS -> R.string.weather_condition_snow_grains
        WeatherCondition.SLIGHT_RAIN_SHOWERS -> R.string.weather_condition_slight_rain_showers
        WeatherCondition.MODERATE_RAIN_SHOWERS -> R.string.weather_condition_moderate_rain_showers
        WeatherCondition.VIOLENT_RAIN_SHOWERS -> R.string.weather_condition_violent_rain_showers
        WeatherCondition.SLIGHT_SNOW_SHOWERS -> R.string.weather_condition_slight_snow_showers
        WeatherCondition.HEAVY_SNOW_SHOWERS -> R.string.weather_condition_heavy_snow_showers
        WeatherCondition.THUNDERSTORM -> R.string.weather_condition_thunderstorm
        WeatherCondition.THUNDERSTORM_WITH_SLIGHT_HAIL -> R.string.weather_condition_thunderstorm_with_slight_hail
        WeatherCondition.HEAVY_THUNDERSTORM -> R.string.weather_condition_heavy_thunderstorm
        WeatherCondition.THUNDERSTORM_WITH_HEAVY_HAIL -> R.string.weather_condition_thunderstorm_with_heavy_hail
    }
)

@Composable
fun weatherErrorMessage(error: WeatherLoadError): String = stringResource(
    when (error) {
        WeatherLoadError.NETWORK -> R.string.weather_error_network
        WeatherLoadError.SERVICE_BUSY -> R.string.weather_error_service_busy
        WeatherLoadError.SERVICE_UNAVAILABLE -> R.string.weather_error_service_unavailable
        WeatherLoadError.EMPTY_RESPONSE -> R.string.weather_error_empty_response
        WeatherLoadError.INVALID_RESPONSE -> R.string.weather_error_invalid_response
        WeatherLoadError.UNKNOWN -> R.string.weather_error_unknown
    }
)

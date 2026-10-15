# Before You Go

A small Android utility for checking the weather before heading out. The Weather screen combines current conditions, 24 hourly forecast periods and a seven-day overview. Its umbrella suggestion stays focused on the current hour and the next two. Settings lets you choose a city, temperature units and a personal reminder threshold.

Built from the CP3406 / CP5307 Assessment 1 starter using Kotlin, Jetpack Compose and Material 3.


## Features

- Live Open-Meteo weather for Singapore, Townsville and Cairns.
- Current temperature, feels-like temperature, weather description and model timestamp.
- A continuous, horizontally scrollable strip of up to 24 hourly cards with temperatures, weather icons and rain probabilities. Swipe, use Earlier/Later to move one hour at a time, or tap a card to view its explicit interval and details. An "Hour X of 24" indicator follows the selected hour; its total reflects the actual number of available periods.
- A seven-day overview with daily weather, low/high temperatures, proportional temperature ranges and Peak rain. Peak rain is the day's highest hourly precipitation probability.
- Umbrella advice when the highest probability in the current hour and next two meets the selected 30%, 60% or 80% threshold. Browsing later hours or days does not change this near-term rule.
- Celsius/Fahrenheit conversion for current, hourly and daily temperatures, plus an advice visibility switch, without extra network requests.
- Loading, refresh, retry, missing-data and stale-data states. Failed refreshes retain clearly labelled data for the same city; changing city clears the previous city's data.
- Shared ViewModel state across the two tabs and configuration changes, plus independently defined light/dark themes.
- An illustrated weather hero, condition-aware hourly/daily icons, compact selectable settings and separate light/dark palettes. Hourly cards grow with system text size; daily rows and settings reflow at larger font scales or narrower widths. Daily rows are informational, with a subtle highlight for today.

Settings are session-only, as permitted by the assignment. They may reset when the process is restarted. The app uses predefined cities; it does not request location permission.

## Run in Android Studio

1. Open this directory, containing `settings.gradle.kts`, in Android Studio.
2. Select the Android Studio bundled JetBrains JDK 21 as the Gradle JDK.
3. Install Android SDK Platform 36 and Build Tools 35.0.0 if requested, and let Gradle sync.
4. Run the `app` configuration on an emulator or device running API 26 or later.
5. Allow an internet connection to `api.open-meteo.com`. No API key is needed for this non-commercial educational use.

The template's AGP 8.13.2, Gradle 8.13 and Kotlin 2.0.21 are retained. Retrofit 2.11.0, Gson converter 2.11.0 and OkHttp 4.12.0 are used with Lifecycle 2.9.2. The local SDK path belongs in the ignored `local.properties`, not version control.

On Windows, when Java is not on PATH, set `JAVA_HOME` to your Android Studio `jbr` directory for the current terminal, then run:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

The second command needs a running emulator or attached device. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

```text
MainActivity / UtilityApp
          |
  UtilityViewModel (StateFlow)
          |
   WeatherRepository
          |
 Retrofit WeatherApi -> Open-Meteo
```

`UtilityApplication` owns an `AppContainer`, which constructs the HTTP client and repository once. `UtilityViewModelFactory` passes the repository into an Activity-scoped ViewModel. The two screen composables receive immutable state and callbacks, and the Activity collects the flow with `collectAsStateWithLifecycle()`.

The ViewModel cancels obsolete requests and also checks a request version so late results cannot overwrite another city's weather. A foreground lifecycle callback refreshes stale data, while a foreground-only clock update keeps displayed periods and local dates current. Pure mapping and advice functions are separate from Compose. The hourly browser uses a `LazyRow` and saveable selection by interval start time; Earlier/Later updates that selection and scrolls it into view. Changing city resets the browser to that city's forecast.

### Forecast semantics

The HTTPS forecast request uses:

- `current=temperature_2m,apparent_temperature,weather_code`
- `hourly=temperature_2m,weather_code,is_day,precipitation_probability`
- `daily=weather_code,temperature_2m_min,temperature_2m_max,precipitation_probability_max`
- `forecast_days=7`, `timeformat=unixtime`, `timezone=auto` and `temperature_unit=celsius`

The response supplies seven days of forecast data. The hourly browser selects up to 24 periods beginning with the ongoing hour; that first interval may already have started. Daily rows cover today and the following six calendar days in the selected city's timezone. Missing periods or days are reported rather than filled with sample data.

A probability timestamped at 15:00 covers 14:00-15:00, while temperature, condition code and `is_day` timestamped at 14:00 describe the start of that interval. The mapper joins these values by timestamp, retaining their original array indices when fields are missing. Hourly icons use the daylight flag when available; daily icons represent the day's weather category.

Umbrella advice uses only the ongoing hour and the following two consecutive periods, independently of the selected card. It compares their largest hourly probability against the user's threshold. This is **not** the combined probability of rain over all three periods. Similarly, daily Peak rain is the maximum hourly probability for that day, not a combined whole-day probability.

Missing probabilities never become zero. Advice requires three valid, consecutive intervals covering the current time. It is unavailable when data is incomplete or when the last successful fetch is at least 30 minutes old. A below-threshold result does not promise dry weather. Current conditions are weather-model data, not a local sensor measurement.

Unix timestamps remain UTC instants internally and are formatted in the timezone returned by the API. Daily dates are derived in that same city timezone. Temperatures remain Celsius internally and are converted only for display. Missing or invalid daily temperature ranges are not drawn, and missing probabilities never appear as zero.

## Data and references

Weather data by [Open-Meteo](https://open-meteo.com/), licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). The app's umbrella suggestion is a derived rule, not a provider-issued recommendation.

- [Open-Meteo Forecast API](https://open-meteo.com/en/docs)
- [Open-Meteo geocoding](https://open-meteo.com/en/docs/geocoding-api)
- [Open-Meteo terms](https://open-meteo.com/en/terms) and [attribution requirements](https://open-meteo.com/en/licence)
- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Manual dependency injection](https://developer.android.com/training/dependency-injection/manual)
- [Compose state](https://developer.android.com/develop/ui/compose/state)
- [Retrofit](https://square.github.io/retrofit/)
- Original course starter: CP3406 / CP5307 Utility App template supplied with the assessment.

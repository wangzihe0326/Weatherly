# Before You Go

A two-screen Android weather utility built with Kotlin, Jetpack Compose and Material 3. Weather shows current conditions, up to 24 hourly forecast periods, a seven-day overview and near-term umbrella advice. Settings controls the city, temperature units and reminder threshold.

## Features

- Live Open-Meteo weather for Singapore, Townsville and Cairns, including current and feels-like temperatures, weather conditions and a model timestamp.
- Up to 24 hourly cards showing temperature, conditions and rain probability. Swipe horizontally, use Earlier/Later to move one hour, or select a card to see its complete interval and details.
- Seven-day weather, low/high temperatures, temperature-range bars and Peak rain: the day's highest hourly precipitation probability.
- Umbrella advice based on the current hour and next two, with selectable 30%, 60% or 80% thresholds. Browsing other hours or days does not change this rule.
- Celsius/Fahrenheit conversion across all forecasts and a reminder switch, without extra network requests.
- Loading, refresh, retry, missing-data and stale-data states. A failed refresh retains labelled data for the same city; changing city clears the previous city's data.
- Light/dark themes, scalable text and layouts that reflow on narrower screens or at larger font sizes.
- A compact overview places current conditions and near-term umbrella advice first. Larger text removes decorative artwork to leave more room for the forecast.
- Returning from Settings keeps the selected forecast hour and both vertical and horizontal browsing positions. Weather browsing is separate for each city; an expired selected hour falls back to the first available interval.

Settings are session-only and may reset when the process restarts. Cities are predefined; location permission is not required. Current conditions come from a weather model rather than a local sensor.


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

`UtilityApplication` owns an `AppContainer`, which constructs the HTTP client and repository once. `UtilityViewModelFactory` injects the repository into an Activity-scoped ViewModel. Weather data and settings are shared through `StateFlow`; screen composables receive state and callbacks, collected using `collectAsStateWithLifecycle()`.

The ViewModel cancels obsolete requests and rejects late results using a request version. A foreground lifecycle callback refreshes stale data; a foreground-only clock keeps displayed periods and city-local dates current. Response mapping and umbrella rules are separate from Compose. The hourly interface uses a `LazyRow` with selection keyed by interval start time.

`SaveableStateHolder` retains each city's weather browsing state and the Settings scroll position across tab changes and Activity recreation. A separate empty/loading page and keyed layout subtree prevent a pending city request from consuming a previously saved scroll position. Settings and fetched weather remain session-only ViewModel state.

### Forecast handling

- The API supplies current conditions and seven days of hourly and daily forecasts. The app selects up to 24 hourly periods starting with the ongoing hour, plus today and the next six city-local dates.
- Hourly conditions and temperature describe an interval's start. Rain probability describes the preceding hour: the probability at 15:00 belongs with conditions at 14:00 for the 14:00-15:00 card. Values are joined by timestamp while preserving their original array indices.
- Umbrella advice compares the highest hourly probability in three valid, consecutive intervals against the chosen threshold. This is not a combined three-hour probability. Incomplete data or a last successful fetch at least 30 minutes old makes advice unavailable; a below-threshold result does not guarantee dry weather.
- Missing values remain unavailable. Timestamps are stored as UTC instants and displayed in the API's city timezone. Temperatures remain Celsius internally and are converted for display. Invalid daily temperature ranges are not drawn.

## Run in Android Studio

1. Open this directory, containing `settings.gradle.kts`, in Android Studio.
2. Select JetBrains JDK 21 as the Gradle JDK, using Android Studio's bundled `jbr` when it is version 21.
3. Install Android SDK Platform 36 and any Build Tools requested by Gradle sync.
4. Run the `app` configuration on an emulator or device running Android 8.0 / API 26 or later.
5. Provide internet access to `api.open-meteo.com` for live forecasts. No API key is configured in the project.

The project uses AGP 8.13.2, Gradle 8.13, Kotlin 2.0.21, Lifecycle 2.9.2, Retrofit/Gson converter 2.11.0 and OkHttp 4.12.0. The local SDK path belongs in the ignored `local.properties`.

For command-line builds on Windows, make JDK 21 available through `JAVA_HOME` or `PATH`, then run from this directory:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

The first command builds the APK and runs local unit tests and lint. The second runs Compose instrumentation tests and requires a running emulator or attached device. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Data and references

Weather data by [Open-Meteo](https://open-meteo.com/), licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). The app's umbrella suggestion is a derived rule, not a provider-issued recommendation.

- [Open-Meteo Forecast API](https://open-meteo.com/en/docs)
- [Open-Meteo geocoding](https://open-meteo.com/en/docs/geocoding-api), used to obtain the preset city coordinates.
- [Open-Meteo terms](https://open-meteo.com/en/terms) and [attribution requirements](https://open-meteo.com/en/licence)
- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Manual dependency injection](https://developer.android.com/training/dependency-injection/manual)
- [Compose state](https://developer.android.com/develop/ui/compose/state)
- [Saving UI state in Compose](https://developer.android.com/develop/ui/compose/state-saving)
- [Retrofit](https://square.github.io/retrofit/)
- Original course starter: CP3406 / CP5307 Utility App template supplied with the assessment.

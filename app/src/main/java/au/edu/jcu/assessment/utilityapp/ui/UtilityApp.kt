package au.edu.jcu.assessment.utilityapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherGlyph
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherIcon
import au.edu.jcu.assessment.utilityapp.ui.screens.SettingsScreen
import au.edu.jcu.assessment.utilityapp.ui.screens.UtilityScreen

/** Stateless screen content; the selected tab alone belongs to this UI. */
@Composable
fun UtilityApp(
    state: UtilityUiState,
    onRefresh: () -> Unit,
    onCityChanged: (City) -> Unit,
    onUnitChanged: (TemperatureUnit) -> Unit,
    onUmbrellaEnabledChanged: (Boolean) -> Unit,
    onThresholdChanged: (Int) -> Unit
) {
    var settingsSelected by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = settingsSelected) { settingsSelected = false }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 4.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        val itemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        NavigationBarItem(
                            selected = !settingsSelected,
                            onClick = { settingsSelected = false },
                            icon = { WeatherIcon(WeatherGlyph.SUN) },
                            label = { Text(stringResource(R.string.weather_tab)) },
                            colors = itemColors,
                            modifier = Modifier.testTag("weather_tab")
                        )
                        NavigationBarItem(
                            selected = settingsSelected,
                            onClick = { settingsSelected = true },
                            icon = { WeatherIcon(WeatherGlyph.SETTINGS) },
                            label = { Text(stringResource(R.string.settings_tab)) },
                            colors = itemColors,
                            modifier = Modifier.testTag("settings_tab")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
            if (settingsSelected) {
                SettingsScreen(
                    settings = state.settings,
                    onCityChanged = onCityChanged,
                    onUnitChanged = onUnitChanged,
                    onUmbrellaEnabledChanged = onUmbrellaEnabledChanged,
                    onThresholdChanged = onThresholdChanged
                )
            } else {
                UtilityScreen(state = state, onRefresh = onRefresh)
            }
        }
    }
}

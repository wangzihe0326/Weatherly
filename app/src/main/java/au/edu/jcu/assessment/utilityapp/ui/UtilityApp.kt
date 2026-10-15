package au.edu.jcu.assessment.utilityapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.City
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
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = !settingsSelected,
                    onClick = { settingsSelected = false },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.weather_tab)) },
                    modifier = Modifier.testTag("weather_tab")
                )
                NavigationBarItem(
                    selected = settingsSelected,
                    onClick = { settingsSelected = true },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings_tab)) },
                    modifier = Modifier.testTag("settings_tab")
                )
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

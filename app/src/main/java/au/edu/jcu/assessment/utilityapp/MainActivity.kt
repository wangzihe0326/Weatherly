package au.edu.jcu.assessment.utilityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.edu.jcu.assessment.utilityapp.ui.UtilityApp
import au.edu.jcu.assessment.utilityapp.ui.UtilityViewModel
import au.edu.jcu.assessment.utilityapp.ui.UtilityViewModelFactory
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as UtilityApplication).container.weatherRepository
        val viewModel = ViewModelProvider(this, UtilityViewModelFactory(repository))[UtilityViewModel::class.java]

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onForeground()
                try {
                    while (isActive) {
                        delay(60_000)
                        viewModel.onForegroundTick()
                    }
                } finally {
                    viewModel.onBackground()
                }
            }
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MobileTech_UtilityAppTheme {
                UtilityApp(
                    state = state,
                    onRefresh = viewModel::refresh,
                    onCityChanged = viewModel::selectCity,
                    onUnitChanged = viewModel::setTemperatureUnit,
                    onUmbrellaEnabledChanged = viewModel::setUmbrellaEnabled,
                    onThresholdChanged = viewModel::setRainThreshold
                )
            }
        }
    }
}

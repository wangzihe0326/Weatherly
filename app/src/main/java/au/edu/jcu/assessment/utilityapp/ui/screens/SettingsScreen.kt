package au.edu.jcu.assessment.utilityapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.ui.AppSettings
import au.edu.jcu.assessment.utilityapp.ui.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.ui.components.ScreenContent
import au.edu.jcu.assessment.utilityapp.ui.components.SectionHeading
import au.edu.jcu.assessment.utilityapp.ui.components.cityLabel

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onCityChanged: (City) -> Unit,
    onUnitChanged: (TemperatureUnit) -> Unit,
    onUmbrellaEnabledChanged: (Boolean) -> Unit,
    onThresholdChanged: (Int) -> Unit
) {
    ScreenContent(tag = "settings_screen") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.settings_heading),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SettingCard(stringResource(R.string.city_heading), stringResource(R.string.city_helper)) {
            Column(Modifier.selectableGroup()) {
                City.entries.forEach { city ->
                    ChoiceRow(
                        label = cityLabel(city),
                        supportingText = stringResource(
                            when (city) {
                                City.SINGAPORE -> R.string.city_singapore_country
                                City.TOWNSVILLE -> R.string.city_townsville_country
                                City.CAIRNS -> R.string.city_cairns_country
                            }
                        ),
                        selected = settings.city == city,
                        tag = "city_${city.name}",
                        onClick = { onCityChanged(city) }
                    )
                }
            }
        }
        SettingCard(stringResource(R.string.units_heading), stringResource(R.string.units_helper)) {
            Column(Modifier.selectableGroup()) {
                TemperatureUnit.entries.forEach { unit ->
                    ChoiceRow(
                        label = stringResource(
                            if (unit == TemperatureUnit.CELSIUS) R.string.unit_celsius
                            else R.string.unit_fahrenheit
                        ),
                        selected = settings.temperatureUnit == unit,
                        tag = "unit_${unit.name}",
                        onClick = { onUnitChanged(unit) }
                    )
                }
            }
        }
        SettingCard(stringResource(R.string.reminder_heading)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .testTag("umbrella_toggle")
                    .toggleable(
                        value = settings.umbrellaEnabled,
                        role = Role.Switch,
                        onValueChange = onUmbrellaEnabledChanged
                    )
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.reminder_label), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.reminder_helper),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = settings.umbrellaEnabled, onCheckedChange = null)
            }
            Text(
                stringResource(R.string.threshold_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp).semantics { heading() }
            )
            Text(
                stringResource(
                    if (settings.umbrellaEnabled) R.string.threshold_helper else R.string.threshold_disabled
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(Modifier.selectableGroup()) {
                listOf(30, 60, 80).forEach { threshold ->
                    ChoiceRow(
                        label = stringResource(R.string.threshold_option, threshold),
                        selected = settings.rainThresholdPercent == threshold,
                        enabled = settings.umbrellaEnabled,
                        tag = "threshold_$threshold",
                        onClick = { onThresholdChanged(threshold) }
                    )
                }
            }
        }
        Text(
            stringResource(R.string.session_settings_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingCard(
    title: String,
    helper: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeading(title)
            if (helper != null) {
                Text(helper, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    tag: String,
    supportingText: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(tag)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            if (supportingText != null) {
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

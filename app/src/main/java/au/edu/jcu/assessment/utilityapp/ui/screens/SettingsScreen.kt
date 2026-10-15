package au.edu.jcu.assessment.utilityapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.City
import au.edu.jcu.assessment.utilityapp.ui.AppSettings
import au.edu.jcu.assessment.utilityapp.ui.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.ui.components.ScreenContent
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherGlyph
import au.edu.jcu.assessment.utilityapp.ui.components.WeatherIcon
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
                stringResource(R.string.settings_visual_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.settings_heading),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingCard(
            title = stringResource(R.string.city_heading),
            helper = stringResource(R.string.city_helper),
            glyph = WeatherGlyph.LOCATION
        ) {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                City.entries.forEach { city ->
                    CityChoice(
                        city = city,
                        selected = settings.city == city,
                        onClick = { onCityChanged(city) }
                    )
                }
            }
        }

        SettingCard(
            title = stringResource(R.string.units_heading),
            helper = stringResource(R.string.units_helper),
            glyph = WeatherGlyph.THERMOMETER
        ) {
            TemperatureChoices(settings.temperatureUnit, onUnitChanged)
        }

        SettingCard(
            title = stringResource(R.string.reminder_heading),
            glyph = WeatherGlyph.UMBRELLA,
            useWarmAccent = true
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("umbrella_toggle")
                    .toggleable(
                        value = settings.umbrellaEnabled,
                        role = Role.Switch,
                        onValueChange = onUmbrellaEnabledChanged
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.reminder_label),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(R.string.reminder_helper),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = settings.umbrellaEnabled, onCheckedChange = null)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.threshold_heading),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    stringResource(
                        if (settings.umbrellaEnabled) R.string.threshold_helper
                        else R.string.threshold_disabled
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ThresholdChoices(
                selectedThreshold = settings.rainThresholdPercent,
                enabled = settings.umbrellaEnabled,
                onThresholdChanged = onThresholdChanged
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            WeatherIcon(
                glyph = WeatherGlyph.CLOCK,
                modifier = Modifier.size(20.dp).clearAndSetSemantics {},
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.settings_visual_session_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.session_settings_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    glyph: WeatherGlyph,
    helper: String? = null,
    useWarmAccent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (useWarmAccent) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(14.dp)
                        )
                        .clearAndSetSemantics {},
                    contentAlignment = Alignment.Center
                ) {
                    WeatherIcon(
                        glyph = glyph,
                        modifier = Modifier.size(24.dp),
                        tint = if (useWarmAccent) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                    if (helper != null) {
                        Text(
                            helper,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun CityChoice(city: City, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .testTag("city_${city.name}")
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                cityLabel(city),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(
                    if (city == City.SINGAPORE) R.string.settings_visual_singapore_region
                    else R.string.settings_visual_queensland_region
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SelectionMark(selected = selected)
    }
}

@Composable
private fun TemperatureChoices(selectedUnit: TemperatureUnit, onUnitChanged: (TemperatureUnit) -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Reflow controls rather than shrinking labels when the user increases text size.
        val stackChoices = maxWidth < 300.dp || fontScale >= 1.2f
        if (stackChoices) {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TemperatureUnit.entries.forEach { unit ->
                    TemperatureChoice(unit, selectedUnit == unit, Modifier.fillMaxWidth()) { onUnitChanged(unit) }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TemperatureUnit.entries.forEach { unit ->
                    TemperatureChoice(unit, selectedUnit == unit, Modifier.weight(1f)) { onUnitChanged(unit) }
                }
            }
        }
    }
}

@Composable
private fun TemperatureChoice(
    unit: TemperatureUnit,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .testTag("unit_${unit.name}")
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(
                if (unit == TemperatureUnit.CELSIUS) R.string.unit_celsius else R.string.unit_fahrenheit
            ),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
        SelectionMark(selected = selected, small = true)
    }
}

@Composable
private fun ThresholdChoices(selectedThreshold: Int, enabled: Boolean, onThresholdChanged: (Int) -> Unit) {
    val thresholds = listOf(30, 60, 80)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stackChoices = maxWidth < 280.dp || fontScale >= 1.2f
        if (stackChoices) {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                thresholds.forEach { threshold ->
                    ThresholdChoice(
                        threshold, selectedThreshold == threshold, enabled, Modifier.fillMaxWidth()
                    ) { onThresholdChanged(threshold) }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                thresholds.forEach { threshold ->
                    ThresholdChoice(
                        threshold, selectedThreshold == threshold, enabled, Modifier.weight(1f)
                    ) { onThresholdChanged(threshold) }
                }
            }
        }
    }
}

@Composable
private fun ThresholdChoice(
    threshold: Int,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val accessibleLabel = stringResource(R.string.threshold_option, threshold)
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = modifier
            .heightIn(min = 88.dp)
            .clip(shape)
            .background(
                if (selected && enabled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .border(
                1.dp,
                if (selected && enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .testTag("threshold_$threshold")
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = accessibleLabel }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                stringResource(R.string.settings_visual_threshold_percent, threshold),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            SelectionMark(selected = selected, enabled = enabled, small = true)
        }
        Text(
            stringResource(
                when (threshold) {
                    30 -> R.string.settings_visual_threshold_lower
                    60 -> R.string.settings_visual_threshold_medium
                    else -> R.string.settings_visual_threshold_higher
                }
            ),
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled && !selected) MaterialTheme.colorScheme.onSurfaceVariant else contentColor
        )
    }
}

@Composable
private fun SelectionMark(selected: Boolean, enabled: Boolean = true, small: Boolean = false) {
    val color = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Box(
        modifier = Modifier
            .size(if (small) 18.dp else 24.dp)
            .background(if (selected) color else Color.Transparent, CircleShape)
            .border(
                1.dp,
                if (selected) color else MaterialTheme.colorScheme.outline,
                CircleShape
            )
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            WeatherIcon(
                glyph = WeatherGlyph.CHECK,
                modifier = Modifier.size(if (small) 12.dp else 16.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface
            )
        }
    }
}

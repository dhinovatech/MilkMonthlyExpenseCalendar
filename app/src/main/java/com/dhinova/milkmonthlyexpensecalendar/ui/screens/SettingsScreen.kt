package com.dhinova.milkmonthlyexpensecalendar.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dhinova.milkmonthlyexpensecalendar.data.PreferenceManager
import com.dhinova.milkmonthlyexpensecalendar.data.Settings
import com.dhinova.milkmonthlyexpensecalendar.ui.components.BannerAd
import com.dhinova.milkmonthlyexpensecalendar.ui.components.showInterstitial
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferenceManager: PreferenceManager,
    onSettingsSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var settings by remember { mutableStateOf(preferenceManager.getSettings()) }
    var isFirstLoad by remember { mutableStateOf(preferenceManager.isFirstLoad()) }
    var isLoading by remember { mutableStateOf(false) }

    // Dropdown options
    val units = listOf("litre", "ounce")
    val currencies = listOf("INR", "USD", "GBP", "EUR")
    val weekdayStarts = listOf("sunday", "monday")
    val applyTos = listOf("Future days only", "Current Month and Future days")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BannerAd()

            // Unit Dropdown
            DropdownField(
                label = "Unit",
                options = units,
                selectedOption = settings.unit,
                onOptionSelected = { settings = settings.copy(unit = it) }
            )

            // Default Volume
            OutlinedTextField(
                value = settings.defaultVolume.toString(),
                onValueChange = { 
                    settings = settings.copy(defaultVolume = it.toFloatOrNull() ?: 0f) 
                },
                label = { Text("Default Volume") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Cost per Unit
            OutlinedTextField(
                value = settings.costPerVolume.toString(),
                onValueChange = { 
                    settings = settings.copy(costPerVolume = it.toFloatOrNull() ?: 0f) 
                },
                label = { Text("Cost per Unit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Currency Dropdown
            DropdownField(
                label = "Currency",
                options = currencies,
                selectedOption = settings.currency,
                onOptionSelected = { 
                    val symbol = when(it) {
                        "INR" -> "₹"
                        "USD" -> "$"
                        "GBP" -> "£"
                        "EUR" -> "€"
                        else -> ""
                    }
                    settings = settings.copy(currency = it, currencySymbol = symbol) 
                }
            )

            // Weekday Start Dropdown
            DropdownField(
                label = "Weekday Start",
                options = weekdayStarts,
                selectedOption = settings.weekdayStart,
                onOptionSelected = { settings = settings.copy(weekdayStart = it) }
            )

            // Apply To Dropdown (only if not first load)
            if (!isFirstLoad) {
                DropdownField(
                    label = "Apply Changes To",
                    options = applyTos,
                    selectedOption = settings.applyTo,
                    onOptionSelected = { settings = settings.copy(applyTo = it) }
                )
            }

            Button(
                onClick = {
                    isLoading = true
                    // Save settings logic
                    preferenceManager.saveSettings(settings)
                    
                    // Update calendar data logic would go here if needed based on 'applyTo'
                    // For now, we just save settings and first load flag
                    if (isFirstLoad) {
                        preferenceManager.setFirstLoad(false)
                    }

                    showInterstitial(context) {
                        isLoading = false
                        Toast.makeText(context, "Settings Saved", Toast.LENGTH_SHORT).show()
                        onSettingsSaved()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Settings")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selectedOption,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

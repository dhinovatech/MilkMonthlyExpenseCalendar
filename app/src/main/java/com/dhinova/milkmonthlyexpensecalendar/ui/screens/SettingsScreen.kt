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
    val applyTos = listOf("Future days only", "Current Month and Future days", "Date from picked date")
    
    var showDatePicker by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        val minDate = java.time.YearMonth.now().minusYears(2).atDay(1)
                        val maxDate = java.time.YearMonth.now().plusMonths(1).atEndOfMonth()
                        
                        if (!date.isBefore(minDate) && !date.isAfter(maxDate)) {
                            pickedDate = date
                            showDatePicker = false
                        } else {
                            Toast.makeText(context, "Please select a date within the last 2 years.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }

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
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) {
                        settings = settings.copy(defaultVolume = it.toFloatOrNull() ?: 0f)
                    }
                },
                label = { Text("Default Volume") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Cost per Unit
            OutlinedTextField(
                value = settings.costPerVolume.toString(),
                onValueChange = { 
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) {
                        settings = settings.copy(costPerVolume = it.toFloatOrNull() ?: 0f)
                    }
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
                    onOptionSelected = { 
                        settings = settings.copy(applyTo = it)
                        if (it == "Date from picked date") {
                            showDatePicker = true
                        }
                    }
                )
                if (settings.applyTo == "Date from picked date" && pickedDate != null) {
                    Text(
                        text = "Selected Date: ${pickedDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                    Button(onClick = { showDatePicker = true }, modifier = Modifier.padding(start = 16.dp)) {
                        Text("Change Date")
                    }
                }
            }

            Button(
                onClick = {
                    isLoading = true
                    // Save settings logic
                    preferenceManager.saveSettings(settings)
                    
                    // Update calendar data based on 'applyTo'
                    val currentData = preferenceManager.getCalendarData().toMutableMap()
                    val today = java.time.LocalDate.now()
                    val firstDayOfCurrentMonth = java.time.YearMonth.now().atDay(1)
                    
                    // Purge data outside range: [Current Month - 2 months, Current Month + 1 month]
                    val minDate = java.time.YearMonth.now().minusYears(2).atDay(1)
                    val maxDate = java.time.YearMonth.now().plusMonths(1).atEndOfMonth()
                    
                    val keysToRemove = currentData.keys.filter { dateString ->
                        try {
                            val date = java.time.LocalDate.parse(dateString)
                            date.isBefore(minDate) || date.isAfter(maxDate)
                        } catch (e: Exception) {
                            true // Remove invalid keys
                        }
                    }
                    keysToRemove.forEach { currentData.remove(it) }

                    // Determine start date for applying changes
                    val applyStartDate: java.time.LocalDate? = when (settings.applyTo) {
                        "Future days only" -> today // "Future dates include current date"
                        "Current Month and Future days" -> firstDayOfCurrentMonth
                        "Date from picked date" -> pickedDate
                        else -> null
                    }

                    if (applyStartDate != null) {
                        val keysToUpdate = currentData.keys.filter { dateString ->
                            try {
                                val date = java.time.LocalDate.parse(dateString)
                                !date.isBefore(applyStartDate) // Apply from this date onwards
                            } catch (e: Exception) {
                                false
                            }
                        }

                        keysToUpdate.forEach { key ->
                            val currentDayData = currentData[key]
                            if (currentDayData != null) {
                                // Update with new defaults
                                currentData[key] = currentDayData.copy(
                                    volume = settings.defaultVolume,
                                    costPerUnit = settings.costPerVolume
                                )
                            }
                        }
                    }
                    preferenceManager.saveCalendarData(currentData)

                    // For first load, we just save settings and first load flag
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

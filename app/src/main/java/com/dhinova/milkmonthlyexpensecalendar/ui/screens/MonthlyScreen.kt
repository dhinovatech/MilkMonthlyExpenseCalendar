package com.dhinova.milkmonthlyexpensecalendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhinova.milkmonthlyexpensecalendar.data.DayData
import com.dhinova.milkmonthlyexpensecalendar.data.PreferenceManager
import com.dhinova.milkmonthlyexpensecalendar.ui.components.BannerAd
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyScreen(
    preferenceManager: PreferenceManager,
    onNavigateToSettings: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var calendarData by remember { mutableStateOf(preferenceManager.getCalendarData()) }
    val settings = preferenceManager.getSettings()

    // Dialog state
    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var editVolume by remember { mutableStateOf("") }
    var editCost by remember { mutableStateOf("") }

    // Calculate totals
    val monthDates = (1..currentMonth.lengthOfMonth()).map { currentMonth.atDay(it) }
    val monthData = monthDates.map { date ->
        val dateString = date.toString()
        val dayData = calendarData[dateString]
        val volume = dayData?.volume ?: settings.defaultVolume
        val costPerUnit = dayData?.costPerUnit ?: settings.costPerVolume
        DayData(volume, costPerUnit)
    }
    val totalVolume = monthData.sumOf { it.volume.toDouble() }.toFloat()
    val totalCost = monthData.sumOf { (it.volume * it.costPerUnit).toDouble() }.toFloat()

    // Persist default values when month changes
    LaunchedEffect(currentMonth) {
        val monthDates = (1..currentMonth.lengthOfMonth()).map { currentMonth.atDay(it) }
        var dataChanged = false
        val newMap = calendarData.toMutableMap()
        
        monthDates.forEach { date ->
            val dateString = date.toString()
            if (!newMap.containsKey(dateString)) {
                newMap[dateString] = DayData(settings.defaultVolume, settings.costPerVolume)
                dataChanged = true
            }
        }

        // Purge data older than 2 years
        val minDate = java.time.LocalDate.now().minusYears(2)
        val keysToRemove = newMap.keys.filter { dateString ->
            try {
                val date = java.time.LocalDate.parse(dateString)
                date.isBefore(minDate)
            } catch (e: Exception) {
                true
            }
        }
        
        if (keysToRemove.isNotEmpty()) {
            keysToRemove.forEach { newMap.remove(it) }
            dataChanged = true
        }
        
        if (dataChanged) {
            calendarData = newMap
            preferenceManager.saveCalendarData(newMap)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Milk Calendar") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            BannerAd()

            // Month Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val minMonth = YearMonth.now().minusYears(2)
                val maxMonth = YearMonth.now().plusMonths(1)

                IconButton(
                    onClick = { 
                        if (currentMonth.isAfter(minMonth)) {
                            currentMonth = currentMonth.minusMonths(1) 
                        }
                    },
                    enabled = currentMonth.isAfter(minMonth)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Previous Month",
                        tint = if (currentMonth.isAfter(minMonth)) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                }
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = { 
                        if (currentMonth.isBefore(maxMonth)) {
                            currentMonth = currentMonth.plusMonths(1) 
                        }
                    },
                    enabled = currentMonth.isBefore(maxMonth)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward, 
                        contentDescription = "Next Month",
                        tint = if (currentMonth.isBefore(maxMonth)) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                }
            }

            // Calendar Grid
            // Weekday Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                val weekDays = if (settings.weekdayStart == "sunday") {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                } else {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                }
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Days
            val firstDayOfMonth = currentMonth.atDay(1)
            val dayOfWeekValue = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            
            // Adjust for start day preference
            // If start is Sunday (7): Mon(1)->1, Tue(2)->2 ... Sun(7)->0
            // If start is Monday (1): Mon(1)->0, Tue(2)->1 ... Sun(7)->6
            val startOffset = if (settings.weekdayStart == "sunday") {
                if (dayOfWeekValue == 7) 0 else dayOfWeekValue
            } else {
                dayOfWeekValue - 1
            }

            val daysInMonth = currentMonth.lengthOfMonth()
            val totalCells = startOffset + daysInMonth
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                items(totalCells) { index ->
                    if (index < startOffset) {
                        Box(modifier = Modifier.aspectRatio(1f)) // Empty cell
                    } else {
                        val day = index - startOffset + 1
                        val date = currentMonth.atDay(day)
                        val dateString = date.toString()
                        val dayData = calendarData[dateString]
                        
                        // Use default if no data, but don't save yet
                        val displayVolume = dayData?.volume ?: settings.defaultVolume
                        val displayCostPerUnit = dayData?.costPerUnit ?: settings.costPerVolume
                        
                        val isToday = date == LocalDate.now()

                        Card(
                            modifier = Modifier
                                .padding(2.dp)
                                .aspectRatio(1f)
                                .clickable {
                                    selectedDate = date
                                    editVolume = displayVolume.toString()
                                    editCost = displayCostPerUnit.toString()
                                    showDialog = true
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = day.toString(), fontWeight = FontWeight.Bold)
                                val unitAbbr = if (settings.unit == "litre") "L" else "Oz"
                                val volFormat = java.text.DecimalFormat("#.###")
                                val costFormat = java.text.DecimalFormat("#.##")
                                Text(text = "${volFormat.format(displayVolume)} $unitAbbr", style = MaterialTheme.typography.bodySmall)
                                Text(text = "${settings.currencySymbol}${costFormat.format(displayCostPerUnit)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Totals Footer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Volume", style = MaterialTheme.typography.labelMedium)
                        val volFormat = java.text.DecimalFormat("#.###")
                        Text("${volFormat.format(totalVolume)} ${settings.unit}", style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Cost", style = MaterialTheme.typography.labelMedium)
                        Text("${settings.currencySymbol}${String.format("%.2f", totalCost)}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (showDialog && selectedDate != null) {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Edit Entry: ${selectedDate?.format(formatter)}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editVolume,
                        onValueChange = { 
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                editVolume = it
                            }
                        },
                        label = { Text("Volume") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextButton(onClick = { editVolume = "0" }) {
                        Text("Make Volume Zero")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editCost,
                        onValueChange = { 
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                editCost = it
                            }
                        },
                        label = { Text("Cost per Unit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val vol = editVolume.toFloatOrNull() ?: 0f
                    val costPerUnit = editCost.toFloatOrNull() ?: 0f
                    val newDayData = DayData(vol, costPerUnit)
                    
                    val newMap = calendarData.toMutableMap()
                    newMap[selectedDate.toString()] = newDayData
                    calendarData = newMap // Update state
                    preferenceManager.saveCalendarData(newMap) // Persist
                    
                    showDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

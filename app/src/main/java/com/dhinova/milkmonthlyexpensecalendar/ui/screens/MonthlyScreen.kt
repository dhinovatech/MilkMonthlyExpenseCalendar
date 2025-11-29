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
    val monthData = monthDates.mapNotNull { date ->
        calendarData[date.toString()]
    }
    val totalVolume = monthData.sumOf { it.volume.toDouble() }.toFloat()
    val totalCost = monthData.sumOf { it.cost.toDouble() }.toFloat()

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
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                }
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
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
                        val displayCost = dayData?.cost ?: (displayVolume * settings.costPerVolume)
                        
                        val isToday = date == LocalDate.now()

                        Card(
                            modifier = Modifier
                                .padding(2.dp)
                                .aspectRatio(1f)
                                .clickable {
                                    selectedDate = date
                                    editVolume = displayVolume.toString()
                                    editCost = displayCost.toString()
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
                                Text(text = String.format("%.1f", displayVolume), style = MaterialTheme.typography.bodySmall)
                                Text(text = "${settings.currencySymbol}${String.format("%.0f", displayCost)}", style = MaterialTheme.typography.bodySmall)
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
                        Text(String.format("%.2f %s", totalVolume, settings.unit), style = MaterialTheme.typography.titleMedium)
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
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Edit Entry: ${selectedDate}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editVolume,
                        onValueChange = { 
                            editVolume = it
                            // Auto-calc cost based on volume change if desired, or let user edit both
                            val vol = it.toFloatOrNull() ?: 0f
                            editCost = (vol * settings.costPerVolume).toString()
                        },
                        label = { Text("Volume") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editCost,
                        onValueChange = { editCost = it },
                        label = { Text("Cost") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val vol = editVolume.toFloatOrNull() ?: 0f
                    val cost = editCost.toFloatOrNull() ?: 0f
                    val newDayData = DayData(vol, cost)
                    
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

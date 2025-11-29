package com.dhinova.milkmonthlyexpensecalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dhinova.milkmonthlyexpensecalendar.data.PreferenceManager
import com.dhinova.milkmonthlyexpensecalendar.ui.screens.MonthlyScreen
import com.dhinova.milkmonthlyexpensecalendar.ui.screens.SettingsScreen
import com.dhinova.milkmonthlyexpensecalendar.ui.theme.MilkMonthlyExpenseCalendarTheme
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob
        MobileAds.initialize(this) {}

        val preferenceManager = PreferenceManager(this)

        setContent {
            MilkMonthlyExpenseCalendarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = if (preferenceManager.isFirstLoad()) "settings" else "monthly"

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("monthly") {
                            MonthlyScreen(
                                preferenceManager = preferenceManager,
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                preferenceManager = preferenceManager,
                                onSettingsSaved = {
                                    // Clear back stack so user can't go back to settings easily if it was first load
                                    navController.navigate("monthly") {
                                        popUpTo("settings") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
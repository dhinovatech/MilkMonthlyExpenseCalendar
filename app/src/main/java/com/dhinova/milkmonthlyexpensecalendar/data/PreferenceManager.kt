package com.dhinova.milkmonthlyexpensecalendar.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MilkCalendarPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_SETTINGS = "PREF_SETTINGS"
        private const val KEY_CALENDAR_DATA = "PREF_CALENDAR_DATA"
        private const val KEY_FIRST_LOAD = "PREF_FIRST_LOAD"
    }

    fun saveSettings(settings: Settings) {
        val json = gson.toJson(settings)
        sharedPreferences.edit().putString(KEY_SETTINGS, json).apply()
    }

    fun getSettings(): Settings {
        val json = sharedPreferences.getString(KEY_SETTINGS, null)
        return if (json != null) {
            gson.fromJson(json, Settings::class.java)
        } else {
            Settings()
        }
    }

    fun saveCalendarData(data: Map<String, DayData>) {
        val json = gson.toJson(data)
        sharedPreferences.edit().putString(KEY_CALENDAR_DATA, json).apply()
    }

    fun getCalendarData(): Map<String, DayData> {
        val json = sharedPreferences.getString(KEY_CALENDAR_DATA, null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, DayData>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyMap()
        }
    }

    fun isFirstLoad(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LOAD, true)
    }

    fun setFirstLoad(isFirstLoad: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_FIRST_LOAD, isFirstLoad).apply()
    }
}

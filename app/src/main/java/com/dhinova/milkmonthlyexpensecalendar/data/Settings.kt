package com.dhinova.milkmonthlyexpensecalendar.data

data class Settings(
    val unit: String = "litre", // "litre", "ounce"
    val defaultVolume: Float = 1.0f,
    val costPerVolume: Float = 50.0f,
    val currency: String = "INR", // "INR", "USD", "GBP", "EUR"
    val currencySymbol: String = "₹", // "₹", "$", "£", "€"
    val weekdayStart: String = "sunday", // "sunday", "monday"
    val applyTo: String = "Future days only" // "Future days only", "Current Month and Future days"
)

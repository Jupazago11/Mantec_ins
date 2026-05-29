package com.example.mantec_ins.presentation.navigation

sealed class AppScreen {
    data object Loading : AppScreen()
    data object Login : AppScreen()
    data object Home : AppScreen()
    data object Report : AppScreen()
    data object MeasurementThickness : AppScreen()
    data object UnsupportedRole : AppScreen()
}
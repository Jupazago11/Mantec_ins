package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.mantec_ins.presentation.navigation.AppScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppNavigationViewModel : ViewModel() {

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Loading)
    val currentScreen: StateFlow<AppScreen> = _currentScreen

    fun goToLogin() {
        _currentScreen.value = AppScreen.Login
    }

    fun goToHome() {
        _currentScreen.value = AppScreen.Home
    }

    fun goToReport() {
        _currentScreen.value = AppScreen.Report
    }

    fun goToMeasurementThickness() {
        _currentScreen.value = AppScreen.MeasurementThickness
    }

    fun goToUnsupportedRole() {
        _currentScreen.value = AppScreen.UnsupportedRole
    }

    fun logout() {
        _currentScreen.value = AppScreen.Login
    }
}
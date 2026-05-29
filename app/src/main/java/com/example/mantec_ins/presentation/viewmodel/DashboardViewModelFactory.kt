package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mantec_ins.data.repository.CatalogLocalRepository
import com.example.mantec_ins.data.repository.InspectionLocalRepository
import com.example.mantec_ins.data.repository.PendingDiagnosticsRepository
import com.example.mantec_ins.data.repository.WeeklyElementStatusRepository

class DashboardViewModelFactory(
    private val catalogRepository: CatalogLocalRepository,
    private val inspectionRepository: InspectionLocalRepository,
    private val pendingDiagnosticsRepository: PendingDiagnosticsRepository,
    private val weeklyElementStatusRepository: WeeklyElementStatusRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(
                catalogRepository = catalogRepository,
                inspectionRepository = inspectionRepository,
                pendingDiagnosticsRepository = pendingDiagnosticsRepository,
                weeklyElementStatusRepository = weeklyElementStatusRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
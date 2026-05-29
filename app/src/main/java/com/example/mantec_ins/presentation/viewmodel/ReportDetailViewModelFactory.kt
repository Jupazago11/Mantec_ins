package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mantec_ins.data.repository.CatalogLocalRepository
import com.example.mantec_ins.data.repository.InspectionLocalRepository

class ReportDetailViewModelFactory(
    private val inspectionRepository: InspectionLocalRepository,
    private val catalogRepository: CatalogLocalRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportDetailViewModel::class.java)) {
            return ReportDetailViewModel(
                inspectionRepository = inspectionRepository,
                catalogRepository = catalogRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
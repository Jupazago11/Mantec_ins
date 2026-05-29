package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mantec_ins.data.repository.CatalogLocalRepository
import com.example.mantec_ins.data.repository.InspectionLocalRepository

class InspectionBatchViewModelFactory(
    private val catalogRepository: CatalogLocalRepository,
    private val inspectionRepository: InspectionLocalRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InspectionBatchViewModel::class.java)) {
            return InspectionBatchViewModel(
                catalogRepository = catalogRepository,
                inspectionRepository = inspectionRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

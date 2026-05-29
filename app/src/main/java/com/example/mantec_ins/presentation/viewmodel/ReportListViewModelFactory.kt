package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mantec_ins.data.repository.InspectionLocalRepository

class ReportListViewModelFactory(
    private val repository: InspectionLocalRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportListViewModel::class.java)) {
            return ReportListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
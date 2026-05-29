package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mantec_ins.data.repository.MeasurementThicknessRepository

class MeasurementPendingViewModelFactory(
    private val repository: MeasurementThicknessRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeasurementPendingViewModel::class.java)) {
            return MeasurementPendingViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
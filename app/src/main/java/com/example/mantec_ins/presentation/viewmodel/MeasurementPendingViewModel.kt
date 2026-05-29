package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.repository.MeasurementThicknessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeasurementPendingViewModel(
    private val repository: MeasurementThicknessRepository
) : ViewModel() {

    private val _pendingDraftCount = MutableStateFlow(0)
    val pendingDraftCount: StateFlow<Int> = _pendingDraftCount

    private val _hasMeasurementAccess = MutableStateFlow(false)
    val hasMeasurementAccess: StateFlow<Boolean> = _hasMeasurementAccess

    fun loadPendingDraftCount() {
        viewModelScope.launch {
            _pendingDraftCount.value = repository.countPendingDrafts()
        }
    }

    fun loadMeasurementAccess() {
        viewModelScope.launch {
            _hasMeasurementAccess.value = repository.hasEnabledElementTypes()
        }
    }

    fun syncPendingDrafts() {
        viewModelScope.launch {
            repository.syncAllPendingDrafts()
            _pendingDraftCount.value = repository.countPendingDrafts()
        }
    }
}
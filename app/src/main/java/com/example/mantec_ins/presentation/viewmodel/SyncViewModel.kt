package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SyncViewModel(
    private val repository: SyncRepository
) : ViewModel() {

    private val _lastSyncedCount = MutableStateFlow(0)
    val lastSyncedCount: StateFlow<Int> = _lastSyncedCount

    fun syncPendingReports() {
        viewModelScope.launch {
            _lastSyncedCount.value = repository.syncPendingReports()
        }
    }
}

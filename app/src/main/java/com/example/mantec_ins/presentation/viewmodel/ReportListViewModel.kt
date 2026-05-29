package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.local.ReportEntity
import com.example.mantec_ins.data.repository.InspectionLocalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class ReportListViewModel(
    private val repository: InspectionLocalRepository
) : ViewModel() {

    private val _reports = MutableStateFlow<List<ReportEntity>>(emptyList())
    val reports: StateFlow<List<ReportEntity>> = _reports

    private val _pendingSyncItems = MutableStateFlow<List<PendingSyncReportItemUi>>(emptyList())
    val pendingSyncItems: StateFlow<List<PendingSyncReportItemUi>> = _pendingSyncItems

    private val _localPendingDiagnosticItems =
        MutableStateFlow<List<LocalPendingDiagnosticItemUi>>(emptyList())

    val localPendingDiagnosticItems: StateFlow<List<LocalPendingDiagnosticItemUi>> =
        _localPendingDiagnosticItems

    fun loadReports() {
        viewModelScope.launch {
            _reports.value = repository.getAllReports()
        }
    }

    fun loadPendingReports() {
        viewModelScope.launch {
            _reports.value = repository.getPendingReports()
        }
    }

    fun loadPendingSyncItems() {
        viewModelScope.launch {
            _pendingSyncItems.value = repository.getPendingSyncReportItems()
        }
    }

    fun loadLocalPendingDiagnosticItemsForCurrentWeek() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                minimalDaysInFirstWeek = 4
            }

            val week = calendar.get(Calendar.WEEK_OF_YEAR)
            val year = calendar.weekYear

            _localPendingDiagnosticItems.value =
                repository.getLocalPendingDiagnosticsForWeek(
                    week = week,
                    year = year
                )
        }
    }
}

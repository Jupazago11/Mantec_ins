package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.repository.CatalogLocalRepository
import com.example.mantec_ins.data.repository.InspectionLocalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.example.mantec_ins.data.repository.WeeklyElementStatusRepository
import java.text.SimpleDateFormat
import com.example.mantec_ins.data.repository.PendingDiagnosticsRepository
import java.util.Locale
import com.example.mantec_ins.data.local.ElementEntity

class DashboardViewModel(
    private val catalogRepository: CatalogLocalRepository,
    private val inspectionRepository: InspectionLocalRepository,
    private val pendingDiagnosticsRepository: PendingDiagnosticsRepository,
    private val weeklyElementStatusRepository: WeeklyElementStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    fun loadPendingDiagnosticsForElement(
        elementId: Long,
        tryServerRefresh: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                if (tryServerRefresh) {
                    pendingDiagnosticsRepository.refreshFromServer(elementId)
                }

                val cachedStatus = pendingDiagnosticsRepository.getCachedStatusForElement(elementId)

                val weeklyItems = cachedStatus.map {
                    PendingDiagnosticItemUi(
                        elementId = it.elementId,
                        componentId = it.componentId,
                        componentName = it.componentName,
                        diagnosticId = it.diagnosticId,
                        diagnosticName = it.diagnosticName,
                        status = it.status
                    )
                }

                val pendingItems = weeklyItems.filter { it.status == "PENDING" }

                android.util.Log.d(
                    "DASHBOARD_STATUS",
                    "=============================="
                )

                android.util.Log.d(
                    "DASHBOARD_STATUS",
                    "loadPendingDiagnosticsForElement(elementId=$elementId)"
                )

                android.util.Log.d(
                    "DASHBOARD_STATUS",
                    "cachedStatus.size=${cachedStatus.size}"
                )

                android.util.Log.d(
                    "DASHBOARD_STATUS",
                    "weeklyItems.size=${weeklyItems.size}"
                )

                android.util.Log.d(
                    "DASHBOARD_STATUS",
                    "DONE count=${weeklyItems.count { it.status == "DONE" }}"
                )

                android.util.Log.d(
                    "DASHBOARD_STATUS",
                    "PENDING count=${weeklyItems.count { it.status == "PENDING" }}"
                )

                weeklyItems.forEach {
                    android.util.Log.d(
                        "DASHBOARD_STATUS",
                        "elementId=${it.elementId} componentId=${it.componentId} component=${it.componentName} diagnosticId=${it.diagnosticId} diagnostic=${it.diagnosticName} status=${it.status}"
                    )
                }

                android.util.Log.d(
                    "DASHBOARD_STATUS",
                    "=============================="
                )

                _uiState.value = _uiState.value.copy(
                    pendingItems = pendingItems,
                    weeklyDiagnosticItems = weeklyItems
                )
            } catch (e: Exception) {
                android.util.Log.e("DASHBOARD", "Error cargando diagnósticos pendientes para elementId=$elementId", e)
            }
        }
    }

    fun loadWeeklyElementsStatus(
        areaId: Long,
        elementTypeId: Long,
        tryServerRefresh: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                if (tryServerRefresh) {
                    weeklyElementStatusRepository.refreshFromServer(
                        areaId = areaId,
                        elementTypeId = elementTypeId
                    )
                }

                val cached = weeklyElementStatusRepository.getCachedStatus(
                    areaId = areaId,
                    elementTypeId = elementTypeId
                )

                val items = cached.map {
                    WeeklyElementStatusItemUi(
                        areaId = it.areaId,
                        elementTypeId = it.elementTypeId,
                        elementId = it.elementId,
                        elementName = it.elementName,
                        status = it.status,
                        expectedCount = it.expectedCount,
                        doneCount = it.doneCount
                    )
                }

                _uiState.value = _uiState.value.copy(
                    weeklyElementStatuses = items
                )
            } catch (e: Exception) {
                android.util.Log.e("DASHBOARD", "Error cargando estado semanal para areaId=$areaId elementTypeId=$elementTypeId", e)
            }
        }
    }

    fun loadWeeklyElementsStatusForElements(
        elements: List<ElementEntity>,
        tryServerRefresh: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                if (elements.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        weeklyElementStatuses = emptyList()
                    )
                    return@launch
                }

                val pairs = elements
                    .map { it.areaId to it.elementTypeId }
                    .distinct()

                val allowedElementIds = elements.map { it.id }.toSet()

                suspend fun emitFromCache() {
                    val allCachedItems = pairs.flatMap { (areaId, elementTypeId) ->
                        weeklyElementStatusRepository.getCachedStatus(
                            areaId = areaId,
                            elementTypeId = elementTypeId
                        )
                    }

                    val items = allCachedItems
                        .filter { it.elementId in allowedElementIds }
                        .distinctBy { it.elementId }
                        .map {
                            WeeklyElementStatusItemUi(
                                areaId = it.areaId,
                                elementTypeId = it.elementTypeId,
                                elementId = it.elementId,
                                elementName = it.elementName,
                                status = it.status,
                                expectedCount = it.expectedCount,
                                doneCount = it.doneCount
                            )
                        }

                    _uiState.value = _uiState.value.copy(
                        weeklyElementStatuses = items
                    )
                }

                // Local-first: pinta lo que ya haya en caché antes de esperar la red,
                // para que los íconos de área/activo no tarden lo que tarden N requests.
                emitFromCache()

                if (tryServerRefresh) {
                    coroutineScope {
                        pairs.map { (areaId, elementTypeId) ->
                            async {
                                weeklyElementStatusRepository.refreshFromServer(
                                    areaId = areaId,
                                    elementTypeId = elementTypeId
                                )
                            }
                        }.awaitAll()
                    }

                    emitFromCache()
                }
            } catch (e: Exception) {
                android.util.Log.e("DASHBOARD", "Error cargando estado semanal para ${elements.size} elementos", e)
            }
        }
    }

    fun checkCatalogCompleteness(
        groupId: Long,
        tryRepairIfIncomplete: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    catalogCompleteness = _uiState.value.catalogCompleteness.copy(
                        status = CatalogCompletenessStatus.CHECKING
                    )
                )

                var result = catalogRepository.auditCatalogCompleteness(groupId)

                if (!result.isComplete) {
                    android.util.Log.w(
                        "CATALOG_COMPLETENESS",
                        "Catálogo incompleto: ${result.gaps.size} componente(s) con huecos. Intentando reparar con el servidor."
                    )

                    try {
                        tryRepairIfIncomplete()
                        result = catalogRepository.auditCatalogCompleteness(groupId)
                    } catch (e: Exception) {
                        android.util.Log.w(
                            "CATALOG_COMPLETENESS",
                            "No se pudo reparar el catálogo (probablemente sin conexión).",
                            e
                        )
                    }
                }

                _uiState.value = _uiState.value.copy(
                    catalogCompleteness = CatalogCompletenessUi(
                        status = if (result.isComplete) {
                            CatalogCompletenessStatus.COMPLETE
                        } else {
                            CatalogCompletenessStatus.INCOMPLETE
                        },
                        gapCount = result.gaps.size
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("CATALOG_COMPLETENESS", "Error verificando completitud del catálogo", e)

                _uiState.value = _uiState.value.copy(
                    catalogCompleteness = CatalogCompletenessUi(status = CatalogCompletenessStatus.UNKNOWN)
                )
            }
        }
    }

    fun loadRecentReports24h() {
        viewModelScope.launch {
            try {
                val calendar = java.util.Calendar.getInstance()
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)

                val fromDate = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(calendar.time)

                val recentDetails = inspectionRepository.getRecentReportDetailsFromDate(fromDate)
                val reports = inspectionRepository.getAllReports()

                val mapped = recentDetails.map { detail ->
                    val report = reports.firstOrNull { it.localId == detail.reportLocalId }
                    val element = report?.elementId?.let { catalogRepository.getElementById(it) }
                    val component = catalogRepository.getComponentById(detail.componentId)
                    val diagnostic = catalogRepository.getDiagnosticById(detail.diagnosticId)
                    val condition = catalogRepository.getConditionById(detail.conditionId)

                    RecentReportItemUi(
                        reportLocalId = detail.reportLocalId,
                        elementName = element?.name ?: "Activo",
                        componentName = component?.name ?: "Componente",
                        diagnosticName = diagnostic?.name ?: "Diagnóstico",
                        conditionName = condition?.name ?: "Condición",
                        executionDate = detail.executionDate,
                        reportStatus = report?.status ?: "LOCAL_ONLY",
                        detailSyncStatus = detail.syncStatus
                    )
                }

                _uiState.value = _uiState.value.copy(
                    recentItems = mapped
                )
            } catch (e: Exception) {
                android.util.Log.e("DASHBOARD", "Error cargando reportes recientes de las últimas 24h", e)
            }
        }
    }
}
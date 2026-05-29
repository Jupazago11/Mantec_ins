package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.repository.CatalogLocalRepository
import com.example.mantec_ins.data.repository.RemoteCatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RemoteCatalogViewModel(
    private val remoteRepository: RemoteCatalogRepository,
    private val localRepository: CatalogLocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteCatalogUiState())
    val uiState: StateFlow<RemoteCatalogUiState> = _uiState

    fun loadInitialCatalog(clientId: Long) {
        if (clientId == 0L) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val localAreas = localRepository.getAreasByClient(clientId)
                val localConditions = localRepository.getConditionsByClient(clientId)

                _uiState.value = _uiState.value.copy(
                    areas = localAreas,
                    conditions = localConditions,
                    isLoading = false,
                    errorMessage = null
                )

                remoteRepository.syncInitialCatalog(clientId)

                val refreshedAreas = localRepository.getAreasByClient(clientId)
                val refreshedConditions = localRepository.getConditionsByClient(clientId)

                _uiState.value = _uiState.value.copy(
                    areas = refreshedAreas,
                    conditions = refreshedConditions,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                val fallbackAreas = localRepository.getAreasByClient(clientId)
                val fallbackConditions = localRepository.getConditionsByClient(clientId)

                _uiState.value = _uiState.value.copy(
                    areas = fallbackAreas,
                    conditions = fallbackConditions,
                    isLoading = false,
                    errorMessage = if (fallbackAreas.isEmpty() && fallbackConditions.isEmpty()) {
                        e.message ?: "Error cargando catálogo"
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun loadElementsByArea(areaId: Long) {
        if (areaId == 0L) return

        viewModelScope.launch {
            try {
                val localElements = localRepository.getElementsByArea(areaId)

                _uiState.value = _uiState.value.copy(
                    elements = localElements,
                    components = emptyList(),
                    diagnostics = emptyList(),
                    errorMessage = null
                )

                remoteRepository.syncElementsByArea(areaId)

                val refreshedElements = localRepository.getElementsByArea(areaId)

                _uiState.value = _uiState.value.copy(
                    elements = refreshedElements,
                    components = emptyList(),
                    diagnostics = emptyList(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                val fallbackElements = localRepository.getElementsByArea(areaId)

                _uiState.value = _uiState.value.copy(
                    elements = fallbackElements,
                    components = emptyList(),
                    diagnostics = emptyList(),
                    errorMessage = if (fallbackElements.isEmpty()) {
                        e.message ?: "Error cargando activos"
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun loadComponentsByElement(elementId: Long) {
        if (elementId == 0L) return

        viewModelScope.launch {
            try {
                val localComponents = localRepository.getComponentsByElement(elementId)

                _uiState.value = _uiState.value.copy(
                    components = localComponents,
                    diagnostics = emptyList(),
                    errorMessage = null
                )

                remoteRepository.syncComponentsByElement(elementId)

                val refreshedComponents = localRepository.getComponentsByElement(elementId)

                _uiState.value = _uiState.value.copy(
                    components = refreshedComponents,
                    diagnostics = emptyList(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                val fallbackComponents = localRepository.getComponentsByElement(elementId)

                _uiState.value = _uiState.value.copy(
                    components = fallbackComponents,
                    diagnostics = emptyList(),
                    errorMessage = if (fallbackComponents.isEmpty()) {
                        e.message ?: "Error cargando componentes"
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun loadDiagnosticsByComponent(componentId: Long, elementId: Long) {
        if (componentId == 0L || elementId == 0L) return

        viewModelScope.launch {
            try {
                val localDiagnostics = localRepository.getDiagnosticsByComponent(componentId)

                _uiState.value = _uiState.value.copy(
                    diagnostics = localDiagnostics,
                    errorMessage = null
                )

                remoteRepository.syncDiagnosticsByComponent(
                    componentId = componentId,
                    elementId = elementId
                )

                val refreshedDiagnostics = localRepository.getDiagnosticsByComponent(componentId)

                _uiState.value = _uiState.value.copy(
                    diagnostics = refreshedDiagnostics,
                    errorMessage = null
                )
            } catch (e: Exception) {
                val fallbackDiagnostics = localRepository.getDiagnosticsByComponent(componentId)

                _uiState.value = _uiState.value.copy(
                    diagnostics = fallbackDiagnostics,
                    errorMessage = if (fallbackDiagnostics.isEmpty()) {
                        e.message ?: "Error cargando diagnósticos"
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun clearElements() {
        _uiState.value = _uiState.value.copy(
            elements = emptyList(),
            components = emptyList(),
            diagnostics = emptyList()
        )
    }

    fun clearComponents() {
        _uiState.value = _uiState.value.copy(
            components = emptyList(),
            diagnostics = emptyList()
        )
    }

    fun clearDiagnostics() {
        _uiState.value = _uiState.value.copy(
            diagnostics = emptyList()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.local.AreaEntity
import com.example.mantec_ins.data.local.ComponentEntity
import com.example.mantec_ins.data.local.ConditionEntity
import com.example.mantec_ins.data.local.DiagnosticEntity
import com.example.mantec_ins.data.local.ElementEntity
import com.example.mantec_ins.data.repository.CatalogLocalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.mantec_ins.data.repository.RemoteCatalogRepository


class CatalogViewModel(
    private val repository: CatalogLocalRepository,
    private val remoteCatalogRepository: RemoteCatalogRepository
) : ViewModel() {

    private val _areas = MutableStateFlow<List<AreaEntity>>(emptyList())
    val areas: StateFlow<List<AreaEntity>> = _areas.asStateFlow()

    private val _elements = MutableStateFlow<List<ElementEntity>>(emptyList())
    val elements: StateFlow<List<ElementEntity>> = _elements.asStateFlow()

    private val _components = MutableStateFlow<List<ComponentEntity>>(emptyList())
    val components: StateFlow<List<ComponentEntity>> = _components.asStateFlow()

    private val _diagnostics = MutableStateFlow<List<DiagnosticEntity>>(emptyList())
    val diagnostics: StateFlow<List<DiagnosticEntity>> = _diagnostics.asStateFlow()

    private val _conditions = MutableStateFlow<List<ConditionEntity>>(emptyList())
    val conditions: StateFlow<List<ConditionEntity>> = _conditions.asStateFlow()

    private val _selectedElementId = MutableStateFlow<Long?>(null)
    val selectedElementId: StateFlow<Long?> = _selectedElementId.asStateFlow()

    private val _selectedComponentId = MutableStateFlow<Long?>(null)
    val selectedComponentId: StateFlow<Long?> = _selectedComponentId.asStateFlow()

    fun loadAreas(clientId: Long) {
        viewModelScope.launch {
            _areas.value = repository.getAreasByClient(clientId)
        }
    }

    fun loadAreasByClientAndElementType(clientId: Long, elementTypeId: Long) {
        viewModelScope.launch {
            _areas.value = repository.getAreasByClientAndElementType(
                clientId = clientId,
                elementTypeId = elementTypeId
            )
        }
    }


    fun loadElements(areaId: Long) {
        viewModelScope.launch {
            _elements.value = repository.getElementsByArea(areaId)
        }
    }

    fun loadElementsByAreaAndType(areaId: Long, elementTypeId: Long) {
        viewModelScope.launch {
            _elements.value = repository.getElementsByAreaAndType(
                areaId = areaId,
                elementTypeId = elementTypeId
            )
        }
    }

    fun loadElementsByGroup(groupId: Long) {
        viewModelScope.launch {
            _elements.value = repository.getElementsByGroup(groupId)
        }
    }

    fun loadComponents(elementId: Long) {
        viewModelScope.launch {
            _components.value = repository.getComponentsByElement(elementId)
        }
    }

    fun loadDiagnostics(componentId: Long) {
        viewModelScope.launch {
            _diagnostics.value = repository.getDiagnosticsByComponent(componentId)
        }
    }

    fun loadConditionsForComponent(
        elementId: Long,
        componentId: Long
    ) {
        viewModelScope.launch {
            try {
                _conditions.value = remoteCatalogRepository.getConditionsByElementAndComponent(
                    elementId = elementId,
                    componentId = componentId
                )
            } catch (e: Exception) {
                android.util.Log.e(
                    "CATALOG_VM",
                    "Error cargando condiciones remotas para elementId=$elementId componentId=$componentId. Usando fallback local.",
                    e
                )

                _conditions.value = repository.getConditionsByComponent(componentId)
            }
        }
    }

    fun onElementSelected(elementId: Long) {
        _selectedElementId.value = elementId
        _selectedComponentId.value = null
    }

    fun onComponentSelected(componentId: Long) {
        _selectedComponentId.value = componentId
    }

    fun clearElements() {
        _elements.value = emptyList()
    }

    fun clearComponents() {
        _components.value = emptyList()
    }

    fun clearDiagnostics() {
        _diagnostics.value = emptyList()
    }

    fun clearConditions() {
        _conditions.value = emptyList()
    }

    fun clearComponentSelection() {
        _selectedComponentId.value = null
    }
    fun clearElementSelection() {
        _selectedElementId.value = null
    }


    fun clearAllSelections() {
        _selectedElementId.value = null
        _selectedComponentId.value = null
        _elements.value = emptyList()
        _components.value = emptyList()
        _diagnostics.value = emptyList()
        _conditions.value = emptyList()
    }
}

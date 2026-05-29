package com.example.mantec_ins.data.repository

import com.example.mantec_ins.data.local.AppDatabase
import com.example.mantec_ins.data.local.AreaEntity
import com.example.mantec_ins.data.local.ComponentEntity
import com.example.mantec_ins.data.local.ConditionEntity
import com.example.mantec_ins.data.local.DiagnosticEntity
import com.example.mantec_ins.data.local.ElementEntity
import com.example.mantec_ins.data.local.ElementTypeEntity
import com.example.mantec_ins.data.local.GroupEntity

class CatalogLocalRepository(
    private val database: AppDatabase
) {

    suspend fun getAssignedGroup(): GroupEntity? {
        return database.groupDao().getFirst()
    }

    suspend fun getElementsByGroup(groupId: Long): List<ElementEntity> {
        return database.elementDao().getByGroup(groupId)
    }

    suspend fun getAreasByClient(clientId: Long): List<AreaEntity> {
        return database.areaDao().getByClient(clientId)
    }

    suspend fun getElementTypesByClient(clientId: Long): List<ElementTypeEntity> {
        return database.elementTypeDao().getByClient(clientId)
    }

    suspend fun getElementsByArea(areaId: Long): List<ElementEntity> {
        return database.elementDao().getByArea(areaId)
    }

    suspend fun getElementsByAreaAndType(
        areaId: Long,
        elementTypeId: Long
    ): List<ElementEntity> {
        return database.elementDao().getByAreaAndType(areaId, elementTypeId)
    }

    suspend fun getElementById(elementId: Long): ElementEntity? {
        return database.elementDao().getById(elementId)
    }

    suspend fun getAreasByClientAndElementType(
        clientId: Long,
        elementTypeId: Long
    ): List<AreaEntity> {
        val areas = database.areaDao().getByClient(clientId)
        val elements = database.elementDao().getByElementType(elementTypeId)
        val allowedAreaIds = elements.map { it.areaId }.toSet()

        return areas.filter { it.id in allowedAreaIds }
    }


    suspend fun getComponentsByElement(elementId: Long): List<ComponentEntity> {
        return database.catalogRelationDao()
            .getElementWithComponents(elementId)
            ?.components
            ?.sortedBy { it.name.trim().lowercase() }
            ?: emptyList()
    }

    suspend fun getComponentById(componentId: Long): ComponentEntity? {
        return database.componentDao().getById(componentId)
    }

    suspend fun getDiagnosticsByComponent(componentId: Long): List<DiagnosticEntity> {
        return database.catalogRelationDao()
            .getComponentWithDiagnostics(componentId)
            ?.diagnostics
            ?.sortedBy { it.name.trim().lowercase() }
            ?: emptyList()
    }

    suspend fun getConditionsByComponent(componentId: Long): List<ConditionEntity> {
        return database.catalogRelationDao()
            .getComponentWithConditions(componentId)
            ?.conditions
            ?.sortedWith(compareBy<ConditionEntity> { it.severity }.thenBy { it.name })
            ?: emptyList()
    }

    suspend fun getDiagnosticById(diagnosticId: Long): DiagnosticEntity? {
        return database.diagnosticDao().getById(diagnosticId)
    }

    suspend fun getConditionsByClient(clientId: Long): List<ConditionEntity> {
        return database.conditionDao().getByClient(clientId)
    }

    suspend fun getConditionsByClientAndElementType(
        clientId: Long,
        elementTypeId: Long
    ): List<ConditionEntity> {
        return database.conditionDao().getByClientAndElementType(
            clientId = clientId,
            elementTypeId = elementTypeId
        )
    }

    suspend fun getConditionById(conditionId: Long): ConditionEntity? {
        return database.conditionDao().getById(conditionId)
    }
}

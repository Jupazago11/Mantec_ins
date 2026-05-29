package com.example.mantec_ins.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.mantec_ins.data.local.AppDatabase
import com.example.mantec_ins.data.local.AreaEntity
import com.example.mantec_ins.data.local.ClientEntity
import com.example.mantec_ins.data.local.ComponentConditionCrossRef
import com.example.mantec_ins.data.local.ComponentDiagnosticCrossRef
import com.example.mantec_ins.data.local.ComponentEntity
import com.example.mantec_ins.data.local.ConditionEntity
import com.example.mantec_ins.data.local.DiagnosticEntity
import com.example.mantec_ins.data.local.ElementComponentCrossRef
import com.example.mantec_ins.data.local.ElementEntity
import com.example.mantec_ins.data.local.ElementTypeEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.mantec_ins.data.local.GroupEntity
import com.example.mantec_ins.data.remote.AuthApiService
import com.example.mantec_ins.data.local.MeasurementElementTypeAccessEntity

class RemoteCatalogRepository(
    private val apiService: AuthApiService,
    private val database: AppDatabase
) {

    companion object {
        private val offlineCatalogMutex = Mutex()
    }

    suspend fun syncInitialCatalog(clientId: Long) {
        val areas = apiService.getAreasByClient(clientId).map { dto ->
            AreaEntity(
                id = dto.id,
                clientId = dto.clientId,
                name = dto.name,
                code = dto.code,
                status = dto.status
            )
        }

        database.areaDao().insertAll(areas)

        Log.d(
            "CATALOG_SYNC",
            "syncInitialCatalog() -> áreas: ${areas.size}"
        )
    }

    suspend fun syncElementsByArea(areaId: Long) {
        val elements = apiService.getElementsByArea(areaId).map { dto ->
            ElementEntity(
                id = dto.id,
                areaId = dto.areaId,
                elementTypeId = dto.elementTypeId,
                name = dto.name,
                code = dto.code ?: dto.name,
                warehouseCode = dto.warehouseCode,
                status = dto.status,
                groupId = dto.groupId
            )
        }

        database.elementDao().insertAll(elements)

        Log.d("CATALOG_SYNC", "syncElementsByArea($areaId) -> elementos: ${elements.size}")
    }

    suspend fun syncComponentsByElement(elementId: Long) {
        val components = apiService.getComponentsByElement(elementId).map { dto ->
            ComponentEntity(
                id = dto.id,
                clientId = dto.clientId,
                name = dto.name,
                code = dto.code,
                elementTypeId = dto.elementTypeId,
                isRequired = dto.isRequired,
                isDefault = dto.isDefault,
                status = dto.status
            )
        }

        database.componentDao().insertAll(components)
        database.elementComponentDao().deleteByElementId(elementId)
        database.elementComponentDao().insertAll(
            components.map { component ->
                ElementComponentCrossRef(
                    elementId = elementId,
                    componentId = component.id
                )
            }
        )

        Log.d("CATALOG_SYNC", "syncComponentsByElement($elementId) -> componentes: ${components.size}")
    }

    suspend fun syncOfflineCatalog() {
        offlineCatalogMutex.withLock {
            val startTotal = System.currentTimeMillis()

            Log.d(
                "CATALOG_SYNC",
                "Cargando catálogo offline completo para reportes y mediciones..."
            )

            val startDownload = System.currentTimeMillis()
            val response = apiService.getOfflineCatalog()
            val endDownload = System.currentTimeMillis()

            Log.d(
                "CATALOG_SYNC",
                "offline-catalog descargado en ${endDownload - startDownload} ms"
            )

            if (!response.success) {
                throw IllegalStateException(response.message)
            }

            val startMapping = System.currentTimeMillis()

            val client = ClientEntity(
                id = response.client.id,
                name = response.client.name,
                obs = response.client.obs,
                status = response.client.status
            )

            val group = GroupEntity(
                id = response.group.id,
                clientId = response.group.clientId,
                name = response.group.name,
                description = response.group.description,
                autoSync = response.group.autoSync,
                status = response.group.status
            )

            val elementTypes = response.elementTypes.map { dto ->
                ElementTypeEntity(
                    id = dto.id,
                    clientId = dto.clientId,
                    name = dto.name,
                    description = dto.description,
                    status = dto.status
                )
            }

            val areas = response.areas.map { dto ->
                AreaEntity(
                    id = dto.id,
                    clientId = dto.clientId,
                    name = dto.name,
                    code = dto.code,
                    status = dto.status
                )
            }

            val conditions = response.conditions.map { dto ->
                ConditionEntity(
                    id = dto.id,
                    clientId = dto.clientId,
                    elementTypeId = dto.elementTypeId,
                    name = dto.name,
                    code = dto.code,
                    description = dto.description,
                    severity = dto.severity,
                    color = dto.color,
                    status = dto.status
                )
            }

            val elements = response.elements.map { dto ->
                ElementEntity(
                    id = dto.id,
                    areaId = dto.areaId,
                    elementTypeId = dto.elementTypeId,
                    name = dto.name,
                    code = dto.code ?: dto.name,
                    warehouseCode = dto.warehouseCode,
                    status = dto.status,
                    groupId = dto.groupId
                )
            }

            val components = response.components.map { dto ->
                ComponentEntity(
                    id = dto.id,
                    clientId = dto.clientId,
                    name = dto.name,
                    code = dto.code,
                    elementTypeId = dto.elementTypeId,
                    isRequired = dto.isRequired,
                    isDefault = dto.isDefault,
                    status = dto.status
                )
            }

            val diagnostics = response.diagnostics.map { dto ->
                DiagnosticEntity(
                    id = dto.id,
                    clientId = dto.clientId,
                    elementTypeId = dto.elementTypeId,
                    name = dto.name,
                    description = dto.description,
                    status = dto.status
                )
            }

            val elementComponentRelations = response.elementComponentRelations.map { dto ->
                ElementComponentCrossRef(
                    elementId = dto.elementId,
                    componentId = dto.componentId
                )
            }

            val componentDiagnosticRelations = response.componentDiagnosticRelations.map { dto ->
                ComponentDiagnosticCrossRef(
                    componentId = dto.componentId,
                    diagnosticId = dto.diagnosticId
                )
            }

            val componentConditionRelations = response.componentConditionRelations.map { dto ->
                ComponentConditionCrossRef(
                    componentId = dto.componentId,
                    conditionId = dto.conditionId
                )
            }

            val measurementElementTypes = response.measurementElementTypes.map { dto ->
                MeasurementElementTypeAccessEntity(
                    elementTypeId = dto.elementTypeId,
                    clientId = dto.clientId,
                    name = dto.name,
                    description = dto.description,
                    moduleEnabled = dto.moduleEnabled,
                    creationEnabled = dto.creationEnabled,
                    status = dto.status
                )
            }

            val endMapping = System.currentTimeMillis()

            Log.d(
                "CATALOG_SYNC",
                "offline-catalog recibido -> " +
                        "cliente: ${client.id}, " +
                        "grupo: ${group.id} (${group.name}), " +
                        "autoSync: ${group.autoSync}, " +
                        "tipos: ${elementTypes.size}, " +
                        "áreas: ${areas.size}, " +
                        "condiciones: ${conditions.size}, " +
                        "elementos: ${elements.size}, " +
                        "componentes: ${components.size}, " +
                        "diagnósticos: ${diagnostics.size}, " +
                        "rel element-component: ${elementComponentRelations.size}, " +
                        "rel component-diagnostic: ${componentDiagnosticRelations.size}, " +
                        "rel component-condition: ${componentConditionRelations.size}, " +
                        "tipos mediciones: ${measurementElementTypes.size}"
            )

            val startRoomWrite = System.currentTimeMillis()

            database.withTransaction {
                database.clientDao().deleteAll()
                database.groupDao().deleteAll()
                database.elementTypeDao().deleteAll()
                database.areaDao().deleteAll()
                database.conditionDao().deleteAll()
                database.elementDao().deleteAll()
                database.componentDao().deleteAll()
                database.diagnosticDao().deleteAll()
                database.elementComponentDao().deleteAll()
                database.componentDiagnosticDao().deleteAll()
                database.componentConditionDao().deleteAll()
                database.measurementElementTypeAccessDao().deleteAll()

                database.clientDao().insertAll(listOf(client))
                database.groupDao().insertAll(listOf(group))
                database.elementTypeDao().insertAll(elementTypes)
                database.areaDao().insertAll(areas)
                database.conditionDao().insertAll(conditions)
                database.elementDao().insertAll(elements)
                database.componentDao().insertAll(components)
                database.diagnosticDao().insertAll(diagnostics)
                database.elementComponentDao().insertAll(elementComponentRelations)
                database.componentDiagnosticDao().insertAll(componentDiagnosticRelations)
                database.componentConditionDao().insertAll(componentConditionRelations)
                database.measurementElementTypeAccessDao().insertAll(measurementElementTypes)
            }

            val endRoomWrite = System.currentTimeMillis()

            Log.d(
                "CATALOG_SYNC",
                "Catálogo offline completo para reportes y mediciones guardado correctamente"
            )

            val startVerification = System.currentTimeMillis()

            val clientsCount = database.clientDao().getAll().size
            val groupsCount = database.groupDao().getAll().size
            val elementTypesCount = database.elementTypeDao().getAll().size
            val areasCount = database.areaDao().getAll().size
            val conditionsCount = database.conditionDao().getAll().size
            val elementsCount = database.elementDao().getAll().size
            val componentsCount = database.componentDao().getAll().size
            val diagnosticsCount = database.diagnosticDao().getAll().size

            val endVerification = System.currentTimeMillis()
            val endTotal = System.currentTimeMillis()

            Log.d(
                "CATALOG_SYNC",
                "Verificación Room -> " +
                        "clientes: $clientsCount, " +
                        "grupos: $groupsCount, " +
                        "tipos: $elementTypesCount, " +
                        "áreas: $areasCount, " +
                        "condiciones: $conditionsCount, " +
                        "elementos: $elementsCount, " +
                        "componentes: $componentsCount, " +
                        "diagnósticos: $diagnosticsCount"
            )

            Log.d(
                "CATALOG_SYNC",
                "Tiempo catálogo offline -> " +
                        "descarga: ${endDownload - startDownload} ms, " +
                        "mapeo: ${endMapping - startMapping} ms, " +
                        "guardado Room: ${endRoomWrite - startRoomWrite} ms, " +
                        "verificación: ${endVerification - startVerification} ms, " +
                        "total: ${endTotal - startTotal} ms"
            )

            Log.d(
                "CATALOG_SYNC",
                "Tiempo catálogo offline total: %.2f segundos".format(
                    (endTotal - startTotal) / 1000.0
                )
            )
        }
    }

    suspend fun syncDiagnosticsByComponent(
        componentId: Long,
        elementId: Long
    ) {
        val diagnostics = apiService.getDiagnosticsByComponent(
            componentId = componentId,
            elementId = elementId
        ).map { dto ->
            DiagnosticEntity(
                id = dto.id,
                clientId = dto.clientId,
                elementTypeId = dto.elementTypeId,
                name = dto.name,
                description = dto.description,
                status = dto.status
            )
        }

        database.diagnosticDao().insertAll(diagnostics)
        database.componentDiagnosticDao().deleteByComponentId(componentId)
        database.componentDiagnosticDao().insertAll(
            diagnostics.map { diagnostic ->
                ComponentDiagnosticCrossRef(
                    componentId = componentId,
                    diagnosticId = diagnostic.id
                )
            }
        )

        Log.d("CATALOG_SYNC", "syncDiagnosticsByComponent($componentId) -> diagnósticos: ${diagnostics.size}")
    }

    suspend fun getConditionsByElementAndComponent(
        elementId: Long,
        componentId: Long
    ): List<ConditionEntity> {
        return apiService.getConditionsByElement(
            elementId = elementId,
            componentId = componentId
        ).map { dto ->
            ConditionEntity(
                id = dto.id,
                clientId = dto.clientId,
                elementTypeId = dto.elementTypeId,
                name = dto.name,
                code = dto.code,
                description = dto.description,
                severity = dto.severity,
                color = dto.color,
                status = dto.status
            )
        }
    }
}
package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class OfflineCatalogResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("client")
    val client: OfflineClientDto,

    @SerializedName("group")
    val group: RemoteGroupDto,

    @SerializedName("element_types")
    val elementTypes: List<OfflineElementTypeDto>,

    @SerializedName("areas")
    val areas: List<RemoteAreaDto>,

    @SerializedName("conditions")
    val conditions: List<RemoteConditionDto>,

    @SerializedName("elements")
    val elements: List<RemoteElementDto>,

    @SerializedName("components")
    val components: List<RemoteComponentDto>,

    @SerializedName("diagnostics")
    val diagnostics: List<RemoteDiagnosticDto>,

    @SerializedName("element_component_relations")
    val elementComponentRelations: List<RemoteElementComponentRelationDto>,

    @SerializedName("component_diagnostic_relations")
    val componentDiagnosticRelations: List<RemoteComponentDiagnosticRelationDto>,

    @SerializedName("component_condition_relations")
    val componentConditionRelations: List<RemoteComponentConditionRelationDto>,

    @SerializedName("measurement_element_types")
    val measurementElementTypes: List<RemoteMeasurementElementTypeAccessDto> = emptyList()
)

data class RemoteMeasurementElementTypeAccessDto(
    @SerializedName("element_type_id")
    val elementTypeId: Long,

    @SerializedName("client_id")
    val clientId: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("module_enabled")
    val moduleEnabled: Boolean,

    @SerializedName("creation_enabled")
    val creationEnabled: Boolean,

    @SerializedName("status")
    val status: Boolean
)
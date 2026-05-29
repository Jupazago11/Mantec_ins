package com.example.mantec_ins.data.remote

import com.google.gson.annotations.SerializedName

data class MeasurementElementTypesResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("client")
    val client: MeasurementSimpleClientDto?,

    @SerializedName("group")
    val group: MeasurementSimpleGroupDto?,

    @SerializedName("element_types")
    val elementTypes: List<MeasurementElementTypeDto>
)

data class MeasurementSimpleClientDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String
)

data class MeasurementSimpleGroupDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String
)

data class MeasurementElementTypeDto(
    @SerializedName("element_type_id")
    val elementTypeId: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("module_enabled")
    val moduleEnabled: Boolean,

    @SerializedName("creation_enabled")
    val creationEnabled: Boolean
)

data class MeasurementAreasResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("element_type")
    val elementType: MeasurementSimpleElementTypeDto?,

    @SerializedName("areas")
    val areas: List<MeasurementAreaDto>
)

data class MeasurementSimpleElementTypeDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String
)

data class MeasurementAreaDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("code")
    val code: String?
)

data class MeasurementElementsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("area")
    val area: MeasurementAreaDto?,

    @SerializedName("element_type")
    val elementType: MeasurementSimpleElementTypeDto?,

    @SerializedName("elements")
    val elements: List<MeasurementElementDto>
)

data class MeasurementElementDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("code")
    val code: String?,

    @SerializedName("warehouse_code")
    val warehouseCode: String?,

    @SerializedName("area_id")
    val areaId: Long,

    @SerializedName("element_type_id")
    val elementTypeId: Long
)

data class MeasurementThicknessStateResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("element")
    val element: MeasurementThicknessElementDetailDto,

    @SerializedName("draft")
    val draft: MeasurementThicknessDraftDto?,

    @SerializedName("latest_report")
    val latestReport: MeasurementThicknessReportDto?,

    @SerializedName("historical_reports")
    val historicalReports: List<MeasurementThicknessReportDto>
)

data class MeasurementThicknessElementDetailDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("code")
    val code: String?,

    @SerializedName("warehouse_code")
    val warehouseCode: String?,

    @SerializedName("area")
    val area: MeasurementNestedNameDto?,

    @SerializedName("element_type")
    val elementType: MeasurementNestedNameDto?,

    @SerializedName("group")
    val group: MeasurementNestedNameDto?
)

data class MeasurementNestedNameDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String
)

data class MeasurementThicknessDraftDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("element_id")
    val elementId: Long,

    @SerializedName("created_by")
    val createdBy: Long?,

    @SerializedName("updated_by")
    val updatedBy: Long?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?,

    @SerializedName("lines")
    val lines: List<MeasurementThicknessLineDto>
)

data class MeasurementThicknessReportDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("element_id")
    val elementId: Long,

    @SerializedName("report_date")
    val reportDate: String?,

    @SerializedName("published_at")
    val publishedAt: String?,

    @SerializedName("notes")
    val notes: String?,

    @SerializedName("lines")
    val lines: List<MeasurementThicknessLineDto>
)

data class MeasurementThicknessLineDto(
    @SerializedName("id")
    val id: Long?,

    @SerializedName("cover_number")
    val coverNumber: Int,

    @SerializedName("top_left")
    val topLeft: Double?,

    @SerializedName("top_center")
    val topCenter: Double?,

    @SerializedName("top_right")
    val topRight: Double?,

    @SerializedName("bottom_left")
    val bottomLeft: Double?,

    @SerializedName("bottom_center")
    val bottomCenter: Double?,

    @SerializedName("bottom_right")
    val bottomRight: Double?,

    @SerializedName("hardness_left")
    val hardnessLeft: Double?,

    @SerializedName("hardness_center")
    val hardnessCenter: Double?,

    @SerializedName("hardness_right")
    val hardnessRight: Double?
)

data class MeasurementThicknessDraftSyncRequest(
    @SerializedName("last_known_updated_at")
    val lastKnownUpdatedAt: String?,

    @SerializedName("lines")
    val lines: List<MeasurementThicknessDraftLineRequest>
)

data class MeasurementThicknessDraftLineRequest(
    @SerializedName("cover_number")
    val coverNumber: Int,

    @SerializedName("top_left")
    val topLeft: Double?,

    @SerializedName("top_center")
    val topCenter: Double?,

    @SerializedName("top_right")
    val topRight: Double?,

    @SerializedName("bottom_left")
    val bottomLeft: Double?,

    @SerializedName("bottom_center")
    val bottomCenter: Double?,

    @SerializedName("bottom_right")
    val bottomRight: Double?,

    @SerializedName("hardness_left")
    val hardnessLeft: Double?,

    @SerializedName("hardness_center")
    val hardnessCenter: Double?,

    @SerializedName("hardness_right")
    val hardnessRight: Double?
)

data class MeasurementThicknessDraftSyncResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("conflict")
    val conflict: Boolean?,

    @SerializedName("draft")
    val draft: MeasurementThicknessDraftDto?
)
package com.example.mantec_ins.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mantec_ins.data.local.*
import com.example.mantec_ins.presentation.viewmodel.InspectionUiState
import java.util.Calendar
import java.util.GregorianCalendar
import androidx.compose.foundation.clickable
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mantec_ins.presentation.viewmodel.WeeklyElementStatusItemUi
import androidx.compose.ui.unit.Dp
import com.example.mantec_ins.presentation.viewmodel.PendingDiagnosticItemUi
import com.example.mantec_ins.presentation.viewmodel.LocalPendingDiagnosticItemUi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import com.example.mantec_ins.presentation.viewmodel.MediaEvidenceUi



private val ReportBg = Color(0xFFF8F4EE)
private val CardBg = Color(0xFFFFFBF8)
private val BorderSoft = Color(0xFFE5E7EB)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)
private val MantecOrange = Color(0xFFD94D33)
private val SuccessBg = Color(0xFFF0FDF4)
private val DoneGreen = Color(0xFF16A34A)
private val PendingOrange = Color(0xFFF97316)
private val SuccessBorder = Color(0xFFBBF7D0)
private val SuccessText = Color(0xFF15803D)

private fun conditionDisplayName(condition: ConditionEntity): String {
    val description = condition.description
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    return if (description != null) {
        "${condition.name} - $description"
    } else {
        condition.name
    }
}

@Composable
fun ReportFormScreen(
    clientName: String,
    groupName: String,
    availableElementTypes: List<ElementTypeEntity>,
    areas: List<AreaEntity>,
    elements: List<ElementEntity>,
    components: List<ComponentEntity>,
    diagnostics: List<DiagnosticEntity>,
    pendingDiagnosticItems: List<PendingDiagnosticItemUi>,
    weeklyElementStatuses: List<WeeklyElementStatusItemUi>,
    localPendingDiagnosticItems: List<LocalPendingDiagnosticItemUi>,
    conditions: List<ConditionEntity>,
    selectedAreaId: Long?,
    selectedElementTypeId: Long?,
    selectedElementId: Long?,
    selectedComponentId: Long?,
    inspectionUiState: InspectionUiState,
    onAreaSelected: (Long) -> Unit,
    onElementTypeSelected: (Long) -> Unit,
    onElementSelected: (Long) -> Unit,
    onComponentSelected: (Long) -> Unit,
    onDiagnosticSelected: (Long) -> Unit,
    onConditionSelected: (Long) -> Unit,
    onRecommendationChange: (String) -> Unit,
    onBeltChangeSelected: (Boolean) -> Unit,
    onTakePhotoClick: () -> Unit,
    onRecordVideoClick: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemoveEvidenceClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {

    val selectedElement = elements.firstOrNull { it.id == selectedElementId }

    val areaById = remember(areas) {
        areas.associateBy { it.id }
    }

    val typeById = remember(availableElementTypes) {
        availableElementTypes.associateBy { it.id }
    }

    val groupAreaIds = remember(elements) {
        elements.map { it.areaId }.distinct().toSet()
    }

    val areaOptions = remember(areas, groupAreaIds) {
        areas
            .filter { it.id in groupAreaIds }
            .sortedBy { it.name.trim().lowercase() }
            .map { it.id to it.name }
    }

    val groupElementTypeIds = remember(elements) {
        elements.map { it.elementTypeId }.distinct().toSet()
    }

    val groupElementTypes = remember(availableElementTypes, groupElementTypeIds) {
        availableElementTypes
            .filter { it.id in groupElementTypeIds }
            .sortedBy { it.name.trim().lowercase() }
    }

    val shouldShowElementTypeSelect = groupElementTypes.size >= 2

    val availableTypesForSelectedArea = remember(
        selectedAreaId,
        elements,
        groupElementTypes
    ) {
        if (selectedAreaId == null) {
            emptyList()
        } else {
            val typeIdsForArea = elements
                .filter { it.areaId == selectedAreaId }
                .map { it.elementTypeId }
                .distinct()
                .toSet()

            groupElementTypes.filter { it.id in typeIdsForArea }
        }
    }

    val effectiveElementTypeId = when {
        shouldShowElementTypeSelect -> selectedElementTypeId
        groupElementTypes.size == 1 -> groupElementTypes.first().id
        else -> selectedElementTypeId
    }

    val filteredElements = remember(
        elements,
        selectedAreaId,
        effectiveElementTypeId
    ) {
        elements
            .filter { element ->
                val areaOk = selectedAreaId == null || element.areaId == selectedAreaId
                val typeOk = effectiveElementTypeId == null || element.elementTypeId == effectiveElementTypeId
                areaOk && typeOk
            }
            .sortedBy { it.name.trim().lowercase() }
    }

    val selectedAreaName =
        selectedAreaId?.let { areaById[it]?.name } ?: ""

    val selectedElementTypeName =
        effectiveElementTypeId?.let { typeById[it]?.name } ?: ""

    val selectedElementName = selectedElement?.name ?: ""
    val selectedComponentName = components.firstOrNull { it.id == selectedComponentId }?.name ?: ""

    LaunchedEffect(groupElementTypes) {
        if (groupElementTypes.size == 1 && selectedElementTypeId != groupElementTypes.first().id) {
            onElementTypeSelected(groupElementTypes.first().id)
        }
    }


    val selectedDiagnosticName =
        diagnostics.firstOrNull { it.id == inspectionUiState.selectedDiagnosticId }?.name ?: ""

    val localPendingDiagnosticIds = remember(
        localPendingDiagnosticItems,
        selectedElementId,
        selectedComponentId
    ) {
        if (selectedElementId == null || selectedComponentId == null) {
            emptySet()
        } else {
            localPendingDiagnosticItems
                .filter {
                    it.elementId == selectedElementId &&
                            it.componentId == selectedComponentId
                }
                .map { it.diagnosticId }
                .toSet()
        }
    }

    val completedDiagnosticIds = remember(
        pendingDiagnosticItems,
        selectedElementId,
        selectedComponentId
    ) {
        if (selectedElementId == null || selectedComponentId == null) {
            emptySet()
        } else {
            pendingDiagnosticItems
                .filter {
                    it.elementId == selectedElementId &&
                            it.componentId == selectedComponentId &&
                            it.status.trim().uppercase() == "DONE"
                }
                .map { it.diagnosticId }
                .toSet()
        }
    }

    val localPendingComponentIds = remember(
        pendingDiagnosticItems,
        localPendingDiagnosticItems,
        selectedElementId,
        components
    ) {
        if (selectedElementId == null) {
            emptySet()
        } else {
            components.mapNotNull { component ->
                val expectedItems = pendingDiagnosticItems.filter {
                    it.elementId == selectedElementId &&
                            it.componentId == component.id
                }

                if (expectedItems.isEmpty()) {
                    null
                } else {
                    val allCoveredByServerOrLocal = expectedItems.all { item ->
                        val serverDone = item.status.trim().uppercase() == "DONE"

                        val localPending = localPendingDiagnosticItems.any {
                            it.elementId == selectedElementId &&
                                    it.componentId == component.id &&
                                    it.diagnosticId == item.diagnosticId
                        }

                        serverDone || localPending
                    }

                    val hasLocalPending = localPendingDiagnosticItems.any {
                        it.elementId == selectedElementId &&
                                it.componentId == component.id
                    }

                    if (allCoveredByServerOrLocal && hasLocalPending) {
                        component.id
                    } else {
                        null
                    }
                }
            }.toSet()
        }
    }

    val completedComponentIds = remember(
        pendingDiagnosticItems,
        selectedElementId,
        components
    ) {
        if (selectedElementId == null) {
            emptySet()
        } else {
            android.util.Log.d(
                "REPORT_CHULOS",
                "selectedElementId=$selectedElementId components=${components.size} pendingDiagnosticItems=${pendingDiagnosticItems.size}"
            )

            pendingDiagnosticItems.forEach {
                android.util.Log.d(
                    "REPORT_CHULOS",
                    "item elementId=${it.elementId} componentId=${it.componentId} diagnosticId=${it.diagnosticId} status=${it.status}"
                )
            }

            val completed = components.mapNotNull { component ->
                val itemsForComponent = pendingDiagnosticItems.filter {
                    it.elementId == selectedElementId &&
                            it.componentId == component.id
                }

                android.util.Log.d(
                    "REPORT_CHULOS",
                    "componentId=${component.id} component=${component.name} itemsForComponent=${itemsForComponent.size} done=${itemsForComponent.count { it.status == "DONE" }}"
                )

                if (itemsForComponent.isNotEmpty() && itemsForComponent.all { it.status.trim().uppercase() == "DONE" }) {                    component.id
                } else {
                    null
                }
            }.toSet()

            android.util.Log.d(
                "REPORT_CHULOS",
                "completedComponentIds=$completed"
            )

            completed
        }
    }


    val completedElementIds = remember(
        weeklyElementStatuses
    ) {
        weeklyElementStatuses
            .filter { it.status == "DONE" }
            .map { it.elementId }
            .toSet()
    }

    val localPendingElementIds = remember(
        weeklyElementStatuses,
        localPendingDiagnosticItems
    ) {
        val localPendingByElement = localPendingDiagnosticItems
            .groupBy { it.elementId }
            .mapValues { entry ->
                entry.value
                    .map { it.componentId to it.diagnosticId }
                    .distinct()
                    .size
            }

        weeklyElementStatuses.mapNotNull { statusItem ->
            val localPendingCount = localPendingByElement[statusItem.elementId] ?: 0

            val isAlreadyDone = statusItem.status.trim().uppercase() == "DONE"
            val expectedCount = statusItem.expectedCount
            val doneCount = statusItem.doneCount

            val allCoveredByServerOrLocal =
                expectedCount > 0 &&
                        !isAlreadyDone &&
                        localPendingCount > 0 &&
                        doneCount + localPendingCount >= expectedCount

            if (allCoveredByServerOrLocal) {
                statusItem.elementId
            } else {
                null
            }
        }.toSet()
    }


    val selectedConditionName =
        conditions.firstOrNull { it.id == inspectionUiState.selectedConditionId }
            ?.let { conditionDisplayName(it) } ?: ""


    val shouldShowBeltChange =
        selectedComponentName.equals("Banda", true) &&
                selectedDiagnosticName.equals("Estado", true)

    LaunchedEffect(shouldShowBeltChange) {
        if (!shouldShowBeltChange) {
            onBeltChangeSelected(false)
        }
    }



    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        ReportHeaderSection(
            onBackClick = onBackClick
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {

                Text(
                    text = "Nuevo reporte",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                HorizontalDivider(color = BorderSoft)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, BorderSoft)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = DoneGreen,
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            text = "Confirmado",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )

                        PendingSyncBadge()

                        Text(
                            text = "Pendiente por sincronizar",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                ReportReadOnlyField(
                    label = "Cliente",
                    value = clientName
                )

                ReportReadOnlyField(
                    label = "Agrupación",
                    value = groupName.ifBlank { "Sin agrupación asignada" }
                )

                ProgressiveDropdownField(
                    label = "Área",
                    value = selectedAreaName,
                    options = areaOptions,
                    placeholder = "Seleccione un área",
                    visible = true,
                    enabled = areaOptions.isNotEmpty(),
                    onOptionSelected = onAreaSelected
                )

                if (shouldShowElementTypeSelect) {
                    ProgressiveDropdownField(
                        label = "Tipo de activo",
                        value = selectedElementTypeName,
                        options = availableTypesForSelectedArea.map { it.id to it.name },
                        placeholder = "Seleccione un tipo de activo",
                        visible = selectedAreaId != null,
                        enabled = selectedAreaId != null && availableTypesForSelectedArea.isNotEmpty(),
                        onOptionSelected = onElementTypeSelected
                    )
                }

                ElementDropdownField(
                    label = "Activo",
                    value = selectedElementName,
                    elements = filteredElements,
                    areas = areas,
                    elementTypes = availableElementTypes,
                    completedElementIds = completedElementIds,
                    localPendingElementIds = localPendingElementIds,
                    placeholder = "Seleccione un activo",
                    visible = selectedAreaId != null && effectiveElementTypeId != null,
                    enabled = selectedAreaId != null && effectiveElementTypeId != null && filteredElements.isNotEmpty(),
                    onOptionSelected = onElementSelected
                )

                ComponentDropdownField(
                    label = "Componente",
                    value = selectedComponentName,
                    components = components,
                    completedComponentIds = completedComponentIds,
                    localPendingComponentIds = localPendingComponentIds,
                    placeholder = "Seleccione un componente",
                    visible = selectedElementId != null,
                    enabled = selectedElementId != null,
                    onOptionSelected = onComponentSelected
                )



                DiagnosticDropdownField(
                    label = "Diagnóstico",
                    value = selectedDiagnosticName,
                    diagnostics = diagnostics,
                    completedDiagnosticIds = completedDiagnosticIds,
                    localPendingDiagnosticIds = localPendingDiagnosticIds,
                    placeholder = "Seleccione un diagnóstico",
                    visible = selectedComponentId != null,
                    enabled = selectedComponentId != null,
                    onOptionSelected = onDiagnosticSelected
                )


                val showFinalSection =
                    inspectionUiState.selectedDiagnosticId != null || diagnostics.size == 1

                if (showFinalSection) {

                    ProgressiveDropdownField(
                        label = "Condición",
                        value = selectedConditionName,
                        options = conditions.map { it.id to conditionDisplayName(it) },
                        placeholder = "Seleccione una condición",
                        visible = true,
                        enabled = true,
                        onOptionSelected = onConditionSelected
                    )

                    if (shouldShowBeltChange) {
                        BeltChangeField(
                            selectedValue = inspectionUiState.isBeltChange,
                            onValueSelected = onBeltChangeSelected
                        )
                    }

                    ReportTextArea(
                        label = "Recomendación",
                        value = inspectionUiState.recommendation,
                        placeholder = "Describe la acción recomendada",
                        onValueChange = onRecommendationChange
                    )
                    EvidenceSection(
                        evidenceItems = inspectionUiState.evidences,
                        onTakePhotoClick = onTakePhotoClick,
                        onRecordVideoClick = onRecordVideoClick,
                        onPickFromGallery = onPickFromGallery,
                        onRemoveEvidenceClick = onRemoveEvidenceClick
                    )


                    Button(
                        onClick = onSaveClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MantecOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Guardar reporte",
                            modifier = Modifier.padding(vertical = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                inspectionUiState.message?.let {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SuccessBg,
                        border = BorderStroke(1.dp, SuccessBorder)
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(14.dp),
                            color = SuccessText
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun ReportHeaderSection(
    onBackClick: () -> Unit
) {
    val calendar = GregorianCalendar().apply {
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
    }

    val week = calendar.get(Calendar.WEEK_OF_YEAR)
    val year = calendar.get(Calendar.YEAR)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable { onBackClick() }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = CardBg,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Volver atrás",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = "Volver",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Registro de reportes",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Surface(
                modifier = Modifier.padding(start = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = CardBg,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "SEMANA ACTUAL",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "S$week",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportReadOnlyField(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = Color(0xFFF8FAFC),
                disabledBorderColor = BorderSoft,
                disabledTextColor = TextPrimary
            )
        )
    }
}

private data class SelectOptionUi(
    val id: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val isPendingSync: Boolean = false
)

@Composable
private fun PendingSyncBadge() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFFFF7ED),
        border = BorderStroke(1.dp, Color(0xFFFED7AA))
    ) {
        Text(
            text = "P",
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            color = PendingOrange,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

private sealed class ElementPickerRowUi {
    data class TypeHeader(
        val key: String,
        val title: String
    ) : ElementPickerRowUi()

    data class AreaHeader(
        val key: String,
        val title: String
    ) : ElementPickerRowUi()

    data class ElementOption(
        val key: String,
        val id: Long,
        val title: String,
        val isCompleted: Boolean,
        val isPendingSync: Boolean
    ) : ElementPickerRowUi()
}

private fun buildElementPickerRows(
    elements: List<ElementEntity>,
    areas: List<AreaEntity>,
    elementTypes: List<ElementTypeEntity>,
    completedElementIds: Set<Long>,
    localPendingElementIds: Set<Long>
): List<ElementPickerRowUi> {
    val areaById = areas.associateBy { it.id }
    val typeById = elementTypes.associateBy { it.id }

    return elements
        .sortedWith(
            compareBy<ElementEntity>(
                { typeById[it.elementTypeId]?.name?.trim()?.lowercase() ?: "zzzz" },
                { areaById[it.areaId]?.name?.trim()?.lowercase() ?: "zzzz" },
                { it.name.trim().lowercase() }
            )
        )
        .groupBy { it.elementTypeId }
        .flatMap { (elementTypeId, elementsByType) ->
            val typeName = typeById[elementTypeId]?.name ?: "Tipo de activo no definido"

            val typeRows = mutableListOf<ElementPickerRowUi>(
                ElementPickerRowUi.TypeHeader(
                    key = "type_$elementTypeId",
                    title = typeName
                )
            )

            val areaRows = elementsByType
                .groupBy { it.areaId }
                .flatMap { (areaId, elementsByArea) ->
                    val areaName = areaById[areaId]?.name ?: "Área no definida"

                    listOf<ElementPickerRowUi>(
                        ElementPickerRowUi.AreaHeader(
                            key = "area_${elementTypeId}_$areaId",
                            title = areaName
                        )
                    ) + elementsByArea.map { element ->
                        ElementPickerRowUi.ElementOption(
                            key = "element_${element.id}",
                            id = element.id,
                            title = element.name,
                            isCompleted = element.id in completedElementIds,
                            isPendingSync = element.id in localPendingElementIds &&
                                    element.id !in completedElementIds
                        )
                    }
                }

            typeRows + areaRows
        }
}

@Composable
private fun SelectOptionsDialog(
    title: String,
    options: List<SelectOptionUi>,
    onDismiss: () -> Unit,
    onOptionSelected: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            if (options.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, BorderSoft)
                ) {
                    Text(
                        text = "No hay opciones disponibles.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = options,
                        key = { it.id }
                    ) { option ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOptionSelected(option.id)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, BorderSoft)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option.title,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )

                                when {
                                    option.isCompleted -> {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Confirmado por servidor",
                                            tint = DoneGreen
                                        )
                                    }

                                    option.isPendingSync -> {
                                        PendingSyncBadge()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cerrar",
                    color = MantecOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun GroupedElementOptionsDialog(
    title: String,
    elements: List<ElementEntity>,
    areas: List<AreaEntity>,
    elementTypes: List<ElementTypeEntity>,
    completedElementIds: Set<Long>,
    localPendingElementIds: Set<Long>,
    onDismiss: () -> Unit,
    onOptionSelected: (Long) -> Unit
) {
    val rows = remember(
        elements,
        areas,
        elementTypes,
        completedElementIds,
        localPendingElementIds
    ) {
        buildElementPickerRows(
            elements = elements,
            areas = areas,
            elementTypes = elementTypes,
            completedElementIds = completedElementIds,
            localPendingElementIds = localPendingElementIds
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            if (rows.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, BorderSoft)
                ) {
                    Text(
                        text = "No hay activos disponibles.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = rows,
                        key = {
                            when (it) {
                                is ElementPickerRowUi.TypeHeader -> it.key
                                is ElementPickerRowUi.AreaHeader -> it.key
                                is ElementPickerRowUi.ElementOption -> it.key
                            }
                        }
                    ) { row ->
                        when (row) {
                            is ElementPickerRowUi.TypeHeader -> {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFFF7ED),
                                    border = BorderStroke(1.dp, Color(0xFFFED7AA))
                                ) {
                                    Text(
                                        text = row.title,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MantecOrange
                                    )
                                }
                            }

                            is ElementPickerRowUi.AreaHeader -> {
                                Text(
                                    text = row.title,
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }

                            is ElementPickerRowUi.ElementOption -> {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onOptionSelected(row.id)
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, BorderSoft)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = row.title,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )

                                        when {
                                            row.isCompleted -> {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Activo confirmado por servidor",
                                                    tint = DoneGreen
                                                )
                                            }

                                            row.isPendingSync -> {
                                                PendingSyncBadge()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cerrar",
                    color = MantecOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun ProgressiveDropdownField(
    label: String,
    value: String,
    options: List<Pair<Long, String>>,
    placeholder: String,
    visible: Boolean,
    enabled: Boolean,
    onOptionSelected: (Long) -> Unit
) {
    if (!visible) return

    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) {
                        expanded = true
                    },
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (value.isNotBlank()) value else placeholder,
                        color = if (value.isNotBlank()) TextPrimary else TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "▼",
                        color = TextSecondary
                    )
                }
            }

            if (expanded && enabled) {
                SelectOptionsDialog(
                    title = label,
                    options = options.map { (id, text) ->
                        SelectOptionUi(
                            id = id,
                            title = text
                        )
                    },
                    onDismiss = {
                        expanded = false
                    },
                    onOptionSelected = { id ->
                        expanded = false
                        onOptionSelected(id)
                    }
                )
            }
        }
    }
}

@Composable
private fun DiagnosticDropdownField(
    label: String,
    value: String,
    diagnostics: List<DiagnosticEntity>,
    completedDiagnosticIds: Set<Long>,
    localPendingDiagnosticIds: Set<Long>,
    placeholder: String,
    visible: Boolean,
    enabled: Boolean,
    onOptionSelected: (Long) -> Unit
) {
    if (!visible) return

    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) {
                        expanded = true
                    },
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (value.isNotBlank()) value else placeholder,
                        color = if (value.isNotBlank()) TextPrimary else TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "▼",
                        color = TextSecondary
                    )
                }
            }

            if (expanded && enabled) {
                SelectOptionsDialog(
                    title = label,
                    options = diagnostics.map { diagnostic ->
                        SelectOptionUi(
                            id = diagnostic.id,
                            title = diagnostic.name,
                            isCompleted = diagnostic.id in completedDiagnosticIds,
                            isPendingSync = diagnostic.id in localPendingDiagnosticIds &&
                                    diagnostic.id !in completedDiagnosticIds
                        )
                    },
                    onDismiss = {
                        expanded = false
                    },
                    onOptionSelected = { id ->
                        expanded = false
                        onOptionSelected(id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ComponentDropdownField(
    label: String,
    value: String,
    components: List<ComponentEntity>,
    completedComponentIds: Set<Long>,
    localPendingComponentIds: Set<Long>,
    placeholder: String,
    visible: Boolean,
    enabled: Boolean,
    onOptionSelected: (Long) -> Unit
) {
    if (!visible) return

    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) {
                        expanded = true
                    },
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (value.isNotBlank()) value else placeholder,
                        color = if (value.isNotBlank()) TextPrimary else TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "▼",
                        color = TextSecondary
                    )
                }
            }

            if (expanded && enabled) {
                SelectOptionsDialog(
                    title = label,
                    options = components.map { component ->
                        SelectOptionUi(
                            id = component.id,
                            title = component.name,
                            isCompleted = component.id in completedComponentIds,
                            isPendingSync = component.id in localPendingComponentIds &&
                                    component.id !in completedComponentIds
                        )
                    },
                    onDismiss = {
                        expanded = false
                    },
                    onOptionSelected = { id ->
                        expanded = false
                        onOptionSelected(id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ElementDropdownField(
    label: String,
    value: String,
    elements: List<ElementEntity>,
    areas: List<AreaEntity>,
    elementTypes: List<ElementTypeEntity>,
    completedElementIds: Set<Long>,
    localPendingElementIds: Set<Long>,
    placeholder: String,
    visible: Boolean,
    enabled: Boolean,
    onOptionSelected: (Long) -> Unit
) {
    if (!visible) return

    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) {
                        expanded = true
                    },
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (value.isNotBlank()) value else placeholder,
                        color = if (value.isNotBlank()) TextPrimary else TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "▼",
                        color = TextSecondary
                    )
                }
            }

            if (expanded && enabled) {
                GroupedElementOptionsDialog(
                    title = label,
                    elements = elements,
                    areas = areas,
                    elementTypes = elementTypes,
                    completedElementIds = completedElementIds,
                    localPendingElementIds = localPendingElementIds,
                    onDismiss = {
                        expanded = false
                    },
                    onOptionSelected = { id ->
                        expanded = false
                        onOptionSelected(id)
                    }
                )
            }
        }
    }
}

@Composable
private fun BeltChangeField(
    selectedValue: Boolean,
    onValueSelected: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "¿Cambio de banda?",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, BorderSoft)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row {
                    RadioButton(
                        selected = selectedValue,
                        onClick = { onValueSelected(true) }
                    )
                    Text(
                        text = "Sí",
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Row {
                    RadioButton(
                        selected = !selectedValue,
                        onClick = { onValueSelected(false) }
                    )
                    Text(
                        text = "No",
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportTextArea(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = TextSecondary
                )
            },
            minLines = 4,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MantecOrange,
                unfocusedBorderColor = BorderSoft,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = MantecOrange
            )
        )
    }
}



@Composable
private fun EvidenceSection(
    evidenceItems: List<MediaEvidenceUi>,
    onTakePhotoClick: () -> Unit,
    onRecordVideoClick: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemoveEvidenceClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Evidencias",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onTakePhotoClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tomar foto")
            }

            Button(
                onClick = onRecordVideoClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Grabar video")
            }
        }

        OutlinedButton(
            onClick = onPickFromGallery,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MantecOrange),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MantecOrange)
        ) {
            Text("Galería")
        }

        if (evidenceItems.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Text(
                    text = "Aún no has agregado evidencias.",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                evidenceItems.forEach { evidence ->
                    EvidenceItemRow(
                        evidence = evidence,
                        onRemoveEvidenceClick = onRemoveEvidenceClick
                    )
                }
            }
        }
    }
}

@Composable
private fun EvidenceItemRow(
    evidence: MediaEvidenceUi,
    onRemoveEvidenceClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EvidenceThumbnail(
                    path = evidence.path,
                    type = evidence.type
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (evidence.type == "image") "Imagen" else "Video",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = evidence.path,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = "Quitar",
                modifier = Modifier.clickable {
                    onRemoveEvidenceClick(evidence.path)
                },
                color = MantecOrange,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EvidenceThumbnail(
    path: String,
    type: String
) {
    val context = LocalContext.current

    val bitmap = remember(path, type) {
        loadEvidenceThumbnail(
            context = context,
            path = path,
            type = type
        )
    }

    Surface(
        modifier = Modifier.size(60.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF3F4F6),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (type == "image") "IMG" else "VID",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun loadEvidenceThumbnail(
    context: Context,
    path: String,
    type: String
): Bitmap? {
    return try {
        val uri = Uri.parse(path)

        if (type == "image") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSampleSize(4)
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } else {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(0)
            retriever.release()
            frame
        }
    } catch (_: Exception) {
        null
    }
}

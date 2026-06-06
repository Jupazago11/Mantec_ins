package com.example.mantec_ins.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mantec_ins.data.local.AreaEntity
import com.example.mantec_ins.data.local.ComponentEntity
import com.example.mantec_ins.data.local.ConditionEntity
import com.example.mantec_ins.data.local.DiagnosticEntity
import com.example.mantec_ins.data.local.ElementEntity
import com.example.mantec_ins.data.local.ReportEntity
import com.example.mantec_ins.data.local.ElementTypeEntity

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.example.mantec_ins.presentation.viewmodel.DashboardUiState
import com.example.mantec_ins.presentation.viewmodel.InspectionUiState
import com.example.mantec_ins.presentation.viewmodel.ReportDetailUiState
import com.example.mantec_ins.presentation.viewmodel.PendingDiagnosticItemUi
import com.example.mantec_ins.presentation.viewmodel.LocalPendingDiagnosticItemUi


@Composable
fun MainScreenHost(
    clientName: String,
    groupName: String,
    availableElementTypes: List<ElementTypeEntity>,
    areas: List<AreaEntity>,
    elements: List<ElementEntity>,
    components: List<ComponentEntity>,
    diagnostics: List<DiagnosticEntity>,
    conditions: List<ConditionEntity>,
    selectedCatalogElementId: Long?,
    selectedCatalogComponentId: Long?,
    inspectionUiState: InspectionUiState,
    reports: List<ReportEntity>,
    reportDetailUiState: ReportDetailUiState,
    dashboardUiState: DashboardUiState,
    selectedAreaId: Long?,
    selectedElementTypeId: Long?,
    localPendingDiagnosticItems: List<LocalPendingDiagnosticItemUi>,
    onAreaClick: (Long) -> Unit,
    onElementTypeClick: (Long) -> Unit,
    onElementClick: (Long) -> Unit,
    onComponentClick: (Long) -> Unit,
    onDiagnosticClick: (Long) -> Unit,
    onConditionClick: (Long) -> Unit,
    onRecommendationChange: (String) -> Unit,
    onBeltChangeSelected: (Boolean) -> Unit,
    onVoiceInputClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRecordVideoClick: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemoveEvidenceClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onReportClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F4EE))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {


        ReportFormScreen(
            clientName = clientName,
            groupName = groupName,

            availableElementTypes = availableElementTypes,

            areas = areas,
            elements = elements,
            components = components,
            diagnostics = diagnostics,
            pendingDiagnosticItems = dashboardUiState.weeklyDiagnosticItems,
            weeklyElementStatuses = dashboardUiState.weeklyElementStatuses,
            localPendingDiagnosticItems = localPendingDiagnosticItems,

            selectedAreaId = selectedAreaId,
            selectedElementTypeId = selectedElementTypeId,
            onAreaSelected = onAreaClick,
            onElementTypeSelected = onElementTypeClick,
            conditions = conditions,
            selectedElementId = selectedCatalogElementId,
            selectedComponentId = selectedCatalogComponentId,
            inspectionUiState = inspectionUiState,
            onElementSelected = onElementClick,
            onComponentSelected = onComponentClick,
            onDiagnosticSelected = onDiagnosticClick,
            onConditionSelected = onConditionClick,
            onRecommendationChange = onRecommendationChange,
            onBeltChangeSelected = onBeltChangeSelected,
            onVoiceInputClick = onVoiceInputClick,
            onTakePhotoClick = onTakePhotoClick,
            onRecordVideoClick = onRecordVideoClick,
            onPickFromGallery = onPickFromGallery,
            onRemoveEvidenceClick = onRemoveEvidenceClick,
            onBackClick = onBackClick,
            onSaveClick = onSaveClick
        )

        if (selectedCatalogElementId != null) {
            HorizontalDivider()

            PendingDiagnosticsSection(
                dashboardUiState = dashboardUiState
            )
        }
    }
}

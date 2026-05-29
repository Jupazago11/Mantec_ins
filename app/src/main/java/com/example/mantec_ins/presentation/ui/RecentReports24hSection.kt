package com.example.mantec_ins.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.mantec_ins.presentation.viewmodel.DashboardUiState

@Composable
fun RecentReports24hSection(
    dashboardUiState: DashboardUiState
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("REPORTES ÚLTIMAS 24H")

        if (dashboardUiState.recentItems.isEmpty()) {
            Text("Sin reportes recientes")
        } else {
            dashboardUiState.recentItems.forEach { item ->
                Text("${item.elementName} · ${item.componentName} · ${item.diagnosticName}")
                Text("Condición: ${item.conditionName}")
                Text("Fecha: ${item.executionDate}")
                Text("Estado reporte: ${item.reportStatus}")
                Text("Estado detalle: ${item.detailSyncStatus}")
                HorizontalDivider()
            }
        }
    }
}
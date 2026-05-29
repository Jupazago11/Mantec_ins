package com.example.mantec_ins.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mantec_ins.presentation.viewmodel.DashboardUiState

private val PendingCardBg = Color(0xFFFFFBF8)
private val PendingBorder = Color(0xFFE5E7EB)
private val PendingPrimary = Color(0xFF111827)
private val PendingSecondary = Color(0xFF6B7280)
private val PendingBadgeBg = Color(0xFFFFF7ED)
private val PendingBadgeText = Color(0xFFC2410C)
private val PendingEmptyBg = Color(0xFFF8FAFC)

@Composable
fun PendingDiagnosticsSection(
    dashboardUiState: DashboardUiState
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = PendingCardBg,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Pendientes de la semana",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PendingPrimary
                    )

                    Text(
                        text = if (dashboardUiState.pendingItems.isEmpty()) {
                            "No hay pendientes o selecciona un activo"
                        } else {
                            "${dashboardUiState.pendingItems.size} pendientes por registrar"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = PendingSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = PendingBadgeBg
                ) {
                    Text(
                        text = dashboardUiState.pendingItems.size.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PendingBadgeText
                    )
                }
            }

            if (dashboardUiState.pendingItems.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = PendingEmptyBg,
                    border = BorderStroke(1.dp, PendingBorder)
                ) {
                    Text(
                        text = "Cuando selecciones un activo, aquí verás los diagnósticos que aún faltan por registrar en la semana actual.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PendingSecondary
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    dashboardUiState.pendingItems.forEach { item ->
                        PendingDiagnosticCard(
                            componentName = item.componentName,
                            diagnosticName = item.diagnosticName
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun PendingDiagnosticCard(
    componentName: String,
    diagnosticName: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, PendingBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = componentName.ifBlank { "Componente no definido" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PendingPrimary
            )

            Text(
                text = diagnosticName.ifBlank { "Diagnóstico no definido" },
                style = MaterialTheme.typography.bodyMedium,
                color = PendingSecondary
            )
        }
    }
}

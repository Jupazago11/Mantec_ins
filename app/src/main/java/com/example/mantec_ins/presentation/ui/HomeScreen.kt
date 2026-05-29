package com.example.mantec_ins.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mantec_ins.presentation.viewmodel.PendingSyncReportItemUi

private val HomeBg = Color(0xFFF8F4EE)
private val CardBg = Color(0xFFFFFBF8)
private val SoftBorder = Color(0xFFE5E7EB)
private val PrimaryText = Color(0xFF111827)
private val SecondaryText = Color(0xFF6B7280)
private val MantecOrange = Color(0xFFD94D33)

private val PendingBadgeBg = Color(0xFFFFF7ED)
private val PendingBadgeText = Color(0xFFC2410C)
private val SyncedBadgeBg = Color(0xFFF0FDF4)
private val SyncedBadgeText = Color(0xFF15803D)
private val ErrorBadgeBg = Color(0xFFFEF2F2)
private val ErrorBadgeText = Color(0xFFB91C1C)
private val SyncingBadgeBg = Color(0xFFEFF6FF)
private val SyncingBadgeText = Color(0xFF1D4ED8)

@Composable
fun HomeScreen(
    userName: String,
    groupName: String,
    groupAutoSync: Boolean,
    connectionLabel: String,
    pendingSyncItems: List<PendingSyncReportItemUi>,
    pendingMeasurementDraftCount: Int,
    showMeasurementsButton: Boolean,
    syncSuccessMessage: String?,
    syncWarningMessage: String?,
    isManualSyncRunning: Boolean,
    onManualSyncClick: () -> Unit,
    onGoToReport: () -> Unit,
    onGoToMeasurements: () -> Unit,
    onLogout: () -> Unit
) {
    var showSyncConfirmDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = CardBg,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column {
                    Text(
                        text = "ManTec",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryText
                    )
                    Text(
                        text = "Panel de inspector",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SoftBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bienvenido",
                            style = MaterialTheme.typography.labelLarge,
                            color = SecondaryText
                        )
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = "Agrupación: ${groupName.ifBlank { "No definida" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (groupAutoSync) {
                                "Sincronización: automática con cualquier conexión"
                            } else {
                                "Sincronización: manual con botón"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )
                        Text(
                            text = "Conexión actual: $connectionLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (!groupAutoSync) {
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable(enabled = !isManualSyncRunning) {
                                    showSyncConfirmDialog = true
                                },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isManualSyncRunning) Color(0xFF374151) else Color(0xFF111827),
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isManualSyncRunning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Sincronizar",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onGoToReport,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MantecOrange,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Registrar reporte",
                                modifier = Modifier.padding(vertical = 4.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showMeasurementsButton) {
                            Button(
                                onClick = onGoToMeasurements,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MantecOrange,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "Mediciones de espesores",
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        syncSuccessMessage?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SyncedBadgeBg,
                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = SyncedBadgeText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        syncWarningMessage?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = ErrorBadgeBg,
                border = BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ErrorBadgeText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        PendingSyncReportsSection(
            items = pendingSyncItems
        )

        Spacer(modifier = Modifier.height(12.dp))

        PendingMeasurementDraftsSection(
            count = pendingMeasurementDraftCount
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogout() },
            shape = RoundedCornerShape(18.dp),
            color = ErrorBadgeBg,
            border = BorderStroke(1.dp, Color(0xFFFECACA))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ExitToApp,
                    contentDescription = null,
                    tint = ErrorBadgeText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                Text(
                    text = "Cerrar sesión",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ErrorBadgeText
                )
            }
        }

        if (showSyncConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isManualSyncRunning) {
                        showSyncConfirmDialog = false
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = CardBg,
                title = {
                    Text(
                        text = "Confirmar sincronización",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Se enviarán los reportes pendientes al servidor y se actualizará la información local.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryText
                        )

                        Text(
                            text = "Este proceso puede tardar si hay muchos reportes o evidencias. Se recomienda no cerrar la app mientras finaliza la sincronización.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )

                        Text(
                            text = "Conexión actual: $connectionLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSyncConfirmDialog = false
                            onManualSyncClick()
                        },
                        enabled = !isManualSyncRunning,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MantecOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Sincronizar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSyncConfirmDialog = false
                        },
                        enabled = !isManualSyncRunning
                    ) {
                        Text(
                            text = "Cancelar",
                            color = SecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PendingSyncReportsSection(
    items: List<PendingSyncReportItemUi>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Reportes pendientes de sincronización",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    text = "${items.size} pendientes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }

            if (items.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SoftBorder)
                ) {
                    Text(
                        text = "No hay reportes pendientes.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.forEach { item ->
                        PendingSyncReportCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingSyncReportCard(
    item: PendingSyncReportItemUi
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoftBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.elementName.ifBlank { "Activo sin nombre" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = buildComponentDiagnosticText(
                            componentName = item.componentName,
                            diagnosticName = item.diagnosticName
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            StatusBadge(status = item.status)

            Text(
                text = buildSecondaryContextText(
                    areaName = item.areaName,
                    clientName = item.clientName
                ),
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildComponentDiagnosticText(
    componentName: String,
    diagnosticName: String
): String {
    val component = componentName.ifBlank { "Componente no definido" }
    val diagnostic = diagnosticName.ifBlank { "Diagnóstico no definido" }
    return "$component · $diagnostic"
}

private fun buildSecondaryContextText(
    areaName: String,
    clientName: String
): String {
    val area = areaName.ifBlank { "Área no definida" }
    val client = clientName.ifBlank { "Cliente no definido" }
    return "Área: $area · Cliente: $client"
}

@Composable
private fun StatusBadge(
    status: String
) {
    val (bg, fg, label) = when (status.uppercase()) {
        "SYNCED" -> Triple(SyncedBadgeBg, SyncedBadgeText, "SINCRONIZADO")
        "SYNCING" -> Triple(SyncingBadgeBg, SyncingBadgeText, "SINCRONIZANDO")
        "ERROR" -> Triple(ErrorBadgeBg, ErrorBadgeText, "ERROR")
        else -> Triple(PendingBadgeBg, PendingBadgeText, "PENDIENTE")
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun PendingMeasurementDraftsSection(
    count: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Mediciones pendientes de sincronización",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Text(
                text = if (count == 1) {
                    "1 activo con borrador pendiente"
                } else {
                    "$count activos con borrador pendiente"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )

            if (count == 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SoftBorder)
                ) {
                    Text(
                        text = "No hay borradores de medición pendientes.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
            }
        }
    }
}

package com.example.mantec_ins.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mantec_ins.data.local.MeasurementThicknessDraftLineEntity
import com.example.mantec_ins.data.remote.MeasurementAreaDto
import com.example.mantec_ins.data.remote.MeasurementElementDto
import com.example.mantec_ins.data.remote.MeasurementElementTypeDto
import com.example.mantec_ins.presentation.viewmodel.MeasurementThicknessUiState

private val MeasurementBg = Color(0xFFF8F4EE)
private val CardBg = Color(0xFFFFFBF8)
private val BorderSoft = Color(0xFFE5E7EB)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)
private val MantecOrange = Color(0xFFD94D33)
private val SuccessBg = Color(0xFFF0FDF4)
private val SuccessText = Color(0xFF15803D)
private val ErrorBg = Color(0xFFFEF2F2)
private val ErrorText = Color(0xFFB91C1C)
private val PendingOrange = Color(0xFFF97316)

@Composable
fun MeasurementThicknessScreen(
    clientName: String,
    uiState: MeasurementThicknessUiState,
    onBackClick: () -> Unit,
    onElementTypeSelected: (MeasurementElementTypeDto) -> Unit,
    onAreaSelected: (MeasurementAreaDto) -> Unit,
    onElementSelected: (MeasurementElementDto) -> Unit,
    onLineValueChange: (coverNumber: Int, field: String, value: String) -> Unit,
    onAddCoverClick: () -> Unit,
    onRemoveLastCoverClick: () -> Unit,
    onSaveDraftClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeasurementBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MeasurementHeader(
            onBackClick = onBackClick
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Medición de espesores y dureza",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                HorizontalDivider(color = BorderSoft)

                MeasurementReadOnlyField(
                    label = "Cliente",
                    value = clientName
                )

                if (uiState.shouldShowElementTypeSelector) {
                    MeasurementSelectorField(
                        label = "Tipo de activo",
                        value = uiState.selectedElementTypeName,
                        placeholder = "Seleccione tipo de activo",
                        enabled = uiState.elementTypes.isNotEmpty(),
                        options = uiState.elementTypes.map {
                            SelectorOption(
                                id = it.elementTypeId,
                                title = it.name,
                                payload = it
                            )
                        },
                        onOptionSelected = { selected ->
                            onElementTypeSelected(selected as MeasurementElementTypeDto)
                        }
                    )
                } else {
                    if (uiState.selectedElementTypeName.isNotBlank()) {
                        MeasurementReadOnlyField(
                            label = "Tipo de activo",
                            value = uiState.selectedElementTypeName
                        )
                    }
                }

                MeasurementSelectorField(
                    label = "Área",
                    value = uiState.selectedAreaName,
                    placeholder = "Seleccione un área",
                    enabled = uiState.areas.isNotEmpty(),
                    options = uiState.areas.map {
                        SelectorOption(
                            id = it.id,
                            title = it.name,
                            payload = it
                        )
                    },
                    onOptionSelected = { selected ->
                        onAreaSelected(selected as MeasurementAreaDto)
                    }
                )

                MeasurementSelectorField(
                    label = "Activo",
                    value = uiState.selectedElementName,
                    placeholder = "Seleccione un activo",
                    enabled = uiState.elements.isNotEmpty(),
                    options = uiState.elements.map {
                        SelectorOption(
                            id = it.id,
                            title = it.name,
                            payload = it
                        )
                    },
                    onOptionSelected = { selected ->
                        onElementSelected(selected as MeasurementElementDto)
                    }
                )

                uiState.draft?.let { draft ->
                    DraftStatusCard(
                        syncStatus = draft.syncStatus,
                        lastError = draft.lastError
                    )
                }

                if (uiState.selectedElementId != null) {
                    HorizontalDivider(color = BorderSoft)

                    ThicknessLinesSection(
                        lines = uiState.lines,
                        onLineValueChange = onLineValueChange
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onAddCoverClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Agregar cubierta")
                        }

                        Button(
                            onClick = onRemoveLastCoverClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF374151),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Eliminar última")
                        }
                    }

                    Button(
                        onClick = onSaveDraftClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSavingLocal && !uiState.isSyncing,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MantecOrange,
                            contentColor = Color.White
                        )
                    ) {
                        if (uiState.isSavingLocal || uiState.isSyncing) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )

                                Text(
                                    text = "Guardando...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "Guardar borrador",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                uiState.message?.let {
                    InfoMessage(
                        text = it,
                        success = true
                    )
                }

                uiState.errorMessage?.let {
                    InfoMessage(
                        text = it,
                        success = false
                    )
                }

                if (uiState.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementHeader(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBackClick() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CardBg,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, BorderSoft)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = TextPrimary,
                modifier = Modifier.padding(10.dp)
            )
        }

        Text(
            text = "Volver",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MeasurementReadOnlyField(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
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

private data class SelectorOption(
    val id: Long,
    val title: String,
    val payload: Any
)

@Composable
private fun MeasurementSelectorField(
    label: String,
    value: String,
    placeholder: String,
    enabled: Boolean,
    options: List<SelectorOption>,
    onOptionSelected: (Any) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )

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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (value.isNotBlank()) value else placeholder,
                    color = if (value.isNotBlank()) TextPrimary else TextSecondary,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "▼",
                    color = TextSecondary
                )
            }
        }

        if (expanded) {
            AlertDialog(
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = CardBg,
                title = {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(options, key = { it.id }) { option ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expanded = false
                                        onOptionSelected(option.payload)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BorderSoft)
                            ) {
                                Text(
                                    text = option.title,
                                    modifier = Modifier.padding(14.dp),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { expanded = false }) {
                        Text(
                            text = "Cerrar",
                            color = MantecOrange
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun DraftStatusCard(
    syncStatus: String,
    lastError: String?
) {
    val normalized = syncStatus.uppercase()

    val (bg, fg, label) = when (normalized) {
        "SYNCED" -> Triple(SuccessBg, SuccessText, "Borrador sincronizado")
        "PENDING_SYNC" -> Triple(Color(0xFFFFF7ED), PendingOrange, "Pendiente por sincronizar")
        "CONFLICT" -> Triple(ErrorBg, ErrorText, "Conflicto con borrador remoto")
        "ERROR" -> Triple(ErrorBg, ErrorText, "Error de sincronización")
        else -> Triple(Color(0xFFF8FAFC), TextSecondary, syncStatus)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                color = fg,
                fontWeight = FontWeight.Bold
            )

            if (!lastError.isNullOrBlank()) {
                Text(
                    text = lastError,
                    color = fg,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ThicknessLinesSection(
    lines: List<MeasurementThicknessDraftLineEntity>,
    onLineValueChange: (coverNumber: Int, field: String, value: String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Cubiertas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        lines.sortedBy { it.coverNumber }.forEach { line ->
            ThicknessLineCard(
                line = line,
                onLineValueChange = onLineValueChange
            )
        }
    }
}

@Composable
private fun ThicknessLineCard(
    line: MeasurementThicknessDraftLineEntity,
    onLineValueChange: (coverNumber: Int, field: String, value: String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Cubierta ${line.coverNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            MeasurementGroupTitle("Cubierta superior")
            ThreeNumberFields(
                left = line.topLeft,
                center = line.topCenter,
                right = line.topRight,
                leftField = "topLeft",
                centerField = "topCenter",
                rightField = "topRight",
                coverNumber = line.coverNumber,
                onLineValueChange = onLineValueChange
            )

            MeasurementGroupTitle("Cubierta inferior")
            ThreeNumberFields(
                left = line.bottomLeft,
                center = line.bottomCenter,
                right = line.bottomRight,
                leftField = "bottomLeft",
                centerField = "bottomCenter",
                rightField = "bottomRight",
                coverNumber = line.coverNumber,
                onLineValueChange = onLineValueChange
            )

            MeasurementGroupTitle("Dureza")
            ThreeNumberFields(
                left = line.hardnessLeft,
                center = line.hardnessCenter,
                right = line.hardnessRight,
                leftField = "hardnessLeft",
                centerField = "hardnessCenter",
                rightField = "hardnessRight",
                coverNumber = line.coverNumber,
                onLineValueChange = onLineValueChange
            )
        }
    }
}

@Composable
private fun MeasurementGroupTitle(
    text: String
) {
    Text(
        text = text,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ThreeNumberFields(
    left: Double?,
    center: Double?,
    right: Double?,
    leftField: String,
    centerField: String,
    rightField: String,
    coverNumber: Int,
    onLineValueChange: (coverNumber: Int, field: String, value: String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NumberField(
            label = "Izq.",
            value = left,
            modifier = Modifier.weight(1f),
            onValueChange = {
                onLineValueChange(coverNumber, leftField, it)
            }
        )

        NumberField(
            label = "Centro",
            value = center,
            modifier = Modifier.weight(1f),
            onValueChange = {
                onLineValueChange(coverNumber, centerField, it)
            }
        )

        NumberField(
            label = "Der.",
            value = right,
            modifier = Modifier.weight(1f),
            onValueChange = {
                onLineValueChange(coverNumber, rightField, it)
            }
        )
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Double?,
    modifier: Modifier,
    onValueChange: (String) -> Unit
) {
    var text by remember(value) {
        mutableStateOf(value?.toString() ?: "")
    }

    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            val sanitized = newValue
                .replace(",", ".")
                .filter { it.isDigit() || it == '.' }

            text = sanitized
            onValueChange(sanitized)
        },
        modifier = modifier,
        label = {
            Text(label)
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MantecOrange,
            unfocusedBorderColor = BorderSoft,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = MantecOrange
        )
    )
}

@Composable
private fun InfoMessage(
    text: String,
    success: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (success) SuccessBg else ErrorBg,
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            color = if (success) SuccessText else ErrorText
        )
    }
}
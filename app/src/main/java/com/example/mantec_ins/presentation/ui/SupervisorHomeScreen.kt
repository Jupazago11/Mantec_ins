package com.example.mantec_ins.presentation.ui

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.mantec_ins.presentation.viewmodel.DiaActividad
import com.example.mantec_ins.presentation.viewmodel.SupervisorActivityUi
import com.example.mantec_ins.presentation.viewmodel.SupervisorEvidenceUi
import com.example.mantec_ins.presentation.viewmodel.SupervisorPersonaUi
import com.example.mantec_ins.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToLong
import java.io.File

private val MantecOrange = Color(0xFFD94D33)
private val SoftBackground = Color(0xFFF8F4EE)
private val CardBackground = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)
private val BadgeGreenBg = Color(0xFFDCFCE7)
private val BadgeGreenText = Color(0xFF166534)
private val BadgeGrayBg = Color(0xFFF1F5F9)
private val BadgeGrayText = Color(0xFF64748B)
private val BadgeAmberBg = Color(0xFFFEF3C7)
private val BadgeAmberText = Color(0xFF92400E)
private val BadgeRedBg = Color(0xFFFEE2E2)
private val BadgeRedText = Color(0xFF991B1B)

// "12.0" -> "12", "12.5" se queda igual. Redondea a 2 decimales primero
// para no arrastrar ruido de punto flotante tras varios +/- de 0.5.
// internal (no private) para poder probarla desde app/src/test — ver
// SupervisorHomeScreenUtilsTest.
internal fun formatHoras(value: Double): String {
    val redondeado = (value * 100.0).roundToLong() / 100.0
    return if (redondeado == redondeado.toLong().toDouble()) {
        redondeado.toLong().toString()
    } else {
        redondeado.toString()
    }
}

// Miniatura real desde el archivo local — mismo patron que ya usan los
// reportes de activos (ReportFormScreen.kt: loadEvidenceThumbnail), asi
// una evidencia recien tomada se ve de inmediato, sin esperar a que
// suba a R2 y sin depender de conexion. Se usa un path de archivo plano
// (no content:// Uri) porque encolarEvidenciaDesdeUri ya copia la
// evidencia a filesDir antes de guardarla en Room. Devuelve null si el
// archivo ya no existe (evidencia ya subida y borrada localmente, o
// evidencia que vino del servidor con localPath="").
private fun loadLocalEvidenceThumbnail(path: String, fileType: String): Bitmap? {
    if (path.isBlank() || !File(path).exists()) return null
    return try {
        if (fileType == "video") {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val frame = retriever.getFrameAtTime(0)
            retriever.release()
            frame
        } else {
            BitmapFactory.decodeFile(path)
        }
    } catch (_: Exception) {
        null
    }
}

// Equivalente Android de resources/views/personal/ver-como/index.blade.php
// (backend Laravel), offline-first (ver OFFLINE_SUPERVISOR.md): lee
// siempre de Room via SupervisorActivityViewModel, nunca espera a la red
// para pintar. registrationSyncStatus/syncStatus de cada actividad y
// evidencia reflejan si un cambio local todavia no llego al servidor.
@Composable
fun SupervisorHomeScreen(
    userName: String,
    isLoading: Boolean,
    actividades: List<SupervisorActivityUi>,
    actividadesHoy: List<SupervisorActivityUi>,
    actividadesAyer: List<SupervisorActivityUi>,
    diaSeleccionado: DiaActividad,
    errorMessage: String?,
    guardando: Boolean,
    subiendoEvidenciaActivityId: Long?,
    sincronizando: Boolean,
    toastMessage: String?,
    onRefresh: () -> Unit,
    onSincronizar: () -> Unit,
    onSeleccionarDia: (DiaActividad) -> Unit,
    onGuardar: (activityId: Long, comments: String?, allWorked: Boolean, personasHoras: Map<Long, Double>) -> Unit,
    onGuardarComentarioPersona: (activityId: Long, comments: String?, allWorked: Boolean, personasHoras: Map<Long, Double>, employeeId: Long, comentario: String?) -> Unit,
    onSubirEvidencias: (activityId: Long, uris: List<Uri>) -> Unit,
    onBorrarEvidencia: (activityId: Long, evidencia: SupervisorEvidenceUi) -> Unit,
    onAbrirEvidencia: suspend (activityId: Long, evidencia: SupervisorEvidenceUi) -> String?,
    onClearToast: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Acordeon: solo una actividad expandida a la vez, para no confundir
    // sobre cual registro se esta editando.
    var actividadExpandidaId by remember { mutableStateOf<Long?>(null) }
    // Visor de evidencia en pantalla completa (modal) — antes tocar una
    // evidencia mandaba al navegador con Intent.ACTION_VIEW, sacando al
    // usuario de la app. Ahora se muestra adentro, con boton de cerrar.
    var evidenciaEnVisor by remember { mutableStateOf<EvidenciaVisorState?>(null) }

    LaunchedEffect(Unit) {
        onRefresh()
        onSincronizar()
    }

    // Sin esto, sincronizar() solo se disparaba al abrir esta pantalla, al
    // guardar un registro/evidencia, o con el boton manual — si el
    // supervisor se queda con la app abierta y solo recupera señal (sale
    // de una zona sin cobertura, por ejemplo), nada avisaba que ya se
    // podia sincronizar hasta cerrar y reabrir la app (pedido 2026-09-22,
    // confirmado en vivo: el reconecte silencioso no disparaba nada).
    // Este NetworkCallback dispara sincronizar() apenas vuelve la
    // conexion, reutilizando el mismo spinner que ya muestra el boton de
    // refresco manual (parametro "sincronizando"), asi el supervisor ve
    // en vivo que arranco a sincronizar sin tener que minimizar la app.
    DisposableEffect(Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                coroutineScope.launch(Dispatchers.Main) { onSincronizar() }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(3000)
            onClearToast()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
            // Sin esto, la barra de arriba ("Mis actividades" + botones de
            // refrescar/cerrar sesion) queda pegada al borde fisico de la
            // pantalla y en telefonos con notch/isla de camara/barra de
            // estado mas alta terminaba tapada por el propio sistema —
            // HomeScreen.kt (Inspector) ya usa este mismo patron, esta
            // pantalla se habia quedado sin el (pedido 2026-09-22).
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Surface(color = CardBackground, shadowElevation = 2.dp) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Mis actividades", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize, color = TextPrimary)
                        Text(userName, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSincronizar, enabled = !sincronizando) {
                            if (sincronizando) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MantecOrange)
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = "Sincronizar", tint = TextSecondary)
                            }
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión", tint = TextSecondary)
                        }
                    }
                }

                // Toggle Hoy/Ayer: solo aparece si hay actividades en ambos
                // dias — si solo hay de uno, no tiene sentido elegir. "Ayer"
                // en la practica es siempre el turno Nocturno que cruza
                // medianoche (ver Api\Personal\ActivityController::index()),
                // agrupado por la fecha real programada de cada actividad.
                if (actividadesHoy.isNotEmpty() && actividadesAyer.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(SoftBackground, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DiaToggleButton(
                            text = "Ayer",
                            seleccionado = diaSeleccionado == DiaActividad.AYER,
                            onClick = { onSeleccionarDia(DiaActividad.AYER) },
                            modifier = Modifier.weight(1f)
                        )
                        DiaToggleButton(
                            text = "Hoy",
                            seleccionado = diaSeleccionado == DiaActividad.HOY,
                            onClick = { onSeleccionarDia(DiaActividad.HOY) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        toastMessage?.let {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                color = Color(0xFF111827),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(it, color = Color.White, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MantecOrange)
                }
            }

            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage, color = TextSecondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = MantecOrange)) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            actividades.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Sin actividades por diligenciar", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            "No tienes actividades programadas para hoy, ni turnos nocturnos pendientes de ayer.",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            else -> {
                val actividadesDelDia = if (diaSeleccionado == DiaActividad.HOY) actividadesHoy else actividadesAyer
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(actividadesDelDia, key = { it.id }) { actividad ->
                        ActividadCard(
                            actividad = actividad,
                            guardando = guardando,
                            subiendoEvidencia = subiendoEvidenciaActivityId == actividad.id,
                            abierta = actividadExpandidaId == actividad.id,
                            onToggleAbierta = {
                                actividadExpandidaId = if (actividadExpandidaId == actividad.id) null else actividad.id
                            },
                            onGuardar = { comments, allWorked, personasHoras ->
                                onGuardar(actividad.id, comments, allWorked, personasHoras)
                            },
                            onGuardarComentarioPersona = { comments, allWorked, personasHoras, employeeId, comentario ->
                                onGuardarComentarioPersona(actividad.id, comments, allWorked, personasHoras, employeeId, comentario)
                            },
                            onSubirEvidencias = { uris -> onSubirEvidencias(actividad.id, uris) },
                            onBorrarEvidencia = { evidencia -> onBorrarEvidencia(actividad.id, evidencia) },
                            onAbrirEvidencia = { evidencia ->
                                coroutineScope.launch {
                                    val url = onAbrirEvidencia(actividad.id, evidencia)
                                    if (url != null) {
                                        evidenciaEnVisor = EvidenciaVisorState(url, evidencia.fileType, evidencia.originalName)
                                    }
                                }
                            },
                            onObtenerUrlMiniatura = { evidencia -> onAbrirEvidencia(actividad.id, evidencia) }
                        )
                    }
                }
            }
        }
    }

    evidenciaEnVisor?.let { visor ->
        EvidenciaVisorDialog(visor = visor, onDismiss = { evidenciaEnVisor = null })
    }
}

private data class EvidenciaVisorState(val url: String, val fileType: String, val originalName: String)

// Modal a pantalla completa para ver una evidencia sin salir de la app
// (antes se abria en el navegador via Intent.ACTION_VIEW). Foto con
// AsyncImage (Coil), video con VideoView nativo (con sus propios
// controles de reproduccion) — se cierra tocando la X o fuera del
// contenido.
@Composable
private fun EvidenciaVisorDialog(visor: EvidenciaVisorState, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            if (visor.fileType == "video") {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clickable(enabled = false) {},
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(visor.url))
                            setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                            setOnPreparedListener { it.isLooping = false; start() }
                        }
                    }
                )
            } else {
                AsyncImage(
                    model = visor.url,
                    contentDescription = visor.originalName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable(enabled = false) {},
                    contentScale = ContentScale.Fit
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color(0x66000000), CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}

// Comentario del responsable sobre las horas reportadas de UN trabajador
// puntual de esta actividad (pedido 2026-09-22) — se abre al tocar el
// nombre en la lista de personas cuando "No, no todos trabajaron..." esta
// seleccionado. A lo sumo 1 comentario por responsable+actividad+trabajador
// (lo garantiza el backend con upsert, ver Api\Personal\ActivityController
// ::store), asi que este modal siempre edita ese unico comentario, nunca
// acumula un historial (a diferencia del historial mezclado de Bitacora en
// la web, que si combina esto con los comentarios de administrativo).
@Composable
private fun ComentarioPersonaDialog(
    persona: SupervisorPersonaUi,
    onDismiss: () -> Unit,
    onGuardar: (String?) -> Unit,
    onBorrar: () -> Unit
) {
    var texto by remember(persona.employeeId) { mutableStateOf(persona.comentario ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CardBackground
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(persona.nombre, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "Comentario sobre las horas reportadas de este trabajador",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej. se fue temprano por cita médica...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MantecOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = MantecOrange
                    ),
                    minLines = 3
                )
                Spacer(Modifier.height(14.dp))
                // contentPadding reducido: con los 3 botones (Borrar +
                // Cancelar + Guardar) el padding por defecto de
                // OutlinedButton/Button hacia que "Guardar" se partiera en
                // dos lineas en telefonos angostos (visto en vivo contra
                // el emulador).
                val botonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    if (!persona.comentario.isNullOrBlank()) {
                        OutlinedButton(onClick = onBorrar, contentPadding = botonPadding) {
                            Text("Borrar", color = BadgeRedText)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    OutlinedButton(onClick = onDismiss, contentPadding = botonPadding) { Text("Cancelar") }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = { onGuardar(texto.ifBlank { null }) },
                        colors = ButtonDefaults.buttonColors(containerColor = MantecOrange),
                        contentPadding = botonPadding
                    ) { Text("Guardar") }
                }
            }
        }
    }
}

@Composable
private fun ActividadCard(
    actividad: SupervisorActivityUi,
    guardando: Boolean,
    subiendoEvidencia: Boolean,
    abierta: Boolean,
    onToggleAbierta: () -> Unit,
    onGuardar: (comments: String?, allWorked: Boolean, personasHoras: Map<Long, Double>) -> Unit,
    // Comentario por trabajador (pedido 2026-09-22) reutiliza el mismo
    // endpoint de guardado que "Guardar registro" (no hay ruta nueva en el
    // backend) — por eso este callback tambien manda el estado ACTUAL del
    // formulario (comments/allWorked/personasHoras), no solo el comentario.
    // Sin esto, guardar un comentario ANTES de tocar "Guardar registro"
    // dejaba allWorkedScheduledHours en null en Room, y el push siguiente
    // caia en el default `?: true`, mandaba personas=[] y el comentario
    // nunca llegaba al servidor (encontrado verificando en vivo contra el
    // backend local, no en teoria).
    onGuardarComentarioPersona: (comments: String?, allWorked: Boolean, personasHoras: Map<Long, Double>, employeeId: Long, comentario: String?) -> Unit,
    onSubirEvidencias: (List<Uri>) -> Unit,
    onBorrarEvidencia: (SupervisorEvidenceUi) -> Unit,
    onAbrirEvidencia: (SupervisorEvidenceUi) -> Unit,
    onObtenerUrlMiniatura: suspend (SupervisorEvidenceUi) -> String?
) {
    val context = LocalContext.current
    var comments by remember(actividad.id) { mutableStateOf(actividad.comments ?: "") }
    var todosTrabajaron by remember(actividad.id) { mutableStateOf(actividad.allWorkedScheduledHours ?: true) }
    val horasPorPersona = remember(actividad.id) {
        mutableStateMapOf<Long, String>().apply {
            actividad.personas.forEach { p ->
                put(p.employeeId, formatHoras(p.workedHours ?: actividad.estimatedHours ?: 0.0))
            }
        }
    }
    // Comentario del responsable sobre un trabajador puntual (pedido
    // 2026-09-22) — modal aparte del registro de horas/comentario general,
    // se abre al tocar el nombre cuando "No, no todos trabajaron..." esta
    // seleccionado.
    var comentarioEnEdicion by remember(actividad.id) { mutableStateOf<SupervisorPersonaUi?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris -> if (uris.isNotEmpty()) onSubirEvidencias(uris) }

    // Tomar foto / grabar video con la camara — mismos botones que ya
    // existen en los reportes de activos (ReportFormScreen.kt/
    // MainActivity.kt: TakePicture()/CaptureVideo() sobre una Uri de
    // MediaStore, con permiso de camara pedido en el momento si hace
    // falta). Carpeta propia (ManTecSupervisor) para no mezclar con la
    // que ya usa el Inspector. La Uri resultante entra por el mismo
    // onSubirEvidencias() que ya usa la galeria — encolarEvidenciaDesdeUri()
    // la copia a almacenamiento privado igual, sin importar de donde vino.
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingTakePhoto by remember { mutableStateOf(false) }
    var pendingRecordVideo by remember { mutableStateOf(false) }

    fun crearUriImagen(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "mantec_supervisor_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ManTecSupervisor")
            }
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    fun crearUriVideo(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "mantec_supervisor_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ManTecSupervisor")
            }
        }
        return context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) onSubirEvidencias(listOf(photoUri!!))
    }
    val captureVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success && videoUri != null) onSubirEvidencias(listOf(videoUri!!))
    }
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            if (pendingTakePhoto) {
                pendingTakePhoto = false
                crearUriImagen()?.let { uri -> photoUri = uri; takePictureLauncher.launch(uri) }
            } else if (pendingRecordVideo) {
                pendingRecordVideo = false
                crearUriVideo()?.let { uri -> videoUri = uri; captureVideoLauncher.launch(uri) }
            }
        } else {
            pendingTakePhoto = false
            pendingRecordVideo = false
        }
    }
    fun tomarFoto() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            crearUriImagen()?.let { uri -> photoUri = uri; takePictureLauncher.launch(uri) }
        } else {
            pendingTakePhoto = true
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    fun grabarVideo() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            crearUriVideo()?.let { uri -> videoUri = uri; captureVideoLauncher.launch(uri) }
        } else {
            pendingRecordVideo = true
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardBackground,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleAbierta() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(actividad.description, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        listOfNotNull(actividad.companyName, actividad.team, actividad.process).joinToString(" · "),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${actividad.estimatedHours ?: "—"}h programadas · ${actividad.shift}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Badge(
                        text = if (actividad.registrado) "Registrado" else "Pendiente",
                        bg = if (actividad.registrado) BadgeGreenBg else BadgeGrayBg,
                        fg = if (actividad.registrado) BadgeGreenText else BadgeGrayText
                    )
                    // Estado de SINCRONIZACION (offline), distinto del
                    // estado de negocio de arriba: un registro puede estar
                    // "Registrado" localmente pero aun sin llegar al
                    // servidor.
                    when (actividad.registrationSyncStatus) {
                        "PENDING_SYNC" -> Badge(text = "Sin sincronizar", bg = BadgeAmberBg, fg = BadgeAmberText, iconOffline = true)
                        "ERROR" -> Badge(text = "Error al enviar", bg = BadgeRedBg, fg = BadgeRedText)
                    }
                }
            }

            if (abierta) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    if (actividad.closed) {
                        Text(
                            "Esta actividad ya fue cerrada — no se puede editar el registro.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text("Comentarios", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        OutlinedTextField(
                            value = comments,
                            onValueChange = { comments = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Novedades de la actividad ejecutada...", color = TextSecondary) },
                            // Sin colors explicitos, este campo hereda el
                            // esquema dinamico (Material You) del telefono,
                            // que en varios dispositivos tinta borde y texto
                            // de un rosado apenas legible.
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MantecOrange,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = MantecOrange
                            )
                        )

                        Spacer(Modifier.height(10.dp))
                        Text("¿Todos trabajaron las horas programadas?", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            OutlinedButton(
                                onClick = { todosTrabajaron = true },
                                colors = if (todosTrabajaron) ButtonDefaults.outlinedButtonColors(containerColor = MantecOrange, contentColor = Color.White) else ButtonDefaults.outlinedButtonColors(),
                                // Sin esto, OutlinedButton dibuja su borde por
                                // defecto encima del relleno naranja tambien
                                // cuando esta seleccionado — se veia un trazo
                                // oscuro que "Guardar registro" (un Button
                                // normal, sin borde) no tiene.
                                border = if (todosTrabajaron) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) { Text("Sí") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { todosTrabajaron = false },
                                colors = if (!todosTrabajaron) ButtonDefaults.outlinedButtonColors(containerColor = MantecOrange, contentColor = Color.White) else ButtonDefaults.outlinedButtonColors(),
                                border = if (!todosTrabajaron) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) { Text("No") }
                        }

                        if (!todosTrabajaron) {
                            Spacer(Modifier.height(10.dp))
                            Text("Horas trabajadas por persona", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            actividad.personas.forEach { persona ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clickable { comentarioEnEdicion = persona }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(persona.nombre, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                        // Lapiz SIEMPRE visible (pedido 2026-09-22: mas
                                        // intuitivo que solo aparezca cuando ya hay
                                        // comentario, porque entonces nada indica que el
                                        // nombre es clickeable) — gris = sin comentario
                                        // todavia (invita a agregar uno), naranja = ya
                                        // tiene comentario.
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = if (persona.comentario.isNullOrBlank()) "Agregar comentario" else "Tiene comentario",
                                            tint = if (persona.comentario.isNullOrBlank()) TextSecondary else MantecOrange,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    HorasStepper(
                                        valor = horasPorPersona[persona.employeeId] ?: "0",
                                        onValorChange = { horasPorPersona[persona.employeeId] = it },
                                        onRestar = {
                                            val actual = horasPorPersona[persona.employeeId]?.toDoubleOrNull() ?: 0.0
                                            horasPorPersona[persona.employeeId] = formatHoras((actual - 0.5).coerceAtLeast(0.0))
                                        },
                                        onSumar = {
                                            val actual = horasPorPersona[persona.employeeId]?.toDoubleOrNull() ?: 0.0
                                            horasPorPersona[persona.employeeId] = formatHoras(actual + 0.5)
                                        }
                                    )
                                }
                            }
                        }

                        comentarioEnEdicion?.let { persona ->
                            ComentarioPersonaDialog(
                                persona = persona,
                                onDismiss = { comentarioEnEdicion = null },
                                onGuardar = { texto ->
                                    val personasHorasActual = actividad.personas.associate { p ->
                                        p.employeeId to (horasPorPersona[p.employeeId]?.toDoubleOrNull() ?: 0.0)
                                    }
                                    onGuardarComentarioPersona(comments.ifBlank { null }, todosTrabajaron, personasHorasActual, persona.employeeId, texto)
                                    comentarioEnEdicion = null
                                },
                                onBorrar = {
                                    val personasHorasActual = actividad.personas.associate { p ->
                                        p.employeeId to (horasPorPersona[p.employeeId]?.toDoubleOrNull() ?: 0.0)
                                    }
                                    onGuardarComentarioPersona(comments.ifBlank { null }, todosTrabajaron, personasHorasActual, persona.employeeId, null)
                                    comentarioEnEdicion = null
                                }
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("Evidencias (fotos / video)", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            items(actividad.evidencias, key = { it.localId }) { evidencia ->
                                val borderColor = when (evidencia.syncStatus) {
                                    "PENDING_SYNC" -> BadgeAmberText
                                    "ERROR" -> BadgeRedText
                                    else -> Color.Transparent
                                }
                                // Miniatura local primero (offline, instantanea,
                                // mismo patron que ReportFormScreen.kt en los
                                // reportes de activos: decodifica el archivo
                                // propio, foto o primer frame de video) — asi
                                // una evidencia recien tomada se ve de una vez,
                                // sin esperar a que suba a R2. Solo si ya no
                                // queda copia local (se subio y se borro, ver
                                // SupervisorSyncRepository) se intenta la URL
                                // firmada del servidor, y solo para fotos (un
                                // video no se puede previsualizar asi).
                                val localBitmap = remember(evidencia.localId, evidencia.localPath) {
                                    loadLocalEvidenceThumbnail(evidencia.localPath, evidencia.fileType)
                                }
                                var thumbnailUrl by remember(evidencia.localId, evidencia.syncStatus) { mutableStateOf<String?>(null) }
                                val esFotoSincronizada = evidencia.fileType != "video" && evidencia.syncStatus == "SYNCED"
                                // La URL firmada de R2 vence a los 10 min
                                // (ver API_SUPERVISOR.md seccion 2) — mientras
                                // el tile siga en pantalla, se vuelve a pedir
                                // cada 8 min (bajo el TTL real) para que no
                                // se rompa si la pantalla queda abierta mas
                                // tiempo. Se cancela solo si el tile sale de
                                // composicion (ver OFFLINE_SUPERVISOR.md,
                                // limitacion conocida de v1.9.1, ahora resuelta).
                                LaunchedEffect(evidencia.localId, evidencia.syncStatus, localBitmap) {
                                    if (esFotoSincronizada && localBitmap == null) {
                                        while (true) {
                                            if (NetworkUtils.hasInternet(context)) {
                                                thumbnailUrl = onObtenerUrlMiniatura(evidencia)
                                            }
                                            kotlinx.coroutines.delay(8 * 60 * 1000L)
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BadgeGrayBg)
                                        .clickable { onAbrirEvidencia(evidencia) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Icono siempre de fondo: si la miniatura
                                    // todavia esta cargando o la carga falla
                                    // (AsyncImage no pinta nada en esos
                                    // casos), el icono generico se sigue
                                    // viendo detras en vez de dejar el tile
                                    // en blanco.
                                    Icon(
                                        if (evidencia.fileType == "video") Icons.Filled.VideoFile else Icons.Filled.Image,
                                        contentDescription = evidencia.originalName,
                                        tint = TextSecondary
                                    )
                                    if (localBitmap != null) {
                                        Image(
                                            bitmap = localBitmap.asImageBitmap(),
                                            contentDescription = evidencia.originalName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (thumbnailUrl != null) {
                                        AsyncImage(
                                            model = thumbnailUrl,
                                            contentDescription = evidencia.originalName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    if (evidencia.syncStatus != "SYNCED") {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                                .size(10.dp)
                                                .background(borderColor, CircleShape)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onBorrarEvidencia(evidencia) },
                                        modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier
                                            .background(Color(0xB3111827), CircleShape))
                                    }
                                }
                            }
                        }
                        // Mismos botones que ya usan los reportes de activos
                        // (ReportFormScreen.kt: EvidenceSection) — tomar
                        // foto, grabar video, o elegir de la galeria.
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { tomarFoto() },
                                enabled = !subiendoEvidencia,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MantecOrange),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Tomar foto")
                            }
                            Button(
                                onClick = { grabarVideo() },
                                enabled = !subiendoEvidencia,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MantecOrange),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Grabar video")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            },
                            enabled = !subiendoEvidencia,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MantecOrange),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MantecOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (subiendoEvidencia) "Guardando..." else "Galería")
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val personasHoras = if (todosTrabajaron) {
                                    emptyMap()
                                } else {
                                    actividad.personas.associate { p ->
                                        p.employeeId to (horasPorPersona[p.employeeId]?.toDoubleOrNull() ?: 0.0)
                                    }
                                }
                                onGuardar(comments.ifBlank { null }, todosTrabajaron, personasHoras)
                            },
                            enabled = !guardando,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MantecOrange)
                        ) {
                            if (guardando) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Guardar registro")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Stepper de horas: contenedor en pastilla (mismo fondo suave que el
// resto de la pantalla) con los botones +/- como circulos blancos
// flotando adentro y el campo de texto sin borde propio en el medio —
// antes eran 3 elementos sueltos (icono, caja con borde, icono) que se
// veian muy simples/desconectados entre si.
@Composable
private fun HorasStepper(valor: String, onValorChange: (String) -> Unit, onRestar: () -> Unit, onSumar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onRestar, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "Restar media hora", tint = MantecOrange, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(4.dp))
        // BasicTextField, no OutlinedTextField: este ultimo trae un
        // padding interno fijo que con un ancho chico (para verse
        // minimalista) dejaba "12.5" prácticamente invisible — un
        // entero de 1 digito alcanzaba a caber, un decimal de 3-4
        // caracteres no. BasicTextField no tiene chrome/padding propio,
        // asi que el ancho disponible es todo para el texto.
        BasicTextField(
            value = valor,
            onValueChange = onValorChange,
            modifier = Modifier.width(52.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            cursorBrush = SolidColor(MantecOrange),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onSumar, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Sumar media hora", tint = MantecOrange, modifier = Modifier.size(18.dp))
        }
    }
}

// Segmented control (un solo contenedor con fondo suave, y el segmento
// activo como una pastilla naranja adentro) — no dos OutlinedButton sueltos con espacio
// entre ellos, que se veian como dos botones independientes en vez de un
// selector de dos opciones.
@Composable
private fun DiaToggleButton(text: String, seleccionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (seleccionado) MantecOrange else Color.Transparent
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
            color = if (seleccionado) Color.White else TextSecondary
        )
    }
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color, iconOffline: Boolean = false) {
    Surface(color = bg, shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 4.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconOffline) {
                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = fg, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(3.dp))
            }
            Text(text, color = fg, style = MaterialTheme.typography.labelSmall)
        }
    }
}

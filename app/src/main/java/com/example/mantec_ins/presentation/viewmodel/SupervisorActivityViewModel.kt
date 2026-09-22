package com.example.mantec_ins.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mantec_ins.data.repository.PersonalActivityLocalRepository
import com.example.mantec_ins.data.repository.PersonalActivityRepository
import com.example.mantec_ins.data.repository.SupervisorSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// HOY = actividades con date == fecha real de hoy. AYER = date == fecha
// real de ayer (en la practica, solo turno Nocturno — el backend nunca
// manda otra cosa para ayer, ver Api\Personal\ActivityController::index()).
// Comparar contra actividad.date (la fecha programada real), nunca inferir
// "ayer" por el turno solo.
enum class DiaActividad { HOY, AYER }

data class SupervisorPersonaUi(
    val employeeId: Long,
    val nombre: String,
    val nickname: String,
    val workedHours: Double?,
    val comentario: String?
)

data class SupervisorEvidenceUi(
    val localId: Long,
    val serverId: Long?,
    val originalName: String,
    val fileType: String,
    val syncStatus: String,
    // Path del archivo propio en filesDir (ver PersonalActivityLocalRepository.
    // encolarEvidenciaDesdeUri) — "" si ya se subio y se borro la copia
    // local (ver SupervisorSyncRepository.pushEvidenciaPendiente). Se usa
    // para mostrar una miniatura real offline, sin depender de una URL
    // firmada de R2.
    val localPath: String
)

data class SupervisorActivityUi(
    val id: Long,
    val date: String,
    val companyName: String,
    val team: String?,
    val process: String?,
    val description: String,
    val activityType: String,
    val shift: String,
    val estimatedHours: Double?,
    val closed: Boolean,
    val comments: String?,
    val allWorkedScheduledHours: Boolean?,
    val registrado: Boolean,
    // SYNCED | PENDING_SYNC | ERROR — ver PATRONES_ASINCRONISMO_OFFLINE.md
    val registrationSyncStatus: String,
    val personas: List<SupervisorPersonaUi>,
    val evidencias: List<SupervisorEvidenceUi>
)

data class SupervisorHomeUiState(
    val isLoading: Boolean = false,
    // Union de ambos dias (hoy + nocturno de ayer) tal como llega de Room —
    // se usa solo para el estado vacio general. La UI pinta actividadesHoy/
    // actividadesAyer segun diaSeleccionado.
    val actividades: List<SupervisorActivityUi> = emptyList(),
    val actividadesHoy: List<SupervisorActivityUi> = emptyList(),
    val actividadesAyer: List<SupervisorActivityUi> = emptyList(),
    val diaSeleccionado: DiaActividad = DiaActividad.HOY,
    // Distingue "HOY porque todavia no se eligio nada a mano" de "el
    // usuario realmente toco el toggle" — ver recargarDesdeRoom() y el fix
    // 2026-09-22 de la carrera que dejaba la pantalla pegada en AYER.
    val diaSeleccionadoManualmente: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
    val guardando: Boolean = false,
    val subiendoEvidenciaActivityId: Long? = null,
    val sincronizando: Boolean = false
)

// Local-first (ver PATRONES_ASINCRONISMO_OFFLINE.md patron 1): la UI
// siempre lee de Room via PersonalActivityLocalRepository, nunca espera
// a la red para pintar. El refresh/push contra el servidor lo hace
// SupervisorSyncRepository aparte, disparado desde aca y desde
// MainActivity (arranque, entrar a la pantalla, onResume, boton manual
// — mismos puntos que el Inspector). Equivalente Android de
// resources/views/personal/ver-como/index.blade.php (backend Laravel).
class SupervisorActivityViewModel(
    private val localRepository: PersonalActivityLocalRepository,
    private val syncRepository: SupervisorSyncRepository,
    private val remoteRepository: PersonalActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupervisorHomeUiState())
    val uiState: StateFlow<SupervisorHomeUiState> = _uiState

    // Pinta lo que ya hay en Room de inmediato; no espera a la red.
    fun cargarActividades() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            recargarDesdeRoom()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    // Dispara el sync (push + refresh) y vuelve a pintar desde Room al
    // terminar. Se puede llamar aunque no haya red: SupervisorSyncRepository
    // maneja el fallo en silencio (ver patron 7 del documento de patrones).
    fun sincronizar() {
        if (_uiState.value.sincronizando) return
        _uiState.value = _uiState.value.copy(sincronizando = true)
        viewModelScope.launch {
            syncRepository.sync()
            recargarDesdeRoom()
            _uiState.value = _uiState.value.copy(sincronizando = false)
        }
    }

    private suspend fun recargarDesdeRoom() {
        val actividades = localRepository.getActividades().map { entity ->
            val personas = localRepository.getPersonas(entity.id).map {
                SupervisorPersonaUi(it.employeeId, it.nombre, it.nickname, it.workedHours, it.comentario)
            }
            val evidencias = localRepository.getEvidencias(entity.id).map {
                SupervisorEvidenceUi(it.id, it.serverId, it.originalName, it.fileType, it.syncStatus, it.localPath)
            }
            SupervisorActivityUi(
                id = entity.id, date = entity.date, companyName = entity.companyName,
                team = entity.team, process = entity.process, description = entity.description,
                activityType = entity.activityType, shift = entity.shift,
                estimatedHours = entity.estimatedHours, closed = entity.closed,
                comments = entity.comments, allWorkedScheduledHours = entity.allWorkedScheduledHours,
                registrado = entity.registrado, registrationSyncStatus = entity.registrationSyncStatus,
                personas = personas, evidencias = evidencias
            )
        }

        // date es la fecha real programada de cada actividad (una nocturna
        // que cruza medianoche sigue con la fecha de ayer) — se compara tal
        // cual, sin inferir nada por el turno.
        val actividadesHoy = actividades.filter { it.date == todayDateString() }
        val actividadesAyer = actividades.filter { it.date == yesterdayDateString() }

        // Bug real reportado 2026-09-22: con actividades de ayer Y de hoy,
        // la pantalla quedaba pegada en AYER en vez de arrancar siempre en
        // HOY. Causa — una carrera en el primer arranque: cargarActividades()
        // (onRefresh) lee Room ANTES de que sincronizar() haya traido las
        // actividades reales de hoy; si Room todavia tenia en cache algo de
        // AYER de una sesion previa, ese primer recargarDesdeRoom() elegia
        // AYER por defecto (unica opcion con datos en ese instante) — y como
        // la logica de abajo trata cualquier valor de diaSeleccionado como
        // si fuera una eleccion deliberada del usuario, quedaba "pegado" en
        // AYER para siempre, aunque el siguiente recargarDesdeRoom() (tras
        // el sync real) ya trajera actividades de hoy.
        //
        // Fix: mientras el usuario NO haya tocado el toggle a mano
        // (diaSeleccionadoManualmente = false), siempre se prefiere HOY en
        // cuanto haya datos — nunca se queda pegado en un default
        // accidental. Solo despues de una eleccion manual real se respeta
        // "no dejar la pantalla en un dia vacio" (el comportamiento
        // original), para no sacar al supervisor de AYER a mitad de
        // completar un reporte nocturno pendiente solo porque llego un
        // sync en background.
        val diaActual = _uiState.value.diaSeleccionado
        val elegidoManualmente = _uiState.value.diaSeleccionadoManualmente
        val diaEfectivo = when {
            !elegidoManualmente && actividadesHoy.isNotEmpty() -> DiaActividad.HOY
            !elegidoManualmente && actividadesAyer.isNotEmpty() -> DiaActividad.AYER
            !elegidoManualmente -> DiaActividad.HOY
            diaActual == DiaActividad.HOY && actividadesHoy.isNotEmpty() -> DiaActividad.HOY
            diaActual == DiaActividad.AYER && actividadesAyer.isNotEmpty() -> DiaActividad.AYER
            actividadesHoy.isNotEmpty() -> DiaActividad.HOY
            actividadesAyer.isNotEmpty() -> DiaActividad.AYER
            else -> DiaActividad.HOY
        }

        _uiState.value = _uiState.value.copy(
            actividades = actividades,
            actividadesHoy = actividadesHoy,
            actividadesAyer = actividadesAyer,
            diaSeleccionado = diaEfectivo
        )
    }

    // Toggle manual Hoy/Ayer desde la UI — se respeta en el proximo
    // recargarDesdeRoom() mientras ese dia siga teniendo actividades.
    fun seleccionarDia(dia: DiaActividad) {
        _uiState.value = _uiState.value.copy(diaSeleccionado = dia, diaSeleccionadoManualmente = true)
    }

    // Guarda local de inmediato (PENDING_SYNC) e intenta sincronizar en
    // el momento — si no hay red, queda pendiente sin error visible al
    // usuario (patron 7: un refresh/push opcional que falla no debe
    // bloquear ni asustar).
    fun guardarRegistro(
        activityId: Long,
        comments: String?,
        allWorkedScheduledHours: Boolean,
        personasHoras: Map<Long, Double>
    ) {
        _uiState.value = _uiState.value.copy(guardando = true)
        viewModelScope.launch {
            localRepository.guardarRegistroLocal(activityId, comments, allWorkedScheduledHours, personasHoras)
            recargarDesdeRoom()
            _uiState.value = _uiState.value.copy(guardando = false, toastMessage = "Registro guardado.")
            sincronizar()
        }
    }

    // Comentario del responsable sobre las horas reportadas de UN
    // trabajador puntual (pedido 2026-09-22). Reutiliza el mismo endpoint
    // de guardado que guardarRegistro() (no hay ruta nueva en el backend),
    // asi que persiste TAMBIEN el snapshot actual del formulario
    // (comments/allWorked/personasHoras) antes del comentario — si no,
    // Room se queda con allWorkedScheduledHours = null (nunca se toco
    // "Guardar registro") y el push siguiente cae en el default `?: true`
    // de SupervisorSyncRepository, manda personas=[] y el comentario nunca
    // sale de la app. comentario = null o "" borra el comentario existente.
    fun guardarComentarioPersona(
        activityId: Long,
        comments: String?,
        allWorkedScheduledHours: Boolean,
        personasHoras: Map<Long, Double>,
        employeeId: Long,
        comentario: String?
    ) {
        val mensaje = if (comentario.isNullOrBlank()) "Comentario eliminado." else "Comentario guardado."
        viewModelScope.launch {
            localRepository.guardarRegistroLocal(activityId, comments, allWorkedScheduledHours, personasHoras)
            localRepository.guardarComentarioPersonaLocal(activityId, employeeId, comentario)
            recargarDesdeRoom()
            _uiState.value = _uiState.value.copy(toastMessage = mensaje)
            sincronizar()
        }
    }

    fun subirEvidencias(activityId: Long, uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(subiendoEvidenciaActivityId = activityId)
        viewModelScope.launch {
            uris.forEach { uri -> localRepository.encolarEvidenciaDesdeUri(activityId, uri) }
            recargarDesdeRoom()
            _uiState.value = _uiState.value.copy(
                subiendoEvidenciaActivityId = null,
                toastMessage = "Evidencia guardada, se subirá cuando haya conexión."
            )
            sincronizar()
        }
    }

    fun borrarEvidencia(activityId: Long, evidencia: SupervisorEvidenceUi) {
        viewModelScope.launch {
            if (evidencia.serverId != null) {
                remoteRepository.borrarEvidenciaRemota(activityId, evidencia.serverId).onFailure {
                    _uiState.value = _uiState.value.copy(toastMessage = it.message ?: "No se pudo eliminar la evidencia.")
                    return@launch
                }
            }
            localRepository.borrarEvidenciaLocal(evidencia.localId)
            recargarDesdeRoom()
            _uiState.value = _uiState.value.copy(toastMessage = "Evidencia eliminada.")
        }
    }

    // Solo disponible para evidencia ya sincronizada (con serverId) — la
    // pendiente todavia no tiene URL firmada de R2.
    suspend fun obtenerUrlEvidencia(activityId: Long, evidencia: SupervisorEvidenceUi): String? {
        val serverId = evidencia.serverId ?: return null
        return remoteRepository.obtenerUrlEvidencia(activityId, serverId).getOrNull()
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}

// El backend fija 'timezone' => 'America/Bogota' en config/app.php — todo
// "hoy"/"ayer" del lado servidor (today(), Activity::date) esta calculado
// en esa zona, sin importar donde corra el servidor. Si el cliente usara
// Calendar.getInstance() (zona horaria del dispositivo) en vez de anclar
// a Bogota explicitamente, un telefono mal configurado o en otra zona
// (o, como paso probando esto, un emulador con el reloj en GMT) clasificaria
// mal Hoy/Ayer durante las horas en que ambas zonas no coinciden en la
// fecha — hasta 5 horas al dia, siempre alrededor de la medianoche.
private val bogotaZone = java.util.TimeZone.getTimeZone("America/Bogota")
private val fechaFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = bogotaZone }

// Mismo formato que Activity::date->toDateString() del backend (Api\
// Personal\ActivityController::serialize()), para comparar tal cual contra
// SupervisorActivityUi.date. nowMillis es parametro (no System.currentTimeMillis()
// directo) para poder fijar un instante exacto en el test — ver
// SupervisorActivityDateTest.
internal fun todayDateString(nowMillis: Long = System.currentTimeMillis()): String {
    val cal = Calendar.getInstance(bogotaZone)
    cal.timeInMillis = nowMillis
    return fechaFormat.format(cal.time)
}

internal fun yesterdayDateString(nowMillis: Long = System.currentTimeMillis()): String {
    val cal = Calendar.getInstance(bogotaZone)
    cal.timeInMillis = nowMillis
    cal.add(Calendar.DAY_OF_YEAR, -1)
    return fechaFormat.format(cal.time)
}

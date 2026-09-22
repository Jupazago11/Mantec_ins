package com.example.mantec_ins.data.repository

import com.example.mantec_ins.data.local.AppDatabase
import com.example.mantec_ins.data.local.SupervisorActivityDao
import com.example.mantec_ins.data.local.SupervisorActivityEntity
import com.example.mantec_ins.data.local.SupervisorEvidenceDao
import com.example.mantec_ins.data.local.SupervisorEvidenceEntity
import com.example.mantec_ins.data.local.SupervisorPersonaDao
import com.example.mantec_ins.data.local.SupervisorPersonaEntity
import com.example.mantec_ins.data.remote.personal.PersonalActivitiesResponse
import com.example.mantec_ins.data.remote.personal.PersonalActivityDto
import com.example.mantec_ins.data.remote.personal.PersonalApiService
import com.example.mantec_ins.data.remote.personal.PersonalEvidenceDto
import com.example.mantec_ins.data.remote.personal.PersonalEvidenceUploadResponse
import com.example.mantec_ins.data.remote.personal.PersonalPersonaDto
import com.example.mantec_ins.data.remote.personal.PersonalSaveActivityRequest
import com.example.mantec_ins.data.remote.personal.PersonalSaveActivityResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.File

// Cubre la maquina de estados PENDING_SYNC -> SYNCED/ERROR de
// SupervisorSyncRepository — la logica mas critica de OFFLINE_SUPERVISOR.md
// (patron 2 de PATRONES_ASINCRONISMO_OFFLINE.md), antes sin ningun test
// automatizado. Se prueba a traves de la API publica (sync()/
// refreshDesdeServidor()), verificando las llamadas a los DAOs — no se
// abre visibilidad de metodos privados solo para testear.
class SupervisorSyncRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var api: PersonalApiService
    private lateinit var activityDao: SupervisorActivityDao
    private lateinit var personaDao: SupervisorPersonaDao
    private lateinit var evidenceDao: SupervisorEvidenceDao
    private lateinit var repository: SupervisorSyncRepository

    @Before
    fun setUp() {
        db = mockk()
        api = mockk()
        activityDao = mockk(relaxed = true)
        personaDao = mockk(relaxed = true)
        evidenceDao = mockk(relaxed = true)

        every { db.supervisorActivityDao() } returns activityDao
        every { db.supervisorPersonaDao() } returns personaDao
        every { db.supervisorEvidenceDao() } returns evidenceDao

        // Por defecto, sin nada pendiente y sin novedades del servidor —
        // cada test sobreescribe lo que le interesa.
        coEvery { activityDao.getByStatus(any()) } returns emptyList()
        coEvery { evidenceDao.getByStatus(any()) } returns emptyList()
        coEvery { api.getActividades() } returns PersonalActivitiesResponse(
            success = true, date = "2026-09-20", actividades = emptyList()
        )

        repository = SupervisorSyncRepository(db = db, api = api)
    }

    private fun actividad(id: Long = 1L, status: String = "PENDING_SYNC") = SupervisorActivityEntity(
        id = id, date = "2026-09-20", companyName = "ARGOS", team = "Equipo 1", process = "Proceso 1",
        description = "desc", activityType = "tipo", shift = "Diurno", estimatedHours = 8.0, closed = false,
        comments = "comentario", allWorkedScheduledHours = true, reportedHours = null, registrado = true,
        registrationSyncStatus = status
    )

    private fun dto(id: Long = 1L, comments: String? = "del servidor") = PersonalActivityDto(
        id = id, date = "2026-09-20", company_name = "ARGOS", team = "Equipo 1", process = "Proceso 1",
        description = "desc", activity_type = "tipo", shift = "Diurno", estimated_hours = 8.0, closed = false,
        comments = comments, all_worked_scheduled_hours = true, reported_hours = null, registrado = true,
        personas = emptyList(), evidencias = emptyList()
    )

    private fun errorResponse(code: Int): Response<PersonalSaveActivityResponse> =
        Response.error(code, "{}".toResponseBody("application/json".toMediaType()))

    private fun errorEvidenceResponse(code: Int): Response<PersonalEvidenceUploadResponse> =
        Response.error(code, "{}".toResponseBody("application/json".toMediaType()))

    // --- push de registro (comentarios/horas) ---

    @Test
    fun `push de registro exitoso marca SYNCED`() = runTest {
        val act = actividad()
        coEvery { activityDao.getByStatus("PENDING_SYNC") } returns listOf(act)
        coEvery { personaDao.getByActivity(act.id) } returns emptyList()
        coEvery { api.saveActividad(act.id, any()) } returns Response.success(
            PersonalSaveActivityResponse(success = true, message = "ok", activity = dto(act.id))
        )

        repository.sync()

        coVerify(exactly = 1) { activityDao.updateStatus(act.id, "SYNCED") }
    }

    @Test
    fun `push de registro con 403 marca ERROR con mensaje`() = runTest {
        val act = actividad()
        coEvery { activityDao.getByStatus("PENDING_SYNC") } returns listOf(act)
        coEvery { personaDao.getByActivity(act.id) } returns emptyList()
        coEvery { api.saveActividad(act.id, any()) } returns errorResponse(403)

        repository.sync()

        coVerify(exactly = 1) { activityDao.updateStatus(act.id, "ERROR", any()) }
    }

    @Test
    fun `push de registro con 422 marca ERROR con mensaje`() = runTest {
        val act = actividad()
        coEvery { activityDao.getByStatus("PENDING_SYNC") } returns listOf(act)
        coEvery { personaDao.getByActivity(act.id) } returns emptyList()
        coEvery { api.saveActividad(act.id, any()) } returns errorResponse(422)

        repository.sync()

        coVerify(exactly = 1) { activityDao.updateStatus(act.id, "ERROR", any()) }
    }

    @Test
    fun `push de registro con fallo transitorio (500) no toca el estado`() = runTest {
        val act = actividad()
        coEvery { activityDao.getByStatus("PENDING_SYNC") } returns listOf(act)
        coEvery { personaDao.getByActivity(act.id) } returns emptyList()
        coEvery { api.saveActividad(act.id, any()) } returns errorResponse(500)

        repository.sync()

        // Ni SYNCED ni ERROR — se deja PENDING_SYNC para reintentar despues.
        coVerify(exactly = 0) { activityDao.updateStatus(any(), any(), any()) }
    }

    @Test
    fun `push de registro con excepcion de red no toca el estado`() = runTest {
        val act = actividad()
        coEvery { activityDao.getByStatus("PENDING_SYNC") } returns listOf(act)
        coEvery { personaDao.getByActivity(act.id) } returns emptyList()
        coEvery { api.saveActividad(act.id, any()) } throws java.io.IOException("sin red")

        repository.sync()

        coVerify(exactly = 0) { activityDao.updateStatus(any(), any(), any()) }
    }

    // --- push de evidencia ---

    @Test
    fun `push de evidencia exitoso marca SYNCED y borra el archivo local`() = runTest {
        val archivo = File.createTempFile("evidencia_test", ".jpg")
        archivo.writeText("contenido de prueba")
        val evidencia = SupervisorEvidenceEntity(
            id = 5L, activityId = 1L, localPath = archivo.absolutePath,
            originalName = "foto.jpg", fileType = "image", syncStatus = "PENDING_SYNC"
        )
        coEvery { evidenceDao.getByStatus("PENDING_SYNC") } returns listOf(evidencia)
        coEvery { api.uploadEvidencias(1L, any()) } returns Response.success(
            PersonalEvidenceUploadResponse(
                success = true, message = "ok",
                evidencias = listOf(PersonalEvidenceDto(99L, "foto.jpg", "image", "http://x", "http://x"))
            )
        )

        repository.sync()

        coVerify(exactly = 1) { evidenceDao.updateSyncData(5L, "SYNCED", 99L) }
        assertFalse("el archivo local debe borrarse tras subir a R2", archivo.exists())
    }

    @Test
    fun `push de evidencia con 422 marca ERROR`() = runTest {
        val archivo = File.createTempFile("evidencia_test", ".jpg")
        archivo.writeText("contenido de prueba")
        val evidencia = SupervisorEvidenceEntity(
            id = 5L, activityId = 1L, localPath = archivo.absolutePath,
            originalName = "foto.jpg", fileType = "image", syncStatus = "PENDING_SYNC"
        )
        coEvery { evidenceDao.getByStatus("PENDING_SYNC") } returns listOf(evidencia)
        coEvery { api.uploadEvidencias(1L, any()) } returns errorEvidenceResponse(422)

        repository.sync()

        coVerify(exactly = 1) { evidenceDao.updateStatus(5L, "ERROR", any()) }
        archivo.delete()
    }

    @Test
    fun `push de evidencia sin archivo local marca ERROR sin llamar a la api`() = runTest {
        val evidencia = SupervisorEvidenceEntity(
            id = 5L, activityId = 1L, localPath = "/ruta/que/no/existe/${System.nanoTime()}.jpg",
            originalName = "foto.jpg", fileType = "image", syncStatus = "PENDING_SYNC"
        )
        coEvery { evidenceDao.getByStatus("PENDING_SYNC") } returns listOf(evidencia)

        repository.sync()

        coVerify(exactly = 1) { evidenceDao.updateStatus(5L, "ERROR", any()) }
        coVerify(exactly = 0) { api.uploadEvidencias(any(), any()) }
    }

    // --- refresh/merge desde el servidor ---

    @Test
    fun `refresh inserta actividad nueva como SYNCED`() = runTest {
        coEvery { activityDao.getById(1L) } returns null
        coEvery { api.getActividades() } returns PersonalActivitiesResponse(
            success = true, date = "2026-09-20", actividades = listOf(dto(1L))
        )

        val ok = repository.refreshDesdeServidor()

        assertTrue(ok)
        coVerify(exactly = 1) { activityDao.insert(match { it.id == 1L && it.registrationSyncStatus == "SYNCED" }) }
    }

    @Test
    fun `refresh NO pisa comentarios de una actividad con registro pendiente`() = runTest {
        val local = actividad(id = 1L, status = "PENDING_SYNC")
        coEvery { activityDao.getById(1L) } returns local
        coEvery { api.getActividades() } returns PersonalActivitiesResponse(
            success = true, date = "2026-09-20", actividades = listOf(dto(1L, comments = "distinto del local"))
        )

        repository.refreshDesdeServidor()

        coVerify(exactly = 1) { activityDao.updateReadOnlyFields(1L, any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { activityDao.updateRegistration(any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { personaDao.deleteByActivity(any()) }
    }

    @Test
    fun `refresh SI actualiza una actividad ya sincronizada`() = runTest {
        val local = actividad(id = 1L, status = "SYNCED")
        coEvery { activityDao.getById(1L) } returns local
        coEvery { api.getActividades() } returns PersonalActivitiesResponse(
            success = true, date = "2026-09-20", actividades = listOf(dto(1L, comments = "actualizado"))
        )

        repository.refreshDesdeServidor()

        coVerify(exactly = 1) { activityDao.updateRegistration(1L, "actualizado", any(), any(), any(), "SYNCED", any()) }
        coVerify(exactly = 1) { personaDao.deleteByActivity(1L) }
    }
}

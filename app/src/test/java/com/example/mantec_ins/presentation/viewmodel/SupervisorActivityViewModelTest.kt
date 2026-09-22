package com.example.mantec_ins.presentation.viewmodel

import com.example.mantec_ins.data.local.SupervisorActivityEntity
import com.example.mantec_ins.data.repository.PersonalActivityLocalRepository
import com.example.mantec_ins.data.repository.PersonalActivityRepository
import com.example.mantec_ins.data.repository.SupervisorSyncRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// Bug real reportado 2026-09-22: con actividades de ayer (nocturno
// pendiente) Y de hoy, la pantalla arrancaba pegada en AYER en vez de HOY.
// Causa: cargarActividades() (onRefresh) lee Room ANTES de que
// sincronizar() traiga las actividades reales de hoy — si en ese primer
// instante Room solo tiene la de ayer, recargarDesdeRoom() elegia AYER por
// defecto, y la logica original trataba ese default accidental como si
// fuera una eleccion deliberada del usuario, quedando pegado en AYER para
// siempre aunque el siguiente refresh ya trajera actividades de hoy. Este
// test reproduce esa secuencia exacta contra el ViewModel real (no solo
// lee el codigo) para confirmar que el fix (diaSeleccionadoManualmente)
// la resuelve.
@OptIn(ExperimentalCoroutinesApi::class)
class SupervisorActivityViewModelTest {

    private lateinit var localRepository: PersonalActivityLocalRepository
    private lateinit var syncRepository: SupervisorSyncRepository
    private lateinit var remoteRepository: PersonalActivityRepository
    private lateinit var viewModel: SupervisorActivityViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        localRepository = mockk()
        syncRepository = mockk()
        remoteRepository = mockk(relaxed = true)

        coEvery { localRepository.getPersonas(any()) } returns emptyList()
        coEvery { localRepository.getEvidencias(any()) } returns emptyList()
        coEvery { syncRepository.sync() } returns true

        viewModel = SupervisorActivityViewModel(localRepository, syncRepository, remoteRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun actividad(id: Long, date: String) = SupervisorActivityEntity(
        id = id, date = date, companyName = "ARGOS", team = null, process = null,
        description = "desc", activityType = "P", shift = if (date == yesterdayDateString()) "Nocturno" else "Diurno",
        estimatedHours = 8.0, closed = false, comments = null, allWorkedScheduledHours = null,
        reportedHours = null, registrado = false, registrationSyncStatus = "SYNCED"
    )

    @Test
    fun `arranca en HOY aunque el primer refresh de Room solo trajera AYER`() = runTest {
        val ayer = actividad(1L, yesterdayDateString())
        val hoy = actividad(2L, todayDateString())

        // Paso 1 (onRefresh): Room todavia no tiene la actividad de hoy —
        // simula la carrera real.
        coEvery { localRepository.getActividades() } returns listOf(ayer)
        viewModel.cargarActividades()

        assertEquals(
            "tras el primer refresh (solo AYER en Room), el default automatico cae a AYER",
            DiaActividad.AYER,
            viewModel.uiState.value.diaSeleccionado
        )

        // Paso 2 (sincronizar): ahora Room ya tiene tambien la de hoy.
        coEvery { localRepository.getActividades() } returns listOf(ayer, hoy)
        viewModel.sincronizar()

        assertEquals(
            "en cuanto hay datos de HOY, debe preferirse HOY — nunca quedar pegado en el default accidental de AYER",
            DiaActividad.HOY,
            viewModel.uiState.value.diaSeleccionado
        )
    }

    @Test
    fun `una eleccion MANUAL de AYER si se respeta entre refrescos`() = runTest {
        val ayer = actividad(1L, yesterdayDateString())
        val hoy = actividad(2L, todayDateString())
        coEvery { localRepository.getActividades() } returns listOf(ayer, hoy)

        viewModel.cargarActividades()
        assertEquals(DiaActividad.HOY, viewModel.uiState.value.diaSeleccionado)

        // El usuario elige AYER a mano (ej. para completar un reporte
        // nocturno pendiente).
        viewModel.seleccionarDia(DiaActividad.AYER)
        assertEquals(DiaActividad.AYER, viewModel.uiState.value.diaSeleccionado)

        // Un sync en background (ej. dispara solo) no debe sacarlo de AYER
        // mientras siga teniendo datos — a diferencia del default
        // automatico, esta eleccion fue deliberada.
        viewModel.sincronizar()
        assertEquals(
            "una eleccion manual de AYER no debe revertirse sola mientras siga teniendo datos",
            DiaActividad.AYER,
            viewModel.uiState.value.diaSeleccionado
        )
    }
}

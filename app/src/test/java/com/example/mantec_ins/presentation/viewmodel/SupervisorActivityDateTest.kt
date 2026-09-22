package com.example.mantec_ins.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

// El backend fija 'timezone' => 'America/Bogota' en config/app.php — el
// cliente debe clasificar Hoy/Ayer segun esa misma zona, no la del
// dispositivo (o la de la maquina donde corren los tests/CI). Se detectó
// en vivo que el emulador de prueba corría con el reloj en GMT, lo que
// habría clasificado mal Hoy/Ayer en las ~5 horas al día donde Bogotá y
// GMT no coinciden en la fecha. Ver DOCUMENTACION_PROYECTO.md.
class SupervisorActivityDateTest {

    @Test
    fun `todayDateString usa Bogota, no la zona del dispositivo o CI`() {
        // 2026-09-21 02:00:00 UTC = 2026-09-20 21:00:00 America/Bogota
        // (UTC-5, sin horario de verano) — ya es "manana" en UTC/GMT pero
        // todavia "hoy" en Bogota.
        val instanteUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.SEPTEMBER, 21, 2, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals("2026-09-20", todayDateString(instanteUtc))
        assertEquals("2026-09-19", yesterdayDateString(instanteUtc))
    }

    @Test
    fun `fecha simple sin cruce de zona horaria`() {
        // 2026-09-20 15:00:00 UTC = 2026-09-20 10:00:00 Bogota — mismo dia
        // calendario en ambas zonas, caso sin ambiguedad.
        val instanteUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals("2026-09-20", todayDateString(instanteUtc))
        assertEquals("2026-09-19", yesterdayDateString(instanteUtc))
    }
}

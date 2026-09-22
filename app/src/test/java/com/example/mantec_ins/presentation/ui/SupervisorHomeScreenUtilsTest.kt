package com.example.mantec_ins.presentation.ui

import org.junit.Assert.assertEquals
import org.junit.Test

// formatHoras() decide como se muestra el campo de horas trabajadas por
// persona en SupervisorHomeScreen — "12.0" confundia al usuario (ver
// DOCUMENTACION_PROYECTO.md v1.9.1), y el stepper +/- de 0.5 depende de
// que el redondeo no arrastre ruido de punto flotante entre pasos.
class SupervisorHomeScreenUtilsTest {

    @Test
    fun `numero entero se muestra sin decimales`() {
        assertEquals("12", formatHoras(12.0))
        assertEquals("0", formatHoras(0.0))
    }

    @Test
    fun `medio punto se muestra con su decimal`() {
        assertEquals("12.5", formatHoras(12.5))
        assertEquals("0.5", formatHoras(0.5))
    }

    @Test
    fun `redondea ruido de punto flotante tras sumas repetidas de 0_5`() {
        var valor = 0.0
        repeat(7) { valor += 0.5 } // 0.5 * 7 = 3.5, pero en Double acumula error
        assertEquals("3.5", formatHoras(valor))
    }

    @Test
    fun `redondea a 2 decimales un valor con mas precision`() {
        assertEquals("12.33", formatHoras(12.3333333))
    }

    @Test
    fun `valor negativo tambien se formatea sin decimales de mas`() {
        // El clamping a 0 lo hace el caller (stepper); formatHoras solo formatea.
        assertEquals("-0.5", formatHoras(-0.5))
    }
}

package com.example.msdocumentos.controller

import com.example.msdocumentos.DTO.BloqueHorarioDTO
import com.example.msdocumentos.model.EstadoReserva
import com.example.msdocumentos.model.Reserva
import com.example.msdocumentos.service.ConflictoReservaException
import com.example.msdocumentos.service.ReservaInvalidaException
import com.example.msdocumentos.service.ReservaService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * Pruebas del controller de reservas: verifican el código HTTP de cada endpoint.
 * El servicio se simula con MockK. Se registra ManejadorErrores para comprobar
 * que las excepciones se traducen a 400 y 409.
 */
class ReservaControllerTest {

    private val service = mockk<ReservaService>()
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(ReservaController(service))
        .setControllerAdvice(ManejadorErrores())
        .build()

    /** Reserva ficticia, sin datos reales. */
    private fun reserva(id: Int? = 1, estado: EstadoReserva = EstadoReserva.PENDIENTE) = Reserva(
        id = id, salaId = 1, salaNombre = "Sala de Clases 101", usuarioId = 10,
        profesorNombre = "Ana Docente", fecha = "2026-11-24",
        horaInicio = "08:15", horaFin = "09:00", estado = estado
    )

    private val jsonReserva = """
        {"salaId":1,"salaNombre":"Sala de Clases 101","usuarioId":10,"profesorNombre":"Ana Docente",
         "fecha":"2026-11-24","horaInicio":"08:15","horaFin":"09:00"}
    """.trimIndent()

    // ================================================================== bloques

    @Test
    fun bloquesResponde200ConLaListaDeBloques() {
        // Arrange
        every { service.obtenerBloquesDisponibles(1, "2026-11-24") } returns listOf(
            BloqueHorarioDTO("08:15", "09:00", false, "DISPONIBLE"),
            BloqueHorarioDTO("09:00", "09:45", true, "ACEPTADA")
        )
        // Act + Assert
        mockMvc.perform(get("/api/reservas/bloques").param("salaId", "1").param("fecha", "2026-11-24"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.length()").value(2))
            .andExpect(jsonPath("\$[1].estado").value("ACEPTADA"))
            .andExpect(jsonPath("\$[1].ocupado").value(true))
    }

    @Test
    fun bloquesConFechaInvalidaResponde400ConMensaje() {
        // Arrange
        every { service.obtenerBloquesDisponibles(1, "mañana") } throws
                ReservaInvalidaException("Fecha inválida, use el formato AAAA-MM-DD")
        // Act + Assert
        mockMvc.perform(get("/api/reservas/bloques").param("salaId", "1").param("fecha", "mañana"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("\$.mensaje").value("Fecha inválida, use el formato AAAA-MM-DD"))
    }

    // ================================================================= solicitar

    @Test
    fun solicitarResponde201ConLaReservaCreada() {
        // Arrange
        every { service.solicitarReserva(any()) } returns reserva(id = 5)
        // Act + Assert
        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(jsonReserva))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("\$.id").value(5))
            .andExpect(jsonPath("\$.estado").value("PENDIENTE"))
    }

    @Test
    fun solicitarUnBloqueOcupadoResponde409ConMensaje() {
        // Arrange
        every { service.solicitarReserva(any()) } returns null
        // Act + Assert
        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(jsonReserva))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("\$.mensaje").value("Ese bloque horario ya está reservado"))
    }

    @Test
    fun solicitarSinAnticipacionResponde400ConMensaje() {
        // Arrange
        every { service.solicitarReserva(any()) } throws
                ReservaInvalidaException("La solicitud debe hacerse con al menos un día hábil de anticipación")
        // Act + Assert
        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(jsonReserva))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("\$.mensaje")
                    .value("La solicitud debe hacerse con al menos un día hábil de anticipación")
            )
    }

    // =================================================================== listados

    @Test
    fun listarTodasResponde200() {
        // Arrange
        every { service.listarTodas() } returns listOf(reserva(id = 1), reserva(id = 2))
        // Act + Assert
        mockMvc.perform(get("/api/reservas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.length()").value(2))
    }

    @Test
    fun listarPendientesResponde200() {
        // Arrange
        every { service.listarPendientes() } returns listOf(reserva(id = 1))
        // Act + Assert
        mockMvc.perform(get("/api/reservas/pendientes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.length()").value(1))
    }

    @Test
    fun listarPorProfesorResponde200() {
        // Arrange
        every { service.listarPorProfesor(10) } returns listOf(reserva(id = 1), reserva(id = 2))
        // Act + Assert
        mockMvc.perform(get("/api/reservas/usuario/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.length()").value(2))
    }

    // =================================================================== aprobar

    @Test
    fun aprobarResponde200ConLaReservaAceptada() {
        // Arrange
        every { service.aprobarReserva(1) } returns reserva(id = 1, estado = EstadoReserva.ACEPTADA)
        // Act + Assert
        mockMvc.perform(put("/api/reservas/1/aprobar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.estado").value("ACEPTADA"))
    }

    @Test
    fun aprobarInexistenteResponde404() {
        // Arrange
        every { service.aprobarReserva(999) } returns null
        // Act + Assert
        mockMvc.perform(put("/api/reservas/999/aprobar"))
            .andExpect(status().isNotFound())
    }

    @Test
    fun aprobarConElBloqueOcupadoResponde409ConMensaje() {
        // Arrange
        every { service.aprobarReserva(2) } throws
                ConflictoReservaException("Ese bloque horario ya fue aprobado para otra solicitud")
        // Act + Assert
        mockMvc.perform(put("/api/reservas/2/aprobar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("\$.mensaje").value("Ese bloque horario ya fue aprobado para otra solicitud"))
    }

    // ================================================================== rechazar

    @Test
    fun rechazarConMotivoResponde200YEnviaElMotivoAlServicio() {
        // Arrange
        every { service.rechazarReserva(1, "Mantención eléctrica") } returns
                reserva(id = 1, estado = EstadoReserva.RECHAZADA)
        // Act + Assert
        mockMvc.perform(
            put("/api/reservas/1/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"motivo":"Mantención eléctrica"}""")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.estado").value("RECHAZADA"))
        verify(exactly = 1) { service.rechazarReserva(1, "Mantención eléctrica") }
    }

    @Test
    fun rechazarSinCampoMotivoEnviaTextoVacioYResponde400() {
        // Arrange: el controller ya no inventa un motivo, lo valida el servicio
        every { service.rechazarReserva(1, "") } throws
                ReservaInvalidaException("Debe indicar el motivo del rechazo")
        // Act + Assert
        mockMvc.perform(
            put("/api/reservas/1/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("\$.mensaje").value("Debe indicar el motivo del rechazo"))
    }

    @Test
    fun rechazarInexistenteResponde404() {
        // Arrange
        every { service.rechazarReserva(999, any()) } returns null
        // Act + Assert
        mockMvc.perform(
            put("/api/reservas/999/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"motivo":"Sin sala"}""")
        )
            .andExpect(status().isNotFound())
    }

    @Test
    fun rechazarUnaSolicitudYaResueltaResponde409() {
        // Arrange
        every { service.rechazarReserva(1, any()) } throws
                ConflictoReservaException("La solicitud ya fue resuelta (ACEPTADA)")
        // Act + Assert
        mockMvc.perform(
            put("/api/reservas/1/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"motivo":"Cambio de planes"}""")
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("\$.mensaje").value("La solicitud ya fue resuelta (ACEPTADA)"))
    }

    // =================================================================== eliminar

    @Test
    fun eliminarExistenteResponde204() {
        // Arrange
        every { service.eliminar(1) } returns true
        // Act + Assert
        mockMvc.perform(delete("/api/reservas/1"))
            .andExpect(status().isNoContent())
    }

    @Test
    fun eliminarInexistenteResponde404() {
        // Arrange
        every { service.eliminar(999) } returns false
        // Act + Assert
        mockMvc.perform(delete("/api/reservas/999"))
            .andExpect(status().isNotFound())
    }
}
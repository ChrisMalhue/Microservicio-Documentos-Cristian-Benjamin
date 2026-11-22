package com.example.msdocumentos.service

import com.example.msdocumentos.model.EstadoReserva
import com.example.msdocumentos.model.Reserva
import com.example.msdocumentos.repository.ReservaRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas unitarias de ReservaService.
 * - El repositorio se simula con MockK (no se usa Spring ni Neon).
 * - El reloj se fija en una fecha conocida para que las pruebas den el mismo resultado cualquier día.
 *
 * Calendario usado (noviembre 2026):
 * Lun 23 (hoy) · Mar 24 · Mié 25 · Jue 26 · Vie 27 · Sáb 28 · Dom 29 · Lun 30
 */
class ReservaServiceTest {

    private val repository = mockk<ReservaRepository>()
    private val zona = ZoneId.of("America/Santiago")

    /** Crea el servicio como si "hoy" fuera la fecha indicada (a mediodía, hora de Chile). */
    private fun servicioConHoy(hoy: String): ReservaService {
        val instante = LocalDate.parse(hoy).atTime(12, 0).atZone(zona).toInstant()
        return ReservaService(repository, Clock.fixed(instante, zona))
    }

    // Por defecto, hoy es lunes 23 de noviembre de 2026
    private val service = servicioConHoy("2026-11-23")

    /** Reserva ficticia, sin datos reales. */
    private fun reserva(
        id: Int? = 1,
        salaId: Int = 1,
        usuarioId: Int = 10,
        fecha: String = "2026-11-24",
        horaInicio: String = "08:15",
        horaFin: String = "09:00",
        estado: EstadoReserva = EstadoReserva.PENDIENTE,
        motivoRechazo: String? = null
    ) = Reserva(
        id = id, salaId = salaId, salaNombre = "Sala de Clases 101",
        usuarioId = usuarioId, profesorNombre = "Ana Docente",
        fecha = fecha, horaInicio = horaInicio, horaFin = horaFin,
        estado = estado, motivoRechazo = motivoRechazo
    )

    // ============================================================ bloques horarios

    @Test
    fun lunesAJuevesTieneDiezBloquesDisponibles() {
        // Arrange
        every { repository.findBySalaIdAndFecha(1, "2026-11-24") } returns emptyList()
        // Act
        val bloques = service.obtenerBloquesDisponibles(1, "2026-11-24")
        // Assert
        assertEquals(10, bloques.size)
        assertEquals("08:15", bloques.first().horaInicio)
        assertTrue(bloques.all { it.estado == "DISPONIBLE" && !it.ocupado })
    }

    @Test
    fun elViernesTieneSeisBloques() {
        // Arrange
        every { repository.findBySalaIdAndFecha(1, "2026-11-27") } returns emptyList()
        // Act
        val bloques = service.obtenerBloquesDisponibles(1, "2026-11-27")
        // Assert
        assertEquals(6, bloques.size)
        assertEquals("08:30", bloques.first().horaInicio)
        assertEquals("13:00", bloques.last().horaFin)
    }

    @Test
    fun sabadoYDomingoNoTienenBloques() {
        // Act + Assert
        assertTrue(service.obtenerBloquesDisponibles(1, "2026-11-28").isEmpty())
        assertTrue(service.obtenerBloquesDisponibles(1, "2026-11-29").isEmpty())
        verify(exactly = 0) { repository.findBySalaIdAndFecha(any(), any()) }
    }

    @Test
    fun bloquesConFechaInvalidaLanzaExcepcion() {
        // Act + Assert
        assertFailsWith<ReservaInvalidaException> {
            service.obtenerBloquesDisponibles(1, "24-11-2026")
        }
    }

    @Test
    fun bloqueConReservaAceptadaApareceOcupado() {
        // Arrange
        every { repository.findBySalaIdAndFecha(1, "2026-11-24") } returns listOf(
            reserva(horaInicio = "09:00", estado = EstadoReserva.ACEPTADA)
        )
        // Act
        val bloque = service.obtenerBloquesDisponibles(1, "2026-11-24").first { it.horaInicio == "09:00" }
        // Assert
        assertTrue(bloque.ocupado)
        assertEquals("ACEPTADA", bloque.estado)
    }

    @Test
    fun bloqueConReservaPendienteApareceComoPendienteYNoOcupado() {
        // Arrange
        every { repository.findBySalaIdAndFecha(1, "2026-11-24") } returns listOf(
            reserva(horaInicio = "09:00", estado = EstadoReserva.PENDIENTE)
        )
        // Act
        val bloque = service.obtenerBloquesDisponibles(1, "2026-11-24").first { it.horaInicio == "09:00" }
        // Assert
        assertFalse(bloque.ocupado)
        assertEquals("PENDIENTE", bloque.estado)
    }

    @Test
    fun reservaRechazadaNoOcupaElBloque() {
        // Arrange
        every { repository.findBySalaIdAndFecha(1, "2026-11-24") } returns listOf(
            reserva(horaInicio = "09:00", estado = EstadoReserva.RECHAZADA)
        )
        // Act
        val bloque = service.obtenerBloquesDisponibles(1, "2026-11-24").first { it.horaInicio == "09:00" }
        // Assert
        assertEquals("DISPONIBLE", bloque.estado)
    }

    @Test
    fun aceptadaTienePrioridadSobrePendienteEnElMismoBloque() {
        // Arrange: la PENDIENTE aparece primero en la lista
        every { repository.findBySalaIdAndFecha(1, "2026-11-24") } returns listOf(
            reserva(id = 1, horaInicio = "09:00", estado = EstadoReserva.PENDIENTE),
            reserva(id = 2, horaInicio = "09:00", estado = EstadoReserva.ACEPTADA)
        )
        // Act
        val bloque = service.obtenerBloquesDisponibles(1, "2026-11-24").first { it.horaInicio == "09:00" }
        // Assert
        assertTrue(bloque.ocupado)
        assertEquals("ACEPTADA", bloque.estado)
    }

    // ======================================================== solicitar reserva

    @Test
    fun solicitarGuardaLaReservaComoPendienteSinMotivoNiId() {
        // Arrange: llega con id, estado y motivo que no deben respetarse
        val guardada = slot<Reserva>()
        every {
            repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(1, "2026-11-24", "08:15", EstadoReserva.ACEPTADA)
        } returns false
        every { repository.save(capture(guardada)) } answers { guardada.captured.copy(id = 5) }
        val entrada = reserva(id = 99, estado = EstadoReserva.ACEPTADA, motivoRechazo = "inventado")
        // Act
        val resultado = service.solicitarReserva(entrada)
        // Assert
        assertNull(guardada.captured.id)
        assertEquals(EstadoReserva.PENDIENTE, guardada.captured.estado)
        assertNull(guardada.captured.motivoRechazo)
        assertEquals(5, resultado?.id)
    }

    @Test
    fun solicitarBloqueYaAceptadoDevuelveNullYNoGuarda() {
        // Arrange
        every {
            repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(1, "2026-11-24", "08:15", EstadoReserva.ACEPTADA)
        } returns true
        // Act
        val resultado = service.solicitarReserva(reserva())
        // Assert
        assertNull(resultado)
        verify(exactly = 0) { repository.save(any<Reserva>()) }
    }

    @Test
    fun solicitarParaElSiguienteDiaHabilEsValido() {
        // Arrange: hoy es lunes 23, mañana martes 24 es el primer día permitido
        every { repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(any(), any(), any(), any()) } returns false
        every { repository.save(any<Reserva>()) } answers { firstArg<Reserva>().copy(id = 1) }
        // Act
        val resultado = service.solicitarReserva(reserva(fecha = "2026-11-24"))
        // Assert
        assertNotNull(resultado)
    }

    @Test
    fun solicitarParaHoyLanzaExcepcionPorFaltaDeAnticipacion() {
        // Act
        val error = assertFailsWith<ReservaInvalidaException> {
            service.solicitarReserva(reserva(fecha = "2026-11-23"))
        }
        // Assert
        assertEquals("La solicitud debe hacerse con al menos un día hábil de anticipación", error.message)
        verify(exactly = 0) { repository.save(any<Reserva>()) }
    }

    @Test
    fun solicitarParaUnaFechaPasadaLanzaExcepcion() {
        // Act + Assert
        assertFailsWith<ReservaInvalidaException> {
            service.solicitarReserva(reserva(fecha = "2026-11-20"))
        }
    }

    @Test
    fun solicitarParaUnSabadoLanzaExcepcion() {
        // Act
        val error = assertFailsWith<ReservaInvalidaException> {
            service.solicitarReserva(reserva(fecha = "2026-11-28"))
        }
        // Assert
        assertEquals("Solo se pueden reservar días hábiles (lunes a viernes)", error.message)
    }

    @Test
    fun solicitarParaUnDomingoLanzaExcepcion() {
        // Act + Assert
        assertFailsWith<ReservaInvalidaException> {
            service.solicitarReserva(reserva(fecha = "2026-11-29"))
        }
    }

    @Test
    fun desdeUnViernesElPrimerDiaPermitidoEsElLunes() {
        // Arrange: hoy es viernes 27
        val servicioViernes = servicioConHoy("2026-11-27")
        every { repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(any(), any(), any(), any()) } returns false
        every { repository.save(any<Reserva>()) } answers { firstArg<Reserva>().copy(id = 1) }
        // Act
        val resultado = servicioViernes.solicitarReserva(reserva(fecha = "2026-11-30"))
        // Assert
        assertNotNull(resultado)
    }

    @Test
    fun desdeUnViernesNoSePuedeReservarElMismoViernes() {
        // Arrange
        val servicioViernes = servicioConHoy("2026-11-27")
        // Act + Assert
        assertFailsWith<ReservaInvalidaException> {
            servicioViernes.solicitarReserva(reserva(fecha = "2026-11-27"))
        }
    }

    @Test
    fun desdeUnSabadoElPrimerDiaPermitidoTambienEsElLunes() {
        // Arrange: hoy es sábado 28
        val servicioSabado = servicioConHoy("2026-11-28")
        every { repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(any(), any(), any(), any()) } returns false
        every { repository.save(any<Reserva>()) } answers { firstArg<Reserva>().copy(id = 1) }
        // Act
        val resultado = servicioSabado.solicitarReserva(reserva(fecha = "2026-11-30"))
        // Assert
        assertNotNull(resultado)
    }

    @Test
    fun solicitarConFechaMalEscritaLanzaExcepcion() {
        // Act
        val error = assertFailsWith<ReservaInvalidaException> {
            service.solicitarReserva(reserva(fecha = "mañana"))
        }
        // Assert
        assertEquals("Fecha inválida, use el formato AAAA-MM-DD", error.message)
    }

    // ================================================================== aprobar

    @Test
    fun aprobarUnaPendienteLaDejaAceptadaYSinMotivo() {
        // Arrange
        every { repository.findById(1) } returns Optional.of(reserva(id = 1, estado = EstadoReserva.PENDIENTE))
        every {
            repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(1, "2026-11-24", "08:15", EstadoReserva.ACEPTADA)
        } returns false
        every { repository.save(any<Reserva>()) } answers { firstArg<Reserva>() }
        // Act
        val resultado = service.aprobarReserva(1)
        // Assert
        assertEquals(EstadoReserva.ACEPTADA, resultado?.estado)
        assertNull(resultado?.motivoRechazo)
    }

    @Test
    fun aprobarUnaReservaInexistenteDevuelveNull() {
        // Arrange
        every { repository.findById(999) } returns Optional.empty()
        // Act
        val resultado = service.aprobarReserva(999)
        // Assert
        assertNull(resultado)
    }

    @Test
    fun aprobarConElBloqueYaAceptadoLanzaConflictoYNoGuarda() {
        // Arrange: dos solicitudes pendientes para el mismo bloque, y la otra ya fue aprobada
        every { repository.findById(2) } returns Optional.of(reserva(id = 2, estado = EstadoReserva.PENDIENTE))
        every {
            repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(1, "2026-11-24", "08:15", EstadoReserva.ACEPTADA)
        } returns true
        // Act
        val error = assertFailsWith<ConflictoReservaException> { service.aprobarReserva(2) }
        // Assert
        assertEquals("Ese bloque horario ya fue aprobado para otra solicitud", error.message)
        verify(exactly = 0) { repository.save(any<Reserva>()) }
    }

    @Test
    fun aprobarUnaSolicitudYaRechazadaLanzaConflicto() {
        // Arrange
        every { repository.findById(1) } returns Optional.of(reserva(id = 1, estado = EstadoReserva.RECHAZADA))
        // Act + Assert
        assertFailsWith<ConflictoReservaException> { service.aprobarReserva(1) }
        verify(exactly = 0) { repository.save(any<Reserva>()) }
    }

    @Test
    fun aprobarUnaSolicitudYaAceptadaLanzaConflicto() {
        // Arrange
        every { repository.findById(1) } returns Optional.of(reserva(id = 1, estado = EstadoReserva.ACEPTADA))
        // Act + Assert
        assertFailsWith<ConflictoReservaException> { service.aprobarReserva(1) }
        verify(exactly = 0) { repository.save(any<Reserva>()) }
    }

    // ================================================================= rechazar

    @Test
    fun rechazarUnaPendienteGuardaElMotivoSinEspacios() {
        // Arrange
        every { repository.findById(1) } returns Optional.of(reserva(id = 1, estado = EstadoReserva.PENDIENTE))
        every { repository.save(any<Reserva>()) } answers { firstArg<Reserva>() }
        // Act
        val resultado = service.rechazarReserva(1, "  Mantención eléctrica  ")
        // Assert
        assertEquals(EstadoReserva.RECHAZADA, resultado?.estado)
        assertEquals("Mantención eléctrica", resultado?.motivoRechazo)
    }

    @Test
    fun rechazarSinMotivoLanzaExcepcionYNoBuscaLaReserva() {
        // Act
        val error = assertFailsWith<ReservaInvalidaException> { service.rechazarReserva(1, "   ") }
        // Assert
        assertEquals("Debe indicar el motivo del rechazo", error.message)
        verify(exactly = 0) { repository.findById(any<Int>()) }
        verify(exactly = 0) { repository.save(any<Reserva>()) }
    }

    @Test
    fun rechazarUnaReservaInexistenteDevuelveNull() {
        // Arrange
        every { repository.findById(999) } returns Optional.empty()
        // Act
        val resultado = service.rechazarReserva(999, "Sin sala disponible")
        // Assert
        assertNull(resultado)
    }

    @Test
    fun rechazarUnaSolicitudYaResueltaLanzaConflicto() {
        // Arrange
        every { repository.findById(1) } returns Optional.of(reserva(id = 1, estado = EstadoReserva.ACEPTADA))
        // Act + Assert
        assertFailsWith<ConflictoReservaException> { service.rechazarReserva(1, "Cambio de planes") }
        verify(exactly = 0) { repository.save(any<Reserva>()) }
    }

    // =================================================================== listar

    @Test
    fun listarPendientesConsultaPorEstadoPendiente() {
        // Arrange
        every { repository.findByEstado(EstadoReserva.PENDIENTE) } returns listOf(reserva(id = 1), reserva(id = 2))
        // Act
        val resultado = service.listarPendientes()
        // Assert
        assertEquals(2, resultado.size)
    }

    @Test
    fun listarPorProfesorConsultaPorUsuario() {
        // Arrange
        every { repository.findByUsuarioId(10) } returns listOf(reserva(usuarioId = 10))
        // Act
        val resultado = service.listarPorProfesor(10)
        // Assert
        assertEquals(1, resultado.size)
        assertEquals(10, resultado.first().usuarioId)
    }

    @Test
    fun listarTodasDevuelveTodasLasReservas() {
        // Arrange
        every { repository.findAll() } returns listOf(reserva(id = 1), reserva(id = 2), reserva(id = 3))
        // Act
        val resultado = service.listarTodas()
        // Assert
        assertEquals(3, resultado.size)
    }

    // ================================================================== eliminar

    @Test
    fun eliminarUnaReservaExistenteDevuelveTrueYLaBorra() {
        // Arrange
        every { repository.existsById(1) } returns true
        justRun { repository.deleteById(1) }
        // Act
        val resultado = service.eliminar(1)
        // Assert
        assertTrue(resultado)
        verify(exactly = 1) { repository.deleteById(1) }
    }

    @Test
    fun eliminarUnaReservaInexistenteDevuelveFalseYNoBorra() {
        // Arrange
        every { repository.existsById(999) } returns false
        // Act
        val resultado = service.eliminar(999)
        // Assert
        assertFalse(resultado)
        verify(exactly = 0) { repository.deleteById(any<Int>()) }
    }
}
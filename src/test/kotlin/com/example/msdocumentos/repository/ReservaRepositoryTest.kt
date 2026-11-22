package com.example.msdocumentos.repository

import com.example.msdocumentos.model.EstadoReserva
import com.example.msdocumentos.model.Reserva
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pruebas de integración del repositorio: usan una base H2 real, pero en memoria
 * (no se conectan a Neon). Cada prueba corre en una transacción que se revierte al terminar.
 */
@DataJpaTest
class ReservaRepositoryTest(
    @Autowired private val repository: ReservaRepository
) {

    /** Reserva ficticia, sin datos reales. */
    private fun reserva(
        salaId: Int = 1,
        usuarioId: Int = 10,
        fecha: String = "2026-11-24",
        horaInicio: String = "08:15",
        estado: EstadoReserva = EstadoReserva.PENDIENTE,
        motivoRechazo: String? = null
    ) = Reserva(
        salaId = salaId, salaNombre = "Sala de Clases 101",
        usuarioId = usuarioId, profesorNombre = "Ana Docente",
        fecha = fecha, horaInicio = horaInicio, horaFin = "09:00",
        estado = estado, motivoRechazo = motivoRechazo
    )

    @BeforeEach
    fun partirConTablaVacia() {
        repository.deleteAll()
    }

    @Test
    fun guardarUnaReservaLeAsignaUnId() {
        // Act
        val guardada = repository.save(reserva())
        // Assert
        assertNotNull(guardada.id)
    }

    @Test
    fun elEstadoPorDefectoEsPendiente() {
        // Arrange
        val guardada = repository.saveAndFlush(Reserva(
            salaId = 1, salaNombre = "Sala", usuarioId = 10, profesorNombre = "Ana Docente",
            fecha = "2026-11-24", horaInicio = "08:15", horaFin = "09:00"
        ))
        // Act
        val leida = repository.findById(guardada.id!!).orElse(null)
        // Assert
        assertEquals(EstadoReserva.PENDIENTE, leida?.estado)
    }

    @Test
    fun findByUsuarioIdDevuelveSoloLasReservasDeEseUsuario() {
        // Arrange
        repository.save(reserva(usuarioId = 10))
        repository.save(reserva(usuarioId = 10, horaInicio = "09:00"))
        repository.save(reserva(usuarioId = 20))
        // Act
        val resultado = repository.findByUsuarioId(10)
        // Assert
        assertEquals(2, resultado.size)
        assertTrue(resultado.all { it.usuarioId == 10 })
    }

    @Test
    fun findByEstadoDevuelveSoloLasDeEseEstado() {
        // Arrange
        repository.save(reserva(horaInicio = "08:15", estado = EstadoReserva.PENDIENTE))
        repository.save(reserva(horaInicio = "09:00", estado = EstadoReserva.ACEPTADA))
        repository.save(reserva(horaInicio = "10:00", estado = EstadoReserva.RECHAZADA))
        // Act
        val pendientes = repository.findByEstado(EstadoReserva.PENDIENTE)
        // Assert
        assertEquals(1, pendientes.size)
        assertEquals("08:15", pendientes.first().horaInicio)
    }

    @Test
    fun findBySalaIdAndFechaDevuelveSoloLaSalaYElDiaPedidos() {
        // Arrange
        repository.save(reserva(salaId = 1, fecha = "2026-11-24", horaInicio = "08:15"))
        repository.save(reserva(salaId = 1, fecha = "2026-11-24", horaInicio = "09:00"))
        repository.save(reserva(salaId = 2, fecha = "2026-11-24"))
        repository.save(reserva(salaId = 1, fecha = "2026-11-25"))
        // Act
        val resultado = repository.findBySalaIdAndFecha(1, "2026-11-24")
        // Assert
        assertEquals(2, resultado.size)
    }

    @Test
    fun existsDetectaUnBloqueYaAceptado() {
        // Arrange
        repository.save(reserva(salaId = 1, fecha = "2026-11-24", horaInicio = "08:15", estado = EstadoReserva.ACEPTADA))
        // Act + Assert
        assertTrue(
            repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(1, "2026-11-24", "08:15", EstadoReserva.ACEPTADA)
        )
    }

    @Test
    fun existsNoCuentaUnaSolicitudSoloPendiente() {
        // Arrange
        repository.save(reserva(estado = EstadoReserva.PENDIENTE))
        // Act + Assert
        assertFalse(
            repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(1, "2026-11-24", "08:15", EstadoReserva.ACEPTADA)
        )
    }

    @Test
    fun existsDistingueEntreSalas() {
        // Arrange
        repository.save(reserva(salaId = 1, estado = EstadoReserva.ACEPTADA))
        // Act + Assert
        assertFalse(
            repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(2, "2026-11-24", "08:15", EstadoReserva.ACEPTADA)
        )
    }

    @Test
    fun existsDistingueEntreFechas() {
        // Arrange
        repository.save(reserva(fecha = "2026-11-24", estado = EstadoReserva.ACEPTADA))
        // Act + Assert
        assertFalse(
            repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(1, "2026-11-25", "08:15", EstadoReserva.ACEPTADA)
        )
    }

    @Test
    fun existsDistingueEntreBloquesHorarios() {
        // Arrange
        repository.save(reserva(horaInicio = "08:15", estado = EstadoReserva.ACEPTADA))
        // Act + Assert
        assertFalse(
            repository.existsBySalaIdAndFechaAndHoraInicioAndEstado(1, "2026-11-24", "09:00", EstadoReserva.ACEPTADA)
        )
    }

    @Test
    fun unMotivoDeRechazoLargoSeGuardaCompleto() {
        // Arrange: el motivo es una columna TEXT, se prueba con 5.000 caracteres
        val motivoLargo = "M".repeat(5_000)
        val guardada = repository.saveAndFlush(
            reserva(estado = EstadoReserva.RECHAZADA, motivoRechazo = motivoLargo)
        )
        // Act
        val leida = repository.findById(guardada.id!!).orElse(null)
        // Assert
        assertEquals(motivoLargo, leida?.motivoRechazo)
    }

    @Test
    fun eliminarPorIdBorraLaReserva() {
        // Arrange
        val guardada = repository.save(reserva())
        // Act
        repository.deleteById(guardada.id!!)
        // Assert
        assertFalse(repository.existsById(guardada.id!!))
    }
}
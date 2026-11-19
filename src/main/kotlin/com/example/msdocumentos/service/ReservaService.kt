package com.example.msdocumentos.service

import com.example.msdocumentos.DTO.BloqueHorarioDTO
import com.example.msdocumentos.model.EstadoReserva
import com.example.msdocumentos.model.Reserva
import com.example.msdocumentos.repository.ReservaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class ReservaService(
    private val reservaRepository: ReservaRepository
) {
    private val logger = LoggerFactory.getLogger(ReservaService::class.java)

    // 10 bloques de 45 min para Lunes a Jueves (con recreos escolares estándar)
    private val bloquesLunesAJueves = listOf(
        "08:15" to "09:00",
        "09:00" to "09:45",
        // Recreo 09:45 - 10:00
        "10:00" to "10:45",
        "10:45" to "11:30",
        // Recreo 11:30 - 11:45
        "11:45" to "12:30",
        "12:30" to "13:15",
        // Almuerzo 13:15 - 14:00
        "14:00" to "14:45",
        "14:45" to "15:30",
        "15:30" to "16:15",
        "16:15" to "16:30" // Bloque final / cierre jornada
    )

    // 6 bloques de 45 min para Viernes (08:30 a 13:00)
    private val bloquesViernes = listOf(
        "08:30" to "09:15",
        "09:15" to "10:00",
        "10:00" to "10:45",
        "10:45" to "11:30",
        "11:30" to "12:15",
        "12:15" to "13:00"
    )

    fun obtenerBloquesDisponibles(salaId: Int, fechaStr: String): List<BloqueHorarioDTO> {
        logger.info("Consultando bloques horarios para sala ID: {} en fecha: {}", salaId, fechaStr)
        val fecha = LocalDate.parse(fechaStr)
        val diaSemana = fecha.dayOfWeek

        // Si es sábado o domingo no hay bloques
        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            return emptyList()
        }

        val plantillaBloques = if (diaSemana == DayOfWeek.FRIDAY) bloquesViernes else bloquesLunesAJueves
        val reservasDelDia = reservaRepository.findBySalaIdAndFecha(salaId, fechaStr)

        return plantillaBloques.map { (inicio, fin) ->
            val reservaExistente = reservasDelDia.find {
                it.horaInicio == inicio && it.estado != EstadoReserva.RECHAZADA
            }

            when (reservaExistente?.estado) {
                EstadoReserva.ACEPTADA -> BloqueHorarioDTO(inicio, fin, ocupado = true, estado = "ACEPTADA")
                EstadoReserva.PENDIENTE -> BloqueHorarioDTO(inicio, fin, ocupado = false, estado = "PENDIENTE")
                else -> BloqueHorarioDTO(inicio, fin, ocupado = false, estado = "DISPONIBLE")
            }
        }
    }

    fun solicitarReserva(reserva: Reserva): Reserva? {
        logger.info("Profesor {} solicita sala {} el {} ({}-{})",
            reserva.profesorNombre, reserva.salaNombre, reserva.fecha, reserva.horaInicio, reserva.horaFin)

        val yaAceptada = reservaRepository.existsBySalaIdAndFechaAndHoraInicioAndEstado(
            reserva.salaId, reserva.fecha, reserva.horaInicio, EstadoReserva.ACEPTADA
        )
        if (yaAceptada) {
            logger.warn("Solicitud rechazada automáticamente: el bloque ya está ocupado.")
            return null
        }

        val nueva = reserva.copy(
            id = null,
            estado = EstadoReserva.PENDIENTE,
            motivoRechazo = null
        )
        return reservaRepository.save(nueva)
    }

    fun listarTodas(): List<Reserva> = reservaRepository.findAll()

    fun listarPendientes(): List<Reserva> {
        logger.info("Inspectoría consultando reservas pendientes")
        return reservaRepository.findByEstado(EstadoReserva.PENDIENTE)
    }

    fun listarPorProfesor(usuarioId: Int): List<Reserva> =
        reservaRepository.findByUsuarioId(usuarioId)

    fun aprobarReserva(id: Int): Reserva? {
        val reserva = reservaRepository.findById(id).orElse(null) ?: return null
        logger.info("Inspectoría ACEPTA la reserva ID: {}", id)
        val aprobada = reserva.copy(estado = EstadoReserva.ACEPTADA, motivoRechazo = null)
        return reservaRepository.save(aprobada)
    }

    fun rechazarReserva(id: Int, motivo: String): Reserva? {
        val reserva = reservaRepository.findById(id).orElse(null) ?: return null
        logger.info("Inspectoría RECHAZA la reserva ID: {}. Motivo: {}", id, motivo)
        val rechazada = reserva.copy(estado = EstadoReserva.RECHAZADA, motivoRechazo = motivo)
        return reservaRepository.save(rechazada)
    }

    fun eliminar(id: Int): Boolean {
        if (!reservaRepository.existsById(id)) return false
        reservaRepository.deleteById(id)
        return true
    }
}
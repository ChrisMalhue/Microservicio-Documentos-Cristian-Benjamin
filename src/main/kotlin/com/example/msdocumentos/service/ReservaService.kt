package com.example.msdocumentos.service

import com.example.msdocumentos.DTO.BloqueHorarioDTO
import com.example.msdocumentos.model.EstadoReserva
import com.example.msdocumentos.model.Reserva
import com.example.msdocumentos.repository.ReservaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Service
class ReservaService(
    private val reservaRepository: ReservaRepository,
    private val clock: Clock
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
        val fecha = parsearFecha(fechaStr)
        val diaSemana = fecha.dayOfWeek

        // Si es sábado o domingo no hay bloques
        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            return emptyList()
        }

        val plantillaBloques = if (diaSemana == DayOfWeek.FRIDAY) bloquesViernes else bloquesLunesAJueves
        val reservasDelDia = reservaRepository.findBySalaIdAndFecha(salaId, fechaStr)

        return plantillaBloques.map { (inicio, fin) ->
            val reservasDelBloque = reservasDelDia.filter {
                it.horaInicio == inicio && it.estado != EstadoReserva.RECHAZADA
            }
            // Si hay una ACEPTADA, esa manda sobre cualquier PENDIENTE del mismo bloque
            val reservaExistente = reservasDelBloque.find { it.estado == EstadoReserva.ACEPTADA }
                ?: reservasDelBloque.firstOrNull()

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

        validarFechaSolicitada(parsearFecha(reserva.fecha))

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

        if (reserva.estado != EstadoReserva.PENDIENTE) {
            throw ConflictoReservaException("La solicitud ya fue resuelta (${reserva.estado})")
        }
        val bloqueOcupado = reservaRepository.existsBySalaIdAndFechaAndHoraInicioAndEstado(
            reserva.salaId, reserva.fecha, reserva.horaInicio, EstadoReserva.ACEPTADA
        )
        if (bloqueOcupado) {
            throw ConflictoReservaException("Ese bloque horario ya fue aprobado para otra solicitud")
        }

        logger.info("Inspectoría ACEPTA la reserva ID: {}", id)
        val aprobada = reserva.copy(estado = EstadoReserva.ACEPTADA, motivoRechazo = null)
        return reservaRepository.save(aprobada)
    }

    fun rechazarReserva(id: Int, motivo: String): Reserva? {
        val motivoLimpio = motivo.trim()
        if (motivoLimpio.isEmpty()) {
            throw ReservaInvalidaException("Debe indicar el motivo del rechazo")
        }

        val reserva = reservaRepository.findById(id).orElse(null) ?: return null

        if (reserva.estado != EstadoReserva.PENDIENTE) {
            throw ConflictoReservaException("La solicitud ya fue resuelta (${reserva.estado})")
        }

        logger.info("Inspectoría RECHAZA la reserva ID: {}. Motivo: {}", id, motivoLimpio)
        val rechazada = reserva.copy(estado = EstadoReserva.RECHAZADA, motivoRechazo = motivoLimpio)
        return reservaRepository.save(rechazada)
    }

    fun eliminar(id: Int): Boolean {
        if (!reservaRepository.existsById(id)) return false
        reservaRepository.deleteById(id)
        return true
    }

    // ------------------------------------------------------------------ ayudas

    private fun parsearFecha(fechaStr: String): LocalDate =
        try {
            LocalDate.parse(fechaStr)
        } catch (e: DateTimeParseException) {
            throw ReservaInvalidaException("Fecha inválida, use el formato AAAA-MM-DD")
        }

    /**
     * La fecha pedida debe ser un día hábil (lunes a viernes) y no puede ser anterior
     * al siguiente día hábil después de hoy. No se consideran feriados.
     */
    private fun validarFechaSolicitada(fecha: LocalDate) {
        if (fecha.dayOfWeek == DayOfWeek.SATURDAY || fecha.dayOfWeek == DayOfWeek.SUNDAY) {
            throw ReservaInvalidaException("Solo se pueden reservar días hábiles (lunes a viernes)")
        }
        val primerDiaPermitido = siguienteDiaHabil(LocalDate.now(clock))
        if (fecha.isBefore(primerDiaPermitido)) {
            throw ReservaInvalidaException(
                "La solicitud debe hacerse con al menos un día hábil de anticipación"
            )
        }
    }

    private fun siguienteDiaHabil(desde: LocalDate): LocalDate {
        var dia = desde.plusDays(1)
        while (dia.dayOfWeek == DayOfWeek.SATURDAY || dia.dayOfWeek == DayOfWeek.SUNDAY) {
            dia = dia.plusDays(1)
        }
        return dia
    }
}
package com.example.msdocumentos.repository

import com.example.msdocumentos.model.EstadoReserva
import com.example.msdocumentos.model.Reserva
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReservaRepository : JpaRepository<Reserva, Int> {
    fun findByUsuarioId(usuarioId: Int): List<Reserva>
    fun findByEstado(estado: EstadoReserva): List<Reserva>
    fun findBySalaIdAndFecha(salaId: Int, fecha: String): List<Reserva>
    fun existsBySalaIdAndFechaAndHoraInicioAndEstado(
        salaId: Int,
        fecha: String,
        horaInicio: String,
        estado: EstadoReserva
    ): Boolean
}
package com.example.msdocumentos.model

import jakarta.persistence.*

@Entity
@Table(name = "reservas")
data class Reserva(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(nullable = false)
    val salaId: Int = 0,

    @Column(nullable = false)
    val salaNombre: String = "",

    @Column(nullable = false)
    val usuarioId: Int = 0,

    @Column(nullable = false)
    val profesorNombre: String = "",

    @Column(nullable = false)
    val fecha: String = "", // Formato "YYYY-MM-DD" ej: "2026-11-19"

    @Column(nullable = false)
    val horaInicio: String = "", // Ej: "08:30"

    @Column(nullable = false)
    val horaFin: String = "", // Ej: "09:15"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val estado: EstadoReserva = EstadoReserva.PENDIENTE,

    @Column(columnDefinition = "TEXT")
    val motivoRechazo: String? = null
)
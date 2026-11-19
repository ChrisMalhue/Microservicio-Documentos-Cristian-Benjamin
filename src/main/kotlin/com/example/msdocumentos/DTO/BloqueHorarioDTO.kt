package com.example.msdocumentos.DTO

data class BloqueHorarioDTO(
    val horaInicio: String,
    val horaFin: String,
    val ocupado: Boolean, // true = pintar en gris (ACEPTADA)
    val estado: String    // "DISPONIBLE", "PENDIENTE" o "ACEPTADA"
)
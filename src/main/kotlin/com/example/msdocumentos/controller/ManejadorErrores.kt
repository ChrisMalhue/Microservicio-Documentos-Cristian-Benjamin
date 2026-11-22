package com.example.msdocumentos.controller

import com.example.msdocumentos.service.ConflictoReservaException
import com.example.msdocumentos.service.ReservaInvalidaException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Traduce las excepciones del servicio de reservas a respuestas HTTP con un mensaje legible. */
@RestControllerAdvice
class ManejadorErrores {

    @ExceptionHandler(ReservaInvalidaException::class)
    fun reservaInvalida(e: ReservaInvalidaException): ResponseEntity<Map<String, String>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("mensaje" to (e.message ?: "Solicitud inválida")))

    @ExceptionHandler(ConflictoReservaException::class)
    fun conflictoReserva(e: ConflictoReservaException): ResponseEntity<Map<String, String>> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(mapOf("mensaje" to (e.message ?: "Conflicto con el estado de la reserva")))
}
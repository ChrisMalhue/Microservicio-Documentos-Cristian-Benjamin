package com.example.msdocumentos.controller

import com.example.msdocumentos.DTO.BloqueHorarioDTO
import com.example.msdocumentos.model.Reserva
import com.example.msdocumentos.service.ReservaService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = ["*"])
class ReservaController(
    private val reservaService: ReservaService
) {
    // Endpoint para pintar los cuadritos horarios en Android (ej: /api/reservas/bloques?salaId=1&fecha=2026-11-19)
    @GetMapping("/bloques")
    fun obtenerBloques(
        @RequestParam salaId: Int,
        @RequestParam fecha: String
    ): ResponseEntity<List<BloqueHorarioDTO>> {
        return ResponseEntity.ok(reservaService.obtenerBloquesDisponibles(salaId, fecha))
    }

    @PostMapping
    fun solicitar(@RequestBody reserva: Reserva): ResponseEntity<Any> {
        val creada = reservaService.solicitarReserva(reserva)
            ?: return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(mapOf("mensaje" to "Ese bloque horario ya está reservado"))

        return ResponseEntity.status(HttpStatus.CREATED).body(creada)
    }

    @GetMapping
    fun listarTodas(): ResponseEntity<List<Reserva>> =
        ResponseEntity.ok(reservaService.listarTodas())

    @GetMapping("/pendientes")
    fun listarPendientes(): ResponseEntity<List<Reserva>> =
        ResponseEntity.ok(reservaService.listarPendientes())

    @GetMapping("/usuario/{usuarioId}")
    fun listarPorProfesor(@PathVariable usuarioId: Int): ResponseEntity<List<Reserva>> =
        ResponseEntity.ok(reservaService.listarPorProfesor(usuarioId))

    @PutMapping("/{id}/aprobar")
    fun aprobar(@PathVariable id: Int): ResponseEntity<Reserva> {
        val aprobada = reservaService.aprobarReserva(id)
        return if (aprobada != null) ResponseEntity.ok(aprobada) else ResponseEntity.notFound().build()
    }

    @PutMapping("/{id}/rechazar")
    fun rechazar(@PathVariable id: Int, @RequestBody body: Map<String, String>): ResponseEntity<Reserva> {
        val motivo = body["motivo"] ?: ""
        val rechazada = reservaService.rechazarReserva(id, motivo)
        return if (rechazada != null) ResponseEntity.ok(rechazada) else ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{id}")
    fun eliminar(@PathVariable id: Int): ResponseEntity<Void> =
        if (reservaService.eliminar(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
}
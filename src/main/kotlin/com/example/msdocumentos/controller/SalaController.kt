package com.example.msdocumentos.controller

import com.example.msdocumentos.model.AreaSala
import com.example.msdocumentos.model.Sala
import com.example.msdocumentos.service.SalaService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/salas")
@CrossOrigin(origins = ["*"])
class SalaController(
    private val salaService: SalaService
) {
    @GetMapping
    fun listarTodas(): ResponseEntity<List<Sala>> =
        ResponseEntity.ok(salaService.listarTodas())

    @GetMapping("/area/{area}")
    fun listarPorArea(@PathVariable area: AreaSala): ResponseEntity<List<Sala>> =
        ResponseEntity.ok(salaService.listarPorArea(area))

    @PostMapping
    fun crear(@RequestBody sala: Sala): ResponseEntity<Sala> =
        ResponseEntity.status(HttpStatus.CREATED).body(salaService.crear(sala))

    @DeleteMapping("/{id}")
    fun eliminar(@PathVariable id: Int): ResponseEntity<Void> =
        if (salaService.eliminar(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
}
package com.example.msdocumentos.controller

import com.example.msdocumentos.model.Documento
import com.example.msdocumentos.service.DocumentoService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/documentos")
@CrossOrigin(origins = ["*"])
class DocumentoController(
    private val documentoService: DocumentoService
) {
    @PostMapping
    fun crear(@RequestBody documento: Documento): ResponseEntity<Documento> =
        ResponseEntity.status(HttpStatus.CREATED).body(documentoService.crear(documento))

    @GetMapping("/usuario/{usuarioId}")
    fun listarPorUsuario(@PathVariable usuarioId: Int): ResponseEntity<List<Documento>> =
        ResponseEntity.ok(documentoService.listarPorUsuario(usuarioId))

    @GetMapping("/{id}")
    fun obtenerPorId(@PathVariable id: Int): ResponseEntity<Documento> {
        val doc = documentoService.obtenerPorId(id)
        return if (doc != null) ResponseEntity.ok(doc) else ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{id}")
    fun eliminar(@PathVariable id: Int): ResponseEntity<Void> =
        if (documentoService.eliminar(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
}
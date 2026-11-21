package com.example.msdocumentos.controller

import com.example.msdocumentos.model.Documento
import com.example.msdocumentos.service.DocumentoService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/documentos")
@CrossOrigin(origins = ["*"])
class DocumentoController(
    private val documentoService: DocumentoService
) {
    @PostMapping
    fun crear(@RequestBody documento: Documento): ResponseEntity<Documento> =
        ResponseEntity.status(HttpStatus.CREATED).body(documentoService.crear(documento))

    @PostMapping("/subir", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun subirArchivo(
        @RequestParam("archivo") archivo: MultipartFile,
        @RequestParam("usuarioId") usuarioId: Int,
        @RequestParam("nombre", required = false) nombre: String?
    ): ResponseEntity<Documento> {
        val nombreFinal = nombre?.takeIf { it.isNotBlank() }
            ?: archivo.originalFilename
            ?: "documento.pdf"

        val nuevoDoc = Documento(
            usuarioId = usuarioId,
            nombre = nombreFinal,
            uri = "",
            contenido = archivo.bytes
        )

        val guardado = documentoService.crear(nuevoDoc)
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado)
    }

    @GetMapping("/{id}/descargar")
    fun descargarArchivo(@PathVariable id: Int): ResponseEntity<ByteArray> {
        val doc = documentoService.obtenerPorId(id)
        val bytes = doc?.contenido

        return if (doc != null && bytes != null) {
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${doc.nombre}\"")
                .body(bytes)
        } else {
            ResponseEntity.notFound().build()
        }
    }

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
package com.example.msdocumentos.controller

import com.example.msdocumentos.model.Documento
import com.example.msdocumentos.service.DocumentoService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** Pruebas del controller de documentos: subida, descarga y códigos HTTP (servicio simulado con MockK). */
class DocumentoControllerTest {

    private val service = mockk<DocumentoService>()
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(DocumentoController(service)).build()

    private val bytesPdf = byteArrayOf(37, 80, 68, 70, 45, 49) // "%PDF-1"

    private fun documento(id: Int? = 1, nombre: String = "horario.pdf", contenido: ByteArray? = null) =
        Documento(id = id, usuarioId = 10, nombre = nombre, uri = "", contenido = contenido)

    private fun archivo(nombreOriginal: String = "horario.pdf") =
        MockMultipartFile("archivo", nombreOriginal, "application/pdf", bytesPdf)

    // ================================================================ crear (JSON)

    @Test
    fun crearResponde201ConElDocumentoCreado() {
        // Arrange
        every { service.crear(any()) } returns documento(id = 4)
        // Act + Assert
        mockMvc.perform(
            post("/api/documentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"usuarioId":10,"nombre":"horario.pdf","uri":""}""")
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("\$.id").value(4))
            .andExpect(jsonPath("\$.nombre").value("horario.pdf"))
    }

    // ============================================================== subir archivo

    @Test
    fun subirArchivoConNombreUsaEseNombreYGuardaLosBytes() {
        // Arrange
        val enviado = slot<Documento>()
        every { service.crear(capture(enviado)) } answers { enviado.captured.copy(id = 3) }
        // Act
        mockMvc.perform(
            multipart("/api/documentos/subir")
                .file(archivo())
                .param("usuarioId", "10")
                .param("nombre", "Mi horario")
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("\$.id").value(3))
        // Assert
        assertEquals("Mi horario", enviado.captured.nombre)
        assertEquals(10, enviado.captured.usuarioId)
        assertContentEquals(bytesPdf, enviado.captured.contenido)
    }

    @Test
    fun subirArchivoSinNombreUsaElNombreDelArchivo() {
        // Arrange
        val enviado = slot<Documento>()
        every { service.crear(capture(enviado)) } answers { enviado.captured.copy(id = 3) }
        // Act
        mockMvc.perform(
            multipart("/api/documentos/subir")
                .file(archivo(nombreOriginal = "planificacion.pdf"))
                .param("usuarioId", "10")
        )
            .andExpect(status().isCreated())
        // Assert
        assertEquals("planificacion.pdf", enviado.captured.nombre)
    }

    @Test
    fun subirArchivoConNombreEnBlancoUsaElNombreDelArchivo() {
        // Arrange
        val enviado = slot<Documento>()
        every { service.crear(capture(enviado)) } answers { enviado.captured.copy(id = 3) }
        // Act
        mockMvc.perform(
            multipart("/api/documentos/subir")
                .file(archivo(nombreOriginal = "planificacion.pdf"))
                .param("usuarioId", "10")
                .param("nombre", "   ")
        )
            .andExpect(status().isCreated())
        // Assert
        assertEquals("planificacion.pdf", enviado.captured.nombre)
    }

    // ================================================================== descargar

    @Test
    fun descargarUnDocumentoConContenidoResponde200ConElPdf() {
        // Arrange
        every { service.obtenerPorId(1) } returns documento(id = 1, nombre = "horario.pdf", contenido = bytesPdf)
        // Act + Assert
        mockMvc.perform(get("/api/documentos/{id}/descargar", 1))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"horario.pdf\""))
            .andExpect(content().bytes(bytesPdf))
    }

    @Test
    fun descargarUnDocumentoSinContenidoResponde404() {
        // Arrange
        every { service.obtenerPorId(1) } returns documento(id = 1, contenido = null)
        // Act + Assert
        mockMvc.perform(get("/api/documentos/{id}/descargar", 1))
            .andExpect(status().isNotFound())
    }

    @Test
    fun descargarUnDocumentoInexistenteResponde404() {
        // Arrange
        every { service.obtenerPorId(999) } returns null
        // Act + Assert
        mockMvc.perform(get("/api/documentos/{id}/descargar", 999))
            .andExpect(status().isNotFound())
    }

    // ===================================================================== consultas

    @Test
    fun listarPorUsuarioResponde200ConLosDocumentos() {
        // Arrange
        every { service.listarPorUsuario(10) } returns listOf(documento(id = 1), documento(id = 2, nombre = "otro.pdf"))
        // Act + Assert
        mockMvc.perform(get("/api/documentos/usuario/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.length()").value(2))
    }

    @Test
    fun obtenerPorIdExistenteResponde200() {
        // Arrange
        every { service.obtenerPorId(1) } returns documento(id = 1)
        // Act + Assert
        mockMvc.perform(get("/api/documentos/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.id").value(1))
    }

    @Test
    fun obtenerPorIdInexistenteResponde404() {
        // Arrange
        every { service.obtenerPorId(999) } returns null
        // Act + Assert
        mockMvc.perform(get("/api/documentos/999"))
            .andExpect(status().isNotFound())
    }

    @Test
    fun elJsonDeUnDocumentoNoExponeElContenidoBinario() {
        // Arrange: el PDF completo no debe viajar dentro del JSON (se descarga por /descargar)
        every { service.obtenerPorId(1) } returns documento(id = 1, contenido = bytesPdf)
        // Act + Assert
        mockMvc.perform(get("/api/documentos/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.contenido").doesNotExist())
    }

    // ===================================================================== eliminar

    @Test
    fun eliminarExistenteResponde204() {
        // Arrange
        every { service.eliminar(1) } returns true
        // Act + Assert
        mockMvc.perform(delete("/api/documentos/1"))
            .andExpect(status().isNoContent())
    }

    @Test
    fun eliminarInexistenteResponde404() {
        // Arrange
        every { service.eliminar(999) } returns false
        // Act + Assert
        mockMvc.perform(delete("/api/documentos/999"))
            .andExpect(status().isNotFound())
    }
}
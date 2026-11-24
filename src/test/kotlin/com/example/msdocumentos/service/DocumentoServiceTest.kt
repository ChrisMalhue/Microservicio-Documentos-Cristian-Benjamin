package com.example.msdocumentos.service

import com.example.msdocumentos.model.Documento
import com.example.msdocumentos.repository.DocumentoRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas unitarias de DocumentoService: el repositorio se simula con MockK (sin Spring ni Neon). */
class DocumentoServiceTest {

    private val repository = mockk<DocumentoRepository>()
    private val service = DocumentoService(repository)

    private fun documento(
        id: Int? = 1,
        usuarioId: Int = 10,
        nombre: String = "horario.pdf",
        contenido: ByteArray? = null
    ) = Documento(id = id, usuarioId = usuarioId, nombre = nombre, uri = "", contenido = contenido)

    @Test
    fun crearIgnoraElIdYConservaLosDemasDatos() {
        // Arrange
        val bytes = byteArrayOf(1, 2, 3)
        val guardado = slot<Documento>()
        every { repository.save(capture(guardado)) } answers { guardado.captured.copy(id = 4) }
        // Act
        val resultado = service.crear(documento(id = 99, usuarioId = 10, nombre = "horario.pdf", contenido = bytes))
        // Assert
        assertNull(guardado.captured.id)
        assertEquals(10, guardado.captured.usuarioId)
        assertEquals("horario.pdf", guardado.captured.nombre)
        assertContentEquals(bytes, guardado.captured.contenido)
        assertEquals(4, resultado.id)
    }

    @Test
    fun listarPorUsuarioDevuelveSusDocumentos() {
        // Arrange
        every { repository.findByUsuarioId(10) } returns listOf(documento(id = 1), documento(id = 2, nombre = "planificacion.pdf"))
        // Act
        val resultado = service.listarPorUsuario(10)
        // Assert
        assertEquals(2, resultado.size)
    }

    @Test
    fun listarPorUsuarioSinDocumentosDevuelveListaVacia() {
        // Arrange
        every { repository.findByUsuarioId(77) } returns emptyList()
        // Act
        val resultado = service.listarPorUsuario(77)
        // Assert
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun obtenerPorIdExistenteDevuelveElDocumento() {
        // Arrange
        val existente = documento(id = 1)
        every { repository.findById(1) } returns Optional.of(existente)
        // Act
        val resultado = service.obtenerPorId(1)
        // Assert
        assertEquals(existente, resultado)
    }

    @Test
    fun obtenerPorIdInexistenteDevuelveNull() {
        // Arrange
        every { repository.findById(999) } returns Optional.empty()
        // Act
        val resultado = service.obtenerPorId(999)
        // Assert
        assertNull(resultado)
    }

    @Test
    fun eliminarUnDocumentoExistenteDevuelveTrueYLoBorra() {
        // Arrange
        every { repository.existsById(1) } returns true
        justRun { repository.deleteById(1) }
        // Act
        val resultado = service.eliminar(1)
        // Assert
        assertTrue(resultado)
        verify(exactly = 1) { repository.deleteById(1) }
    }

    @Test
    fun eliminarUnDocumentoInexistenteDevuelveFalseYNoBorra() {
        // Arrange
        every { repository.existsById(999) } returns false
        // Act
        val resultado = service.eliminar(999)
        // Assert
        assertFalse(resultado)
        verify(exactly = 0) { repository.deleteById(any<Int>()) }
    }
}
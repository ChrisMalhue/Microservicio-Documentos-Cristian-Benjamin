package com.example.msdocumentos.repository

import com.example.msdocumentos.model.Documento
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de integración de DocumentoRepository con una base H2 en memoria (no se usa Neon).
 * Antes de releer se hace flush + clear para que la lectura vaya a la base y no a un objeto en caché.
 */
@DataJpaTest
class DocumentoRepositoryTest(
    @Autowired private val repository: DocumentoRepository,
    @Autowired private val em: EntityManager
) {

    private fun documento(
        usuarioId: Int = 10,
        nombre: String = "horario.pdf",
        contenido: ByteArray? = null
    ) = Documento(usuarioId = usuarioId, nombre = nombre, uri = "", contenido = contenido)

    /** Escribe en la base, olvida los objetos en memoria y vuelve a leer desde la base. */
    private fun releer(id: Int): Documento? {
        em.flush()
        em.clear()
        return repository.findById(id).orElse(null)
    }

    @BeforeEach
    fun partirConTablaVacia() {
        repository.deleteAll()
    }

    @Test
    fun guardarUnDocumentoLeAsignaUnId() {
        // Act
        val guardado = repository.save(documento())
        // Assert
        assertNotNull(guardado.id)
    }

    @Test
    fun findByUsuarioIdDevuelveSoloLosDocumentosDeEseUsuario() {
        // Arrange
        repository.save(documento(usuarioId = 10, nombre = "horario.pdf"))
        repository.save(documento(usuarioId = 10, nombre = "planificacion.pdf"))
        repository.save(documento(usuarioId = 20, nombre = "otro.pdf"))
        // Act
        val resultado = repository.findByUsuarioId(10)
        // Assert
        assertEquals(2, resultado.size)
        assertTrue(resultado.all { it.usuarioId == 10 })
    }

    @Test
    fun findByUsuarioIdSinDocumentosDevuelveListaVacia() {
        // Act
        val resultado = repository.findByUsuarioId(999)
        // Assert
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun elContenidoBinarioSeGuardaYSeLeeIgualDesdeLaBase() {
        // Arrange: 2.000 bytes con todos los valores posibles de un byte
        val bytes = ByteArray(2_000) { (it % 256).toByte() }
        val guardado = repository.save(documento(contenido = bytes))
        // Act
        val leido = releer(guardado.id!!)
        // Assert
        assertContentEquals(bytes, leido?.contenido)
    }

    @Test
    fun unDocumentoSinContenidoSeGuardaConContenidoNulo() {
        // Arrange
        val guardado = repository.save(documento(contenido = null))
        // Act
        val leido = releer(guardado.id!!)
        // Assert
        assertNotNull(leido)
        assertNull(leido.contenido)
    }

    @Test
    fun eliminarPorIdBorraElDocumento() {
        // Arrange
        val guardado = repository.save(documento())
        // Act
        repository.deleteById(guardado.id!!)
        // Assert
        assertFalse(repository.existsById(guardado.id!!))
    }
}
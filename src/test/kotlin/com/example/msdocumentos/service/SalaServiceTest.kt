package com.example.msdocumentos.service

import com.example.msdocumentos.model.AreaSala
import com.example.msdocumentos.model.Sala
import com.example.msdocumentos.repository.SalaRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas unitarias de SalaService: el repositorio se simula con MockK (sin Spring ni Neon). */
class SalaServiceTest {

    private val repository = mockk<SalaRepository>()
    private val service = SalaService(repository)

    private fun sala(id: Int? = 1, nombre: String = "Sala de Clases 101", area: AreaSala = AreaSala.SALA_GENERICA) =
        Sala(id = id, nombre = nombre, area = area)

    @Test
    fun listarTodasDevuelveTodasLasSalas() {
        // Arrange
        every { repository.findAll() } returns listOf(sala(id = 1), sala(id = 2, nombre = "Biblioteca Central"))
        // Act
        val resultado = service.listarTodas()
        // Assert
        assertEquals(2, resultado.size)
    }

    @Test
    fun listarPorAreaConsultaSoloEsaArea() {
        // Arrange
        every { repository.findByArea(AreaSala.MUSICA) } returns listOf(sala(nombre = "Sala de Música 101", area = AreaSala.MUSICA))
        // Act
        val resultado = service.listarPorArea(AreaSala.MUSICA)
        // Assert
        assertEquals(1, resultado.size)
        assertEquals(AreaSala.MUSICA, resultado.first().area)
    }

    @Test
    fun crearIgnoraElIdRecibido() {
        // Arrange
        val guardada = slot<Sala>()
        every { repository.save(capture(guardada)) } answers { guardada.captured.copy(id = 8) }
        // Act
        val resultado = service.crear(sala(id = 99, nombre = "Auditorio Principal", area = AreaSala.AUDITORIO))
        // Assert
        assertNull(guardada.captured.id)
        assertEquals("Auditorio Principal", guardada.captured.nombre)
        assertEquals(8, resultado.id)
    }

    @Test
    fun actualizarCambiaNombreYAreaYConservaElId() {
        // Arrange
        every { repository.findById(1) } returns Optional.of(sala(id = 1))
        every { repository.save(any<Sala>()) } answers { firstArg<Sala>() }
        // Act
        val resultado = service.actualizar(1, sala(id = null, nombre = "Sala de Música 101", area = AreaSala.MUSICA))
        // Assert
        assertEquals(1, resultado?.id)
        assertEquals("Sala de Música 101", resultado?.nombre)
        assertEquals(AreaSala.MUSICA, resultado?.area)
    }

    @Test
    fun actualizarUnaSalaInexistenteDevuelveNullYNoGuarda() {
        // Arrange
        every { repository.findById(999) } returns Optional.empty()
        // Act
        val resultado = service.actualizar(999, sala(id = null))
        // Assert
        assertNull(resultado)
        verify(exactly = 0) { repository.save(any<Sala>()) }
    }

    @Test
    fun eliminarUnaSalaExistenteDevuelveTrueYLaBorra() {
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
    fun eliminarUnaSalaInexistenteDevuelveFalseYNoBorra() {
        // Arrange
        every { repository.existsById(999) } returns false
        // Act
        val resultado = service.eliminar(999)
        // Assert
        assertFalse(resultado)
        verify(exactly = 0) { repository.deleteById(any<Int>()) }
    }
}
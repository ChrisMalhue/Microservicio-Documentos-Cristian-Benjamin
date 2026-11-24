package com.example.msdocumentos.repository

import com.example.msdocumentos.model.AreaSala
import com.example.msdocumentos.model.Sala
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pruebas de integración de SalaRepository con una base H2 en memoria (no se usa Neon).
 * Para comprobar lo que realmente quedó en la base, se vacía la memoria de Hibernate
 * (flush + clear) antes de volver a leer; así la lectura va a la base y no a un objeto en caché.
 */
@DataJpaTest
class SalaRepositoryTest(
    @Autowired private val repository: SalaRepository,
    @Autowired private val em: EntityManager
) {

    private fun sala(nombre: String = "Sala de Clases 101", area: AreaSala = AreaSala.SALA_GENERICA) =
        Sala(nombre = nombre, area = area)

    /** Escribe en la base, olvida los objetos en memoria y vuelve a leer desde la base. */
    private fun releer(id: Int): Sala? {
        em.flush()
        em.clear()
        return repository.findById(id).orElse(null)
    }

    @BeforeEach
    fun partirConTablaVacia() {
        repository.deleteAll()
    }

    @Test
    fun guardarUnaSalaLeAsignaUnId() {
        // Act
        val guardada = repository.save(sala())
        // Assert
        assertNotNull(guardada.id)
    }

    @Test
    fun elAreaSeGuardaYSeLeeDesdeLaBase() {
        // Arrange
        val guardada = repository.save(sala(nombre = "Sala de Música 101", area = AreaSala.MUSICA))
        // Act
        val leida = releer(guardada.id!!)
        // Assert
        assertEquals(AreaSala.MUSICA, leida?.area)
        assertEquals("Sala de Música 101", leida?.nombre)
    }

    @Test
    fun findByAreaDevuelveSoloLasSalasDeEsaArea() {
        // Arrange
        repository.save(sala(nombre = "Sala de Música 101", area = AreaSala.MUSICA))
        repository.save(sala(nombre = "Sala de Música 102", area = AreaSala.MUSICA))
        repository.save(sala(nombre = "Biblioteca Central", area = AreaSala.BIBLIOTECA))
        // Act
        val resultado = repository.findByArea(AreaSala.MUSICA)
        // Assert
        assertEquals(2, resultado.size)
        assertTrue(resultado.all { it.area == AreaSala.MUSICA })
    }

    @Test
    fun findByAreaSinSalasDevuelveListaVacia() {
        // Arrange
        repository.save(sala(area = AreaSala.BIBLIOTECA))
        // Act
        val resultado = repository.findByArea(AreaSala.AUDITORIO)
        // Assert
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun eliminarPorIdBorraLaSala() {
        // Arrange
        val guardada = repository.save(sala())
        // Act
        repository.deleteById(guardada.id!!)
        // Assert
        assertFalse(repository.existsById(guardada.id!!))
    }
}
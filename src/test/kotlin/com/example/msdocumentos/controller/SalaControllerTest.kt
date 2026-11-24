package com.example.msdocumentos.controller

import com.example.msdocumentos.model.AreaSala
import com.example.msdocumentos.model.Sala
import com.example.msdocumentos.service.SalaService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/** Pruebas del controller de salas: verifican el código HTTP de cada endpoint (servicio simulado con MockK). */
class SalaControllerTest {

    private val service = mockk<SalaService>()
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(SalaController(service)).build()

    private fun sala(id: Int? = 1, nombre: String = "Sala de Clases 101", area: AreaSala = AreaSala.SALA_GENERICA) =
        Sala(id = id, nombre = nombre, area = area)

    private val jsonSala = """{"nombre":"Sala de Musica 101","area":"MUSICA"}"""

    @Test
    fun listarTodasResponde200ConLaLista() {
        // Arrange
        every { service.listarTodas() } returns listOf(sala(id = 1), sala(id = 2, nombre = "Biblioteca Central"))
        // Act + Assert
        mockMvc.perform(get("/api/salas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.length()").value(2))
            .andExpect(jsonPath("\$[1].nombre").value("Biblioteca Central"))
    }

    @Test
    fun listarPorAreaResponde200ConLasSalasDeEsaArea() {
        // Arrange
        every { service.listarPorArea(AreaSala.MUSICA) } returns listOf(sala(area = AreaSala.MUSICA))
        // Act + Assert
        mockMvc.perform(get("/api/salas/area/{area}", "MUSICA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.length()").value(1))
            .andExpect(jsonPath("\$[0].area").value("MUSICA"))
    }

    @Test
    fun listarPorAreaInexistenteResponde400() {
        // Act + Assert: "NOEXISTE" no es un valor del enum AreaSala
        mockMvc.perform(get("/api/salas/area/{area}", "NOEXISTE"))
            .andExpect(status().isBadRequest())
    }

    @Test
    fun crearResponde201ConLaSalaCreada() {
        // Arrange
        every { service.crear(any()) } returns sala(id = 8, nombre = "Sala de Musica 101", area = AreaSala.MUSICA)
        // Act + Assert
        mockMvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON).content(jsonSala))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("\$.id").value(8))
            .andExpect(jsonPath("\$.area").value("MUSICA"))
    }

    @Test
    fun actualizarExistenteResponde200() {
        // Arrange
        every { service.actualizar(1, any()) } returns sala(id = 1, nombre = "Sala de Musica 101", area = AreaSala.MUSICA)
        // Act + Assert
        mockMvc.perform(put("/api/salas/1").contentType(MediaType.APPLICATION_JSON).content(jsonSala))
            .andExpect(status().isOk())
            .andExpect(jsonPath("\$.nombre").value("Sala de Musica 101"))
    }

    @Test
    fun actualizarInexistenteResponde404() {
        // Arrange
        every { service.actualizar(999, any()) } returns null
        // Act + Assert
        mockMvc.perform(put("/api/salas/999").contentType(MediaType.APPLICATION_JSON).content(jsonSala))
            .andExpect(status().isNotFound())
    }

    @Test
    fun eliminarExistenteResponde204() {
        // Arrange
        every { service.eliminar(1) } returns true
        // Act + Assert
        mockMvc.perform(delete("/api/salas/1"))
            .andExpect(status().isNoContent())
    }

    @Test
    fun eliminarInexistenteResponde404() {
        // Arrange
        every { service.eliminar(999) } returns false
        // Act + Assert
        mockMvc.perform(delete("/api/salas/999"))
            .andExpect(status().isNotFound())
    }
}
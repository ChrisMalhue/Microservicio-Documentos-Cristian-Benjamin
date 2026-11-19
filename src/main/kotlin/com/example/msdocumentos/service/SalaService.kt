package com.example.msdocumentos.service

import com.example.msdocumentos.model.AreaSala
import com.example.msdocumentos.model.Sala
import com.example.msdocumentos.repository.SalaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SalaService(
    private val salaRepository: SalaRepository
) {
    private val logger = LoggerFactory.getLogger(SalaService::class.java)

    fun listarTodas(): List<Sala> {
        logger.info("Consultando todas las salas")
        return salaRepository.findAll()
    }

    fun listarPorArea(area: AreaSala): List<Sala> {
        logger.info("Consultando salas del área: {}", area)
        return salaRepository.findByArea(area)
    }

    fun crear(sala: Sala): Sala {
        logger.info("Creando nueva sala: {} ({})", sala.nombre, sala.area)
        return salaRepository.save(sala.copy(id = null))
    }

    fun eliminar(id: Int): Boolean {
        if (!salaRepository.existsById(id)) return false
        salaRepository.deleteById(id)
        logger.info("Sala ID: {} eliminada", id)
        return true
    }
}
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

    fun actualizar(id: Int, salaActualizada: Sala): Sala? {
        val existente = salaRepository.findById(id).orElse(null) ?: return null
        val guardada = salaRepository.save(
            existente.copy(
                nombre = salaActualizada.nombre,
                area = salaActualizada.area
            )
        )
        logger.info("Sala ID: {} actualizada a {} ({})", id, guardada.nombre, guardada.area)
        return guardada
    }

    fun eliminar(id: Int): Boolean {
        if (!salaRepository.existsById(id)) return false
        salaRepository.deleteById(id)
        logger.info("Sala ID: {} eliminada", id)
        return true
    }
}
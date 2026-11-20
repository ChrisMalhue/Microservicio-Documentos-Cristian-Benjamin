package com.example.msdocumentos.config

import com.example.msdocumentos.model.AreaSala
import com.example.msdocumentos.model.Sala
import com.example.msdocumentos.repository.SalaRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataInitializer {

    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)

    @Bean
    fun inicializarSalas(salaRepository: SalaRepository) = CommandLineRunner {
        if (salaRepository.count() == 0L) {
            val salasIniciales = listOf(
                Sala(nombre = "Sala de Clases 101", area = AreaSala.SALA_GENERICA),
                Sala(nombre = "Sala de Música 101", area = AreaSala.MUSICA),
                Sala(nombre = "Biblioteca Central", area = AreaSala.BIBLIOTECA),
                Sala(nombre = "Cancha Techada", area = AreaSala.DEPORTE),
                Sala(nombre = "Laboratorio de Ciencias", area = AreaSala.LABORATORIO),
                Sala(nombre = "Cafetería Principal", area = AreaSala.CAFETERIA),
                Sala(nombre = "Auditorio Principal", area = AreaSala.AUDITORIO)
            )
            salaRepository.saveAll(salasIniciales)
            logger.info("Se han cargado {} salas iniciales en la base de datos.", salasIniciales.size)
        } else {
            logger.info("Las salas ya existen en la base de datos.")
        }
    }
}
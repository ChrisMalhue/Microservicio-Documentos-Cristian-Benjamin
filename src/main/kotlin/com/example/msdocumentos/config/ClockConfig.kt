package com.example.msdocumentos.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

/**
 * Reloj del sistema con hora de Chile. El servidor de Render trabaja en UTC, y sin esto
 * "hoy" cambiaría de día varias horas antes que en la escuela.
 * Se inyecta en ReservaService para poder fijar la fecha en las pruebas.
 */
@Configuration
class ClockConfig {

    @Bean
    fun clock(): Clock = Clock.system(ZoneId.of("America/Santiago"))
}
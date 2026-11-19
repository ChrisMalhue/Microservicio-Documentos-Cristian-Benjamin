package com.example.msdocumentos.repository

import com.example.msdocumentos.model.AreaSala
import com.example.msdocumentos.model.Sala
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SalaRepository : JpaRepository<Sala, Int> {
    fun findByArea(area: AreaSala): List<Sala>
}
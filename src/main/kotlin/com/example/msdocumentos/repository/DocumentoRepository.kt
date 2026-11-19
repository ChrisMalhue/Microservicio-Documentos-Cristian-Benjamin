package com.example.msdocumentos.repository

import com.example.msdocumentos.model.Documento
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentoRepository : JpaRepository<Documento, Int> {
    fun findByUsuarioId(usuarioId: Int): List<Documento>
}
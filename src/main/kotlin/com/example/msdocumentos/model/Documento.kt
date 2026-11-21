package com.example.msdocumentos.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(name = "documentos")
data class Documento(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(nullable = false)
    val usuarioId: Int = 0,

    @Column(nullable = false)
    val nombre: String = "",

    @Column(columnDefinition = "TEXT")
    val uri: String = "",

    @JsonIgnore
    @Column(name = "contenido", columnDefinition = "BYTEA")
    val contenido: ByteArray? = null
)
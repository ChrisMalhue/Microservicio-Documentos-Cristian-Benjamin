package com.example.msdocumentos.model

import jakarta.persistence.*

@Entity
@Table(name = "salas")
data class Sala(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(nullable = false)
    val nombre: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val area: AreaSala = AreaSala.DEPORTE
)
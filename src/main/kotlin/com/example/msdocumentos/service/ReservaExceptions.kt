package com.example.msdocumentos.service

/** La solicitud choca con el estado actual (bloque ocupado, solicitud ya resuelta). Responde 409. */
class ConflictoReservaException(mensaje: String) : RuntimeException(mensaje)

/** Los datos enviados no cumplen una regla (fecha inválida, sin anticipación, sin motivo). Responde 400. */
class ReservaInvalidaException(mensaje: String) : RuntimeException(mensaje)
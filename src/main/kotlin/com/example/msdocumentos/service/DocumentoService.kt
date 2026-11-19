package com.example.msdocumentos.service

import com.example.msdocumentos.model.Documento
import com.example.msdocumentos.repository.DocumentoRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DocumentoService(
    private val documentoRepository: DocumentoRepository
) {
    private val logger = LoggerFactory.getLogger(DocumentoService::class.java)

    fun crear(documento: Documento): Documento {
        logger.info("Guardando documento '{}' para usuario ID: {}", documento.nombre, documento.usuarioId)
        return documentoRepository.save(documento.copy(id = null))
    }

    fun listarPorUsuario(usuarioId: Int): List<Documento> {
        logger.info("Listando documentos del usuario ID: {}", usuarioId)
        return documentoRepository.findByUsuarioId(usuarioId)
    }

    fun obtenerPorId(id: Int): Documento? = documentoRepository.findById(id).orElse(null)

    fun eliminar(id: Int): Boolean {
        if (!documentoRepository.existsById(id)) {
            logger.warn("No se encontró documento con ID: {} para eliminar", id)
            return false
        }
        documentoRepository.deleteById(id)
        logger.info("Documento ID: {} eliminado correctamente", id)
        return true
    }
}
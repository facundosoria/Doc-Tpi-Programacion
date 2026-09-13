package com.example.demo.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Servicio encargado de persistir y recuperar los archivos PDF originales en disco local.
 * Evita la pérdida de datos tras reinicios del servidor y asegura que cada documento
 * se procese con sus bytes reales sin recurrir a fallbacks involuntarios.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path storageDirectory;

    public FileStorageService() {
        this.storageDirectory = Paths.get("data", "uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
            log.info("Directorio de almacenamiento de PDFs inicializado en: {}", this.storageDirectory);
        } catch (IOException e) {
            log.error("No se pudo crear el directorio de almacenamiento de PDFs: {}", e.getMessage(), e);
        }
    }

    /**
     * Guarda los bytes de un PDF asociado al documentId.
     */
    public void savePdf(String documentId, byte[] bytes) throws IOException {
        if (documentId == null || bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("documentId y bytes son requeridos para almacenar el PDF.");
        }
        Path targetPath = resolvePath(documentId);
        Files.write(targetPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.debug("PDF persistido para el documento {}: {} bytes en {}", documentId, bytes.length, targetPath);
    }

    /**
     * Carga los bytes de un PDF dado su documentId. Retorna null si no existe.
     */
    public byte[] loadPdf(String documentId) {
        if (documentId == null) return null;
        Path targetPath = resolvePath(documentId);
        if (!Files.exists(targetPath)) {
            log.warn("No se encontró el archivo PDF para el documento ID: {} en {}", documentId, targetPath);
            return null;
        }
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            log.error("Error al leer el archivo PDF {}: {}", targetPath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Elimina el archivo PDF asociado al documentId cuando se borra la fuente.
     */
    public boolean deletePdf(String documentId) {
        if (documentId == null) return false;
        Path targetPath = resolvePath(documentId);
        try {
            return Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo PDF {}: {}", targetPath, e.getMessage());
            return false;
        }
    }

    private Path resolvePath(String documentId) {
        // Sanitizar el documentId para evitar path traversal
        String safeId = documentId.replaceAll("[^a-zA-Z0-9_-]", "_");
        return this.storageDirectory.resolve(safeId + ".pdf");
    }
}

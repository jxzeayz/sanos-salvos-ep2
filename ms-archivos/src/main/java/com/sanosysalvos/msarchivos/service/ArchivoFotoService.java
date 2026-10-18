package com.sanosysalvos.msarchivos.service;

import com.sanosysalvos.msarchivos.model.ArchivoFoto;
import com.sanosysalvos.msarchivos.repository.ArchivoFotoRepository;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ArchivoFotoService {

    private final ArchivoFotoRepository repository;
    private final MinioClient minioClient;
    private final String bucketName = "archivos";

    public ArchivoFotoService(ArchivoFotoRepository repository, MinioClient minioClient) {
        this.repository = repository;
        this.minioClient = minioClient;
    }

    public ArchivoFoto subirArchivo(MultipartFile file, Long mascotaId) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String objectName = UUID.randomUUID().toString() + extension;

            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            String urlPublica = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(60 * 60 * 24)
                            .build()
            );

            ArchivoFoto archivo = new ArchivoFoto();
            archivo.setNombreArchivo(originalFilename);
            archivo.setUrlPublica(urlPublica);
            archivo.setMimeType(file.getContentType());
            archivo.setTamanioBytes(file.getSize());
            archivo.setMascotaId(mascotaId);
            archivo.setSubidoEn(LocalDateTime.now());

            return repository.save(archivo);

        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo a MinIO: " + e.getMessage());
        }
    }

    public void eliminarArchivo(Long id) {
        ArchivoFoto archivo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));

        try {
            String objectName = extraerObjectNameDeUrl(archivo.getUrlPublica());
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            repository.delete(archivo);
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar archivo de MinIO: " + e.getMessage());
        }
    }

    public String obtenerUrlTemporal(Long id) {
        ArchivoFoto archivo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(extraerObjectNameDeUrl(archivo.getUrlPublica()))
                            .method(Method.GET)
                            .expiry(60 * 60 * 24)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al generar URL temporal: " + e.getMessage());
        }
    }

    public List<ArchivoFoto> listarArchivosPorMascota(Long mascotaId) {
        return repository.findByMascotaIdOrderBySubidoEnDesc(mascotaId);
    }

    public ArchivoFoto obtenerArchivo(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));
    }

    public InputStream descargarStream(Long id) {
        ArchivoFoto archivo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));

        try {
            String objectName = extraerObjectNameDeUrl(archivo.getUrlPublica());
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al descargar archivo de MinIO: " + e.getMessage());
        }
    }

    private String extraerObjectNameDeUrl(String url) {
        String path = url.split("\\?")[0];
        return path.substring(path.lastIndexOf("/") + 1);
    }
}
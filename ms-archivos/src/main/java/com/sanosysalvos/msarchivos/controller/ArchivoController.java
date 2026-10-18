package com.sanosysalvos.msarchivos.controller;

import com.sanosysalvos.msarchivos.model.ArchivoFoto;
import com.sanosysalvos.msarchivos.service.ArchivoFotoService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/archivos")
public class ArchivoController {

    private final ArchivoFotoService service;

    public ArchivoController(ArchivoFotoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ArchivoFoto> subirArchivo(@RequestParam("file") MultipartFile file, @RequestParam("mascotaId") Long mascotaId) {
        return ResponseEntity.ok(service.subirArchivo(file, mascotaId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarArchivo(@PathVariable Long id) {
        service.eliminarArchivo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArchivoFoto> obtenerArchivo(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerArchivo(id));
    }

    @GetMapping("/{id}/url")
    public ResponseEntity<String> obtenerUrlTemporal(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerUrlTemporal(id));
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<InputStreamResource> descargarArchivo(@PathVariable Long id) {
        ArchivoFoto archivo = service.obtenerArchivo(id);
        InputStream stream = service.descargarStream(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(archivo.getMimeType() != null ? archivo.getMimeType() : "application/octet-stream"));
        headers.setContentLength(archivo.getTamanioBytes() != null ? archivo.getTamanioBytes() : 0);
        headers.set("Cache-Control", "public, max-age=86400");

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/mascota/{mascotaId}")
    public ResponseEntity<List<ArchivoFoto>> listarArchivosPorMascota(@PathVariable Long mascotaId) {
        return ResponseEntity.ok(service.listarArchivosPorMascota(mascotaId));
    }
}
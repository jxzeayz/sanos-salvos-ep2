package com.sanosysalvos.msauditoria.controller;

import com.sanosysalvos.msauditoria.model.AuditLog;
import com.sanosysalvos.msauditoria.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> obtenerTodosLosLogs() {
        return ResponseEntity.ok(service.obtenerTodosLosLogs());
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<AuditLog> obtenerLogPorId(@PathVariable Long id) {
        return service.obtenerLogPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/logs/servicio")
    public ResponseEntity<List<AuditLog>> obtenerLogsPorServicio(@RequestParam String servicio) {
        return ResponseEntity.ok(service.obtenerLogsPorServicio(servicio));
    }

    @GetMapping("/logs/evento")
    public ResponseEntity<List<AuditLog>> obtenerLogsPorEvento(@RequestParam String evento) {
        return ResponseEntity.ok(service.obtenerLogsPorEvento(evento));
    }
}
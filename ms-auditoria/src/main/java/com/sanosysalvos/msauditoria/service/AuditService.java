package com.sanosysalvos.msauditoria.service;

import com.sanosysalvos.msauditoria.model.AuditLog;
import com.sanosysalvos.msauditoria.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public List<AuditLog> obtenerTodosLosLogs() {
        return repository.findAll();
    }

    public Optional<AuditLog> obtenerLogPorId(Long id) {
        return repository.findById(id);
    }

    public List<AuditLog> obtenerLogsPorServicio(String servicioOrigen) {
        return repository.findByServicioOrigenOrderByFechaEventoDesc(servicioOrigen);
    }

    public List<AuditLog> obtenerLogsPorEvento(String evento) {
        return repository.findByEventoOrderByFechaEventoDesc(evento);
    }

    public void guardar(AuditLog auditLog) {
        if (auditLog.getFechaEvento() == null) {
            auditLog.setFechaEvento(LocalDateTime.now());
        }
        repository.save(auditLog);
    }
}
package com.sanosysalvos.msnotificaciones.service;

import com.sanosysalvos.msnotificaciones.model.Notificacion;
import com.sanosysalvos.msnotificaciones.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository repository;

    public NotificacionService(NotificacionRepository repository) {
        this.repository = repository;
    }

    public List<Notificacion> obtenerNotificacionesPorUsuario(Long usuarioId) {
        return repository.findByUsuarioIdOrderByEnviadaEnDesc(usuarioId);
    }

    public List<Notificacion> obtenerNoLeidasPorUsuario(Long usuarioId) {
        return repository.findByUsuarioIdAndLeidaFalseOrderByEnviadaEnDesc(usuarioId);
    }

    public Notificacion obtenerNotificacionPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Notificacion no encontrada"));
    }

    public Notificacion marcarComoLeida(Long id) {
        Notificacion notificacion = obtenerNotificacionPorId(id);
        notificacion.setLeida(true);
        return repository.save(notificacion);
    }

    @Transactional
    public long marcarTodasComoLeidas(Long usuarioId) {
        List<Notificacion> noLeidas = repository.findByUsuarioIdAndLeidaFalseOrderByEnviadaEnDesc(usuarioId);
        for (Notificacion n : noLeidas) {
            n.setLeida(true);
        }
        repository.saveAll(noLeidas);
        return noLeidas.size();
    }

    public long contarNoLeidas(Long usuarioId) {
        return repository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    public void guardar(Notificacion notificacion) {
        if (notificacion.getEnviadaEn() == null) {
            notificacion.setEnviadaEn(LocalDateTime.now());
        }
        repository.save(notificacion);
    }

    public Notificacion guardarYRetornar(Notificacion notificacion) {
        if (notificacion.getEnviadaEn() == null) {
            notificacion.setEnviadaEn(LocalDateTime.now());
        }
        return repository.save(notificacion);
    }
}

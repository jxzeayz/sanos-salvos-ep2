package com.sanosysalvos.msnotificaciones.repository;

import com.sanosysalvos.msnotificaciones.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByUsuarioIdOrderByEnviadaEnDesc(Long usuarioId);
    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByEnviadaEnDesc(Long usuarioId);
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);
}
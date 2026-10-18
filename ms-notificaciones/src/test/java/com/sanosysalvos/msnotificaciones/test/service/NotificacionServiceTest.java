package com.sanosysalvos.msnotificaciones.test.service;

import com.sanosysalvos.msnotificaciones.model.Notificacion;
import com.sanosysalvos.msnotificaciones.model.TipoNotificacion;
import com.sanosysalvos.msnotificaciones.repository.NotificacionRepository;
import com.sanosysalvos.msnotificaciones.service.NotificacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository repository;

    @InjectMocks
    private NotificacionService service;

    @Test
    void testObtenerNotificacionesPorUsuario() {
        Notificacion notificacion1 = new Notificacion(1L, TipoNotificacion.Tipo.COINCIDENCIA, "Título 1", "Mensaje 1", false, LocalDateTime.now(), 1L, null);
        Notificacion notificacion2 = new Notificacion(2L, TipoNotificacion.Tipo.ALERTA, "Título 2", "Mensaje 2", true, LocalDateTime.now(), 1L, null);
        when(repository.findByUsuarioIdOrderByEnviadaEnDesc(1L)).thenReturn(Arrays.asList(notificacion1, notificacion2));

        List<Notificacion> result = service.obtenerNotificacionesPorUsuario(1L);

        assertEquals(2, result.size());
        verify(repository).findByUsuarioIdOrderByEnviadaEnDesc(1L);
    }

    @Test
    void testObtenerNotificacionPorId() {
        Notificacion notificacion = new Notificacion(1L, TipoNotificacion.Tipo.COINCIDENCIA, "Título", "Mensaje", false, LocalDateTime.now(), 1L, null);
        when(repository.findById(1L)).thenReturn(Optional.of(notificacion));

        Notificacion result = service.obtenerNotificacionPorId(1L);

        assertEquals(1L, result.getId());
        verify(repository).findById(1L);
    }

    @Test
    void testMarcarComoLeida() {
        Notificacion notificacion = new Notificacion(1L, TipoNotificacion.Tipo.COINCIDENCIA, "Título", "Mensaje", false, LocalDateTime.now(), 1L, null);
        when(repository.findById(1L)).thenReturn(Optional.of(notificacion));
        when(repository.save(any(Notificacion.class))).thenReturn(notificacion);

        Notificacion result = service.marcarComoLeida(1L);

        assertTrue(result.isLeida());
        verify(repository).save(notificacion);
    }

    @Test
    void testContarNoLeidas() {
        when(repository.countByUsuarioIdAndLeidaFalse(1L)).thenReturn(3L);

        long result = service.contarNoLeidas(1L);

        assertEquals(3L, result);
        verify(repository).countByUsuarioIdAndLeidaFalse(1L);
    }
}
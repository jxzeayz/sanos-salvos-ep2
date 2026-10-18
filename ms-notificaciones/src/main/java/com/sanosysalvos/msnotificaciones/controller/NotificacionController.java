package com.sanosysalvos.msnotificaciones.controller;

import com.sanosysalvos.msnotificaciones.model.Notificacion;
import com.sanosysalvos.msnotificaciones.model.TipoNotificacion;
import com.sanosysalvos.msnotificaciones.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Notificacion>> obtenerNotificaciones(
            @RequestParam Long usuarioId,
            @RequestParam(required = false, defaultValue = "false") boolean soloNoLeidas) {
        if (soloNoLeidas) {
            return ResponseEntity.ok(service.obtenerNoLeidasPorUsuario(usuarioId));
        }
        return ResponseEntity.ok(service.obtenerNotificacionesPorUsuario(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notificacion> obtenerNotificacion(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerNotificacionPorId(id));
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Notificacion> marcarComoLeida(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarComoLeida(id));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<Notificacion> marcarComoLeidaAlt(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarComoLeida(id));
    }

    @PatchMapping("/leer-todas")
    public ResponseEntity<Map<String, Object>> marcarTodasLeidas(@RequestParam Long usuarioId) {
        long count = service.marcarTodasComoLeidas(usuarioId);
        return ResponseEntity.ok(Map.of("mensaje", "Notificaciones marcadas como leídas", "cantidad", count));
    }

    @GetMapping("/no-leidas/count")
    public ResponseEntity<Long> contarNotificacionesNoLeidas(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(service.contarNoLeidas(usuarioId));
    }

    @PostMapping
    public ResponseEntity<Notificacion> crearNotificacion(@RequestBody Map<String, Object> body) {
        Long usuarioId = body.get("usuarioId") != null ? Long.valueOf(body.get("usuarioId").toString()) : null;
        Long origenUsuarioId = body.get("origenUsuarioId") != null ? Long.valueOf(body.get("origenUsuarioId").toString()) : null;
        String tipo = (String) body.getOrDefault("tipo", "SISTEMA");
        String titulo = (String) body.getOrDefault("titulo", "");
        String mensaje = (String) body.getOrDefault("mensaje", "");

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(usuarioId);
        notificacion.setOrigenUsuarioId(origenUsuarioId);
        notificacion.setTipo(TipoNotificacion.Tipo.valueOf(tipo));
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setLeida(false);

        return ResponseEntity.ok(service.guardarYRetornar(notificacion));
    }
}

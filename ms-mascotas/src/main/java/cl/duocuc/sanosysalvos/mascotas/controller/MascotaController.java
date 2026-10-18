package cl.duocuc.sanosysalvos.mascotas.controller;

import cl.duocuc.sanosysalvos.mascotas.dto.MascotaRequest;
import cl.duocuc.sanosysalvos.mascotas.model.EstadoMascota;
import cl.duocuc.sanosysalvos.mascotas.model.Mascota;
import cl.duocuc.sanosysalvos.mascotas.service.MascotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
public class MascotaController {

    private final MascotaService mascotaService;

    @PostMapping
    public ResponseEntity<Mascota> registrar(@Valid @RequestBody MascotaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mascotaService.registrar(request));
    }

    @GetMapping
    public ResponseEntity<List<Mascota>> listar(
            @RequestParam(required = false) EstadoMascota estado) {
        if (estado != null) {
            return ResponseEntity.ok(mascotaService.listarPorEstado(estado));
        }
        return ResponseEntity.ok(mascotaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mascota> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(mascotaService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Mascota> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody MascotaRequest request) {
        return ResponseEntity.ok(mascotaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @RequestParam Long usuarioId) {
        mascotaService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Mascota> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoMascota estado,
            @RequestParam(required = false) Long usuarioId) {
        return ResponseEntity.ok(mascotaService.actualizarEstado(id, estado, usuarioId));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Mascota>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(mascotaService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-mascotas"));
    }
}

package cl.duocuc.sanosysalvos.matching.controller;

import cl.duocuc.sanosysalvos.matching.model.Coincidencia;
import cl.duocuc.sanosysalvos.matching.model.EstadoCoincidencia;
import cl.duocuc.sanosysalvos.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    @GetMapping("/coincidencias")
    public ResponseEntity<List<Coincidencia>> listar() {
        return ResponseEntity.ok(matchingService.listarCoincidencias());
    }

    @GetMapping("/coincidencias/mascota/{mascotaId}")
    public ResponseEntity<List<Coincidencia>> listarPorMascota(@PathVariable Long mascotaId) {
        return ResponseEntity.ok(matchingService.listarPorMascota(mascotaId));
    }

    @PatchMapping("/coincidencias/{id}/estado")
    public ResponseEntity<Coincidencia> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoCoincidencia estado) {
        return ResponseEntity.ok(matchingService.actualizarEstado(id, estado));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-matching"));
    }
}

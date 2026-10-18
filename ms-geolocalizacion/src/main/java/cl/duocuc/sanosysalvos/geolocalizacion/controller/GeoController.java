package cl.duocuc.sanosysalvos.geolocalizacion.controller;

import cl.duocuc.sanosysalvos.geolocalizacion.dto.ZonaReporteRequest;
import cl.duocuc.sanosysalvos.geolocalizacion.model.ZonaReporte;
import cl.duocuc.sanosysalvos.geolocalizacion.service.GeoService;
import cl.duocuc.sanosysalvos.geolocalizacion.dto.ZonaReporteMapaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeoService geoService;

    @PostMapping("/reportes")
    public ResponseEntity<ZonaReporte> registrar(@Valid @RequestBody ZonaReporteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(geoService.registrarZona(request));
    }

    @GetMapping("/reportes")
    public ResponseEntity<List<ZonaReporteMapaResponse>> listar() {
        return ResponseEntity.ok(geoService.listarParaMapa());
    }

    @GetMapping("/reportes/radio")
    public ResponseEntity<List<ZonaReporte>> buscarEnRadio(
            @RequestParam double latitud,
            @RequestParam double longitud,
            @RequestParam(defaultValue = "2000") double radioMetros) {
        return ResponseEntity.ok(geoService.buscarEnRadio(latitud, longitud, radioMetros));
    }

    @GetMapping("/zonas-calientes")
    public ResponseEntity<List<Map<String, Object>>> zonasCalientes() {
        return ResponseEntity.ok(geoService.obtenerZonasCalientes());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-geolocalizacion"));
    }
}

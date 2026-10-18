package cl.duocuc.sanosysalvos.geolocalizacion.service;

import cl.duocuc.sanosysalvos.geolocalizacion.dto.ZonaReporteRequest;
import cl.duocuc.sanosysalvos.geolocalizacion.event.ZonaEventPublisher;
import cl.duocuc.sanosysalvos.geolocalizacion.model.ZonaReporte;
import cl.duocuc.sanosysalvos.geolocalizacion.repository.ZonaReporteRepository;
import cl.duocuc.sanosysalvos.geolocalizacion.dto.ZonaReporteMapaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoService {

    private final ZonaReporteRepository zonaReporteRepository;
    private final ZonaEventPublisher zonaEventPublisher;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final Random random = new Random();

    private static final double RADIO_ANONIMIZACION_METROS = 500.0;

    @Transactional
    public ZonaReporte registrarZona(ZonaReporteRequest req) {
        Point punto = geometryFactory.createPoint(new Coordinate(req.getLongitud(), req.getLatitud()));

        ZonaReporte zona = ZonaReporte.builder()
                .mascotaId(req.getMascotaId())
                .usuarioId(req.getUsuarioId())
                .latitud(req.getLatitud())
                .longitud(req.getLongitud())
                .ubicacion(punto)
                .tipoReporte(req.getTipoReporte())
                .descripcion(req.getDescripcion())
                .build();

        ZonaReporte saved = zonaReporteRepository.save(zona);

        zonaEventPublisher.publishZonaActualizada(saved);

        return saved;
    }

    public List<ZonaReporteMapaResponse> listarParaMapa() {
        return zonaReporteRepository.findAll()
                .stream()
                .map(zona -> {
                    double[] coordsAnonimizadas = anonimizarCoordenadas(zona.getLatitud(), zona.getLongitud());
                    return ZonaReporteMapaResponse.builder()
                            .id(zona.getId())
                            .mascotaId(zona.getMascotaId())
                            .latitud(coordsAnonimizadas[0])
                            .longitud(coordsAnonimizadas[1])
                            .tipoReporte(zona.getTipoReporte())
                            .estado(zona.getTipoReporte())
                            .descripcion(zona.getDescripcion())
                            .fechaReporte(zona.getFechaReporte())
                            .build();
                })
                .toList();
    }

    private double[] anonimizarCoordenadas(double latitud, double longitud) {
        double radioGrados = RADIO_ANONIMIZACION_METROS / 111320.0;
        double latOffset = (random.nextDouble() * 2 - 1) * radioGrados;
        double lonOffset = (random.nextDouble() * 2 - 1) * radioGrados / Math.cos(Math.toRadians(latitud));
        return new double[]{latitud + latOffset, longitud + lonOffset};
    }

    public List<ZonaReporte> buscarEnRadio(double latitud, double longitud, double radioMetros) {
        return zonaReporteRepository.findByRadio(latitud, longitud, radioMetros);
    }

    public List<ZonaReporte> listarTodas() {
        return zonaReporteRepository.findAll();
    }

    public List<Map<String, Object>> obtenerZonasCalientes() {
        return zonaReporteRepository.findZonasCalientes().stream()
                .map(row -> Map.<String, Object>of(
                        "latitud", row[0],
                        "longitud", row[1],
                        "cantidad", row[2]))
                .collect(Collectors.toList());
    }
}

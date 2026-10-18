package cl.duocuc.sanosysalvos.geolocalizacion.service;

import cl.duocuc.sanosysalvos.geolocalizacion.dto.ZonaReporteRequest;
import cl.duocuc.sanosysalvos.geolocalizacion.event.ZonaEventPublisher;
import cl.duocuc.sanosysalvos.geolocalizacion.model.ZonaReporte;
import cl.duocuc.sanosysalvos.geolocalizacion.repository.ZonaReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeoServiceTest {

    @Mock private ZonaReporteRepository zonaReporteRepository;
    @Mock private ZonaEventPublisher zonaEventPublisher;

    @InjectMocks private GeoService geoService;

    private ZonaReporteRequest request;
    private ZonaReporte zonaReporte;

    @BeforeEach
    void setUp() {
        request = new ZonaReporteRequest();
        request.setMascotaId(1L);
        request.setUsuarioId(2L);
        request.setLatitud(-33.4569);
        request.setLongitud(-70.6483);
        request.setTipoReporte("PERDIDA");
        request.setDescripcion("Mascota vista en parque O'Higgins");

        zonaReporte = ZonaReporte.builder()
                .id(1L)
                .mascotaId(1L)
                .usuarioId(2L)
                .latitud(-33.4569)
                .longitud(-70.6483)
                .tipoReporte("PERDIDA")
                .descripcion("Mascota vista en parque O'Higgins")
                .build();
    }

    @Test
    void registrarZona_guardaZonaConPuntoGeometrico() {
        when(zonaReporteRepository.save(any(ZonaReporte.class))).thenReturn(zonaReporte);

        ZonaReporte resultado = geoService.registrarZona(request);

        assertThat(resultado.getMascotaId()).isEqualTo(1L);
        assertThat(resultado.getTipoReporte()).isEqualTo("PERDIDA");
        assertThat(resultado.getLatitud()).isEqualTo(-33.4569);
        verify(zonaReporteRepository).save(any(ZonaReporte.class));
        verify(zonaEventPublisher).publishZonaActualizada(zonaReporte);
    }

    @Test
    void listarTodas_retornaTodasLasZonas() {
        ZonaReporte segunda = ZonaReporte.builder().id(2L).mascotaId(3L)
                .usuarioId(4L).latitud(-33.47).longitud(-70.65)
                .tipoReporte("ENCONTRADA").build();
        when(zonaReporteRepository.findAll()).thenReturn(List.of(zonaReporte, segunda));

        List<ZonaReporte> resultado = geoService.listarTodas();

        assertThat(resultado).hasSize(2);
    }

    @Test
    void buscarEnRadio_delegaConsultaConParametrosCorrectos() {
        when(zonaReporteRepository.findByRadio(-33.4569, -70.6483, 5000.0))
                .thenReturn(List.of(zonaReporte));

        List<ZonaReporte> resultado = geoService.buscarEnRadio(-33.4569, -70.6483, 5000.0);

        assertThat(resultado).hasSize(1);
        verify(zonaReporteRepository).findByRadio(-33.4569, -70.6483, 5000.0);
    }

    @Test
    void buscarEnRadio_sinResultados_retornaListaVacia() {
        when(zonaReporteRepository.findByRadio(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        List<ZonaReporte> resultado = geoService.buscarEnRadio(0.0, 0.0, 1000.0);

        assertThat(resultado).isEmpty();
    }

    @Test
    void obtenerZonasCalientes_retornaMapaConCamposEsperados() {
        Object[] row1 = { -33.4569, -70.6483, 8L };
        Object[] row2 = { -33.48,   -70.65,   3L };
        when(zonaReporteRepository.findZonasCalientes()).thenReturn(List.of(row1, row2));

        List<Map<String, Object>> resultado = geoService.obtenerZonasCalientes();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0)).containsKeys("latitud", "longitud", "cantidad");
        assertThat(resultado.get(0).get("latitud")).isEqualTo(-33.4569);
        assertThat(resultado.get(0).get("cantidad")).isEqualTo(8L);
    }
}

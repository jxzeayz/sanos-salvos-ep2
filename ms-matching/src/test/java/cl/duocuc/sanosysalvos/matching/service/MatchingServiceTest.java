package cl.duocuc.sanosysalvos.matching.service;

import cl.duocuc.sanosysalvos.matching.event.CoincidenciaEventPublisher;
import cl.duocuc.sanosysalvos.matching.model.Coincidencia;
import cl.duocuc.sanosysalvos.matching.model.EstadoCoincidencia;
import cl.duocuc.sanosysalvos.matching.model.MascotaSnapshot;
import cl.duocuc.sanosysalvos.matching.repository.CoincidenciaRepository;
import cl.duocuc.sanosysalvos.matching.repository.MascotaSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock private MascotaSnapshotRepository snapshotRepository;
    @Mock private CoincidenciaRepository coincidenciaRepository;
    @Mock private MatchingAlgorithm algorithm;
    @Mock private CoincidenciaEventPublisher eventPublisher;

    @InjectMocks private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(matchingService, "scoreMinimo", 0.60);
    }

    private MascotaSnapshot buildSnapshot(Long id, String estado, String especie) {
        return MascotaSnapshot.builder()
                .mascotaId(id).estado(estado).especie(especie)
                .raza("Labrador").color("dorado").tamano("GRANDE")
                .latitud(-33.45).longitud(-70.64)
                .fechaReporte(LocalDateTime.now())
                .build();
    }

    @Test
    void procesarNuevaMascota_scoreAlto_guardaCoincidencia() {
        MascotaSnapshot nueva    = buildSnapshot(1L, "PERDIDA",    "PERRO");
        MascotaSnapshot candidata = buildSnapshot(2L, "ENCONTRADA", "PERRO");
        Coincidencia coincidencia = Coincidencia.builder()
                .id(1L).mascotaPerdidaId(1L).mascotaEncontradaId(2L)
                .scoreMatch(0.85).estado(EstadoCoincidencia.PENDIENTE).build();

        when(snapshotRepository.save(any())).thenReturn(nueva);
        when(snapshotRepository.findByEspecieAndEstado("PERRO", "ENCONTRADA"))
                .thenReturn(List.of(candidata));
        when(coincidenciaRepository.existsByMascotaPerdidaIdAndMascotaEncontradaId(1L, 2L))
                .thenReturn(false);
        when(algorithm.calcularScore(any(), any())).thenReturn(0.85);
        when(coincidenciaRepository.save(any())).thenReturn(coincidencia);

        List<Coincidencia> resultado = matchingService.procesarNuevaMascota(nueva);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getScoreMatch()).isEqualTo(0.85);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoCoincidencia.PENDIENTE);
        verify(coincidenciaRepository).save(any());
    }

    @Test
    void procesarNuevaMascota_scoreBajo_noGuardaCoincidencia() {
        MascotaSnapshot nueva     = buildSnapshot(1L, "PERDIDA",    "PERRO");
        MascotaSnapshot candidata = buildSnapshot(2L, "ENCONTRADA", "PERRO");

        when(snapshotRepository.save(any())).thenReturn(nueva);
        when(snapshotRepository.findByEspecieAndEstado("PERRO", "ENCONTRADA"))
                .thenReturn(List.of(candidata));
        when(coincidenciaRepository.existsByMascotaPerdidaIdAndMascotaEncontradaId(1L, 2L))
                .thenReturn(false);
        when(algorithm.calcularScore(any(), any())).thenReturn(0.40);

        List<Coincidencia> resultado = matchingService.procesarNuevaMascota(nueva);

        assertThat(resultado).isEmpty();
        verify(coincidenciaRepository, never()).save(any());
    }

    @Test
    void procesarNuevaMascota_coincidenciaDuplicada_seOmite() {
        MascotaSnapshot nueva     = buildSnapshot(1L, "PERDIDA",    "PERRO");
        MascotaSnapshot candidata = buildSnapshot(2L, "ENCONTRADA", "PERRO");

        when(snapshotRepository.save(any())).thenReturn(nueva);
        when(snapshotRepository.findByEspecieAndEstado("PERRO", "ENCONTRADA"))
                .thenReturn(List.of(candidata));
        when(coincidenciaRepository.existsByMascotaPerdidaIdAndMascotaEncontradaId(1L, 2L))
                .thenReturn(true);

        List<Coincidencia> resultado = matchingService.procesarNuevaMascota(nueva);

        assertThat(resultado).isEmpty();
        verify(algorithm, never()).calcularScore(any(), any());
    }

    @Test
    void procesarNuevaMascota_mascotaEncontrada_buscaCandidatasPerdidas() {
        MascotaSnapshot nueva     = buildSnapshot(3L, "ENCONTRADA", "GATO");
        MascotaSnapshot candidata = buildSnapshot(4L, "PERDIDA",    "GATO");

        when(snapshotRepository.save(any())).thenReturn(nueva);
        when(snapshotRepository.findByEspecieAndEstado("GATO", "PERDIDA"))
                .thenReturn(List.of(candidata));
        when(coincidenciaRepository.existsByMascotaPerdidaIdAndMascotaEncontradaId(4L, 3L))
                .thenReturn(false);
        when(algorithm.calcularScore(any(), any())).thenReturn(0.75);
        when(coincidenciaRepository.save(any())).thenReturn(
                Coincidencia.builder().id(1L).mascotaPerdidaId(4L).mascotaEncontradaId(3L)
                        .scoreMatch(0.75).estado(EstadoCoincidencia.PENDIENTE).build());

        List<Coincidencia> resultado = matchingService.procesarNuevaMascota(nueva);

        assertThat(resultado).hasSize(1);
        // La perdida es la candidata (4L), la encontrada es la nueva (3L)
        assertThat(resultado.get(0).getMascotaPerdidaId()).isEqualTo(4L);
        assertThat(resultado.get(0).getMascotaEncontradaId()).isEqualTo(3L);
    }

    @Test
    void actualizarEstado_coincidenciaExistente_cambiaEstado() {
        Coincidencia c = Coincidencia.builder()
                .id(1L).mascotaPerdidaId(1L).mascotaEncontradaId(2L)
                .scoreMatch(0.85).estado(EstadoCoincidencia.PENDIENTE).build();

        when(coincidenciaRepository.findById(1L)).thenReturn(Optional.of(c));
        c.setEstado(EstadoCoincidencia.CONFIRMADA);
        when(coincidenciaRepository.save(c)).thenReturn(c);

        Coincidencia resultado = matchingService.actualizarEstado(1L, EstadoCoincidencia.CONFIRMADA);

        assertThat(resultado.getEstado()).isEqualTo(EstadoCoincidencia.CONFIRMADA);
    }

    @Test
    void actualizarEstado_noExistente_lanzaIllegalArgumentException() {
        when(coincidenciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchingService.actualizarEstado(99L, EstadoCoincidencia.CONFIRMADA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void listarCoincidencias_retornaTodasLasCoincidencias() {
        Coincidencia c = Coincidencia.builder().id(1L).scoreMatch(0.80)
                .estado(EstadoCoincidencia.PENDIENTE).build();
        when(coincidenciaRepository.findAll()).thenReturn(List.of(c));

        List<Coincidencia> resultado = matchingService.listarCoincidencias();

        assertThat(resultado).hasSize(1);
    }
}

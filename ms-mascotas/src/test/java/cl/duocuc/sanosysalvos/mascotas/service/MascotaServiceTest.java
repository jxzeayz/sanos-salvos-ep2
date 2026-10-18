package cl.duocuc.sanosysalvos.mascotas.service;

import cl.duocuc.sanosysalvos.mascotas.dto.MascotaRequest;
import cl.duocuc.sanosysalvos.mascotas.event.MascotaEventPublisher;
import cl.duocuc.sanosysalvos.mascotas.model.EstadoMascota;
import cl.duocuc.sanosysalvos.mascotas.model.EspecieMascota;
import cl.duocuc.sanosysalvos.mascotas.model.Mascota;
import cl.duocuc.sanosysalvos.mascotas.model.TamanoMascota;
import cl.duocuc.sanosysalvos.mascotas.repository.MascotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MascotaServiceTest {

    @Mock private MascotaRepository mascotaRepository;
    @Mock private MascotaEventPublisher eventPublisher;

    @InjectMocks private MascotaService mascotaService;

    private Mascota mascota;
    private MascotaRequest request;

    @BeforeEach
    void setUp() {
        request = new MascotaRequest();
        request.setNombre("Firulais");
        request.setEspecie(EspecieMascota.PERRO);
        request.setRaza("Labrador");
        request.setColor("dorado");
        request.setTamano(TamanoMascota.GRANDE);
        request.setEstado(EstadoMascota.PERDIDA);
        request.setLatitud(-33.4569);
        request.setLongitud(-70.6483);
        request.setUsuarioId(1L);

        mascota = Mascota.builder()
                .id(1L)
                .nombre("Firulais")
                .especie(EspecieMascota.PERRO)
                .raza("Labrador")
                .color("dorado")
                .tamano(TamanoMascota.GRANDE)
                .estado(EstadoMascota.PERDIDA)
                .latitud(-33.4569)
                .longitud(-70.6483)
                .usuarioId(1L)
                .build();
    }

    @Test
    void registrar_guardaYRetornaMascota() {
        when(mascotaRepository.save(any(Mascota.class))).thenReturn(mascota);

        Mascota resultado = mascotaService.registrar(request);

        assertThat(resultado.getNombre()).isEqualTo("Firulais");
        assertThat(resultado.getEspecie()).isEqualTo(EspecieMascota.PERRO);
        assertThat(resultado.getEstado()).isEqualTo(EstadoMascota.PERDIDA);
        verify(mascotaRepository).save(any(Mascota.class));
    }

    @Test
    void listarTodas_retornaTodasLasMascotas() {
        Mascota segunda = Mascota.builder().id(2L).nombre("Luna").especie(EspecieMascota.GATO)
                .color("negro").tamano(TamanoMascota.MEDIANO).estado(EstadoMascota.ENCONTRADA).usuarioId(2L).build();
        when(mascotaRepository.findAll()).thenReturn(List.of(mascota, segunda));

        List<Mascota> resultado = mascotaService.listarTodas();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(Mascota::getNombre)
                .containsExactly("Firulais", "Luna");
    }

    @Test
    void listarPorEstado_filtraPorEstadoIndicado() {
        when(mascotaRepository.findByEstado(EstadoMascota.PERDIDA)).thenReturn(List.of(mascota));

        List<Mascota> resultado = mascotaService.listarPorEstado(EstadoMascota.PERDIDA);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoMascota.PERDIDA);
    }

    @Test
    void obtenerPorId_existente_retornaMascota() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));

        Mascota resultado = mascotaService.obtenerPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Firulais");
    }

    @Test
    void obtenerPorId_noExistente_lanzaIllegalArgumentException() {
        when(mascotaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mascotaService.obtenerPorId(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void actualizarEstado_cambiaEstadoCorrectamente() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        Mascota reunificada = Mascota.builder().id(1L).nombre("Firulais")
                .estado(EstadoMascota.REUNIFICADA).usuarioId(1L).build();
        when(mascotaRepository.save(any(Mascota.class))).thenReturn(reunificada);

        Mascota resultado = mascotaService.actualizarEstado(1L, EstadoMascota.REUNIFICADA, null);

        assertThat(resultado.getEstado()).isEqualTo(EstadoMascota.REUNIFICADA);
        verify(mascotaRepository).save(any(Mascota.class));
    }

    @Test
    void listarPorUsuario_retornaSoloMascotasDelUsuario() {
        when(mascotaRepository.findByUsuarioId(1L)).thenReturn(List.of(mascota));

        List<Mascota> resultado = mascotaService.listarPorUsuario(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getUsuarioId()).isEqualTo(1L);
    }

    @Test
    void listarPorUsuario_usuarioSinMascotas_retornaListaVacia() {
        when(mascotaRepository.findByUsuarioId(99L)).thenReturn(List.of());

        List<Mascota> resultado = mascotaService.listarPorUsuario(99L);

        assertThat(resultado).isEmpty();
    }
}

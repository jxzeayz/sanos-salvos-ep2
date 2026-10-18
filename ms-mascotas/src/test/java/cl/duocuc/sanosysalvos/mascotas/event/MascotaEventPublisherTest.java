package cl.duocuc.sanosysalvos.mascotas.event;

import cl.duocuc.sanosysalvos.mascotas.config.MensajeriaProperties;
import cl.duocuc.sanosysalvos.mascotas.model.EstadoMascota;
import cl.duocuc.sanosysalvos.mascotas.model.Mascota;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/** Verifica que las routing keys salgan de la configuración centralizada. */
@ExtendWith(MockitoExtension.class)
class MascotaEventPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;

    private MascotaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        MensajeriaProperties props = new MensajeriaProperties();
        props.setExchange("sanos-salvos.events");
        props.getRoutingKeys().setPrefijoMascota("mascota");
        props.getRoutingKeys().setMascotaEliminada("mascota.eliminada");
        publisher = new MascotaEventPublisher(rabbitTemplate, props);
    }

    @Test
    void registrarMascotaPerdida_publicaConRoutingKeyPorEstado() {
        Mascota mascota = Mascota.builder().id(1L).usuarioId(2L).nombre("Toby").estado(EstadoMascota.PERDIDA).build();

        publisher.publishMascotaRegistrada(mascota);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("sanos-salvos.events"), eq("mascota.perdida"), captor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> evento = (Map<String, Object>) captor.getValue();
        assertThat(evento)
                .containsEntry("evento", "mascota.registrada")
                .containsEntry("servicioOrigen", "ms-mascotas")
                .containsEntry("mascotaId", 1L);
    }

    @Test
    void eliminarMascota_publicaMascotaEliminada() {
        Mascota mascota = Mascota.builder().id(3L).usuarioId(2L).estado(EstadoMascota.ENCONTRADA).build();

        publisher.publishMascotaEliminada(mascota);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("sanos-salvos.events"), eq("mascota.eliminada"), captor.capture());
    }
}

package cl.duocuc.sanosysalvos.rabbitadmin.service;

import cl.duocuc.sanosysalvos.rabbitadmin.client.RabbitManagementClient;
import cl.duocuc.sanosysalvos.rabbitadmin.config.RabbitAdminProperties;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.BindingRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.CrearColaRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.CrearExchangeRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.PublicarMensajeRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.ExchangeResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.TipoExchange;
import cl.duocuc.sanosysalvos.rabbitadmin.exception.RecursoExistenteException;
import cl.duocuc.sanosysalvos.rabbitadmin.exception.RecursoNoEncontradoException;
import cl.duocuc.sanosysalvos.rabbitadmin.exception.RecursoProtegidoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock private AmqpAdmin amqpAdmin;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private RabbitManagementClient managementClient;

    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        RabbitAdminProperties properties = new RabbitAdminProperties();
        properties.setRecursosProtegidos(List.of("sanos-salvos.events", "q.matching", "q.matching.dlq"));
        service = new RabbitAdminService(amqpAdmin, rabbitTemplate, managementClient, properties, new ObjectMapper());
    }

    private void exchangeExiste(boolean existe) {
        if (existe) {
            when(rabbitTemplate.execute(any())).thenReturn(Boolean.TRUE);
        } else {
            when(rabbitTemplate.execute(any())).thenThrow(new AmqpException("NOT_FOUND - no exchange"));
        }
    }

    // ─── Colas ──────────────────────────────────────────────────────────────

    @Test
    void crearCola_conDeadLetterYTtl_declaraColaDurableConArgumentos() {
        when(amqpAdmin.getQueueProperties("q.pruebas")).thenReturn(null);
        exchangeExiste(true);
        CrearColaRequest req = CrearColaRequest.builder()
                .nombre("q.pruebas")
                .ttlMensajesMs(60_000L)
                .maxMensajes(100)
                .deadLetterExchange("sanos-salvos.dlx")
                .deadLetterRoutingKey("q.pruebas.dlq")
                .build();

        service.crearCola(req);

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        Queue cola = captor.getValue();
        assertThat(cola.getName()).isEqualTo("q.pruebas");
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments())
                .containsEntry("x-message-ttl", 60_000L)
                .containsEntry("x-max-length", 100)
                .containsEntry("x-dead-letter-exchange", "sanos-salvos.dlx")
                .containsEntry("x-dead-letter-routing-key", "q.pruebas.dlq");
    }

    @Test
    void crearCola_existente_lanzaConflictoYNoDeclara() {
        when(amqpAdmin.getQueueProperties("q.pruebas")).thenReturn(new Properties());

        assertThatThrownBy(() -> service.crearCola(CrearColaRequest.builder().nombre("q.pruebas").build()))
                .isInstanceOf(RecursoExistenteException.class);
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void crearCola_conExchangeDeadLetterInexistente_lanzaNoEncontrado() {
        when(amqpAdmin.getQueueProperties("q.pruebas")).thenReturn(null);
        exchangeExiste(false);

        assertThatThrownBy(() -> service.crearCola(CrearColaRequest.builder()
                .nombre("q.pruebas").deadLetterExchange("no.existe").build()))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void eliminarCola_protegida_lanzaProtegidoYNoElimina() {
        assertThatThrownBy(() -> service.eliminarCola("q.matching", false, false))
                .isInstanceOf(RecursoProtegidoException.class);
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminarCola_inexistente_lanzaNoEncontrado() {
        when(amqpAdmin.getQueueProperties("q.pruebas")).thenReturn(null);

        assertThatThrownBy(() -> service.eliminarCola("q.pruebas", false, false))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarCola_existente_laElimina() {
        when(amqpAdmin.getQueueProperties("q.pruebas")).thenReturn(new Properties());

        service.eliminarCola("q.pruebas", false, false);

        verify(amqpAdmin).deleteQueue("q.pruebas");
    }

    @Test
    void eliminarCola_soloSiNoUsadaConConsumidores_lanzaConflicto() {
        when(amqpAdmin.getQueueProperties("q.pruebas")).thenReturn(new Properties());
        when(amqpAdmin.getQueueInfo("q.pruebas")).thenReturn(new QueueInformation("q.pruebas", 0, 2));

        assertThatThrownBy(() -> service.eliminarCola("q.pruebas", true, false))
                .isInstanceOf(RecursoExistenteException.class)
                .hasMessageContaining("consumidor");
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminarCola_nombreInvalido_lanzaIllegalArgument() {
        assertThatThrownBy(() -> service.eliminarCola("cola con espacios", false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void leerMensajes_cantidadFueraDeRango_lanzaIllegalArgument() {
        assertThatThrownBy(() -> service.leerMensajes("q.matching.dlq", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── Exchanges ──────────────────────────────────────────────────────────

    @Test
    void crearExchange_nuevo_declaraExchangeTopicDurable() {
        exchangeExiste(false);

        ExchangeResponse resp = service.crearExchange(CrearExchangeRequest.builder()
                .nombre("pruebas.events").tipo(TipoExchange.TOPIC).build());

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("topic");
        assertThat(captor.getValue().isDurable()).isTrue();
        assertThat(resp.nombre()).isEqualTo("pruebas.events");
    }

    @Test
    void crearExchange_existente_lanzaConflicto() {
        exchangeExiste(true);

        assertThatThrownBy(() -> service.crearExchange(CrearExchangeRequest.builder()
                .nombre("pruebas.events").tipo(TipoExchange.DIRECT).build()))
                .isInstanceOf(RecursoExistenteException.class);
        verify(amqpAdmin, never()).declareExchange(any());
    }

    @Test
    void eliminarExchange_delSistema_lanzaProtegido() {
        assertThatThrownBy(() -> service.eliminarExchange("sanos-salvos.events"))
                .isInstanceOf(RecursoProtegidoException.class);
        assertThatThrownBy(() -> service.eliminarExchange("amq.topic"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(amqpAdmin, never()).deleteExchange(anyString());
    }

    @Test
    void listarExchanges_omiteExchangesInternos() {
        when(managementClient.listarExchanges()).thenReturn(List.of(
                Map.of("name", "", "type", "direct", "durable", true, "auto_delete", false),
                Map.of("name", "amq.topic", "type", "topic", "durable", true, "auto_delete", false),
                Map.of("name", "sanos-salvos.events", "type", "topic", "durable", true, "auto_delete", false)));

        List<ExchangeResponse> resultado = service.listarExchanges();

        assertThat(resultado).extracting(ExchangeResponse::nombre).containsExactly("sanos-salvos.events");
    }

    // ─── Bindings ───────────────────────────────────────────────────────────

    @Test
    void crearBinding_conColaInexistente_lanzaNoEncontrado() {
        exchangeExiste(true);
        when(amqpAdmin.getQueueProperties("q.pruebas")).thenReturn(null);

        assertThatThrownBy(() -> service.crearBinding(new BindingRequest("sanos-salvos.events", "q.pruebas", "mascota.*")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(amqpAdmin, never()).declareBinding(any());
    }

    @Test
    void crearBinding_valido_declaraBinding() {
        exchangeExiste(true);
        when(amqpAdmin.getQueueProperties("q.pruebas")).thenReturn(new Properties());

        service.crearBinding(new BindingRequest("sanos-salvos.events", "q.pruebas", "mascota.*"));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(captor.capture());
        assertThat(captor.getValue().getExchange()).isEqualTo("sanos-salvos.events");
        assertThat(captor.getValue().getDestination()).isEqualTo("q.pruebas");
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("mascota.*");
    }

    @Test
    void eliminarBinding_deColaDelSistema_lanzaProtegido() {
        assertThatThrownBy(() -> service.eliminarBinding(new BindingRequest("sanos-salvos.events", "q.matching", "mascota.*")))
                .isInstanceOf(RecursoProtegidoException.class);
        verify(amqpAdmin, never()).removeBinding(any());
    }

    // ─── Mensajes ───────────────────────────────────────────────────────────

    @Test
    void publicarMensaje_enExchangeInexistente_lanzaNoEncontrado() {
        exchangeExiste(false);

        assertThatThrownBy(() -> service.publicarMensaje(new PublicarMensajeRequest(
                "no.existe", "mascota.perdida", Map.of("mascotaId", 1))))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}

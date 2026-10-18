package cl.duocuc.sanosysalvos.geolocalizacion.consumer;

import cl.duocuc.sanosysalvos.geolocalizacion.dto.ZonaReporteRequest;
import cl.duocuc.sanosysalvos.geolocalizacion.service.GeoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataAccessResourceFailureException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifica las decisiones de ACK / NACK / DLQ del consumidor de geolocalización. */
@ExtendWith(MockitoExtension.class)
class MascotaEventConsumerTest {

    private static final long TAG = 5L;
    private static final String EVENTO_VALIDO =
            "{\"mascotaId\":1,\"usuarioId\":2,\"latitud\":-33.45,\"longitud\":-70.66,\"tipoReporte\":\"PERDIDA\"}";

    @Mock private GeoService geoService;
    @Mock private Channel channel;

    private MascotaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new MascotaEventConsumer(geoService, new ObjectMapper());
    }

    private static Message mensaje(String json, boolean reentregado) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        props.setRedelivered(reentregado);
        props.setReceivedRoutingKey("mascota.perdida");
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    void eventoValido_registraZonaYHaceAck() throws Exception {
        consumer.procesarMascotaRegistrada(mensaje(EVENTO_VALIDO, false), channel);

        ArgumentCaptor<ZonaReporteRequest> captor = ArgumentCaptor.forClass(ZonaReporteRequest.class);
        verify(geoService).registrarZona(captor.capture());
        assertThat(captor.getValue().getTipoReporte()).isEqualTo("PERDIDA");
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void eventoSinCoordenadas_haceAckSinRegistrar() throws Exception {
        consumer.procesarMascotaRegistrada(
                mensaje("{\"mascotaId\":1,\"usuarioId\":2,\"estado\":\"PERDIDA\"}", false), channel);

        verify(geoService, never()).registrarZona(any());
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void eventoSinMascotaId_vaALaDlq() throws Exception {
        consumer.procesarMascotaRegistrada(mensaje("{\"usuarioId\":2,\"estado\":\"PERDIDA\"}", false), channel);

        verify(channel).basicNack(TAG, false, false);
        verify(geoService, never()).registrarZona(any());
    }

    @Test
    void errorRecuperable_reencolaYEnReentregaVaALaDlq() throws Exception {
        when(geoService.registrarZona(any())).thenThrow(new DataAccessResourceFailureException("BD caída"));

        consumer.procesarMascotaRegistrada(mensaje(EVENTO_VALIDO, false), channel);
        verify(channel).basicNack(TAG, false, true);

        consumer.procesarMascotaRegistrada(mensaje(EVENTO_VALIDO, true), channel);
        verify(channel).basicNack(TAG, false, false);
    }
}

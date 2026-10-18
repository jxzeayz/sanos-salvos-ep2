package com.sanosysalvos.msauditoria.test.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.sanosysalvos.msauditoria.consumer.AuditEventConsumer;
import com.sanosysalvos.msauditoria.model.AuditLog;
import com.sanosysalvos.msauditoria.service.AuditService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Verifica las decisiones de ACK / NACK / DLQ del consumidor de auditoría. */
@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    private static final long TAG = 11L;

    @Mock private AuditService auditService;
    @Mock private Channel channel;

    private AuditEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuditEventConsumer(auditService, new ObjectMapper());
    }

    private static Message mensaje(String json, String routingKey, boolean reentregado) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        props.setRedelivered(reentregado);
        props.setReceivedRoutingKey(routingKey);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    void eventoJson_seRegistraYHaceAck() throws Exception {
        String json = "{\"evento\":\"zona.actualizada\",\"servicioOrigen\":\"ms-geolocalizacion\",\"payload\":{\"zonaId\":1}}";

        consumer.procesarEvento(mensaje(json, "zona.actualizada", false), channel);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).guardar(captor.capture());
        assertThat(captor.getValue().getEvento()).isEqualTo("zona.actualizada");
        assertThat(captor.getValue().getServicioOrigen()).isEqualTo("ms-geolocalizacion");
        assertThat(captor.getValue().getPayload()).isEqualTo(json);
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void eventoSinNombre_usaLaRoutingKey() throws Exception {
        consumer.procesarEvento(mensaje("{\"mascotaId\":4}", "mascota.perdida", false), channel);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).guardar(captor.capture());
        assertThat(captor.getValue().getEvento()).isEqualTo("mascota.perdida");
        assertThat(captor.getValue().getServicioOrigen()).isEqualTo("desconocido");
    }

    @Test
    void cuerpoNoJson_vaALaDlq() throws Exception {
        consumer.procesarEvento(mensaje("texto plano", "mascota.perdida", false), channel);

        verify(channel).basicNack(TAG, false, false);
        verify(auditService, never()).guardar(any());
    }

    @Test
    void errorAlGuardar_reencolaUnaVezYLuegoDlq() throws Exception {
        doThrow(new DataAccessResourceFailureException("BD caída")).when(auditService).guardar(any());

        consumer.procesarEvento(mensaje("{\"evento\":\"x.y\"}", "x.y", false), channel);
        verify(channel).basicNack(TAG, false, true);

        consumer.procesarEvento(mensaje("{\"evento\":\"x.y\"}", "x.y", true), channel);
        verify(channel).basicNack(TAG, false, false);
    }
}

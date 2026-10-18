package cl.duocuc.sanosysalvos.rabbitadmin.service;

import cl.duocuc.sanosysalvos.rabbitadmin.client.RabbitManagementClient;
import cl.duocuc.sanosysalvos.rabbitadmin.config.RabbitAdminProperties;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.BindingRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.CrearColaRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.CrearExchangeRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.NombresRabbit;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.PublicarMensajeRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.BindingResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.ColaResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.ExchangeResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.MensajeResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.PublicacionResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.PurgaResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.exception.RecursoExistenteException;
import cl.duocuc.sanosysalvos.rabbitadmin.exception.RecursoNoEncontradoException;
import cl.duocuc.sanosysalvos.rabbitadmin.exception.RecursoProtegidoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Servicio dedicado a la administración del broker RabbitMQ.
 * <p>
 * Concentra TODA la lógica de creación, consulta y eliminación de colas,
 * exchanges y bindings. Los controladores solo invocan estos métodos de alto
 * nivel y no conocen {@link AmqpAdmin}, {@link RabbitTemplate} ni la API HTTP
 * de management.
 * <p>
 * Reglas de negocio:
 * <ul>
 *   <li>No se puede crear un recurso que ya existe (409).</li>
 *   <li>No se puede eliminar ni desvincular un recurso protegido del sistema (403).</li>
 *   <li>Operar sobre un recurso inexistente devuelve 404.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile(NombresRabbit.PATRON_NOMBRE);
    private static final String ARG_TTL = "x-message-ttl";
    private static final String ARG_MAX_LENGTH = "x-max-length";
    private static final String ARG_DLX = "x-dead-letter-exchange";
    private static final String ARG_DLK = "x-dead-letter-routing-key";
    private static final String DESTINO_COLA = "queue";

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitManagementClient managementClient;
    private final RabbitAdminProperties properties;
    private final ObjectMapper objectMapper;

    // ─── Colas ──────────────────────────────────────────────────────────────

    public List<ColaResponse> listarColas() {
        return managementClient.listarColas().stream()
                .map(q -> new ColaResponse(
                        (String) q.get("name"),
                        aLong(q.get("messages")),
                        aLong(q.get("consumers")),
                        (Boolean) q.get("durable"),
                        mapa(q.get("arguments"))))
                .toList();
    }

    public ColaResponse obtenerCola(String nombre) {
        validarNombre(nombre, "cola");
        QueueInformation info = amqpAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return new ColaResponse(info.getName(), (long) info.getMessageCount(),
                (long) info.getConsumerCount(), null, null);
    }

    public ColaResponse crearCola(CrearColaRequest req) {
        String nombre = req.getNombre().trim();
        if (colaExiste(nombre)) {
            throw new RecursoExistenteException("La cola '" + nombre + "' ya existe");
        }
        if (req.getDeadLetterExchange() != null && !exchangeExiste(req.getDeadLetterExchange())) {
            throw new RecursoNoEncontradoException(
                    "El exchange de dead-letter '" + req.getDeadLetterExchange() + "' no existe");
        }

        boolean durable = !Boolean.FALSE.equals(req.getDurable());
        QueueBuilder builder = durable ? QueueBuilder.durable(nombre) : QueueBuilder.nonDurable(nombre);
        if (Boolean.TRUE.equals(req.getExclusiva())) {
            builder.exclusive();
        }
        if (Boolean.TRUE.equals(req.getAutoEliminar())) {
            builder.autoDelete();
        }
        if (req.getTtlMensajesMs() != null) {
            builder.withArgument(ARG_TTL, req.getTtlMensajesMs());
        }
        if (req.getMaxMensajes() != null) {
            builder.withArgument(ARG_MAX_LENGTH, req.getMaxMensajes());
        }
        if (req.getDeadLetterExchange() != null) {
            builder.withArgument(ARG_DLX, req.getDeadLetterExchange());
        }
        if (req.getDeadLetterRoutingKey() != null) {
            builder.withArgument(ARG_DLK, req.getDeadLetterRoutingKey());
        }

        Queue cola = builder.build();
        amqpAdmin.declareQueue(cola);
        log.info("[ADMIN] Cola creada: {} (durable={}, argumentos={})", nombre, durable, cola.getArguments());
        return new ColaResponse(nombre, 0L, 0L, durable, cola.getArguments());
    }

    /**
     * Elimina una cola.
     *
     * @param soloSiNoUsada si es true, falla si la cola tiene consumidores
     * @param soloSiVacia   si es true, falla si la cola tiene mensajes
     */
    public void eliminarCola(String nombre, boolean soloSiNoUsada, boolean soloSiVacia) {
        validarNombre(nombre, "cola");
        verificarNoProtegido(nombre, "cola");
        if (!colaExiste(nombre)) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        if (soloSiNoUsada || soloSiVacia) {
            QueueInformation info = amqpAdmin.getQueueInfo(nombre);
            if (info != null && soloSiNoUsada && info.getConsumerCount() > 0) {
                throw new RecursoExistenteException(
                        "La cola '" + nombre + "' tiene " + info.getConsumerCount() + " consumidor(es) activos");
            }
            if (info != null && soloSiVacia && info.getMessageCount() > 0) {
                throw new RecursoExistenteException(
                        "La cola '" + nombre + "' tiene " + info.getMessageCount() + " mensaje(s) pendientes");
            }
        }
        amqpAdmin.deleteQueue(nombre);
        log.info("[ADMIN] Cola eliminada: {}", nombre);
    }

    public PurgaResponse purgarCola(String nombre) {
        validarNombre(nombre, "cola");
        if (!colaExiste(nombre)) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        int eliminados = amqpAdmin.purgeQueue(nombre);
        log.info("[ADMIN] Cola purgada: {} ({} mensajes eliminados)", nombre, eliminados);
        return new PurgaResponse(nombre, eliminados);
    }

    /** Muestra mensajes de una cola (por ejemplo una DLQ) sin consumirlos. */
    public List<MensajeResponse> leerMensajes(String nombre, int cantidad) {
        validarNombre(nombre, "cola");
        if (cantidad < 1 || cantidad > 50) {
            throw new IllegalArgumentException("cantidad debe estar entre 1 y 50");
        }
        if (!colaExiste(nombre)) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return managementClient.leerMensajesSinConsumir(nombre, cantidad).stream()
                .map(m -> new MensajeResponse(
                        (String) m.get("routing_key"),
                        (String) m.get("exchange"),
                        (Boolean) m.get("redelivered"),
                        mapa(m.get("properties")),
                        parsearPayload(m.get("payload"))))
                .toList();
    }

    // ─── Exchanges ──────────────────────────────────────────────────────────

    /** Lista los exchanges creados por el sistema o por el usuario (omite los internos amq.* y el por defecto). */
    public List<ExchangeResponse> listarExchanges() {
        return managementClient.listarExchanges().stream()
                .filter(e -> !esInterno((String) e.get("name")))
                .map(e -> new ExchangeResponse(
                        (String) e.get("name"),
                        (String) e.get("type"),
                        (Boolean) e.get("durable"),
                        (Boolean) e.get("auto_delete")))
                .toList();
    }

    public ExchangeResponse crearExchange(CrearExchangeRequest req) {
        String nombre = req.getNombre().trim();
        if (exchangeExiste(nombre)) {
            throw new RecursoExistenteException("El exchange '" + nombre + "' ya existe");
        }
        boolean durable = !Boolean.FALSE.equals(req.getDurable());
        boolean autoEliminar = Boolean.TRUE.equals(req.getAutoEliminar());

        ExchangeBuilder builder = new ExchangeBuilder(nombre, req.getTipo().getValor()).durable(durable);
        if (autoEliminar) {
            builder.autoDelete();
        }
        Exchange exchange = builder.build();
        amqpAdmin.declareExchange(exchange);
        log.info("[ADMIN] Exchange creado: {} (tipo={}, durable={})", nombre, req.getTipo().getValor(), durable);
        return new ExchangeResponse(nombre, req.getTipo().getValor(), durable, autoEliminar);
    }

    public void eliminarExchange(String nombre) {
        validarNombre(nombre, "exchange");
        verificarNoProtegido(nombre, "exchange");
        if (!exchangeExiste(nombre)) {
            throw new RecursoNoEncontradoException("El exchange '" + nombre + "' no existe");
        }
        amqpAdmin.deleteExchange(nombre);
        log.info("[ADMIN] Exchange eliminado: {}", nombre);
    }

    // ─── Bindings ───────────────────────────────────────────────────────────

    public List<BindingResponse> listarBindings() {
        return managementClient.listarBindings().stream()
                .filter(b -> !((String) b.getOrDefault("source", "")).isEmpty())
                .filter(b -> DESTINO_COLA.equals(b.get("destination_type")))
                .map(b -> new BindingResponse(
                        (String) b.get("source"),
                        (String) b.get("destination"),
                        (String) b.get("routing_key")))
                .toList();
    }

    public BindingResponse crearBinding(BindingRequest req) {
        verificarExchangeYCola(req);
        amqpAdmin.declareBinding(aBinding(req));
        log.info("[ADMIN] Binding creado: {} --[{}]--> {}", req.getExchange(), req.getRoutingKey(), req.getCola());
        return new BindingResponse(req.getExchange(), req.getCola(), req.getRoutingKey());
    }

    /** Elimina un binding. No se permite desvincular colas protegidas del sistema. */
    public void eliminarBinding(BindingRequest req) {
        verificarNoProtegido(req.getCola(), "cola");
        verificarExchangeYCola(req);
        amqpAdmin.removeBinding(aBinding(req));
        log.info("[ADMIN] Binding eliminado: {} --[{}]--> {}", req.getExchange(), req.getRoutingKey(), req.getCola());
    }

    // ─── Mensajes de prueba ─────────────────────────────────────────────────

    /** Publica un mensaje JSON de prueba (productor del administrador). */
    public PublicacionResponse publicarMensaje(PublicarMensajeRequest req) {
        if (!exchangeExiste(req.getExchange())) {
            throw new RecursoNoEncontradoException("El exchange '" + req.getExchange() + "' no existe");
        }
        rabbitTemplate.convertAndSend(req.getExchange(), req.getRoutingKey(), req.getPayload(), mensaje -> {
            mensaje.getMessageProperties().setHeader("x-origen", "ms-rabbit-admin");
            return mensaje;
        });
        log.info("[ADMIN] Mensaje de prueba publicado en {} con routing key {}", req.getExchange(), req.getRoutingKey());
        return new PublicacionResponse(req.getExchange(), req.getRoutingKey(), "PUBLICADO");
    }

    // ─── Apoyo ──────────────────────────────────────────────────────────────

    boolean colaExiste(String nombre) {
        return amqpAdmin.getQueueProperties(nombre) != null;
    }

    /**
     * Declaración pasiva: RabbitMQ responde 404 (y cierra el canal) si el
     * exchange no existe, sin crearlo.
     */
    boolean exchangeExiste(String nombre) {
        try {
            Boolean existe = rabbitTemplate.execute(channel -> {
                channel.exchangeDeclarePassive(nombre);
                return Boolean.TRUE;
            });
            return Boolean.TRUE.equals(existe);
        } catch (AmqpConnectException e) {
            throw e;
        } catch (AmqpException e) {
            return false;
        }
    }

    boolean esProtegido(String nombre) {
        return nombre == null
                || nombre.isBlank()
                || esInterno(nombre)
                || properties.getRecursosProtegidos().contains(nombre.trim());
    }

    private static boolean esInterno(String nombre) {
        return nombre == null || nombre.isEmpty() || nombre.startsWith("amq.");
    }

    private void verificarNoProtegido(String nombre, String tipo) {
        if (esProtegido(nombre)) {
            throw new RecursoProtegidoException(
                    "La " + tipo + " '" + nombre + "' es un recurso del sistema y no se puede modificar desde la API");
        }
    }

    private void verificarExchangeYCola(BindingRequest req) {
        if (!exchangeExiste(req.getExchange())) {
            throw new RecursoNoEncontradoException("El exchange '" + req.getExchange() + "' no existe");
        }
        if (!colaExiste(req.getCola())) {
            throw new RecursoNoEncontradoException("La cola '" + req.getCola() + "' no existe");
        }
    }

    private static Binding aBinding(BindingRequest req) {
        return new Binding(req.getCola(), Binding.DestinationType.QUEUE,
                req.getExchange(), req.getRoutingKey(), null);
    }

    private static void validarNombre(String nombre, String tipo) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new IllegalArgumentException("Nombre de " + tipo + " inválido: " + NombresRabbit.MENSAJE_NOMBRE);
        }
    }

    private Object parsearPayload(Object payload) {
        if (!(payload instanceof String texto)) {
            return payload;
        }
        try {
            return objectMapper.readValue(texto, Object.class);
        } catch (Exception e) {
            return texto;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object valor) {
        return valor instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static Long aLong(Object valor) {
        return valor instanceof Number n ? n.longValue() : 0L;
    }
}

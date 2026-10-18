package cl.duocuc.sanosysalvos.rabbitadmin.controller;

import cl.duocuc.sanosysalvos.rabbitadmin.dto.BindingRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.CrearColaRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.CrearExchangeRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.PublicarMensajeRequest;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.BindingResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.ColaResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.ExchangeResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.MensajeResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.PublicacionResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.PurgaResponse;
import cl.duocuc.sanosysalvos.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * API REST del administrador de RabbitMQ (solo rol ADMIN).
 *
 * <pre>
 * GET    /api/rabbit/queues                          lista colas
 * GET    /api/rabbit/queues/{nombre}                 detalle de una cola
 * POST   /api/rabbit/queues                          crea una cola          → 201
 * DELETE /api/rabbit/queues/{nombre}                 elimina una cola       → 204
 * DELETE /api/rabbit/queues/{nombre}/messages        purga sus mensajes
 * GET    /api/rabbit/queues/{nombre}/messages        lee mensajes sin consumirlos (DLQ)
 * GET    /api/rabbit/exchanges                       lista exchanges
 * POST   /api/rabbit/exchanges                       crea un exchange       → 201
 * DELETE /api/rabbit/exchanges/{nombre}              elimina un exchange    → 204
 * GET    /api/rabbit/bindings                        lista bindings
 * POST   /api/rabbit/bindings                        crea un binding        → 201
 * DELETE /api/rabbit/bindings?exchange=&amp;cola=&amp;routingKey=   elimina un binding → 204
 * POST   /api/rabbit/messages                        publica un mensaje de prueba → 202
 * </pre>
 *
 * El controlador no contiene lógica de RabbitMQ: delega todo en {@link RabbitAdminService}.
 */
@RestController
@RequestMapping("/api/rabbit")
@RequiredArgsConstructor
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    // ─── Colas ──────────────────────────────────────────────────────────────

    @GetMapping("/queues")
    public List<ColaResponse> listarColas() {
        return rabbitAdminService.listarColas();
    }

    @GetMapping("/queues/{nombre}")
    public ColaResponse obtenerCola(@PathVariable String nombre) {
        return rabbitAdminService.obtenerCola(nombre);
    }

    @PostMapping("/queues")
    public ResponseEntity<ColaResponse> crearCola(@Valid @RequestBody CrearColaRequest request) {
        ColaResponse creada = rabbitAdminService.crearCola(request);
        return ResponseEntity.created(URI.create("/api/rabbit/queues/" + creada.nombre())).body(creada);
    }

    @DeleteMapping("/queues/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCola(@PathVariable String nombre,
                             @RequestParam(defaultValue = "false") boolean ifUnused,
                             @RequestParam(defaultValue = "false") boolean ifEmpty) {
        rabbitAdminService.eliminarCola(nombre, ifUnused, ifEmpty);
    }

    @DeleteMapping("/queues/{nombre}/messages")
    public PurgaResponse purgarCola(@PathVariable String nombre) {
        return rabbitAdminService.purgarCola(nombre);
    }

    @GetMapping("/queues/{nombre}/messages")
    public List<MensajeResponse> leerMensajes(@PathVariable String nombre,
                                              @RequestParam(defaultValue = "10") int cantidad) {
        return rabbitAdminService.leerMensajes(nombre, cantidad);
    }

    // ─── Exchanges ──────────────────────────────────────────────────────────

    @GetMapping("/exchanges")
    public List<ExchangeResponse> listarExchanges() {
        return rabbitAdminService.listarExchanges();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<ExchangeResponse> crearExchange(@Valid @RequestBody CrearExchangeRequest request) {
        ExchangeResponse creado = rabbitAdminService.crearExchange(request);
        return ResponseEntity.created(URI.create("/api/rabbit/exchanges/" + creado.nombre())).body(creado);
    }

    @DeleteMapping("/exchanges/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarExchange(@PathVariable String nombre) {
        rabbitAdminService.eliminarExchange(nombre);
    }

    // ─── Bindings ───────────────────────────────────────────────────────────

    @GetMapping("/bindings")
    public List<BindingResponse> listarBindings() {
        return rabbitAdminService.listarBindings();
    }

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public BindingResponse crearBinding(@Valid @RequestBody BindingRequest request) {
        return rabbitAdminService.crearBinding(request);
    }

    /** Los datos del binding llegan como query params y se validan con el mismo DTO. */
    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarBinding(@Valid @ModelAttribute BindingRequest request) {
        rabbitAdminService.eliminarBinding(request);
    }

    // ─── Mensajes ───────────────────────────────────────────────────────────

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublicacionResponse publicarMensaje(@Valid @RequestBody PublicarMensajeRequest request) {
        return rabbitAdminService.publicarMensaje(request);
    }
}

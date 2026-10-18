package cl.duocuc.sanosysalvos.rabbitadmin.exception;

import cl.duocuc.sanosysalvos.rabbitadmin.dto.Respuestas.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Traduce las excepciones del administrador a respuestas JSON con mensajes claros.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Errores de validación de DTO (body con @Valid y query params con @ModelAttribute). */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> validacion(BindException ex) {
        List<String> detalles = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : e.toString())
                .sorted()
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getMostSpecificCause();
        String detalle = causa instanceof IllegalArgumentException ? causa.getMessage()
                : "El cuerpo de la petición no es un JSON válido o tiene tipos incorrectos";
        return respuesta(HttpStatus.BAD_REQUEST, "Cuerpo de la petición inválido", List.of(detalle));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> parametros(Exception ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetros inválidos", List.of(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> argumentoInvalido(IllegalArgumentException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(RecursoExistenteException.class)
    public ResponseEntity<ErrorResponse> conflicto(RecursoExistenteException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(RecursoProtegidoException.class)
    public ResponseEntity<ErrorResponse> protegido(RecursoProtegidoException ex) {
        return respuesta(HttpStatus.FORBIDDEN, ex.getMessage(), List.of());
    }

    @ExceptionHandler({BrokerNoDisponibleException.class, AmqpConnectException.class})
    public ResponseEntity<ErrorResponse> brokerNoDisponible(RuntimeException ex) {
        log.error("RabbitMQ no disponible: {}", ex.getMessage());
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ no está disponible en este momento", List.of());
    }

    /** Errores del broker (por ejemplo, PRECONDITION_FAILED al declarar). */
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ErrorResponse> errorBroker(AmqpException ex) {
        log.error("Error de RabbitMQ: {}", ex.getMessage(), ex);
        return respuesta(HttpStatus.BAD_GATEWAY, "RabbitMQ rechazó la operación", List.of(ex.getMessage()));
    }

    /** Ruta inexistente (404), método no permitido (405) o content-type no soportado (415). */
    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErrorResponse> errorHttp(Exception ex) {
        HttpStatusCode codigo = ((org.springframework.web.ErrorResponse) ex).getStatusCode();
        HttpStatus status = HttpStatus.valueOf(codigo.value());
        return respuesta(status, ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generico(Exception ex) {
        log.error("Error inesperado en ms-rabbit-admin", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }

    private static ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String mensaje, List<String> detalles) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje, detalles));
    }
}

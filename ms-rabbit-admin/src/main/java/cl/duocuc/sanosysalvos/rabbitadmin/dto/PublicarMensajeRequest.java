package cl.duocuc.sanosysalvos.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Mensaje de prueba para verificar rutas de mensajería (por ejemplo, enviar
 * un evento inválido y comprobar que termina en la DLQ del consumidor).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicarMensajeRequest {

    @NotBlank(message = "el exchange es obligatorio")
    @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = "exchange " + NombresRabbit.MENSAJE_NOMBRE)
    private String exchange;

    @NotBlank(message = "la routingKey es obligatoria")
    @Pattern(regexp = "^[A-Za-z0-9._:-]{1,255}$",
            message = "routingKey de publicación solo admite letras, números, '.', '_', '-' y ':' (sin comodines)")
    private String routingKey;

    @NotNull(message = "el payload es obligatorio")
    @Size(min = 1, max = 50, message = "el payload debe tener entre 1 y 50 campos")
    private Map<String, Object> payload;
}

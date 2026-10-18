package cl.duocuc.sanosysalvos.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Identifica un binding exchange → cola. Se usa como body en POST /bindings
 * y como query params en DELETE /bindings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindingRequest {

    @NotBlank(message = "el exchange es obligatorio")
    @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = "exchange " + NombresRabbit.MENSAJE_NOMBRE)
    private String exchange;

    @NotBlank(message = "la cola es obligatoria")
    @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = "cola " + NombresRabbit.MENSAJE_NOMBRE)
    private String cola;

    @NotNull(message = "la routingKey es obligatoria (puede ser vacía para exchanges fanout)")
    @Pattern(regexp = NombresRabbit.PATRON_ROUTING_KEY, message = "routingKey " + NombresRabbit.MENSAJE_ROUTING_KEY)
    private String routingKey;
}

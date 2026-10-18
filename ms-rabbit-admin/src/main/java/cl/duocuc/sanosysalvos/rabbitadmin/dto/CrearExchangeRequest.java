package cl.duocuc.sanosysalvos.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos para crear un exchange. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearExchangeRequest {

    @NotBlank(message = "el nombre del exchange es obligatorio")
    @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = "nombre " + NombresRabbit.MENSAJE_NOMBRE)
    private String nombre;

    @NotNull(message = "el tipo es obligatorio (direct, topic, fanout o headers)")
    private TipoExchange tipo;

    /** Por defecto true. */
    private Boolean durable;

    /** Por defecto false. */
    private Boolean autoEliminar;
}

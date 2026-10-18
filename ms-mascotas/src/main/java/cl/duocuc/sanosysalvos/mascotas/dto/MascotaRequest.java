package cl.duocuc.sanosysalvos.mascotas.dto;

import cl.duocuc.sanosysalvos.mascotas.model.EstadoMascota;
import cl.duocuc.sanosysalvos.mascotas.model.EspecieMascota;
import cl.duocuc.sanosysalvos.mascotas.model.TamanoMascota;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MascotaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "La especie es obligatoria")
    private EspecieMascota especie;

    private String raza;

    @NotBlank(message = "El color es obligatorio")
    private String color;

    private String descripcion;

    @NotNull(message = "El tamaño es obligatorio")
    private TamanoMascota tamano;

    @NotNull(message = "El estado es obligatorio")
    private EstadoMascota estado;

    private String fotoUrl;

    private Double latitud;
    private Double longitud;

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId;
}

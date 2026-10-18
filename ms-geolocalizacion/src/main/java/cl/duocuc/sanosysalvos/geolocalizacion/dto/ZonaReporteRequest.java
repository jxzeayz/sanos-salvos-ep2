package cl.duocuc.sanosysalvos.geolocalizacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ZonaReporteRequest {

    @NotNull
    private Long mascotaId;

    @NotNull
    private Long usuarioId;

    @NotNull
    private Double latitud;

    @NotNull
    private Double longitud;

    @NotBlank
    private String tipoReporte;

    private String descripcion;
}

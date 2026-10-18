package cl.duocuc.sanosysalvos.geolocalizacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZonaReporteMapaResponse {

    private Long id;
    private Long mascotaId;

    private Double latitud;
    private Double longitud;

    private String tipoReporte;
    private String estado;
    private String descripcion;

    private LocalDateTime fechaReporte;
}
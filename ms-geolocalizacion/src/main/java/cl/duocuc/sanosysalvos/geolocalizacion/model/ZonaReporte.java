package cl.duocuc.sanosysalvos.geolocalizacion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zonas_reporte")
public class ZonaReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long mascotaId;

    @Column(nullable = false)
    private Long usuarioId;

    @JsonIgnore
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point ubicacion;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @Column(nullable = false)
    private String tipoReporte;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaReporte;

    @PrePersist
    void prePersist() {
        this.fechaReporte = LocalDateTime.now();
    }
}

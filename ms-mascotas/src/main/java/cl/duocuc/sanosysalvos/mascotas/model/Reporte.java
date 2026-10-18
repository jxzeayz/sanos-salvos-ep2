package cl.duocuc.sanosysalvos.mascotas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reportes")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoReporte tipo;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Long mascotaId;

    @Column(nullable = false)
    private Long usuarioId;

    private String zona;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaReporte;

    @PrePersist
    void prePersist() {
        this.fechaReporte = LocalDateTime.now();
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaReporte = fechaHora;
    }

    public LocalDateTime getFechaHora() {
        return this.fechaReporte;
    }
}

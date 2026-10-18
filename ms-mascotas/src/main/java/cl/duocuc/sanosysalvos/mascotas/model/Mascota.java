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
@Table(name = "mascotas")
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EspecieMascota especie;

    private String raza;

    @Column(nullable = false)
    private String color;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private TamanoMascota tamano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMascota estado;

    private Double latitud;
    private Double longitud;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaReporte;

    private LocalDateTime fechaActualizacion;

    private String fotoUrl;

    @PrePersist
    void prePersist() {
        this.fechaReporte = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    public void agregarFoto(String url) {
        this.fotoUrl = url;
    }
}

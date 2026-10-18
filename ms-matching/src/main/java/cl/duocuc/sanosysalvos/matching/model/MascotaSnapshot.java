package cl.duocuc.sanosysalvos.matching.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Snapshot local de mascotas recibidas desde RabbitMQ para el motor de matching.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mascotas_snapshot")
public class MascotaSnapshot {

    @Id
    private Long mascotaId;

    private Long usuarioId;
    private String nombre;
    private String especie;
    private String raza;
    private String color;
    private String tamano;
    private String descripcion;
    private String estado;
    private Double latitud;
    private Double longitud;
    private LocalDateTime fechaReporte;
}

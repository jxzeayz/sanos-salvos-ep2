package cl.duocuc.sanosysalvos.matching.model;

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
@Table(name = "coincidencias")
public class Coincidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long mascotaPerdidaId;

    @Column(nullable = false)
    private Long mascotaEncontradaId;

    @Column(nullable = false)
    private Long usuarioIdPerdida;

    private Long usuarioIdEncontrada;

    @Column(nullable = false)
    private Double scoreMatch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCoincidencia estado;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaDeteccion;

    @PrePersist
    void prePersist() {
        this.fechaDeteccion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoCoincidencia.PENDIENTE;
        }
    }

    public void confirmar() {
        this.estado = EstadoCoincidencia.CONFIRMADA;
    }

    public void rechazar() {
        this.estado = EstadoCoincidencia.RECHAZADA;
    }

    public void notificar() {
        // Publicar evento a q.notificaciones
    }
}

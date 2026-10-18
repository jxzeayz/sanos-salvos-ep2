package com.sanosysalvos.msnotificaciones.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoNotificacion.Tipo tipo;

    private String titulo;

    private String mensaje;

    private boolean leida = false;

    private LocalDateTime enviadaEn;

    private Long usuarioId;

    private Long origenUsuarioId;
}
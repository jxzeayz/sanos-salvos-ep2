package com.sanosysalvos.msnotificaciones.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tipos_notificacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private Tipo tipo;

    public enum Tipo {
        COINCIDENCIA, ALERTA, SISTEMA, CONTACTO
    }
}
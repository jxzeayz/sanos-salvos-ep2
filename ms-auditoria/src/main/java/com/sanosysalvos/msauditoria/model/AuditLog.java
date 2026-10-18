package com.sanosysalvos.msauditoria.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String evento;

    private String servicioOrigen;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private LocalDateTime fechaEvento;
}
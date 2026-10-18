package com.sanosysalvos.msarchivos.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "archivos_foto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchivoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreArchivo;

    @Column(columnDefinition = "TEXT")
    private String urlPublica;

    private String mimeType;

    private Long tamanioBytes;

    private Long mascotaId;

    private LocalDateTime subidoEn;
}
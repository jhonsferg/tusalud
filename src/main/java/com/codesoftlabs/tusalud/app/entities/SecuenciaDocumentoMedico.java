package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "secuencias_documentos_medicos",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_secuencias_documentos_medicos",
                        columnNames = {"tipo", "anio"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SecuenciaDocumentoMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false)
    private int ultimo;
}

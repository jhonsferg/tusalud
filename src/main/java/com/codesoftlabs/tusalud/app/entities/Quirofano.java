package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quirofanos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quirofano {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 150)
    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoQuirofano estado =
            com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoQuirofano.DISPONIBLE;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(length = 300)
    private String observaciones;
}

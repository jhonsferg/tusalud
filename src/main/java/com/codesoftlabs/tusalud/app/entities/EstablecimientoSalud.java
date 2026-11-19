package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.NivelEstablecimiento;
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
@Table(name = "establecimientos_salud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstablecimientoSalud {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo_ipress", length = 20)
    private String codigoIpress;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NivelEstablecimiento nivel;

    @Column(length = 100)
    private String distrito;

    @Column(length = 150)
    private String contacto;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}

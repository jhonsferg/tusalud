package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.NaturalezaCuenta;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaContable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cuentas_contables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaContable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoCuentaContable tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NaturalezaCuenta naturaleza;

    @Column(name = "acepta_movimientos", nullable = false)
    private boolean aceptaMovimientos;

    @Column(nullable = false)
    private boolean activa;

    @Column(nullable = false)
    private boolean sistema;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;
}

package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.AccionRetencion;
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
@Table(name = "politicas_retencion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoliticaRetencion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tipo_dato", nullable = false, length = 30)
    private String tipoDato;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 400)
    private String descripcion;

    @Column(name = "plazo_dias", nullable = false)
    private int plazoDias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AccionRetencion accion;

    @Column(name = "base_legal", nullable = false, length = 250)
    private String baseLegal;

    @Column(nullable = false)
    private boolean activa;

    @Column(name = "ultima_ejecucion")
    private LocalDateTime ultimaEjecucion;
}

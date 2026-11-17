package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "medicos",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_medicos_colegiatura", columnNames = "colegiatura"),
            @UniqueConstraint(name = "uk_medicos_usuario", columnNames = "usuario_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String colegiatura;

    @Column(length = 50)
    private String consultorio;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "medico_especialidades",
            joinColumns = @JoinColumn(name = "medico_id"),
            inverseJoinColumns = @JoinColumn(name = "especialidad_id"))
    @OrderBy("nombre ASC")
    @Builder.Default
    private Set<Especialidad> especialidades = new LinkedHashSet<>();

    @OneToMany(mappedBy = "medico", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HorarioAtencion> horarios = new ArrayList<>();

    public void agregarHorario(HorarioAtencion horario) {
        horarios.add(horario);
        horario.setMedico(this);
    }

    public void quitarHorario(HorarioAtencion horario) {
        horarios.remove(horario);
        horario.setMedico(null);
    }
}

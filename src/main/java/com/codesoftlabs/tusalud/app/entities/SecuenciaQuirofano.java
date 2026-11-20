package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "secuencias_quirofano")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SecuenciaQuirofano {

    @jakarta.persistence.Id
    @Column(length = 20)
    private String nombre;

    @Column(nullable = false)
    private long ultimo;
}

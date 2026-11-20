package com.codesoftlabs.tusalud.app.services.quirofano;

import com.codesoftlabs.tusalud.app.entities.SecuenciaQuirofano;
import com.codesoftlabs.tusalud.app.repositories.SecuenciaQuirofanoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SecuenciasQuirofano {

    public static final String CIRUGIA = "CIRUGIA";
    public static final String CICLO = "CICLO";
    public static final String LOTE = "LOTE";

    private final SecuenciaQuirofanoRepository repositorio;

    @Transactional
    public long siguiente(String nombre) {
        SecuenciaQuirofano secuencia = repositorio
                .bloquear(nombre)
                .orElseGet(() -> repositorio.saveAndFlush(new SecuenciaQuirofano(nombre, 0)));
        secuencia.setUltimo(secuencia.getUltimo() + 1);
        repositorio.saveAndFlush(secuencia);
        return secuencia.getUltimo();
    }

    public String codigo(String prefijo, String nombre) {
        return prefijo + "-" + String.format("%06d", siguiente(nombre));
    }
}

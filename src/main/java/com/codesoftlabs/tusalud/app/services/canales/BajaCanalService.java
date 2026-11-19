package com.codesoftlabs.tusalud.app.services.canales;

import com.codesoftlabs.tusalud.app.canales.FirmasCanales;
import com.codesoftlabs.tusalud.app.config.CanalesProperties;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BajaCanalService {

    private final ContactosCanalService contactos;
    private final CanalesProperties propiedades;

    public Canal baja(String token) {
        FirmasCanales.TokenBaja datos = FirmasCanales.leerTokenBaja(propiedades.secreto(), token)
                .orElseThrow(() -> new DatosInvalidosException("El enlace de baja no es válido"));
        contactos.darDeBaja(datos.usuarioId(), datos.canal());
        log.info("Baja del canal {} para el usuario {}", datos.canal(), datos.usuarioId());
        return datos.canal();
    }
}

package com.codesoftlabs.tusalud.app.documentos;

import com.codesoftlabs.tusalud.app.entities.PlantillaDocumento;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.PlantillaDocumentoRepository;
import com.codesoftlabs.tusalud.app.storage.StorageService;
import com.codesoftlabs.tusalud.app.storage.StorageService.ObjetoAlmacenado;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProveedorPlantillasImpl implements ProveedorPlantillas {

    private final PlantillaDocumentoRepository plantillaRepository;
    private final StorageService almacen;

    @Override
    @Transactional(readOnly = true)
    public String claveActiva(String codigo) {
        return plantillaRepository
                .findActiva(codigo)
                .filter(plantilla -> plantilla.getArchivo() != null && !plantilla.isRestauracion())
                .map(plantilla -> plantilla.getId().toString())
                .orElse(CLAVE_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] contenido(String codigo) {
        Optional<PlantillaDocumento> activa = plantillaRepository
                .findActiva(codigo)
                .filter(plantilla -> plantilla.getArchivo() != null && !plantilla.isRestauracion());
        if (activa.isEmpty()) {
            return contenidoPorDefecto(codigo);
        }
        try (ObjetoAlmacenado objeto = almacen.abrir(
                activa.get().getArchivo().getBucket(), activa.get().getArchivo().getClave())) {
            return objeto.flujo().readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer la plantilla " + codigo, ex);
        }
    }

    @Override
    public byte[] contenidoPorDefecto(String codigo) {
        try (InputStream entrada = getClass().getResourceAsStream("/reportes/" + codigo + ".jrxml")) {
            if (entrada == null) {
                throw new ResourceNotFoundException("Plantilla", "código", codigo);
            }
            return entrada.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer la plantilla por defecto " + codigo, ex);
        }
    }
}

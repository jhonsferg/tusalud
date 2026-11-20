package com.codesoftlabs.tusalud.app.services.receta;

import com.codesoftlabs.tusalud.app.entities.Atencion;
import java.util.UUID;

public interface RecetaDispensacionPuerto {

    void validar(Atencion atencion, int posicion, int cantidad);

    boolean registrar(Atencion atencion, int posicion, int cantidad, UUID dispensacionId, UUID usuarioId);

    void revertir(UUID dispensacionId);
}

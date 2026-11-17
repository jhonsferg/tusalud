package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.UUID;

public interface AuditoriaClinicaService {

    String HISTORIA_CONSULTADA = "HISTORIA_CONSULTADA";
    String DATOS_MEDICOS_ACTUALIZADOS = "DATOS_MEDICOS_ACTUALIZADOS";
    String IMAGENES_CONSULTADAS = "IMAGENES_CONSULTADAS";

    void registrar(UsuarioPrincipal actor, UUID pacienteId, String accion, String detalle);
}

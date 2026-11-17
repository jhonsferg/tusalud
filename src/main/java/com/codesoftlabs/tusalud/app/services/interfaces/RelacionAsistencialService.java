package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.UUID;

public interface RelacionAsistencialService {

    boolean tieneRelacion(UsuarioPrincipal medico, UUID pacienteId);

    void exigirRelacion(UsuarioPrincipal medico, UUID pacienteId, String mensaje);
}

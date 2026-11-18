package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.ExportacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ExportacionResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService.Descarga;
import java.util.List;
import java.util.UUID;

public interface ExportacionService {

    ExportacionResponse solicitar(UsuarioPrincipal actor, ExportacionRequest request);

    List<ExportacionResponse> mias(UsuarioPrincipal actor);

    ExportacionResponse obtener(UsuarioPrincipal actor, UUID id);

    Descarga descargar(UsuarioPrincipal actor, UUID id);

    void eliminar(UsuarioPrincipal actor, UUID id);

    void purgar();
}

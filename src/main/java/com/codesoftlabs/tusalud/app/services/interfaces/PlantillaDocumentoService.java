package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.PlantillaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlantillaVersionResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;

public interface PlantillaDocumentoService {

    record PlantillaContenido(byte[] contenido, String nombre) {}

    List<PlantillaResponse> listar();

    PlantillaContenido descargar(String codigo);

    PlantillaResponse reemplazar(UsuarioPrincipal actor, String codigo, byte[] contenido, String motivo);

    PlantillaResponse restaurar(UsuarioPrincipal actor, String codigo, String motivo);

    List<PlantillaVersionResponse> historial(String codigo);
}

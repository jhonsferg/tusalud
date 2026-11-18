package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.documentos.DocumentoGenerado;
import com.codesoftlabs.tusalud.app.documentos.FormatoDocumento;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.Map;
import java.util.UUID;

public interface DocumentoService {

    DocumentoGenerado generarDirecto(UsuarioPrincipal actor, TipoDocumento tipo, UUID id);

    DocumentoGenerado generar(
            UsuarioPrincipal actor, TipoDocumento tipo, FormatoDocumento formato, Map<String, String> parametros);

    void exigirPermiso(UsuarioPrincipal actor, TipoDocumento tipo, FormatoDocumento formato);
}

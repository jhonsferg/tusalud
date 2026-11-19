package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.Cie10FrecuenteResponse;
import com.codesoftlabs.tusalud.app.dto.response.Cie10ImportacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.Cie10Response;
import com.codesoftlabs.tusalud.app.dto.response.Cie10ResumenResponse;
import com.codesoftlabs.tusalud.app.entities.Cie10Catalogo;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface Cie10Service {

    List<Cie10Response> buscar(String consulta, int limite);

    Cie10Response obtener(String codigo);

    List<Cie10FrecuenteResponse> frecuentes(UsuarioPrincipal medico, int limite);

    Cie10Catalogo exigirActivo(String codigo);

    Cie10ResumenResponse resumen();

    byte[] plantillaCsv();

    Cie10ImportacionResponse importar(
            MultipartFile archivo,
            String etiqueta,
            boolean aplicar,
            boolean desactivarAusentes,
            boolean omitirErrores,
            UsuarioPrincipal administrador);
}

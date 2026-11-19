package com.codesoftlabs.tusalud.app.services.privacidad;

import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.AccesoDatosResponse;
import com.codesoftlabs.tusalud.app.entities.AuditoriaClinica;
import com.codesoftlabs.tusalud.app.repositories.AuditoriaClinicaRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccesosDatosService {

    private static final int LIMITE = 200;
    private static final Map<String, String> ETIQUETAS = Map.of(
            "HISTORIA_CONSULTADA", "Consulta de su historia clínica",
            "DATOS_MEDICOS_ACTUALIZADOS", "Actualización de sus datos médicos",
            "IMAGENES_CONSULTADAS", "Consulta de sus imágenes diagnósticas",
            "DOCUMENTO_CARGADO", "Carga de un documento",
            "DOCUMENTO_CONSULTADO", "Consulta de un documento",
            "DOCUMENTO_GENERADO", "Generación de un documento");

    private final AuditoriaClinicaRepository auditoria;
    private final PacienteRepository pacientes;

    public List<AccesoDatosResponse> misAccesos(UsuarioPrincipal actor) {
        return pacientes
                .findByUsuarioId(actor.getId())
                .map(p -> auditoria.findByPacienteIdOrderByFechaDesc(p.getId()).stream()
                        .limit(LIMITE)
                        .map(a -> mapear(a, actor))
                        .toList())
                .orElse(List.of());
    }

    private AccesoDatosResponse mapear(AuditoriaClinica a, UsuarioPrincipal actor) {
        boolean propio = a.getUsuario().getId().equals(actor.getId());
        return new AccesoDatosResponse(
                a.getId(),
                a.getFecha(),
                ETIQUETAS.getOrDefault(a.getAccion(), a.getAccion()),
                a.getDetalle(),
                propio ? "Usted" : a.getUsuario().getNombreCompleto());
    }
}

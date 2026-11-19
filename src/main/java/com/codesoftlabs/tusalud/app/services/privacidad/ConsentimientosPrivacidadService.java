package com.codesoftlabs.tusalud.app.services.privacidad;

import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.ConsentimientoEstadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.HistorialConsentimientoResponse;
import com.codesoftlabs.tusalud.app.entities.ConsentimientoTratamiento;
import com.codesoftlabs.tusalud.app.entities.FinalidadTratamiento;
import com.codesoftlabs.tusalud.app.entities.PoliticaPrivacidad;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.AccionConsentimiento;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.OrigenPrivacidad;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoTratamientoRepository;
import com.codesoftlabs.tusalud.app.repositories.FinalidadTratamientoRepository;
import com.codesoftlabs.tusalud.app.repositories.PoliticaPrivacidadRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentimientosPrivacidadService {

    private final ConsentimientoTratamientoRepository consentimientos;
    private final FinalidadTratamientoRepository finalidades;
    private final PoliticaPrivacidadRepository politicas;
    private final UsuarioRepository usuarios;

    public List<ConsentimientoEstadoResponse> mios(UUID usuarioId) {
        Map<UUID, ConsentimientoTratamiento> vigentes = new HashMap<>();
        for (ConsentimientoTratamiento c : consentimientos.historial(usuarioId)) {
            vigentes.putIfAbsent(c.getFinalidad().getId(), c);
        }
        return finalidades.activas().stream()
                .map(f -> estado(f, vigentes.get(f.getId())))
                .toList();
    }

    @Transactional
    public ConsentimientoEstadoResponse otorgar(UsuarioPrincipal actor, UUID finalidadId, String motivo) {
        FinalidadTratamiento f = finalidad(finalidadId);
        if (f.isObligatoria()) {
            throw new ConflictException("Esta finalidad no requiere consentimiento, se basa en " + f.getBaseLegal());
        }
        return estado(f, registrar(actor.getId(), f, AccionConsentimiento.OTORGAR, OrigenPrivacidad.APP, motivo));
    }

    @Transactional
    public ConsentimientoEstadoResponse revocar(UsuarioPrincipal actor, UUID finalidadId, String motivo) {
        FinalidadTratamiento f = finalidad(finalidadId);
        if (f.isObligatoria()) {
            throw new ConflictException(
                    "Esta finalidad es necesaria para prestar el servicio y no se puede revocar. Base: "
                            + f.getBaseLegal());
        }
        return estado(f, registrar(actor.getId(), f, AccionConsentimiento.REVOCAR, OrigenPrivacidad.APP, motivo));
    }

    @Transactional
    public void revocarPorSolicitud(UUID usuarioId, FinalidadTratamiento f, String motivo, OrigenPrivacidad origen) {
        registrar(usuarioId, f, AccionConsentimiento.REVOCAR, origen, motivo);
    }

    @Transactional
    public int revocarOpcionales(UUID usuarioId, String motivo, OrigenPrivacidad origen) {
        int revocados = 0;
        for (ConsentimientoEstadoResponse e : mios(usuarioId)) {
            if ("OTORGADO".equals(e.estado())) {
                registrar(usuarioId, finalidad(e.finalidadId()), AccionConsentimiento.REVOCAR, origen, motivo);
                revocados++;
            }
        }
        return revocados;
    }

    public List<HistorialConsentimientoResponse> historial(UUID usuarioId) {
        return consentimientos.historial(usuarioId).stream()
                .map(c -> new HistorialConsentimientoResponse(
                        c.getId(),
                        c.getFinalidad().getNombre(),
                        c.getAccion().name(),
                        c.getOrigen().name(),
                        c.getPoliticaVersion(),
                        c.getMotivo(),
                        c.getRegistradoEn()))
                .toList();
    }

    private ConsentimientoTratamiento registrar(
            UUID usuarioId,
            FinalidadTratamiento f,
            AccionConsentimiento accion,
            OrigenPrivacidad origen,
            String motivo) {
        Integer version = politicas
                .vigente(LocalDate.now())
                .map(PoliticaPrivacidad::getVersion)
                .orElse(null);
        return consentimientos.save(ConsentimientoTratamiento.builder()
                .usuario(usuarios.getReferenceById(usuarioId))
                .finalidad(f)
                .accion(accion)
                .origen(origen)
                .politicaVersion(version)
                .motivo(motivo == null || motivo.isBlank() ? null : motivo.trim())
                .registradoEn(instante(usuarioId, f))
                .build());
    }

    private LocalDateTime instante(UUID usuarioId, FinalidadTratamiento f) {
        LocalDateTime ahora = LocalDateTime.now();
        return consentimientos.historial(usuarioId).stream()
                .filter(c -> c.getFinalidad().getId().equals(f.getId()))
                .map(ConsentimientoTratamiento::getRegistradoEn)
                .findFirst()
                .filter(ultimo -> !ahora.isAfter(ultimo))
                .map(ultimo -> ultimo.plusNanos(1000))
                .orElse(ahora);
    }

    private ConsentimientoEstadoResponse estado(FinalidadTratamiento f, ConsentimientoTratamiento ultimo) {
        String estado;
        LocalDateTime desde = null;
        if (f.isObligatoria()) {
            estado = "NO_REQUIERE";
        } else if (ultimo == null) {
            estado = "SIN_REGISTRO";
        } else {
            estado = ultimo.getAccion() == AccionConsentimiento.OTORGAR ? "OTORGADO" : "REVOCADO";
            desde = ultimo.getRegistradoEn();
        }
        return new ConsentimientoEstadoResponse(
                f.getId(),
                f.getCodigo(),
                f.getNombre(),
                f.getDescripcion(),
                f.getBaseLegal(),
                f.isObligatoria(),
                estado,
                desde);
    }

    private FinalidadTratamiento finalidad(UUID id) {
        return finalidades
                .findById(id)
                .filter(FinalidadTratamiento::isActiva)
                .orElseThrow(() -> new ResourceNotFoundException("Finalidad de tratamiento", "id", id));
    }
}

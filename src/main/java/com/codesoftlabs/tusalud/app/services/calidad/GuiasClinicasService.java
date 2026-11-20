package com.codesoftlabs.tusalud.app.services.calidad;

import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.GuiaRequest;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.GuiaDetalleResponse;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.GuiaResumenResponse;
import com.codesoftlabs.tusalud.app.entities.Especialidad;
import com.codesoftlabs.tusalud.app.entities.GuiaClinica;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.EstadoGuia;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.GuiaClinicaRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuiasClinicasService {

    private final GuiaClinicaRepository guias;
    private final UsuarioRepository usuarios;
    private final EntityManager entityManager;

    public List<GuiaResumenResponse> buscar(String q, UUID especialidadId, boolean soloVigentes) {
        String texto = q == null || q.isBlank() ? null : q.strip().toLowerCase(Locale.ROOT);
        return guias.todas().stream()
                .filter(g -> !soloVigentes || g.getEstado() == EstadoGuia.VIGENTE)
                .filter(g -> especialidadId == null
                        || (g.getEspecialidad() != null
                                && g.getEspecialidad().getId().equals(especialidadId)))
                .filter(g -> texto == null
                        || g.getTitulo().toLowerCase(Locale.ROOT).contains(texto)
                        || g.getResumen().toLowerCase(Locale.ROOT).contains(texto)
                        || g.getCodigo().toLowerCase(Locale.ROOT).contains(texto)
                        || g.getCodigosCie10().stream()
                                .anyMatch(c -> c.toLowerCase(Locale.ROOT).startsWith(texto)))
                .map(GuiasClinicasService::resumen)
                .toList();
    }

    public GuiaDetalleResponse detalle(UUID id) {
        GuiaClinica guia = guias.detalle(id).orElseThrow(() -> new ResourceNotFoundException("Guía", "id", id));
        return new GuiaDetalleResponse(
                resumen(guia),
                guia.getContenido(),
                guias.versiones(guia.getCodigo()).stream()
                        .map(GuiasClinicasService::resumen)
                        .toList());
    }

    public List<GuiaResumenResponse> sugeridas(List<String> codigos) {
        List<String> diagnosticos = codigos == null
                ? List.of()
                : codigos.stream()
                        .filter(c -> c != null && !c.isBlank())
                        .map(c -> c.strip().toUpperCase(Locale.ROOT))
                        .toList();
        return guias.vigentes(EstadoGuia.VIGENTE).stream()
                .filter(g -> g.getCodigosCie10().stream()
                        .anyMatch(guia ->
                                diagnosticos.stream().anyMatch(d -> d.startsWith(guia.toUpperCase(Locale.ROOT)))))
                .map(GuiasClinicasService::resumen)
                .toList();
    }

    @Transactional
    public GuiaDetalleResponse crear(UsuarioPrincipal actor, GuiaRequest request) {
        String codigo = request.codigo().strip().toUpperCase(Locale.ROOT);
        if (!guias.versiones(codigo).isEmpty()) {
            throw new ConflictException("Ya existe una guía con ese código: cree una nueva versión");
        }
        GuiaClinica guia = construir(actor, request, codigo, 1);
        return detalle(guias.save(guia).getId());
    }

    @Transactional
    public GuiaDetalleResponse nuevaVersion(UsuarioPrincipal actor, UUID id, GuiaRequest request) {
        GuiaClinica actual = guias.detalle(id).orElseThrow(() -> new ResourceNotFoundException("Guía", "id", id));
        if (actual.getEstado() != EstadoGuia.VIGENTE) {
            throw new BusinessLogicException("Solo se versiona la guía vigente");
        }
        int siguiente = guias.versiones(actual.getCodigo()).get(0).getVersion() + 1;
        actual.setEstado(EstadoGuia.OBSOLETA);
        actual.setVigenteHasta(LocalDate.now());
        guias.save(actual);
        GuiaClinica nueva = construir(actor, request, actual.getCodigo(), siguiente);
        return detalle(guias.save(nueva).getId());
    }

    private GuiaClinica construir(UsuarioPrincipal actor, GuiaRequest request, String codigo, int version) {
        Set<String> cie = new LinkedHashSet<>();
        for (String c : request.codigosCie10()) {
            cie.add(c.strip().toUpperCase(Locale.ROOT));
        }
        return GuiaClinica.builder()
                .codigo(codigo)
                .version(version)
                .titulo(request.titulo().strip())
                .especialidad(
                        request.especialidadId() == null
                                ? null
                                : entityManager.getReference(Especialidad.class, request.especialidadId()))
                .resumen(request.resumen().strip())
                .contenido(request.contenido().strip())
                .estado(EstadoGuia.VIGENTE)
                .vigenteDesde(request.vigenteDesde())
                .porValidar(request.porValidar())
                .creadoPor(usuarios.getReferenceById(actor.getId()))
                .creadoEn(LocalDateTime.now())
                .codigosCie10(cie)
                .build();
    }

    private static GuiaResumenResponse resumen(GuiaClinica g) {
        return new GuiaResumenResponse(
                g.getId(),
                g.getCodigo(),
                g.getVersion(),
                g.getTitulo(),
                g.getEspecialidad() == null ? null : g.getEspecialidad().getNombre(),
                g.getResumen(),
                g.getEstado().name(),
                g.getVigenteDesde(),
                g.getVigenteHasta(),
                g.isPorValidar(),
                g.getCodigosCie10().stream().sorted().toList());
    }
}

package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.ContabilidadAuditoriaResponse;
import com.codesoftlabs.tusalud.app.entities.ContabilidadAuditoria;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.repositories.ContabilidadAuditoriaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ContabilidadAuditor {

    private static final int LIMITE = 200;
    private static final String SISTEMA = "Sistema";

    private final ContabilidadAuditoriaRepository repositorio;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Usuario usuario, String entidad, UUID entidadId, String accion, String detalle) {
        repositorio.save(ContabilidadAuditoria.builder()
                .fecha(LocalDateTime.now())
                .usuario(usuario)
                .entidad(entidad)
                .entidadId(entidadId)
                .accion(accion)
                .detalle(detalle == null ? null : detalle.substring(0, Math.min(300, detalle.length())))
                .build());
    }

    @Transactional(readOnly = true)
    public List<ContabilidadAuditoriaResponse> deEntidad(UUID entidadId) {
        return repositorio.deEntidad(entidadId).stream()
                .map(ContabilidadAuditor::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContabilidadAuditoriaResponse> recientes(String entidad) {
        String filtro =
                entidad == null || entidad.isBlank() ? null : entidad.trim().toUpperCase();
        return repositorio.recientes(filtro, PageRequest.of(0, LIMITE)).stream()
                .map(ContabilidadAuditor::aRespuesta)
                .toList();
    }

    private static ContabilidadAuditoriaResponse aRespuesta(ContabilidadAuditoria a) {
        return new ContabilidadAuditoriaResponse(
                a.getId(),
                a.getFecha(),
                a.getUsuario() == null ? SISTEMA : a.getUsuario().getNombreCompleto(),
                a.getEntidad(),
                a.getEntidadId(),
                a.getAccion(),
                a.getDetalle());
    }
}

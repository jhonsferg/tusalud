package com.codesoftlabs.tusalud.app.services.compras;

import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ComprasAuditoriaResponse;
import com.codesoftlabs.tusalud.app.entities.ComprasAuditoria;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.repositories.ComprasAuditoriaRepository;
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
public class ComprasAuditor {

    private static final int LIMITE = 200;

    private final ComprasAuditoriaRepository repositorio;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Usuario usuario, String entidad, UUID entidadId, String accion, String detalle) {
        repositorio.save(ComprasAuditoria.builder()
                .fecha(LocalDateTime.now())
                .usuario(usuario)
                .entidad(entidad)
                .entidadId(entidadId)
                .accion(accion)
                .detalle(detalle == null ? null : detalle.substring(0, Math.min(300, detalle.length())))
                .build());
    }

    @Transactional(readOnly = true)
    public List<ComprasAuditoriaResponse> deEntidad(UUID entidadId) {
        return repositorio.deEntidad(entidadId).stream()
                .map(ComprasAuditor::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComprasAuditoriaResponse> recientes(String entidad) {
        String filtro =
                entidad == null || entidad.isBlank() ? null : entidad.trim().toUpperCase();
        return repositorio.recientes(filtro, PageRequest.of(0, LIMITE)).stream()
                .map(ComprasAuditor::aRespuesta)
                .toList();
    }

    private static ComprasAuditoriaResponse aRespuesta(ComprasAuditoria a) {
        return new ComprasAuditoriaResponse(
                a.getId(),
                a.getFecha(),
                a.getUsuario().getNombreCompleto(),
                a.getEntidad(),
                a.getEntidadId(),
                a.getAccion(),
                a.getDetalle());
    }
}

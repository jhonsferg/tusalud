package com.codesoftlabs.tusalud.app.services.hce;

import com.codesoftlabs.tusalud.app.dto.request.HceRequests.PoliticaConservacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.PoliticaConservacionResponse;
import com.codesoftlabs.tusalud.app.entities.PoliticaConservacion;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.PoliticaConservacionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HceConservacionService {

    private final PoliticaConservacionRepository politicas;
    private final UsuarioRepository usuarios;

    public List<PoliticaConservacionResponse> listar() {
        return politicas.findAllByOrderByTipoRegistroAsc().stream()
                .map(HceConservacionService::aRespuesta)
                .toList();
    }

    @Transactional
    public PoliticaConservacionResponse actualizar(
            UsuarioPrincipal actor, String tipoRegistro, PoliticaConservacionRequest request) {
        PoliticaConservacion politica = politicas
                .findByTipoRegistro(tipoRegistro)
                .orElseThrow(() -> new ResourceNotFoundException("Política de conservación", "tipo", tipoRegistro));
        politica.setAniosMinimos(request.aniosMinimos());
        politica.setReferencia(
                request.referencia() == null || request.referencia().isBlank()
                        ? null
                        : request.referencia().strip());
        politica.setPorConfirmar(request.porConfirmar());
        politica.setDepuracionBloqueada(true);
        politica.setActualizadoEn(LocalDateTime.now());
        politica.setActualizadoPor(usuarios.getReferenceById(actor.getId()));
        return aRespuesta(politicas.save(politica));
    }

    private static PoliticaConservacionResponse aRespuesta(PoliticaConservacion p) {
        return new PoliticaConservacionResponse(
                p.getId(),
                p.getTipoRegistro(),
                p.getDescripcion(),
                p.getAniosMinimos(),
                p.getReferencia(),
                p.isPorConfirmar(),
                p.isDepuracionBloqueada(),
                p.getActualizadoEn());
    }
}

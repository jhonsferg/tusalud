package com.codesoftlabs.tusalud.app.services.privacidad;

import com.codesoftlabs.tusalud.app.documentos.CodigoDocumentoClinico;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.PoliticaRequest;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.EstadoAceptacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.PoliticaPublicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.PoliticaResponse;
import com.codesoftlabs.tusalud.app.entities.AceptacionPrivacidad;
import com.codesoftlabs.tusalud.app.entities.PoliticaPrivacidad;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.OrigenPrivacidad;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AceptacionPrivacidadRepository;
import com.codesoftlabs.tusalud.app.repositories.PoliticaPrivacidadRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PoliticaPrivacidadService {

    private final PoliticaPrivacidadRepository politicas;
    private final AceptacionPrivacidadRepository aceptaciones;
    private final UsuarioRepository usuarios;

    public Optional<PoliticaPublicaResponse> vigentePublica() {
        return politicas.vigente(LocalDate.now()).map(PoliticaPublicaResponse::de);
    }

    public List<PoliticaResponse> listar() {
        return politicas.findAllByOrderByVersionDesc().stream()
                .map(p -> PoliticaResponse.de(p, aceptaciones.contarDePolitica(p.getId())))
                .toList();
    }

    @Transactional
    public PoliticaResponse crear(UsuarioPrincipal actor, PoliticaRequest r) {
        PoliticaPrivacidad p = PoliticaPrivacidad.builder()
                .version(politicas.ultimaVersion() + 1)
                .titulo(r.titulo().trim())
                .contenido(r.contenido().strip())
                .vigenteDesde(r.vigenteDesde())
                .publicada(false)
                .hash("")
                .creadaPor(usuarios.getReferenceById(actor.getId()))
                .creadaEn(LocalDateTime.now())
                .build();
        p.setHash(CodigoDocumentoClinico.sha256(p.getTitulo() + "\n" + p.getContenido()));
        p = politicas.save(p);
        log.info("Borrador de política de privacidad v{} creado por {}", p.getVersion(), actor.getId());
        return PoliticaResponse.de(p, 0);
    }

    @Transactional
    public PoliticaResponse actualizar(UsuarioPrincipal actor, UUID id, PoliticaRequest r) {
        PoliticaPrivacidad p = cargar(id);
        if (p.isPublicada()) {
            throw new ConflictException("Una política publicada no se puede modificar, cree una nueva versión");
        }
        p.setTitulo(r.titulo().trim());
        p.setContenido(r.contenido().strip());
        p.setVigenteDesde(r.vigenteDesde());
        p.setHash(CodigoDocumentoClinico.sha256(p.getTitulo() + "\n" + p.getContenido()));
        return PoliticaResponse.de(p, 0);
    }

    @Transactional
    public PoliticaResponse publicar(UsuarioPrincipal actor, UUID id) {
        PoliticaPrivacidad p = cargar(id);
        if (p.isPublicada()) {
            throw new ConflictException("La política ya está publicada");
        }
        p.setPublicada(true);
        log.info("Política de privacidad v{} publicada por {}", p.getVersion(), actor.getId());
        return PoliticaResponse.de(p, 0);
    }

    public EstadoAceptacionResponse estado(UsuarioPrincipal actor) {
        Optional<PoliticaPrivacidad> vigente = politicas.vigente(LocalDate.now());
        if (vigente.isEmpty()) {
            return new EstadoAceptacionResponse(null, null, true, null, null);
        }
        Optional<AceptacionPrivacidad> aceptada =
                aceptaciones.deUsuario(actor.getId(), vigente.get().getId());
        return new EstadoAceptacionResponse(
                vigente.get().getVersion(),
                vigente.get().getId(),
                aceptada.isPresent(),
                aceptada.map(AceptacionPrivacidad::getAceptadaEn).orElse(null),
                aceptada.map(a -> a.getPolitica().getVersion()).orElse(null));
    }

    @Transactional
    public EstadoAceptacionResponse aceptar(UsuarioPrincipal actor, OrigenPrivacidad origen) {
        PoliticaPrivacidad vigente = politicas
                .vigente(LocalDate.now())
                .orElseThrow(() -> new ConflictException("No hay una política de privacidad vigente"));
        if (aceptaciones.deUsuario(actor.getId(), vigente.getId()).isEmpty()) {
            aceptaciones.save(AceptacionPrivacidad.builder()
                    .usuario(usuarios.getReferenceById(actor.getId()))
                    .politica(vigente)
                    .aceptadaEn(LocalDateTime.now())
                    .origen(origen)
                    .build());
        }
        return estado(actor);
    }

    private PoliticaPrivacidad cargar(UUID id) {
        return politicas
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Política de privacidad", "id", id));
    }
}

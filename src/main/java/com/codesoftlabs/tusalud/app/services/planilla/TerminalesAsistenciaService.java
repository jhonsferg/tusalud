package com.codesoftlabs.tusalud.app.services.planilla;

import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.TerminalRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.TerminalCreadaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.TerminalResponse;
import com.codesoftlabs.tusalud.app.entities.TerminalAsistencia;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.TerminalAsistenciaRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TerminalesAsistenciaService {

    private static final String ENTIDAD = "TERMINAL";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final TerminalAsistenciaRepository terminales;
    private final UsuarioRepository usuarioRepository;
    private final PlanillaAuditor auditor;

    public List<TerminalResponse> listar() {
        return terminales.findAllByOrderByNombreAsc().stream()
                .map(PlanillaMapper::terminal)
                .toList();
    }

    @Transactional
    public TerminalCreadaResponse crear(UsuarioPrincipal actor, TerminalRequest request) {
        String nombre = request.nombre().trim();
        if (terminales.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe una terminal con ese nombre");
        }
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = "tm_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        TerminalAsistencia terminal = terminales.save(TerminalAsistencia.builder()
                .nombre(nombre)
                .tokenHash(hash(token))
                .activo(true)
                .creadoPor(usuarioRepository.getReferenceById(actor.getId()))
                .build());
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()), ENTIDAD, terminal.getId(), "CREAR", nombre);
        return new TerminalCreadaResponse(terminal.getId(), terminal.getNombre(), token);
    }

    @Transactional
    public TerminalResponse cambiarEstado(UsuarioPrincipal actor, UUID id, boolean activo) {
        TerminalAsistencia terminal =
                terminales.findById(id).orElseThrow(() -> new ResourceNotFoundException("Terminal", "id", id));
        terminal.setActivo(activo);
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()),
                ENTIDAD,
                id,
                activo ? "ACTIVAR" : "DESACTIVAR",
                terminal.getNombre());
        return PlanillaMapper.terminal(terminal);
    }

    @Transactional
    public Optional<TerminalAsistencia> autenticar(String token) {
        if (token == null || token.isBlank() || token.length() > 100) {
            return Optional.empty();
        }
        Optional<TerminalAsistencia> terminal = terminales.findByTokenHashAndActivoTrue(hash(token.trim()));
        terminal.ifPresent(t -> t.setUltimoUso(LocalDateTime.now()));
        return terminal;
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

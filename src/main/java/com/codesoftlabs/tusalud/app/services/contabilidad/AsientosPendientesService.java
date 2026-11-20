package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.AsientoPendienteResponse;
import com.codesoftlabs.tusalud.app.entities.AsientoPendiente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AsientoPendienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CobroAnulado;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CobroRegistrado;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CompraAnulada;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CompraRegistrada;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.PagoProveedorRegistrado;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class AsientosPendientesService {

    private final AsientoPendienteRepository pendientes;
    private final Contabilizador contabilizador;
    private final UsuarioRepository usuarios;
    private final ContabilidadAuditor auditor;
    private final ObjectMapper mapper;
    private final TransactionTemplate nueva;

    public AsientosPendientesService(
            AsientoPendienteRepository pendientes,
            Contabilizador contabilizador,
            UsuarioRepository usuarios,
            ContabilidadAuditor auditor,
            ObjectMapper mapper,
            PlatformTransactionManager administrador) {
        this.pendientes = pendientes;
        this.contabilizador = contabilizador;
        this.usuarios = usuarios;
        this.auditor = auditor;
        this.mapper = mapper;
        this.nueva = new TransactionTemplate(administrador);
        this.nueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void despachar(Object hecho, UUID referenciaId) {
        try {
            nueva.executeWithoutResult(estado -> contabilizador.procesar(hecho));
        } catch (RuntimeException ex) {
            log.warn(
                    "No se pudo contabilizar {} {}: {}",
                    hecho.getClass().getSimpleName(),
                    referenciaId,
                    ex.getMessage());
            registrar(hecho, referenciaId, ex);
        }
    }

    private void registrar(Object hecho, UUID referenciaId, RuntimeException causa) {
        try {
            nueva.executeWithoutResult(estado -> {
                String evento = hecho.getClass().getSimpleName();
                AsientoPendiente existente = pendientes
                        .findFirstByEventoAndReferenciaIdAndResueltoEnIsNull(evento, referenciaId)
                        .orElse(null);
                if (existente != null) {
                    existente.setIntentos(existente.getIntentos() + 1);
                    existente.setError(mensaje(causa));
                    return;
                }
                pendientes.save(AsientoPendiente.builder()
                        .evento(evento)
                        .referenciaId(referenciaId)
                        .payload(mapper.writeValueAsString(hecho))
                        .error(mensaje(causa))
                        .intentos(1)
                        .creadoEn(LocalDateTime.now())
                        .build());
            });
        } catch (RuntimeException ex) {
            log.error("No se pudo guardar el pendiente contable {}", referenciaId, ex);
        }
    }

    @Transactional(readOnly = true)
    public List<AsientoPendienteResponse> listar(boolean soloAbiertos) {
        return (soloAbiertos ? pendientes.findByResueltoEnIsNullOrderByCreadoEnAsc() : pendientes.findAll())
                .stream().map(AsientosPendientesService::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public long abiertos() {
        return pendientes.countByResueltoEnIsNull();
    }

    public AsientoPendienteResponse reprocesar(UsuarioPrincipal actor, UUID id) {
        AsientoPendiente pendiente =
                pendientes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pendiente", "id", id));
        if (pendiente.getResueltoEn() != null) {
            throw new ConflictException("El pendiente ya fue resuelto");
        }
        Object hecho = leer(pendiente);
        try {
            nueva.executeWithoutResult(estado -> {
                contabilizador.procesar(hecho);
                AsientoPendiente actual = pendientes.findById(id).orElseThrow();
                actual.setResueltoEn(LocalDateTime.now());
                actual.setIntentos(actual.getIntentos() + 1);
                Usuario usuario = usuarios.getReferenceById(actor.getId());
                auditor.registrar(usuario, "PENDIENTE", id, "REPROCESAR", actual.getEvento());
            });
        } catch (RuntimeException ex) {
            nueva.executeWithoutResult(estado -> {
                AsientoPendiente actual = pendientes.findById(id).orElseThrow();
                actual.setIntentos(actual.getIntentos() + 1);
                actual.setError(mensaje(ex));
            });
            throw new ConflictException("No se pudo contabilizar: " + mensaje(ex));
        }
        return respuesta(pendientes.findById(id).orElseThrow());
    }

    private Object leer(AsientoPendiente pendiente) {
        Class<?> tipo = switch (pendiente.getEvento()) {
            case "CobroRegistrado" -> CobroRegistrado.class;
            case "CobroAnulado" -> CobroAnulado.class;
            case "CompraRegistrada" -> CompraRegistrada.class;
            case "CompraAnulada" -> CompraAnulada.class;
            case "PagoProveedorRegistrado" -> PagoProveedorRegistrado.class;
            default -> throw new ConflictException("Evento desconocido: " + pendiente.getEvento());
        };
        return mapper.readValue(pendiente.getPayload(), tipo);
    }

    private static String mensaje(RuntimeException ex) {
        String texto = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return texto.length() <= 300 ? texto : texto.substring(0, 300);
    }

    private static AsientoPendienteResponse respuesta(AsientoPendiente p) {
        return new AsientoPendienteResponse(
                p.getId(),
                p.getEvento(),
                p.getReferenciaId(),
                p.getError(),
                p.getIntentos(),
                p.getCreadoEn(),
                p.getResueltoEn());
    }
}

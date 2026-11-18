package com.codesoftlabs.tusalud.app.services.seguros;

import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.AprobarPreautorizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.PreautorizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.RechazarPreautorizacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.PreautorizacionResponse;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Poliza;
import com.codesoftlabs.tusalud.app.entities.Preautorizacion;
import com.codesoftlabs.tusalud.app.entities.ReglaCobertura;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPreautorizacion;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.PolizaRepository;
import com.codesoftlabs.tusalud.app.repositories.PreautorizacionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreautorizacionService {

    private static final int DIAS_POR_DEFECTO = 30;
    private static final int LIMITE_AVISOS = 20;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final PreautorizacionRepository repository;
    private final PolizaRepository polizaRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final RelacionAsistencialService relacionAsistencial;

    @Transactional
    public PreautorizacionResponse solicitar(UsuarioPrincipal actor, PreautorizacionRequest request) {
        relacionAsistencial.exigirRelacion(
                actor,
                request.pacienteId(),
                "Solo puede solicitar preautorizaciones de pacientes que atiende o que otro médico compartió con usted");
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.pacienteId()));
        LocalDate hoy = LocalDate.now();
        Poliza poliza = polizaRepository.usablesDelPaciente(paciente.getId(), hoy).stream()
                .filter(p -> p.usableEn(hoy))
                .findFirst()
                .orElseThrow(() -> new BusinessLogicException("El paciente no tiene una póliza vigente"));
        ReglaCobertura regla = poliza.getConvenio().getReglas().stream()
                .filter(r -> r.getCategoria() == request.categoria())
                .findFirst()
                .orElseThrow(() -> new BusinessLogicException("El convenio no cubre esta categoría"));
        if (!regla.isRequierePreautorizacion()) {
            throw new BusinessLogicException("El convenio no exige preautorización para esta categoría");
        }
        if (request.referenciaId() != null
                && repository.solicitudesAbiertas(paciente.getId(), request.categoria(), request.referenciaId()) > 0) {
            throw new ConflictException("Ya existe una solicitud abierta para este servicio");
        }
        Usuario solicitante = usuarioRepository.getReferenceById(actor.getId());
        Preautorizacion creada = repository.save(Preautorizacion.builder()
                .codigo(String.format("PA-%d-%05d", hoy.getYear(), repository.count() + 1))
                .paciente(paciente)
                .poliza(poliza)
                .categoria(request.categoria())
                .referenciaId(request.referenciaId())
                .descripcion(request.descripcion().trim())
                .montoEstimado(request.montoEstimado().setScale(2, RoundingMode.HALF_UP))
                .estado(EstadoPreautorizacion.SOLICITADA)
                .solicitadaPor(solicitante)
                .solicitadaEn(LocalDateTime.now())
                .build());
        avisarGestores(creada, poliza.getConvenio().getAseguradora().getNombre());
        log.info("Preautorización {} solicitada por {}", creada.getId(), actor.getId());
        return SegurosMapper.preautorizacion(detalle(creada.getId()), hoy);
    }

    @Transactional
    public PreautorizacionResponse aprobar(UsuarioPrincipal actor, UUID id, AprobarPreautorizacionRequest request) {
        Preautorizacion preautorizacion = detalle(id);
        exigirSolicitada(preautorizacion);
        LocalDate hoy = LocalDate.now();
        int dias = request.vigenciaDias() == null ? DIAS_POR_DEFECTO : request.vigenciaDias();
        preautorizacion.setEstado(EstadoPreautorizacion.APROBADA);
        preautorizacion.setMontoAprobado(request.montoAprobado().setScale(2, RoundingMode.HALF_UP));
        preautorizacion.setVigenteHasta(hoy.plusDays(dias));
        preautorizacion.setCodigoAutorizacion(
                "AUT-" + HexFormat.of().withUpperCase().formatHex(bytes(5)));
        registrarGestion(preautorizacion, actor);
        notificacionService.crear(
                preautorizacion.getSolicitadaPor(),
                TipoNotificacion.PREAUTORIZACION_RESUELTA,
                "Preautorización aprobada",
                "La preautorización " + preautorizacion.getCodigo() + " fue aprobada con el código "
                        + preautorizacion.getCodigoAutorizacion() + ".",
                "/preautorizaciones");
        log.info("Preautorización {} aprobada por {}", id, actor.getId());
        return SegurosMapper.preautorizacion(preautorizacion, hoy);
    }

    @Transactional
    public PreautorizacionResponse rechazar(UsuarioPrincipal actor, UUID id, RechazarPreautorizacionRequest request) {
        Preautorizacion preautorizacion = detalle(id);
        exigirSolicitada(preautorizacion);
        preautorizacion.setEstado(EstadoPreautorizacion.RECHAZADA);
        preautorizacion.setMotivoRechazo(request.motivo().trim());
        registrarGestion(preautorizacion, actor);
        notificacionService.crear(
                preautorizacion.getSolicitadaPor(),
                TipoNotificacion.PREAUTORIZACION_RESUELTA,
                "Preautorización rechazada",
                "La preautorización " + preautorizacion.getCodigo() + " fue rechazada: "
                        + request.motivo().trim(),
                "/preautorizaciones");
        log.info("Preautorización {} rechazada por {}", id, actor.getId());
        return SegurosMapper.preautorizacion(preautorizacion, LocalDate.now());
    }

    public List<PreautorizacionResponse> listar(EstadoPreautorizacion estado) {
        LocalDate hoy = LocalDate.now();
        List<Preautorizacion> lista = estado == null ? repository.todas() : repository.porEstado(estado);
        return lista.stream().map(p -> SegurosMapper.preautorizacion(p, hoy)).toList();
    }

    public List<PreautorizacionResponse> delMedico(UsuarioPrincipal actor) {
        LocalDate hoy = LocalDate.now();
        return repository.deSolicitante(actor.getId()).stream()
                .map(p -> SegurosMapper.preautorizacion(p, hoy))
                .toList();
    }

    public List<PreautorizacionResponse> delPaciente(UsuarioPrincipal actor) {
        LocalDate hoy = LocalDate.now();
        return repository.deLUsuario(actor.getId()).stream()
                .map(p -> SegurosMapper.preautorizacion(p, hoy))
                .toList();
    }

    public void exigirSiCorresponde(
            Paciente paciente, CategoriaServicio categoria, UUID referenciaId, boolean urgente) {
        if (urgente) {
            return;
        }
        LocalDate hoy = LocalDate.now();
        for (Poliza poliza : polizaRepository.usablesDelPaciente(paciente.getId(), hoy)) {
            if (!poliza.usableEn(hoy)) {
                continue;
            }
            ReglaCobertura regla = poliza.getConvenio().getReglas().stream()
                    .filter(r -> r.getCategoria() == categoria)
                    .findFirst()
                    .orElse(null);
            if (regla == null || !regla.isRequierePreautorizacion()) {
                return;
            }
            boolean aprobada = repository.aprobadasVigentes(paciente.getId(), categoria, hoy).stream()
                    .anyMatch(p ->
                            p.getReferenciaId() == null || p.getReferenciaId().equals(referenciaId));
            if (!aprobada) {
                throw new BusinessLogicException(
                        "El convenio " + poliza.getConvenio().getAseguradora().getNombre()
                                + " exige preautorización para " + etiqueta(categoria)
                                + ". Solicítela antes de continuar o registre el caso como urgente.");
            }
            return;
        }
    }

    @Transactional
    @Scheduled(cron = "0 30 2 * * *")
    public void vencerAprobadas() {
        List<Preautorizacion> vencidas = repository.vencidas(LocalDate.now());
        vencidas.forEach(p -> p.setEstado(EstadoPreautorizacion.VENCIDA));
        if (!vencidas.isEmpty()) {
            log.info("{} preautorizaciones vencidas", vencidas.size());
        }
    }

    private void exigirSolicitada(Preautorizacion preautorizacion) {
        if (preautorizacion.getEstado() != EstadoPreautorizacion.SOLICITADA) {
            throw new ConflictException("La preautorización ya fue resuelta");
        }
    }

    private void registrarGestion(Preautorizacion preautorizacion, UsuarioPrincipal actor) {
        preautorizacion.setGestionadaPor(usuarioRepository.getReferenceById(actor.getId()));
        preautorizacion.setGestionadaEn(LocalDateTime.now());
    }

    private void avisarGestores(Preautorizacion preautorizacion, String aseguradora) {
        List<Usuario> gestores = new java.util.ArrayList<>();
        gestores.addAll(usuarioRepository.findActivosPorRol(RolNombre.ROLE_FINANZAS));
        gestores.addAll(usuarioRepository.findActivosPorRol(RolNombre.ROLE_RECEPCION));
        gestores.stream()
                .limit(LIMITE_AVISOS)
                .forEach(usuario -> notificacionService.crear(
                        usuario,
                        TipoNotificacion.PREAUTORIZACION_SOLICITADA,
                        "Preautorización solicitada",
                        "Hay una solicitud " + preautorizacion.getCodigo() + " para " + aseguradora + " por "
                                + etiqueta(preautorizacion.getCategoria()) + ".",
                        "/preautorizaciones"));
    }

    private Preautorizacion detalle(UUID id) {
        return repository
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Preautorización", "id", id));
    }

    private static byte[] bytes(int cantidad) {
        byte[] datos = new byte[cantidad];
        ALEATORIO.nextBytes(datos);
        return datos;
    }

    static String etiqueta(CategoriaServicio categoria) {
        return switch (categoria) {
            case CONSULTA -> "consultas";
            case LABORATORIO -> "laboratorio";
            case IMAGEN -> "estudios de imagen";
            case HOSPITALIZACION -> "hospitalización";
            case FARMACIA -> "farmacia";
            case PROCEDIMIENTO -> "procedimientos";
        };
    }
}

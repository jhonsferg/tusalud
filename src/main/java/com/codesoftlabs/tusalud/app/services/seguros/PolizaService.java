package com.codesoftlabs.tusalud.app.services.seguros;

import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.PolizaEstadoRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.PolizaRequest;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.CoberturaPacienteResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.MiPolizaResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.PolizaResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.VerificacionResponse;
import com.codesoftlabs.tusalud.app.entities.Convenio;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Poliza;
import com.codesoftlabs.tusalud.app.entities.VerificacionCobertura;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPoliza;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ConvenioRepository;
import com.codesoftlabs.tusalud.app.repositories.CuentaAseguradoraRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.PolizaRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.repositories.VerificacionCoberturaRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolizaService {

    private static final int LIMITE_BUSQUEDA = 50;
    private static final int MINIMO_BUSQUEDA = 2;

    private final PolizaRepository polizaRepository;
    private final ConvenioRepository convenioRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaAseguradoraRepository cuentaRepository;
    private final VerificacionCoberturaRepository verificacionRepository;
    private final VerificadorCobertura verificador;
    private final RelacionAsistencialService relacionAsistencial;

    @Transactional
    public PolizaResponse registrar(UsuarioPrincipal actor, PolizaRequest request) {
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.pacienteId()));
        Convenio convenio = convenioRepository
                .findConDetalle(request.convenioId())
                .orElseThrow(() -> new ResourceNotFoundException("Convenio", "id", request.convenioId()));
        if (!convenio.isActivo() || !convenio.getAseguradora().isActiva()) {
            throw new DatosInvalidosException("El convenio no está activo");
        }
        if (request.vigenciaHasta().isBefore(request.vigenciaDesde())) {
            throw new DatosInvalidosException("La vigencia final no puede ser anterior a la inicial");
        }
        String numero = request.numero().trim().toUpperCase(Locale.ROOT);
        if (polizaRepository.existsByNumeroIgnoreCaseAndConvenioId(numero, convenio.getId())) {
            throw new ConflictException("Ya existe una póliza con el número " + numero + " en este convenio");
        }
        Poliza poliza = polizaRepository.save(Poliza.builder()
                .paciente(paciente)
                .convenio(convenio)
                .numero(numero)
                .titularNombre(request.titularNombre().trim())
                .parentesco(request.parentesco().trim().toUpperCase(Locale.ROOT))
                .plan(
                        request.plan() == null || request.plan().isBlank()
                                ? null
                                : request.plan().trim())
                .vigenciaDesde(request.vigenciaDesde())
                .vigenciaHasta(request.vigenciaHasta())
                .estado(EstadoPoliza.ACTIVA)
                .registradaPor(usuarioRepository.getReferenceById(actor.getId()))
                .creadaEn(LocalDateTime.now())
                .build());
        log.info("Póliza {} registrada por {}", poliza.getId(), actor.getId());
        return SegurosMapper.poliza(poliza, LocalDate.now());
    }

    @Transactional
    public PolizaResponse cambiarEstado(UsuarioPrincipal actor, UUID id, PolizaEstadoRequest request) {
        Poliza poliza = buscar(id);
        if (request.estado() == EstadoPoliza.VENCIDA) {
            throw new DatosInvalidosException("El estado vencida se calcula por la vigencia");
        }
        if (request.estado() == EstadoPoliza.SUSPENDIDA) {
            if (request.motivo() == null || request.motivo().trim().length() < 5) {
                throw new DatosInvalidosException("Indique el motivo de la suspensión");
            }
            poliza.setMotivoSuspension(request.motivo().trim());
        } else {
            poliza.setMotivoSuspension(null);
        }
        poliza.setEstado(request.estado());
        log.info("Póliza {} cambió a {} por {}", id, request.estado(), actor.getId());
        return SegurosMapper.poliza(poliza, LocalDate.now());
    }

    public List<PolizaResponse> buscar(String q) {
        String termino = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (termino.length() < MINIMO_BUSQUEDA) {
            throw new DatosInvalidosException("Escriba al menos " + MINIMO_BUSQUEDA + " caracteres para buscar");
        }
        LocalDate hoy = LocalDate.now();
        return polizaRepository.buscar("%" + termino + "%", PageRequest.of(0, LIMITE_BUSQUEDA)).stream()
                .map(p -> SegurosMapper.poliza(p, hoy))
                .toList();
    }

    public List<PolizaResponse> delPaciente(UUID pacienteId) {
        LocalDate hoy = LocalDate.now();
        return polizaRepository.deLPaciente(pacienteId).stream()
                .map(p -> SegurosMapper.poliza(p, hoy))
                .toList();
    }

    public List<MiPolizaResponse> mias(UsuarioPrincipal actor) {
        LocalDate hoy = LocalDate.now();
        return polizaRepository.deLUsuario(actor.getId()).stream()
                .map(p -> SegurosMapper.miPoliza(p, hoy))
                .toList();
    }

    @Transactional
    public VerificacionResponse verificar(UsuarioPrincipal actor, UUID polizaId) {
        Poliza poliza = buscar(polizaId);
        LocalDate hoy = LocalDate.now();
        BigDecimal consumido = cuentaRepository.consumido(
                poliza.getPaciente().getId(),
                poliza.getConvenio().getId(),
                hoy.withDayOfYear(1).atStartOfDay(),
                hoy.withDayOfYear(1).plusYears(1).atStartOfDay());
        VerificadorCobertura.ResultadoVerificacion resultado = verificador.verificar(poliza, consumido, hoy);
        LocalDateTime ahora = LocalDateTime.now();
        verificacionRepository.save(VerificacionCobertura.builder()
                .poliza(poliza)
                .vigente(resultado.vigente())
                .estado(resultado.estado())
                .saldoTope(resultado.saldoTope())
                .codigo(resultado.codigo())
                .verificadaPor(usuarioRepository.getReferenceById(actor.getId()))
                .fecha(ahora)
                .build());
        log.info("Póliza {} verificada por {} con resultado {}", polizaId, actor.getId(), resultado.estado());
        return new VerificacionResponse(
                polizaId,
                resultado.vigente(),
                resultado.estado(),
                resultado.saldoTope(),
                resultado.codigo(),
                ahora,
                resultado.mensaje());
    }

    public CoberturaPacienteResponse coberturaParaMedico(UsuarioPrincipal medico, UUID pacienteId) {
        relacionAsistencial.exigirRelacion(
                medico,
                pacienteId,
                "Solo puede consultar la cobertura de pacientes que atiende o que otro médico compartió con usted");
        List<Poliza> usables = polizaRepository.usablesDelPaciente(pacienteId, LocalDate.now());
        for (Poliza poliza : usables) {
            if (poliza.usableEn(LocalDate.now())) {
                return new CoberturaPacienteResponse(
                        pacienteId,
                        true,
                        poliza.getConvenio().getAseguradora().getNombre(),
                        EstadoPoliza.ACTIVA,
                        poliza.getConvenio().getReglas().stream()
                                .filter(r -> r.isRequierePreautorizacion())
                                .map(r -> r.getCategoria())
                                .sorted()
                                .toList());
            }
        }
        return new CoberturaPacienteResponse(pacienteId, false, null, null, List.of());
    }

    private Poliza buscar(UUID id) {
        return polizaRepository.findConDetalle(id).orElseThrow(() -> new ResourceNotFoundException("Póliza", "id", id));
    }
}

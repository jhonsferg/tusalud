package com.codesoftlabs.tusalud.app.services.planilla;

import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ContratoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.FinalizarContratoRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ContratoResponse;
import com.codesoftlabs.tusalud.app.entities.ContratoLaboral;
import com.codesoftlabs.tusalud.app.entities.Empleado;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoContrato;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoPlanilla;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.SistemaPensionario;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.BoletaPagoRepository;
import com.codesoftlabs.tusalud.app.repositories.ContratoLaboralRepository;
import com.codesoftlabs.tusalud.app.repositories.EmpleadoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContratosLaboralesService {

    private static final String ENTIDAD = "CONTRATO";
    private static final Set<EstadoPlanilla> CERRADAS =
            EnumSet.of(EstadoPlanilla.APROBADA, EstadoPlanilla.PAGADA, EstadoPlanilla.CERRADA);

    private final ContratoLaboralRepository contratos;
    private final EmpleadoRepository empleados;
    private final BoletaPagoRepository boletas;
    private final UsuarioRepository usuarioRepository;
    private final PlanillaAuditor auditor;

    public List<ContratoResponse> listar(UUID empleadoId, EstadoContrato estado) {
        return contratos.filtrar(empleadoId, estado).stream()
                .map(PlanillaMapper::contrato)
                .toList();
    }

    public ContratoResponse obtener(UUID id) {
        return PlanillaMapper.contrato(contrato(id));
    }

    @Transactional
    public ContratoResponse crear(UsuarioPrincipal actor, ContratoRequest request) {
        Empleado empleado = empleados
                .findByIdConUsuario(request.empleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", request.empleadoId()));
        validar(request);
        LocalDate fin = request.fechaFin();
        if (contratos.contarSolapados(
                        empleado.getId(), request.fechaInicio(), fin == null ? LocalDate.of(9999, 12, 31) : fin, null)
                > 0) {
            throw new ConflictException("El empleado ya tiene un contrato vigente en esas fechas");
        }
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        ContratoLaboral contrato = contratos.save(ContratoLaboral.builder()
                .empleado(empleado)
                .regimen(request.regimen())
                .remuneracion(request.remuneracion())
                .asignacionFamiliar(Boolean.TRUE.equals(request.asignacionFamiliar()))
                .sistemaPensionario(request.sistemaPensionario())
                .afpNombre(request.sistemaPensionario() == SistemaPensionario.AFP ? limpiar(request.afpNombre()) : null)
                .horasSemanales(request.horasSemanales() == null ? 48 : request.horasSemanales())
                .banco(limpiar(request.banco()))
                .cuenta(limpiar(request.cuenta()))
                .cargo(request.cargo().trim())
                .fechaInicio(request.fechaInicio())
                .fechaFin(fin)
                .estado(estadoPara(request.fechaInicio(), fin))
                .creadoPor(usuario)
                .build());
        sincronizar(empleado, contrato);
        auditor.registrar(
                usuario, ENTIDAD, contrato.getId(), "CREAR", request.regimen() + " desde " + request.fechaInicio());
        return PlanillaMapper.contrato(contrato);
    }

    @Transactional
    public ContratoResponse actualizar(UsuarioPrincipal actor, UUID id, ContratoRequest request) {
        ContratoLaboral contrato = contrato(id);
        if (contrato.getEstado() == EstadoContrato.FINALIZADO) {
            throw new ConflictException("El contrato está finalizado y no se puede modificar");
        }
        validar(request);
        boolean cambiaEconomia = contrato.getRemuneracion().compareTo(request.remuneracion()) != 0
                || contrato.getRegimen() != request.regimen()
                || contrato.getSistemaPensionario() != request.sistemaPensionario()
                || contrato.isAsignacionFamiliar() != Boolean.TRUE.equals(request.asignacionFamiliar());
        if (cambiaEconomia && boletas.existsByContratoIdAndPlanillaEstadoIn(id, CERRADAS)) {
            throw new ConflictException(
                    "El contrato ya tiene planillas aprobadas, finalícelo y cree uno nuevo para cambiar las condiciones");
        }
        LocalDate fin = request.fechaFin();
        if (contratos.contarSolapados(
                        contrato.getEmpleado().getId(),
                        request.fechaInicio(),
                        fin == null ? LocalDate.of(9999, 12, 31) : fin,
                        id)
                > 0) {
            throw new ConflictException("Las fechas se cruzan con otro contrato del empleado");
        }
        contrato.setRegimen(request.regimen());
        contrato.setRemuneracion(request.remuneracion());
        contrato.setAsignacionFamiliar(Boolean.TRUE.equals(request.asignacionFamiliar()));
        contrato.setSistemaPensionario(request.sistemaPensionario());
        contrato.setAfpNombre(
                request.sistemaPensionario() == SistemaPensionario.AFP ? limpiar(request.afpNombre()) : null);
        if (request.horasSemanales() != null) {
            contrato.setHorasSemanales(request.horasSemanales());
        }
        contrato.setBanco(limpiar(request.banco()));
        contrato.setCuenta(limpiar(request.cuenta()));
        contrato.setCargo(request.cargo().trim());
        contrato.setFechaInicio(request.fechaInicio());
        contrato.setFechaFin(fin);
        contrato.setEstado(estadoPara(request.fechaInicio(), fin));
        sincronizar(contrato.getEmpleado(), contrato);
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()),
                ENTIDAD,
                id,
                "ACTUALIZAR",
                request.regimen() + " " + request.remuneracion());
        return PlanillaMapper.contrato(contrato);
    }

    @Transactional
    public ContratoResponse finalizar(UsuarioPrincipal actor, UUID id, FinalizarContratoRequest request) {
        ContratoLaboral contrato = contrato(id);
        if (contrato.getEstado() == EstadoContrato.FINALIZADO) {
            throw new ConflictException("El contrato ya está finalizado");
        }
        if (request.fechaFin().isBefore(contrato.getFechaInicio())) {
            throw new DatosInvalidosException("La fecha de fin no puede ser anterior al inicio");
        }
        contrato.setFechaFin(request.fechaFin());
        contrato.setEstado(EstadoContrato.FINALIZADO);
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()),
                ENTIDAD,
                id,
                "FINALIZAR",
                "Fin " + request.fechaFin());
        return PlanillaMapper.contrato(contrato);
    }

    private ContratoLaboral contrato(UUID id) {
        return contratos.findByIdConDetalle(id).orElseThrow(() -> new ResourceNotFoundException("Contrato", "id", id));
    }

    private static void validar(ContratoRequest request) {
        if (request.fechaFin() != null && request.fechaFin().isBefore(request.fechaInicio())) {
            throw new DatosInvalidosException("La fecha de fin no puede ser anterior al inicio");
        }
        if (request.sistemaPensionario() == SistemaPensionario.AFP
                && (request.afpNombre() == null || request.afpNombre().isBlank())) {
            throw new DatosInvalidosException("Indique la AFP del trabajador");
        }
    }

    private static EstadoContrato estadoPara(LocalDate inicio, LocalDate fin) {
        return fin != null && fin.isBefore(LocalDate.now()) ? EstadoContrato.FINALIZADO : EstadoContrato.VIGENTE;
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void sincronizar(Empleado empleado, ContratoLaboral contrato) {
        LocalDate hoy = LocalDate.now();
        boolean vigente = !contrato.getFechaInicio().isAfter(hoy)
                && (contrato.getFechaFin() == null || !contrato.getFechaFin().isBefore(hoy));
        if (vigente) {
            empleado.setSueldo(contrato.getRemuneracion());
            empleado.setCargo(contrato.getCargo());
        }
    }
}

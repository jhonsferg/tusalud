package com.codesoftlabs.tusalud.app.services.odontologia;

import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.ItemPlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.PagoPlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.PlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.SesionRequest;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.ItemPlanResponse;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.PagoPlanResponse;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.PlanResponse;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.PlanResumen;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.SesionOdontologicaResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Consentimiento;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PlanOdontologico;
import com.codesoftlabs.tusalud.app.entities.PlanOdontologicoItem;
import com.codesoftlabs.tusalud.app.entities.PlanOdontologicoPago;
import com.codesoftlabs.tusalud.app.entities.ProcedimientoOdontologico;
import com.codesoftlabs.tusalud.app.entities.SesionCaja;
import com.codesoftlabs.tusalud.app.entities.SesionOdontologica;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoConsentimiento;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoItemPlan;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoPlan;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PlanOdontologicoItemRepository;
import com.codesoftlabs.tusalud.app.repositories.PlanOdontologicoPagoRepository;
import com.codesoftlabs.tusalud.app.repositories.PlanOdontologicoRepository;
import com.codesoftlabs.tusalud.app.repositories.ProcedimientoOdontologicoRepository;
import com.codesoftlabs.tusalud.app.repositories.SesionCajaRepository;
import com.codesoftlabs.tusalud.app.repositories.SesionOdontologicaRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanesOdontologicosService {

    public static final String ACCION_PLAN = "PLAN_ODONTOLOGICO";
    private static final Set<String> METODOS_PAGO = Set.of("EFECTIVO", "TARJETA", "YAPE", "PLIN", "TRANSFERENCIA");

    private final PlanOdontologicoRepository planes;
    private final PlanOdontologicoItemRepository items;
    private final SesionOdontologicaRepository sesiones;
    private final PlanOdontologicoPagoRepository pagos;
    private final ProcedimientoOdontologicoRepository procedimientos;
    private final ConsentimientoRepository consentimientos;
    private final CitaRepository citas;
    private final MedicoRepository medicoRepository;
    private final SesionCajaRepository sesionesCaja;
    private final UsuarioRepository usuarios;
    private final OdontogramaService odontograma;
    private final AuditoriaClinicaService auditoria;
    private final NotificacionService notificaciones;

    @Transactional
    public PlanResponse crear(UsuarioPrincipal actor, PlanRequest request) {
        Paciente paciente = odontograma.exigirEscritura(actor, request.pacienteId());
        Medico medico = medico(actor);
        PlanOdontologico plan = planes.save(PlanOdontologico.builder()
                .paciente(paciente)
                .medico(medico)
                .notas(
                        request.notas() == null || request.notas().isBlank()
                                ? null
                                : request.notas().strip())
                .build());
        auditoria.registrar(actor, paciente.getId(), ACCION_PLAN, "Plan creado");
        return detalle(planes.findConDetalle(plan.getId()).orElseThrow());
    }

    @Transactional
    public PlanResponse agregarItem(UsuarioPrincipal actor, UUID planId, ItemPlanRequest request) {
        PlanOdontologico plan = propioEditable(actor, planId);
        ProcedimientoOdontologico procedimiento = procedimientos
                .findById(request.procedimientoId())
                .filter(ProcedimientoOdontologico::isActivo)
                .orElseThrow(() -> new DatosInvalidosException("El procedimiento no existe o no está activo"));
        if (procedimiento.isRequierePieza()) {
            if (request.pieza() == null || !Dentadura.valida(request.pieza())) {
                throw new DatosInvalidosException("Indique una pieza válida en la numeración FDI");
            }
        } else if (request.pieza() != null) {
            throw new DatosInvalidosException("Este procedimiento no se asocia a una pieza");
        }
        if (procedimiento.isRequiereSuperficie() && request.superficie() == null) {
            throw new DatosInvalidosException("Indique la superficie de la pieza");
        }
        if (!procedimiento.isRequiereSuperficie() && request.superficie() != null) {
            throw new DatosInvalidosException("Este procedimiento no se asocia a una superficie");
        }
        Consentimiento consentimiento = request.consentimientoId() == null
                ? null
                : consentimientoFirmado(plan.getPaciente().getId(), request.consentimientoId());
        List<PlanOdontologicoItem> existentes = items.delPlan(plan.getId());
        items.save(PlanOdontologicoItem.builder()
                .plan(plan)
                .procedimiento(procedimiento)
                .pieza(request.pieza() == null ? null : request.pieza().shortValue())
                .superficie(request.superficie())
                .precio(procedimiento.getPrecio())
                .posicion(existentes.size())
                .consentimiento(consentimiento)
                .observacion(request.observacion())
                .build());
        recalcular(plan);
        return detalle(planes.findConDetalle(plan.getId()).orElseThrow());
    }

    @Transactional
    public PlanResponse quitarItem(UsuarioPrincipal actor, UUID planId, UUID itemId) {
        PlanOdontologico plan = propioEditable(actor, planId);
        PlanOdontologicoItem item = itemDelPlan(plan, itemId);
        items.delete(item);
        items.flush();
        recalcular(plan);
        return detalle(planes.findConDetalle(plan.getId()).orElseThrow());
    }

    @Transactional
    public PlanResponse vincularConsentimiento(
            UsuarioPrincipal actor, UUID planId, UUID itemId, UUID consentimientoId) {
        PlanOdontologico plan = planDelMedico(actor, planId);
        if (!EstadoPlan.ejecutables().contains(plan.getEstado())
                && !EstadoPlan.editables().contains(plan.getEstado())
                && plan.getEstado() != EstadoPlan.PRESENTADO) {
            throw new ConflictException("El plan ya no admite cambios");
        }
        PlanOdontologicoItem item = itemDelPlan(plan, itemId);
        if (item.getEstado() != EstadoItemPlan.PENDIENTE) {
            throw new ConflictException("El procedimiento ya no está pendiente");
        }
        item.setConsentimiento(consentimientoFirmado(plan.getPaciente().getId(), consentimientoId));
        return detalle(planes.findConDetalle(plan.getId()).orElseThrow());
    }

    @Transactional
    public PlanResponse presentar(UsuarioPrincipal actor, UUID planId) {
        PlanOdontologico plan = propioEditable(actor, planId);
        if (items.delPlan(plan.getId()).isEmpty()) {
            throw new DatosInvalidosException("Agregue al menos un procedimiento antes de presentar el plan");
        }
        plan.setEstado(EstadoPlan.PRESENTADO);
        plan.setPresentadoEn(LocalDateTime.now());
        auditoria.registrar(actor, plan.getPaciente().getId(), ACCION_PLAN, "Plan presentado");
        notificaciones.crear(
                plan.getPaciente().getUsuario(),
                TipoNotificacion.PLAN_ODONTOLOGICO_PRESENTADO,
                "Tiene un presupuesto odontológico",
                "Su odontólogo le presentó un plan de tratamiento. Revíselo y respóndalo en su cuenta.",
                "/mis-planes-odontologicos");
        return detalle(plan);
    }

    @Transactional
    public PlanResponse responder(UsuarioPrincipal actor, UUID planId, boolean aceptar, String motivo) {
        Paciente propio = odontograma.propio(actor);
        PlanOdontologico plan = planes.findConDetalle(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan odontológico", "id", planId));
        if (!plan.getPaciente().getId().equals(propio.getId())) {
            throw new ForbiddenOperationException("Solo puede responder sus propios planes");
        }
        if (plan.getEstado() != EstadoPlan.PRESENTADO) {
            throw new ConflictException("El plan no está pendiente de su respuesta");
        }
        plan.setEstado(aceptar ? EstadoPlan.ACEPTADO : EstadoPlan.RECHAZADO);
        plan.setResueltoEn(LocalDateTime.now());
        plan.setResueltoPor(usuarios.getReferenceById(actor.getId()));
        plan.setMotivoRechazo(aceptar || motivo == null ? null : motivo.strip());
        return detalle(plan);
    }

    @Transactional
    public PlanResponse cancelar(UsuarioPrincipal actor, UUID planId, String motivo) {
        PlanOdontologico plan = planDelMedico(actor, planId);
        if (plan.getEstado() == EstadoPlan.COMPLETADO
                || plan.getEstado() == EstadoPlan.CANCELADO
                || plan.getEstado() == EstadoPlan.RECHAZADO) {
            throw new ConflictException("El plan ya está cerrado");
        }
        plan.setEstado(EstadoPlan.CANCELADO);
        plan.setMotivoRechazo(motivo.strip());
        for (PlanOdontologicoItem item : items.delPlan(plan.getId())) {
            if (item.getEstado() == EstadoItemPlan.PENDIENTE) {
                item.setEstado(EstadoItemPlan.CANCELADO);
            }
        }
        recalcular(plan);
        auditoria.registrar(actor, plan.getPaciente().getId(), ACCION_PLAN, "Plan cancelado");
        return detalle(plan);
    }

    @Transactional
    public PlanResponse registrarSesion(UsuarioPrincipal actor, UUID planId, SesionRequest request) {
        PlanOdontologico plan = planes.findConDetalle(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan odontológico", "id", planId));
        Paciente paciente =
                odontograma.exigirEscritura(actor, plan.getPaciente().getId());
        if (!EstadoPlan.ejecutables().contains(plan.getEstado())) {
            throw new ConflictException("El plan debe estar aceptado por el paciente para registrar sesiones");
        }
        Medico medico = medico(actor);
        Cita cita = null;
        if (request.citaId() != null) {
            cita = citas.findById(request.citaId())
                    .filter(c -> c.getPaciente().getId().equals(paciente.getId())
                            && c.getMedico().getId().equals(medico.getId()))
                    .orElseThrow(() -> new DatosInvalidosException("La cita no corresponde al paciente y al médico"));
        }
        Map<UUID, PlanOdontologicoItem> porId =
                items.delPlan(plan.getId()).stream().collect(Collectors.toMap(PlanOdontologicoItem::getId, i -> i));
        Set<UUID> vistos = new HashSet<>();
        List<PlanOdontologicoItem> seleccion = new ArrayList<>();
        for (UUID id : request.itemIds()) {
            PlanOdontologicoItem item = porId.get(id);
            if (item == null) {
                throw new DatosInvalidosException("Un procedimiento no pertenece al plan");
            }
            if (!vistos.add(id)) {
                throw new DatosInvalidosException("Un procedimiento está repetido");
            }
            if (item.getEstado() != EstadoItemPlan.PENDIENTE) {
                throw new ConflictException(
                        "El procedimiento " + item.getProcedimiento().getNombre() + " ya no está pendiente");
            }
            if (item.getProcedimiento().isRequiereConsentimiento()) {
                Consentimiento c = item.getConsentimiento();
                if (c == null || c.estadoEfectivo() != EstadoConsentimiento.FIRMADO) {
                    throw new ConflictException("El procedimiento "
                            + item.getProcedimiento().getNombre() + " requiere un consentimiento firmado y vigente");
                }
            }
            seleccion.add(item);
        }
        SesionOdontologica sesion = sesiones.save(SesionOdontologica.builder()
                .plan(plan)
                .medico(medico)
                .cita(cita)
                .notas(
                        request.notas() == null || request.notas().isBlank()
                                ? null
                                : request.notas().strip())
                .build());
        LocalDateTime ahora = LocalDateTime.now();
        for (PlanOdontologicoItem item : seleccion) {
            item.setEstado(EstadoItemPlan.REALIZADO);
            item.setSesion(sesion);
            item.setRealizadoEn(ahora);
            ProcedimientoOdontologico p = item.getProcedimiento();
            if (p.getEstadoResultante() != null && item.getPieza() != null) {
                odontograma.agregar(
                        actor,
                        paciente,
                        item.getPieza(),
                        p.isRequiereSuperficie() ? item.getSuperficie() : null,
                        p.getEstadoResultante(),
                        p.getNombre(),
                        item.getId());
            }
        }
        plan.setEstado(EstadoPlan.EN_CURSO);
        boolean pendientes = porId.values().stream().anyMatch(i -> i.getEstado() == EstadoItemPlan.PENDIENTE);
        if (!pendientes) {
            plan.setEstado(EstadoPlan.COMPLETADO);
            plan.setCompletadoEn(ahora);
        }
        auditoria.registrar(actor, paciente.getId(), ACCION_PLAN, "Sesión con " + seleccion.size() + " procedimientos");
        return detalle(plan);
    }

    @Transactional
    public PlanResponse registrarPago(UsuarioPrincipal actor, UUID planId, PagoPlanRequest request) {
        PlanOdontologico plan = planes.findConDetalle(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan odontológico", "id", planId));
        if (!EstadoPlan.cobrables().contains(plan.getEstado())) {
            throw new ConflictException("El plan no admite cobros en su estado actual");
        }
        String metodo = request.metodo().strip().toUpperCase();
        if (!METODOS_PAGO.contains(metodo)) {
            throw new DatosInvalidosException("Método de pago no admitido");
        }
        SesionCaja caja = sesionesCaja
                .findDelCajero(actor.getId(), EstadoCaja.ABIERTA)
                .orElseThrow(() -> new ConflictException("Abra la caja antes de registrar un cobro"));
        BigDecimal saldo = plan.getTotal().subtract(pagos.totalPagado(plan.getId()));
        if (request.monto().compareTo(saldo) > 0) {
            throw new DatosInvalidosException("El monto supera el saldo pendiente del plan");
        }
        pagos.save(PlanOdontologicoPago.builder()
                .plan(plan)
                .monto(request.monto())
                .metodo(metodo)
                .referencia(
                        request.referencia() == null || request.referencia().isBlank()
                                ? null
                                : request.referencia().strip())
                .sesionCaja(caja)
                .registradoPor(usuarios.getReferenceById(actor.getId()))
                .build());
        return detalleSinClinica(plan);
    }

    public List<PlanResumen> delPaciente(UsuarioPrincipal actor, UUID pacienteId) {
        odontograma.exigirLectura(actor, pacienteId);
        return planes.delPaciente(pacienteId).stream()
                .filter(p -> !odontograma.esPaciente(actor) || p.getEstado() != EstadoPlan.BORRADOR)
                .map(p -> resumen(p, p.getPaciente()))
                .toList();
    }

    public List<PlanResumen> mios(UsuarioPrincipal actor) {
        Paciente propio = odontograma.propio(actor);
        return planes.delPaciente(propio.getId()).stream()
                .filter(p -> p.getEstado() != EstadoPlan.BORRADOR)
                .map(p -> resumen(p, propio))
                .toList();
    }

    public List<PlanResumen> porCobrar() {
        return planes.porEstados(EstadoPlan.cobrables()).stream()
                .map(p -> resumen(p, p.getPaciente()))
                .filter(r -> r.saldo().signum() > 0)
                .toList();
    }

    public PlanResponse obtener(UsuarioPrincipal actor, UUID planId) {
        PlanOdontologico plan = planes.findConDetalle(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan odontológico", "id", planId));
        if (odontograma.esPaciente(actor)) {
            Paciente propio = odontograma.propio(actor);
            if (!plan.getPaciente().getId().equals(propio.getId()) || plan.getEstado() == EstadoPlan.BORRADOR) {
                throw new ForbiddenOperationException("No tiene acceso a este plan");
            }
        } else {
            odontograma.exigirEscritura(actor, plan.getPaciente().getId());
        }
        return detalle(plan);
    }

    private PlanOdontologico propioEditable(UsuarioPrincipal actor, UUID planId) {
        PlanOdontologico plan = planDelMedico(actor, planId);
        if (!EstadoPlan.editables().contains(plan.getEstado())) {
            throw new ConflictException("Solo se puede modificar un plan en borrador");
        }
        return plan;
    }

    private PlanOdontologico planDelMedico(UsuarioPrincipal actor, UUID planId) {
        PlanOdontologico plan = planes.findConDetalle(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan odontológico", "id", planId));
        odontograma.exigirEscritura(actor, plan.getPaciente().getId());
        Medico medico = medico(actor);
        if (!plan.getMedico().getId().equals(medico.getId())) {
            throw new ForbiddenOperationException("Solo el odontólogo que creó el plan puede modificarlo");
        }
        return plan;
    }

    private PlanOdontologicoItem itemDelPlan(PlanOdontologico plan, UUID itemId) {
        return items.delPlan(plan.getId()).stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento del plan", "id", itemId));
    }

    private Consentimiento consentimientoFirmado(UUID pacienteId, UUID consentimientoId) {
        Consentimiento c = consentimientos
                .findById(consentimientoId)
                .orElseThrow(() -> new DatosInvalidosException("El consentimiento no existe"));
        if (!c.getPaciente().getId().equals(pacienteId)) {
            throw new DatosInvalidosException("El consentimiento no corresponde al paciente");
        }
        if (c.estadoEfectivo() != EstadoConsentimiento.FIRMADO) {
            throw new DatosInvalidosException("El consentimiento debe estar firmado y vigente");
        }
        return c;
    }

    private void recalcular(PlanOdontologico plan) {
        BigDecimal total = items.delPlan(plan.getId()).stream()
                .filter(i -> i.getEstado() != EstadoItemPlan.CANCELADO)
                .map(PlanOdontologicoItem::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        plan.setTotal(total);
    }

    private Medico medico(UsuarioPrincipal actor) {
        return medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(
                        () -> new ForbiddenOperationException("Solo el personal médico gestiona planes odontológicos"));
    }

    private PlanResumen resumen(PlanOdontologico p, Paciente paciente) {
        BigDecimal pagado = pagos.totalPagado(p.getId());
        return new PlanResumen(
                p.getId(),
                paciente.getId(),
                paciente.getUsuario().getNombreCompleto(),
                p.getMedico().getUsuario().getNombreCompleto(),
                p.getEstado(),
                p.getTotal(),
                pagado,
                p.getTotal().subtract(pagado),
                p.getCreadoEn());
    }

    private PlanResponse detalle(PlanOdontologico p) {
        return armar(p, true);
    }

    private PlanResponse detalleSinClinica(PlanOdontologico p) {
        return armar(p, false);
    }

    private PlanResponse armar(PlanOdontologico p, boolean clinico) {
        BigDecimal pagado = pagos.totalPagado(p.getId());
        List<ItemPlanResponse> listaItems = clinico
                ? items.delPlan(p.getId()).stream()
                        .map(PlanesOdontologicosService::item)
                        .toList()
                : List.of();
        List<SesionOdontologicaResponse> listaSesiones = clinico
                ? sesiones.delPlan(p.getId()).stream()
                        .map(s -> new SesionOdontologicaResponse(
                                s.getId(),
                                s.getFecha(),
                                s.getMedico().getUsuario().getNombreCompleto(),
                                s.getCita() == null ? null : s.getCita().getId(),
                                s.getNotas(),
                                items.delPlan(p.getId()).stream()
                                        .filter(i -> i.getSesion() != null
                                                && i.getSesion().getId().equals(s.getId()))
                                        .map(PlanOdontologicoItem::getId)
                                        .toList()))
                        .toList()
                : List.of();
        List<PagoPlanResponse> listaPagos = pagos.delPlan(p.getId()).stream()
                .map(g -> new PagoPlanResponse(
                        g.getId(),
                        g.getMonto(),
                        g.getMetodo(),
                        g.getReferencia(),
                        g.getRegistradoPor().getNombreCompleto(),
                        g.getRegistradoEn()))
                .toList();
        return new PlanResponse(
                p.getId(),
                p.getPaciente().getId(),
                p.getPaciente().getUsuario().getNombreCompleto(),
                p.getMedico().getUsuario().getNombreCompleto(),
                p.getEstado(),
                clinico ? p.getNotas() : null,
                p.getTotal(),
                pagado,
                p.getTotal().subtract(pagado),
                p.getCreadoEn(),
                p.getPresentadoEn(),
                p.getResueltoEn(),
                p.getMotivoRechazo(),
                p.getCompletadoEn(),
                listaItems,
                listaSesiones,
                listaPagos);
    }

    private static ItemPlanResponse item(PlanOdontologicoItem i) {
        return new ItemPlanResponse(
                i.getId(),
                i.getProcedimiento().getId(),
                i.getProcedimiento().getCodigo(),
                i.getProcedimiento().getNombre(),
                i.getPieza() == null ? null : i.getPieza().intValue(),
                i.getSuperficie(),
                i.getPrecio(),
                i.getEstado(),
                i.getProcedimiento().isRequiereConsentimiento(),
                i.getConsentimiento() == null ? null : i.getConsentimiento().getId(),
                i.getRealizadoEn(),
                i.getObservacion());
    }
}

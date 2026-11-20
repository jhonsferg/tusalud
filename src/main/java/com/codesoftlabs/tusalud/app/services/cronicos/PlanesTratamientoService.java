package com.codesoftlabs.tusalud.app.services.cronicos;

import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.CumplirControlRequest;
import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.EstadoPlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.MedicionRequest;
import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.MetaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.PlanRequest;
import com.codesoftlabs.tusalud.app.dto.response.CronicosResponses.ControlResponse;
import com.codesoftlabs.tusalud.app.dto.response.CronicosResponses.MedicionResponse;
import com.codesoftlabs.tusalud.app.dto.response.CronicosResponses.MetaResponse;
import com.codesoftlabs.tusalud.app.dto.response.CronicosResponses.PlanDetalleResponse;
import com.codesoftlabs.tusalud.app.dto.response.CronicosResponses.PlanResumenResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PlanControl;
import com.codesoftlabs.tusalud.app.entities.PlanMedicion;
import com.codesoftlabs.tusalud.app.entities.PlanMeta;
import com.codesoftlabs.tusalud.app.entities.PlanTratamiento;
import com.codesoftlabs.tusalud.app.entities.RecetaElectronica;
import com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.EstadoControl;
import com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.EstadoPlan;
import com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.OrigenMedicion;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.PlanMedicionRepository;
import com.codesoftlabs.tusalud.app.repositories.PlanTratamientoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanesTratamientoService {

    private final PlanTratamientoRepository planes;
    private final PlanMedicionRepository mediciones;
    private final PacienteRepository pacientes;
    private final MedicoRepository medicos;
    private final UsuarioRepository usuarios;
    private final RelacionAsistencialService relacion;
    private final NotificacionService notificaciones;
    private final EntityManager entityManager;

    @Value("${app.cronicos.alertas-habilitadas:true}")
    private boolean alertasHabilitadas;

    @Transactional
    public PlanDetalleResponse crear(UsuarioPrincipal actor, PlanRequest request) {
        relacion.exigirRelacion(actor, request.pacienteId(), "No tiene relación asistencial con este paciente");
        Paciente paciente = pacientes
                .findById(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.pacienteId()));
        var medico = medicos.findByUsuarioEmail(actor.getUsername())
                .orElseThrow(() -> new ForbiddenOperationException("Solo un médico crea planes de tratamiento"));
        RecetaElectronica receta = null;
        if (request.recetaId() != null) {
            receta = entityManager.find(RecetaElectronica.class, request.recetaId());
            if (receta == null || !receta.getPaciente().getId().equals(paciente.getId())) {
                throw new BusinessLogicException("La receta no existe o no corresponde al paciente");
            }
        }
        Set<String> vistos = new HashSet<>();
        for (MetaRequest meta : request.metas()) {
            if (meta.valorMin() == null && meta.valorMax() == null) {
                throw new BusinessLogicException("Cada meta necesita un valor mínimo, un máximo o ambos");
            }
            if (meta.valorMin() != null
                    && meta.valorMax() != null
                    && meta.valorMin().compareTo(meta.valorMax()) > 0) {
                throw new BusinessLogicException("El valor mínimo de la meta no puede superar al máximo");
            }
            if (!vistos.add(meta.parametro().name())) {
                throw new BusinessLogicException("Hay una meta repetida para " + meta.parametro());
            }
        }
        LocalDate inicio = request.inicio() == null ? LocalDate.now() : request.inicio();
        PlanTratamiento plan = PlanTratamiento.builder()
                .paciente(paciente)
                .medico(medico)
                .programa(request.programa())
                .diagnosticoCodigo(texto(request.diagnosticoCodigo()))
                .objetivos(request.objetivos().strip())
                .intervenciones(texto(request.intervenciones()))
                .medicacion(texto(request.medicacion()))
                .receta(receta)
                .inicio(inicio)
                .frecuenciaControlDias(request.frecuenciaControlDias())
                .creadoEn(LocalDateTime.now())
                .build();
        for (MetaRequest meta : request.metas()) {
            plan.getMetas()
                    .add(PlanMeta.builder()
                            .plan(plan)
                            .parametro(meta.parametro())
                            .valorMin(meta.valorMin())
                            .valorMax(meta.valorMax())
                            .unidad(meta.unidad().strip())
                            .build());
        }
        for (int n = 1; n <= request.cantidadControles(); n++) {
            plan.getControles()
                    .add(PlanControl.builder()
                            .plan(plan)
                            .numero(n)
                            .fechaProgramada(inicio.plusDays((long) n * request.frecuenciaControlDias()))
                            .build());
        }
        return detalle(planes.save(plan));
    }

    public List<PlanResumenResponse> mios(UsuarioPrincipal actor) {
        return planes.delPaciente(actor.getId()).stream().map(this::resumen).toList();
    }

    public List<PlanResumenResponse> deUnPaciente(UsuarioPrincipal actor, UUID pacienteId) {
        relacion.exigirRelacion(actor, pacienteId, "No tiene relación asistencial con este paciente");
        return planes.porPacienteId(pacienteId).stream().map(this::resumen).toList();
    }

    public List<PlanResumenResponse> tablero(UsuarioPrincipal actor) {
        return planes.activosDelMedico(actor.getId(), EstadoPlan.ACTIVO).stream()
                .map(this::resumen)
                .sorted((a, b) -> Integer.compare(peso(b.riesgo()), peso(a.riesgo())))
                .toList();
    }

    public PlanDetalleResponse obtener(UsuarioPrincipal actor, UUID id) {
        PlanTratamiento plan = cargar(id);
        exigirLectura(actor, plan);
        return detalle(plan);
    }

    @Transactional
    public PlanDetalleResponse registrarMedicion(UsuarioPrincipal actor, UUID id, MedicionRequest request) {
        PlanTratamiento plan = cargar(id);
        boolean esPaciente = actor.getRoles().contains(RolNombre.ROLE_PACIENTE.name());
        if (esPaciente) {
            if (!plan.getPaciente().getUsuario().getId().equals(actor.getId())) {
                throw new ForbiddenOperationException("Solo el titular registra sus mediciones");
            }
        } else {
            relacion.exigirRelacion(
                    actor, plan.getPaciente().getId(), "No tiene relación asistencial con este paciente");
        }
        if (plan.getEstado() != EstadoPlan.ACTIVO) {
            throw new BusinessLogicException("El plan no está activo");
        }
        LocalDateTime medidoEn = request.medidoEn() == null ? LocalDateTime.now() : request.medidoEn();
        if (medidoEn.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BusinessLogicException("La fecha de la medición no puede ser futura");
        }
        boolean fuera = plan.getMetas().stream()
                .filter(m -> m.getParametro() == request.parametro())
                .anyMatch(m -> (m.getValorMin() != null && request.valor().compareTo(m.getValorMin()) < 0)
                        || (m.getValorMax() != null && request.valor().compareTo(m.getValorMax()) > 0));
        mediciones.save(PlanMedicion.builder()
                .plan(plan)
                .numero(mediciones.ultimoNumero(plan.getId()) + 1)
                .parametro(request.parametro())
                .valor(request.valor())
                .medidoEn(medidoEn)
                .origen(esPaciente ? OrigenMedicion.PACIENTE : OrigenMedicion.PERSONAL)
                .registradoPor(usuarios.getReferenceById(actor.getId()))
                .fueraDeRango(fuera)
                .build());
        if (fuera) {
            avisarFueraDeRango(plan, esPaciente);
        }
        return detalle(plan);
    }

    @Transactional
    public PlanDetalleResponse cumplirControl(
            UsuarioPrincipal actor, UUID planId, UUID controlId, CumplirControlRequest request) {
        PlanTratamiento plan = cargar(planId);
        exigirAutor(actor, plan);
        PlanControl control = plan.getControles().stream()
                .filter(c -> c.getId().equals(controlId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Control", "id", controlId));
        if (control.getEstado() != EstadoControl.PENDIENTE) {
            throw new BusinessLogicException("El control ya fue atendido");
        }
        if (request != null && request.citaId() != null) {
            Cita cita = entityManager.find(Cita.class, request.citaId());
            if (cita == null
                    || !cita.getPaciente().getId().equals(plan.getPaciente().getId())) {
                throw new BusinessLogicException("La cita no existe o no corresponde al paciente");
            }
            control.setCita(cita);
        }
        control.setEstado(EstadoControl.CUMPLIDO);
        control.setCumplidoEn(LocalDateTime.now());
        return detalle(planes.save(plan));
    }

    @Transactional
    public PlanDetalleResponse cambiarEstado(UsuarioPrincipal actor, UUID id, EstadoPlanRequest request) {
        PlanTratamiento plan = cargar(id);
        exigirAutor(actor, plan);
        if (request.estado() == EstadoPlan.ACTIVO && plan.getEstado() == EstadoPlan.COMPLETADO) {
            throw new BusinessLogicException("Un plan completado no se reactiva");
        }
        plan.setEstado(request.estado());
        plan.setFin(request.estado() == EstadoPlan.ACTIVO ? null : LocalDate.now());
        return detalle(planes.save(plan));
    }

    @Scheduled(cron = "${app.cronicos.alerta-cron:0 0 7 * * *}")
    @Transactional
    public int alertarControlesVencidos() {
        if (!alertasHabilitadas) {
            return 0;
        }
        List<PlanControl> vencidos =
                planes.controlesVencidosSinAlerta(LocalDate.now(), EstadoControl.PENDIENTE, EstadoPlan.ACTIVO);
        for (PlanControl control : vencidos) {
            PlanTratamiento plan = control.getPlan();
            notificaciones.crear(
                    plan.getPaciente().getUsuario(),
                    TipoNotificacion.SISTEMA,
                    "Control pendiente",
                    "Tiene un control de su plan de tratamiento vencido. Programe su cita con su médico.",
                    "/mi-plan");
            notificaciones.crear(
                    plan.getMedico().getUsuario(),
                    TipoNotificacion.SISTEMA,
                    "Control vencido en un plan",
                    "Uno de sus pacientes tiene un control vencido en su plan de tratamiento.",
                    "/pacientes-cronicos");
            control.setAlertadoEn(LocalDateTime.now());
        }
        return vencidos.size();
    }

    private void avisarFueraDeRango(PlanTratamiento plan, boolean registraPaciente) {
        if (registraPaciente) {
            notificaciones.crear(
                    plan.getMedico().getUsuario(),
                    TipoNotificacion.SISTEMA,
                    "Medición para revisar",
                    "Un paciente de su plan registró una medición fuera de la meta.",
                    "/pacientes-cronicos");
        } else {
            notificaciones.crear(
                    plan.getPaciente().getUsuario(),
                    TipoNotificacion.SISTEMA,
                    "Medición para revisar",
                    "Se registró una medición que conviene revisar con su médico.",
                    "/mi-plan");
        }
    }

    private PlanTratamiento cargar(UUID id) {
        return planes.detalle(id).orElseThrow(() -> new ResourceNotFoundException("Plan de tratamiento", "id", id));
    }

    private void exigirLectura(UsuarioPrincipal actor, PlanTratamiento plan) {
        if (actor.getRoles().contains(RolNombre.ROLE_PACIENTE.name())) {
            if (!plan.getPaciente().getUsuario().getId().equals(actor.getId())) {
                throw new ForbiddenOperationException("Solo el titular ve su plan");
            }
            return;
        }
        relacion.exigirRelacion(actor, plan.getPaciente().getId(), "No tiene relación asistencial con este paciente");
    }

    private void exigirAutor(UsuarioPrincipal actor, PlanTratamiento plan) {
        if (!plan.getMedico().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Solo el médico del plan lo modifica");
        }
    }

    private PlanResumenResponse resumen(PlanTratamiento p) {
        List<PlanMedicion> historial = mediciones.historial(p.getId());
        int vencidos = vencidos(p);
        int fuera =
                (int) historial.stream().filter(PlanMedicion::isFueraDeRango).count();
        return new PlanResumenResponse(
                p.getId(),
                p.getPaciente().getUsuario().getNombreCompleto(),
                p.getPrograma().name(),
                p.getEstado().name(),
                p.getInicio(),
                adherencia(p),
                vencidos,
                fuera,
                riesgo(p, historial, vencidos));
    }

    private PlanDetalleResponse detalle(PlanTratamiento p) {
        List<PlanMedicion> historial = mediciones.historial(p.getId());
        int vencidos = vencidos(p);
        return new PlanDetalleResponse(
                p.getId(),
                p.getPaciente().getId(),
                p.getPaciente().getUsuario().getNombreCompleto(),
                p.getMedico().getUsuario().getNombreCompleto(),
                p.getPrograma().name(),
                p.getDiagnosticoCodigo(),
                p.getObjetivos(),
                p.getIntervenciones(),
                p.getMedicacion(),
                p.getReceta() == null ? null : p.getReceta().getId(),
                p.getEstado().name(),
                p.getInicio(),
                p.getFin(),
                p.getFrecuenciaControlDias(),
                adherencia(p),
                riesgo(p, historial, vencidos),
                p.getMetas().stream()
                        .map(m -> new MetaResponse(
                                m.getParametro().name(), m.getValorMin(), m.getValorMax(), m.getUnidad()))
                        .toList(),
                p.getControles().stream()
                        .map(c -> new ControlResponse(
                                c.getId(),
                                c.getNumero(),
                                c.getFechaProgramada(),
                                c.getEstado().name(),
                                c.getEstado() == EstadoControl.PENDIENTE
                                        && c.getFechaProgramada().isBefore(LocalDate.now()),
                                c.getCita() == null ? null : c.getCita().getId()))
                        .toList(),
                historial.stream()
                        .map(m -> new MedicionResponse(
                                m.getId(),
                                m.getNumero(),
                                m.getParametro().name(),
                                m.getValor(),
                                m.getMedidoEn(),
                                m.getOrigen().name(),
                                m.isFueraDeRango()))
                        .toList());
    }

    private static int vencidos(PlanTratamiento p) {
        return (int) p.getControles().stream()
                .filter(c -> c.getEstado() == EstadoControl.PENDIENTE
                        && c.getFechaProgramada().isBefore(LocalDate.now()))
                .count();
    }

    private static int adherencia(PlanTratamiento p) {
        List<PlanControl> debidos = p.getControles().stream()
                .filter(c ->
                        !c.getFechaProgramada().isAfter(LocalDate.now()) || c.getEstado() != EstadoControl.PENDIENTE)
                .toList();
        if (debidos.isEmpty()) {
            return 100;
        }
        long cumplidos = debidos.stream()
                .filter(c -> c.getEstado() == EstadoControl.CUMPLIDO)
                .count();
        return (int) Math.round(cumplidos * 100.0 / debidos.size());
    }

    private static String riesgo(PlanTratamiento p, List<PlanMedicion> historial, int vencidos) {
        int tamano = historial.size();
        List<PlanMedicion> ultimas = historial.subList(Math.max(0, tamano - 3), tamano);
        long fuera = ultimas.stream().filter(PlanMedicion::isFueraDeRango).count();
        if (vencidos >= 2 || (ultimas.size() == 3 && fuera == 3)) {
            return "ALTO";
        }
        if (vencidos == 1 || fuera >= 1) {
            return "MEDIO";
        }
        return "BAJO";
    }

    private static int peso(String riesgo) {
        return switch (riesgo) {
            case "ALTO" -> 3;
            case "MEDIO" -> 2;
            default -> 1;
        };
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}

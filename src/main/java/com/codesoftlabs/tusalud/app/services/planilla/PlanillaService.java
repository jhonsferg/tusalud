package com.codesoftlabs.tusalud.app.services.planilla;

import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.CalcularPlanillaRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.PagarPlanillaRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ArchivoPlanillaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.AsistenciaDiaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.BoletaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.MiBoletaResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.PlanillaDetalleResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.PlanillaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ResumenPlanillaResponse;
import com.codesoftlabs.tusalud.app.entities.AsistenciaDia;
import com.codesoftlabs.tusalud.app.entities.Ausencia;
import com.codesoftlabs.tusalud.app.entities.BoletaLinea;
import com.codesoftlabs.tusalud.app.entities.BoletaPago;
import com.codesoftlabs.tusalud.app.entities.ContratoLaboral;
import com.codesoftlabs.tusalud.app.entities.Empleado;
import com.codesoftlabs.tusalud.app.entities.EmpleadoConcepto;
import com.codesoftlabs.tusalud.app.entities.Planilla;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.DestinoExtra;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoAsistencia;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoContrato;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoPlanilla;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.ModoConcepto;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AsistenciaDiaRepository;
import com.codesoftlabs.tusalud.app.repositories.AusenciaRepository;
import com.codesoftlabs.tusalud.app.repositories.BoletaPagoRepository;
import com.codesoftlabs.tusalud.app.repositories.ContratoLaboralRepository;
import com.codesoftlabs.tusalud.app.repositories.EmpleadoConceptoRepository;
import com.codesoftlabs.tusalud.app.repositories.EmpleadoRepository;
import com.codesoftlabs.tusalud.app.repositories.PlanillaRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.PlanillaAprobada;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.PlanillaPagada;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanillaService {

    private static final String ENTIDAD = "PLANILLA";
    private static final Set<TipoAusencia> SIN_GOCE =
            EnumSet.of(TipoAusencia.PERMISO, TipoAusencia.LICENCIA, TipoAusencia.OTRO);
    private static final Set<EstadoPlanilla> VISIBLES_AL_EMPLEADO =
            EnumSet.of(EstadoPlanilla.PAGADA, EstadoPlanilla.CERRADA);
    private static final Map<RolNombre, String> APPS = new LinkedHashMap<>();

    static {
        APPS.put(RolNombre.ROLE_MEDICO, "medico");
        APPS.put(RolNombre.ROLE_ENFERMERIA, "enfermeria");
        APPS.put(RolNombre.ROLE_RECEPCION, "recepcion");
        APPS.put(RolNombre.ROLE_CAJA, "caja");
        APPS.put(RolNombre.ROLE_LABORATORIO, "laboratorio");
        APPS.put(RolNombre.ROLE_IMAGENES, "imagenes");
        APPS.put(RolNombre.ROLE_FARMACIA, "farmacia");
        APPS.put(RolNombre.ROLE_ALMACEN, "almacen");
        APPS.put(RolNombre.ROLE_FINANZAS, "finanzas");
        APPS.put(RolNombre.ROLE_OPERACIONES, "operaciones");
        APPS.put(RolNombre.ROLE_CALIDAD, "calidad");
        APPS.put(RolNombre.ROLE_RRHH, "rrhh");
        APPS.put(RolNombre.ROLE_GERENCIA, "gerencia");
        APPS.put(RolNombre.ROLE_ADMIN, "admin");
    }

    private final PlanillaRepository planillas;
    private final BoletaPagoRepository boletas;
    private final ContratoLaboralRepository contratos;
    private final EmpleadoRepository empleados;
    private final EmpleadoConceptoRepository asignaciones;
    private final AusenciaRepository ausencias;
    private final AsistenciaDiaRepository asistencias;
    private final UsuarioRepository usuarioRepository;
    private final ParametrosPlanillaService parametros;
    private final AsistenciaService asistenciaService;
    private final PlanillaSecuencias secuencias;
    private final PlanillaAuditor auditor;
    private final PlanillaAvisos avisos;
    private final ApplicationEventPublisher eventos;

    public List<PlanillaResponse> listar() {
        return planillas.findAllByOrderByAnioDescMesDesc().stream()
                .map(PlanillaMapper::planilla)
                .toList();
    }

    public PlanillaDetalleResponse obtener(UsuarioPrincipal actor, UUID id) {
        Planilla planilla = planilla(id);
        boolean verBoletas = tiene(actor, RolNombre.ROLE_RRHH) || tiene(actor, RolNombre.ROLE_FINANZAS);
        return new PlanillaDetalleResponse(
                PlanillaMapper.planilla(planilla),
                verBoletas
                        ? boletas.dePlanilla(id).stream()
                                .map(PlanillaMapper::boletaResumen)
                                .toList()
                        : List.of());
    }

    @Transactional
    public PlanillaResponse calcular(UsuarioPrincipal actor, CalcularPlanillaRequest request) {
        int anio = request.anio();
        int mes = request.mes();
        YearMonth periodo = YearMonth.of(anio, mes);
        if (periodo.isAfter(YearMonth.now())) {
            throw new ConflictException("No se puede calcular la planilla de un periodo futuro");
        }
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        Planilla planilla = planillas.findByAnioAndMes(anio, mes).orElse(null);
        if (planilla == null) {
            planilla = planillas.saveAndFlush(Planilla.builder()
                    .anio(anio)
                    .mes(mes)
                    .codigo(String.format("PLA-%d-%02d", anio, mes))
                    .estado(EstadoPlanilla.BORRADOR)
                    .cantidadBoletas(0)
                    .totalIngresos(BigDecimal.ZERO)
                    .totalDescuentos(BigDecimal.ZERO)
                    .totalNeto(BigDecimal.ZERO)
                    .totalAporteEmpleador(BigDecimal.ZERO)
                    .totalRenta(BigDecimal.ZERO)
                    .totalPension(BigDecimal.ZERO)
                    .build());
        }
        planilla = planillas.bloquear(planilla.getId()).orElseThrow();
        if (planilla.getEstado() != EstadoPlanilla.BORRADOR && planilla.getEstado() != EstadoPlanilla.CALCULADA) {
            throw new ConflictException("La planilla " + planilla.getCodigo() + " ya está " + planilla.getEstado()
                    + " y no se puede recalcular");
        }
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();
        asistenciaService.consolidarRango(desde, hasta, null);
        CalculadoraPlanilla.Parametros params = parametros.vigentesEn(hasta);

        Map<UUID, ContratoLaboral> porEmpleado = new LinkedHashMap<>();
        for (ContratoLaboral c : contratos.solapados(desde, hasta)) {
            porEmpleado.putIfAbsent(c.getEmpleado().getId(), c);
        }
        if (porEmpleado.isEmpty()) {
            throw new ConflictException("No hay contratos laborales vigentes en " + periodo);
        }

        Map<UUID, String> numeros = new HashMap<>();
        List<BoletaPago> anteriores = boletas.dePlanilla(planilla.getId());
        for (BoletaPago b : anteriores) {
            numeros.put(b.getEmpleado().getId(), b.getNumero());
        }
        boletas.deleteAll(anteriores);
        boletas.flush();

        BigDecimal totalIngresos = BigDecimal.ZERO;
        BigDecimal totalDescuentos = BigDecimal.ZERO;
        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal totalAporte = BigDecimal.ZERO;
        BigDecimal totalRenta = BigDecimal.ZERO;
        BigDecimal totalPension = BigDecimal.ZERO;
        List<String> huellas = new ArrayList<>();
        int cantidad = 0;
        List<ContratoLaboral> ordenados = porEmpleado.values().stream()
                .sorted(Comparator.comparing(c -> c.getEmpleado().getUsuario().getNombreCompleto()))
                .toList();
        for (ContratoLaboral contrato : ordenados) {
            BoletaPago boleta = construirBoleta(planilla, contrato, desde, hasta, params, numeros);
            boletas.save(boleta);
            totalIngresos = totalIngresos.add(boleta.getTotalIngresos());
            totalDescuentos = totalDescuentos.add(boleta.getTotalDescuentos());
            totalNeto = totalNeto.add(boleta.getNeto());
            totalAporte = totalAporte.add(boleta.getAporteEmpleador());
            totalRenta = totalRenta.add(boleta.getRentaQuinta());
            totalPension = totalPension.add(boleta.getPension());
            huellas.add(boleta.getNumero() + ":" + boleta.getHash());
            cantidad++;
        }
        huellas.sort(String::compareTo);
        planilla.setEstado(EstadoPlanilla.CALCULADA);
        planilla.setCantidadBoletas(cantidad);
        planilla.setTotalIngresos(totalIngresos);
        planilla.setTotalDescuentos(totalDescuentos);
        planilla.setTotalNeto(totalNeto);
        planilla.setTotalAporteEmpleador(totalAporte);
        planilla.setTotalRenta(totalRenta);
        planilla.setTotalPension(totalPension);
        planilla.setHash(sha256(planilla.getCodigo() + "|" + String.join(",", huellas)));
        planilla.setCalculadaPor(usuario);
        planilla.setCalculadaEn(LocalDateTime.now());
        planilla.setAprobadaPor(null);
        planilla.setAprobadaEn(null);
        auditor.registrar(
                usuario,
                ENTIDAD,
                planilla.getId(),
                "CALCULAR",
                planilla.getCodigo() + ", " + cantidad + " boletas, neto " + totalNeto);
        avisos.avisarRol(
                RolNombre.ROLE_FINANZAS,
                actor.getId(),
                TipoNotificacion.PLANILLA_POR_APROBAR,
                "Planilla por aprobar",
                "La planilla " + planilla.getCodigo() + " con " + cantidad + " boletas espera su aprobación.",
                "/finanzas/planilla/" + planilla.getId());
        return PlanillaMapper.planilla(planilla);
    }

    private BoletaPago construirBoleta(
            Planilla planilla,
            ContratoLaboral contrato,
            LocalDate desde,
            LocalDate hasta,
            CalculadoraPlanilla.Parametros params,
            Map<UUID, String> numeros) {
        Empleado empleado = contrato.getEmpleado();
        LocalDate inicio = contrato.getFechaInicio().isAfter(desde) ? contrato.getFechaInicio() : desde;
        LocalDate fin = contrato.getFechaFin() != null && contrato.getFechaFin().isBefore(hasta)
                ? contrato.getFechaFin()
                : hasta;
        int diasMes = params.get("DIAS_MES").intValue();
        long calendario = ChronoUnit.DAYS.between(inicio, fin) + 1;
        int diasPagables = inicio.equals(desde) && fin.equals(hasta) ? diasMes : (int) Math.min(calendario, diasMes);

        List<AsistenciaDia> filas = asistencias.entre(inicio, fin, empleado.getId());
        int diasFalta = 0;
        int minutosTardanza = 0;
        int extra1 = 0;
        int extra2 = 0;
        int tramo = params.get("HORAS_EXTRA_TRAMO_MINUTOS").intValue();
        for (AsistenciaDia d : filas) {
            if (d.getEstado() == EstadoAsistencia.FALTA && !d.isFaltaJustificada()) {
                diasFalta++;
            }
            minutosTardanza += d.getMinutosTardanza();
            if (d.getExtraDestino() == DestinoExtra.PAGO && d.getMinutosExtra() > 0) {
                extra1 += Math.min(d.getMinutosExtra(), tramo);
                extra2 += Math.max(0, d.getMinutosExtra() - tramo);
            }
        }
        int diasNoRemunerados = 0;
        for (Ausencia a : ausencias.aprobadasDeEmpleado(empleado.getId(), inicio, fin)) {
            if (!SIN_GOCE.contains(a.getTipo())) {
                continue;
            }
            LocalDate ini = a.getFechaInicio().isBefore(inicio) ? inicio : a.getFechaInicio();
            LocalDate ter = a.getFechaFin().isAfter(fin) ? fin : a.getFechaFin();
            diasNoRemunerados += (int) (ChronoUnit.DAYS.between(ini, ter) + 1);
        }

        List<CalculadoraPlanilla.ConceptoAplicado> conceptos = new ArrayList<>();
        for (EmpleadoConcepto ec : asignaciones.aplicables(empleado.getId(), desde, hasta)) {
            BigDecimal monto = ec.getConcepto().getModo() == ModoConcepto.PORCENTAJE_BASICO
                    ? contrato.getRemuneracion()
                            .multiply(ec.getValor())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : ec.getValor().setScale(2, RoundingMode.HALF_UP);
            conceptos.add(new CalculadoraPlanilla.ConceptoAplicado(
                    ec.getConcepto().getCodigo(),
                    ec.getConcepto().getNombre(),
                    ec.getConcepto().getTipo(),
                    monto,
                    ec.getConcepto().isAfectoRenta(),
                    ec.getConcepto().isAfectoPension(),
                    ec.getConcepto().isAfectoEssalud()));
        }

        CalculadoraPlanilla.Resultado r = CalculadoraPlanilla.calcular(
                new CalculadoraPlanilla.Entrada(
                        contrato.getRegimen(),
                        contrato.getSistemaPensionario(),
                        contrato.getRemuneracion(),
                        contrato.isAsignacionFamiliar(),
                        diasPagables,
                        diasFalta,
                        diasNoRemunerados,
                        minutosTardanza,
                        extra1,
                        extra2,
                        conceptos),
                params);

        String numero = numeros.get(empleado.getId());
        if (numero == null) {
            numero = PlanillaSecuencias.formatear(
                    PlanillaSecuencias.BOLETA,
                    planilla.getAnio(),
                    secuencias.siguiente(PlanillaSecuencias.BOLETA, planilla.getAnio()));
        }
        BoletaPago boleta = BoletaPago.builder()
                .planilla(planilla)
                .empleado(empleado)
                .contrato(contrato)
                .numero(numero)
                .empleadoNombre(empleado.getUsuario().getNombreCompleto())
                .cargo(contrato.getCargo())
                .regimen(contrato.getRegimen())
                .sistemaPensionario(contrato.getSistemaPensionario())
                .banco(contrato.getBanco())
                .cuenta(contrato.getCuenta())
                .diasLaborados(Math.max(0, diasPagables - diasFalta - diasNoRemunerados))
                .diasFalta(diasFalta)
                .diasNoRemunerados(diasNoRemunerados)
                .minutosTardanza(minutosTardanza)
                .minutosExtraPagados(extra1 + extra2)
                .remuneracionBasica(contrato.getRemuneracion())
                .totalIngresos(r.totalIngresos())
                .totalDescuentos(r.totalDescuentos())
                .rentaQuinta(r.rentaQuinta())
                .pension(r.pension())
                .aporteEmpleador(r.aporteEmpleador())
                .neto(r.neto())
                .provisionGratificacion(r.provisionGratificacion())
                .provisionCts(r.provisionCts())
                .provisionVacaciones(r.provisionVacaciones())
                .build();
        int posicion = 0;
        for (CalculadoraPlanilla.Linea l : r.lineas()) {
            boleta.getLineas()
                    .add(BoletaLinea.builder()
                            .boleta(boleta)
                            .posicion(posicion++)
                            .codigo(l.codigo())
                            .nombre(l.nombre())
                            .tipo(l.tipo())
                            .base(l.base())
                            .monto(l.monto())
                            .build());
        }
        boleta.setHash(huellaBoleta(boleta, planilla));
        return boleta;
    }

    @Transactional
    public PlanillaResponse rechazar(UsuarioPrincipal actor, UUID id, String motivo) {
        Planilla planilla =
                planillas.bloquear(id).orElseThrow(() -> new ResourceNotFoundException("Planilla", "id", id));
        if (planilla.getEstado() != EstadoPlanilla.CALCULADA) {
            throw new ConflictException("Solo se devuelve una planilla calculada");
        }
        planilla.setEstado(EstadoPlanilla.BORRADOR);
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        auditor.registrar(usuario, ENTIDAD, id, "DEVOLVER", motivo);
        avisos.avisarRol(
                RolNombre.ROLE_RRHH,
                actor.getId(),
                TipoNotificacion.PLANILLA_APROBADA,
                "Planilla devuelta",
                "La planilla " + planilla.getCodigo() + " fue devuelta: " + motivo,
                "/rrhh/planilla/" + id);
        return PlanillaMapper.planilla(planilla);
    }

    @Transactional
    public PlanillaResponse aprobar(UsuarioPrincipal actor, UUID id) {
        Planilla planilla =
                planillas.bloquear(id).orElseThrow(() -> new ResourceNotFoundException("Planilla", "id", id));
        if (planilla.getEstado() != EstadoPlanilla.CALCULADA) {
            throw new ConflictException("Solo se aprueba una planilla calculada");
        }
        if (planilla.getCalculadaPor() != null
                && planilla.getCalculadaPor().getId().equals(actor.getId())) {
            throw new ConflictException("Quien calculó la planilla no puede aprobarla");
        }
        List<BoletaPago> lista = boletas.dePlanilla(id);
        List<String> huellas = new ArrayList<>();
        BigDecimal faltasTardanza = BigDecimal.ZERO;
        for (BoletaPago b : lista) {
            if (!huellaBoleta(b, planilla).equals(b.getHash())) {
                throw new ConflictException(
                        "La boleta " + b.getNumero() + " no coincide con su huella, recalcule la planilla");
            }
            huellas.add(b.getNumero() + ":" + b.getHash());
            for (BoletaLinea l : b.getLineas()) {
                if (l.getCodigo().equals("DESC_FALTAS") || l.getCodigo().equals("DESC_TARDANZA")) {
                    faltasTardanza = faltasTardanza.add(l.getMonto());
                }
            }
        }
        huellas.sort(String::compareTo);
        if (!sha256(planilla.getCodigo() + "|" + String.join(",", huellas)).equals(planilla.getHash())) {
            throw new ConflictException("La planilla fue alterada después de calcularse, recalcule antes de aprobar");
        }
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        planilla.setEstado(EstadoPlanilla.APROBADA);
        planilla.setAprobadaPor(usuario);
        planilla.setAprobadaEn(LocalDateTime.now());
        BigDecimal remuneracionBruta = planilla.getTotalIngresos().subtract(faltasTardanza);
        BigDecimal otrosDescuentos = planilla.getTotalDescuentos()
                .subtract(planilla.getTotalPension())
                .subtract(planilla.getTotalRenta())
                .subtract(faltasTardanza);
        auditor.registrar(usuario, ENTIDAD, id, "APROBAR", planilla.getCodigo() + ", neto " + planilla.getTotalNeto());
        eventos.publishEvent(new PlanillaAprobada(
                id,
                planilla.getCodigo(),
                LocalDate.now(),
                remuneracionBruta,
                planilla.getTotalAporteEmpleador(),
                planilla.getTotalPension(),
                planilla.getTotalRenta(),
                otrosDescuentos,
                planilla.getTotalNeto()));
        avisos.avisarRol(
                RolNombre.ROLE_RRHH,
                actor.getId(),
                TipoNotificacion.PLANILLA_APROBADA,
                "Planilla aprobada",
                "La planilla " + planilla.getCodigo() + " fue aprobada y queda pendiente de pago.",
                "/rrhh/planilla/" + id);
        return PlanillaMapper.planilla(planilla);
    }

    @Transactional
    public PlanillaResponse pagar(UsuarioPrincipal actor, UUID id, PagarPlanillaRequest request) {
        Planilla planilla =
                planillas.bloquear(id).orElseThrow(() -> new ResourceNotFoundException("Planilla", "id", id));
        if (planilla.getEstado() != EstadoPlanilla.APROBADA) {
            throw new ConflictException("Solo se paga una planilla aprobada");
        }
        if (planilla.getAprobadaPor() != null
                && planilla.getAprobadaPor().getId().equals(actor.getId())) {
            throw new ConflictException("Quien aprobó la planilla no puede registrar su pago");
        }
        if (request.fechaPago().isAfter(LocalDate.now())) {
            throw new ConflictException("La fecha de pago no puede ser futura");
        }
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        planilla.setEstado(EstadoPlanilla.PAGADA);
        planilla.setPagadaPor(usuario);
        planilla.setPagadaEn(LocalDateTime.now());
        planilla.setFechaPago(request.fechaPago());
        planilla.setReferenciaPago(request.referencia().trim());
        auditor.registrar(
                usuario,
                ENTIDAD,
                id,
                "PAGAR",
                planilla.getCodigo() + ", referencia " + request.referencia().trim());
        eventos.publishEvent(
                new PlanillaPagada(id, planilla.getCodigo(), request.fechaPago(), planilla.getTotalNeto()));
        String periodo = Month.of(planilla.getMes()).getDisplayName(TextStyle.FULL, Locale.of("es", "PE")) + " "
                + planilla.getAnio();
        for (BoletaPago b : boletas.dePlanilla(id)) {
            Usuario destinatario = b.getEmpleado().getUsuario();
            avisos.avisarUsuario(
                    destinatario,
                    TipoNotificacion.BOLETA_DISPONIBLE,
                    "Boleta de pago disponible",
                    "Su boleta de " + periodo + " ya está disponible.",
                    "/" + appDe(destinatario) + "/mis-boletas");
        }
        return PlanillaMapper.planilla(planilla);
    }

    @Transactional
    public PlanillaResponse cerrar(UsuarioPrincipal actor, UUID id) {
        Planilla planilla =
                planillas.bloquear(id).orElseThrow(() -> new ResourceNotFoundException("Planilla", "id", id));
        if (planilla.getEstado() != EstadoPlanilla.PAGADA) {
            throw new ConflictException("Solo se cierra una planilla pagada");
        }
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        planilla.setEstado(EstadoPlanilla.CERRADA);
        planilla.setCerradaPor(usuario);
        planilla.setCerradaEn(LocalDateTime.now());
        auditor.registrar(usuario, ENTIDAD, id, "CERRAR", planilla.getCodigo());
        return PlanillaMapper.planilla(planilla);
    }

    public BoletaResponse boleta(UsuarioPrincipal actor, UUID boletaId) {
        BoletaPago boleta = boletas.findConDetalle(boletaId)
                .orElseThrow(() -> new ResourceNotFoundException("Boleta", "id", boletaId));
        boolean personalDePlanilla = tiene(actor, RolNombre.ROLE_RRHH) || tiene(actor, RolNombre.ROLE_FINANZAS);
        if (personalDePlanilla) {
            return PlanillaMapper.boleta(boleta, true);
        }
        boolean propia = boleta.getEmpleado().getUsuario().getId().equals(actor.getId());
        if (!propia || !VISIBLES_AL_EMPLEADO.contains(boleta.getPlanilla().getEstado())) {
            throw new ForbiddenOperationException("No tiene acceso a esta boleta");
        }
        return PlanillaMapper.boleta(boleta, false);
    }

    public BoletaPago boletaParaDocumento(UsuarioPrincipal actor, UUID boletaId) {
        BoletaResponse permitida = boleta(actor, boletaId);
        return boletas.findConDetalle(permitida.id()).orElseThrow();
    }

    public List<MiBoletaResumenResponse> misBoletas(UsuarioPrincipal actor) {
        Empleado empleado = empleados.findByUsuarioId(actor.getId()).orElse(null);
        if (empleado == null) {
            return List.of();
        }
        return boletas.deEmpleado(empleado.getId(), VISIBLES_AL_EMPLEADO).stream()
                .map(PlanillaMapper::miBoleta)
                .toList();
    }

    @Transactional
    public ArchivoPlanillaResponse archivoBancario(UsuarioPrincipal actor, UUID id) {
        Planilla planilla = planilla(id);
        if (planilla.getEstado() == EstadoPlanilla.BORRADOR || planilla.getEstado() == EstadoPlanilla.CALCULADA) {
            throw new ConflictException("La planilla debe estar aprobada para generar el archivo bancario");
        }
        StringBuilder csv = new StringBuilder("numero_boleta,empleado,banco,cuenta,monto,moneda,referencia\n");
        int filas = 0;
        for (BoletaPago b : boletas.dePlanilla(id)) {
            if (b.getNeto().signum() <= 0) {
                continue;
            }
            csv.append(campo(b.getNumero()))
                    .append(',')
                    .append(campo(b.getEmpleadoNombre()))
                    .append(',')
                    .append(campo(b.getBanco()))
                    .append(',')
                    .append(campo(b.getCuenta()))
                    .append(',')
                    .append(b.getNeto().setScale(2, RoundingMode.HALF_UP).toPlainString())
                    .append(',')
                    .append("PEN,")
                    .append(campo("Planilla " + planilla.getCodigo()))
                    .append('\n');
            filas++;
        }
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()), ENTIDAD, id, "EXPORTAR_BANCARIO", filas + " abonos");
        return new ArchivoPlanillaResponse(
                "pagos-" + planilla.getCodigo() + ".csv", csv.toString(), filas, "CSV", false);
    }

    @Transactional
    public ArchivoPlanillaResponse plame(UsuarioPrincipal actor, UUID id) {
        Planilla planilla = planilla(id);
        if (planilla.getEstado() == EstadoPlanilla.BORRADOR || planilla.getEstado() == EstadoPlanilla.CALCULADA) {
            throw new ConflictException("La planilla debe estar aprobada para generar el archivo");
        }
        StringBuilder txt = new StringBuilder();
        int filas = 0;
        for (BoletaPago b : boletas.dePlanilla(id)) {
            txt.append(b.getEmpleado().getCodigoMarcacion())
                    .append('|')
                    .append(b.getEmpleadoNombre())
                    .append('|')
                    .append(b.getDiasLaborados())
                    .append('|')
                    .append(plano(b.getTotalIngresos()))
                    .append('|')
                    .append(plano(b.getTotalDescuentos()))
                    .append('|')
                    .append(plano(b.getNeto()))
                    .append('|')
                    .append(plano(b.getAporteEmpleador()))
                    .append('|')
                    .append(b.getSistemaPensionario())
                    .append('\n');
            filas++;
        }
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()), ENTIDAD, id, "EXPORTAR_PLAME", filas + " filas");
        return new ArchivoPlanillaResponse(
                String.format("plame-%d%02d.txt", planilla.getAnio(), planilla.getMes()),
                txt.toString(),
                filas,
                "TXT",
                true);
    }

    public ResumenPlanillaResponse resumen() {
        LocalDate hoy = LocalDate.now();
        List<ContratoLaboral> vigentes = contratos.solapados(hoy, hoy);
        Map<UUID, ContratoLaboral> porEmpleado = new LinkedHashMap<>();
        for (ContratoLaboral c : vigentes) {
            if (c.getEstado() == EstadoContrato.VIGENTE) {
                porEmpleado.putIfAbsent(c.getEmpleado().getId(), c);
            }
        }
        BigDecimal masa = porEmpleado.values().stream()
                .map(ContratoLaboral::getRemuneracion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Planilla> todas = planillas.findAllByOrderByAnioDescMesDesc();
        long pendientes = todas.stream()
                .filter(p -> p.getEstado() == EstadoPlanilla.CALCULADA)
                .count();
        return new ResumenPlanillaResponse(
                porEmpleado.size(),
                masa,
                todas.isEmpty() ? null : PlanillaMapper.planilla(todas.get(0)),
                pendientes,
                asistenciaService.marcacionesDeHoy());
    }

    public List<AsistenciaDiaResponse> asistenciaDePlanilla(UUID planillaId) {
        Planilla planilla = planilla(planillaId);
        YearMonth periodo = YearMonth.of(planilla.getAnio(), planilla.getMes());
        return asistenciaService.dias(null, periodo.atDay(1), periodo.atEndOfMonth());
    }

    private Planilla planilla(UUID id) {
        return planillas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Planilla", "id", id));
    }

    private static boolean tiene(UsuarioPrincipal actor, RolNombre rol) {
        return actor.getRoles().contains(rol.name());
    }

    private static String appDe(Usuario usuario) {
        return APPS.entrySet().stream()
                .filter(e -> usuario.getRoles().stream().anyMatch(r -> r.getNombre() == e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("paciente");
    }

    private static String campo(String valor) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.replace("\"", "'").replace("\n", " ").replace("\r", " ");
        if (limpio.startsWith("=") || limpio.startsWith("+") || limpio.startsWith("-") || limpio.startsWith("@")) {
            limpio = "'" + limpio;
        }
        return limpio.contains(",") ? "\"" + limpio + "\"" : limpio;
    }

    private static String plano(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static String huellaBoleta(BoletaPago b, Planilla planilla) {
        StringBuilder canonico = new StringBuilder()
                .append(b.getNumero())
                .append('|')
                .append(b.getEmpleado().getId())
                .append('|')
                .append(planilla.getAnio())
                .append('-')
                .append(planilla.getMes())
                .append('|')
                .append(plano(b.getRemuneracionBasica()))
                .append('|')
                .append(plano(b.getTotalIngresos()))
                .append('|')
                .append(plano(b.getTotalDescuentos()))
                .append('|')
                .append(plano(b.getNeto()))
                .append('|')
                .append(plano(b.getAporteEmpleador()))
                .append('|');
        for (BoletaLinea l : b.getLineas()) {
            canonico.append(l.getCodigo())
                    .append(':')
                    .append(plano(l.getMonto()))
                    .append(';');
        }
        return sha256(canonico.toString());
    }

    static String sha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

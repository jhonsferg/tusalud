package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.AjusteAsistenciaRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.CalcularPlanillaRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ConceptoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ConsolidarRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ContratoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.EmpleadoConceptoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.MarcacionManualRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.MarcacionTerminalRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.PagarPlanillaRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ParametroRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.TerminalRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.AsistenciaDiaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.BoletaResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.PlanillaDetalleResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.PlanillaResponse;
import com.codesoftlabs.tusalud.app.entities.Ausencia;
import com.codesoftlabs.tusalud.app.entities.Empleado;
import com.codesoftlabs.tusalud.app.entities.ParametroPlanilla;
import com.codesoftlabs.tusalud.app.entities.PlantillaTurno;
import com.codesoftlabs.tusalud.app.entities.TurnoPersonal;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.DestinoExtra;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoAsistencia;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoPlanilla;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.ModoConcepto;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.RegimenLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.SistemaPensionario;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoConcepto;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoMarcacion;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.TipoContrato;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AusenciaRepository;
import com.codesoftlabs.tusalud.app.repositories.EmpleadoRepository;
import com.codesoftlabs.tusalud.app.repositories.ParametroPlanillaRepository;
import com.codesoftlabs.tusalud.app.repositories.PlantillaTurnoRepository;
import com.codesoftlabs.tusalud.app.repositories.TurnoPersonalRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.PlanillaAprobada;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.PlanillaPagada;
import com.codesoftlabs.tusalud.app.services.planilla.AsistenciaService;
import com.codesoftlabs.tusalud.app.services.planilla.ConceptosPlanillaService;
import com.codesoftlabs.tusalud.app.services.planilla.ContratosLaboralesService;
import com.codesoftlabs.tusalud.app.services.planilla.ParametrosPlanillaService;
import com.codesoftlabs.tusalud.app.services.planilla.PlanillaService;
import com.codesoftlabs.tusalud.app.services.planilla.TerminalesAsistenciaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
@RecordApplicationEvents
class PlanillaServiceTest {

    @Autowired
    private PlanillaService planillas;

    @Autowired
    private ContratosLaboralesService contratos;

    @Autowired
    private ConceptosPlanillaService conceptos;

    @Autowired
    private ParametrosPlanillaService parametros;

    @Autowired
    private AsistenciaService asistencia;

    @Autowired
    private TerminalesAsistenciaService terminales;

    @Autowired
    private ParametroPlanillaRepository parametroRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private PlantillaTurnoRepository plantillaRepository;

    @Autowired
    private TurnoPersonalRepository turnoRepository;

    @Autowired
    private AusenciaRepository ausenciaRepository;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private ApplicationEvents eventos;

    private Usuario rrhhUsuario;
    private UsuarioPrincipal rrhh;
    private UsuarioPrincipal finanzas;
    private UsuarioPrincipal otraFinanzas;
    private Empleado ana;
    private Empleado beto;
    private UsuarioPrincipal anaPrincipal;
    private UsuarioPrincipal betoPrincipal;
    private final YearMonth mesAnterior = YearMonth.now().minusMonths(1);

    @BeforeEach
    void setUp() {
        for (Map.Entry<String, BigDecimal> e : CalculadoraPlanillaTest.VALORES.entrySet()) {
            parametroRepository.save(ParametroPlanilla.builder()
                    .codigo(e.getKey())
                    .valor(e.getValue())
                    .vigenteDesde(LocalDate.of(2020, 1, 1))
                    .descripcion(e.getKey())
                    .porValidar(true)
                    .build());
        }
        rrhhUsuario = fixture.usuario("rrhh", RolNombre.ROLE_RRHH);
        rrhh = fixture.principal(rrhhUsuario);
        finanzas = fixture.principal(fixture.usuario("fin", RolNombre.ROLE_FINANZAS));
        otraFinanzas = fixture.principal(fixture.usuario("fin2", RolNombre.ROLE_FINANZAS));
        Usuario usuarioAna = fixture.usuario("ana", RolNombre.ROLE_MEDICO);
        Usuario usuarioBeto = fixture.usuario("beto", RolNombre.ROLE_ENFERMERIA);
        ana = empleado(usuarioAna, AreaLaboral.MEDICA, "3000");
        beto = empleado(usuarioBeto, AreaLaboral.ENFERMERIA, "2000");
        anaPrincipal = fixture.principal(usuarioAna);
        betoPrincipal = fixture.principal(usuarioBeto);
    }

    private Empleado empleado(Usuario usuario, AreaLaboral area, String sueldo) {
        return empleadoRepository.save(Empleado.builder()
                .usuario(usuario)
                .cargo("Cargo")
                .area(area)
                .tipoContrato(TipoContrato.INDEFINIDO)
                .fechaIngreso(LocalDate.now().minusYears(2))
                .estado(EstadoEmpleado.ACTIVO)
                .sueldo(new BigDecimal(sueldo))
                .build());
    }

    private ContratoRequest contrato(Empleado empleado, String remuneracion, SistemaPensionario pension) {
        return new ContratoRequest(
                empleado.getId(),
                RegimenLaboral.GENERAL,
                new BigDecimal(remuneracion),
                false,
                pension,
                pension == SistemaPensionario.AFP ? "Integra" : null,
                48,
                "Banco de Crédito",
                "19412345678",
                "Cargo contractual",
                LocalDate.now().minusYears(2),
                null);
    }

    private void contratosBase() {
        contratos.crear(rrhh, contrato(ana, "3000", SistemaPensionario.ONP));
        contratos.crear(rrhh, contrato(beto, "2000", SistemaPensionario.AFP));
    }

    private PlanillaResponse calcular() {
        return planillas.calcular(
                rrhh, new CalcularPlanillaRequest(mesAnterior.getYear(), mesAnterior.getMonthValue()));
    }

    private PlantillaTurno plantilla(String nombre, int inicio, int fin) {
        return plantillaRepository.save(PlantillaTurno.builder()
                .nombre(nombre + UUID.randomUUID().toString().substring(0, 6))
                .horaInicio(LocalTime.of(inicio, 0))
                .horaFin(LocalTime.of(fin, 0))
                .activa(true)
                .build());
    }

    private TurnoPersonal turno(Empleado empleado, PlantillaTurno plantilla, LocalDate fecha) {
        return turnoRepository.save(TurnoPersonal.builder()
                .empleado(empleado)
                .plantilla(plantilla)
                .fecha(fecha)
                .creadoPor(rrhhUsuario)
                .build());
    }

    @Test
    void elContratoSeCreaConSusReglasYNoAdmiteSolapamiento() {
        var creado = contratos.crear(rrhh, contrato(ana, "3000", SistemaPensionario.ONP));

        assertThat(creado.cuentaEnmascarada()).isEqualTo("****5678");
        assertThat(creado.remuneracion()).isEqualByComparingTo("3000");
        assertThat(empleadoRepository.findById(ana.getId()).orElseThrow().getSueldo())
                .isEqualByComparingTo("3000");
        assertThatThrownBy(() -> contratos.crear(rrhh, contrato(ana, "3500", SistemaPensionario.ONP)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> contratos.crear(
                        rrhh,
                        new ContratoRequest(
                                beto.getId(),
                                RegimenLaboral.GENERAL,
                                new BigDecimal("2000"),
                                false,
                                SistemaPensionario.AFP,
                                null,
                                48,
                                null,
                                null,
                                "Cargo",
                                LocalDate.now().minusYears(1),
                                null)))
                .hasMessageContaining("AFP");
    }

    @Test
    void elCicloCompletoCalculaApruebaPagaYCierraConSusAsientos() {
        contratosBase();

        PlanillaResponse calculada = calcular();

        assertThat(calculada.estado()).isEqualTo(EstadoPlanilla.CALCULADA);
        assertThat(calculada.cantidadBoletas()).isEqualTo(2);
        assertThat(calculada.totalIngresos()).isEqualByComparingTo("5000.00");
        assertThat(calculada.hash()).hasSize(64);

        PlanillaResponse aprobada = planillas.aprobar(finanzas, calculada.id());
        assertThat(aprobada.estado()).isEqualTo(EstadoPlanilla.APROBADA);
        PlanillaAprobada hecho =
                eventos.stream(PlanillaAprobada.class).findFirst().orElseThrow();
        BigDecimal debe = hecho.remuneracionBruta().add(hecho.aporteEmpleador());
        BigDecimal haber = hecho.retencionPension()
                .add(hecho.retencionRenta())
                .add(hecho.otrosDescuentos())
                .add(hecho.aporteEmpleador())
                .add(hecho.neto());
        assertThat(debe).isEqualByComparingTo(haber);
        assertThat(hecho.neto()).isEqualByComparingTo(calculada.totalNeto());

        PlanillaResponse pagada =
                planillas.pagar(otraFinanzas, calculada.id(), new PagarPlanillaRequest(LocalDate.now(), "TRF-001"));
        assertThat(pagada.estado()).isEqualTo(EstadoPlanilla.PAGADA);
        assertThat(eventos.stream(PlanillaPagada.class).count()).isEqualTo(1);

        assertThat(planillas.cerrar(rrhh, calculada.id()).estado()).isEqualTo(EstadoPlanilla.CERRADA);
        assertThatThrownBy(() -> calcular()).isInstanceOf(ConflictException.class);
    }

    @Test
    void quienCalculaNoApruebaYQuienApruebaNoPaga() {
        contratosBase();
        var rrhhYFinanzas = fixture.usuario("mixto", RolNombre.ROLE_RRHH);
        rrhhYFinanzas.getRoles().add(fixture.rol(RolNombre.ROLE_FINANZAS));
        UsuarioPrincipal mixto = UsuarioPrincipal.from(rrhhYFinanzas);
        PlanillaResponse calculada = planillas.calcular(
                mixto, new CalcularPlanillaRequest(mesAnterior.getYear(), mesAnterior.getMonthValue()));

        assertThatThrownBy(() -> planillas.aprobar(mixto, calculada.id()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("calculó");

        planillas.aprobar(finanzas, calculada.id());
        assertThatThrownBy(
                        () -> planillas.pagar(finanzas, calculada.id(), new PagarPlanillaRequest(LocalDate.now(), "X")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("aprobó");
        assertThatThrownBy(() -> planillas.cerrar(rrhh, calculada.id())).isInstanceOf(ConflictException.class);
    }

    @Test
    void unaPlanillaNoSePuedeCalcularParaUnMesFuturo() {
        contratosBase();
        YearMonth futuro = YearMonth.now().plusMonths(1);

        assertThatThrownBy(() ->
                        planillas.calcular(rrhh, new CalcularPlanillaRequest(futuro.getYear(), futuro.getMonthValue())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void sinContratosNoHayPlanilla() {
        assertThatThrownBy(this::calcular).isInstanceOf(ConflictException.class);
    }

    @Test
    void recalcularConservaElNumeroDeBoletaYLaDevolucionPermiteCorregir() {
        contratosBase();
        PlanillaResponse primera = calcular();
        PlanillaDetalleResponse detalle1 = planillas.obtener(rrhh, primera.id());
        String numero = detalle1.boletas().get(0).numero();

        planillas.rechazar(finanzas, primera.id(), "Revisar");
        PlanillaResponse segunda = calcular();
        PlanillaDetalleResponse detalle2 = planillas.obtener(rrhh, segunda.id());

        assertThat(segunda.id()).isEqualTo(primera.id());
        assertThat(detalle2.boletas()).extracting(BoletaResumenResponse::numero).contains(numero);
        assertThat(detalle2.boletas()).hasSize(2);
    }

    @Test
    void elEmpleadoSoloVeSusBoletasYSoloDespuesDelPago() {
        contratosBase();
        PlanillaResponse calculada = calcular();
        PlanillaDetalleResponse detalle = planillas.obtener(rrhh, calculada.id());
        UUID boletaAna = detalle.boletas().stream()
                .filter(b -> b.empleadoId().equals(ana.getId()))
                .findFirst()
                .orElseThrow()
                .id();
        UUID boletaBeto = detalle.boletas().stream()
                .filter(b -> b.empleadoId().equals(beto.getId()))
                .findFirst()
                .orElseThrow()
                .id();

        assertThat(planillas.misBoletas(anaPrincipal)).isEmpty();
        assertThatThrownBy(() -> planillas.boleta(anaPrincipal, boletaAna))
                .isInstanceOf(ForbiddenOperationException.class);

        planillas.aprobar(finanzas, calculada.id());
        planillas.pagar(otraFinanzas, calculada.id(), new PagarPlanillaRequest(LocalDate.now(), "TRF-9"));

        assertThat(planillas.misBoletas(anaPrincipal)).hasSize(1);
        assertThat(planillas.boleta(anaPrincipal, boletaAna).cuentaEnmascarada())
                .isNull();
        assertThat(planillas.boleta(anaPrincipal, boletaAna).neto()).isPositive();
        assertThatThrownBy(() -> planillas.boleta(anaPrincipal, boletaBeto))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> planillas.boleta(betoPrincipal, boletaAna))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(planillas.boleta(rrhh, boletaBeto).cuentaEnmascarada()).isEqualTo("****5678");
        assertThat(planillas.boleta(finanzas, boletaAna).lineas()).isNotEmpty();
    }

    @Test
    void gerenciaVeLosTotalesPeroNoLasBoletas() {
        contratosBase();
        PlanillaResponse calculada = calcular();
        UsuarioPrincipal gerencia = fixture.principal(fixture.usuario("ger", RolNombre.ROLE_GERENCIA));

        PlanillaDetalleResponse detalle = planillas.obtener(gerencia, calculada.id());

        assertThat(detalle.planilla().totalNeto()).isPositive();
        assertThat(detalle.boletas()).isEmpty();
    }

    @Test
    void lasFaltasLasTardanzasYLasHorasExtraSalenDeLaAsistencia() {
        contratosBase();
        PlantillaTurno manana = plantilla("Mañana", 8, 16);
        LocalDate dia1 = mesAnterior.atDay(3);
        LocalDate dia2 = mesAnterior.atDay(4);
        LocalDate dia3 = mesAnterior.atDay(5);
        LocalDate dia4 = mesAnterior.atDay(6);
        turno(ana, manana, dia1);
        turno(ana, manana, dia2);
        turno(ana, manana, dia3);
        turno(ana, manana, dia4);
        asistencia.registrarManual(rrhh, manual(ana, TipoMarcacion.ENTRADA, dia1.atTime(8, 30)));
        asistencia.registrarManual(rrhh, manual(ana, TipoMarcacion.SALIDA, dia1.atTime(17, 0)));
        asistencia.registrarManual(rrhh, manual(ana, TipoMarcacion.ENTRADA, dia2.atTime(8, 5)));
        asistencia.registrarManual(rrhh, manual(ana, TipoMarcacion.SALIDA, dia2.atTime(16, 0)));
        ausenciaRepository.save(Ausencia.builder()
                .empleado(ana)
                .tipo(TipoAusencia.VACACIONES)
                .fechaInicio(dia4)
                .fechaFin(dia4)
                .dias(1)
                .estado(EstadoAusencia.APROBADA)
                .build());

        var consolidacion = asistencia.consolidar(
                rrhh, new ConsolidarRequest(mesAnterior.atDay(1), mesAnterior.atEndOfMonth(), ana.getId()));

        assertThat(consolidacion.dias()).isEqualTo(4);
        List<AsistenciaDiaResponse> dias =
                asistencia.dias(ana.getId(), mesAnterior.atDay(1), mesAnterior.atEndOfMonth());
        AsistenciaDiaResponse tarde =
                dias.stream().filter(d -> d.fecha().equals(dia1)).findFirst().orElseThrow();
        assertThat(tarde.estado()).isEqualTo(EstadoAsistencia.TARDANZA);
        assertThat(tarde.minutosTardanza()).isEqualTo(30);
        assertThat(tarde.minutosExtra()).isEqualTo(60);
        AsistenciaDiaResponse puntual =
                dias.stream().filter(d -> d.fecha().equals(dia2)).findFirst().orElseThrow();
        assertThat(puntual.estado()).isEqualTo(EstadoAsistencia.PRESENTE);
        assertThat(puntual.minutosTardanza()).isZero();
        AsistenciaDiaResponse falta =
                dias.stream().filter(d -> d.fecha().equals(dia3)).findFirst().orElseThrow();
        assertThat(falta.estado()).isEqualTo(EstadoAsistencia.FALTA);
        AsistenciaDiaResponse vacaciones =
                dias.stream().filter(d -> d.fecha().equals(dia4)).findFirst().orElseThrow();
        assertThat(vacaciones.estado()).isEqualTo(EstadoAsistencia.AUSENCIA_APROBADA);

        PlanillaResponse calculada = calcular();
        var boleta = planillas.boleta(
                rrhh,
                planillas.obtener(rrhh, calculada.id()).boletas().stream()
                        .filter(b -> b.empleadoId().equals(ana.getId()))
                        .findFirst()
                        .orElseThrow()
                        .id());
        assertThat(boleta.diasFalta()).isEqualTo(1);
        assertThat(boleta.minutosTardanza()).isEqualTo(30);
        assertThat(boleta.minutosExtraPagados()).isEqualTo(60);
        assertThat(boleta.lineas()).extracting(l -> l.codigo()).contains("DESC_FALTAS", "DESC_TARDANZA", "HORAS_EXTRA");

        asistencia.ajustar(
                rrhh, falta.id(), new AjusteAsistenciaRequest(true, null, null, null, "Certificado médico presentado"));
        planillas.rechazar(finanzas, calculada.id(), "Falta justificada");
        PlanillaResponse recalculada = calcular();
        var boleta2 = planillas.boleta(
                rrhh,
                planillas.obtener(rrhh, recalculada.id()).boletas().stream()
                        .filter(b -> b.empleadoId().equals(ana.getId()))
                        .findFirst()
                        .orElseThrow()
                        .id());
        assertThat(boleta2.diasFalta()).isZero();
        assertThat(boleta2.neto()).isGreaterThan(boleta.neto());
    }

    @Test
    void lasHorasExtraCompensadasNoSePagan() {
        contratosBase();
        PlantillaTurno manana = plantilla("Mañana", 8, 16);
        LocalDate dia = mesAnterior.atDay(10);
        turno(ana, manana, dia);
        asistencia.registrarManual(rrhh, manual(ana, TipoMarcacion.ENTRADA, dia.atTime(8, 0)));
        asistencia.registrarManual(rrhh, manual(ana, TipoMarcacion.SALIDA, dia.atTime(18, 0)));
        asistencia.consolidarRango(mesAnterior.atDay(1), mesAnterior.atEndOfMonth(), ana.getId());
        AsistenciaDiaResponse fila = asistencia.dias(ana.getId(), dia, dia).get(0);
        assertThat(fila.minutosExtra()).isEqualTo(120);

        asistencia.ajustar(
                rrhh,
                fila.id(),
                new AjusteAsistenciaRequest(null, null, null, DestinoExtra.COMPENSACION, "Se compensa con descanso"));
        PlanillaResponse calculada = calcular();

        var boleta = planillas.boleta(
                rrhh,
                planillas.obtener(rrhh, calculada.id()).boletas().stream()
                        .filter(b -> b.empleadoId().equals(ana.getId()))
                        .findFirst()
                        .orElseThrow()
                        .id());
        assertThat(boleta.minutosExtraPagados()).isZero();
    }

    @Test
    void unAjusteNoSePuedeHacerSobreUnMesAprobado() {
        contratosBase();
        PlantillaTurno manana = plantilla("Mañana", 8, 16);
        LocalDate dia = mesAnterior.atDay(11);
        turno(ana, manana, dia);
        asistencia.consolidarRango(mesAnterior.atDay(1), mesAnterior.atEndOfMonth(), ana.getId());
        AsistenciaDiaResponse falta = asistencia.dias(ana.getId(), dia, dia).get(0);
        PlanillaResponse calculada = calcular();
        planillas.aprobar(finanzas, calculada.id());

        assertThatThrownBy(() -> asistencia.ajustar(
                        rrhh, falta.id(), new AjusteAsistenciaRequest(true, null, null, null, "Tarde")))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> asistencia.registrarManual(rrhh, manual(ana, TipoMarcacion.ENTRADA, dia.atTime(9, 0))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void laTerminalMarcaAlternandoEntradaYSalidaYRechazaCodigosDesconocidos() {
        var terminal = terminales.crear(rrhh, new TerminalRequest("Puerta principal"));
        assertThat(terminal.token()).startsWith("tm_");
        String codigo = empleadoRepository.findById(ana.getId()).orElseThrow().getCodigoMarcacion();

        var entrada = asistencia.marcarEnTerminal(terminal.token(), new MarcacionTerminalRequest(codigo, null));
        assertThat(entrada.tipo()).isEqualTo(TipoMarcacion.ENTRADA);
        assertThatThrownBy(
                        () -> asistencia.marcarEnTerminal(terminal.token(), new MarcacionTerminalRequest(codigo, null)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() ->
                        asistencia.marcarEnTerminal(terminal.token(), new MarcacionTerminalRequest("ZZZZZZZZ", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> asistencia.marcarEnTerminal("tm_falso", new MarcacionTerminalRequest(codigo, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> asistencia.marcarEnTerminal(null, new MarcacionTerminalRequest(codigo, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        terminales.cambiarEstado(rrhh, terminal.id(), false);
        assertThatThrownBy(() -> asistencia.marcarEnTerminal(
                        terminal.token(), new MarcacionTerminalRequest(codigo, TipoMarcacion.SALIDA)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void laTerminalNoGuardaElTokenEnClaro() {
        var terminal = terminales.crear(rrhh, new TerminalRequest("Recepción"));

        assertThat(terminales.listar()).extracting(t -> t.nombre()).contains("Recepción");
        assertThat(terminales.autenticar(terminal.token())).isPresent();
        assertThat(terminales.autenticar(terminal.token() + "x")).isEmpty();
    }

    @Test
    void losParametrosSeVersionanPorVigenciaYRespetanLasPlanillasAprobadas() {
        LocalDate desde = LocalDate.now().plusMonths(2).withDayOfMonth(1);

        var nueva = parametros.nuevaVersion(rrhh, new ParametroRequest("RMV", new BigDecimal("1200"), desde, null));

        assertThat(nueva.porValidar()).isTrue();
        assertThat(parametros.listar("RMV", false)).hasSize(2);
        assertThatThrownBy(() -> parametros.nuevaVersion(
                        rrhh, new ParametroRequest("RMV", new BigDecimal("1300"), desde.plusDays(3), null)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() ->
                        parametros.nuevaVersion(rrhh, new ParametroRequest("INEXISTENTE", BigDecimal.ONE, desde, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(parametros.validar(finanzas, nueva.id()).porValidar()).isFalse();

        contratosBase();
        PlanillaResponse calculada = calcular();
        planillas.aprobar(finanzas, calculada.id());
        LocalDate antes = mesAnterior.atDay(1);
        assertThatThrownBy(() ->
                        parametros.nuevaVersion(rrhh, new ParametroRequest("UIT", new BigDecimal("5500"), antes, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void losConceptosRecurrentesEntranALaBoletaYLosDelSistemaNoSeAsignan() {
        contratosBase();
        var bono = conceptos.crear(
                rrhh,
                new ConceptoRequest(
                        "BONO_X",
                        "Bono especial",
                        TipoConcepto.INGRESO,
                        ModoConcepto.MONTO_FIJO,
                        new BigDecimal("100"),
                        true,
                        true,
                        true,
                        true));
        conceptos.asignar(
                rrhh,
                ana.getId(),
                new EmpleadoConceptoRequest(
                        bono.id(), new BigDecimal("250"), LocalDate.now().minusMonths(6), null));

        PlanillaResponse calculada = calcular();

        var boletaAna = planillas.boleta(
                rrhh,
                planillas.obtener(rrhh, calculada.id()).boletas().stream()
                        .filter(b -> b.empleadoId().equals(ana.getId()))
                        .findFirst()
                        .orElseThrow()
                        .id());
        assertThat(boletaAna.totalIngresos()).isEqualByComparingTo("3250.00");
        assertThat(boletaAna.lineas()).extracting(l -> l.codigo()).contains("BONO_X");
        assertThatThrownBy(() -> conceptos.crear(
                        rrhh,
                        new ConceptoRequest(
                                "BONO_X",
                                "Repetido",
                                TipoConcepto.INGRESO,
                                ModoConcepto.MONTO_FIJO,
                                BigDecimal.ONE,
                                false,
                                false,
                                false,
                                true)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> conceptos.crear(
                        rrhh,
                        new ConceptoRequest(
                                "APORTE",
                                "Aporte",
                                TipoConcepto.APORTE_EMPLEADOR,
                                ModoConcepto.MONTO_FIJO,
                                BigDecimal.ONE,
                                false,
                                false,
                                false,
                                true)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void elArchivoBancarioYElPlameRequierenPlanillaAprobada() {
        contratosBase();
        PlanillaResponse calculada = calcular();

        assertThatThrownBy(() -> planillas.archivoBancario(finanzas, calculada.id()))
                .isInstanceOf(ConflictException.class);
        planillas.aprobar(finanzas, calculada.id());

        var banco = planillas.archivoBancario(finanzas, calculada.id());
        assertThat(banco.filas()).isEqualTo(2);
        assertThat(banco.contenido()).startsWith("numero_boleta,empleado,banco,cuenta,monto,moneda,referencia");
        assertThat(banco.porValidar()).isFalse();
        var plame = planillas.plame(rrhh, calculada.id());
        assertThat(plame.porValidar()).isTrue();
        assertThat(plame.filas()).isEqualTo(2);
    }

    @Test
    void elContratoConPlanillasAprobadasNoCambiaSusCondiciones() {
        var creado = contratos.crear(rrhh, contrato(ana, "3000", SistemaPensionario.ONP));
        PlanillaResponse calculada = calcular();
        planillas.aprobar(finanzas, calculada.id());

        assertThatThrownBy(() -> contratos.actualizar(rrhh, creado.id(), contrato(ana, "4000", SistemaPensionario.ONP)))
                .isInstanceOf(ConflictException.class);
        var finalizado = contratos.finalizar(
                rrhh,
                creado.id(),
                new com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.FinalizarContratoRequest(
                        LocalDate.now()));
        assertThat(finalizado.estado().name()).isEqualTo("FINALIZADO");
    }

    private static MarcacionManualRequest manual(Empleado empleado, TipoMarcacion tipo, LocalDateTime fechaHora) {
        return new MarcacionManualRequest(empleado.getId(), tipo, fechaHora, "Registro de prueba");
    }
}

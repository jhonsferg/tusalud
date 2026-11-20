package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.IngresoItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ProductoRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.ChecklistRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.CicloRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.EsterilizadorRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.IndicadoresRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.InsumoCirugiaRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.MiembroEquipoRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.NotaOperatoriaRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.ProgramacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.QuirofanoRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.RegistroAnestesicoRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.SetDelCicloRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.SetInstrumentalRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.SolicitudCirugiaRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.UsoSetRequest;
import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.CicloResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.CirugiaDetalle;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.QuirofanoResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.SetInstrumentalResponse;
import com.codesoftlabs.tusalud.app.entities.Cie10Catalogo;
import com.codesoftlabs.tusalud.app.entities.Consentimiento;
import com.codesoftlabs.tusalud.app.entities.ConsentimientoPlantilla;
import com.codesoftlabs.tusalud.app.entities.LoteEsteril;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoConsentimiento;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoEfectivoLote;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoLoteEsteril;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoQuirofano;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.MomentoChecklist;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoCiclo;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoIndicador;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.RolQuirurgico;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoAnestesia;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoCirugia;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoEsterilizacion;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.Cie10CatalogoRepository;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoPlantillaRepository;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoRepository;
import com.codesoftlabs.tusalud.app.repositories.LoteEsterilRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AlmacenService;
import com.codesoftlabs.tusalud.app.services.interfaces.InventarioService;
import com.codesoftlabs.tusalud.app.services.interfaces.ProductoService;
import com.codesoftlabs.tusalud.app.services.quirofano.CirugiaService;
import com.codesoftlabs.tusalud.app.services.quirofano.EsterilizacionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class QuirofanoServiceTest {

    @Autowired
    private CirugiaService cirugias;

    @Autowired
    private EsterilizacionService esterilizacion;

    @Autowired
    private Cie10CatalogoRepository cie10;

    @Autowired
    private ConsentimientoPlantillaRepository plantillas;

    @Autowired
    private ConsentimientoRepository consentimientos;

    @Autowired
    private LoteEsterilRepository lotes;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private AlmacenService almacenService;

    @Autowired
    private InventarioService inventario;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico cirujano;
    private Paciente paciente;
    private UsuarioPrincipal pCirujano;
    private UsuarioPrincipal pAnestesiologo;
    private UsuarioPrincipal pInstrumentista;
    private UsuarioPrincipal pEnfermeria;
    private UsuarioPrincipal pAlmacen;
    private Usuario anestesiologo;
    private Usuario instrumentista;
    private QuirofanoResponse quirofano;
    private Cie10Catalogo diagnostico;
    private LocalDateTime inicio;

    @BeforeEach
    void setUp() {
        cirujano = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        fixture.cita(cirujano, paciente, LocalDateTime.now().plusDays(1), EstadoCita.CONFIRMADA);
        pCirujano = fixture.principal(cirujano.getUsuario());
        anestesiologo = fixture.medicoConHorarioLunes(new BigDecimal("90.00")).getUsuario();
        pAnestesiologo = fixture.principal(anestesiologo);
        instrumentista = fixture.usuario("qx-ins", RolNombre.ROLE_ENFERMERIA);
        pInstrumentista = fixture.principal(instrumentista);
        pEnfermeria = fixture.principal(fixture.usuario("qx-enf", RolNombre.ROLE_ENFERMERIA));
        pAlmacen = fixture.principal(fixture.usuario("qx-alm", RolNombre.ROLE_ALMACEN));
        diagnostico = cie10.save(Cie10Catalogo.builder()
                .codigo("K80." + ServiceTestFixture.siguiente() % 10)
                .descripcion("Colelitiasis")
                .textoBusqueda("colelitiasis")
                .activo(true)
                .muestra(true)
                .actualizadoEn(LocalDateTime.now())
                .build());
        plantillas.save(plantilla(CirugiaService.PLANTILLA_QUIRURGICA));
        plantillas.save(plantilla(CirugiaService.PLANTILLA_ANESTESICA));
        quirofano = cirugias.crearQuirofano(new QuirofanoRequest(
                "QX-" + ServiceTestFixture.siguiente(),
                "Quirófano 1",
                "Piso 2",
                EstadoQuirofano.DISPONIBLE,
                true,
                null,
                List.of()));
        inicio = LocalDateTime.now()
                .plusDays(3)
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    private ConsentimientoPlantilla plantilla(String codigo) {
        return ConsentimientoPlantilla.builder()
                .codigo(codigo)
                .nombre(codigo)
                .texto("t")
                .riesgos("r")
                .beneficios("b")
                .alternativas("a")
                .build();
    }

    private CirugiaDetalle solicitada() {
        return cirugias.solicitar(
                pCirujano,
                new SolicitudCirugiaRequest(
                        paciente.getId(),
                        diagnostico.getCodigo(),
                        "Colecistectomía laparoscópica",
                        TipoCirugia.ELECTIVA,
                        TipoAnestesia.GENERAL,
                        null,
                        "Ayuno de 8 horas"));
    }

    private List<MiembroEquipoRequest> equipo() {
        return List.of(
                new MiembroEquipoRequest(cirujano.getUsuario().getId(), RolQuirurgico.CIRUJANO),
                new MiembroEquipoRequest(anestesiologo.getId(), RolQuirurgico.ANESTESIOLOGO),
                new MiembroEquipoRequest(instrumentista.getId(), RolQuirurgico.INSTRUMENTISTA));
    }

    private CirugiaDetalle programada() {
        CirugiaDetalle c = solicitada();
        return cirugias.programar(
                pEnfermeria, c.resumen().id(), new ProgramacionRequest(quirofano.id(), inicio, 120, equipo()));
    }

    private void firmarConsentimientos(CirugiaDetalle c) {
        for (UUID id : List.of(
                c.consentimientoQuirurgico().id(), c.consentimientoAnestesico().id())) {
            Consentimiento consentimiento = consentimientos.findById(id).orElseThrow();
            consentimiento.setEstado(EstadoConsentimiento.FIRMADO);
            consentimiento.setFirmadoEn(LocalDateTime.now());
            consentimiento.setVenceEn(LocalDateTime.now().plusDays(30));
            consentimientos.saveAndFlush(consentimiento);
        }
    }

    private List<String> todos(MomentoChecklist momento) {
        return momento.items();
    }

    private LoteEsteril loteConforme() {
        SetInstrumentalResponse set = esterilizacion.crearSet(new SetInstrumentalRequest(
                "SET-" + ServiceTestFixture.siguiente(), "Set de laparoscopía", null, 30, true));
        var esterilizador = esterilizacion.crearEsterilizador(new EsterilizadorRequest(
                "AUT-" + ServiceTestFixture.siguiente(), "Autoclave 1", TipoEsterilizacion.VAPOR, true));
        CicloResponse ciclo = esterilizacion.crearCiclo(
                pEnfermeria,
                new CicloRequest(
                        esterilizador.id(),
                        LocalDateTime.now().minusHours(3),
                        new BigDecimal("134.0"),
                        new BigDecimal("210.0"),
                        30,
                        null,
                        List.of(new SetDelCicloRequest(set.id(), 1))));
        esterilizacion.registrarIndicadores(
                pEnfermeria,
                ciclo.id(),
                new IndicadoresRequest(ResultadoIndicador.CONFORME, ResultadoIndicador.CONFORME, null));
        return lotes.findById(ciclo.lotes().get(0).id()).orElseThrow();
    }

    @Test
    void noProgramaDosCirugiasEnElMismoQuirofanoYHorario() {
        programada();
        CirugiaDetalle otra = solicitada();

        assertThatThrownBy(() -> cirugias.programar(
                        pEnfermeria,
                        otra.resumen().id(),
                        new ProgramacionRequest(quirofano.id(), inicio.plusMinutes(60), 60, equipo())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void noAsignaAUnMiembroOcupadoEnOtroQuirofano() {
        programada();
        QuirofanoResponse segundo = cirugias.crearQuirofano(new QuirofanoRequest(
                "QX-" + ServiceTestFixture.siguiente(),
                "Quirófano 2",
                null,
                EstadoQuirofano.DISPONIBLE,
                true,
                null,
                List.of()));
        CirugiaDetalle otra = solicitada();

        assertThatThrownBy(() -> cirugias.programar(
                        pEnfermeria,
                        otra.resumen().id(),
                        new ProgramacionRequest(segundo.id(), inicio.plusMinutes(30), 60, equipo())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya está asignado");
    }

    @Test
    void validaLosRolesDelEquipo() {
        CirugiaDetalle c = solicitada();

        assertThatThrownBy(() -> cirugias.programar(
                        pEnfermeria,
                        c.resumen().id(),
                        new ProgramacionRequest(
                                quirofano.id(),
                                inicio,
                                60,
                                List.of(
                                        new MiembroEquipoRequest(instrumentista.getId(), RolQuirurgico.CIRUJANO),
                                        new MiembroEquipoRequest(anestesiologo.getId(), RolQuirurgico.ANESTESIOLOGO)))))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> cirugias.programar(
                        pEnfermeria,
                        c.resumen().id(),
                        new ProgramacionRequest(
                                quirofano.id(),
                                inicio,
                                60,
                                List.of(new MiembroEquipoRequest(
                                        cirujano.getUsuario().getId(), RolQuirurgico.CIRUJANO)))))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("anestesiólogo");
    }

    @Test
    void noIniciaSinConsentimientosNiListaDeVerificacion() {
        CirugiaDetalle c = programada();
        UUID id = c.resumen().id();
        cirugias.preparar(pEnfermeria, id);

        assertThatThrownBy(() -> cirugias.iniciar(pCirujano, id))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("consentimiento quirúrgico");

        CirugiaDetalle conConsentimientos = cirugias.solicitarConsentimientos(pCirujano, id);
        assertThatThrownBy(() -> cirugias.iniciar(pCirujano, id))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("firmado");

        firmarConsentimientos(conConsentimientos);
        assertThatThrownBy(() -> cirugias.iniciar(pCirujano, id))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("antes de la inducción");

        assertThatThrownBy(() -> cirugias.registrarChecklist(
                        pInstrumentista,
                        id,
                        new ChecklistRequest(MomentoChecklist.ANTES_INDUCCION, List.of("IDENTIDAD_CONFIRMADA"), null)))
                .isInstanceOf(DatosInvalidosException.class);

        cirugias.registrarChecklist(
                pInstrumentista,
                id,
                new ChecklistRequest(MomentoChecklist.ANTES_INDUCCION, todos(MomentoChecklist.ANTES_INDUCCION), null));
        CirugiaDetalle iniciada = cirugias.iniciar(pCirujano, id);
        assertThat(iniciada.resumen().estado()).isEqualTo(EstadoCirugia.EN_CURSO);
    }

    @Test
    void soloElCirujanoDelEquipoIniciaYLosAjenosNoRegistran() {
        CirugiaDetalle c = programada();
        UUID id = c.resumen().id();
        cirugias.preparar(pEnfermeria, id);

        assertThatThrownBy(() -> cirugias.iniciar(pAnestesiologo, id)).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> cirugias.registrarChecklist(
                        pEnfermeria,
                        id,
                        new ChecklistRequest(
                                MomentoChecklist.ANTES_INDUCCION, todos(MomentoChecklist.ANTES_INDUCCION), null)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(
                        () -> cirugias.registrarInsumo(pEnfermeria, id, new InsumoCirugiaRequest(UUID.randomUUID(), 1)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void completaElCicloQuirurgicoDescuentaInventarioYDejaTrazabilidad() {
        ProductoResponse gasa = productoService.crear(new ProductoRequest(
                "GAS-" + ServiceTestFixture.siguiente(),
                "Gasa estéril",
                null,
                TipoProducto.INSUMO,
                null,
                "Paquete",
                "unidad",
                new BigDecimal("2.00"),
                1,
                false,
                true));
        almacenService.registrarIngreso(
                pAlmacen,
                new IngresoRequest(
                        null,
                        "F001-9",
                        null,
                        List.of(new IngresoItemRequest(
                                gasa.id(),
                                "LQ-1",
                                LocalDate.now().plusDays(200),
                                20,
                                new BigDecimal("1.00"),
                                "Estante"))));
        LoteEsteril lote = loteConforme();

        CirugiaDetalle c = programada();
        UUID id = c.resumen().id();
        cirugias.preparar(pEnfermeria, id);
        firmarConsentimientos(cirugias.solicitarConsentimientos(pCirujano, id));
        cirugias.registrarChecklist(
                pInstrumentista,
                id,
                new ChecklistRequest(MomentoChecklist.ANTES_INDUCCION, todos(MomentoChecklist.ANTES_INDUCCION), null));
        cirugias.iniciar(pCirujano, id);

        cirugias.usarSet(pInstrumentista, id, new UsoSetRequest(lote.getId()));
        assertThat(lotes.findById(lote.getId()).orElseThrow().getEstado()).isEqualTo(EstadoLoteEsteril.EN_USO);
        assertThatThrownBy(() -> cirugias.usarSet(pInstrumentista, id, new UsoSetRequest(lote.getId())))
                .isInstanceOf(ConflictException.class);

        long antes = inventario.stockDe(List.of(gasa.id())).get(gasa.id()).stock();
        CirugiaDetalle conInsumo =
                cirugias.registrarInsumo(pInstrumentista, id, new InsumoCirugiaRequest(gasa.id(), 4));
        assertThat(inventario.stockDe(List.of(gasa.id())).get(gasa.id()).stock())
                .isEqualTo(antes - 4);
        assertThat(conInsumo.insumos()).hasSize(1);
        assertThatThrownBy(
                        () -> cirugias.registrarInsumo(pInstrumentista, id, new InsumoCirugiaRequest(gasa.id(), 500)))
                .isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> cirugias.finalizar(pCirujano, id))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("nota operatoria");

        cirugias.registrarChecklist(
                pInstrumentista,
                id,
                new ChecklistRequest(MomentoChecklist.ANTES_INCISION, todos(MomentoChecklist.ANTES_INCISION), null));
        cirugias.registrarChecklist(
                pInstrumentista,
                id,
                new ChecklistRequest(
                        MomentoChecklist.ANTES_SALIDA, todos(MomentoChecklist.ANTES_SALIDA), "Conteo correcto"));
        cirugias.notaOperatoria(
                pCirujano,
                id,
                new NotaOperatoriaRequest(
                        "Vesícula inflamada",
                        "Laparoscópica con tres puertos",
                        null,
                        30,
                        "Colecistitis",
                        "Dieta blanda"));
        assertThatThrownBy(() -> cirugias.finalizar(pCirujano, id))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("registro anestésico");
        cirugias.registroAnestesico(
                pAnestesiologo, id, new RegistroAnestesicoRequest(2, inicio, inicio.plusHours(2), "Sin incidentes"));

        CirugiaDetalle finalizada = cirugias.finalizar(pCirujano, id);

        assertThat(finalizada.resumen().estado()).isEqualTo(EstadoCirugia.FINALIZADA);
        assertThat(finalizada.finReal()).isNotNull();
        assertThat(finalizada.sets()).hasSize(1);
        assertThat(lotes.findById(lote.getId()).orElseThrow().getEstado()).isEqualTo(EstadoLoteEsteril.USADO);
        var trazabilidad = esterilizacion.trazabilidadLote(lote.getId());
        assertThat(trazabilidad.usos()).hasSize(1);
        assertThat(trazabilidad.usos().get(0).cirugiaCodigo())
                .isEqualTo(finalizada.resumen().codigo());
        assertThat(cirugias.historial(pCirujano, id))
                .extracting(e -> e.accion())
                .contains("INICIADA", "FINALIZADA");
        assertThatThrownBy(() -> cirugias.cancelar(pEnfermeria, id, "Error")).isInstanceOf(ConflictException.class);
    }

    @Test
    void suspenderLiberaElQuirofanoYPermiteReprogramar() {
        CirugiaDetalle c = programada();
        UUID id = c.resumen().id();

        CirugiaDetalle suspendida = cirugias.suspender(pEnfermeria, id, "Paciente con fiebre");
        assertThat(suspendida.resumen().estado()).isEqualTo(EstadoCirugia.SUSPENDIDA);
        assertThat(suspendida.motivo()).isEqualTo("Paciente con fiebre");

        CirugiaDetalle otra = solicitada();
        assertThat(cirugias.programar(
                                pEnfermeria,
                                otra.resumen().id(),
                                new ProgramacionRequest(quirofano.id(), inicio, 60, equipo()))
                        .resumen()
                        .estado())
                .isEqualTo(EstadoCirugia.PROGRAMADA);

        CirugiaDetalle reprogramada = cirugias.programar(
                pEnfermeria, id, new ProgramacionRequest(quirofano.id(), inicio.plusDays(1), 60, equipo()));
        assertThat(reprogramada.resumen().estado()).isEqualTo(EstadoCirugia.PROGRAMADA);
    }

    @Test
    void elMedicoSinRelacionNoVeElDetalleYLaAgendaOcultaAlPaciente() {
        CirugiaDetalle c = programada();
        UsuarioPrincipal ajeno = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("80.00")).getUsuario());

        assertThatThrownBy(() -> cirugias.detalle(ajeno, c.resumen().id()))
                .isInstanceOf(ForbiddenOperationException.class);
        var agenda = cirugias.agenda(ajeno, inicio.minusDays(1), inicio.plusDays(1), null, null);
        assertThat(agenda).hasSize(1);
        assertThat(agenda.get(0).detalleVisible()).isFalse();
        assertThat(agenda.get(0).pacienteNombre()).isNull();
        assertThat(agenda.get(0).procedimiento()).isNull();

        var propia = cirugias.agenda(pCirujano, inicio.minusDays(1), inicio.plusDays(1), null, null);
        assertThat(propia.get(0).pacienteNombre()).isNotBlank();
    }

    @Test
    void elPacienteVeSusCirugiasConSusConsentimientos() {
        CirugiaDetalle c = programada();
        cirugias.solicitarConsentimientos(pCirujano, c.resumen().id());

        var mias = cirugias.mias(fixture.principal(paciente.getUsuario()));

        assertThat(mias).hasSize(1);
        assertThat(mias.get(0).consentimientoQuirurgico()).isNotNull();
        assertThat(mias.get(0).consentimientoQuirurgico().estado()).isEqualTo(EstadoConsentimiento.PENDIENTE_FIRMA);
        assertThatThrownBy(() -> cirugias.mias(pCirujano)).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void unCicloNoConformeBloqueaSusLotesYUnoVencidoNoSePuedeUsar() {
        SetInstrumentalResponse set = esterilizacion.crearSet(
                new SetInstrumentalRequest("SET-" + ServiceTestFixture.siguiente(), "Set menor", null, 30, true));
        var autoclave = esterilizacion.crearEsterilizador(new EsterilizadorRequest(
                "AUT-" + ServiceTestFixture.siguiente(), "Autoclave 2", TipoEsterilizacion.VAPOR, true));
        CicloResponse ciclo = esterilizacion.crearCiclo(
                pEnfermeria,
                new CicloRequest(
                        autoclave.id(),
                        LocalDateTime.now().minusHours(2),
                        new BigDecimal("134.0"),
                        null,
                        20,
                        null,
                        List.of(new SetDelCicloRequest(set.id(), 2))));
        assertThat(ciclo.lotes()).hasSize(2);
        assertThat(ciclo.resultado()).isEqualTo(ResultadoCiclo.EN_PROCESO);
        assertThat(ciclo.lotes().get(0).estado()).isEqualTo(EstadoEfectivoLote.EN_ESPERA_DE_INDICADOR);
        assertThat(ciclo.lotes().get(0).utilizable()).isFalse();

        CicloResponse noConforme = esterilizacion.registrarIndicadores(
                pEnfermeria,
                ciclo.id(),
                new IndicadoresRequest(ResultadoIndicador.CONFORME, ResultadoIndicador.NO_CONFORME, "Esporas viables"));
        assertThat(noConforme.resultado()).isEqualTo(ResultadoCiclo.NO_CONFORME);
        assertThat(noConforme.lotes()).allSatisfy(l -> assertThat(l.estado()).isEqualTo(EstadoEfectivoLote.BLOQUEADO));
        assertThatThrownBy(() -> esterilizacion.registrarIndicadores(
                        pEnfermeria,
                        ciclo.id(),
                        new IndicadoresRequest(ResultadoIndicador.CONFORME, ResultadoIndicador.CONFORME, null)))
                .isInstanceOf(ConflictException.class);
        assertThat(esterilizacion.lotes(true)).noneMatch(l -> l.cicloId().equals(ciclo.id()));
        assertThat(esterilizacion.alertas()).anyMatch(a -> a.tipo().equals("LOTE_BLOQUEADO"));

        LoteEsteril conforme = loteConforme();
        conforme.setVenceEn(LocalDateTime.now().minusMinutes(1));
        lotes.saveAndFlush(conforme);
        CirugiaDetalle c = programada();
        UUID id = c.resumen().id();
        cirugias.preparar(pEnfermeria, id);
        assertThatThrownBy(() -> cirugias.usarSet(pInstrumentista, id, new UsoSetRequest(conforme.getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("venció");
        UUID bloqueado = noConforme.lotes().get(0).id();
        assertThatThrownBy(() -> cirugias.usarSet(pInstrumentista, id, new UsoSetRequest(bloqueado)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no conforme");
        assertThat(esterilizacion.trazabilidadCiclo(ciclo.id())).hasSize(2);
    }
}

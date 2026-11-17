package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CancelarOrdenLabRequest;
import com.codesoftlabs.tusalud.app.dto.request.ExamenLabRequest;
import com.codesoftlabs.tusalud.app.dto.request.OrdenLabCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ResultadosLabRequest;
import com.codesoftlabs.tusalud.app.dto.response.ExamenLabResponse;
import com.codesoftlabs.tusalud.app.dto.response.LabResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenLabResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PacienteCompartido;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaExamen;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoItemLab;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMuestra;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoParametro;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.PacienteCompartidoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.LaboratorioService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.math.BigDecimal;
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
class LaboratorioServiceTest {

    @Autowired
    private LaboratorioService laboratorioService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private PacienteCompartidoRepository compartidoRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal medicoPrincipal;
    private UsuarioPrincipal pacientePrincipal;
    private UsuarioPrincipal laboratorio;
    private UsuarioPrincipal admin;
    private ExamenLabResponse hemograma;
    private ExamenLabResponse glucosa;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        medicoPrincipal = fixture.principal(medico.getUsuario());
        pacientePrincipal = fixture.principal(paciente.getUsuario());
        laboratorio = fixture.principal(fixture.usuario("lab", RolNombre.ROLE_LABORATORIO));
        admin = fixture.principal(fixture.usuario("admin", RolNombre.ROLE_ADMIN));
        fixture.cita(medico, paciente, LocalDateTime.now().minusDays(2), EstadoCita.ATENDIDA);
        hemograma = laboratorioService.crearExamen(examen(
                "T-HEM" + ServiceTestFixture.siguiente(),
                "Hemograma de prueba",
                CategoriaExamen.HEMATOLOGIA,
                new BigDecimal("35.00"),
                List.of(
                        new ExamenLabRequest.ParametroRequest(
                                "Hemoglobina",
                                "g/dL",
                                TipoParametro.NUMERICO,
                                new BigDecimal("12"),
                                new BigDecimal("17.5"),
                                1),
                        new ExamenLabRequest.ParametroRequest(
                                "Observación", null, TipoParametro.TEXTO, null, null, 2))));
        glucosa = laboratorioService.crearExamen(examen(
                "T-GLU" + ServiceTestFixture.siguiente(),
                "Glucosa de prueba",
                CategoriaExamen.BIOQUIMICA,
                new BigDecimal("15.00"),
                List.of(new ExamenLabRequest.ParametroRequest(
                        "Glucosa", "mg/dL", TipoParametro.NUMERICO, new BigDecimal("70"), new BigDecimal("100"), 1))));
    }

    private ExamenLabRequest examen(
            String codigo,
            String nombre,
            CategoriaExamen categoria,
            BigDecimal precio,
            List<ExamenLabRequest.ParametroRequest> parametros) {
        return new ExamenLabRequest(
                codigo, nombre, categoria, TipoMuestra.SANGRE, precio, 4, "Ayuno", true, parametros);
    }

    private OrdenLabResponse solicitar(PrioridadOrden prioridad, ExamenLabResponse... examenes) {
        return laboratorioService.crearOrden(
                medicoPrincipal,
                new OrdenLabCreateRequest(
                        paciente.getId(),
                        null,
                        prioridad,
                        "Control",
                        java.util.Arrays.stream(examenes)
                                .map(ExamenLabResponse::id)
                                .toList()));
    }

    private ResultadosLabRequest resultados(ExamenLabResponse examen, String... valores) {
        List<ResultadosLabRequest.ResultadoValor> lista = new java.util.ArrayList<>();
        for (int i = 0; i < valores.length; i++) {
            lista.add(new ResultadosLabRequest.ResultadoValor(
                    examen.parametros().get(i).id(), valores[i]));
        }
        return new ResultadosLabRequest(lista, null);
    }

    private OrdenLabResponse completar(OrdenLabResponse orden, String hemoglobina, String glu) {
        laboratorioService.tomarMuestra(laboratorio, orden.id());
        laboratorioService.registrarResultados(
                laboratorio, orden.id(), orden.items().get(0).id(), resultados(hemograma, hemoglobina));
        laboratorioService.registrarResultados(
                laboratorio, orden.id(), orden.items().get(1).id(), resultados(glucosa, glu));
        return laboratorioService.validar(laboratorio, orden.id());
    }

    @Test
    void creaLaOrdenConCodigoTotalYNotificaAlPacienteYAlLaboratorio() {
        OrdenLabResponse orden = solicitar(PrioridadOrden.URGENTE, hemograma, glucosa);

        assertThat(orden.codigo()).matches("LAB-[0-9]{6}");
        assertThat(orden.estado()).isEqualTo(EstadoOrdenLab.SOLICITADA);
        assertThat(orden.total()).isEqualByComparingTo("50.00");
        assertThat(orden.items()).hasSize(2).allSatisfy(item -> {
            assertThat(item.estado()).isEqualTo(EstadoItemLab.PENDIENTE);
            assertThat(item.resultados()).isEmpty();
        });
        assertThat(orden.medicoNombre()).isEqualTo(medico.getUsuario().getNombreCompleto());

        List<NotificacionResponse> delPaciente = notificacionService.listar(pacientePrincipal, false, 10);
        assertThat(delPaciente).anySatisfy(n -> {
            assertThat(n.tipo()).isEqualTo(TipoNotificacion.ORDEN_LABORATORIO);
            assertThat(n.enlace()).isEqualTo("/mis-resultados");
        });
        List<NotificacionResponse> delLaboratorio = notificacionService.listar(laboratorio, false, 10);
        assertThat(delLaboratorio).anySatisfy(n -> {
            assertThat(n.tipo()).isEqualTo(TipoNotificacion.ORDEN_LABORATORIO);
            assertThat(n.enlace()).isEqualTo("/laboratorio/ordenes/" + orden.id());
        });
    }

    @Test
    void ignoraExamenesRepetidosAlCalcularElTotal() {
        OrdenLabResponse orden = solicitar(PrioridadOrden.NORMAL, hemograma, hemograma);
        assertThat(orden.items()).hasSize(1);
        assertThat(orden.total()).isEqualByComparingTo("35.00");
    }

    @Test
    void soloSolicitaExamenesElMedicoQueAtiendeOTieneCompartidoAlPaciente() {
        UsuarioPrincipal ajeno = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("90.00")).getUsuario());

        assertThatThrownBy(() -> laboratorioService.crearOrden(
                        ajeno,
                        new OrdenLabCreateRequest(
                                paciente.getId(), null, PrioridadOrden.NORMAL, null, List.of(hemograma.id()))))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void rechazaExamenesInactivosInexistentesYAtencionesAjenas() {
        laboratorioService.actualizarExamen(
                glucosa.id(),
                new ExamenLabRequest(
                        glucosa.codigo(),
                        glucosa.nombre(),
                        glucosa.categoria(),
                        glucosa.muestra(),
                        glucosa.precio(),
                        glucosa.tiempoEntregaHoras(),
                        null,
                        false,
                        List.of()));

        assertThatThrownBy(() -> solicitar(PrioridadOrden.NORMAL, glucosa)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> laboratorioService.crearOrden(
                        medicoPrincipal,
                        new OrdenLabCreateRequest(
                                paciente.getId(), null, PrioridadOrden.NORMAL, null, List.of(UUID.randomUUID()))))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> laboratorioService.crearOrden(
                        medicoPrincipal,
                        new OrdenLabCreateRequest(
                                UUID.randomUUID(), null, PrioridadOrden.NORMAL, null, List.of(hemograma.id()))))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> laboratorioService.crearOrden(
                        medicoPrincipal,
                        new OrdenLabCreateRequest(
                                paciente.getId(),
                                UUID.randomUUID(),
                                PrioridadOrden.NORMAL,
                                null,
                                List.of(hemograma.id()))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recorreElFlujoCompletoYMarcaLosValoresFueraDeRango() {
        OrdenLabResponse orden = solicitar(PrioridadOrden.NORMAL, hemograma, glucosa);

        OrdenLabResponse conMuestra = laboratorioService.tomarMuestra(laboratorio, orden.id());
        assertThat(conMuestra.estado()).isEqualTo(EstadoOrdenLab.MUESTRA_TOMADA);
        assertThat(conMuestra.tomaMuestraEn()).isNotNull();

        OrdenLabResponse parcial = laboratorioService.registrarResultados(
                laboratorio, orden.id(), orden.items().get(0).id(), resultados(hemograma, "10.5"));
        assertThat(parcial.estado()).isEqualTo(EstadoOrdenLab.EN_PROCESO);
        assertThat(parcial.items().get(0).estado()).isEqualTo(EstadoItemLab.CON_RESULTADO);
        assertThat(parcial.items().get(0).resultados()).singleElement().satisfies(r -> {
            assertThat(r.parametro()).isEqualTo("Hemoglobina");
            assertThat(r.valor()).isEqualTo("10.5");
            assertThat(r.fueraDeRango()).isTrue();
            assertThat(r.valorMin()).isEqualByComparingTo("12");
            assertThat(r.valorMax()).isEqualByComparingTo("17.5");
        });

        assertThatThrownBy(() -> laboratorioService.validar(laboratorio, orden.id()))
                .isInstanceOf(ConflictException.class);

        laboratorioService.registrarResultados(
                laboratorio, orden.id(), orden.items().get(1).id(), resultados(glucosa, "88,5"));
        OrdenLabResponse completa = laboratorioService.validar(laboratorio, orden.id());

        assertThat(completa.estado()).isEqualTo(EstadoOrdenLab.COMPLETADA);
        assertThat(completa.completadaEn()).isNotNull();
        assertThat(completa.items()).allSatisfy(item -> {
            assertThat(item.estado()).isEqualTo(EstadoItemLab.VALIDADO);
            assertThat(item.validadoPor()).isNotBlank();
        });
        assertThat(completa.items().get(1).resultados().get(0).fueraDeRango()).isFalse();

        assertThat(notificacionService.listar(pacientePrincipal, false, 10))
                .anySatisfy(n -> assertThat(n.tipo()).isEqualTo(TipoNotificacion.RESULTADO_LABORATORIO));
        assertThat(notificacionService.listar(medicoPrincipal, false, 10))
                .anySatisfy(n -> assertThat(n.tipo()).isEqualTo(TipoNotificacion.RESULTADO_LABORATORIO));
    }

    @Test
    void detectaLosLimitesExactosComoDentroDelRango() {
        OrdenLabResponse orden = solicitar(PrioridadOrden.NORMAL, hemograma);
        laboratorioService.tomarMuestra(laboratorio, orden.id());

        OrdenLabResponse minimo = laboratorioService.registrarResultados(
                laboratorio, orden.id(), orden.items().get(0).id(), resultados(hemograma, "12"));
        assertThat(minimo.items().get(0).resultados().get(0).fueraDeRango()).isFalse();

        OrdenLabResponse alto = laboratorioService.registrarResultados(
                laboratorio, orden.id(), orden.items().get(0).id(), resultados(hemograma, "17.6"));
        assertThat(alto.items().get(0).resultados().get(0).fueraDeRango()).isTrue();
        assertThat(alto.items().get(0).resultados()).hasSize(1);
    }

    @Test
    void rechazaTransicionesInvalidas() {
        OrdenLabResponse orden = solicitar(PrioridadOrden.NORMAL, hemograma);

        assertThatThrownBy(() -> laboratorioService.registrarResultados(
                        laboratorio, orden.id(), orden.items().get(0).id(), resultados(hemograma, "13")))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> laboratorioService.validar(laboratorio, orden.id()))
                .isInstanceOf(ConflictException.class);

        laboratorioService.tomarMuestra(laboratorio, orden.id());
        assertThatThrownBy(() -> laboratorioService.tomarMuestra(laboratorio, orden.id()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> laboratorioService.validar(laboratorio, orden.id()))
                .isInstanceOf(ConflictException.class);

        laboratorioService.registrarResultados(
                laboratorio, orden.id(), orden.items().get(0).id(), resultados(hemograma, "13"));
        laboratorioService.validar(laboratorio, orden.id());

        assertThatThrownBy(() -> laboratorioService.validar(laboratorio, orden.id()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> laboratorioService.registrarResultados(
                        laboratorio, orden.id(), orden.items().get(0).id(), resultados(hemograma, "14")))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> laboratorioService.cancelar(laboratorio, orden.id(), null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void validaLosParametrosYLosValoresNumericos() {
        OrdenLabResponse orden = solicitar(PrioridadOrden.NORMAL, hemograma, glucosa);
        laboratorioService.tomarMuestra(laboratorio, orden.id());
        UUID itemHemograma = orden.items().get(0).id();

        assertThatThrownBy(() -> laboratorioService.registrarResultados(
                        laboratorio, orden.id(), itemHemograma, resultados(hemograma, "abc")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> laboratorioService.registrarResultados(
                        laboratorio,
                        orden.id(),
                        itemHemograma,
                        new ResultadosLabRequest(
                                List.of(new ResultadosLabRequest.ResultadoValor(
                                        glucosa.parametros().get(0).id(), "90")),
                                null)))
                .isInstanceOf(DatosInvalidosException.class);
        UUID hemoglobina = hemograma.parametros().get(0).id();
        assertThatThrownBy(() -> laboratorioService.registrarResultados(
                        laboratorio,
                        orden.id(),
                        itemHemograma,
                        new ResultadosLabRequest(
                                List.of(
                                        new ResultadosLabRequest.ResultadoValor(hemoglobina, "13"),
                                        new ResultadosLabRequest.ResultadoValor(hemoglobina, "14")),
                                null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> laboratorioService.registrarResultados(
                        laboratorio, orden.id(), UUID.randomUUID(), resultados(hemograma, "13")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void elPacienteSoloVeResultadosCuandoLaOrdenEstaCompletada() {
        OrdenLabResponse orden = solicitar(PrioridadOrden.NORMAL, hemograma, glucosa);
        laboratorioService.tomarMuestra(laboratorio, orden.id());

        assertThatThrownBy(() -> laboratorioService.obtenerOrden(pacientePrincipal, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        List<OrdenLabResponse> pendientes = laboratorioService.misOrdenes(pacientePrincipal);
        assertThat(pendientes).singleElement().satisfies(o -> {
            assertThat(o.estado()).isEqualTo(EstadoOrdenLab.MUESTRA_TOMADA);
            assertThat(o.items())
                    .allSatisfy(item -> assertThat(item.resultados()).isEmpty());
        });

        laboratorioService.registrarResultados(
                laboratorio, orden.id(), orden.items().get(0).id(), resultados(hemograma, "10"));
        laboratorioService.registrarResultados(
                laboratorio, orden.id(), orden.items().get(1).id(), resultados(glucosa, "90"));
        assertThat(laboratorioService
                        .misOrdenes(pacientePrincipal)
                        .get(0)
                        .items()
                        .get(0)
                        .resultados())
                .isEmpty();
        laboratorioService.validar(laboratorio, orden.id());

        OrdenLabResponse visible = laboratorioService.obtenerOrden(pacientePrincipal, orden.id());
        assertThat(visible.items().get(0).resultados()).hasSize(1);
        assertThat(laboratorioService
                        .misOrdenes(pacientePrincipal)
                        .get(0)
                        .items()
                        .get(0)
                        .resultados())
                .hasSize(1);

        UsuarioPrincipal otroPaciente = fixture.principal(fixture.paciente().getUsuario());
        assertThatThrownBy(() -> laboratorioService.obtenerOrden(otroPaciente, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(laboratorioService.misOrdenes(otroPaciente)).isEmpty();
    }

    @Test
    void elAccesoAUnaOrdenDependeDelRol() {
        OrdenLabResponse orden = solicitar(PrioridadOrden.NORMAL, hemograma);
        UsuarioPrincipal ajeno = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("90.00")).getUsuario());

        assertThat(laboratorioService.obtenerOrden(laboratorio, orden.id()).id())
                .isEqualTo(orden.id());
        assertThatThrownBy(() -> laboratorioService.obtenerOrden(admin, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(laboratorioService.obtenerOrden(medicoPrincipal, orden.id()).id())
                .isEqualTo(orden.id());
        assertThatThrownBy(() -> laboratorioService.obtenerOrden(ajeno, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> laboratorioService.obtenerOrden(laboratorio, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void elMedicoSoloCancelaMientrasLaOrdenEstaSolicitada() {
        OrdenLabResponse primera = solicitar(PrioridadOrden.NORMAL, hemograma);
        OrdenLabResponse cancelada =
                laboratorioService.cancelar(medicoPrincipal, primera.id(), new CancelarOrdenLabRequest("Error"));
        assertThat(cancelada.estado()).isEqualTo(EstadoOrdenLab.CANCELADA);

        OrdenLabResponse segunda = solicitar(PrioridadOrden.NORMAL, hemograma);
        laboratorioService.tomarMuestra(laboratorio, segunda.id());
        assertThatThrownBy(() -> laboratorioService.cancelar(medicoPrincipal, segunda.id(), null))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(laboratorioService.cancelar(laboratorio, segunda.id(), null).estado())
                .isEqualTo(EstadoOrdenLab.CANCELADA);

        OrdenLabResponse tercera = solicitar(PrioridadOrden.NORMAL, hemograma);
        UsuarioPrincipal ajeno = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("90.00")).getUsuario());
        assertThatThrownBy(() -> laboratorioService.cancelar(ajeno, tercera.id(), null))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(laboratorioService.cancelar(admin, tercera.id(), null).estado())
                .isEqualTo(EstadoOrdenLab.CANCELADA);
        assertThatThrownBy(() -> laboratorioService.tomarMuestra(laboratorio, tercera.id()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void listaLasOrdenesSegunElRolConFiltrosYUrgentesPrimero() {
        OrdenLabResponse normal = solicitar(PrioridadOrden.NORMAL, hemograma);
        OrdenLabResponse urgente = solicitar(PrioridadOrden.URGENTE, glucosa);
        OrdenLabResponse terminada = solicitar(PrioridadOrden.NORMAL, hemograma, glucosa);
        completar(terminada, "13", "90");

        List<OrdenLabResponse> deLaboratorio = laboratorioService.listarOrdenes(laboratorio, null, null, null);
        List<UUID> ids = deLaboratorio.stream().map(OrdenLabResponse::id).toList();
        assertThat(ids).containsSubsequence(urgente.id(), normal.id(), terminada.id());

        assertThat(laboratorioService.listarOrdenes(laboratorio, EstadoOrdenLab.COMPLETADA, null, null))
                .extracting(OrdenLabResponse::id)
                .contains(terminada.id())
                .doesNotContain(normal.id(), urgente.id());
        assertThat(laboratorioService.listarOrdenes(laboratorio, null, null, PrioridadOrden.URGENTE))
                .extracting(OrdenLabResponse::id)
                .contains(urgente.id())
                .doesNotContain(normal.id());
        assertThat(laboratorioService.listarOrdenes(laboratorio, null, paciente.getDni(), null))
                .extracting(OrdenLabResponse::id)
                .contains(normal.id(), urgente.id(), terminada.id());
        assertThat(laboratorioService.listarOrdenes(admin, null, normal.codigo().toLowerCase(), null))
                .extracting(OrdenLabResponse::id)
                .containsExactly(normal.id());

        UsuarioPrincipal otroMedico = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("90.00")).getUsuario());
        assertThat(laboratorioService.listarOrdenes(otroMedico, null, null, null))
                .isEmpty();
        assertThat(laboratorioService.listarOrdenes(medicoPrincipal, null, null, null))
                .extracting(OrdenLabResponse::id)
                .contains(normal.id(), urgente.id(), terminada.id());
    }

    @Test
    void gestionaElCatalogoDeExamenes() {
        ExamenLabResponse creado = laboratorioService.crearExamen(examen(
                " t-cat" + ServiceTestFixture.siguiente() + " ",
                "Catálogo de prueba",
                CategoriaExamen.ORINA,
                new BigDecimal("20.00"),
                List.of(
                        new ExamenLabRequest.ParametroRequest("Color", null, TipoParametro.TEXTO, null, null, null),
                        new ExamenLabRequest.ParametroRequest(
                                "pH", null, TipoParametro.NUMERICO, new BigDecimal("5"), new BigDecimal("8"), null))));

        assertThat(creado.codigo()).isEqualTo(creado.codigo().toUpperCase()).doesNotContain(" ");
        assertThat(creado.parametros())
                .extracting(ExamenLabResponse.ParametroResponse::orden)
                .containsExactly(1, 2);
        assertThat(creado.parametros().get(0).valorMin()).isNull();

        assertThatThrownBy(() -> laboratorioService.crearExamen(examen(
                        creado.codigo().toLowerCase(), "Duplicado", CategoriaExamen.OTROS, BigDecimal.ONE, List.of())))
                .isInstanceOf(ConflictException.class);

        ExamenLabResponse editado = laboratorioService.actualizarExamen(
                creado.id(),
                examen(
                        creado.codigo(),
                        "Catálogo editado",
                        CategoriaExamen.ORINA,
                        new BigDecimal("25.00"),
                        List.of(new ExamenLabRequest.ParametroRequest(
                                "Densidad",
                                null,
                                TipoParametro.NUMERICO,
                                new BigDecimal("1.005"),
                                new BigDecimal("1.030"),
                                1))));
        assertThat(editado.nombre()).isEqualTo("Catálogo editado");
        assertThat(editado.parametros())
                .singleElement()
                .satisfies(p -> assertThat(p.nombre()).isEqualTo("Densidad"));

        assertThatThrownBy(() -> laboratorioService.actualizarExamen(
                        creado.id(),
                        examen(hemograma.codigo(), "Choca con otro", CategoriaExamen.ORINA, BigDecimal.ONE, List.of())))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> laboratorioService.actualizarExamen(
                        creado.id(),
                        examen(
                                creado.codigo(),
                                "Rango invertido",
                                CategoriaExamen.ORINA,
                                BigDecimal.ONE,
                                List.of(new ExamenLabRequest.ParametroRequest(
                                        "pH",
                                        null,
                                        TipoParametro.NUMERICO,
                                        new BigDecimal("9"),
                                        new BigDecimal("5"),
                                        1)))))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> laboratorioService.obtenerExamen(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(laboratorioService.listarExamenes("catálogo editado", null, null))
                .extracting(ExamenLabResponse::id)
                .containsExactly(creado.id());
        assertThat(laboratorioService.listarExamenes(null, CategoriaExamen.ORINA, true))
                .extracting(ExamenLabResponse::id)
                .contains(creado.id())
                .doesNotContain(hemograma.id());
        assertThat(laboratorioService.listarExamenes(null, null, false))
                .extracting(ExamenLabResponse::id)
                .doesNotContain(creado.id());
    }

    @Test
    void losResultadosConservanElSnapshotAunqueSeEditeElExamen() {
        OrdenLabResponse orden = solicitar(PrioridadOrden.NORMAL, hemograma);
        laboratorioService.tomarMuestra(laboratorio, orden.id());
        laboratorioService.registrarResultados(
                laboratorio, orden.id(), orden.items().get(0).id(), resultados(hemograma, "13"));

        laboratorioService.actualizarExamen(
                hemograma.id(),
                examen(
                        hemograma.codigo(),
                        "Hemograma renovado",
                        CategoriaExamen.HEMATOLOGIA,
                        new BigDecimal("40.00"),
                        List.of(new ExamenLabRequest.ParametroRequest(
                                "Plaquetas",
                                "x10³/µL",
                                TipoParametro.NUMERICO,
                                new BigDecimal("150"),
                                new BigDecimal("450"),
                                1))));

        OrdenLabResponse recargada = laboratorioService.obtenerOrden(laboratorio, orden.id());
        assertThat(recargada.items().get(0).resultados()).singleElement().satisfies(r -> {
            assertThat(r.parametro()).isEqualTo("Hemoglobina");
            assertThat(r.valorMax()).isEqualByComparingTo("17.5");
        });
    }

    @Test
    void resumeLaColaDelLaboratorio() {
        LabResumenResponse antes = laboratorioService.resumen();
        solicitar(PrioridadOrden.URGENTE, hemograma);
        OrdenLabResponse enMuestra = solicitar(PrioridadOrden.NORMAL, hemograma);
        laboratorioService.tomarMuestra(laboratorio, enMuestra.id());
        OrdenLabResponse terminada = solicitar(PrioridadOrden.NORMAL, hemograma, glucosa);
        completar(terminada, "13", "90");

        LabResumenResponse despues = laboratorioService.resumen();
        assertThat(despues.pendientes() - antes.pendientes()).isEqualTo(1);
        assertThat(despues.enProceso() - antes.enProceso()).isEqualTo(1);
        assertThat(despues.completadasHoy() - antes.completadasHoy()).isEqualTo(1);
        assertThat(despues.urgentes() - antes.urgentes()).isEqualTo(1);
    }

    @Test
    void ordenaAlMedicoConElQueSeCompartioElPaciente() {
        Medico colega = fixture.medicoConHorarioLunes(new BigDecimal("90.00"));
        UsuarioPrincipal colegaPrincipal = fixture.principal(colega.getUsuario());
        OrdenLabCreateRequest solicitud =
                new OrdenLabCreateRequest(paciente.getId(), null, PrioridadOrden.NORMAL, null, List.of(hemograma.id()));
        assertThatThrownBy(() -> laboratorioService.crearOrden(colegaPrincipal, solicitud))
                .isInstanceOf(ForbiddenOperationException.class);

        compartidoRepository.save(PacienteCompartido.builder()
                .paciente(paciente)
                .medicoOrigen(medico)
                .medicoDestino(colega)
                .nota("Interconsulta")
                .build());

        OrdenLabResponse orden = laboratorioService.crearOrden(colegaPrincipal, solicitud);
        assertThat(orden.medicoId()).isEqualTo(colega.getId());
    }

    @Test
    void laAdministracionVeLasOrdenesSinResultadosIndicacionesNiDni() {
        OrdenLabResponse orden = solicitar(PrioridadOrden.NORMAL, hemograma);

        OrdenLabResponse vista = laboratorioService
                .listarOrdenes(admin, null, orden.codigo(), null)
                .get(0);

        assertThat(vista.id()).isEqualTo(orden.id());
        assertThat(vista.pacienteNombre()).isNotBlank();
        assertThat(vista.dni()).isNull();
        assertThat(vista.indicaciones()).isNull();
        assertThat(vista.atencionId()).isNull();
        assertThat(vista.items())
                .allSatisfy(item -> assertThat(item.resultados()).isEmpty());
        assertThat(laboratorioService
                        .listarOrdenes(laboratorio, null, orden.codigo(), null)
                        .get(0)
                        .dni())
                .isNotNull();
    }
}

package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.CambioPiezaRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.ItemPlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.PagoPlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.PlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.ProcedimientoRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.SesionRequest;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.OdontogramaResponse;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.PiezaOdontograma;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.PlanResponse;
import com.codesoftlabs.tusalud.app.entities.Consentimiento;
import com.codesoftlabs.tusalud.app.entities.ConsentimientoPlantilla;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.OdontogramaEvento;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.ProcedimientoOdontologico;
import com.codesoftlabs.tusalud.app.entities.SesionCaja;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoConsentimiento;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoItemPlan;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoPieza;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoPlan;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.Superficie;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoPlantillaRepository;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoRepository;
import com.codesoftlabs.tusalud.app.repositories.ProcedimientoOdontologicoRepository;
import com.codesoftlabs.tusalud.app.repositories.SesionCajaRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.odontologia.Dentadura;
import com.codesoftlabs.tusalud.app.services.odontologia.OdontogramaService;
import com.codesoftlabs.tusalud.app.services.odontologia.PlanesOdontologicosService;
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
class OdontologiaServiceTest {

    @Autowired
    private OdontogramaService odontograma;

    @Autowired
    private PlanesOdontologicosService planes;

    @Autowired
    private ProcedimientoOdontologicoRepository procedimientos;

    @Autowired
    private ConsentimientoPlantillaRepository plantillas;

    @Autowired
    private ConsentimientoRepository consentimientos;

    @Autowired
    private SesionCajaRepository sesionesCaja;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal principalMedico;
    private UsuarioPrincipal principalPaciente;
    private ProcedimientoOdontologico obturacion;
    private ProcedimientoOdontologico exodoncia;
    private ProcedimientoOdontologico profilaxis;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        fixture.cita(medico, paciente, LocalDateTime.now().plusDays(1), EstadoCita.CONFIRMADA);
        principalMedico = fixture.principal(medico.getUsuario());
        principalPaciente = fixture.principal(paciente.getUsuario());
        obturacion = procedimientos.save(
                procedimiento("OBT-T", "Obturación", "120.00", true, true, EstadoPieza.OBTURADO, false));
        exodoncia = procedimientos.save(
                procedimiento("EXO-T", "Extracción", "110.00", true, false, EstadoPieza.AUSENTE, true));
        profilaxis = procedimientos.save(procedimiento("PRO-T", "Profilaxis", "90.00", false, false, null, false));
    }

    private static ProcedimientoOdontologico procedimiento(
            String codigo,
            String nombre,
            String precio,
            boolean pieza,
            boolean superficie,
            EstadoPieza resultante,
            boolean consentimiento) {
        return ProcedimientoOdontologico.builder()
                .codigo(codigo)
                .nombre(nombre)
                .categoria("Prueba")
                .precio(new BigDecimal(precio))
                .requierePieza(pieza)
                .requiereSuperficie(superficie)
                .estadoResultante(resultante)
                .requiereConsentimiento(consentimiento)
                .build();
    }

    private Consentimiento consentimientoFirmado() {
        ConsentimientoPlantilla plantilla = plantillas.save(ConsentimientoPlantilla.builder()
                .codigo("DENT-" + ServiceTestFixture.siguiente())
                .nombre("Exodoncia")
                .texto("t")
                .riesgos("r")
                .beneficios("b")
                .alternativas("a")
                .build());
        return consentimientos.save(Consentimiento.builder()
                .plantilla(plantilla)
                .plantillaVersion(1)
                .paciente(paciente)
                .medico(medico)
                .procedimiento("Extracción dental")
                .estado(EstadoConsentimiento.FIRMADO)
                .textoAceptado("texto")
                .firmadoEn(LocalDateTime.now())
                .venceEn(LocalDateTime.now().plusDays(30))
                .build());
    }

    private PlanResponse planConTratamientosAceptado() {
        PlanResponse plan = planes.crear(principalMedico, new PlanRequest(paciente.getId(), "Rehabilitación"));
        planes.agregarItem(
                principalMedico, plan.id(), new ItemPlanRequest(obturacion.getId(), 16, Superficie.O, null, null));
        planes.agregarItem(principalMedico, plan.id(), new ItemPlanRequest(exodoncia.getId(), 18, null, null, null));
        planes.presentar(principalMedico, plan.id());
        return planes.responder(principalPaciente, plan.id(), true, null);
    }

    @Test
    void validaLaNumeracionFdiDeLasDentaduras() {
        assertThat(Dentadura.valida(11)).isTrue();
        assertThat(Dentadura.valida(48)).isTrue();
        assertThat(Dentadura.valida(55)).isTrue();
        assertThat(Dentadura.valida(85)).isTrue();
        assertThat(Dentadura.valida(19)).isFalse();
        assertThat(Dentadura.valida(56)).isFalse();
        assertThat(Dentadura.valida(90)).isFalse();
        assertThat(Dentadura.tipo(26)).isEqualTo(Dentadura.PERMANENTE);
        assertThat(Dentadura.tipo(74)).isEqualTo(Dentadura.TEMPORAL);
    }

    @Test
    void elHistorialDelOdontogramaEsInmutableYElEstadoActualSaleDelUltimoCambio() {
        odontograma.registrarCambio(
                principalMedico,
                paciente.getId(),
                new CambioPiezaRequest(16, Superficie.O, EstadoPieza.CARIES, "Mancha"));
        odontograma.registrarCambio(
                principalMedico,
                paciente.getId(),
                new CambioPiezaRequest(16, Superficie.O, EstadoPieza.OBTURADO, null));
        odontograma.registrarCambio(
                principalMedico, paciente.getId(), new CambioPiezaRequest(24, null, EstadoPieza.CORONA, null));

        OdontogramaResponse actual = odontograma.obtener(principalMedico, paciente.getId());

        PiezaOdontograma p16 = pieza(actual, 16);
        assertThat(p16.superficies()).containsEntry(Superficie.O, EstadoPieza.OBTURADO);
        assertThat(p16.estado()).isNull();
        assertThat(pieza(actual, 24).estado()).isEqualTo(EstadoPieza.CORONA);
        assertThat(pieza(actual, 24).dentadura()).isEqualTo(Dentadura.PERMANENTE);
        assertThat(odontograma.historial(principalMedico, paciente.getId(), 16)).hasSize(2);
        assertThat(odontograma.historial(principalMedico, paciente.getId(), null))
                .hasSize(3);
        assertThat(OdontogramaEvento.class.isAnnotationPresent(org.hibernate.annotations.Immutable.class))
                .isTrue();
    }

    @Test
    void unEstadoDePiezaCompletaDejaSinEfectoLasSuperficiesAnteriores() {
        odontograma.registrarCambio(
                principalMedico, paciente.getId(), new CambioPiezaRequest(36, Superficie.M, EstadoPieza.CARIES, null));
        odontograma.registrarCambio(
                principalMedico, paciente.getId(), new CambioPiezaRequest(36, null, EstadoPieza.AUSENTE, "Extraída"));

        PiezaOdontograma p36 = pieza(odontograma.obtener(principalMedico, paciente.getId()), 36);

        assertThat(p36.estado()).isEqualTo(EstadoPieza.AUSENTE);
        assertThat(p36.superficies()).isEmpty();
    }

    @Test
    void rechazaPiezasInexistentesYEstadosDePiezaCompletaPorSuperficie() {
        assertThatThrownBy(() -> odontograma.registrarCambio(
                        principalMedico, paciente.getId(), new CambioPiezaRequest(19, null, EstadoPieza.CARIES, null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> odontograma.registrarCambio(
                        principalMedico,
                        paciente.getId(),
                        new CambioPiezaRequest(16, Superficie.V, EstadoPieza.AUSENTE, null)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void soloElMedicoConRelacionEscribeYElPacienteSoloLeeLoSuyo() {
        UsuarioPrincipal ajeno = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("70.00")).getUsuario());
        UsuarioPrincipal enfermeria = fixture.principal(fixture.usuario("enf", RolNombre.ROLE_ENFERMERIA));
        Paciente otro = fixture.paciente();

        assertThatThrownBy(() -> odontograma.obtener(ajeno, paciente.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> odontograma.obtener(enfermeria, paciente.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> odontograma.registrarCambio(
                        principalPaciente, paciente.getId(), new CambioPiezaRequest(16, null, EstadoPieza.SANO, null)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(odontograma.obtener(principalPaciente, paciente.getId())).isNotNull();
        assertThat(odontograma.mio(principalPaciente).pacienteId()).isEqualTo(paciente.getId());
        assertThatThrownBy(() -> odontograma.obtener(principalPaciente, otro.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void elPlanExigeLosDatosDelProcedimientoYSumaElPresupuesto() {
        PlanResponse plan = planes.crear(principalMedico, new PlanRequest(paciente.getId(), null));

        assertThatThrownBy(() -> planes.agregarItem(
                        principalMedico, plan.id(), new ItemPlanRequest(obturacion.getId(), null, null, null, null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> planes.agregarItem(
                        principalMedico, plan.id(), new ItemPlanRequest(obturacion.getId(), 16, null, null, null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> planes.agregarItem(
                        principalMedico, plan.id(), new ItemPlanRequest(profilaxis.getId(), 16, null, null, null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> planes.presentar(principalMedico, plan.id()))
                .isInstanceOf(DatosInvalidosException.class);

        planes.agregarItem(
                principalMedico, plan.id(), new ItemPlanRequest(obturacion.getId(), 16, Superficie.O, null, null));
        PlanResponse conDos = planes.agregarItem(
                principalMedico, plan.id(), new ItemPlanRequest(profilaxis.getId(), null, null, null, null));

        assertThat(conDos.items()).hasSize(2);
        assertThat(conDos.total()).isEqualByComparingTo("210.00");
        UUID quitar = conDos.items().get(1).id();
        assertThat(planes.quitarItem(principalMedico, plan.id(), quitar).total())
                .isEqualByComparingTo("120.00");
    }

    @Test
    void elPlanSigueElFlujoPresentarAceptarYEjecutarSesionesActualizandoElOdontograma() {
        PlanResponse plan = planes.crear(principalMedico, new PlanRequest(paciente.getId(), "Plan"));
        planes.agregarItem(
                principalMedico, plan.id(), new ItemPlanRequest(obturacion.getId(), 16, Superficie.O, null, null));
        UUID itemObturacion =
                planes.obtener(principalMedico, plan.id()).items().get(0).id();

        assertThat(planes.mios(principalPaciente)).isEmpty();
        assertThatThrownBy(() -> planes.obtener(principalPaciente, plan.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> planes.registrarSesion(
                        principalMedico, plan.id(), new SesionRequest(List.of(itemObturacion), null, null)))
                .isInstanceOf(ConflictException.class);

        PlanResponse presentado = planes.presentar(principalMedico, plan.id());
        assertThat(presentado.estado()).isEqualTo(EstadoPlan.PRESENTADO);
        assertThatThrownBy(() -> planes.agregarItem(
                        principalMedico, plan.id(), new ItemPlanRequest(profilaxis.getId(), null, null, null, null)))
                .isInstanceOf(ConflictException.class);
        assertThat(planes.mios(principalPaciente)).hasSize(1);

        assertThat(planes.responder(principalPaciente, plan.id(), true, null).estado())
                .isEqualTo(EstadoPlan.ACEPTADO);
        assertThatThrownBy(() -> planes.responder(principalPaciente, plan.id(), false, "x"))
                .isInstanceOf(ConflictException.class);

        PlanResponse tras = planes.registrarSesion(
                principalMedico, plan.id(), new SesionRequest(List.of(itemObturacion), null, "Primera sesión"));

        assertThat(tras.estado()).isEqualTo(EstadoPlan.COMPLETADO);
        assertThat(tras.items().get(0).estado()).isEqualTo(EstadoItemPlan.REALIZADO);
        assertThat(tras.sesiones()).hasSize(1);
        assertThat(pieza(odontograma.obtener(principalMedico, paciente.getId()), 16)
                        .superficies())
                .containsEntry(Superficie.O, EstadoPieza.OBTURADO);
        assertThat(odontograma
                        .historial(principalMedico, paciente.getId(), 16)
                        .get(0)
                        .itemId())
                .isEqualTo(itemObturacion);
        assertThatThrownBy(() -> planes.registrarSesion(
                        principalMedico, plan.id(), new SesionRequest(List.of(itemObturacion), null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unProcedimientoConConsentimientoNoSeEjecutaSinUnoFirmadoYVigente() {
        PlanResponse aceptado = planConTratamientosAceptado();
        UUID itemExo = aceptado.items().get(1).id();

        assertThatThrownBy(() -> planes.registrarSesion(
                        principalMedico, aceptado.id(), new SesionRequest(List.of(itemExo), null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("consentimiento");

        Consentimiento pendiente = consentimientos.save(Consentimiento.builder()
                .plantilla(consentimientoFirmado().getPlantilla())
                .plantillaVersion(1)
                .paciente(paciente)
                .medico(medico)
                .procedimiento("Extracción")
                .textoAceptado("t")
                .build());
        assertThatThrownBy(
                        () -> planes.vincularConsentimiento(principalMedico, aceptado.id(), itemExo, pendiente.getId()))
                .isInstanceOf(DatosInvalidosException.class);
        Consentimiento otroPaciente = consentimientos.save(Consentimiento.builder()
                .plantilla(consentimientoFirmado().getPlantilla())
                .plantillaVersion(1)
                .paciente(fixture.paciente())
                .medico(medico)
                .procedimiento("Extracción")
                .estado(EstadoConsentimiento.FIRMADO)
                .textoAceptado("t")
                .firmadoEn(LocalDateTime.now())
                .build());
        assertThatThrownBy(() ->
                        planes.vincularConsentimiento(principalMedico, aceptado.id(), itemExo, otroPaciente.getId()))
                .isInstanceOf(DatosInvalidosException.class);

        planes.vincularConsentimiento(
                principalMedico, aceptado.id(), itemExo, consentimientoFirmado().getId());
        PlanResponse tras =
                planes.registrarSesion(principalMedico, aceptado.id(), new SesionRequest(List.of(itemExo), null, null));

        assertThat(tras.items().get(1).estado()).isEqualTo(EstadoItemPlan.REALIZADO);
        assertThat(tras.estado()).isEqualTo(EstadoPlan.EN_CURSO);
        assertThat(pieza(odontograma.obtener(principalMedico, paciente.getId()), 18)
                        .estado())
                .isEqualTo(EstadoPieza.AUSENTE);
    }

    @Test
    void soloElOdontologoQueCreoElPlanLoModificaYElPacientePuedeRechazarlo() {
        UsuarioPrincipal colega = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("70.00")).getUsuario());
        PlanResponse plan = planes.crear(principalMedico, new PlanRequest(paciente.getId(), null));

        assertThatThrownBy(() -> planes.agregarItem(
                        colega, plan.id(), new ItemPlanRequest(profilaxis.getId(), null, null, null, null)))
                .isInstanceOf(ForbiddenOperationException.class);
        planes.agregarItem(principalMedico, plan.id(), new ItemPlanRequest(profilaxis.getId(), null, null, null, null));
        planes.presentar(principalMedico, plan.id());

        PlanResponse rechazado = planes.responder(principalPaciente, plan.id(), false, "Es muy costoso");

        assertThat(rechazado.estado()).isEqualTo(EstadoPlan.RECHAZADO);
        assertThat(rechazado.motivoRechazo()).isEqualTo("Es muy costoso");
        assertThatThrownBy(() -> planes.cancelar(principalMedico, plan.id(), "Ya rechazado"))
                .isInstanceOf(ConflictException.class);
        Paciente otro = fixture.paciente();
        assertThatThrownBy(() -> planes.responder(fixture.principal(otro.getUsuario()), plan.id(), true, null))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void cancelarUnPlanCancelaLosProcedimientosPendientesYReduceElPresupuesto() {
        PlanResponse aceptado = planConTratamientosAceptado();

        PlanResponse cancelado = planes.cancelar(principalMedico, aceptado.id(), "El paciente se mudó");

        assertThat(cancelado.estado()).isEqualTo(EstadoPlan.CANCELADO);
        assertThat(cancelado.items()).allMatch(i -> i.estado() == EstadoItemPlan.CANCELADO);
        assertThat(cancelado.total()).isEqualByComparingTo("0");
    }

    @Test
    void cajaCobraConSesionAbiertaSinSuperarElSaldoYSinVerElDetalleClinico() {
        PlanResponse aceptado = planConTratamientosAceptado();
        var usuarioCaja = fixture.usuario("caj", RolNombre.ROLE_CAJA);
        UsuarioPrincipal caja = fixture.principal(usuarioCaja);

        assertThatThrownBy(() -> planes.registrarPago(
                        caja, aceptado.id(), new PagoPlanRequest(new BigDecimal("50"), "EFECTIVO", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("caja");
        sesionesCaja.save(SesionCaja.builder()
                .cajero(usuarioCaja)
                .apertura(LocalDateTime.now())
                .montoApertura(BigDecimal.ZERO)
                .estado(EstadoCaja.ABIERTA)
                .build());

        assertThatThrownBy(() -> planes.registrarPago(
                        caja, aceptado.id(), new PagoPlanRequest(new BigDecimal("500"), "EFECTIVO", null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> planes.registrarPago(
                        caja, aceptado.id(), new PagoPlanRequest(new BigDecimal("50"), "CHEQUE", null)))
                .isInstanceOf(DatosInvalidosException.class);
        PlanResponse tras =
                planes.registrarPago(caja, aceptado.id(), new PagoPlanRequest(new BigDecimal("30.50"), "yape", "OP-1"));

        assertThat(tras.items()).isEmpty();
        assertThat(tras.notas()).isNull();
        assertThat(tras.pagado()).isEqualByComparingTo("30.50");
        assertThat(tras.saldo()).isEqualByComparingTo("199.50");
        assertThat(tras.pagos().get(0).metodo()).isEqualTo("YAPE");
        assertThat(planes.porCobrar()).anyMatch(p -> p.id().equals(aceptado.id()));
        planes.registrarPago(caja, aceptado.id(), new PagoPlanRequest(new BigDecimal("199.50"), "TARJETA", null));
        assertThat(planes.porCobrar()).noneMatch(p -> p.id().equals(aceptado.id()));
    }

    @Test
    void unPlanEnBorradorNoAdmiteCobros() {
        PlanResponse plan = planes.crear(principalMedico, new PlanRequest(paciente.getId(), null));
        var usuarioCaja = fixture.usuario("caj2", RolNombre.ROLE_CAJA);

        assertThatThrownBy(() -> planes.registrarPago(
                        fixture.principal(usuarioCaja),
                        plan.id(),
                        new PagoPlanRequest(new BigDecimal("10"), "EFECTIVO", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void elCatalogoDeProcedimientosSeAdministraSinDuplicarCodigos() {
        ProcedimientoRequest request = new ProcedimientoRequest(
                "nuevo-1",
                "Resina estética",
                "Estética",
                new BigDecimal("150"),
                true,
                true,
                EstadoPieza.OBTURADO,
                false,
                true);

        var creado = odontograma.crearProcedimiento(request);

        assertThat(creado.codigo()).isEqualTo("NUEVO-1");
        assertThatThrownBy(() -> odontograma.crearProcedimiento(request)).isInstanceOf(ConflictException.class);
        var editado = odontograma.actualizarProcedimiento(
                creado.id(),
                new ProcedimientoRequest(
                        "NUEVO-1",
                        "Resina estética",
                        "Estética",
                        new BigDecimal("175"),
                        true,
                        true,
                        EstadoPieza.OBTURADO,
                        false,
                        false));
        assertThat(editado.precio()).isEqualByComparingTo("175");
        assertThat(odontograma.procedimientos(true)).noneMatch(p -> p.codigo().equals("NUEVO-1"));
        assertThat(odontograma.procedimientos(false)).anyMatch(p -> p.codigo().equals("NUEVO-1"));
        assertThatThrownBy(() -> odontograma.actualizarProcedimiento(
                        creado.id(),
                        new ProcedimientoRequest("NUEVO-1", "X", "Y", BigDecimal.ONE, false, true, null, false, true)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    private static PiezaOdontograma pieza(OdontogramaResponse odontograma, int numero) {
        return odontograma.piezas().stream()
                .filter(p -> p.pieza() == numero)
                .findFirst()
                .orElseThrow();
    }
}

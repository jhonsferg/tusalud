package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.CumplirControlRequest;
import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.EstadoPlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.MedicionRequest;
import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.MetaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.PlanRequest;
import com.codesoftlabs.tusalud.app.dto.response.CronicosResponses.PlanDetalleResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.EstadoPlan;
import com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.ParametroSeguimiento;
import com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.ProgramaCronico;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.cronicos.PlanesTratamientoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
class PlanesTratamientoServiceTest {

    @Autowired
    private PlanesTratamientoService planes;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal principalMedico;
    private UsuarioPrincipal principalPaciente;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        fixture.cita(medico, paciente, LocalDateTime.now().plusDays(1), EstadoCita.CONFIRMADA);
        principalMedico = fixture.principal(medico.getUsuario());
        principalPaciente = fixture.principal(paciente.getUsuario());
    }

    private PlanRequest plan(LocalDate inicio) {
        return new PlanRequest(
                paciente.getId(),
                ProgramaCronico.HIPERTENSION,
                "I10",
                "Presión arterial bajo 130 sobre 80",
                "Dieta y ejercicio",
                "Losartán 50 mg cada día",
                null,
                inicio,
                30,
                3,
                List.of(
                        new MetaRequest(ParametroSeguimiento.PRESION_SISTOLICA, null, new BigDecimal("130"), "mmHg"),
                        new MetaRequest(ParametroSeguimiento.PRESION_DIASTOLICA, null, new BigDecimal("80"), "mmHg")));
    }

    @Test
    void elPlanGeneraSusControlesProgramados() {
        PlanDetalleResponse creado = planes.crear(principalMedico, plan(LocalDate.now()));

        assertThat(creado.controles()).hasSize(3);
        assertThat(creado.controles().get(0).fechaProgramada())
                .isEqualTo(LocalDate.now().plusDays(30));
        assertThat(creado.controles().get(2).fechaProgramada())
                .isEqualTo(LocalDate.now().plusDays(90));
        assertThat(creado.metas()).hasSize(2);
        assertThat(creado.adherencia()).isEqualTo(100);
        assertThat(creado.riesgo()).isEqualTo("BAJO");
    }

    @Test
    void soloUnMedicoConRelacionCreaElPlan() {
        Medico otro = fixture.medicoConHorarioLunes(new BigDecimal("90.00"));

        assertThatThrownBy(() -> planes.crear(fixture.principal(otro.getUsuario()), plan(LocalDate.now())))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void lasMetasNoSeRepitenNiQuedanSinRango() {
        var repetida = new PlanRequest(
                paciente.getId(),
                ProgramaCronico.DIABETES,
                "E11",
                "Glucosa en ayunas bajo 130",
                null,
                null,
                null,
                LocalDate.now(),
                30,
                2,
                List.of(
                        new MetaRequest(ParametroSeguimiento.GLUCOSA_AYUNO, null, new BigDecimal("130"), "mg/dl"),
                        new MetaRequest(ParametroSeguimiento.GLUCOSA_AYUNO, null, new BigDecimal("140"), "mg/dl")));

        assertThatThrownBy(() -> planes.crear(principalMedico, repetida)).isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void unaMedicionFueraDeLaMetaSeMarcaYSubeElRiesgo() {
        PlanDetalleResponse creado = planes.crear(principalMedico, plan(LocalDate.now()));

        PlanDetalleResponse conMedicion = planes.registrarMedicion(
                principalPaciente,
                creado.id(),
                new MedicionRequest(ParametroSeguimiento.PRESION_SISTOLICA, new BigDecimal("150"), null));

        assertThat(conMedicion.mediciones()).hasSize(1);
        assertThat(conMedicion.mediciones().get(0).fueraDeRango()).isTrue();
        assertThat(conMedicion.mediciones().get(0).origen()).isEqualTo("PACIENTE");
        assertThat(conMedicion.riesgo()).isEqualTo("MEDIO");

        planes.registrarMedicion(
                principalPaciente,
                creado.id(),
                new MedicionRequest(ParametroSeguimiento.PRESION_SISTOLICA, new BigDecimal("155"), null));
        PlanDetalleResponse tres = planes.registrarMedicion(
                principalMedico,
                creado.id(),
                new MedicionRequest(ParametroSeguimiento.PRESION_SISTOLICA, new BigDecimal("160"), null));

        assertThat(tres.mediciones()).extracting(m -> m.numero()).containsExactly(1, 2, 3);
        assertThat(tres.riesgo()).isEqualTo("ALTO");
        assertThat(planes.tablero(principalMedico)).hasSize(1);
        assertThat(planes.tablero(principalMedico).get(0).riesgo()).isEqualTo("ALTO");
    }

    @Test
    void unaMedicionDentroDeLaMetaNoAlerta() {
        PlanDetalleResponse creado = planes.crear(principalMedico, plan(LocalDate.now()));

        PlanDetalleResponse conMedicion = planes.registrarMedicion(
                principalPaciente,
                creado.id(),
                new MedicionRequest(ParametroSeguimiento.PRESION_SISTOLICA, new BigDecimal("120"), null));

        assertThat(conMedicion.mediciones().get(0).fueraDeRango()).isFalse();
        assertThat(conMedicion.riesgo()).isEqualTo("BAJO");
    }

    @Test
    void otroPacienteNoVeNiMideElPlan() {
        PlanDetalleResponse creado = planes.crear(principalMedico, plan(LocalDate.now()));
        UsuarioPrincipal ajeno = fixture.principal(fixture.paciente().getUsuario());

        assertThatThrownBy(() -> planes.obtener(ajeno, creado.id())).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> planes.registrarMedicion(
                        ajeno,
                        creado.id(),
                        new MedicionRequest(ParametroSeguimiento.PESO_KG, new BigDecimal("70"), null)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void laAdherenciaYElVencimientoSeCalculanPorLaFecha() {
        PlanDetalleResponse creado =
                planes.crear(principalMedico, plan(LocalDate.now().minusDays(80)));

        assertThat(creado.controles().stream().filter(c -> c.vencido()).count()).isEqualTo(2);
        assertThat(creado.riesgo()).isEqualTo("ALTO");
        assertThat(creado.adherencia()).isZero();

        PlanDetalleResponse cumplido = planes.cumplirControl(
                principalMedico, creado.id(), creado.controles().get(0).id(), new CumplirControlRequest(null));

        assertThat(cumplido.adherencia()).isEqualTo(50);
        assertThatThrownBy(() -> planes.cumplirControl(
                        principalMedico, creado.id(), creado.controles().get(0).id(), new CumplirControlRequest(null)))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void laAlertaDeControlesVencidosSeEnviaUnaSolaVez() {
        planes.crear(principalMedico, plan(LocalDate.now().minusDays(70)));

        assertThat(planes.alertarControlesVencidos()).isEqualTo(2);
        assertThat(planes.alertarControlesVencidos()).isZero();
    }

    @Test
    void unPlanCompletadoNoAceptaMedicionesNiSeReactiva() {
        PlanDetalleResponse creado = planes.crear(principalMedico, plan(LocalDate.now()));
        planes.cambiarEstado(principalMedico, creado.id(), new EstadoPlanRequest(EstadoPlan.COMPLETADO));

        assertThatThrownBy(() -> planes.registrarMedicion(
                        principalPaciente,
                        creado.id(),
                        new MedicionRequest(ParametroSeguimiento.PRESION_SISTOLICA, new BigDecimal("120"), null)))
                .isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(() ->
                        planes.cambiarEstado(principalMedico, creado.id(), new EstadoPlanRequest(EstadoPlan.ACTIVO)))
                .isInstanceOf(BusinessLogicException.class);
    }
}

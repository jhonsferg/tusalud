package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.request.DatosMedicosRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaItemRequest;
import com.codesoftlabs.tusalud.app.dto.response.HistoriaClinicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteHistoriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteMedicoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import com.codesoftlabs.tusalud.app.services.interfaces.HistoriaClinicaService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
class HistoriaClinicaServiceImplTest {

    @Autowired
    private HistoriaClinicaService historiaClinicaService;

    @Autowired
    private AtencionService atencionService;

    @Autowired
    private ServiceTestFixture fixture;

    private static AtencionRequest atencion(String diagnostico) {
        return new AtencionRequest(
                diagnostico,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(new RecetaItemRequest("Paracetamol 500 mg", "1 tableta", "Cada 8 horas", "3 días")));
    }

    @Test
    void obtenerMiaDevuelveAtencionesDeLaMasRecienteALaMasAntigua() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Paciente paciente = fixture.paciente();
        UsuarioPrincipal principalMedico = fixture.principal(medico.getUsuario());
        Cita antigua = fixture.cita(medico, paciente, LocalDateTime.now().minusDays(10), EstadoCita.CONFIRMADA);
        Cita reciente = fixture.cita(medico, paciente, LocalDateTime.now().minusDays(2), EstadoCita.CONFIRMADA);
        atencionService.registrar(antigua.getId(), atencion("Antigua"), principalMedico);
        atencionService.registrar(reciente.getId(), atencion("Reciente"), principalMedico);

        HistoriaClinicaResponse historia = historiaClinicaService.obtenerMia(fixture.principal(paciente.getUsuario()));

        assertThat(historia.atenciones()).extracting(a -> a.diagnostico()).containsExactly("Reciente", "Antigua");
        assertThat(historia.atenciones().get(0).receta())
                .singleElement()
                .satisfies(r -> assertThat(r.medicamento()).isEqualTo("Paracetamol 500 mg"));
        assertThat(historia.paciente().dni()).isEqualTo(paciente.getDni());
        assertThat(historia.paciente().historialClinicoNumero()).isEqualTo(paciente.getHistorialClinicoNumero());
    }

    @Test
    void actualizarDatosMedicosGuardaGrupoAlergiasYAntecedentes() {
        Paciente paciente = fixture.paciente();
        UsuarioPrincipal principal = fixture.principal(paciente.getUsuario());

        PacienteHistoriaResponse datos = historiaClinicaService.actualizarDatosMedicos(
                principal, new DatosMedicosRequest("O+", "Penicilina", "Hipertensión familiar"));

        assertThat(datos.grupoSanguineo()).isEqualTo("O+");
        assertThat(datos.alergias()).isEqualTo("Penicilina");
        assertThat(datos.antecedentes()).isEqualTo("Hipertensión familiar");
        assertThat(datos.edad()).isNotNull();
        assertThat(historiaClinicaService.obtenerMia(principal).paciente().grupoSanguineo())
                .isEqualTo("O+");
    }

    @Test
    void actualizarDatosMedicosConTextoVacioLosLimpia() {
        Paciente paciente = fixture.paciente();
        UsuarioPrincipal principal = fixture.principal(paciente.getUsuario());
        historiaClinicaService.actualizarDatosMedicos(principal, new DatosMedicosRequest("A+", "Polen", null));

        PacienteHistoriaResponse datos =
                historiaClinicaService.actualizarDatosMedicos(principal, new DatosMedicosRequest(null, "  ", null));

        assertThat(datos.grupoSanguineo()).isNull();
        assertThat(datos.alergias()).isNull();
        assertThat(datos.antecedentes()).isNull();
    }

    @Test
    void medicoSoloVeLaHistoriaDePacientesConCitasSuyas() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Paciente conCita = fixture.paciente();
        Paciente sinCita = fixture.paciente();
        fixture.cita(medico, conCita, LocalDateTime.now().plusDays(3), EstadoCita.PENDIENTE);
        UsuarioPrincipal principalMedico = fixture.principal(medico.getUsuario());

        HistoriaClinicaResponse historia = historiaClinicaService.obtenerDePaciente(conCita.getId(), principalMedico);

        assertThat(historia.paciente().pacienteId()).isEqualTo(conCita.getId());
        assertThatThrownBy(() -> historiaClinicaService.obtenerDePaciente(sinCita.getId(), principalMedico))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void listarPacientesDeMedicoAgregaCitasYUltimaCita() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Paciente paciente = fixture.paciente();
        LocalDateTime primera = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime segunda = LocalDateTime.now().plusDays(5).withNano(0);
        fixture.cita(medico, paciente, primera, EstadoCita.CONFIRMADA);
        fixture.cita(medico, paciente, segunda, EstadoCita.PENDIENTE);

        List<PacienteMedicoResponse> pacientes =
                historiaClinicaService.listarPacientesDeMedico(fixture.principal(medico.getUsuario()));

        assertThat(pacientes).singleElement().satisfies(p -> {
            assertThat(p.pacienteId()).isEqualTo(paciente.getId());
            assertThat(p.totalCitas()).isEqualTo(2);
            assertThat(p.ultimaCita()).isEqualTo(segunda);
        });
    }

    @Test
    void estadoDeCitaNoAfectaLaHistoriaDelPaciente() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Paciente paciente = fixture.paciente();
        fixture.cita(medico, paciente, LocalDateTime.now().plusDays(4), EstadoCita.CANCELADA);

        assertThat(historiaClinicaService
                        .obtenerDePaciente(paciente.getId(), fixture.principal(medico.getUsuario()))
                        .atenciones())
                .isEmpty();
    }
}

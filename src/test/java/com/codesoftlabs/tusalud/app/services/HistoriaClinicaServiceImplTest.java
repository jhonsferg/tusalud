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
import com.codesoftlabs.tusalud.app.entities.PacienteCompartido;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.AuditoriaClinicaRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteCompartidoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
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

    @Autowired
    private AuditoriaClinicaRepository auditoriaRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private PacienteCompartidoRepository compartidoRepository;

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

    private static void pausar() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void obtenerMiaDevuelveAtencionesDeLaMasRecienteALaMasAntigua() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Paciente paciente = fixture.paciente();
        UsuarioPrincipal principalMedico = fixture.principal(medico.getUsuario());
        Cita antigua = fixture.cita(medico, paciente, LocalDateTime.now().minusDays(10), EstadoCita.CONFIRMADA);
        Cita reciente = fixture.cita(medico, paciente, LocalDateTime.now().minusDays(2), EstadoCita.CONFIRMADA);
        atencionService.registrar(antigua.getId(), atencion("Antigua"), principalMedico);
        pausar();
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
    void medicoConRelacionActualizaLosDatosMedicosYQuedaAuditado() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Paciente paciente = fixture.paciente();
        fixture.cita(medico, paciente, LocalDateTime.now().plusDays(3), EstadoCita.CONFIRMADA);
        UsuarioPrincipal principalMedico = fixture.principal(medico.getUsuario());

        PacienteHistoriaResponse datos = historiaClinicaService.actualizarDatosMedicos(
                paciente.getId(),
                principalMedico,
                new DatosMedicosRequest("O+", "Penicilina", "Hipertensión familiar"));

        assertThat(datos.grupoSanguineo()).isEqualTo("O+");
        assertThat(datos.alergias()).isEqualTo("Penicilina");
        assertThat(datos.antecedentes()).isEqualTo("Hipertensión familiar");
        assertThat(datos.edad()).isNotNull();
        assertThat(datos.datosMedicosActualizadoPor())
                .isEqualTo(medico.getUsuario().getNombreCompleto());
        assertThat(datos.datosMedicosActualizadoEn()).isNotNull();
        assertThat(historiaClinicaService
                        .obtenerMia(fixture.principal(paciente.getUsuario()))
                        .paciente()
                        .grupoSanguineo())
                .isEqualTo("O+");
        assertThat(auditoriaRepository.findByPacienteIdOrderByFechaDesc(paciente.getId()))
                .singleElement()
                .satisfies(registro -> {
                    assertThat(registro.getAccion()).isEqualTo(AuditoriaClinicaService.DATOS_MEDICOS_ACTUALIZADOS);
                    assertThat(registro.getUsuario().getId())
                            .isEqualTo(medico.getUsuario().getId());
                    assertThat(registro.getDetalle()).contains("grupo sanguíneo", "alergias", "antecedentes");
                });
    }

    @Test
    void actualizarDatosMedicosConTextoVacioLosLimpia() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Paciente paciente = fixture.paciente();
        fixture.cita(medico, paciente, LocalDateTime.now().plusDays(3), EstadoCita.CONFIRMADA);
        UsuarioPrincipal principalMedico = fixture.principal(medico.getUsuario());
        historiaClinicaService.actualizarDatosMedicos(
                paciente.getId(), principalMedico, new DatosMedicosRequest("A+", "Polen", null));

        PacienteHistoriaResponse datos = historiaClinicaService.actualizarDatosMedicos(
                paciente.getId(), principalMedico, new DatosMedicosRequest(null, "  ", null));

        assertThat(datos.grupoSanguineo()).isNull();
        assertThat(datos.alergias()).isNull();
        assertThat(datos.antecedentes()).isNull();
    }

    @Test
    void medicoSinRelacionNoPuedeActualizarDatosMedicos() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Paciente ajeno = fixture.paciente();
        UsuarioPrincipal principalMedico = fixture.principal(medico.getUsuario());

        assertThatThrownBy(() -> historiaClinicaService.actualizarDatosMedicos(
                        ajeno.getId(), principalMedico, new DatosMedicosRequest("B+", "Latex", null)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(pacienteRepository.findById(ajeno.getId()).orElseThrow().getGrupoSanguineo())
                .isNull();
    }

    @Test
    void unaCitaCanceladaNoDaRelacionAsistencial() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Paciente paciente = fixture.paciente();
        fixture.cita(medico, paciente, LocalDateTime.now().plusDays(4), EstadoCita.CANCELADA);
        UsuarioPrincipal principalMedico = fixture.principal(medico.getUsuario());

        assertThatThrownBy(() -> historiaClinicaService.obtenerDePaciente(paciente.getId(), principalMedico))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> historiaClinicaService.actualizarDatosMedicos(
                        paciente.getId(), principalMedico, new DatosMedicosRequest("O+", null, null)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(historiaClinicaService.listarPacientesDeMedico(principalMedico))
                .isEmpty();
    }

    @Test
    void elPacienteCompartidoDaAccesoYConsultarLaHistoriaQuedaAuditado() {
        Medico origen = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Medico destino = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Paciente paciente = fixture.paciente();
        fixture.cita(origen, paciente, LocalDateTime.now().plusDays(3), EstadoCita.PENDIENTE);
        UsuarioPrincipal principalDestino = fixture.principal(destino.getUsuario());
        assertThatThrownBy(() -> historiaClinicaService.obtenerDePaciente(paciente.getId(), principalDestino))
                .isInstanceOf(ForbiddenOperationException.class);

        compartidoRepository.save(PacienteCompartido.builder()
                .paciente(paciente)
                .medicoOrigen(origen)
                .medicoDestino(destino)
                .build());
        historiaClinicaService.obtenerDePaciente(paciente.getId(), principalDestino);

        assertThat(auditoriaRepository.findByPacienteIdOrderByFechaDesc(paciente.getId()))
                .singleElement()
                .satisfies(registro -> {
                    assertThat(registro.getAccion()).isEqualTo(AuditoriaClinicaService.HISTORIA_CONSULTADA);
                    assertThat(registro.getUsuario().getId())
                            .isEqualTo(destino.getUsuario().getId());
                });
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
}

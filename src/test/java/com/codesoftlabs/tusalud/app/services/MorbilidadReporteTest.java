package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.request.DiagnosticoRequest;
import com.codesoftlabs.tusalud.app.dto.response.ReporteMorbilidadResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.AgrupacionReporte;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDiagnostico;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import com.codesoftlabs.tusalud.app.services.interfaces.ReporteService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class MorbilidadReporteTest {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private AtencionService atencionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ServiceTestFixture fixture;

    private int minuto;

    private Paciente paciente(SexoPaciente sexo, Integer edad) {
        Paciente paciente = fixture.paciente();
        paciente.setSexo(sexo);
        paciente.setFechaNacimiento(
                edad == null ? null : LocalDate.now().minusYears(edad).minusDays(30));
        return paciente;
    }

    private void atender(Medico medico, Paciente paciente, String principal, String secundario) {
        UsuarioPrincipal actor = fixture.principal(medico.getUsuario());
        List<DiagnosticoRequest> diagnosticos = new ArrayList<>();
        diagnosticos.add(new DiagnosticoRequest(principal, TipoDiagnostico.DEFINITIVO, true, null));
        if (secundario != null) {
            diagnosticos.add(new DiagnosticoRequest(secundario, TipoDiagnostico.PRESUNTIVO, false, null));
        }
        var cita = fixture.cita(
                medico, paciente, LocalDateTime.now().plusDays(3).plusMinutes(minuto++), EstadoCita.CONFIRMADA);
        atencionService.registrar(
                cita.getId(),
                new AtencionRequest(null, null, null, null, null, null, null, null, null, List.of(), diagnosticos),
                actor);
    }

    @Test
    void cuentaSoloElDiagnosticoPrincipalYAgrupaPorCapituloEspecialidadYSerie() {
        Medico medico = fixture.medicoConHorarioLunes(new java.math.BigDecimal("100.00"));
        for (int i = 0; i < 5; i++) {
            atender(medico, paciente(SexoPaciente.M, 40), "I10", i == 0 ? "E11.9" : null);
        }
        for (int i = 0; i < 2; i++) {
            atender(medico, paciente(SexoPaciente.F, 40), "I10", null);
        }
        for (int i = 0; i < 3; i++) {
            atender(medico, paciente(SexoPaciente.M, 8), "J06.9", null);
        }
        atender(medico, paciente(null, null), "R50.9", null);

        ReporteMorbilidadResponse reporte = reporteService.morbilidad(null, null, AgrupacionReporte.DIA);

        assertThat(reporte.casos()).isEqualTo(11);
        assertThat(reporte.diagnosticosDistintos()).isEqualTo(3);
        assertThat(reporte.umbralSupresion()).isEqualTo(5);
        assertThat(reporte.top())
                .extracting(ReporteMorbilidadResponse.Diagnostico::codigo)
                .containsExactly("I10", "J06.9", "R50.9");
        assertThat(reporte.top().get(0).casos()).isEqualTo(7);
        assertThat(reporte.top().get(0).porcentaje()).isEqualByComparingTo("63.64");
        assertThat(reporte.top()).noneMatch(d -> d.codigo().equals("E11.9"));
        assertThat(reporte.porCapitulo())
                .extracting(ReporteMorbilidadResponse.Capitulo::capitulo)
                .containsExactly("Capítulo de prueba");
        assertThat(reporte.porCapitulo().get(0).casos()).isEqualTo(11);
        assertThat(reporte.porEspecialidad()).hasSize(1);
        assertThat(reporte.porEspecialidad().get(0).casos()).isEqualTo(11);
        assertThat(reporte.porEspecialidad().get(0).top()).hasSize(3);
        assertThat(reporte.serie().stream()
                        .mapToLong(ReporteMorbilidadResponse.Punto::casos)
                        .sum())
                .isEqualTo(11);
        assertThat(reporte.serie()).hasSize(reporte.periodo().dias());
        assertThat(reporte.tendencia()).hasSize(3);
        assertThat(reporte.tendencia().get(0).serie().stream()
                        .mapToLong(ReporteMorbilidadResponse.Punto::casos)
                        .sum())
                .isEqualTo(7);
    }

    @Test
    void ocultaLasCeldasConMenosDeCincoCasosEnLosCortesPorEdadYSexo() {
        Medico medico = fixture.medicoConHorarioLunes(new java.math.BigDecimal("100.00"));
        for (int i = 0; i < 5; i++) {
            atender(medico, paciente(SexoPaciente.M, 40), "I10", null);
        }
        for (int i = 0; i < 2; i++) {
            atender(medico, paciente(SexoPaciente.F, 40), "I10", null);
        }
        for (int i = 0; i < 3; i++) {
            atender(medico, paciente(SexoPaciente.M, 8), "J06.9", null);
        }
        atender(medico, paciente(null, null), "R50.9", null);

        List<ReporteMorbilidadResponse.GrupoEtario> grupos =
                reporteService.morbilidad(null, null, null).porGrupoEtario();

        ReporteMorbilidadResponse.GrupoEtario adultos = grupos.stream()
                .filter(g -> g.grupo().startsWith("30 a 59"))
                .findFirst()
                .orElseThrow();
        assertThat(adultos.masculino()).isEqualTo(5L);
        assertThat(adultos.femenino()).isNull();
        assertThat(adultos.total()).isNull();
        assertThat(adultos.suprimido()).isTrue();

        ReporteMorbilidadResponse.GrupoEtario ninos = grupos.stream()
                .filter(g -> g.grupo().startsWith("5 a 11"))
                .findFirst()
                .orElseThrow();
        assertThat(ninos.masculino()).isNull();
        assertThat(ninos.total()).isNull();
        assertThat(ninos.suprimido()).isTrue();

        ReporteMorbilidadResponse.GrupoEtario sinDato = grupos.stream()
                .filter(g -> g.grupo().startsWith("Sin dato"))
                .findFirst()
                .orElseThrow();
        assertThat(sinDato.sinDato()).isNull();
        assertThat(sinDato.suprimido()).isTrue();

        ReporteMorbilidadResponse.GrupoEtario vacio = grupos.stream()
                .filter(g -> g.grupo().startsWith("60"))
                .findFirst()
                .orElseThrow();
        assertThat(vacio.total()).isZero();
        assertThat(vacio.suprimido()).isFalse();
    }

    @Test
    void unGrupoConTodasLasCeldasVisiblesMuestraSuTotal() {
        Medico medico = fixture.medicoConHorarioLunes(new java.math.BigDecimal("100.00"));
        for (int i = 0; i < 5; i++) {
            atender(medico, paciente(SexoPaciente.M, 20), "J06.9", null);
            atender(medico, paciente(SexoPaciente.F, 20), "J06.9", null);
        }
        ReporteMorbilidadResponse.GrupoEtario jovenes =
                reporteService.morbilidad(null, null, null).porGrupoEtario().stream()
                        .filter(g -> g.grupo().startsWith("18 a 29"))
                        .findFirst()
                        .orElseThrow();
        assertThat(jovenes.masculino()).isEqualTo(5L);
        assertThat(jovenes.femenino()).isEqualTo(5L);
        assertThat(jovenes.total()).isEqualTo(10L);
        assertThat(jovenes.suprimido()).isFalse();
    }

    @Test
    void elReporteNoContieneDatosDePacientes() throws Exception {
        Medico medico = fixture.medicoConHorarioLunes(new java.math.BigDecimal("100.00"));
        Paciente paciente = paciente(SexoPaciente.M, 40);
        atender(medico, paciente, "I10", null);

        String json = objectMapper.writeValueAsString(reporteService.morbilidad(null, null, null));

        assertThat(json)
                .doesNotContain(paciente.getDni())
                .doesNotContain(paciente.getUsuario().getEmail())
                .doesNotContain(paciente.getUsuario().getApellido())
                .doesNotContain(medico.getUsuario().getEmail());
    }

    @Test
    void validaElRangoDelReporte() {
        assertThatThrownBy(() -> reporteService.morbilidad(LocalDate.now().minusDays(400), LocalDate.now(), null))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> reporteService.morbilidad(
                        LocalDate.now(), LocalDate.now().minusDays(1), null))
                .isInstanceOf(DatosInvalidosException.class);
        assertThat(reporteService
                        .morbilidad(LocalDate.now().minusDays(7), LocalDate.now(), AgrupacionReporte.SEMANA)
                        .casos())
                .isZero();
    }
}

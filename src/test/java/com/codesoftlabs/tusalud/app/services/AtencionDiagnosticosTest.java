package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.request.DiagnosticoRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.Cie10FrecuenteResponse;
import com.codesoftlabs.tusalud.app.dto.response.DiagnosticoResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDiagnostico;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.AuditoriaClinicaRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import com.codesoftlabs.tusalud.app.services.interfaces.Cie10Service;
import jakarta.persistence.EntityManager;
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
class AtencionDiagnosticosTest {

    @Autowired
    private AtencionService atencionService;

    @Autowired
    private AtencionRepository atencionRepository;

    @Autowired
    private AuditoriaClinicaRepository auditoriaRepository;

    @Autowired
    private Cie10Service cie10Service;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal principalMedico;
    private Cita cita;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new java.math.BigDecimal("100.00"));
        paciente = fixture.paciente();
        principalMedico = fixture.principal(medico.getUsuario());
        cita = fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
    }

    private static DiagnosticoRequest dx(String codigo, boolean principal) {
        return new DiagnosticoRequest(codigo, TipoDiagnostico.DEFINITIVO, principal, null);
    }

    private static AtencionRequest atencion(String descripcion, List<DiagnosticoRequest> diagnosticos) {
        return new AtencionRequest(
                descripcion, null, null, null, null, null, null, null, null, List.of(), diagnosticos);
    }

    @Test
    void registraElPrincipalYLosSecundariosCodificadosEnOrden() {
        AtencionResponse respuesta = atencionService.registrar(
                cita.getId(),
                atencion(
                        "Cuadro febril de tres días",
                        List.of(dx("R50.9", false), dx("J06.9", true), dx("E11.9", false))),
                principalMedico);

        assertThat(respuesta.diagnosticos())
                .extracting(DiagnosticoResponse::codigo)
                .containsExactly("J06.9", "R50.9", "E11.9");
        assertThat(respuesta.diagnosticos().get(0).principal()).isTrue();
        assertThat(respuesta.diagnosticos().get(0).descripcion()).contains("vías respiratorias");
        assertThat(respuesta.diagnostico()).isEqualTo("Cuadro febril de tres días");
        assertThat(respuesta.advertencias()).isEmpty();
    }

    @Test
    void siFaltaLaDescripcionLibreUsaLaDelDiagnosticoPrincipal() {
        AtencionResponse respuesta =
                atencionService.registrar(cita.getId(), atencion(null, List.of(dx("I10", true))), principalMedico);
        assertThat(respuesta.diagnostico()).isEqualTo("Hipertensión esencial (primaria)");
        AtencionResponse conEspacios =
                atencionService.actualizar(cita.getId(), atencion("   ", List.of(dx("I10", true))), principalMedico);
        assertThat(conEspacios.diagnostico()).isEqualTo("Hipertensión esencial (primaria)");
    }

    @Test
    void exigeUnDiagnosticoPrincipalCodificadoYValidoSinDuplicados() {
        assertThatThrownBy(() -> atencionService.registrar(cita.getId(), atencion("x", List.of()), principalMedico))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("al menos un diagnóstico");
        assertThatThrownBy(() -> atencionService.registrar(cita.getId(), atencion("x", null), principalMedico))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> atencionService.registrar(
                        cita.getId(), atencion("x", List.of(dx("I10", false), dx("J06.9", false))), principalMedico))
                .hasMessageContaining("exactamente un diagnóstico principal");
        assertThatThrownBy(() -> atencionService.registrar(
                        cita.getId(), atencion("x", List.of(dx("I10", true), dx("J06.9", true))), principalMedico))
                .hasMessageContaining("exactamente un diagnóstico principal");
        assertThatThrownBy(() -> atencionService.registrar(
                        cita.getId(), atencion("x", List.of(dx("I10", true), dx("i10", false))), principalMedico))
                .hasMessageContaining("repetido");
        assertThatThrownBy(() -> atencionService.registrar(
                        cita.getId(), atencion("x", List.of(dx("Z99.9", true))), principalMedico))
                .hasMessageContaining("no existe");
        assertThatThrownBy(() -> atencionService.registrar(
                        cita.getId(), atencion("x", List.of(dx("K29.7", true))), principalMedico))
                .hasMessageContaining("inactivo");
        assertThat(atencionRepository.existsByCitaId(cita.getId())).isFalse();
    }

    @Test
    void actualizarReconciliaSinChocarConLaRestriccionDeUnicidad() {
        atencionService.registrar(
                cita.getId(),
                atencion("Control", List.of(dx("I10", true), dx("E11.9", false), dx("R50.9", false))),
                principalMedico);
        entityManager.flush();
        entityManager.clear();

        AtencionResponse respuesta = atencionService.actualizar(
                cita.getId(),
                atencion("Control", List.of(dx("E11.9", true), dx("I10", false), dx("J06.9", false))),
                principalMedico);
        entityManager.flush();
        entityManager.clear();

        AtencionResponse leida = atencionService.obtener(cita.getId(), principalMedico);
        assertThat(respuesta.diagnosticos())
                .extracting(DiagnosticoResponse::codigo)
                .containsExactly("E11.9", "I10", "J06.9");
        assertThat(leida.diagnosticos())
                .extracting(DiagnosticoResponse::codigo)
                .containsExactly("E11.9", "I10", "J06.9");
        assertThat(leida.diagnosticos().stream()
                        .filter(DiagnosticoResponse::principal)
                        .count())
                .isEqualTo(1);
    }

    @Test
    void avisaSinBloquearCuandoElSexoOLaEdadNoEncajanConElCodigo() {
        paciente.setSexo(SexoPaciente.M);
        paciente.setFechaNacimiento(LocalDate.now().minusYears(8));
        AtencionResponse respuesta = atencionService.registrar(
                cita.getId(), atencion("Revisión", List.of(dx("O80", true), dx("N40", false))), principalMedico);

        assertThat(respuesta.advertencias()).hasSize(3);
        assertThat(respuesta.advertencias()).anyMatch(a -> a.contains("O80") && a.contains("femenino"));
        assertThat(respuesta.advertencias()).anyMatch(a -> a.contains("O80") && a.contains("desde los 10"));
        assertThat(respuesta.advertencias()).anyMatch(a -> a.contains("N40") && a.contains("desde los 40"));
        assertThat(respuesta.diagnosticos()).hasSize(2);
        assertThat(atencionService.obtener(cita.getId(), principalMedico).advertencias())
                .isEmpty();
    }

    @Test
    void sinDatosDelPacienteNoHayAdvertencias() {
        paciente.setSexo(null);
        paciente.setFechaNacimiento(null);
        AtencionResponse respuesta = atencionService.registrar(
                cita.getId(), atencion("Revisión", List.of(dx("O80", true))), principalMedico);
        assertThat(respuesta.advertencias()).isEmpty();
    }

    @Test
    void unaAtencionAnteriorSinCodigosSigueSiendoLegible() {
        Atencion antigua = atencionRepository.save(Atencion.builder()
                .cita(cita)
                .paciente(paciente)
                .medico(medico)
                .fecha(LocalDateTime.now())
                .diagnostico("Gripe común de registro antiguo")
                .build());
        entityManager.flush();
        entityManager.clear();

        AtencionResponse leida = atencionService.obtener(cita.getId(), principalMedico);
        assertThat(leida.id()).isEqualTo(antigua.getId());
        assertThat(leida.diagnostico()).isEqualTo("Gripe común de registro antiguo");
        assertThat(leida.diagnosticos()).isEmpty();
    }

    @Test
    void registrarYActualizarQuedanAuditadosSinContenidoClinicoLibre() {
        long antes = auditoriaRepository.count();
        atencionService.registrar(
                cita.getId(), atencion("Control", List.of(dx("I10", true), dx("E11.9", false))), principalMedico);
        atencionService.actualizar(cita.getId(), atencion("Control", List.of(dx("I10", true))), principalMedico);

        assertThat(auditoriaRepository.count()).isEqualTo(antes + 2);
        assertThat(auditoriaRepository.findAll())
                .filteredOn(a -> a.getAccion().startsWith("DIAGNOSTICOS_"))
                .extracting(a -> a.getDetalle())
                .anyMatch(d -> d.contains("principal I10") && d.contains("E11.9"));
    }

    @Test
    void elMedicoVeSusDiagnosticosMasFrecuentes() {
        atencionService.registrar(
                cita.getId(), atencion("Control", List.of(dx("I10", true), dx("E11.9", false))), principalMedico);

        List<Cie10FrecuenteResponse> frecuentes = cie10Service.frecuentes(principalMedico, 5);
        assertThat(frecuentes).extracting(Cie10FrecuenteResponse::codigo).contains("I10", "E11.9");
        assertThat(cie10Service.frecuentes(
                        fixture.principal(fixture.medicoConHorarioLunes(java.math.BigDecimal.TEN)
                                .getUsuario()),
                        5))
                .isEmpty();
    }
}

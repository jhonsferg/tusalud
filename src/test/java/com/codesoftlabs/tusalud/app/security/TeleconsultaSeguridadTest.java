package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Especialidad;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.repositories.EspecialidadRepository;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.codesoftlabs.tusalud.app.services.interfaces.TeleconsultaService;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class TeleconsultaSeguridadTest {

    private static final String BASE = "/api/v1/teleconsultas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private TeleconsultaService teleconsultaService;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    private Medico medico;
    private Paciente paciente;
    private UUID teleconsultaId;
    private String tokenMedico;
    private String tokenPaciente;
    private String tokenOtroMedico;
    private String tokenOtroPaciente;
    private String tokenAdmin;
    private String tokenRecepcion;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        Cita cita = fixture.cita(medico, paciente, LocalDateTime.now().plusMinutes(3), EstadoCita.CONFIRMADA);
        teleconsultaService.crearParaCita(cita);
        teleconsultaId = teleconsultaRepository(cita);
        tokenMedico = token(UsuarioPrincipal.from(medico.getUsuario()));
        tokenPaciente = token(UsuarioPrincipal.from(paciente.getUsuario()));
        tokenOtroMedico = token(UsuarioPrincipal.from(fixture.usuario("otro-med", RolNombre.ROLE_MEDICO)));
        tokenOtroPaciente = token(UsuarioPrincipal.from(fixture.paciente().getUsuario()));
        tokenAdmin = token(UsuarioPrincipal.from(fixture.usuario("adm", RolNombre.ROLE_ADMIN)));
        tokenRecepcion = token(UsuarioPrincipal.from(fixture.usuario("recep", RolNombre.ROLE_RECEPCION)));
    }

    @Autowired
    private com.codesoftlabs.tusalud.app.repositories.TeleconsultaRepository repositorio;

    private UUID teleconsultaRepository(Cita cita) {
        return repositorio.findByCitaId(cita.getId()).orElseThrow().getId();
    }

    private String token(UsuarioPrincipal principal) {
        return "Bearer " + jwtUtils.generateToken(principal);
    }

    @Test
    void sinTokenTodosLosEndpointsResponden401() throws Exception {
        mockMvc.perform(get(BASE + "/mias")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/" + teleconsultaId)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/" + teleconsultaId + "/ticket")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/" + teleconsultaId + "/ice")).andExpect(status().isUnauthorized());
    }

    @Test
    void laAdministracionYLaRecepcionNoAccedenALasTeleconsultas() throws Exception {
        for (String token : new String[] {tokenAdmin, tokenRecepcion}) {
            mockMvc.perform(get(BASE + "/" + teleconsultaId).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(BASE + "/" + teleconsultaId + "/ticket").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(BASE + "/medico").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void unMedicoOUnPacienteAjenosNoAccedenPorLaUrl() throws Exception {
        for (String token : new String[] {tokenOtroMedico, tokenOtroPaciente}) {
            mockMvc.perform(get(BASE + "/" + teleconsultaId).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(BASE + "/" + teleconsultaId + "/ticket").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(BASE + "/" + teleconsultaId + "/ice").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(BASE + "/" + teleconsultaId + "/resumen-paciente")
                        .header(HttpHeaders.AUTHORIZATION, tokenOtroMedico))
                .andExpect(status().isForbidden());
    }

    @Test
    void unIdQueNoEsUuidResponde400YUnoInexistente404() throws Exception {
        mockMvc.perform(get(BASE + "/no-es-uuid").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isNotFound());
    }

    @Test
    void elPacienteNecesitaElConsentimientoParaElTicketYLuegoObtieneUnoDeUnSoloUso() throws Exception {
        mockMvc.perform(post(BASE + "/" + teleconsultaId + "/ticket").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isConflict());

        mockMvc.perform(post(BASE + "/consentimiento")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"9.9\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/consentimiento")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"1.0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aceptado", is(true)));

        mockMvc.perform(post(BASE + "/" + teleconsultaId + "/ticket").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticket", notNullValue()))
                .andExpect(jsonPath("$.ruta", is("/ws/teleconsulta")))
                .andExpect(jsonPath("$.expiraEnSegundos", is(30)));
    }

    @Test
    void elMedicoNoNecesitaConsentimientoYElPacienteNoPuedeFinalizarNiAbrir() throws Exception {
        mockMvc.perform(post(BASE + "/" + teleconsultaId + "/ticket").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isCreated());
        mockMvc.perform(post(BASE + "/" + teleconsultaId + "/abrir").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/" + teleconsultaId + "/finalizar")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/" + teleconsultaId + "/abrir").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("SALA_ABIERTA")));
    }

    @Test
    void elResumenDelPacienteNoSeEntregaAlPacienteNiAlOtroMedico() throws Exception {
        mockMvc.perform(get(BASE + "/" + teleconsultaId + "/resumen-paciente")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/" + teleconsultaId + "/resumen-paciente")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteNombre", notNullValue()));
    }

    @Test
    void laRespuestaNoExponeLaSalaNiDatosDelOtroParticipanteMasDeLoNecesario() throws Exception {
        MvcResult resultado = mockMvc.perform(
                        get(BASE + "/" + teleconsultaId).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andReturn();
        String json = resultado.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(json)
                .doesNotContain("sala\"")
                .doesNotContain("dni")
                .doesNotContain("email");
        org.assertj.core.api.Assertions.assertThat((String) JsonPath.read(json, "$.rol"))
                .isEqualTo("PACIENTE");
    }

    @Test
    void unaCitaVirtualSeReservaConLaTarifaVirtualYCreaSuTeleconsulta() throws Exception {
        Especialidad especialidad = medico.getEspecialidades().iterator().next();
        especialidad.setPrecioVirtual(new BigDecimal("60.00"));
        especialidadRepository.save(especialidad);
        LocalDate lunes =
                LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
        String cuerpo = "{\"medicoId\":\"" + medico.getId() + "\",\"fechaHora\":\"" + lunes.atTime(9, 0)
                + "\",\"motivo\":\"Control virtual\",\"especialidadId\":\"" + especialidad.getId()
                + "\",\"modalidad\":\"VIRTUAL\"}";
        mockMvc.perform(post("/api/v1/citas")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modalidad", is("VIRTUAL")))
                .andExpect(jsonPath("$.monto", is(60.0)));

        mockMvc.perform(get(BASE + "/mias").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void unaCitaSinModalidadSigueSiendoPresencialYNoCreaTeleconsulta() throws Exception {
        Especialidad especialidad = medico.getEspecialidades().iterator().next();
        LocalDate lunes =
                LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(2);
        String cuerpo = "{\"medicoId\":\"" + medico.getId() + "\",\"fechaHora\":\"" + lunes.atTime(9, 0)
                + "\",\"motivo\":\"Control\",\"especialidadId\":\"" + especialidad.getId() + "\"}";
        mockMvc.perform(post("/api/v1/citas")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modalidad", is("PRESENCIAL")))
                .andExpect(jsonPath("$.monto", is(100.0)));
        mockMvc.perform(get(BASE + "/mias").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(jsonPath("$", hasSize(1)));
    }
}

package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class RecepcionSeguridadTest {

    private static final String BASE = "/api/v1/recepcion";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private Cita cita;
    private String tokenRecepcion;
    private String tokenAdmin;
    private String tokenMedico;
    private String tokenPaciente;
    private String tokenFarmacia;
    private String tokenCaja;
    private String tokenEnfermeria;
    private String tokenLaboratorio;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        cita = fixture.cita(medico, paciente, LocalDate.now().atTime(9, 0), EstadoCita.CONFIRMADA);
        tokenRecepcion = token(fixture.usuario("recepcion", RolNombre.ROLE_RECEPCION));
        tokenAdmin = token(fixture.usuario("admin", RolNombre.ROLE_ADMIN));
        tokenMedico = token(medico.getUsuario());
        tokenPaciente = token(paciente.getUsuario());
        tokenFarmacia = token(fixture.usuario("farmacia", RolNombre.ROLE_FARMACIA));
        tokenCaja = token(fixture.usuario("caja", RolNombre.ROLE_CAJA));
        tokenEnfermeria = token(fixture.usuario("enfermeria", RolNombre.ROLE_ENFERMERIA));
        tokenLaboratorio = token(fixture.usuario("lab", RolNombre.ROLE_LABORATORIO));
    }

    private String token(com.codesoftlabs.tusalud.app.entities.Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    private long crearTurno(String tokenDe, long pacienteId, String prioridad) throws Exception {
        MvcResult resultado = mockMvc.perform(post(BASE + "/turnos")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":" + pacienteId + ",\"medicoId\":" + medico.getId()
                                + ",\"prioridad\":\"" + prioridad + "\",\"motivo\":\"Consulta\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn();
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void sinTokenTodoElModuloRetorna401() throws Exception {
        mockMvc.perform(get(BASE + "/citas-hoy")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/cola")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/pantalla")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/resumen")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/pacientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void soloRecepcionYAdminGestionanLasCitasDeHoyLaLlegadaYElResumen() throws Exception {
        for (String token : new String[] {tokenRecepcion, tokenAdmin}) {
            mockMvc.perform(get(BASE + "/citas-hoy").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk());
            mockMvc.perform(get(BASE + "/resumen").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enEspera", notNullValue()));
        }
        for (String token : new String[] {tokenMedico, tokenPaciente, tokenFarmacia, tokenCaja, tokenLaboratorio}) {
            mockMvc.perform(get(BASE + "/citas-hoy").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(BASE + "/resumen").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(BASE + "/citas/" + cita.getId() + "/llegada").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void laLlegadaCreaElTurnoYUnaSegundaLlegadaResponde409() throws Exception {
        mockMvc.perform(post(BASE + "/citas/" + cita.getId() + "/llegada")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.numero", is("A-001")))
                .andExpect(jsonPath("$.estado", is("EN_ESPERA")));
        mockMvc.perform(post(BASE + "/citas/" + cita.getId() + "/llegada")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isConflict());
        mockMvc.perform(get(BASE + "/citas-hoy").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].llego", is(true)))
                .andExpect(jsonPath("$[0].turnoNumero", is("A-001")));
    }

    @Test
    void elFlujoDeColaPorHttpRespetaLosRoles() throws Exception {
        long turno = crearTurno(tokenRecepcion, paciente.getId(), "URGENTE");

        mockMvc.perform(get(BASE + "/cola").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numero", is("U-001")));
        mockMvc.perform(get(BASE + "/cola").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get(BASE + "/cola").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE + "/turnos/" + turno + "/llamar").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/turnos/" + turno + "/llamar").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("LLAMADO")));
        mockMvc.perform(put(BASE + "/turnos/" + turno + "/llamar").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isConflict());

        mockMvc.perform(put(BASE + "/turnos/" + turno + "/iniciar").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/turnos/" + turno + "/iniciar").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("EN_ATENCION")));
        mockMvc.perform(put(BASE + "/turnos/" + turno + "/finalizar").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("ATENDIDO")));
        mockMvc.perform(put(BASE + "/turnos/999999/llamar").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isNotFound());
    }

    @Test
    void ausenteYCancelarSonDeRecepcionAdminYMedico() throws Exception {
        long uno = crearTurno(tokenRecepcion, paciente.getId(), "NORMAL");
        long dos = crearTurno(tokenAdmin, fixture.paciente().getId(), "PREFERENTE");

        mockMvc.perform(put(BASE + "/turnos/" + uno + "/ausente").header(HttpHeaders.AUTHORIZATION, tokenFarmacia))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/turnos/" + uno + "/ausente").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("AUSENTE")));
        mockMvc.perform(put(BASE + "/turnos/" + dos + "/cancelar").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADO")));
    }

    @Test
    void laPantallaEsDeCualquierUsuarioAutenticado() throws Exception {
        long turno = crearTurno(tokenRecepcion, paciente.getId(), "NORMAL");
        mockMvc.perform(put(BASE + "/turnos/" + turno + "/llamar").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/pantalla").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numero", is("A-001")))
                .andExpect(jsonPath("$[0].consultorio", is("Consultorio 1")));
    }

    @Test
    void laBusquedaDePacientesEstaAbiertaALosRolesDeMostradorYCerradaAlResto() throws Exception {
        for (String token :
                new String[] {tokenRecepcion, tokenAdmin, tokenCaja, tokenMedico, tokenFarmacia, tokenEnfermeria}) {
            mockMvc.perform(get(BASE + "/pacientes")
                            .param("q", paciente.getDni())
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].dni", is(paciente.getDni())));
        }
        mockMvc.perform(get(BASE + "/pacientes").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/pacientes").header(HttpHeaders.AUTHORIZATION, tokenLaboratorio))
                .andExpect(status().isForbidden());
    }

    @Test
    void elRegistroRapidoDevuelveLaContrasenaTemporalYSoloLoHacenRecepcionYAdmin() throws Exception {
        String cuerpo =
                "{\"nombre\":\"Lucía\",\"apellido\":\"Paredes\",\"dni\":\"%s\",\"fechaNacimiento\":\"1992-04-15\","
                        + "\"telefono\":\"987654321\"}";
        String dni = String.format("7%07d", ServiceTestFixture.siguiente());

        mockMvc.perform(post(BASE + "/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.formatted(dni)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.formatted(dni)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordTemporal").isString())
                .andExpect(jsonPath("$.paciente.email", is(dni + "@pacientes.tusalud.local")));
        mockMvc.perform(post(BASE + "/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.formatted(dni)))
                .andExpect(status().isConflict());
        mockMvc.perform(post(BASE + "/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.formatted("123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laCitaDeMostradorSeValidaYSoloLaReservanRecepcionYAdmin() throws Exception {
        LocalDate lunes =
                LocalDate.now().plusDays(7 - ((LocalDate.now().getDayOfWeek().getValue() + 6) % 7));
        String cuerpo = "{\"pacienteId\":" + paciente.getId() + ",\"medicoId\":" + medico.getId()
                + ",\"especialidadId\":"
                + medico.getEspecialidades().iterator().next().getId()
                + ",\"fechaHora\":\"" + lunes + "T09:00:00\",\"motivo\":\"Control\"}";

        mockMvc.perform(post(BASE + "/citas")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/citas")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.estado", is("PENDIENTE")));
        mockMvc.perform(post(BASE + "/citas")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isConflict());
        mockMvc.perform(post(BASE + "/citas")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":1}"))
                .andExpect(status().isBadRequest());
    }
}

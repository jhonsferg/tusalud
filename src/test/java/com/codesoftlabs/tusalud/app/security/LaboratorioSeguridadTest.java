package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class LaboratorioSeguridadTest {

    private static final String BASE = "/api/v1/laboratorio";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private String tokenMedico;
    private String tokenPaciente;
    private String tokenLaboratorio;
    private String tokenAdmin;
    private String tokenFarmacia;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        fixture.cita(medico, paciente, LocalDateTime.now().minusDays(1), EstadoCita.ATENDIDA);
        tokenMedico = token(UsuarioPrincipal.from(medico.getUsuario()));
        tokenPaciente = token(UsuarioPrincipal.from(paciente.getUsuario()));
        tokenLaboratorio = token(UsuarioPrincipal.from(fixture.usuario("lab", RolNombre.ROLE_LABORATORIO)));
        tokenAdmin = token(UsuarioPrincipal.from(fixture.usuario("admin", RolNombre.ROLE_ADMIN)));
        tokenFarmacia = token(UsuarioPrincipal.from(fixture.usuario("farmacia", RolNombre.ROLE_FARMACIA)));
    }

    private String token(UsuarioPrincipal principal) {
        return "Bearer " + jwtUtils.generateToken(principal);
    }

    private static String cuerpoExamen(String codigo) {
        return "{\"codigo\":\"" + codigo + "\",\"nombre\":\"Hemoglobina de prueba\",\"categoria\":\"HEMATOLOGIA\","
                + "\"muestra\":\"SANGRE\",\"precio\":20.00,\"tiempoEntregaHoras\":4,"
                + "\"parametros\":[{\"nombre\":\"Hemoglobina\",\"unidad\":\"g/dL\",\"tipo\":\"NUMERICO\","
                + "\"valorMin\":12,\"valorMax\":17.5}]}";
    }

    private UUID crearExamen(String codigo) throws Exception {
        MvcResult resultado = mockMvc.perform(post(BASE + "/examenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenLaboratorio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoExamen(codigo)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
    }

    @Test
    void sinTokenTodoElModuloRetorna401() throws Exception {
        mockMvc.perform(get(BASE + "/examenes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/ordenes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/mis-ordenes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/resumen")).andExpect(status().isUnauthorized());
    }

    @Test
    void elPacienteSoloAccedeASusOrdenes() throws Exception {
        mockMvc.perform(get(BASE + "/mis-ordenes").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get(BASE + "/examenes").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/ordenes").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/resumen").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        post(BASE + "/ordenes")
                                .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"pacienteId\":\"00000000-0000-0000-0000-000000000001\",\"prioridad\":\"NORMAL\",\"examenIds\":[\"00000000-0000-0000-0000-000000000001\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elMedicoConsultaElCatalogoYSolicitaPeroNoGestionaElLaboratorio() throws Exception {
        mockMvc.perform(get(BASE + "/examenes").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/examenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoExamen("HTTP-MED")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/mis-ordenes").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/ordenes/00000000-0000-0000-0000-000000000001/muestra")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/ordenes/00000000-0000-0000-0000-000000000001/validar")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/resumen").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isForbidden());
    }

    @Test
    void elLaboratorioGestionaElCatalogoPeroNoSolicitaOrdenes() throws Exception {
        mockMvc.perform(post(BASE + "/examenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenLaboratorio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoExamen("HTTP-LAB")))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString(BASE + "/examenes/")))
                .andExpect(jsonPath("$.codigo", is("HTTP-LAB")))
                .andExpect(jsonPath("$.parametros", hasSize(1)))
                .andExpect(jsonPath("$.activo", is(true)));
        mockMvc.perform(post(BASE + "/examenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenLaboratorio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoExamen("http-lab")))
                .andExpect(status().isConflict());
        mockMvc.perform(get(BASE + "/examenes")
                        .param("q", "http-lab")
                        .header(HttpHeaders.AUTHORIZATION, tokenLaboratorio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("HTTP-LAB")));
        mockMvc.perform(get(BASE + "/resumen").header(HttpHeaders.AUTHORIZATION, tokenLaboratorio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientes", notNullValue()));
        mockMvc.perform(
                        post(BASE + "/ordenes")
                                .header(HttpHeaders.AUTHORIZATION, tokenLaboratorio)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"pacienteId\":\"00000000-0000-0000-0000-000000000001\",\"prioridad\":\"NORMAL\",\"examenIds\":[\"00000000-0000-0000-0000-000000000001\"]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/mis-ordenes").header(HttpHeaders.AUTHORIZATION, tokenLaboratorio))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorEditaElCatalogoYVeLaColaPeroNoProcesaMuestras() throws Exception {
        mockMvc.perform(post(BASE + "/examenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoExamen("HTTP-ADM")))
                .andExpect(status().isCreated());
        mockMvc.perform(get(BASE + "/resumen").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/ordenes").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(put(BASE + "/ordenes/00000000-0000-0000-0000-000000000001/muestra")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    void otrosRolesDePersonalNoAccedenAlLaboratorio() throws Exception {
        mockMvc.perform(get(BASE + "/examenes").header(HttpHeaders.AUTHORIZATION, tokenFarmacia))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/ordenes").header(HttpHeaders.AUTHORIZATION, tokenFarmacia))
                .andExpect(status().isForbidden());
    }

    @Test
    void validaLosDatosDeEntradaYLosRecursosInexistentes() throws Exception {
        mockMvc.perform(post(BASE + "/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + paciente.getId()
                                + "\",\"prioridad\":\"NORMAL\",\"examenIds\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/examenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenLaboratorio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/ordenes/00000000-0000-0000-0000-000000999999")
                        .header(HttpHeaders.AUTHORIZATION, tokenLaboratorio))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/examenes/00000000-0000-0000-0000-000000999999")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isNotFound());
    }

    @Test
    void recorreElFlujoCompletoPorHttp() throws Exception {
        UUID examenId = crearExamen("HTTP-FLU");
        MvcResult creada = mockMvc.perform(post(BASE + "/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + paciente.getId() + "\",\"prioridad\":\"URGENTE\","
                                + "\"indicaciones\":\"Control\",\"examenIds\":[\"" + examenId + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString(BASE + "/ordenes/")))
                .andExpect(jsonPath("$.estado", is("SOLICITADA")))
                .andExpect(jsonPath("$.codigo", containsString("LAB-")))
                .andReturn();
        String json = creada.getResponse().getContentAsString();
        UUID ordenId = UUID.fromString(JsonPath.read(json, "$.id"));
        UUID itemId = UUID.fromString(JsonPath.read(json, "$.items[0].id"));
        UUID parametroId = UUID.fromString(JsonPath.read(
                mockMvc.perform(get(BASE + "/examenes/" + examenId).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.parametros[0].id"));

        mockMvc.perform(get(BASE + "/ordenes/" + ordenId).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/ordenes/" + ordenId + "/muestra")
                        .header(HttpHeaders.AUTHORIZATION, tokenLaboratorio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("MUESTRA_TOMADA")));
        mockMvc.perform(put(BASE + "/ordenes/" + ordenId + "/items/" + itemId + "/resultados")
                        .header(HttpHeaders.AUTHORIZATION, tokenLaboratorio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultados\":[{\"parametroId\":\"" + parametroId + "\",\"valor\":\"10.2\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("EN_PROCESO")))
                .andExpect(jsonPath("$.items[0].resultados[0].fueraDeRango", is(true)));
        mockMvc.perform(put(BASE + "/ordenes/" + ordenId + "/items/" + itemId + "/resultados")
                        .header(HttpHeaders.AUTHORIZATION, tokenLaboratorio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultados\":[{\"parametroId\":\"" + parametroId + "\",\"valor\":\"no\"}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(BASE + "/ordenes/" + ordenId + "/validar")
                        .header(HttpHeaders.AUTHORIZATION, tokenLaboratorio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("COMPLETADA")));

        mockMvc.perform(get(BASE + "/ordenes/" + ordenId).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].resultados[0].valor", is("10.2")));
        mockMvc.perform(get(BASE + "/mis-ordenes").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].items[0].resultados", hasSize(1)));
        mockMvc.perform(get(BASE + "/ordenes")
                        .param("estado", "COMPLETADA")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(ordenId.toString())));
        mockMvc.perform(put(BASE + "/ordenes/" + ordenId + "/cancelar").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isConflict());
    }

    @Test
    void elMedicoCancelaSinCuerpoUnaOrdenSolicitada() throws Exception {
        UUID examenId = crearExamen("HTTP-CAN");
        MvcResult creada = mockMvc.perform(post(BASE + "/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + paciente.getId() + "\",\"prioridad\":\"NORMAL\",\"examenIds\":["
                                + "\"" + examenId + "\"]}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID ordenId = UUID.fromString(JsonPath.read(creada.getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(put(BASE + "/ordenes/" + ordenId + "/cancelar").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADA")));
    }
}

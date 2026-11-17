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
class HospitalizacionSeguridadTest {

    private static final String BASE = "/api/v1/hospitalizacion";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private Paciente otroPaciente;
    private String tokenMedico;
    private String tokenPaciente;
    private String tokenOtroPaciente;
    private String tokenEnfermeria;
    private String tokenRecepcion;
    private String tokenAdmin;
    private String tokenFarmacia;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        otroPaciente = fixture.paciente();
        tokenMedico = token(UsuarioPrincipal.from(medico.getUsuario()));
        tokenPaciente = token(UsuarioPrincipal.from(paciente.getUsuario()));
        tokenOtroPaciente = token(UsuarioPrincipal.from(otroPaciente.getUsuario()));
        tokenEnfermeria = token(UsuarioPrincipal.from(fixture.usuario("enf", RolNombre.ROLE_ENFERMERIA)));
        tokenRecepcion = token(UsuarioPrincipal.from(fixture.usuario("recep", RolNombre.ROLE_RECEPCION)));
        tokenAdmin = token(UsuarioPrincipal.from(fixture.usuario("admin", RolNombre.ROLE_ADMIN)));
        tokenFarmacia = token(UsuarioPrincipal.from(fixture.usuario("farm", RolNombre.ROLE_FARMACIA)));
    }

    private String token(UsuarioPrincipal principal) {
        return "Bearer " + jwtUtils.generateToken(principal);
    }

    private static String cuerpoCama(String codigo, String area) {
        return "{\"codigo\":\"" + codigo + "\",\"area\":\"" + area + "\",\"piso\":2,"
                + "\"descripcion\":\"Cama de prueba\",\"precioDia\":150.00,\"activa\":true}";
    }

    private UUID crearCama(String codigo, String area) throws Exception {
        MvcResult resultado = mockMvc.perform(post(BASE + "/camas")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCama(codigo, area)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
    }

    private UUID crearIngreso(UUID camaId, Paciente destino) throws Exception {
        fixture.cita(medico, destino, LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
        MvcResult resultado = mockMvc.perform(post(BASE + "/ingresos")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + destino.getId() + "\",\"camaId\":\"" + camaId + "\""
                                + ",\"motivo\":\"Control postoperatorio\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        return UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
    }

    private static String triaje(int nivel, Paciente destino) {
        return "{\"pacienteId\":\"" + destino.getId() + "\",\"triaje\":{\"nivel\":" + nivel
                + ",\"presionArterial\":\"140/90\",\"frecuenciaCardiaca\":100,\"motivoConsulta\":\"Dolor abdominal\"}}";
    }

    @Test
    void sinTokenTodosLosEndpointsResponden401() throws Exception {
        mockMvc.perform(get(BASE + "/camas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/mapa")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/emergencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(triaje(2, paciente)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elPacienteYLaFarmaciaNoAccedenAlAreaClinica() throws Exception {
        for (String token : new String[] {tokenPaciente, tokenFarmacia}) {
            mockMvc.perform(get(BASE + "/camas").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(BASE + "/mapa").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(BASE + "/emergencias").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(BASE + "/emergencias")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(triaje(2, paciente)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(BASE + "/mis-ingresos").header(HttpHeaders.AUTHORIZATION, tokenFarmacia))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloElAdministradorGestionaCamasYLaEnfermeriaCambiaSuEstado() throws Exception {
        String codigo = "HTTP-" + ServiceTestFixture.siguiente();
        mockMvc.perform(post(BASE + "/camas")
                        .header(HttpHeaders.AUTHORIZATION, tokenEnfermeria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCama(codigo, "UCI")))
                .andExpect(status().isForbidden());
        UUID id = crearCama(codigo, "UCI");
        mockMvc.perform(post(BASE + "/camas")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCama(codigo, "UCI")))
                .andExpect(status().isConflict());
        mockMvc.perform(put(BASE + "/camas/" + id)
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCama(codigo, "OBSERVACION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.area", is("OBSERVACION")));
        mockMvc.perform(put(BASE + "/camas/" + id + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"MANTENIMIENTO\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/camas/" + id + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, tokenEnfermeria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"MANTENIMIENTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("MANTENIMIENTO")));
        mockMvc.perform(get(BASE + "/camas?area=OBSERVACION&estado=MANTENIMIENTO")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')]", hasSize(1)));
    }

    @Test
    void laRecepcionVeElMapaPeroNoRegistraTriajeNiIngresaNiDaAltaNiVeDatosClinicos() throws Exception {
        mockMvc.perform(get(BASE + "/mapa").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areas", hasSize(4)));
        mockMvc.perform(get(BASE + "/resumen").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupacionPorcentaje", notNullValue()));
        mockMvc.perform(post(BASE + "/emergencias")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(triaje(3, paciente)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/emergencias")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(triaje(3, paciente)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/emergencias")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(triaje(3, paciente)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/emergencias")
                        .header(HttpHeaders.AUTHORIZATION, tokenEnfermeria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(triaje(3, paciente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo", is("EMERGENCIA")))
                .andExpect(jsonPath("$.triaje.clasificacion", is("AMARILLO")))
                .andExpect(jsonPath("$.triaje.motivoConsulta", is("Dolor abdominal")));
        mockMvc.perform(get(BASE + "/emergencias").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].esperaMinutos", notNullValue()))
                .andExpect(jsonPath("$[0].triaje.nivel", is(3)))
                .andExpect(jsonPath("$[0].triaje.motivoConsulta").doesNotExist())
                .andExpect(jsonPath("$[0].triaje.presionArterial").doesNotExist())
                .andExpect(jsonPath("$[0].motivo").doesNotExist());
        mockMvc.perform(post(BASE + "/ingresos")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + otroPaciente.getId()
                                + "\",\"camaId\":\"00000000-0000-0000-0000-000000000001\",\"motivo\":\"X\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/ingresos/00000000-0000-0000-0000-000000000001/alta")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosticoAlta\":\"A\",\"indicacionesAlta\":\"B\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validaElTriajeYElIngreso() throws Exception {
        mockMvc.perform(post(BASE + "/emergencias")
                        .header(HttpHeaders.AUTHORIZATION, tokenEnfermeria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(triaje(6, paciente)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/emergencias")
                        .header(HttpHeaders.AUTHORIZATION, tokenEnfermeria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + paciente.getId() + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/ingresos")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + paciente.getId()
                                + "\",\"camaId\":\"00000000-0000-0000-0000-000000000001\",\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/ingresos/00000000-0000-0000-0000-000000999999")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isNotFound());
    }

    @Test
    void flujoCompletoDeIngresoEvolucionTrasladoYAltaPorRol() throws Exception {
        UUID origen = crearCama("FL-" + ServiceTestFixture.siguiente(), "HOSPITALIZACION");
        UUID destino = crearCama("FL-" + ServiceTestFixture.siguiente(), "UCI");
        UUID ingreso = crearIngreso(origen, paciente);

        mockMvc.perform(post(BASE + "/ingresos")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + paciente.getId() + "\",\"camaId\":\"" + destino + "\""
                                + ",\"motivo\":\"Otro ingreso\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post(BASE + "/ingresos/" + ingreso + "/evoluciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenEnfermeria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENFERMERIA\",\"nota\":\"Signos estables\",\"saturacionO2\":97}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post(BASE + "/ingresos/" + ingreso + "/evoluciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"MEDICA\",\"nota\":\"Evolución favorable\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post(BASE + "/ingresos/" + ingreso + "/evoluciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"MEDICA\",\"nota\":\"No corresponde\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/ingresos/" + ingreso + "/evoluciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENFERMERIA\",\"nota\":\"No corresponde\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE + "/ingresos/" + ingreso + "/evoluciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenEnfermeria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get(BASE + "/ingresos/" + ingreso + "/evoluciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipo", is("MEDICA")));
        mockMvc.perform(get(BASE + "/ingresos/" + ingreso).header(HttpHeaders.AUTHORIZATION, tokenOtroPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/mis-ingresos").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(ingreso.toString())));
        mockMvc.perform(get(BASE + "/mis-ingresos").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE + "/ingresos/" + ingreso + "/trasladar")
                        .header(HttpHeaders.AUTHORIZATION, tokenEnfermeria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"camaId\":\"" + destino + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.camaId", is(destino.toString())));
        mockMvc.perform(put(BASE + "/ingresos/" + ingreso + "/cancelar").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isConflict());

        String alta = "{\"diagnosticoAlta\":\"Recuperado\",\"indicacionesAlta\":\"Control en una semana\"}";
        mockMvc.perform(put(BASE + "/ingresos/" + ingreso + "/alta")
                        .header(HttpHeaders.AUTHORIZATION, tokenEnfermeria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/ingresos/" + ingreso + "/alta")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("ALTA")))
                .andExpect(jsonPath("$.cargoCama", notNullValue()));
        mockMvc.perform(get(BASE + "/ingresos?estado=ALTA&q=" + paciente.getDni())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void laEnfermeriaAsignaCamaALaEmergenciaYElAdministradorNoPuede() throws Exception {
        UUID box = crearCama("EM-" + ServiceTestFixture.siguiente(), "EMERGENCIA");
        MvcResult resultado = mockMvc.perform(post(BASE + "/emergencias")
                        .header(HttpHeaders.AUTHORIZATION, tokenEnfermeria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(triaje(1, otroPaciente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.triaje.clasificacion", is("ROJO")))
                .andReturn();
        UUID id = UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(put(BASE + "/ingresos/" + id + "/asignar-cama")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"camaId\":\"" + box + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/ingresos/" + id + "/asignar-cama")
                        .header(HttpHeaders.AUTHORIZATION, tokenEnfermeria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"camaId\":\"" + box + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.camaId", is(box.toString())));
    }
}

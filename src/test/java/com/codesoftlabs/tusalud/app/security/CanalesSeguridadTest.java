package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.canales.FirmasCanales;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class CanalesSeguridadTest {

    private static final String RESPALDO =
            "{\"tipo\":\"RESPALDO\",\"estado\":\"EXITOSO\",\"alcance\":\"COMPLETO\",\"categoria\":\"DIARIO\","
                    + "\"iniciadoEn\":\"2026-01-01T02:00:00\",\"finalizadoEn\":\"2026-01-01T02:01:00\","
                    + "\"bytesTotal\":2048,\"archivo\":\"x.dump.enc\",\"checksum\":\"abc\",\"copiaRemota\":false}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private Usuario pacienteUsuario;
    private String paciente;
    private String medico;
    private String admin;
    private String operaciones;
    private String recepcion;

    @BeforeEach
    void setUp() {
        pacienteUsuario = fixture.paciente().getUsuario();
        paciente = token(pacienteUsuario);
        medico = token(fixture.usuario("cn-med", RolNombre.ROLE_MEDICO));
        admin = token(fixture.usuario("cn-adm", RolNombre.ROLE_ADMIN));
        operaciones = token(fixture.usuario("cn-ops", RolNombre.ROLE_OPERACIONES));
        recepcion = token(fixture.usuario("cn-rec", RolNombre.ROLE_RECEPCION));
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    @Test
    void sinTokenLosEndpointsPrivadosRetornan401() throws Exception {
        mockMvc.perform(get("/api/v1/canales/mis-canales")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/canales/preferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/canales/entregas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/respaldos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/respaldos/estado")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/canales/plantillas")).andExpect(status().isUnauthorized());
    }

    @Test
    void todosLosUsuariosAutenticadosGestionanSusPropiosCanales() throws Exception {
        for (String actor : new String[] {paciente, medico, admin, operaciones, recepcion}) {
            mockMvc.perform(get("/api/v1/canales/mis-canales").header("Authorization", actor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canales.length()").value(3));
        }
    }

    @Test
    void elCorreoDeLaCuentaSeMuestraEnmascarado() throws Exception {
        mockMvc.perform(get("/api/v1/canales/mis-canales").header("Authorization", paciente))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(pacienteUsuario.getEmail()))))
                .andExpect(jsonPath("$.canales[0].destinoFijo").value(true));
    }

    @Test
    void elFlujoDeActivacionRequiereConsentimientoYValidaElCodigo() throws Exception {
        mockMvc.perform(post("/api/v1/canales/contactos/CORREO/solicitar")
                        .header("Authorization", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentimiento\":false}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/canales/contactos/CORREO/solicitar")
                        .header("Authorization", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentimiento\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_VERIFICACION"));
        mockMvc.perform(post("/api/v1/canales/contactos/CORREO/verificar")
                        .header("Authorization", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"12ab\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/canales/contactos/NOEXISTE/solicitar")
                        .header("Authorization", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentimiento\":true}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/v1/canales/contactos/CORREO").header("Authorization", paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BAJA"));
    }

    @Test
    void lasAlertasDeSeguridadNoSeDesactivanPorLaApi() throws Exception {
        mockMvc.perform(
                        put("/api/v1/canales/preferencias")
                                .header("Authorization", paciente)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"preferencias\":[{\"tipo\":\"SESION_SOSPECHOSA\",\"canal\":\"CORREO\",\"activo\":false}]}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/canales/preferencias")
                        .header("Authorization", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferencias\":[{\"tipo\":\"STOCK_BAJO\",\"canal\":\"CORREO\",\"activo\":true}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/canales/preferencias")
                        .header("Authorization", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferencias\":[{\"tipo\":\"CITA_CREADA\",\"canal\":\"CORREO\",\"activo\":false}],"
                                + "\"silencio\":{\"activo\":true,\"desde\":\"21:30\",\"hasta\":\"06:30\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.silencio.desde").value("21:30:00"));
    }

    @Test
    void laBitacoraDeEntregasSoloLaVenAdminYOperaciones() throws Exception {
        for (String sinAcceso : new String[] {paciente, medico, recepcion}) {
            mockMvc.perform(get("/api/v1/canales/entregas").header("Authorization", sinAcceso))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/canales/entregas/resumen").header("Authorization", sinAcceso))
                    .andExpect(status().isForbidden());
        }
        for (String conAcceso : new String[] {admin, operaciones}) {
            mockMvc.perform(get("/api/v1/canales/entregas").header("Authorization", conAcceso))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/canales/entregas/resumen").header("Authorization", conAcceso))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/canales/entregas/" + java.util.UUID.randomUUID() + "/reintentar")
                        .header("Authorization", operaciones))
                .andExpect(status().isForbidden());
    }

    @Test
    void lasPlantillasYLaSimulacionSonSoloDeAdministracion() throws Exception {
        for (String sinAcceso : new String[] {paciente, medico, operaciones, recepcion}) {
            mockMvc.perform(get("/api/v1/admin/canales/plantillas").header("Authorization", sinAcceso))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/admin/canales/simulacion").header("Authorization", sinAcceso))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/v1/admin/canales/plantillas/CITA_CREADA/CORREO")
                            .header("Authorization", sinAcceso)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"asunto\":\"a\",\"cuerpo\":\"b\",\"motivo\":\"motivo valido\"}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/admin/canales/plantillas").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variables.length()").value(3));
        mockMvc.perform(
                        put("/api/v1/admin/canales/plantillas/CITA_CREADA/CORREO")
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"asunto\":\"Cita\",\"cuerpo\":\"Su diagnostico es {{diagnostico}}\",\"motivo\":\"motivo valido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elHistorialDeRespaldosSoloLoVenAdminYOperaciones() throws Exception {
        for (String sinAcceso : new String[] {paciente, medico, recepcion}) {
            mockMvc.perform(get("/api/v1/respaldos").header("Authorization", sinAcceso))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/respaldos/estado").header("Authorization", sinAcceso))
                    .andExpect(status().isForbidden());
        }
        for (String conAcceso : new String[] {admin, operaciones}) {
            mockMvc.perform(get("/api/v1/respaldos/estado").header("Authorization", conAcceso))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rpoObjetivoHoras").value(24));
        }
    }

    @Test
    void elRegistroDeRespaldosSoloAceptaElTokenCompartido() throws Exception {
        mockMvc.perform(post("/api/v1/respaldos/ejecuciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESPALDO))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/respaldos/ejecuciones")
                        .header("X-Respaldo-Token", "token-incorrecto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESPALDO))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/respaldos/ejecuciones")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESPALDO))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/respaldos/ejecuciones")
                        .header("X-Respaldo-Token", "token-de-pruebas-de-respaldo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESPALDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracionSeg").value(60));
        mockMvc.perform(post("/api/v1/respaldos/ejecuciones")
                        .header("X-Respaldo-Token", "token-de-pruebas-de-respaldo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"RESPALDO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laBajaPorEnlaceEsPublicaPeroSoloAceptaTokensFirmados() throws Exception {
        mockMvc.perform(post("/api/v1/canales/baja").param("t", "basura")).andExpect(status().isBadRequest());
        String token = FirmasCanales.crearTokenBaja(
                "secreto-de-desarrollo-para-enlaces-de-baja", pacienteUsuario.getId(), Canal.SMS);
        mockMvc.perform(post("/api/v1/canales/baja").param("t", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canal").value("SMS"));
    }

    @Test
    void elWebhookSinFirmaValidaSeRechazaSinDetalles() throws Exception {
        mockMvc.perform(post("/api/v1/canales/webhook/CORREO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idExterno\":\"x\",\"estado\":\"ENTREGADO\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/canales/webhook/CORREO")
                        .header("X-Marca-Tiempo", String.valueOf(System.currentTimeMillis() / 1000))
                        .header("X-Firma", "sha256=deadbeef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idExterno\":\"x\",\"estado\":\"ENTREGADO\"}"))
                .andExpect(status().isForbidden());
    }
}

package com.codesoftlabs.tusalud.app.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class SegurosSeguridadTest {

    private static final String ASEGURADORA =
            "{\"codigo\":\"SEG1\",\"nombre\":\"Aseguradora\",\"tipo\":\"PRIVADO\",\"ruc\":\"20123456789\"}";

    private static final String POLIZA =
            "{\"pacienteId\":\"PACIENTE\",\"convenioId\":\"11111111-1111-4111-8111-111111111111\","
                    + "\"numero\":\"X-1\",\"titularNombre\":\"Titular\",\"parentesco\":\"TITULAR\","
                    + "\"vigenciaDesde\":\"2026-01-01\",\"vigenciaHasta\":\"2026-12-31\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private String admin;
    private String finanzas;
    private String recepcion;
    private String caja;
    private String gerencia;
    private String medico;
    private String paciente;
    private String farmacia;
    private Paciente pacienteEntidad;

    @BeforeEach
    void setUp() {
        admin = token(fixture.usuario("sg-adm", RolNombre.ROLE_ADMIN));
        finanzas = token(fixture.usuario("sg-fin", RolNombre.ROLE_FINANZAS));
        recepcion = token(fixture.usuario("sg-rec", RolNombre.ROLE_RECEPCION));
        caja = token(fixture.usuario("sg-caj", RolNombre.ROLE_CAJA));
        gerencia = token(fixture.usuario("sg-ger", RolNombre.ROLE_GERENCIA));
        medico = token(fixture.usuario("sg-med", RolNombre.ROLE_MEDICO));
        farmacia = token(fixture.usuario("sg-far", RolNombre.ROLE_FARMACIA));
        pacienteEntidad = fixture.paciente();
        paciente = token(pacienteEntidad.getUsuario());
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    @Test
    void sinTokenTodoRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/seguros/aseguradoras")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/seguros/mis-polizas")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/seguros/preautorizaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elCatalogoSoloLoEscribenAdminYFinanzasYLoLeenLosRolesDeCobro() throws Exception {
        for (String token : new String[] {admin, finanzas}) {
            mockMvc.perform(post("/api/v1/seguros/aseguradoras")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ASEGURADORA.replace("SEG1", "SEG" + Math.abs(token.hashCode() % 1000))))
                    .andExpect(status().isCreated());
        }
        for (String token : new String[] {recepcion, caja, gerencia, medico, paciente, farmacia}) {
            mockMvc.perform(post("/api/v1/seguros/aseguradoras")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ASEGURADORA))
                    .andExpect(status().isForbidden());
        }
        for (String token : new String[] {admin, finanzas, recepcion, caja, gerencia}) {
            mockMvc.perform(get("/api/v1/seguros/aseguradoras").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/seguros/convenios").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk());
        }
        for (String token : new String[] {medico, paciente, farmacia}) {
            mockMvc.perform(get("/api/v1/seguros/aseguradoras").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/seguros/convenios").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void laCarteraYLasLiquidacionesSonDeFinanzasYLecturaParaGerenciaYAdmin() throws Exception {
        for (String token : new String[] {finanzas, gerencia, admin}) {
            mockMvc.perform(get("/api/v1/seguros/cartera").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/seguros/liquidaciones").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk());
        }
        for (String token : new String[] {recepcion, caja, medico, paciente}) {
            mockMvc.perform(get("/api/v1/seguros/cartera").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/seguros/liquidaciones").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        String cuerpo =
                "{\"aseguradoraId\":\"" + UUID.randomUUID() + "\",\"desde\":\"2026-01-01\",\"hasta\":\"2026-01-31\"}";
        for (String token : new String[] {gerencia, admin, recepcion, caja}) {
            mockMvc.perform(post("/api/v1/seguros/liquidaciones")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/v1/seguros/liquidaciones/" + UUID.randomUUID() + "/enviar")
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/v1/seguros/liquidaciones")
                        .header(HttpHeaders.AUTHORIZATION, finanzas)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isNotFound());
    }

    @Test
    void lasPolizasLasRegistraRecepcionYElPacienteSoloVeLasSuyasSinDatosDeOtros() throws Exception {
        for (String token : new String[] {medico, paciente, finanzas, gerencia}) {
            mockMvc.perform(get("/api/v1/seguros/polizas").param("q", "abc").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/seguros/polizas")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(POLIZA.replace(
                                    "PACIENTE", pacienteEntidad.getId().toString())))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/seguros/polizas").param("q", "a").header(HttpHeaders.AUTHORIZATION, recepcion))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/seguros/mis-polizas").header(HttpHeaders.AUTHORIZATION, paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        for (String token : new String[] {recepcion, caja, medico, finanzas}) {
            mockMvc.perform(get("/api/v1/seguros/mis-polizas").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void laCoberturaDelPacienteSoloLaConsultaUnMedicoConRelacion() throws Exception {
        String ruta = "/api/v1/seguros/cobertura/pacientes/" + pacienteEntidad.getId();
        mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, medico)).andExpect(status().isForbidden());
        for (String token : new String[] {paciente, recepcion, caja, finanzas, admin}) {
            mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
        }
    }

    @Test
    void lasPreautorizacionesLasPideElMedicoYLasGestionanFinanzasYRecepcion() throws Exception {
        String cuerpo = "{\"pacienteId\":\"" + pacienteEntidad.getId()
                + "\",\"categoria\":\"IMAGEN\",\"descripcion\":\"Resonancia\",\"montoEstimado\":500}";
        for (String token : new String[] {paciente, recepcion, finanzas, caja, admin}) {
            mockMvc.perform(post("/api/v1/seguros/preautorizaciones")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/v1/seguros/preautorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, medico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isForbidden());
        for (String token : new String[] {medico, paciente, caja, gerencia}) {
            mockMvc.perform(get("/api/v1/seguros/preautorizaciones").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        for (String token : new String[] {finanzas, recepcion, admin}) {
            mockMvc.perform(get("/api/v1/seguros/preautorizaciones").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk());
        }
        for (String token : new String[] {medico, paciente}) {
            mockMvc.perform(get("/api/v1/seguros/preautorizaciones/mias").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk());
        }
        String aprobacion = "{\"montoAprobado\":100}";
        for (String token : new String[] {medico, paciente, caja, admin, gerencia}) {
            mockMvc.perform(put("/api/v1/seguros/preautorizaciones/" + UUID.randomUUID() + "/aprobar")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(aprobacion))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(put("/api/v1/seguros/preautorizaciones/" + UUID.randomUUID() + "/aprobar")
                        .header(HttpHeaders.AUTHORIZATION, finanzas)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aprobacion))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/seguros/preautorizaciones/" + UUID.randomUUID() + "/rechazar")
                        .header(HttpHeaders.AUTHORIZATION, recepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"no\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elDesgloseDeCajaSoloLoConsultaElCajero() throws Exception {
        String cuerpo = "{\"origen\":\"CITA\",\"referenciaId\":\"" + UUID.randomUUID() + "\"}";
        for (String token : new String[] {admin, recepcion, finanzas, medico, paciente}) {
            mockMvc.perform(post("/api/v1/caja/cobros/desglose")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/v1/caja/cobros/desglose")
                        .header(HttpHeaders.AUTHORIZATION, caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isNotFound());
    }

    @Test
    void elResumenLoVenLosRolesDeSegurosYNoElPaciente() throws Exception {
        for (String token : new String[] {finanzas, gerencia, admin, recepcion}) {
            mockMvc.perform(get("/api/v1/seguros/resumen").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk());
        }
        for (String token : new String[] {medico, paciente, caja, farmacia}) {
            mockMvc.perform(get("/api/v1/seguros/resumen").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/seguros/resumen").header(HttpHeaders.AUTHORIZATION, recepcion))
                .andExpect(jsonPath("$.carteraPendiente").doesNotExist())
                .andExpect(jsonPath("$.polizasPorVencer").exists());
        mockMvc.perform(get("/api/v1/seguros/resumen").header(HttpHeaders.AUTHORIZATION, finanzas))
                .andExpect(jsonPath("$.carteraPendiente").exists());
    }

    @Test
    void unIdQueNoEsUuidResponde400() throws Exception {
        mockMvc.perform(get("/api/v1/seguros/convenios/abc").header(HttpHeaders.AUTHORIZATION, finanzas))
                .andExpect(status().isBadRequest());
    }
}

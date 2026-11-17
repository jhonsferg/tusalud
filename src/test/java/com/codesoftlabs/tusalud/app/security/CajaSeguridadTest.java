package com.codesoftlabs.tusalud.app.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.jayway.jsonpath.JsonPath;
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
class CajaSeguridadTest {

    private static final String COBRO =
            "{\"origen\":\"SERVICIO\",\"tipoComprobante\":\"BOLETA\",\"clienteTipoDoc\":\"SIN_DOC\","
                    + "\"clienteNombre\":\"Cliente de mostrador\",\"metodoPago\":\"EFECTIVO\","
                    + "\"items\":[{\"descripcion\":\"Curación\",\"cantidad\":2,\"precioUnitario\":25.00}]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private String caja;
    private String admin;
    private String paciente;
    private String farmacia;

    @BeforeEach
    void setUp() {
        caja = token(fixture.usuario("seg-caja", RolNombre.ROLE_CAJA));
        admin = token(fixture.usuario("seg-adm", RolNombre.ROLE_ADMIN));
        paciente = token(fixture.usuario("seg-pac", RolNombre.ROLE_PACIENTE));
        farmacia = token(fixture.usuario("seg-far", RolNombre.ROLE_FARMACIA));
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    @Test
    void sinTokenTodoRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/caja/actual")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/caja/comprobantes")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/caja/cobros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COBRO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pacientesYOtrosRolesNoAccedenALaCaja() throws Exception {
        for (String token : new String[] {paciente, farmacia}) {
            mockMvc.perform(get("/api/v1/caja/actual").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/caja/comprobantes").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/caja/pendientes").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/caja/reportes/ingresos").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/caja/aperturas")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"montoApertura\":10}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void elAdministradorSoloConsultaYNoOperaLaCaja() throws Exception {
        mockMvc.perform(get("/api/v1/caja/comprobantes").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/caja/pendientes").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/caja/historial").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/caja/reportes/ingresos").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(post("/api/v1/caja/aperturas")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoApertura\":10}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/caja/cobros")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COBRO))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/caja/resumen").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCajeroCompletaElFlujoDeAperturaCobroAnulacionYCierre() throws Exception {
        mockMvc.perform(get("/api/v1/caja/actual").header(HttpHeaders.AUTHORIZATION, caja))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/caja/cobros")
                        .header(HttpHeaders.AUTHORIZATION, caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COBRO))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/caja/aperturas")
                        .header(HttpHeaders.AUTHORIZATION, caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoApertura\":-1}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/caja/aperturas")
                        .header(HttpHeaders.AUTHORIZATION, caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoApertura\":100}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.estado").value("ABIERTA"));
        mockMvc.perform(post("/api/v1/caja/aperturas")
                        .header(HttpHeaders.AUTHORIZATION, caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoApertura\":5}"))
                .andExpect(status().isConflict());

        String respuesta = mockMvc.perform(post("/api/v1/caja/cobros")
                        .header(HttpHeaders.AUTHORIZATION, caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COBRO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.codigo").value(org.hamcrest.Matchers.matchesPattern("B001-\\d{6}")))
                .andExpect(jsonPath("$.total").value(50.0))
                .andExpect(jsonPath("$.subtotal").value(42.37))
                .andExpect(jsonPath("$.igv").value(7.63))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = JsonPath.read(respuesta, "$.id");

        mockMvc.perform(post("/api/v1/caja/cobros")
                        .header(HttpHeaders.AUTHORIZATION, caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COBRO.replace("BOLETA", "FACTURA")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/caja/cobros")
                        .header(HttpHeaders.AUTHORIZATION, caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COBRO.replaceAll("\"items\":.*\\}\\]", "\"items\":[]")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/caja/resumen").header(HttpHeaders.AUTHORIZATION, caja))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comprobantes").value(1))
                .andExpect(jsonPath("$.efectivoEsperado").value(150.0));
        mockMvc.perform(get("/api/v1/caja/comprobantes/" + id).header(HttpHeaders.AUTHORIZATION, caja))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].descripcion").value("Curación"));

        mockMvc.perform(put("/api/v1/caja/comprobantes/" + id + "/anular")
                        .header(HttpHeaders.AUTHORIZATION, caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/caja/comprobantes/" + id + "/anular")
                        .header(HttpHeaders.AUTHORIZATION, caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Error de digitación\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"));

        mockMvc.perform(post("/api/v1/caja/cierre")
                        .header(HttpHeaders.AUTHORIZATION, caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoContado\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.diferencia").value(0.0));
        mockMvc.perform(get("/api/v1/caja/historial").header(HttpHeaders.AUTHORIZATION, caja))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(put("/api/v1/caja/comprobantes/00000000-0000-0000-0000-000000999999/anular")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}"))
                .andExpect(status().isNotFound());
    }
}

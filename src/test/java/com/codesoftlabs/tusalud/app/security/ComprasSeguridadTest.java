package com.codesoftlabs.tusalud.app.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class ComprasSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private String paciente;
    private String medico;
    private String recepcion;
    private String farmacia;
    private String almacen;
    private String finanzas;
    private String gerencia;
    private String admin;

    @BeforeEach
    void setUp() {
        paciente = token(fixture.usuario("cs-pac", RolNombre.ROLE_PACIENTE));
        medico = token(fixture.usuario("cs-med", RolNombre.ROLE_MEDICO));
        recepcion = token(fixture.usuario("cs-rec", RolNombre.ROLE_RECEPCION));
        farmacia = token(fixture.usuario("cs-far", RolNombre.ROLE_FARMACIA));
        almacen = token(fixture.usuario("cs-alm", RolNombre.ROLE_ALMACEN));
        finanzas = token(fixture.usuario("cs-fin", RolNombre.ROLE_FINANZAS));
        gerencia = token(fixture.usuario("cs-ger", RolNombre.ROLE_GERENCIA));
        admin = token(fixture.usuario("cs-adm", RolNombre.ROLE_ADMIN));
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    @Test
    void sinTokenTodoRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/compras/requisiciones")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/compras/ordenes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/compras/comprobantes")).andExpect(status().isUnauthorized());
    }

    @Test
    void lasCuentasPorPagarSoloLasVenFinanzasGerenciaYAdministracion() throws Exception {
        for (String token : new String[] {paciente, medico, recepcion, farmacia, almacen}) {
            mockMvc.perform(get("/api/v1/compras/comprobantes").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/compras/cuentas-por-pagar/resumen").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/compras/reportes/por-proveedor").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/compras/auditoria").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        for (String token : new String[] {finanzas, gerencia, admin}) {
            mockMvc.perform(get("/api/v1/compras/comprobantes").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/compras/cuentas-por-pagar/resumen").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void soloFinanzasRegistraComprobantesYPagos() throws Exception {
        String cuerpo = "{\"proveedorId\":\"" + UUID.randomUUID()
                + "\",\"tipo\":\"FACTURA\",\"serie\":\"F001\",\"numero\":\"1\",\"rucEmisor\":\""
                + ServiceTestFixture.rucValido() + "\","
                + "\"fechaEmision\":\"2026-01-01\",\"subtotal\":10}";
        for (String token : new String[] {almacen, gerencia, admin, farmacia}) {
            mockMvc.perform(post("/api/v1/compras/comprobantes")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/compras/comprobantes/" + UUID.randomUUID() + "/pagos")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"monto\":1,\"metodo\":\"EFECTIVO\"}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/v1/compras/comprobantes")
                        .header(HttpHeaders.AUTHORIZATION, finanzas)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isNotFound());
    }

    @Test
    void laFarmaciaSoloSolicitaYNoVeOrdenesNiCotizaciones() throws Exception {
        mockMvc.perform(get("/api/v1/compras/requisiciones").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/compras/requisiciones/sugeridas").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/compras/ordenes").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/compras/requisiciones/" + UUID.randomUUID() + "/cotizaciones")
                        .header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/compras/devoluciones").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/compras/requisiciones")
                        .header(HttpHeaders.AUTHORIZATION, finanzas)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productoId\":\"" + UUID.randomUUID() + "\",\"cantidad\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloAlmacenCreaOrdenesYSoloFinanzasOGerenciaLasResuelven() throws Exception {
        String orden = "{\"proveedorId\":\"" + UUID.randomUUID() + "\",\"items\":[{\"productoId\":\""
                + UUID.randomUUID() + "\",\"cantidad\":1,\"precioUnitario\":1}]}";
        for (String token : new String[] {finanzas, gerencia, admin, farmacia, medico}) {
            mockMvc.perform(post("/api/v1/compras/ordenes")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orden))
                    .andExpect(status().isForbidden());
        }
        for (String token : new String[] {almacen, admin, farmacia, paciente}) {
            mockMvc.perform(post("/api/v1/compras/ordenes/" + UUID.randomUUID() + "/aprobar")
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/v1/compras/ordenes/" + UUID.randomUUID() + "/aprobar")
                        .header(HttpHeaders.AUTHORIZATION, finanzas))
                .andExpect(status().isNotFound());
        for (String token : new String[] {finanzas, gerencia, admin, farmacia}) {
            mockMvc.perform(post("/api/v1/compras/ordenes/" + UUID.randomUUID() + "/recepciones")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"items\":[{\"ordenItemId\":\"" + UUID.randomUUID()
                                    + "\",\"cantidadRecibida\":1}]}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void elResumenOcultaLosMontosFinancierosAlAlmacen() throws Exception {
        mockMvc.perform(get("/api/v1/compras/resumen").header(HttpHeaders.AUTHORIZATION, almacen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porPagar").doesNotExist())
                .andExpect(jsonPath("$.limiteFinanzas").exists());
        mockMvc.perform(get("/api/v1/compras/resumen").header(HttpHeaders.AUTHORIZATION, finanzas))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porPagar").exists());
        mockMvc.perform(get("/api/v1/compras/resumen").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isForbidden());
    }

    @Test
    void elRucDelProveedorSeValidaEnLaApi() throws Exception {
        mockMvc.perform(post("/api/v1/almacen/proveedores")
                        .header(HttpHeaders.AUTHORIZATION, almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruc\":\"20123456780\",\"razonSocial\":\"Proveedor inválido\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/almacen/proveedores").header(HttpHeaders.AUTHORIZATION, finanzas))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/almacen/proveedores").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isForbidden());
    }
}

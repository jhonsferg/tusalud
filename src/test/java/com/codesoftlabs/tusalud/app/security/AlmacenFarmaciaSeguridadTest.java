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
class AlmacenFarmaciaSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private String paciente;
    private String almacen;
    private String farmacia;
    private String laboratorio;
    private String medico;

    @BeforeEach
    void setUp() {
        paciente = token(fixture.usuario("seg-pac", RolNombre.ROLE_PACIENTE));
        almacen = token(fixture.usuario("seg-alm", RolNombre.ROLE_ALMACEN));
        farmacia = token(fixture.usuario("seg-far", RolNombre.ROLE_FARMACIA));
        laboratorio = token(fixture.usuario("seg-lab", RolNombre.ROLE_LABORATORIO));
        medico = token(fixture.usuario("seg-med", RolNombre.ROLE_MEDICO));
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    private String producto(String codigo, String tipo) {
        return "{\"codigo\":\"" + codigo + "\",\"nombre\":\"Producto " + codigo + "\",\"tipo\":\"" + tipo
                + "\",\"unidad\":\"caja\",\"precioVenta\":2.5,\"stockMinimo\":3,\"requiereReceta\":false}";
    }

    @Test
    void sinTokenTodoRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/almacen/productos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/farmacia/catalogo")).andExpect(status().isUnauthorized());
    }

    @Test
    void elPacienteNoAccedeAAlmacenNiAFarmaciaOperativa() throws Exception {
        mockMvc.perform(get("/api/v1/almacen/productos").header(HttpHeaders.AUTHORIZATION, paciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/almacen/proveedores").header(HttpHeaders.AUTHORIZATION, paciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/farmacia/recetas").header(HttpHeaders.AUTHORIZATION, paciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/farmacia/catalogo").header(HttpHeaders.AUTHORIZATION, paciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/farmacia/dispensaciones").header(HttpHeaders.AUTHORIZATION, paciente))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloElPacientePuedeVerSusDispensaciones() throws Exception {
        mockMvc.perform(get("/api/v1/farmacia/mis-dispensaciones").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/farmacia/mis-dispensaciones").header(HttpHeaders.AUTHORIZATION, medico))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAlmacenCreaProductosYLaFarmaciaSoloLosConsulta() throws Exception {
        String codigo = "SEG-" + ServiceTestFixture.siguiente();
        mockMvc.perform(post("/api/v1/almacen/productos")
                        .header(HttpHeaders.AUTHORIZATION, almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(codigo, "MEDICAMENTO")))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.stockActual").value(0))
                .andExpect(jsonPath("$.bajoStock").value(true));

        mockMvc.perform(post("/api/v1/almacen/productos")
                        .header(HttpHeaders.AUTHORIZATION, farmacia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto("OTRO-" + codigo, "MEDICAMENTO")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/almacen/productos?q=" + codigo).header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value(codigo));
        mockMvc.perform(get("/api/v1/almacen/alertas").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vencidos").isArray());
        mockMvc.perform(get("/api/v1/almacen/movimientos").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/almacen/resumen").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isForbidden());
    }

    @Test
    void elLaboratorioSoloVeReactivosEInsumos() throws Exception {
        String reactivo = "SEG-R" + ServiceTestFixture.siguiente();
        String medicamento = "SEG-M" + ServiceTestFixture.siguiente();
        mockMvc.perform(post("/api/v1/almacen/productos")
                        .header(HttpHeaders.AUTHORIZATION, almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(reactivo, "REACTIVO")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/almacen/productos")
                        .header(HttpHeaders.AUTHORIZATION, almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(medicamento, "MEDICAMENTO")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/almacen/productos?q=SEG-").header(HttpHeaders.AUTHORIZATION, laboratorio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo=='" + reactivo + "')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.codigo=='" + medicamento + "')]").isEmpty());
        mockMvc.perform(get("/api/v1/almacen/proveedores").header(HttpHeaders.AUTHORIZATION, laboratorio))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/almacen/resumen").header(HttpHeaders.AUTHORIZATION, laboratorio))
                .andExpect(status().isForbidden());
    }

    @Test
    void validaLosDatosDeEntrada() throws Exception {
        mockMvc.perform(post("/api/v1/almacen/proveedores")
                        .header(HttpHeaders.AUTHORIZATION, almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruc\":\"123\",\"razonSocial\":\"X\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/almacen/ingresos")
                        .header(HttpHeaders.AUTHORIZATION, almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post("/api/v1/almacen/salidas")
                                .header(HttpHeaders.AUTHORIZATION, almacen)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"productoId\":\"00000000-0000-0000-0000-000000000001\",\"cantidad\":0,\"motivo\":\"MERMA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laFarmaciaLeeRecetasYElMedicoSoloElCatalogo() throws Exception {
        mockMvc.perform(get("/api/v1/farmacia/recetas").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/v1/farmacia/resumen").header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispensacionesHoy").value(0));
        mockMvc.perform(get("/api/v1/farmacia/catalogo").header(HttpHeaders.AUTHORIZATION, medico))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/farmacia/catalogo").header(HttpHeaders.AUTHORIZATION, almacen))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/farmacia/recetas").header(HttpHeaders.AUTHORIZATION, medico))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        post("/api/v1/farmacia/dispensaciones")
                                .header(HttpHeaders.AUTHORIZATION, medico)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"items\":[{\"productoId\":\"00000000-0000-0000-0000-000000000001\",\"cantidad\":1}],\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/farmacia/dispensaciones/00000000-0000-0000-0000-000000000001/anular")
                        .header(HttpHeaders.AUTHORIZATION, almacen))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/farmacia/dispensaciones/00000000-0000-0000-0000-000000999999/anular")
                        .header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isNotFound());
    }

    @Test
    void laDispensacionRechazaDatosInvalidosYMetodosDeAcceso() throws Exception {
        mockMvc.perform(post("/api/v1/farmacia/dispensaciones")
                        .header(HttpHeaders.AUTHORIZATION, farmacia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[],\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post("/api/v1/farmacia/dispensaciones")
                                .header(HttpHeaders.AUTHORIZATION, farmacia)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"items\":[{\"productoId\":\"00000000-0000-0000-0000-000000999999\",\"cantidad\":1}],\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isNotFound());
    }
}

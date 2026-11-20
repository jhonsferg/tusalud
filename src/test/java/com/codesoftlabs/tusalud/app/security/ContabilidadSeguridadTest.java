package com.codesoftlabs.tusalud.app.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import java.time.LocalDate;
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
class ContabilidadSeguridadTest {

    private static final String BASE = "/api/v1/contabilidad";
    private static final String ID = "00000000-0000-4000-8000-000000000001";
    private static final String ASIENTO = "{\"fecha\":\"2026-01-10\",\"glosa\":\"X\",\"lineas\":[{\"cuentaId\":\"" + ID
            + "\",\"debe\":1},{\"cuentaId\":\"" + ID + "\",\"haber\":1}]}";
    private static final String PERIODO = "{\"anio\":2026,\"mes\":1}";
    private static final String MOVIMIENTO = "{\"cuentaId\":\"" + ID
            + "\",\"fecha\":\"2026-01-10\",\"tipo\":\"INGRESO\",\"monto\":1,\"descripcion\":\"X\",\"contrapartidaId\":\""
            + ID + "\"}";
    private static final String CUENTA =
            "{\"codigo\":\"1099\",\"nombre\":\"X\",\"tipo\":\"ACTIVO\",\"aceptaMovimientos\":true}";
    private static final String PLANTILLA =
            "{\"nombre\":\"X\",\"activa\":true,\"cuentas\":[{\"lineaId\":\"" + ID + "\",\"cuentaId\":\"" + ID + "\"}]}";
    private static final String CONCILIAR = "{\"lineaId\":\"" + ID + "\",\"movimientoId\":\"" + ID + "\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private String paciente;
    private String medico;
    private String recepcion;
    private String caja;
    private String farmacia;
    private String almacen;
    private String admin;
    private String finanzas;
    private String gerencia;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void setUp() {
        paciente = token(fixture.usuario("co-pac", RolNombre.ROLE_PACIENTE));
        medico = token(fixture.usuario("co-med", RolNombre.ROLE_MEDICO));
        recepcion = token(fixture.usuario("co-rec", RolNombre.ROLE_RECEPCION));
        caja = token(fixture.usuario("co-caj", RolNombre.ROLE_CAJA));
        farmacia = token(fixture.usuario("co-far", RolNombre.ROLE_FARMACIA));
        almacen = token(fixture.usuario("co-alm", RolNombre.ROLE_ALMACEN));
        admin = token(fixture.usuario("co-adm", RolNombre.ROLE_ADMIN));
        finanzas = token(fixture.usuario("co-fin", RolNombre.ROLE_FINANZAS));
        gerencia = token(fixture.usuario("co-ger", RolNombre.ROLE_GERENCIA));
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    private String[] ajenos() {
        return new String[] {paciente, medico, recepcion, caja, farmacia, almacen, admin};
    }

    @Test
    void sinTokenTodoRetorna401() throws Exception {
        for (String ruta : new String[] {
            "/cuentas",
            "/periodos",
            "/asientos",
            "/plantillas",
            "/pendientes",
            "/resumen",
            "/auditoria",
            "/tesoreria/cuentas",
            "/tesoreria/movimientos",
            "/tesoreria/flujo-caja",
            "/libros/diario"
        }) {
            mockMvc.perform(get(BASE + ruta)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void laContabilidadEsInvisibleParaLosRolesAjenosIncluidoAdministracion() throws Exception {
        for (String token : ajenos()) {
            for (String ruta : new String[] {
                "/cuentas",
                "/periodos",
                "/asientos",
                "/plantillas",
                "/pendientes",
                "/resumen",
                "/auditoria",
                "/tesoreria/cuentas",
                "/tesoreria/movimientos",
                "/tesoreria/flujo-caja",
                "/tesoreria/conciliacion/extractos",
                "/libros/balance-comprobacion?desde=" + hoy + "&hasta=" + hoy,
                "/libros/estado-situacion?al=" + hoy,
                "/libros/electronicos/DIARIO?anio=2026&mes=3"
            }) {
                mockMvc.perform(get(BASE + ruta).header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void losRolesAjenosTampocoEscriben() throws Exception {
        for (String token : ajenos()) {
            mockMvc.perform(post(BASE + "/asientos")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ASIENTO))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(BASE + "/periodos")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PERIODO))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(BASE + "/tesoreria/movimientos")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(MOVIMIENTO))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void gerenciaConsultaPeroNoRegistraNiConfigura() throws Exception {
        mockMvc.perform(get(BASE + "/cuentas").header(HttpHeaders.AUTHORIZATION, gerencia))
                .andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/periodos").header(HttpHeaders.AUTHORIZATION, gerencia))
                .andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/asientos")
                        .header(HttpHeaders.AUTHORIZATION, gerencia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ASIENTO))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/cuentas")
                        .header(HttpHeaders.AUTHORIZATION, gerencia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUENTA))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/plantillas/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, gerencia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTILLA))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/tesoreria/conciliacion/conciliar")
                        .header(HttpHeaders.AUTHORIZATION, gerencia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONCILIAR))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloGerenciaReabreUnPeriodo() throws Exception {
        mockMvc.perform(post(BASE + "/periodos/" + UUID.randomUUID() + "/reabrir")
                        .header(HttpHeaders.AUTHORIZATION, finanzas)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Corrección necesaria\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/periodos/" + UUID.randomUUID() + "/reabrir")
                        .header(HttpHeaders.AUTHORIZATION, gerencia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Corrección necesaria\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void elFlujoCompletoPorHttpRespetaLaSegregacionDeFunciones() throws Exception {
        String cuentaDebe = idDe(post(BASE + "/cuentas")
                .header(HttpHeaders.AUTHORIZATION, finanzas)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"1" + (100 + ServiceTestFixture.siguiente() % 800)
                        + "\",\"nombre\":\"Caja chica\",\"tipo\":\"ACTIVO\",\"aceptaMovimientos\":true}"));
        String cuentaHaber = idDe(post(BASE + "/cuentas")
                .header(HttpHeaders.AUTHORIZATION, finanzas)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"5" + (100 + ServiceTestFixture.siguiente() % 800)
                        + "\",\"nombre\":\"Aporte\",\"tipo\":\"PATRIMONIO\",\"aceptaMovimientos\":true}"));
        String periodo = idDe(post(BASE + "/periodos")
                .header(HttpHeaders.AUTHORIZATION, finanzas)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"anio\":" + hoy.getYear() + ",\"mes\":" + hoy.getMonthValue() + "}"));
        String asiento = idDe(post(BASE + "/asientos")
                .header(HttpHeaders.AUTHORIZATION, finanzas)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fecha\":\"" + hoy + "\",\"glosa\":\"Aporte\",\"lineas\":[{\"cuentaId\":\"" + cuentaDebe
                        + "\",\"debe\":100.00},{\"cuentaId\":\"" + cuentaHaber + "\",\"haber\":100.00}]}"));

        mockMvc.perform(post(BASE + "/asientos/" + asiento + "/contabilizar")
                        .header(HttpHeaders.AUTHORIZATION, finanzas))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONTABILIZADO"))
                .andExpect(jsonPath("$.numero").isNotEmpty());
        mockMvc.perform(post(BASE + "/periodos/" + periodo + "/cerrar").header(HttpHeaders.AUTHORIZATION, finanzas))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/periodos/" + periodo + "/cerrar").header(HttpHeaders.AUTHORIZATION, gerencia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"));
        mockMvc.perform(get(BASE + "/libros/balance-comprobacion?desde=" + hoy.withDayOfMonth(1) + "&hasta=" + hoy)
                        .header(HttpHeaders.AUTHORIZATION, gerencia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuadra").value(true));
    }

    @Test
    void unBorradorConDatosInvalidosRetorna400() throws Exception {
        mockMvc.perform(post(BASE + "/asientos")
                        .header(HttpHeaders.AUTHORIZATION, finanzas)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"" + hoy + "\",\"glosa\":\"\",\"lineas\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/tesoreria/flujo-caja?semanas=99").header(HttpHeaders.AUTHORIZATION, finanzas))
                .andExpect(status().isBadRequest());
    }

    private String idDe(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion)
            throws Exception {
        String cuerpo = mockMvc.perform(peticion)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int inicio = cuerpo.indexOf("\"id\":\"") + 6;
        return cuerpo.substring(inicio, cuerpo.indexOf('"', inicio));
    }
}

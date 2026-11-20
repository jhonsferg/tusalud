package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class PlanillaSeguridadTest {

    private static final String PLANILLA = "/api/v1/planilla";
    private static final String ASISTENCIA = "/api/v1/asistencia";
    private static final String ID = "00000000-0000-4000-8000-000000000001";
    private static final String CALCULAR = "{\"anio\":2026,\"mes\":1}";
    private static final String MOTIVO = "{\"motivo\":\"X\"}";
    private static final String PAGO = "{\"fechaPago\":\"2026-01-10\",\"referencia\":\"R\"}";
    private static final String CONTRATO = "{\"empleadoId\":\"" + ID
            + "\",\"regimen\":\"GENERAL\",\"remuneracion\":1500,\"sistemaPensionario\":\"ONP\",\"cargo\":\"X\",\"fechaInicio\":\"2026-01-01\"}";
    private static final String PARAMETRO = "{\"codigo\":\"RMV\",\"valor\":1200,\"vigenteDesde\":\"2030-01-01\"}";
    private static final String CONCEPTO =
            "{\"codigo\":\"BONO_Z\",\"nombre\":\"X\",\"tipo\":\"INGRESO\",\"modo\":\"MONTO_FIJO\",\"valorDefecto\":1}";
    private static final String ASIGNACION =
            "{\"conceptoId\":\"" + ID + "\",\"valor\":1,\"vigenteDesde\":\"2026-01-01\"}";
    private static final String MARCACION = "{\"empleadoId\":\"" + ID
            + "\",\"tipo\":\"ENTRADA\",\"fechaHora\":\"2026-01-10T08:00:00\",\"observacion\":\"X\"}";
    private static final String CONSOLIDAR = "{\"desde\":\"2026-01-01\",\"hasta\":\"2026-01-10\"}";
    private static final String AJUSTE = "{\"justificacion\":\"X\"}";
    private static final String TERMINAL = "{\"nombre\":\"Puerta\"}";

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
    private String rrhh;
    private String finanzas;
    private String gerencia;

    @BeforeEach
    void setUp() {
        paciente = token(fixture.usuario("pl-pac", RolNombre.ROLE_PACIENTE));
        medico = token(fixture.usuario("pl-med", RolNombre.ROLE_MEDICO));
        recepcion = token(fixture.usuario("pl-rec", RolNombre.ROLE_RECEPCION));
        caja = token(fixture.usuario("pl-caj", RolNombre.ROLE_CAJA));
        farmacia = token(fixture.usuario("pl-far", RolNombre.ROLE_FARMACIA));
        almacen = token(fixture.usuario("pl-alm", RolNombre.ROLE_ALMACEN));
        admin = token(fixture.usuario("pl-adm", RolNombre.ROLE_ADMIN));
        rrhh = token(fixture.usuario("pl-rrh", RolNombre.ROLE_RRHH));
        finanzas = token(fixture.usuario("pl-fin", RolNombre.ROLE_FINANZAS));
        gerencia = token(fixture.usuario("pl-ger", RolNombre.ROLE_GERENCIA));
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    private MockHttpServletRequestBuilder pedido(String metodo, String ruta, String cuerpo) {
        MockHttpServletRequestBuilder b = switch (metodo) {
            case "POST" -> post(ruta);
            case "PUT" -> put(ruta);
            case "PATCH" -> patch(ruta);
            case "DELETE" -> delete(ruta);
            default -> get(ruta);
        };
        if (cuerpo != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return b;
    }

    private void prohibido(String metodo, String ruta, String cuerpo, String... tokens) throws Exception {
        for (String t : tokens) {
            mockMvc.perform(pedido(metodo, ruta, cuerpo).header(HttpHeaders.AUTHORIZATION, t))
                    .andExpect(status().isForbidden());
        }
    }

    private void ok(String metodo, String ruta, String cuerpo, String token) throws Exception {
        int estado = mockMvc.perform(pedido(metodo, ruta, cuerpo).header(HttpHeaders.AUTHORIZATION, token))
                .andReturn()
                .getResponse()
                .getStatus();
        if (estado == 401 || estado == 403) {
            throw new AssertionError(metodo + " " + ruta + " devolvió " + estado);
        }
    }

    @Test
    void sinTokenTodoRetorna401ExceptoLaTerminal() throws Exception {
        String[][] rutas = {
            {"GET", PLANILLA + "/resumen"},
            {"GET", PLANILLA + "/planillas"},
            {"GET", PLANILLA + "/planillas/" + ID},
            {"POST", PLANILLA + "/planillas/calcular"},
            {"POST", PLANILLA + "/planillas/" + ID + "/aprobar"},
            {"GET", PLANILLA + "/boletas/" + ID},
            {"GET", PLANILLA + "/mis-boletas"},
            {"GET", PLANILLA + "/parametros"},
            {"GET", PLANILLA + "/contratos"},
            {"GET", PLANILLA + "/conceptos"},
            {"GET", ASISTENCIA + "/marcaciones?desde=2026-01-01&hasta=2026-01-10"},
            {"GET", ASISTENCIA + "/mi-asistencia?desde=2026-01-01&hasta=2026-01-10"},
            {"GET", ASISTENCIA + "/terminales"},
        };
        for (String[] r : rutas) {
            mockMvc.perform(pedido(r[0], r[1], r[0].equals("POST") ? "{}" : null))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void laAdministracionYLosRolesAjenosNoEntranAlCalculoNiALaConfiguracion() throws Exception {
        String[] ajenos = {paciente, medico, recepcion, caja, farmacia, almacen, admin};
        prohibido("GET", PLANILLA + "/resumen", null, ajenos);
        prohibido("GET", PLANILLA + "/planillas", null, ajenos);
        prohibido("GET", PLANILLA + "/planillas/" + ID, null, ajenos);
        prohibido("POST", PLANILLA + "/planillas/calcular", CALCULAR, ajenos);
        prohibido("POST", PLANILLA + "/planillas/" + ID + "/aprobar", null, ajenos);
        prohibido("POST", PLANILLA + "/planillas/" + ID + "/devolver", MOTIVO, ajenos);
        prohibido("POST", PLANILLA + "/planillas/" + ID + "/pagar", PAGO, ajenos);
        prohibido("POST", PLANILLA + "/planillas/" + ID + "/cerrar", null, ajenos);
        prohibido("GET", PLANILLA + "/planillas/" + ID + "/archivo-bancario", null, ajenos);
        prohibido("GET", PLANILLA + "/planillas/" + ID + "/plame", null, ajenos);
        prohibido("GET", PLANILLA + "/auditoria", null, ajenos);
        prohibido("GET", PLANILLA + "/parametros", null, ajenos);
        prohibido("POST", PLANILLA + "/parametros", PARAMETRO, ajenos);
        prohibido("GET", PLANILLA + "/conceptos", null, ajenos);
        prohibido("POST", PLANILLA + "/conceptos", CONCEPTO, ajenos);
        prohibido("GET", PLANILLA + "/contratos", null, ajenos);
        prohibido("POST", PLANILLA + "/contratos", CONTRATO, ajenos);
        prohibido("GET", PLANILLA + "/empleados/" + ID + "/conceptos", null, ajenos);
        prohibido("POST", PLANILLA + "/empleados/" + ID + "/conceptos", ASIGNACION, ajenos);
        prohibido("GET", ASISTENCIA + "/marcaciones?desde=2026-01-01&hasta=2026-01-10", null, ajenos);
        prohibido("POST", ASISTENCIA + "/marcaciones", MARCACION, ajenos);
        prohibido("GET", ASISTENCIA + "/dias?desde=2026-01-01&hasta=2026-01-10", null, ajenos);
        prohibido("POST", ASISTENCIA + "/consolidar", CONSOLIDAR, ajenos);
        prohibido("PUT", ASISTENCIA + "/dias/" + ID + "/ajuste", AJUSTE, ajenos);
        prohibido("GET", ASISTENCIA + "/terminales", null, ajenos);
        prohibido("POST", ASISTENCIA + "/terminales", TERMINAL, ajenos);
    }

    @Test
    void finanzasApruebaYPagaPeroNoCalculaNiVeContratosNiAsistencia() throws Exception {
        prohibido("POST", PLANILLA + "/planillas/calcular", CALCULAR, finanzas);
        prohibido("GET", PLANILLA + "/contratos", null, finanzas);
        prohibido("POST", PLANILLA + "/contratos", CONTRATO, finanzas);
        prohibido("POST", PLANILLA + "/conceptos", CONCEPTO, finanzas);
        prohibido("POST", PLANILLA + "/parametros", PARAMETRO, finanzas);
        prohibido("GET", PLANILLA + "/planillas/" + ID + "/plame", null, finanzas);
        prohibido("POST", PLANILLA + "/planillas/" + ID + "/cerrar", null, finanzas);
        prohibido("GET", ASISTENCIA + "/marcaciones?desde=2026-01-01&hasta=2026-01-10", null, finanzas);
        prohibido("POST", ASISTENCIA + "/consolidar", CONSOLIDAR, finanzas);
        ok("GET", PLANILLA + "/planillas", null, finanzas);
        ok("GET", PLANILLA + "/conceptos", null, finanzas);
        ok("GET", PLANILLA + "/parametros", null, finanzas);
        ok("POST", PLANILLA + "/planillas/" + ID + "/aprobar", null, finanzas);
        ok("POST", PLANILLA + "/planillas/" + ID + "/pagar", PAGO, finanzas);
        ok("GET", PLANILLA + "/planillas/" + ID + "/archivo-bancario", null, finanzas);
    }

    @Test
    void rrhhCalculaPeroNoApruebaNiPagaNiGeneraElArchivoBancario() throws Exception {
        prohibido("POST", PLANILLA + "/planillas/" + ID + "/aprobar", null, rrhh);
        prohibido("POST", PLANILLA + "/planillas/" + ID + "/devolver", MOTIVO, rrhh);
        prohibido("POST", PLANILLA + "/planillas/" + ID + "/pagar", PAGO, rrhh);
        prohibido("GET", PLANILLA + "/planillas/" + ID + "/archivo-bancario", null, rrhh);
        prohibido("POST", PLANILLA + "/parametros/" + ID + "/validar", null, rrhh);
        ok("POST", PLANILLA + "/planillas/calcular", CALCULAR, rrhh);
        ok("GET", PLANILLA + "/contratos", null, rrhh);
        ok("GET", PLANILLA + "/planillas/" + ID + "/plame", null, rrhh);
        ok("GET", ASISTENCIA + "/terminales", null, rrhh);
    }

    @Test
    void gerenciaSoloConsultaTotalesYCierra() throws Exception {
        prohibido("POST", PLANILLA + "/planillas/calcular", CALCULAR, gerencia);
        prohibido("POST", PLANILLA + "/planillas/" + ID + "/aprobar", null, gerencia);
        prohibido("POST", PLANILLA + "/planillas/" + ID + "/pagar", PAGO, gerencia);
        prohibido("GET", PLANILLA + "/contratos", null, gerencia);
        prohibido("GET", PLANILLA + "/conceptos", null, gerencia);
        prohibido("GET", PLANILLA + "/auditoria", null, gerencia);
        prohibido("GET", ASISTENCIA + "/dias?desde=2026-01-01&hasta=2026-01-10", null, gerencia);
        ok("GET", PLANILLA + "/resumen", null, gerencia);
        ok("GET", PLANILLA + "/planillas", null, gerencia);
        ok("GET", PLANILLA + "/parametros", null, gerencia);
        ok("POST", PLANILLA + "/planillas/" + ID + "/cerrar", null, gerencia);
    }

    @Test
    void cualquierPersonalConsultaSoloLoSuyoYUnaBoletaAjenaNoExiste() throws Exception {
        for (String t : new String[] {medico, recepcion, caja, farmacia, paciente}) {
            mockMvc.perform(get(PLANILLA + "/mis-boletas").header(HttpHeaders.AUTHORIZATION, t))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
            mockMvc.perform(get(ASISTENCIA + "/mi-asistencia?desde=2026-01-01&hasta=2026-01-10")
                            .header(HttpHeaders.AUTHORIZATION, t))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dias", hasSize(0)));
            mockMvc.perform(get(PLANILLA + "/boletas/" + ID).header(HttpHeaders.AUTHORIZATION, t))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void laTerminalNoRevelaSiElCodigoOElTokenExisten() throws Exception {
        String cuerpo = "{\"codigo\":\"ABCD1234\"}";
        mockMvc.perform(post(ASISTENCIA + "/terminal/marcaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(ASISTENCIA + "/terminal/marcaciones")
                        .header("X-Terminal-Token", "tm_inventado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(ASISTENCIA + "/terminal/marcaciones")
                        .header("X-Terminal-Token", "tm_inventado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"<script>\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laValidacionRechazaDatosIncorrectosEnLaConfiguracion() throws Exception {
        mockMvc.perform(
                        post(PLANILLA + "/contratos")
                                .header(HttpHeaders.AUTHORIZATION, rrhh)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"empleadoId\":\"" + ID
                                                + "\",\"regimen\":\"GENERAL\",\"remuneracion\":-5,\"sistemaPensionario\":\"ONP\",\"cargo\":\"X\",\"fechaInicio\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(PLANILLA + "/planillas/calcular")
                        .header(HttpHeaders.AUTHORIZATION, rrhh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anio\":2026,\"mes\":13}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post(PLANILLA + "/conceptos")
                                .header(HttpHeaders.AUTHORIZATION, rrhh)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"codigo\":\"bono mal\",\"nombre\":\"X\",\"tipo\":\"INGRESO\",\"modo\":\"MONTO_FIJO\",\"valorDefecto\":1}"))
                .andExpect(status().isBadRequest());
    }
}

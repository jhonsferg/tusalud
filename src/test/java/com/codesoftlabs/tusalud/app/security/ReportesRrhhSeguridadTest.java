package com.codesoftlabs.tusalud.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.PlantillaTurno;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.repositories.PlantillaTurnoRepository;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class ReportesRrhhSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private PlantillaTurnoRepository plantillaRepository;

    private Map<RolNombre, String> tokens;
    private Usuario usuarioCaja;
    private String tokenCaja;
    private UUID plantillaId;

    @BeforeEach
    void setUp() {
        tokens = new java.util.EnumMap<>(RolNombre.class);
        for (RolNombre rol : RolNombre.values()) {
            tokens.put(rol, token(fixture.usuario("seg-" + rol.name().toLowerCase(), rol)));
        }
        usuarioCaja = fixture.usuario("seg-empleado-caja", RolNombre.ROLE_CAJA);
        tokenCaja = token(usuarioCaja);
        plantillaId = plantillaRepository
                .save(PlantillaTurno.builder()
                        .nombre("Mañana-" + ServiceTestFixture.siguiente())
                        .horaInicio(LocalTime.of(7, 0))
                        .horaFin(LocalTime.of(15, 0))
                        .build())
                .getId();
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    private ResultActions ver(String url, String token) throws Exception {
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions enviar(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder metodo,
            String token,
            String json)
            throws Exception {
        return mockMvc.perform(metodo.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void permitidos(String url, RolNombre... roles) throws Exception {
        java.util.Set<RolNombre> permitidos = java.util.EnumSet.noneOf(RolNombre.class);
        permitidos.addAll(java.util.List.of(roles));
        for (RolNombre rol : RolNombre.values()) {
            if (permitidos.contains(rol)) {
                ver(url, tokens.get(rol)).andExpect(status().isOk());
            } else {
                ver(url, tokens.get(rol)).andExpect(status().isForbidden());
            }
        }
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    @Test
    void cadaReporteSoloLoVenLosRolesDeSuArea() throws Exception {
        permitidos("/api/v1/reportes/ejecutivo", RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN);
        permitidos(
                "/api/v1/reportes/financiero", RolNombre.ROLE_FINANZAS, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN);
        permitidos(
                "/api/v1/reportes/operativo",
                RolNombre.ROLE_OPERACIONES,
                RolNombre.ROLE_GERENCIA,
                RolNombre.ROLE_ADMIN);
        permitidos("/api/v1/reportes/calidad", RolNombre.ROLE_CALIDAD, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN);
    }

    @Test
    void losReportesValidanLosParametros() throws Exception {
        String gerencia = tokens.get(RolNombre.ROLE_GERENCIA);
        LocalDate hoy = LocalDate.now();
        ver("/api/v1/reportes/ejecutivo?desde=" + hoy + "&hasta=" + hoy.minusDays(1), gerencia)
                .andExpect(status().isBadRequest());
        ver("/api/v1/reportes/financiero?desde=" + hoy.minusDays(400) + "&hasta=" + hoy, gerencia)
                .andExpect(status().isBadRequest());
        ver("/api/v1/reportes/operativo?agrupacion=ANO", gerencia).andExpect(status().isBadRequest());
        ver("/api/v1/reportes/calidad?desde=ayer", gerencia).andExpect(status().isBadRequest());
        ver("/api/v1/reportes/ejecutivo?agrupacion=SEMANA", gerencia)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.agrupacion").value("SEMANA"))
                .andExpect(jsonPath("$.periodo.dias").value(30));
    }

    @Test
    void elPersonalSinPermisoNoAccedeAlModuloDeRrhh() throws Exception {
        for (RolNombre rol : new RolNombre[] {
            RolNombre.ROLE_PACIENTE, RolNombre.ROLE_CAJA, RolNombre.ROLE_FINANZAS, RolNombre.ROLE_MEDICO
        }) {
            String t = tokens.get(rol);
            ver("/api/v1/rrhh/resumen", t).andExpect(status().isForbidden());
            ver("/api/v1/rrhh/empleados", t).andExpect(status().isForbidden());
            ver("/api/v1/rrhh/empleados/" + UUID.randomUUID(), t).andExpect(status().isForbidden());
            ver("/api/v1/rrhh/usuarios-disponibles", t).andExpect(status().isForbidden());
            ver("/api/v1/rrhh/plantillas-turno", t).andExpect(status().isForbidden());
            ver("/api/v1/rrhh/turnos", t).andExpect(status().isForbidden());
            ver("/api/v1/rrhh/turnos/semana", t).andExpect(status().isForbidden());
            ver("/api/v1/rrhh/ausencias", t).andExpect(status().isForbidden());
            enviar(put("/api/v1/rrhh/ausencias/" + UUID.randomUUID() + "/aprobar"), t, "{}")
                    .andExpect(status().isForbidden());
        }
        ver("/api/v1/rrhh/mis-turnos", tokens.get(RolNombre.ROLE_PACIENTE)).andExpect(status().isForbidden());
        ver("/api/v1/rrhh/mis-ausencias", tokens.get(RolNombre.ROLE_PACIENTE)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/rrhh/resumen")).andExpect(status().isUnauthorized());
    }

    @Test
    void soloRrhhEscribeYGerenciaYAdminSoloLeen() throws Exception {
        permitidos("/api/v1/rrhh/resumen", RolNombre.ROLE_RRHH, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN);
        permitidos("/api/v1/rrhh/empleados", RolNombre.ROLE_RRHH, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN);
        permitidos("/api/v1/rrhh/plantillas-turno", RolNombre.ROLE_RRHH, RolNombre.ROLE_ADMIN);
        permitidos("/api/v1/rrhh/ausencias", RolNombre.ROLE_RRHH, RolNombre.ROLE_ADMIN);
        permitidos("/api/v1/rrhh/usuarios-disponibles", RolNombre.ROLE_RRHH);

        String json = fichaJson(usuarioCaja.getId());
        for (RolNombre rol : new RolNombre[] {RolNombre.ROLE_ADMIN, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_CAJA}) {
            enviar(post("/api/v1/rrhh/empleados"), tokens.get(rol), json).andExpect(status().isForbidden());
            enviar(post("/api/v1/rrhh/turnos"), tokens.get(rol), turnoValido()).andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/v1/rrhh/turnos/" + UUID.randomUUID())
                            .header(HttpHeaders.AUTHORIZATION, tokens.get(rol)))
                    .andExpect(status().isForbidden());
        }
    }

    private String turnoValido() {
        return "{\"empleadoId\":\"%s\",\"plantillaId\":\"%s\",\"fecha\":\"%s\"}"
                .formatted(UUID.randomUUID(), plantillaId, LocalDate.now().plusDays(3));
    }

    private static String fichaJson(UUID usuarioId) {
        return """
                {"usuarioId":"%s","cargo":"Cajero","area":"CAJA","tipoContrato":"INDEFINIDO",
                 "fechaIngreso":"%s","sueldo":3100.00,"contactoEmergenciaNombre":"Elsa Paredes",
                 "contactoEmergenciaTelefono":"987654321","diasVacacionesAnuales":30}
                """.formatted(usuarioId, LocalDate.now().minusYears(1));
    }

    private UUID crearFicha() throws Exception {
        String respuesta = enviar(
                        post("/api/v1/rrhh/empleados"), tokens.get(RolNombre.ROLE_RRHH), fichaJson(usuarioCaja.getId()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(respuesta, "$.id"));
    }

    @Test
    void elSueldoSoloLlegaARrhhYGerenciaPorHttp() throws Exception {
        UUID id = crearFicha();
        String url = "/api/v1/rrhh/empleados/" + id;

        ver(url, tokens.get(RolNombre.ROLE_RRHH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sueldo").value(3100.0));
        ver(url, tokens.get(RolNombre.ROLE_GERENCIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sueldo").value(3100.0));
        String paraAdmin = ver(url, tokens.get(RolNombre.ROLE_ADMIN))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(paraAdmin)
                .doesNotContain("3100")
                .doesNotContain("Elsa Paredes")
                .doesNotContain("987654321");
        String lista = ver("/api/v1/rrhh/empleados?q=cajero", tokens.get(RolNombre.ROLE_ADMIN))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(lista).doesNotContain("3100");
        String propia = ver("/api/v1/rrhh/mi-ficha", tokenCaja)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(propia).doesNotContain("3100");
    }

    @Test
    void identificadoresInvalidosOInexistentesNoRevelanNiRompen() throws Exception {
        String rrhh = tokens.get(RolNombre.ROLE_RRHH);
        ver("/api/v1/rrhh/empleados/uno", rrhh).andExpect(status().isBadRequest());
        ver("/api/v1/rrhh/empleados/" + UUID.randomUUID(), rrhh).andExpect(status().isNotFound());
        enviar(put("/api/v1/rrhh/ausencias/" + UUID.randomUUID() + "/aprobar"), rrhh, "{}")
                .andExpect(status().isNotFound());
        enviar(put("/api/v1/rrhh/ausencias/xyz/rechazar"), rrhh, "{}").andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/v1/rrhh/turnos/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, rrhh))
                .andExpect(status().isNotFound());
    }

    @Test
    void validaElCuerpoDeLaFichaYNoAceptaCamposAjenos() throws Exception {
        String rrhh = tokens.get(RolNombre.ROLE_RRHH);
        enviar(post("/api/v1/rrhh/empleados"), rrhh, "{}").andExpect(status().isBadRequest());
        enviar(
                        post("/api/v1/rrhh/empleados"),
                        rrhh,
                        fichaJson(usuarioCaja.getId()).replace("3100.00", "-5"))
                .andExpect(status().isBadRequest());
        enviar(
                        post("/api/v1/rrhh/empleados"),
                        rrhh,
                        fichaJson(usuarioCaja.getId()).replace("987654321", "12"))
                .andExpect(status().isBadRequest());
        enviar(
                        post("/api/v1/rrhh/empleados"),
                        rrhh,
                        fichaJson(usuarioCaja.getId()).replace("\"CAJA\"", "\"PLANETA\""))
                .andExpect(status().isBadRequest());
        crearFicha();
        enviar(post("/api/v1/rrhh/empleados"), rrhh, fichaJson(usuarioCaja.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void elEmpleadoSoloVeYGestionaLoSuyo() throws Exception {
        crearFicha();
        LocalDate inicio = LocalDate.now().plusDays(10);
        String solicitud = "{\"tipo\":\"PERMISO\",\"fechaInicio\":\"%s\",\"fechaFin\":\"%s\",\"motivo\":\"Trámite\"}"
                .formatted(inicio, inicio);
        String creada = enviar(post("/api/v1/rrhh/mis-ausencias"), tokenCaja, solicitud)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("SOLICITADA"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String ausenciaId = JsonPath.read(creada, "$.id");

        ver("/api/v1/rrhh/mis-ausencias", tokenCaja)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ausencias.length()").value(1))
                .andExpect(jsonPath("$.saldo.saldo").value(30));
        enviar(post("/api/v1/rrhh/mis-ausencias"), tokenCaja, solicitud).andExpect(status().isConflict());
        enviar(put("/api/v1/rrhh/ausencias/" + ausenciaId + "/aprobar"), tokenCaja, "{}")
                .andExpect(status().isForbidden());
        enviar(put("/api/v1/rrhh/mis-ausencias/" + ausenciaId + "/cancelar"), tokens.get(RolNombre.ROLE_ENFERMERIA), "")
                .andExpect(status().isNotFound());
        ver("/api/v1/rrhh/mis-turnos", tokenCaja)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        enviar(put("/api/v1/rrhh/ausencias/" + ausenciaId + "/rechazar"), tokens.get(RolNombre.ROLE_RRHH), "{}")
                .andExpect(status().isBadRequest());
        enviar(put("/api/v1/rrhh/ausencias/" + ausenciaId + "/aprobar"), tokens.get(RolNombre.ROLE_RRHH), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));
        ver("/api/v1/rrhh/ausencias", tokens.get(RolNombre.ROLE_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motivo").doesNotExist());
    }

    @Test
    void sinFichaLasConsultasPropiasRespondenNoEncontrado() throws Exception {
        ver("/api/v1/rrhh/mi-ficha", tokens.get(RolNombre.ROLE_ENFERMERIA)).andExpect(status().isNotFound());
        ver("/api/v1/rrhh/mis-turnos", tokens.get(RolNombre.ROLE_ENFERMERIA)).andExpect(status().isNotFound());
        ver("/api/v1/rrhh/mis-ausencias", tokens.get(RolNombre.ROLE_ENFERMERIA)).andExpect(status().isNotFound());
    }

    @Test
    void asignaTurnosPorHttpYRechazaLosChoques() throws Exception {
        UUID empleadoId = crearFicha();
        String rrhh = tokens.get(RolNombre.ROLE_RRHH);
        String fecha = LocalDate.now().plusDays(5).toString();
        String turno = "{\"empleadoId\":\"%s\",\"plantillaId\":\"%s\",\"fecha\":\"%s\"}"
                .formatted(empleadoId, plantillaId, fecha);

        String creado = enviar(post("/api/v1/rrhh/turnos"), rrhh, turno)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        enviar(post("/api/v1/rrhh/turnos"), rrhh, turno).andExpect(status().isConflict());
        ver("/api/v1/rrhh/turnos/semana?fecha=" + fecha + "&area=CAJA", tokens.get(RolNombre.ROLE_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filas.length()").value(1));
        ver("/api/v1/rrhh/mis-turnos?desde=" + fecha + "&hasta=" + fecha, tokenCaja)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(delete("/api/v1/rrhh/turnos/" + JsonPath.read(creado, "$.id"))
                        .header(HttpHeaders.AUTHORIZATION, rrhh))
                .andExpect(status().isNoContent());
    }

    @Test
    void elAdministradorRegistraPersonalConLosRolesGerencialesYElPacienteNo() throws Exception {
        for (String rol :
                new String[] {"ROLE_GERENCIA", "ROLE_FINANZAS", "ROLE_OPERACIONES", "ROLE_RRHH", "ROLE_CALIDAD"}) {
            String correo = rol.toLowerCase().replace("role_", "") + ServiceTestFixture.siguiente() + "@test.com";
            String cuerpo =
                    "{\"nombre\":\"Nombre\",\"apellido\":\"Apellido\",\"email\":\"%s\",\"password\":\"Personal123!\",\"roles\":[\"%s\"]}"
                            .formatted(correo, rol);
            enviar(post("/api/v1/admin/personal"), tokens.get(RolNombre.ROLE_ADMIN), cuerpo)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.roles[0]").value(rol));
            enviar(post("/api/v1/admin/personal"), tokens.get(RolNombre.ROLE_PACIENTE), cuerpo)
                    .andExpect(status().isForbidden());
            enviar(
                            post("/api/v1/admin/personal"),
                            tokens.get(RolNombre.ROLE_RRHH),
                            cuerpo.replace(correo, "x" + correo))
                    .andExpect(status().isForbidden());
        }
    }
}

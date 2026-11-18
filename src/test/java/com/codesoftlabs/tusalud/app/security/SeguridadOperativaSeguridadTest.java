package com.codesoftlabs.tusalud.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
class SeguridadOperativaSeguridadTest {

    private static final String EVENTOS = "/api/v1/admin/seguridad/eventos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario admin;
    private Usuario paciente;
    private String tokenAdmin;
    private String tokenPaciente;

    @BeforeEach
    void setUp() {
        admin = fixture.usuario("seg-admin", RolNombre.ROLE_ADMIN);
        paciente = fixture.usuario("seg-pac", RolNombre.ROLE_PACIENTE);
        tokenAdmin = token(admin);
        tokenPaciente = token(paciente);
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions eventos(String consulta) throws Exception {
        return mockMvc.perform(get(EVENTOS + "?" + consulta).header(HttpHeaders.AUTHORIZATION, tokenAdmin));
    }

    private ResultActions enviar(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion,
            String token,
            String json)
            throws Exception {
        return mockMvc.perform(peticion.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void unLoginFallidoRegistraElEventoSinEmailNiContrasena() throws Exception {
        login(paciente.getEmail(), "Incorrecta123!").andExpect(status().isUnauthorized());

        eventos("tipo=LOGIN_FALLIDO&usuarioId=" + paciente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.contenido[0].detalle", is("Credenciales inválidas")))
                .andExpect(jsonPath("$.contenido[0].severidad", is("AVISO")))
                .andExpect(jsonPath("$.contenido[0].ip", is("127.0.0.x")))
                .andExpect(content().string(not(containsString(paciente.getEmail()))))
                .andExpect(content().string(not(containsString("Incorrecta123!"))));
    }

    @Test
    void unLoginConUnaCuentaInexistenteNoGuardaElEmailIntentado() throws Exception {
        String inexistente = "nadie." + ServiceTestFixture.siguiente() + "@tusalud.test";
        login(inexistente, "Incorrecta123!").andExpect(status().isUnauthorized());

        eventos("tipo=LOGIN_FALLIDO&desde=" + LocalDate.now())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].detalle", hasItem(containsString("cuenta inexistente"))))
                .andExpect(content().string(not(containsString(inexistente))));
    }

    @Test
    void cincoFallosSeguidosGeneranUnEventoDeAltaSeveridad() throws Exception {
        for (int i = 0; i < 5; i++) {
            login(paciente.getEmail(), "Incorrecta123!").andExpect(status().isUnauthorized());
        }

        eventos("tipo=LOGIN_FALLIDOS_REPETIDOS&usuarioId=" + paciente.getId())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.contenido[0].severidad", is("ALTA")));
    }

    @Test
    void unIntentoConUnaCuentaInactivaQuedaRegistrado() throws Exception {
        paciente.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.saveAndFlush(paciente);

        login(paciente.getEmail(), ServiceTestFixture.PASSWORD).andExpect(status().isUnauthorized());

        eventos("tipo=LOGIN_CUENTA_INACTIVA&usuarioId=" + paciente.getId()).andExpect(jsonPath("$.total", is(1)));
    }

    @Test
    void losAccesosDenegadosSeRegistranSinIdsYSeRepitenAlQuintoIntento() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/admin/seguridad/resumen").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                    .andExpect(status().isForbidden());
        }

        eventos("tipo=ACCESO_DENEGADO&usuarioId=" + paciente.getId())
                .andExpect(jsonPath("$.total", is(5)))
                .andExpect(jsonPath("$.contenido[0].detalle", is("GET /api/v1/admin/seguridad/resumen")));
        eventos("tipo=ACCESO_DENEGADO_REPETIDO&usuarioId=" + paciente.getId())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.contenido[0].severidad", is("ALTA")));
    }

    @Test
    void soloLaAdministracionConsultaSeguridadSistemaYAlmacenamiento() throws Exception {
        String[] rutas = {
            EVENTOS,
            "/api/v1/admin/seguridad/resumen",
            "/api/v1/admin/sistema/estado",
            "/api/v1/admin/almacenamiento/uso"
        };
        for (RolNombre rol : new RolNombre[] {
            RolNombre.ROLE_PACIENTE, RolNombre.ROLE_MEDICO, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ENFERMERIA
        }) {
            String token = token(fixture.usuario("seg-rol-" + rol.name().toLowerCase(), rol));
            for (String ruta : rutas) {
                mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isForbidden());
            }
        }
        for (String ruta : rutas) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void losCambiosDeSeguridadDeUnaCuentaQuedanRegistrados() throws Exception {
        enviar(put("/api/v1/admin/usuarios/" + paciente.getId() + "/estado"), tokenAdmin, "{\"estado\":\"INACTIVO\"}")
                .andExpect(status().isOk());
        enviar(put("/api/v1/admin/usuarios/" + paciente.getId() + "/estado"), tokenAdmin, "{\"estado\":\"ACTIVO\"}")
                .andExpect(status().isOk());
        enviar(put("/api/v1/admin/usuarios/" + paciente.getId() + "/roles"), tokenAdmin, "{\"roles\":[\"ROLE_CAJA\"]}")
                .andExpect(status().isOk());
        enviar(
                        put("/api/v1/perfil/password"),
                        tokenPaciente,
                        "{\"passwordActual\":\"" + ServiceTestFixture.PASSWORD
                                + "\",\"passwordNueva\":\"OtraClave456!\",\"confirmarPassword\":\"OtraClave456!\"}")
                .andExpect(status().isNoContent());

        for (String tipo :
                new String[] {"CUENTA_DESACTIVADA", "CUENTA_ACTIVADA", "ROLES_CAMBIADOS", "CONTRASENA_CAMBIADA"}) {
            eventos("tipo=" + tipo + "&usuarioId=" + paciente.getId()).andExpect(jsonPath("$.total", is(1)));
        }
        eventos("tipo=ROLES_CAMBIADOS&usuarioId=" + paciente.getId())
                .andExpect(jsonPath("$.contenido[0].detalle", containsString("ROLE_CAJA")))
                .andExpect(jsonPath("$.contenido[0].actorNombre", is(admin.getNombreCompleto())));
    }

    @Test
    void elResumenAgrupaPorTipoYSeveridad() throws Exception {
        login(paciente.getEmail(), "Incorrecta123!").andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/seguridad/resumen").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimas24Horas.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.ultimos7Dias.porTipo[*].tipo", hasItem("LOGIN_FALLIDO")))
                .andExpect(jsonPath("$.ultimas24Horas.avisos", greaterThanOrEqualTo(1)));
    }

    @Test
    void losFiltrosYLaPaginacionSeValidan() throws Exception {
        eventos("pagina=-1").andExpect(status().isBadRequest());
        eventos("tamano=500").andExpect(status().isBadRequest());
        eventos("desde=2026-05-10&hasta=2026-05-01").andExpect(status().isBadRequest());
        eventos("tipo=NO_EXISTE").andExpect(status().isBadRequest());
    }

    @Test
    void elEstadoDelSistemaYElUsoDelAlmacenamientoNoExponenSecretos() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sistema/estado").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoGeneral", is("OK")))
                .andExpect(jsonPath("$.componentes.length()", is(2)))
                .andExpect(jsonPath("$.componentes[0].nombre", is("Base de datos")))
                .andExpect(jsonPath("$.segundosEnMarcha", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(content().string(not(containsString("jdbc:"))))
                .andExpect(content().string(not(containsString("secret"))));
        mockMvc.perform(get("/api/v1/admin/almacenamiento/uso").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalObjetos", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.buckets").isArray());
    }

    @Test
    void lasEvolucionesPendientesSonSoloDeEnfermeriaYAdministracion() throws Exception {
        String enfermeria = token(fixture.usuario("seg-enf", RolNombre.ROLE_ENFERMERIA));
        String base = "/api/v1/hospitalizacion/evoluciones-pendientes";
        mockMvc.perform(get(base).header(HttpHeaders.AUTHORIZATION, enfermeria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get(base).header(HttpHeaders.AUTHORIZATION, tokenAdmin)).andExpect(status().isOk());
        mockMvc.perform(get(base + "?horas=0").header(HttpHeaders.AUTHORIZATION, enfermeria))
                .andExpect(status().isBadRequest());
        for (RolNombre rol : new RolNombre[] {RolNombre.ROLE_PACIENTE, RolNombre.ROLE_RECEPCION, RolNombre.ROLE_CAJA}) {
            mockMvc.perform(get(base).header(HttpHeaders.AUTHORIZATION, token(fixture.usuario("seg-ev", rol))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void cancelarUnaCitaExigeUnMotivoYLoVenElPacienteYElMedico() throws Exception {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        Paciente dueno = fixture.paciente();
        Cita cita = fixture.cita(
                medico,
                dueno,
                LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(9, 0),
                EstadoCita.CONFIRMADA);
        String tokenDueno = token(dueno.getUsuario());
        String url = "/api/v1/citas/" + cita.getId() + "/cancelar";

        mockMvc.perform(put(url).header(HttpHeaders.AUTHORIZATION, tokenDueno)).andExpect(status().isBadRequest());
        enviar(put(url), tokenDueno, "{\"motivo\":\"abc\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Error de validación")));
        enviar(put(url), token(fixture.usuario("seg-rec", RolNombre.ROLE_RECEPCION)), "{\"motivo\":\"Motivo válido\"}")
                .andExpect(status().isForbidden());
        enviar(put(url), tokenDueno, "{\"motivo\":\"No puedo asistir por un viaje\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADA")))
                .andExpect(jsonPath("$.motivoCancelacion", is("No puedo asistir por un viaje")));
        mockMvc.perform(get("/api/v1/citas/" + cita.getId())
                        .header(HttpHeaders.AUTHORIZATION, token(medico.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoCancelacion", is("No puedo asistir por un viaje")));
    }

    @Test
    void anularUnaDispensacionYCancelarUnIngresoTambienExigenMotivo() throws Exception {
        String farmacia = token(fixture.usuario("seg-far", RolNombre.ROLE_FARMACIA));
        String medico = token(fixture.usuario("seg-med", RolNombre.ROLE_MEDICO));
        UUID cualquiera = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/farmacia/dispensaciones/" + cualquiera + "/anular")
                        .header(HttpHeaders.AUTHORIZATION, farmacia))
                .andExpect(status().isBadRequest());
        enviar(put("/api/v1/farmacia/dispensaciones/" + cualquiera + "/anular"), farmacia, "{\"motivo\":\"no\"}")
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/hospitalizacion/ingresos/" + cualquiera + "/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, medico))
                .andExpect(status().isBadRequest());
        enviar(put("/api/v1/hospitalizacion/ingresos/" + cualquiera + "/cancelar"), medico, "{\"motivo\":\"no\"}")
                .andExpect(status().isBadRequest());
        assertThat(cualquiera).isNotNull();
    }
}

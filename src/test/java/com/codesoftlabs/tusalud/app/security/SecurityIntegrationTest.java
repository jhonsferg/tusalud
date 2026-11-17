package com.codesoftlabs.tusalud.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Rol;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.repositories.RolRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    private String tokenPaciente;

    @BeforeEach
    void setUp() {
        Rol rolPaciente = rolRepository
                .findByNombre(RolNombre.ROLE_PACIENTE)
                .orElseGet(() -> rolRepository.save(
                        Rol.builder().nombre(RolNombre.ROLE_PACIENTE).build()));
        Usuario paciente = usuarioRepository.save(usuario("paciente.sec@test.com", EstadoUsuario.ACTIVO, rolPaciente));
        usuarioRepository.save(usuario("inactivo.sec@test.com", EstadoUsuario.INACTIVO, rolPaciente));
        tokenPaciente = jwtUtils.generateToken(UsuarioPrincipal.from(paciente));
    }

    @Test
    void endpointsPublicosNoRequierenToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void sinTokenRetorna401ConFormatoEstandar() throws Exception {
        mockMvc.perform(get("/api/v1/citas/mis-citas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Se requiere autenticación para acceder a este recurso"))
                .andExpect(jsonPath("$.path").value("/api/v1/citas/mis-citas"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/citas/mis-citas").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token inválido"));
    }

    @Test
    void tokenValidoSinRolRequeridoRetorna403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/auditoria").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void tokenValidoConRolCorrectoPasaLaSeguridad() throws Exception {
        mockMvc.perform(get("/api/v1/especialidades").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void losEndpointsDeMfaExigenTokenSalvoElSegundoPaso() throws Exception {
        mockMvc.perform(get("/api/v1/auth/mfa/estado")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/mfa/estado").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mockMvc.perform(post("/api/v1/auth/mfa/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaToken\":\"abc\",\"codigo\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenDeVerificacionMfaNoAutenticaComoTokenDeAcceso() throws Exception {
        String mfaToken = jwtUtils.generateMfaToken(UsuarioPrincipal.from(
                usuarioRepository.findByEmailWithRoles("paciente.sec@test.com").orElseThrow()));

        mockMvc.perform(get("/api/v1/especialidades").header(HttpHeaders.AUTHORIZATION, "Bearer " + mfaToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void losEndpointsDeAdministracionDeRolesSoloLosVeElAdministrador() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/admin/usuarios/00000000-0000-0000-0000-000000000001/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_ADMIN\"]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/medicos/00000000-0000-0000-0000-000000000001/especialidades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especialidadIds\":[\"00000000-0000-0000-0000-000000000001\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorActualizaLasEspecialidadesDeUnMedicoExistente() throws Exception {
        Rol rolAdmin = rolRepository
                .findByNombre(RolNombre.ROLE_ADMIN)
                .orElseGet(() -> rolRepository.save(
                        Rol.builder().nombre(RolNombre.ROLE_ADMIN).build()));
        Usuario admin = usuarioRepository.save(usuario("admin.sec@test.com", EstadoUsuario.ACTIVO, rolAdmin));
        String tokenAdmin = jwtUtils.generateToken(UsuarioPrincipal.from(admin));

        mockMvc.perform(put("/api/v1/medicos/00000000-0000-0000-0000-000000999999/especialidades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especialidadIds\":[\"00000000-0000-0000-0000-000000000001\"]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Médico")));
    }

    @Test
    void elCalendarioPersonalAceptaUnCuerpoMinimoYAislaLosEventosDeCadaUsuario() throws Exception {
        String cuerpo =
                "{\"titulo\":\"Llamar\",\"tipo\":\"PERSONAL\",\"inicio\":\"2027-01-04T09:00:00\",\"fin\":\"2027-01-04T10:00:00\"}";
        mockMvc.perform(post("/api/v1/calendario/eventos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.todoElDia").value(false))
                .andExpect(jsonPath("$.completado").value(false));
        mockMvc.perform(get("/api/v1/calendario/eventos")
                        .param("inicio", "2027-01-01")
                        .param("fin", "2027-01-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Llamar"));
        mockMvc.perform(get("/api/v1/calendario/eventos").param("inicio", "2027-01-01"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/calendario/eventos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.replace("PERSONAL", "BLOQUEO")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/calendario/eventos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.replace("PERSONAL", "REUNION")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tu rol no permite crear eventos de tipo REUNION"));
        mockMvc.perform(get("/api/v1/calendario/tipos").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("PERSONAL"))
                .andExpect(jsonPath("$[1]").value("RECORDATORIO"))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/v1/calendario/tipos")).andExpect(status().isUnauthorized());
    }

    @Test
    void elAltaDePersonalSoloLaHaceElAdministradorYValidaLosRoles() throws Exception {
        String cuerpo =
                "{\"nombre\":\"Rosa\",\"apellido\":\"Medina\",\"email\":\"rosa.sec@tusalud.com\",\"password\":\"Personal123!\",\"roles\":[\"ROLE_FARMACIA\"]}";
        mockMvc.perform(post("/api/v1/admin/personal")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/personal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isUnauthorized());

        Rol rolAdmin = rolRepository
                .findByNombre(RolNombre.ROLE_ADMIN)
                .orElseGet(() -> rolRepository.save(
                        Rol.builder().nombre(RolNombre.ROLE_ADMIN).build()));
        Usuario admin = usuarioRepository.save(usuario("admin.personal@test.com", EstadoUsuario.ACTIVO, rolAdmin));
        String tokenAdmin = jwtUtils.generateToken(UsuarioPrincipal.from(admin));

        mockMvc.perform(post("/api/v1/admin/personal")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_FARMACIA"));
        mockMvc.perform(post("/api/v1/admin/personal")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/admin/personal")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.replace("rosa.sec", "medico.sec").replace("ROLE_FARMACIA", "ROLE_MEDICO")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/admin/personal")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.replace("rosa.sec", "vacio.sec").replace("[\"ROLE_FARMACIA\"]", "[]")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void losPrefijosDeLosModulosClinicosExigenAutenticacion() throws Exception {
        for (String ruta :
                new String[] {"/api/v1/almacen/productos", "/api/v1/farmacia/productos", "/api/v1/laboratorio/examenes"
                }) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void losPrefijosDeFase2ExigenAutenticacionYDelegaranEnLosControladores() throws Exception {
        for (String ruta :
                new String[] {"/api/v1/recepcion/turnos", "/api/v1/caja/comprobantes", "/api/v1/hospitalizacion/camas"
                }) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente))
                    .andExpect(status().is4xxClientError())
                    .andExpect(status().is(org.hamcrest.Matchers.not(401)));
        }
    }

    @Test
    void elAdministradorRegistraPersonalDeRecepcionCajaYEnfermeria() throws Exception {
        Rol rolAdmin = rolRepository
                .findByNombre(RolNombre.ROLE_ADMIN)
                .orElseGet(() -> rolRepository.save(
                        Rol.builder().nombre(RolNombre.ROLE_ADMIN).build()));
        Usuario admin = usuarioRepository.save(usuario("admin.fase2@test.com", EstadoUsuario.ACTIVO, rolAdmin));
        String tokenAdmin = jwtUtils.generateToken(UsuarioPrincipal.from(admin));
        String[] roles = {"ROLE_RECEPCION", "ROLE_CAJA", "ROLE_ENFERMERIA"};
        for (String rol : roles) {
            mockMvc.perform(post("/api/v1/admin/personal")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Ana\",\"apellido\":\"Gómez\",\"email\":\"" + rol.toLowerCase()
                                    + ".sec@tusalud.com\",\"password\":\"Personal123!\",\"roles\":[\"" + rol + "\"]}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.roles[0]").value(rol));
        }
        mockMvc.perform(get("/api/v1/admin/roles").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(14));
        mockMvc.perform(get("/api/v1/admin/roles").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente))
                .andExpect(status().isForbidden());
    }

    @Test
    void losPagosConBilleteraSonDelPacienteYLosPacientesCompartidosDelMedico() throws Exception {
        mockMvc.perform(post("/api/v1/pagos/qr")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/pacientes/00000000-0000-0000-0000-000000000001/compartir")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicoId\":\"00000000-0000-0000-0000-000000000001\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/pagos/qr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citaId\":\"00000000-0000-0000-0000-000000000001\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tokenContieneClaimsEsperados() {
        assertThat(jwtUtils.isTokenValid(tokenPaciente)).isTrue();
        assertThat(jwtUtils.extractUsername(tokenPaciente)).isEqualTo("paciente.sec@test.com");
        assertThat(jwtUtils.extractRoles(tokenPaciente)).containsExactly("ROLE_PACIENTE");
    }

    @Test
    void corsPermitePreflightDesdeAngular() throws Exception {
        mockMvc.perform(options("/api/v1/citas")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));

        mockMvc.perform(options("/api/v1/citas")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticationManagerValidaPasswordBCryptYEstado() {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("paciente.sec@test.com", "Secreto123"));
        assertThat(auth.isAuthenticated()).isTrue();

        assertThatThrownBy(() -> authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken("paciente.sec@test.com", "incorrecta")))
                .isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() -> authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken("inactivo.sec@test.com", "Secreto123")))
                .isInstanceOf(DisabledException.class);
    }

    private Usuario usuario(String email, EstadoUsuario estado, Rol rol) {
        return Usuario.builder()
                .nombre("Test")
                .apellido("Seguridad")
                .email(email)
                .password(passwordEncoder.encode("Secreto123"))
                .estado(estado)
                .roles(Set.of(rol))
                .build();
    }
}

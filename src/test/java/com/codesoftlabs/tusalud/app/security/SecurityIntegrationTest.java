package com.codesoftlabs.tusalud.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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

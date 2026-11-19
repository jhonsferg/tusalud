package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.LoginRequest;
import com.codesoftlabs.tusalud.app.dto.request.RegisterRequest;
import com.codesoftlabs.tusalud.app.dto.response.AuthResponse;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.SesionUsuarioRepository;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.security.JwtUtils;
import com.codesoftlabs.tusalud.app.services.interfaces.AuthService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class AuthServiceImplTest {

    private static final ContextoConexion CONTEXTO =
            new ContextoConexion("10.0.0.5", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0 Safari/537.36");

    @Autowired
    private AuthService authService;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private SesionUsuarioRepository sesionRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private RegisterRequest registro(String email, String dni) {
        return new RegisterRequest(
                "Carlos", "Mendoza", email, "Paciente123!", "987654321", dni, LocalDate.of(1992, 4, 15), null);
    }

    @Test
    void registrarPacienteCreaUsuarioPacienteYDevuelveToken() {
        int n = ServiceTestFixture.siguiente();
        String email = "svc.test.registro" + n + "@test.com";

        AuthResponse respuesta = authService.registrarPaciente(
                registro("  " + email.toUpperCase() + " ", "7" + String.format("%07d", n)), CONTEXTO);

        assertThat(respuesta.email()).isEqualTo(email);
        assertThat(respuesta.tokenType()).isEqualTo("Bearer");
        assertThat(respuesta.roles()).containsExactly(RolNombre.ROLE_PACIENTE.name());
        assertThat(jwtUtils.isTokenValid(respuesta.token())).isTrue();
        Paciente paciente = pacienteRepository.findByUsuarioEmail(email).orElseThrow();
        assertThat(paciente.getHistorialClinicoNumero()).matches("HC-\\d{6}");
        assertThat(paciente.getUsuario().getPassword()).isNotEqualTo("Paciente123!");
    }

    @Test
    void registrarPacienteConEmailDuplicadoLanzaConflict() {
        Usuario existente = fixture.usuario("dup", RolNombre.ROLE_PACIENTE);

        assertThatThrownBy(() -> authService.registrarPaciente(
                        registro(
                                existente.getEmail().toUpperCase(),
                                "6" + String.format("%07d", ServiceTestFixture.siguiente())),
                        CONTEXTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Ya existe un usuario registrado con el email");
    }

    @Test
    void registrarPacienteConDniDuplicadoLanzaConflict() {
        Paciente existente = fixture.paciente();

        assertThatThrownBy(() -> authService.registrarPaciente(
                        registro("svc.test.dni" + ServiceTestFixture.siguiente() + "@test.com", existente.getDni()),
                        CONTEXTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("DNI");
    }

    @Test
    void loginConCredencialesCorrectasDevuelveToken() {
        Usuario usuario = fixture.usuario("login", RolNombre.ROLE_MEDICO);

        AuthResponse respuesta =
                authService.login(new LoginRequest(usuario.getEmail(), ServiceTestFixture.PASSWORD), CONTEXTO);

        assertThat(respuesta.usuarioId()).isEqualTo(usuario.getId());
        assertThat(respuesta.roles()).containsExactly(RolNombre.ROLE_MEDICO.name());
        assertThat(respuesta.expiresIn()).isEqualTo(jwtUtils.getExpirationMs());
        assertThat(jwtUtils.extractUsername(respuesta.token())).isEqualTo(usuario.getEmail());
    }

    @Test
    void loginCreaUnaSesionCuyoTokenIdVaEnElClaimJti() {
        Usuario usuario = fixture.usuario("jti", RolNombre.ROLE_PACIENTE);

        AuthResponse respuesta =
                authService.login(new LoginRequest(usuario.getEmail(), ServiceTestFixture.PASSWORD), CONTEXTO);

        String jti = jwtUtils.parseClaims(respuesta.token()).getId();
        assertThat(jti).isNotBlank();
        assertThat(sesionRepository.findByTokenId(UUID.fromString(jti)))
                .isPresent()
                .get()
                .satisfies(sesion -> {
                    assertThat(sesion.getIp()).isEqualTo("10.0.0.5");
                    assertThat(sesion.getDispositivo()).isEqualTo("Chrome en Windows");
                    assertThat(sesion.isRevocada()).isFalse();
                });
    }

    @Test
    void loginConCredencialesIncorrectasLanzaBadCredentials() {
        Usuario usuario = fixture.usuario("login", RolNombre.ROLE_PACIENTE);

        assertThatThrownBy(() -> authService.login(new LoginRequest(usuario.getEmail(), "incorrecta"), CONTEXTO))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> authService.login(new LoginRequest("svc.test.noexiste@test.com", "x"), CONTEXTO))
                .isInstanceOf(BadCredentialsException.class);
    }
}

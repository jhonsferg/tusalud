package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.LoginRequest;
import com.codesoftlabs.tusalud.app.dto.request.RefreshTokenRequest;
import com.codesoftlabs.tusalud.app.dto.response.AuthResponse;
import com.codesoftlabs.tusalud.app.entities.SesionUsuario;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.RefreshTokenRepository;
import com.codesoftlabs.tusalud.app.repositories.SesionUsuarioRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.security.JwtUtils;
import com.codesoftlabs.tusalud.app.services.interfaces.AuthService;
import com.codesoftlabs.tusalud.app.services.interfaces.SesionService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class RefreshTokenServiceTest {

    private static final ContextoConexion CONTEXTO = new ContextoConexion("10.0.0.9", "JUnit");

    @Autowired
    private AuthService authService;

    @Autowired
    private SesionService sesionService;

    @Autowired
    private SesionUsuarioRepository sesionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ServiceTestFixture fixture;

    private Usuario usuario;
    private AuthResponse inicial;

    @BeforeEach
    void setUp() {
        usuario = fixture.usuario("refresh", RolNombre.ROLE_PACIENTE);
        inicial = authService.login(new LoginRequest(usuario.getEmail(), ServiceTestFixture.PASSWORD), CONTEXTO);
    }

    @Test
    void elLoginEntregaUnRefreshTokenOpacoConSuVigencia() {
        assertThat(inicial.refreshToken()).hasSizeGreaterThanOrEqualTo(40).doesNotContain(".");
        assertThat(inicial.refreshExpiresIn()).isEqualTo(jwtUtils.getRefreshExpirationMs());
        assertThat(inicial.expiresIn()).isEqualTo(jwtUtils.getExpirationMs());
        assertThat(refreshTokenRepository.findAll())
                .anySatisfy(
                        token -> assertThat(token.getTokenHash()).hasSize(64).isNotEqualTo(inicial.refreshToken()));
    }

    @Test
    void refrescarEntregaTokensNuevosYConservaLaMismaSesion() {
        UUID sesionOriginal =
                UUID.fromString(jwtUtils.parseClaims(inicial.token()).getId());

        AuthResponse renovado = authService.refrescar(new RefreshTokenRequest(inicial.refreshToken()));

        assertThat(renovado.refreshToken()).isNotEqualTo(inicial.refreshToken());
        assertThat(UUID.fromString(jwtUtils.parseClaims(renovado.token()).getId()))
                .isEqualTo(sesionOriginal);
        assertThat(renovado.email()).isEqualTo(usuario.getEmail());
        assertThat(renovado.roles()).containsExactly("ROLE_PACIENTE");
        assertThat(sesionRepository.findByTokenId(sesionOriginal).orElseThrow().estaActiva(LocalDateTime.now()))
                .isTrue();
        assertThat(sesionRepository.findAll().stream()
                        .filter(s -> s.getUsuario().getId().equals(usuario.getId()))
                        .count())
                .isEqualTo(1);
    }

    @Test
    void laCadenaDeRenovacionesSigueFuncionando() {
        AuthResponse actual = inicial;
        for (int i = 0; i < 3; i++) {
            actual = authService.refrescar(new RefreshTokenRequest(actual.refreshToken()));
        }
        assertThat(actual.token()).isNotBlank();
    }

    @Test
    void reutilizarUnRefreshTokenRotadoRevocaLaFamiliaYLaSesionYNotifica() {
        AuthResponse renovado = authService.refrescar(new RefreshTokenRequest(inicial.refreshToken()));
        UUID sesionId = UUID.fromString(jwtUtils.parseClaims(renovado.token()).getId());

        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest(inicial.refreshToken())))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(sesionRepository.findByTokenId(sesionId).orElseThrow().isRevocada())
                .isTrue();
        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest(renovado.refreshToken())))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(notificacionRepository.findAll())
                .anyMatch(n -> n.getTipo() == TipoNotificacion.SESION_SOSPECHOSA
                        && n.getUsuario().getId().equals(usuario.getId()));
    }

    @Test
    void rechazaTokensInexistentesVaciosOExpirados() {
        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest("no-existe")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest("  ")))
                .isInstanceOf(BadCredentialsException.class);

        refreshTokenRepository
                .findAll()
                .forEach(t -> t.setExpiraEn(LocalDateTime.now().minusMinutes(1)));
        refreshTokenRepository.flush();
        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest(inicial.refreshToken())))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void cerrarSesionRevocaElRefreshToken() {
        UUID sesionId = UUID.fromString(jwtUtils.parseClaims(inicial.token()).getId());

        authService.cerrarSesion(sesionId);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest(inicial.refreshToken())))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(refreshTokenRepository.findAll()).allMatch(t -> t.isRevocado());
    }

    @Test
    void revocarTodasLasSesionesDelUsuarioBloqueaLaRenovacion() {
        sesionService.revocarTodasDelUsuario(usuario.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest(inicial.refreshToken())))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void unUsuarioInactivoNoPuedeRenovar() {
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.saveAndFlush(usuario);

        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest(inicial.refreshToken())))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void laSesionRenovadaExtiendeSuVencimiento() {
        SesionUsuario sesion = sesionRepository
                .findByTokenId(
                        UUID.fromString(jwtUtils.parseClaims(inicial.token()).getId()))
                .orElseThrow();
        sesion.setExpiraEn(LocalDateTime.now().plusMinutes(1));
        sesionRepository.saveAndFlush(sesion);

        authService.refrescar(new RefreshTokenRequest(inicial.refreshToken()));

        assertThat(sesion.getExpiraEn()).isAfter(LocalDateTime.now().plusDays(6));
    }
}

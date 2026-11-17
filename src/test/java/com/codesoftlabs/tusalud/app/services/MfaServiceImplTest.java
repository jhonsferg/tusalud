package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.LoginRequest;
import com.codesoftlabs.tusalud.app.dto.request.MfaVerificarRequest;
import com.codesoftlabs.tusalud.app.dto.response.AuthResponse;
import com.codesoftlabs.tusalud.app.dto.response.MfaConfiguracionResponse;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.DemasiadosIntentosException;
import com.codesoftlabs.tusalud.app.repositories.MfaUsuarioRepository;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.security.JwtUtils;
import com.codesoftlabs.tusalud.app.security.TotpService;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuthService;
import com.codesoftlabs.tusalud.app.services.interfaces.MfaService;
import com.codesoftlabs.tusalud.app.services.interfaces.PerfilService;
import java.time.Instant;
import java.util.List;
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
class MfaServiceImplTest {

    private static final ContextoConexion CONTEXTO =
            new ContextoConexion("10.0.0.9", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0 Safari/537.36");

    @Autowired
    private MfaService mfaService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PerfilService perfilService;

    @Autowired
    private TotpService totpService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private MfaUsuarioRepository mfaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private String codigoActual(String secreto) {
        return totpService.generarCodigo(secreto, Instant.now());
    }

    private String codigoInvalido(String secreto) {
        String valido = codigoActual(secreto);
        return valido.equals("000000") ? "000001" : "000000";
    }

    private List<String> activarMfa(Usuario usuario, String[] secretoSalida) {
        UsuarioPrincipal actor = fixture.principal(usuario);
        MfaConfiguracionResponse configuracion = mfaService.configurar(actor);
        secretoSalida[0] = configuracion.secreto();
        return mfaService.activar(actor, codigoActual(configuracion.secreto())).codigosRecuperacion();
    }

    @Test
    void activaLaVerificacionYEntregaCodigosDeRecuperacion() {
        Usuario usuario = fixture.usuario("mfa", RolNombre.ROLE_PACIENTE);
        UsuarioPrincipal actor = fixture.principal(usuario);

        assertThat(mfaService.estado(actor).activo()).isFalse();
        MfaConfiguracionResponse configuracion = mfaService.configurar(actor);
        assertThat(configuracion.otpauthUri()).contains("secret=" + configuracion.secreto());
        assertThatThrownBy(() -> mfaService.activar(actor, codigoInvalido(configuracion.secreto())))
                .isInstanceOf(DatosInvalidosException.class);
        assertThat(mfaService.estado(actor).activo()).isFalse();

        List<String> codigos =
                mfaService.activar(actor, codigoActual(configuracion.secreto())).codigosRecuperacion();

        assertThat(codigos).hasSize(8).allMatch(c -> c.matches("[A-Z2-9]{4}-[A-Z2-9]{4}"));
        assertThat(mfaService.estado(actor).activo()).isTrue();
        assertThat(mfaService.estado(actor).codigosRecuperacionRestantes()).isEqualTo(8);
        assertThat(perfilService.obtener(actor).mfaActivo()).isTrue();
        assertThatThrownBy(() -> mfaService.configurar(actor)).isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(() -> mfaService.activar(actor, "123456")).isInstanceOf(BusinessLogicException.class);
        assertThat(notificacionRepository.findAll())
                .anyMatch(n ->
                        n.getUsuario().getId().equals(usuario.getId()) && n.getTipo() == TipoNotificacion.MFA_ACTIVADO);
    }

    @Test
    void elSecretoSeGuardaCifrado() {
        Usuario usuario = fixture.usuario("mfa", RolNombre.ROLE_PACIENTE);

        MfaConfiguracionResponse configuracion = mfaService.configurar(fixture.principal(usuario));

        assertThat(mfaRepository.findByUsuarioId(usuario.getId()).orElseThrow().getSecretoCifrado())
                .isNotEmpty()
                .doesNotContain(configuracion.secreto());
    }

    @Test
    void elInicioDeSesionExigeElSegundoPasoYAceptaTotpOCodigoDeRecuperacion() {
        Usuario usuario = fixture.usuario("mfa", RolNombre.ROLE_PACIENTE);
        String[] secreto = new String[1];
        List<String> recuperacion = activarMfa(usuario, secreto);

        AuthResponse primerPaso =
                authService.login(new LoginRequest(usuario.getEmail(), ServiceTestFixture.PASSWORD), CONTEXTO);

        assertThat(primerPaso.mfaRequerido()).isTrue();
        assertThat(primerPaso.token()).isNull();
        assertThat(primerPaso.mfaToken()).isNotBlank();
        assertThatThrownBy(() -> jwtUtils.extractMfaUserId("no-es-un-jwt")).isInstanceOf(RuntimeException.class);

        AuthResponse conTotp = authService.verificarMfa(
                new MfaVerificarRequest(primerPaso.mfaToken(), codigoActual(secreto[0])), CONTEXTO);
        assertThat(conTotp.mfaRequerido()).isFalse();
        assertThat(conTotp.token()).isNotBlank();
        assertThat(conTotp.roles()).containsExactly(RolNombre.ROLE_PACIENTE.name());

        AuthResponse conRecuperacion = authService.verificarMfa(
                new MfaVerificarRequest(
                        authService
                                .login(new LoginRequest(usuario.getEmail(), ServiceTestFixture.PASSWORD), CONTEXTO)
                                .mfaToken(),
                        recuperacion.get(0).toLowerCase()),
                CONTEXTO);
        assertThat(conRecuperacion.token()).isNotBlank();
        assertThat(mfaService.estado(fixture.principal(usuario)).codigosRecuperacionRestantes())
                .isEqualTo(7);

        String otroToken = authService
                .login(new LoginRequest(usuario.getEmail(), ServiceTestFixture.PASSWORD), CONTEXTO)
                .mfaToken();
        assertThatThrownBy(() ->
                        authService.verificarMfa(new MfaVerificarRequest(otroToken, recuperacion.get(0)), CONTEXTO))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> authService.verificarMfa(
                        new MfaVerificarRequest(otroToken, codigoInvalido(secreto[0])), CONTEXTO))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void unTokenDeAccesoNoSirveComoTokenDeVerificacion() {
        Usuario usuario = fixture.usuario("mfa", RolNombre.ROLE_PACIENTE);

        AuthResponse acceso =
                authService.login(new LoginRequest(usuario.getEmail(), ServiceTestFixture.PASSWORD), CONTEXTO);

        assertThat(acceso.mfaRequerido()).isFalse();
        assertThat(acceso.mfaToken()).isNull();
        assertThatThrownBy(() -> authService.verificarMfa(new MfaVerificarRequest(acceso.token(), "123456"), CONTEXTO))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void cincoFallosBloqueanTemporalmenteLaVerificacion() {
        Usuario usuario = fixture.usuario("mfa", RolNombre.ROLE_PACIENTE);
        String[] secreto = new String[1];
        activarMfa(usuario, secreto);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> mfaService.validarSegundoPaso(usuario.getId(), codigoInvalido(secreto[0])))
                    .isInstanceOf(BadCredentialsException.class);
        }

        assertThatThrownBy(() -> mfaService.validarSegundoPaso(usuario.getId(), codigoActual(secreto[0])))
                .isInstanceOf(DemasiadosIntentosException.class);
        assertThat(mfaRepository.findByUsuarioId(usuario.getId()).orElseThrow().getBloqueadoHasta())
                .isNotNull();
    }

    @Test
    void regenerarInvalidaLosCodigosAnteriores() {
        Usuario usuario = fixture.usuario("mfa", RolNombre.ROLE_MEDICO);
        UsuarioPrincipal actor = fixture.principal(usuario);
        String[] secreto = new String[1];
        List<String> anteriores = activarMfa(usuario, secreto);

        assertThatThrownBy(() -> mfaService.regenerarCodigos(actor, codigoInvalido(secreto[0])))
                .isInstanceOf(DatosInvalidosException.class);
        List<String> nuevos =
                mfaService.regenerarCodigos(actor, codigoActual(secreto[0])).codigosRecuperacion();

        assertThat(nuevos).hasSize(8).doesNotContainAnyElementsOf(anteriores);
        assertThatThrownBy(() -> mfaService.validarSegundoPaso(usuario.getId(), anteriores.get(0)))
                .isInstanceOf(BadCredentialsException.class);
        mfaService.validarSegundoPaso(usuario.getId(), nuevos.get(0));
    }

    @Test
    void desactivarExigePasswordYCodigo() {
        Usuario usuario = fixture.usuario("mfa", RolNombre.ROLE_ADMIN);
        UsuarioPrincipal actor = fixture.principal(usuario);
        String[] secreto = new String[1];
        activarMfa(usuario, secreto);

        assertThatThrownBy(() -> mfaService.desactivar(actor, "incorrecta", codigoActual(secreto[0])))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> mfaService.desactivar(actor, ServiceTestFixture.PASSWORD, codigoInvalido(secreto[0])))
                .isInstanceOf(DatosInvalidosException.class);
        assertThat(mfaService.estado(actor).activo()).isTrue();

        mfaService.desactivar(actor, ServiceTestFixture.PASSWORD, codigoActual(secreto[0]));

        assertThat(mfaService.estado(actor).activo()).isFalse();
        assertThat(authService
                        .login(new LoginRequest(usuario.getEmail(), ServiceTestFixture.PASSWORD), CONTEXTO)
                        .mfaRequerido())
                .isFalse();
        assertThatThrownBy(() -> mfaService.desactivar(actor, ServiceTestFixture.PASSWORD, "123456"))
                .isInstanceOf(BusinessLogicException.class);
    }
}

package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.PreferenciasRequest;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.SesionResponse;
import com.codesoftlabs.tusalud.app.entities.SesionUsuario;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.SesionUsuarioRepository;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.security.DispositivoParser;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.PerfilService;
import com.codesoftlabs.tusalud.app.services.interfaces.SesionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class SesionServiceImplTest {

    private static final String UA_CHROME_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String UA_SAFARI_IPHONE =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

    @Autowired
    private SesionService sesionService;

    @Autowired
    private SesionUsuarioRepository sesionRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private PerfilService perfilService;

    @Autowired
    private ServiceTestFixture fixture;

    @Test
    void iniciarSesionGuardaDispositivoIpYVigencia() {
        Usuario usuario = fixture.usuario("sesion", RolNombre.ROLE_PACIENTE);

        UUID tokenId = sesionService.iniciarSesion(usuario, new ContextoConexion("190.1.2.3", UA_CHROME_WINDOWS));

        SesionUsuario sesion = sesionRepository.findByTokenId(tokenId).orElseThrow();
        assertThat(sesion.getDispositivo()).isEqualTo("Chrome en Windows");
        assertThat(sesion.getIp()).isEqualTo("190.1.2.3");
        assertThat(sesion.getExpiraEn()).isAfter(sesion.getCreadaEn());
        assertThat(sesion.isRevocada()).isFalse();
    }

    @Test
    void primerInicioDeSesionNoGeneraAlerta() {
        Usuario usuario = fixture.usuario("primera", RolNombre.ROLE_PACIENTE);

        sesionService.iniciarSesion(usuario, new ContextoConexion("190.1.2.3", UA_CHROME_WINDOWS));

        assertThat(notificacionService.listar(fixture.principal(usuario), false, 10))
                .isEmpty();
    }

    @Test
    void inicioPosteriorGeneraAlertaConDispositivoEIp() {
        Usuario usuario = fixture.usuario("alerta", RolNombre.ROLE_PACIENTE);
        sesionService.iniciarSesion(usuario, new ContextoConexion("190.1.2.3", UA_CHROME_WINDOWS));

        sesionService.iniciarSesion(usuario, new ContextoConexion("200.1.1.1", UA_SAFARI_IPHONE));

        assertThat(notificacionService.listar(fixture.principal(usuario), false, 10))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.tipo()).isEqualTo(TipoNotificacion.INICIO_SESION);
                    assertThat(n.titulo()).isEqualTo("Nuevo inicio de sesión");
                    assertThat(n.mensaje()).isEqualTo("Se inició sesión desde Safari en iOS con la IP 200.1.1.1.");
                    assertThat(n.enlace()).isEqualTo("/perfil");
                });
    }

    @Test
    void alertasDesactivadasNoGeneranNotificacion() {
        Usuario usuario = fixture.usuario("sinalerta", RolNombre.ROLE_PACIENTE);
        perfilService.actualizarPreferencias(fixture.principal(usuario), new PreferenciasRequest(true, 24, false));
        sesionService.iniciarSesion(usuario, new ContextoConexion("190.1.2.3", UA_CHROME_WINDOWS));

        sesionService.iniciarSesion(usuario, new ContextoConexion("200.1.1.1", UA_SAFARI_IPHONE));

        List<NotificacionResponse> notificaciones = notificacionService.listar(fixture.principal(usuario), false, 10);
        assertThat(notificaciones).isEmpty();
    }

    @Test
    void sesionActivaSoloMientrasNoEsteRevocada() {
        Usuario usuario = fixture.usuario("activa", RolNombre.ROLE_PACIENTE);
        UUID tokenId = sesionService.iniciarSesion(usuario, new ContextoConexion("190.1.2.3", UA_CHROME_WINDOWS));

        assertThat(sesionService.sesionActiva(tokenId)).isTrue();
        assertThat(sesionService.sesionActiva(UUID.randomUUID())).isFalse();

        sesionService.cerrarSesion(tokenId);

        assertThat(sesionService.sesionActiva(tokenId)).isFalse();
    }

    @Test
    void listarActivasMarcaLaSesionActual() {
        Usuario usuario = fixture.usuario("listado", RolNombre.ROLE_PACIENTE);
        UUID actual = sesionService.iniciarSesion(usuario, new ContextoConexion("190.1.2.3", UA_CHROME_WINDOWS));
        sesionService.iniciarSesion(usuario, new ContextoConexion("200.1.1.1", UA_SAFARI_IPHONE));
        UsuarioPrincipal principal = fixture.principal(usuario);

        List<SesionResponse> sesiones = sesionService.listarActivas(principal, actual);

        assertThat(sesiones).hasSize(2);
        assertThat(sesiones)
                .filteredOn(SesionResponse::actual)
                .singleElement()
                .satisfies(s -> assertThat(s.dispositivo()).isEqualTo("Chrome en Windows"));
    }

    @Test
    void revocarTodasExceptoActualConservaLaSesionActual() {
        Usuario usuario = fixture.usuario("revocar", RolNombre.ROLE_PACIENTE);
        UUID actual = sesionService.iniciarSesion(usuario, new ContextoConexion("190.1.2.3", UA_CHROME_WINDOWS));
        sesionService.iniciarSesion(usuario, new ContextoConexion("200.1.1.1", UA_SAFARI_IPHONE));
        UsuarioPrincipal principal = fixture.principal(usuario);

        sesionService.revocarTodasExceptoActual(principal, actual);

        assertThat(sesionService.listarActivas(principal, actual))
                .singleElement()
                .satisfies(s -> assertThat(s.actual()).isTrue());
    }

    @Test
    void revocarSesionDeOtroUsuarioLanzaNotFound() {
        Usuario dueno = fixture.usuario("dueno.ses", RolNombre.ROLE_PACIENTE);
        Usuario otro = fixture.usuario("otro.ses", RolNombre.ROLE_PACIENTE);
        UUID tokenId = sesionService.iniciarSesion(dueno, new ContextoConexion("190.1.2.3", UA_CHROME_WINDOWS));
        Long sesionId = sesionRepository.findByTokenId(tokenId).orElseThrow().getId();

        assertThatThrownBy(() -> sesionService.revocar(fixture.principal(otro), sesionId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(sesionService.sesionActiva(tokenId)).isTrue();
    }

    @Test
    void dispositivoSeDeduceDelUserAgent() {
        assertThat(DispositivoParser.describir(UA_CHROME_WINDOWS)).isEqualTo("Chrome en Windows");
        assertThat(DispositivoParser.describir(UA_SAFARI_IPHONE)).isEqualTo("Safari en iOS");
        assertThat(DispositivoParser.describir(UA_CHROME_WINDOWS + " Edg/120.0.0.0"))
                .isEqualTo("Edge en Windows");
        assertThat(DispositivoParser.describir(null)).isEqualTo("Dispositivo desconocido");
    }
}

package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.entities.EventoSeguridad;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.SeveridadSeguridad;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEventoSeguridad;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.repositories.EventoSeguridadRepository;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.services.interfaces.EventoSeguridadService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
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
class EventoSeguridadServiceTest {

    @Autowired
    private EventoSeguridadService eventoService;

    @Autowired
    private EventoSeguridadRepository eventoRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = fixture.usuario("evt", RolNombre.ROLE_MEDICO);
    }

    @Test
    void laDescargaMasivaSeRegistraUnaSolaVezAlLlegarAlUmbral() {
        for (int i = 0; i < 45; i++) {
            eventoService.registrarDescarga(usuario.getId());
        }

        var pagina =
                eventoService.listar(TipoEventoSeguridad.DESCARGA_MASIVA, null, null, null, usuario.getId(), 0, 10);
        assertThat(pagina.total()).isEqualTo(1);
        assertThat(pagina.contenido().get(0).severidad()).isEqualTo("ALTA");
        assertThat(pagina.contenido().get(0).detalle()).contains("30 descargas");
    }

    @Test
    void laIpSeEnmascaraYElDispositivoSeResume() {
        eventoService.registrar(
                TipoEventoSeguridad.DISPOSITIVO_NUEVO,
                usuario.getId(),
                null,
                "Dispositivo nuevo",
                new ContextoConexion(
                        "192.168.15.77",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36"));
        eventoService.registrar(
                TipoEventoSeguridad.DISPOSITIVO_NUEVO,
                usuario.getId(),
                null,
                "Dispositivo nuevo",
                new ContextoConexion("2001:db8:85a3:8d3:1319:8a2e:370:7348", null));

        var eventos = eventoService
                .listar(TipoEventoSeguridad.DISPOSITIVO_NUEVO, null, null, null, usuario.getId(), 0, 10)
                .contenido();
        assertThat(eventos).extracting(e -> e.ip()).contains("192.168.15.x", "2001:db8:85a3:xxxx");
        assertThat(eventos.stream()
                        .map(e -> e.dispositivo())
                        .filter(d -> d != null)
                        .findFirst()
                        .orElseThrow())
                .doesNotContain("Mozilla");
    }

    @Test
    void laPurgaEliminaSoloLosEventosMasAntiguosQueLaRetencion() {
        eventoRepository.save(EventoSeguridad.builder()
                .tipo(TipoEventoSeguridad.LOGIN_FALLIDO)
                .severidad(SeveridadSeguridad.AVISO)
                .usuario(usuario)
                .detalle("antiguo")
                .creadoEn(LocalDateTime.now().minusDays(200))
                .build());
        eventoRepository.save(EventoSeguridad.builder()
                .tipo(TipoEventoSeguridad.LOGIN_FALLIDO)
                .severidad(SeveridadSeguridad.AVISO)
                .usuario(usuario)
                .detalle("reciente")
                .creadoEn(LocalDateTime.now().minusDays(2))
                .build());

        int eliminados = eventoService.purgar();

        assertThat(eliminados).isGreaterThanOrEqualTo(1);
        var restantes = eventoService
                .listar(TipoEventoSeguridad.LOGIN_FALLIDO, null, null, null, usuario.getId(), 0, 10)
                .contenido();
        assertThat(restantes).extracting(e -> e.detalle()).containsExactly("reciente");
    }

    @Test
    void laConsultaValidaElRangoYElTamano() {
        assertThatThrownBy(() -> eventoService.listar(null, null, null, null, null, 0, 0))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> eventoService.listar(
                        null, null, LocalDate.now(), LocalDate.now().minusDays(1), null, 0, 10))
                .isInstanceOf(DatosInvalidosException.class);
    }
}

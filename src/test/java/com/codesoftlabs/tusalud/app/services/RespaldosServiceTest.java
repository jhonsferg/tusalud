package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.RespaldoEjecucionRequest;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.EstadoRespaldosResponse;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.AlcanceRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.CategoriaRetencion;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEjecucionRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.TipoEjecucionRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.respaldos.RespaldosService;
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
class RespaldosServiceTest {

    private static final String TOKEN = "token-de-pruebas-de-respaldo";

    @Autowired
    private RespaldosService servicio;

    @Autowired
    private NotificacionService notificaciones;

    @Autowired
    private ServiceTestFixture fixture;

    private Usuario admin;
    private Usuario operaciones;
    private Usuario paciente;

    @BeforeEach
    void setUp() {
        admin = fixture.usuario("resp-adm", RolNombre.ROLE_ADMIN);
        operaciones = fixture.usuario("resp-ops", RolNombre.ROLE_OPERACIONES);
        paciente = fixture.usuario("resp-pac", RolNombre.ROLE_PACIENTE);
    }

    private RespaldoEjecucionRequest ejecucion(
            TipoEjecucionRespaldo tipo, EstadoEjecucionRespaldo estado, LocalDateTime fin) {
        return new RespaldoEjecucionRequest(
                tipo,
                estado,
                AlcanceRespaldo.COMPLETO,
                tipo == TipoEjecucionRespaldo.RESPALDO ? CategoriaRetencion.DIARIO : null,
                fin.minusSeconds(42),
                fin,
                1024L,
                "tusalud-20260101.dump.enc",
                "a".repeat(64),
                23,
                70,
                12,
                false,
                null);
    }

    private long avisos(Usuario usuario, TipoNotificacion tipo) {
        return notificaciones.listar(fixture.principal(usuario), false, 100).stream()
                .filter(n -> n.tipo() == tipo)
                .count();
    }

    @Test
    void sinTokenOConUnoIncorrectoNoSeRegistraNada() {
        var request = ejecucion(TipoEjecucionRespaldo.RESPALDO, EstadoEjecucionRespaldo.EXITOSO, LocalDateTime.now());
        assertThatThrownBy(() -> servicio.registrar(null, request)).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> servicio.registrar("otro-token", request))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(servicio.listar(null, 50)).isEmpty();
    }

    @Test
    void unaEjecucionNoPuedeTerminarAntesDeEmpezar() {
        LocalDateTime fin = LocalDateTime.now();
        var invalida = new RespaldoEjecucionRequest(
                TipoEjecucionRespaldo.RESPALDO,
                EstadoEjecucionRespaldo.EXITOSO,
                AlcanceRespaldo.BASE,
                null,
                fin,
                fin.minusMinutes(1),
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null);
        assertThatThrownBy(() -> servicio.registrar(TOKEN, invalida)).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void unRespaldoYUnaPruebaExitososDejanElEstadoEnVerde() {
        LocalDateTime ahora = LocalDateTime.now();
        var respaldo = servicio.registrar(
                TOKEN, ejecucion(TipoEjecucionRespaldo.RESPALDO, EstadoEjecucionRespaldo.EXITOSO, ahora.minusHours(2)));
        servicio.registrar(
                TOKEN,
                ejecucion(
                        TipoEjecucionRespaldo.PRUEBA_RESTAURACION,
                        EstadoEjecucionRespaldo.EXITOSO,
                        ahora.minusHours(1)));
        assertThat(respaldo.duracionSeg()).isEqualTo(42);

        EstadoRespaldosResponse estado = servicio.estado(ahora);
        assertThat(estado.semaforo()).isEqualTo("VERDE");
        assertThat(estado.respaldoVencido()).isFalse();
        assertThat(estado.pruebaVencida()).isFalse();
        assertThat(estado.ultimoRespaldoExitoso().checksum()).hasSize(64);
        assertThat(estado.horasDesdeUltimoRespaldo()).isEqualTo(2);
        assertThat(estado.rpoObjetivoHoras()).isEqualTo(24);
        assertThat(estado.rtoObjetivoMinutos()).isEqualTo(60);
        assertThat(servicio.listar(TipoEjecucionRespaldo.RESPALDO, 10)).hasSize(1);
        assertThat(servicio.listar(null, 10)).hasSize(2);
    }

    @Test
    void sinPruebaDeRestauracionElEstadoEsAmarillo() {
        LocalDateTime ahora = LocalDateTime.now();
        servicio.registrar(
                TOKEN, ejecucion(TipoEjecucionRespaldo.RESPALDO, EstadoEjecucionRespaldo.EXITOSO, ahora.minusHours(1)));
        assertThat(servicio.estado(ahora).semaforo()).isEqualTo("AMARILLO");
    }

    @Test
    void unRespaldoFallidoAvisaALosAdministradoresYAOperacionesPeroNoAOtros() {
        servicio.registrar(
                TOKEN, ejecucion(TipoEjecucionRespaldo.RESPALDO, EstadoEjecucionRespaldo.FALLIDO, LocalDateTime.now()));
        assertThat(avisos(admin, TipoNotificacion.RESPALDO_FALLIDO)).isEqualTo(1);
        assertThat(avisos(operaciones, TipoNotificacion.RESPALDO_FALLIDO)).isEqualTo(1);
        assertThat(avisos(paciente, TipoNotificacion.RESPALDO_FALLIDO)).isZero();

        servicio.registrar(
                TOKEN,
                ejecucion(
                        TipoEjecucionRespaldo.PRUEBA_RESTAURACION,
                        EstadoEjecucionRespaldo.FALLIDO,
                        LocalDateTime.now()));
        assertThat(avisos(admin, TipoNotificacion.RESPALDO_FALLIDO)).isEqualTo(1);
        assertThat(servicio.estado().semaforo()).isEqualTo("ROJO");
    }

    @Test
    void despuesDeUnFalloUnRespaldoExitosoPosteriorVuelveAlEstadoNormal() {
        LocalDateTime ahora = LocalDateTime.now();
        servicio.registrar(
                TOKEN, ejecucion(TipoEjecucionRespaldo.RESPALDO, EstadoEjecucionRespaldo.FALLIDO, ahora.minusHours(3)));
        assertThat(servicio.estado(ahora).semaforo()).isEqualTo("ROJO");
        servicio.registrar(
                TOKEN, ejecucion(TipoEjecucionRespaldo.RESPALDO, EstadoEjecucionRespaldo.EXITOSO, ahora.minusHours(1)));
        assertThat(servicio.estado(ahora).semaforo()).isNotEqualTo("ROJO");
    }

    @Test
    void unRespaldoViejoQuedaVencidoYGeneraUnaSolaAlertaPorVentana() {
        LocalDateTime ahora = LocalDateTime.now();
        servicio.registrar(
                TOKEN,
                ejecucion(TipoEjecucionRespaldo.RESPALDO, EstadoEjecucionRespaldo.EXITOSO, ahora.minusHours(30)));
        EstadoRespaldosResponse estado = servicio.estado(ahora);
        assertThat(estado.respaldoVencido()).isTrue();
        assertThat(estado.semaforo()).isEqualTo("ROJO");

        assertThat(servicio.evaluarVencimientos(ahora)).isGreaterThanOrEqualTo(2);
        assertThat(avisos(admin, TipoNotificacion.RESPALDO_VENCIDO)).isEqualTo(1);
        assertThat(servicio.evaluarVencimientos(ahora)).isZero();
        assertThat(avisos(admin, TipoNotificacion.RESPALDO_VENCIDO)).isEqualTo(1);
    }

    @Test
    void sinNingunRespaldoRecienArrancadoElSistemaNoMarcaVencimiento() {
        EstadoRespaldosResponse estado = servicio.estado();
        assertThat(estado.ultimoRespaldoExitoso()).isNull();
        assertThat(estado.respaldoVencido()).isFalse();
        assertThat(estado.semaforo()).isEqualTo("AMARILLO");
        assertThat(servicio.estado(LocalDateTime.now().plusHours(30)).respaldoVencido())
                .isTrue();
    }
}

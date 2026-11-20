package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.AccionRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.AnalisisRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.CausaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.CierreEventoRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.EventoRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.GuiaRequest;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.EventoDetalleResponse;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.EventoResumenResponse;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.MetodoAnalisis;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.SeveridadEvento;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.TipoEvento;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.calidad.EventosAdversosService;
import com.codesoftlabs.tusalud.app.services.calidad.GuiasClinicasService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
class CalidadServiceTest {

    @Autowired
    private EventosAdversosService eventos;

    @Autowired
    private GuiasClinicasService guias;

    @Autowired
    private ServiceTestFixture fixture;

    private UsuarioPrincipal enfermera;
    private UsuarioPrincipal otraEnfermera;
    private UsuarioPrincipal calidad;
    private UsuarioPrincipal gerencia;
    private UsuarioPrincipal admin;

    @BeforeEach
    void setUp() {
        enfermera = fixture.principal(fixture.usuario("enf", RolNombre.ROLE_ENFERMERIA));
        otraEnfermera = fixture.principal(fixture.usuario("enf", RolNombre.ROLE_ENFERMERIA));
        calidad = fixture.principal(fixture.usuario("cal", RolNombre.ROLE_CALIDAD));
        gerencia = fixture.principal(fixture.usuario("ger", RolNombre.ROLE_GERENCIA));
        admin = fixture.principal(fixture.usuario("adm", RolNombre.ROLE_ADMIN));
    }

    private EventoRequest evento(boolean anonimo) {
        return new EventoRequest(
                TipoEvento.EVENTO_ADVERSO,
                SeveridadEvento.MODERADO,
                "Hospitalización",
                "Caída de una paciente al levantarse de la cama",
                anonimo,
                LocalDateTime.now().minusHours(2),
                null,
                null,
                null);
    }

    private AnalisisRequest analisis() {
        return new AnalisisRequest(
                MetodoAnalisis.CINCO_PORQUES,
                List.of(
                        new CausaRequest("PORQUE", "No había barandas"),
                        new CausaRequest("PORQUE", "No se evaluó el riesgo de caída")),
                "Falta de evaluación del riesgo al ingreso",
                "Caída sin lesión grave");
    }

    @Test
    void elReporteSeNumeraYSoloLoVenElNotificanteYLaGestion() {
        EventoResumenResponse creado = eventos.notificar(enfermera, evento(false));

        assertThat(creado.codigo()).startsWith("EA-" + LocalDate.now().getYear() + "-");
        assertThat(eventos.mios(enfermera)).hasSize(1);
        assertThat(eventos.mios(otraEnfermera)).isEmpty();
        assertThat(eventos.detalle(calidad, creado.id()).descripcion()).contains("Caída");
        assertThat(eventos.detalle(gerencia, creado.id()).notificante()).isNotNull();
        assertThatThrownBy(() -> eventos.detalle(otraEnfermera, creado.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> eventos.listar(enfermera, null)).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void unReporteAnonimoOcultaAlNotificanteALaGestion() {
        EventoResumenResponse creado = eventos.notificar(enfermera, evento(true));

        assertThat(eventos.detalle(calidad, creado.id()).notificante()).isNull();
        assertThat(eventos.listar(calidad, null).get(0).notificante()).isNull();
        assertThat(eventos.mios(enfermera).get(0).notificante()).isNotNull();
    }

    @Test
    void elReporteAnonimoNoVinculaPaciente() {
        var conPaciente = new EventoRequest(
                TipoEvento.CASI_INCIDENTE,
                SeveridadEvento.LEVE,
                "Farmacia",
                "Etiqueta equivocada detectada a tiempo",
                true,
                LocalDateTime.now().minusHours(1),
                fixture.paciente().getId(),
                null,
                null);

        assertThatThrownBy(() -> eventos.notificar(enfermera, conPaciente)).isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void elCierreExigeAnalisisAccionesHechasYVerificacion() {
        EventoResumenResponse creado = eventos.notificar(enfermera, evento(false));
        var cierre = new CierreEventoRequest("Sin nuevas caídas en tres meses", true);

        assertThatThrownBy(() -> eventos.cerrar(calidad, creado.id(), cierre))
                .isInstanceOf(BusinessLogicException.class);
        eventos.analizar(calidad, creado.id(), analisis());
        assertThatThrownBy(() -> eventos.cerrar(calidad, creado.id(), cierre))
                .isInstanceOf(BusinessLogicException.class);

        EventoDetalleResponse conAccion = eventos.agregarAccion(
                calidad,
                creado.id(),
                new AccionRequest(
                        "Evaluar riesgo de caída al ingreso",
                        otraEnfermera.getId(),
                        LocalDate.now().plusDays(10)));
        assertThatThrownBy(() -> eventos.cerrar(calidad, creado.id(), cierre))
                .isInstanceOf(BusinessLogicException.class);

        assertThatThrownBy(() -> eventos.completarAccion(
                        enfermera, conAccion.acciones().get(0).id()))
                .isInstanceOf(ForbiddenOperationException.class);
        eventos.completarAccion(otraEnfermera, conAccion.acciones().get(0).id());
        EventoDetalleResponse cerrado = eventos.cerrar(calidad, creado.id(), cierre);

        assertThat(cerrado.estado()).isEqualTo("CERRADO");
        assertThat(cerrado.accionEficaz()).isTrue();
        assertThatThrownBy(() -> eventos.analizar(calidad, creado.id(), analisis()))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void soloCalidadGestionaYGerenciaSoloLee() {
        EventoResumenResponse creado = eventos.notificar(enfermera, evento(false));

        assertThatThrownBy(() -> eventos.analizar(gerencia, creado.id(), analisis()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(eventos.tablero(gerencia).total()).isEqualTo(1);
        assertThat(eventos.exportar(gerencia)).isNotEmpty();
    }

    @Test
    void laFechaLimiteDeUnaAccionNoPuedeEstarEnElPasado() {
        EventoResumenResponse creado = eventos.notificar(enfermera, evento(false));

        assertThatThrownBy(() -> eventos.agregarAccion(
                        calidad,
                        creado.id(),
                        new AccionRequest(
                                "Capacitar",
                                otraEnfermera.getId(),
                                LocalDate.now().minusDays(1))))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void lasGuiasSeVersionanYSeSugierenPorDiagnostico() {
        var inicial = new GuiaRequest(
                "GPC-HTA",
                "Hipertensión arterial",
                null,
                "Manejo inicial de la hipertensión",
                "Contenido de la guía",
                List.of("I10", "I11"),
                LocalDate.now(),
                true);
        var creada = guias.crear(admin, inicial);

        assertThat(guias.sugeridas(List.of("I10.0"))).hasSize(1);
        assertThat(guias.sugeridas(List.of("E11.9"))).isEmpty();
        assertThatThrownBy(() -> guias.crear(admin, inicial)).isInstanceOf(ConflictException.class);

        var segunda = guias.nuevaVersion(admin, creada.guia().id(), inicial);

        assertThat(segunda.guia().version()).isEqualTo(2);
        assertThat(guias.detalle(creada.guia().id()).guia().estado()).isEqualTo("OBSOLETA");
        assertThat(guias.buscar("hipertensión", null, true)).hasSize(1);
        assertThat(guias.buscar("hipertensión", null, false)).hasSize(2);
    }
}

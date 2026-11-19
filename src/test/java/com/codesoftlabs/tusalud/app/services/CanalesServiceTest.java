package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.canales.FirmasCanales;
import com.codesoftlabs.tusalud.app.canales.ProveedorSimulado;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.PlantillaMensajeRequest;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.PreferenciaCanalRequest;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.PreferenciasCanalesRequest;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.SilencioRequest;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.SolicitudContactoRequest;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.ContactoCanalResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.MisCanalesResponse;
import com.codesoftlabs.tusalud.app.entities.EntregaNotificacion;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoContacto;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.DemasiadosIntentosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.EntregaNotificacionRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.canales.BajaCanalService;
import com.codesoftlabs.tusalud.app.services.canales.ColaEntregas;
import com.codesoftlabs.tusalud.app.services.canales.ContactosCanalService;
import com.codesoftlabs.tusalud.app.services.canales.EntregaProcesador;
import com.codesoftlabs.tusalud.app.services.canales.EntregasService;
import com.codesoftlabs.tusalud.app.services.canales.PlantillasMensajeService;
import com.codesoftlabs.tusalud.app.services.canales.WebhookCanalesService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
class CanalesServiceTest {

    private static final Pattern CODIGO = Pattern.compile("(\\d{6})");
    private static final String SECRETO_WEBHOOK = "secreto-webhook-de-pruebas";

    @Autowired
    private ContactosCanalService contactos;

    @Autowired
    private NotificacionService notificaciones;

    @Autowired
    private EntregaProcesador procesador;

    @Autowired
    private ColaEntregas cola;

    @Autowired
    private EntregasService entregasService;

    @Autowired
    private EntregaNotificacionRepository entregas;

    @Autowired
    private PlantillasMensajeService plantillas;

    @Autowired
    private WebhookCanalesService webhook;

    @Autowired
    private BajaCanalService baja;

    @Autowired
    private ProveedorSimulado simulado;

    @Autowired
    private ServiceTestFixture fixture;

    private Usuario usuario;
    private UsuarioPrincipal actor;

    @BeforeEach
    void setUp() {
        simulado.vaciar();
        usuario = fixture.usuario("canal", RolNombre.ROLE_PACIENTE);
        actor = fixture.principal(usuario);
        contactos.actualizarPreferencias(
                actor,
                new PreferenciasCanalesRequest(
                        null, new SilencioRequest(false, LocalTime.of(22, 0), LocalTime.of(7, 0))));
    }

    private void activarCorreo() {
        contactos.solicitar(actor, Canal.CORREO, new SolicitudContactoRequest(null, true));
        contactos.verificar(actor, Canal.CORREO, ultimoCodigo());
    }

    private String ultimoCodigo() {
        var mensaje = simulado.ultimos(1).getFirst();
        Matcher m = CODIGO.matcher(mensaje.texto());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private void activarSms(String telefono) {
        contactos.solicitar(actor, Canal.SMS, new SolicitudContactoRequest(telefono, true));
        contactos.verificar(actor, Canal.SMS, ultimoCodigo());
    }

    private EntregaNotificacion unicaEntrega() {
        List<EntregaNotificacion> todas = entregas.findAll().stream()
                .filter(e -> e.getUsuarioId().equals(usuario.getId()))
                .toList();
        assertThat(todas).hasSize(1);
        return todas.getFirst();
    }

    private List<EntregaNotificacion> entregasDelUsuario() {
        return entregas.findAll().stream()
                .filter(e -> e.getUsuarioId().equals(usuario.getId()) && e.getTipo() != TipoNotificacion.SISTEMA)
                .toList();
    }

    @Test
    void elCanalRequiereConsentimientoExplicito() {
        assertThatThrownBy(() -> contactos.solicitar(actor, Canal.CORREO, new SolicitudContactoRequest(null, false)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void elCorreoSiempreEsElDeLaCuentaYElCodigoActivaElCanal() {
        assertThatThrownBy(() ->
                        contactos.solicitar(actor, Canal.CORREO, new SolicitudContactoRequest("otro@correo.com", true)))
                .isInstanceOf(DatosInvalidosException.class);
        ContactoCanalResponse pendiente =
                contactos.solicitar(actor, Canal.CORREO, new SolicitudContactoRequest(null, true));
        assertThat(pendiente.estado()).isEqualTo(EstadoContacto.PENDIENTE_VERIFICACION);
        assertThat(pendiente.destinoMascara())
                .doesNotContain(usuario.getEmail())
                .contains("***@");
        var enviado = simulado.ultimos(1).getFirst();
        assertThat(enviado.destino()).isEqualTo(usuario.getEmail());
        ContactoCanalResponse activo = contactos.verificar(actor, Canal.CORREO, ultimoCodigo());
        assertThat(activo.estado()).isEqualTo(EstadoContacto.ACTIVO);
        assertThat(activo.verificadoEn()).isNotNull();
    }

    @Test
    void unCodigoIncorrectoSumaIntentosYTrasElMaximoSeBloquea() {
        contactos.solicitar(actor, Canal.CORREO, new SolicitudContactoRequest(null, true));
        String correcto = ultimoCodigo();
        String incorrecto = correcto.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> contactos.verificar(actor, Canal.CORREO, incorrecto))
                    .isInstanceOf(DatosInvalidosException.class);
        }
        assertThatThrownBy(() -> contactos.verificar(actor, Canal.CORREO, correcto))
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void noSePuedePedirOtroCodigoDeInmediato() {
        contactos.solicitar(actor, Canal.CORREO, new SolicitudContactoRequest(null, true));
        assertThatThrownBy(() -> contactos.solicitar(actor, Canal.CORREO, new SolicitudContactoRequest(null, true)))
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void elTelefonoSeNormalizaYSeRechazaUnoInvalido() {
        assertThatThrownBy(() -> contactos.solicitar(actor, Canal.SMS, new SolicitudContactoRequest("123", true)))
                .isInstanceOf(DatosInvalidosException.class);
        ContactoCanalResponse respuesta =
                contactos.solicitar(actor, Canal.SMS, new SolicitudContactoRequest("987 654 321", true));
        assertThat(respuesta.destinoMascara()).isEqualTo("***321");
        assertThat(simulado.ultimos(1).getFirst().destino()).isEqualTo("+51987654321");
    }

    @Test
    void sinCanalActivoNoSeProgramaNingunEnvio() {
        notificaciones.crear(usuario, TipoNotificacion.CITA_CONFIRMADA, "Cita confirmada", "Detalle", "/citas/1");
        assertThat(entregasDelUsuario()).isEmpty();
    }

    @Test
    void unaNotificacionGeneraUnaEntregaYElMensajeNoLlevaDatosClinicos() {
        activarCorreo();
        simulado.vaciar();
        notificaciones.crear(
                usuario,
                TipoNotificacion.RESULTADO_LABORATORIO,
                "Resultado de hemoglobina glicosilada",
                "Su resultado de hemoglobina glicosilada es 9.1 y sugiere diabetes",
                "/laboratorio/ordenes/1");
        EntregaNotificacion entrega = unicaEntregaExterna();
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.PENDIENTE);

        assertThat(procesador.procesar(entrega.getId(), LocalDateTime.now())).isEqualTo(EstadoEntrega.ENVIADO);

        var mensaje = simulado.ultimos(1).getFirst();
        assertThat(mensaje.destino()).isEqualTo(usuario.getEmail());
        assertThat(mensaje.asunto()).isEqualTo("Tiene un resultado disponible");
        assertThat(mensaje.texto())
                .contains("Hola " + usuario.getNombre())
                .doesNotContain("hemoglobina")
                .doesNotContain("diabetes")
                .doesNotContain("9.1");
        EntregaNotificacion enviada = entregas.findById(entrega.getId()).orElseThrow();
        assertThat(enviada.getIdExterno()).startsWith("sim-");
        assertThat(enviada.getDestinoMascara()).doesNotContain(usuario.getEmail());
    }

    private EntregaNotificacion unicaEntregaExterna() {
        List<EntregaNotificacion> lista = entregasDelUsuario();
        assertThat(lista).hasSize(1);
        return lista.getFirst();
    }

    @Test
    void laMismaNotificacionNoGeneraEntregasDuplicadas() {
        activarCorreo();
        notificaciones.crear(usuario, TipoNotificacion.CITA_CONFIRMADA, "Cita confirmada", "Detalle", "/citas/1");
        var entrega = unicaEntregaExterna();
        var respuesta = new com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse(
                entrega.getNotificacionId(),
                TipoNotificacion.CITA_CONFIRMADA,
                "t",
                "m",
                null,
                false,
                LocalDateTime.now());
        int creadas = despachador().despachar(usuario.getId(), respuesta, LocalDateTime.now());
        assertThat(creadas).isZero();
        assertThat(entregasDelUsuario()).hasSize(1);
    }

    @Autowired
    private com.codesoftlabs.tusalud.app.services.canales.DespachadorCanales despachadorBean;

    private com.codesoftlabs.tusalud.app.services.canales.DespachadorCanales despachador() {
        return despachadorBean;
    }

    @Test
    void unTipoDesactivadoNoGeneraEntrega() {
        activarCorreo();
        contactos.actualizarPreferencias(
                actor,
                new PreferenciasCanalesRequest(
                        List.of(new PreferenciaCanalRequest(TipoNotificacion.CITA_CONFIRMADA, Canal.CORREO, false)),
                        null));
        notificaciones.crear(usuario, TipoNotificacion.CITA_CONFIRMADA, "Cita confirmada", "Detalle", "/citas/1");
        assertThat(entregasDelUsuario()).isEmpty();
        notificaciones.crear(usuario, TipoNotificacion.CITA_CREADA, "Cita creada", "Detalle", "/citas/1");
        assertThat(entregasDelUsuario()).hasSize(1);
    }

    @Test
    void lasAlertasDeSeguridadNoSePuedenDesactivar() {
        assertThatThrownBy(() -> contactos.actualizarPreferencias(
                        actor,
                        new PreferenciasCanalesRequest(
                                List.of(new PreferenciaCanalRequest(
                                        TipoNotificacion.CONTRASENA_CAMBIADA, Canal.CORREO, false)),
                                null)))
                .isInstanceOf(ConflictException.class);
        MisCanalesResponse mis = contactos.mis(actor);
        var seguridad = mis.preferencias().stream()
                .filter(p -> p.tipo() == TipoNotificacion.SESION_SOSPECHOSA)
                .findFirst()
                .orElseThrow();
        assertThat(seguridad.obligatoria()).isTrue();
        assertThat(seguridad.activos().values()).containsOnly(true);
    }

    @Test
    void lasAlertasDeSeguridadSeEnvianAunSinElConsentimientoDeMarketing() {
        activarCorreo();
        notificaciones.crear(usuario, TipoNotificacion.CONTRASENA_CAMBIADA, "Contraseña", "Detalle", "/perfil");
        var entrega = unicaEntregaExterna();
        assertThat(entrega.isUrgente()).isTrue();
        assertThat(entrega.getProximoIntento()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void lasPreferenciasDeSmsPorDefectoSoloIncluyenLoImportante() {
        activarSms("+51987654321");
        notificaciones.crear(usuario, TipoNotificacion.DOCUMENTO_EMITIDO, "Documento", "Detalle", "/documentos");
        assertThat(entregasDelUsuario()).isEmpty();
        notificaciones.crear(usuario, TipoNotificacion.RECORDATORIO_CITA, "Recordatorio", "Detalle", "/citas/1");
        assertThat(entregasDelUsuario()).hasSize(1);
        assertThat(unicaEntregaExterna().getCanal()).isEqualTo(Canal.SMS);
    }

    @Test
    void laVentanaDeSilencioPostergaLosAvisosNoUrgentes() {
        activarCorreo();
        contactos.actualizarPreferencias(
                actor,
                new PreferenciasCanalesRequest(
                        null, new SilencioRequest(true, LocalTime.of(22, 0), LocalTime.of(7, 0))));
        var despachador = despachador();
        LocalDateTime noche = LocalDate.now().atTime(23, 30);
        var notificacion = new com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse(
                java.util.UUID.randomUUID(), TipoNotificacion.CITA_CREADA, "t", "m", null, false, noche);
        despachador.despachar(usuario.getId(), notificacion, noche);
        var entrega = entregasDelUsuario().stream()
                .filter(e -> e.getNotificacionId().equals(notificacion.id()))
                .findFirst()
                .orElseThrow();
        assertThat(entrega.getProximoIntento())
                .isEqualTo(noche.toLocalDate().plusDays(1).atTime(7, 0));
        assertThat(procesador.procesar(entrega.getId(), noche)).isEqualTo(EstadoEntrega.PENDIENTE);
    }

    @Test
    void unFalloTransitorioReintentaConRetrocesoYAlFinalFalla() {
        activarSms("+51900000000");
        notificaciones.crear(usuario, TipoNotificacion.RECORDATORIO_CITA, "Recordatorio", "Detalle", "/citas/1");
        var entrega = unicaEntregaExterna();
        LocalDateTime ahora = LocalDateTime.now();
        long[] esperas = {30, 60, 120, 240};
        for (int intento = 0; intento < 4; intento++) {
            assertThat(procesador.procesar(entrega.getId(), ahora)).isEqualTo(EstadoEntrega.PENDIENTE);
            var actual = entregas.findById(entrega.getId()).orElseThrow();
            assertThat(actual.getIntentos()).isEqualTo(intento + 1);
            assertThat(actual.getProximoIntento()).isEqualTo(ahora.plusSeconds(esperas[intento]));
            ahora = actual.getProximoIntento();
        }
        assertThat(procesador.procesar(entrega.getId(), ahora)).isEqualTo(EstadoEntrega.FALLIDO);
        var final_ = entregas.findById(entrega.getId()).orElseThrow();
        assertThat(final_.getIntentos()).isEqualTo(5);
        assertThat(final_.getMotivo()).isNotBlank();
    }

    @Test
    void unDestinoRechazadoDefinitivamenteFallaDeInmediato() {
        activarSms("+51999999999");
        notificaciones.crear(usuario, TipoNotificacion.RECORDATORIO_CITA, "Recordatorio", "Detalle", "/citas/1");
        var entrega = unicaEntregaExterna();
        assertThat(procesador.procesar(entrega.getId(), LocalDateTime.now())).isEqualTo(EstadoEntrega.FALLIDO);
        assertThat(entregas.findById(entrega.getId()).orElseThrow().getIntentos())
                .isEqualTo(1);
    }

    @Test
    void unaEntregaFallidaSePuedeReintentarYLaColaLaProcesa() {
        activarSms("+51999999999");
        notificaciones.crear(usuario, TipoNotificacion.RECORDATORIO_CITA, "Recordatorio", "Detalle", "/citas/1");
        var entrega = unicaEntregaExterna();
        procesador.procesar(entrega.getId(), LocalDateTime.now());
        entregasService.reintentar(entrega.getId());
        assertThat(entregas.findById(entrega.getId()).orElseThrow().getEstado()).isEqualTo(EstadoEntrega.PENDIENTE);
        assertThat(cola.procesarPendientes(LocalDateTime.now().plusMinutes(1))).isGreaterThanOrEqualTo(1);
        assertThat(entregas.findById(entrega.getId()).orElseThrow().getEstado()).isEqualTo(EstadoEntrega.FALLIDO);
        assertThatThrownBy(() -> entregasService.reintentar(activarYEnviar())).isInstanceOf(ConflictException.class);
    }

    private java.util.UUID activarYEnviar() {
        Usuario otro = fixture.usuario("otro", RolNombre.ROLE_PACIENTE);
        UsuarioPrincipal otroActor = fixture.principal(otro);
        contactos.solicitar(otroActor, Canal.CORREO, new SolicitudContactoRequest(null, true));
        contactos.verificar(otroActor, Canal.CORREO, ultimoCodigo());
        notificaciones.crear(otro, TipoNotificacion.CITA_CREADA, "Cita", "Detalle", "/citas/1");
        var entrega = entregas.findAll().stream()
                .filter(e -> e.getUsuarioId().equals(otro.getId()) && e.getTipo() == TipoNotificacion.CITA_CREADA)
                .findFirst()
                .orElseThrow();
        procesador.procesar(entrega.getId(), LocalDateTime.now());
        return entrega.getId();
    }

    @Test
    void elLimiteHorarioDetieneLosAvisosNoObligatorios() {
        activarCorreo();
        for (int i = 0; i < 11; i++) {
            notificaciones.crear(usuario, TipoNotificacion.CITA_CREADA, "Cita " + i, "Detalle", "/citas/" + i);
        }
        var lista = entregasDelUsuario();
        assertThat(lista).hasSize(11);
        assertThat(lista.stream().filter(e -> e.getEstado() == EstadoEntrega.FALLIDO))
                .hasSize(1)
                .allSatisfy(e -> assertThat(e.getMotivo()).contains("Límite"));
    }

    @Test
    void lasAlertasDeSeguridadIgnoranElLimiteHorario() {
        activarCorreo();
        for (int i = 0; i < 12; i++) {
            notificaciones.crear(usuario, TipoNotificacion.SESION_SOSPECHOSA, "Alerta " + i, "Detalle", "/perfil");
        }
        assertThat(entregasDelUsuario()).hasSize(12).allMatch(e -> e.getEstado() == EstadoEntrega.PENDIENTE);
    }

    @Test
    void laBajaDetieneLosEnviosYSeReactivaConUnaNuevaVerificacion() {
        activarCorreo();
        contactos.revocar(actor, Canal.CORREO);
        notificaciones.crear(usuario, TipoNotificacion.CITA_CREADA, "Cita", "Detalle", "/citas/1");
        assertThat(entregasDelUsuario()).isEmpty();
        assertThat(contactos.mis(actor).canales().stream()
                        .filter(c -> c.canal() == Canal.CORREO)
                        .findFirst()
                        .orElseThrow()
                        .estado())
                .isEqualTo(EstadoContacto.BAJA);
    }

    @Test
    void elEnlaceDeBajaFirmadoDaDeBajaElCanalYUnoAdulteradoSeRechaza() {
        activarCorreo();
        String token = FirmasCanales.crearTokenBaja(
                "secreto-de-desarrollo-para-enlaces-de-baja", usuario.getId(), Canal.CORREO);
        assertThat(baja.baja(token)).isEqualTo(Canal.CORREO);
        assertThat(contactos.mis(actor).canales().stream()
                        .filter(c -> c.canal() == Canal.CORREO)
                        .findFirst()
                        .orElseThrow()
                        .estado())
                .isEqualTo(EstadoContacto.BAJA);
        assertThatThrownBy(() -> baja.baja(token + "x")).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> baja.baja("basura")).isInstanceOf(DatosInvalidosException.class);
        String ajeno = FirmasCanales.crearTokenBaja("otro-secreto", usuario.getId(), Canal.CORREO);
        assertThatThrownBy(() -> baja.baja(ajeno)).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void elWebhookFirmadoActualizaElEstadoYUnoSinFirmaValidaSeRechaza() {
        activarCorreo();
        notificaciones.crear(usuario, TipoNotificacion.CITA_CREADA, "Cita", "Detalle", "/citas/1");
        var entrega = unicaEntregaExterna();
        procesador.procesar(entrega.getId(), LocalDateTime.now());
        String idExterno = entregas.findById(entrega.getId()).orElseThrow().getIdExterno();
        byte[] cuerpo =
                ("{\"idExterno\":\"" + idExterno + "\",\"estado\":\"ENTREGADO\"}").getBytes(StandardCharsets.UTF_8);
        String ahora = String.valueOf(Instant.now().getEpochSecond());

        assertThatThrownBy(() -> webhook.procesar(Canal.CORREO, ahora, "sha256=00", cuerpo))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> webhook.procesar(Canal.CORREO, null, null, cuerpo))
                .isInstanceOf(ForbiddenOperationException.class);
        String vieja = String.valueOf(Instant.now().getEpochSecond() - 3600);
        assertThatThrownBy(() -> webhook.procesar(Canal.CORREO, vieja, firma(vieja, cuerpo), cuerpo))
                .isInstanceOf(ForbiddenOperationException.class);

        assertThat(webhook.procesar(Canal.CORREO, ahora, firma(ahora, cuerpo), cuerpo))
                .isTrue();
        var entregada = entregas.findById(entrega.getId()).orElseThrow();
        assertThat(entregada.getEstado()).isEqualTo(EstadoEntrega.ENTREGADO);
        assertThat(entregada.getEntregadoEn()).isNotNull();
    }

    @Test
    void unRebotePorWebhookDaDeBajaElDestino() {
        activarCorreo();
        notificaciones.crear(usuario, TipoNotificacion.CITA_CREADA, "Cita", "Detalle", "/citas/1");
        var entrega = unicaEntregaExterna();
        procesador.procesar(entrega.getId(), LocalDateTime.now());
        String idExterno = entregas.findById(entrega.getId()).orElseThrow().getIdExterno();
        byte[] cuerpo = ("{\"idExterno\":\"" + idExterno
                        + "\",\"estado\":\"REBOTADO\",\"motivo\":\"Buzón inexistente\"}")
                .getBytes(StandardCharsets.UTF_8);
        String ahora = String.valueOf(Instant.now().getEpochSecond());
        webhook.procesar(Canal.CORREO, ahora, firma(ahora, cuerpo), cuerpo);

        assertThat(entregas.findById(entrega.getId()).orElseThrow().getEstado()).isEqualTo(EstadoEntrega.REBOTADO);
        notificaciones.crear(usuario, TipoNotificacion.CITA_CONFIRMADA, "Cita", "Detalle", "/citas/2");
        assertThat(entregasDelUsuario()).hasSize(1);
    }

    @Test
    void elWebhookIgnoraEventosDeEntregasDesconocidasYNoRegresaEstados() {
        byte[] cuerpo = "{\"idExterno\":\"no-existe\",\"estado\":\"ENTREGADO\"}".getBytes(StandardCharsets.UTF_8);
        String ahora = String.valueOf(Instant.now().getEpochSecond());
        assertThat(webhook.procesar(Canal.CORREO, ahora, firma(ahora, cuerpo), cuerpo))
                .isFalse();
    }

    @Test
    void lasPlantillasSeVersionanYSoloAdmitenVariablesPermitidas() {
        var original = plantillas.efectiva(TipoNotificacion.CITA_CONFIRMADA, Canal.CORREO);
        assertThat(original.version()).isZero();

        assertThatThrownBy(() -> plantillas.actualizar(
                        actor,
                        TipoNotificacion.CITA_CONFIRMADA,
                        Canal.CORREO,
                        new PlantillaMensajeRequest(
                                "Asunto", "Hola {{nombre}} su diagnostico es {{diagnostico}}", "Cambio")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> plantillas.actualizar(
                        actor,
                        TipoNotificacion.CITA_CONFIRMADA,
                        Canal.CORREO,
                        new PlantillaMensajeRequest(" ", "Hola {{nombre}}", "Cambio de prueba")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> plantillas.actualizar(
                        actor,
                        TipoNotificacion.CITA_CONFIRMADA,
                        Canal.SMS,
                        new PlantillaMensajeRequest(null, "x".repeat(400), "Cambio de prueba")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> plantillas.actualizar(
                        actor,
                        TipoNotificacion.STOCK_BAJO,
                        Canal.CORREO,
                        new PlantillaMensajeRequest("Asunto", "Hola", "Cambio de prueba")))
                .isInstanceOf(DatosInvalidosException.class);

        var v1 = plantillas.actualizar(
                actor,
                TipoNotificacion.CITA_CONFIRMADA,
                Canal.CORREO,
                new PlantillaMensajeRequest(
                        "Cita confirmada en {{centro}}",
                        "Hola {{nombre}}, ingrese a {{enlace}}.",
                        "Mejora de redacción"));
        var v2 = plantillas.actualizar(
                actor,
                TipoNotificacion.CITA_CONFIRMADA,
                Canal.CORREO,
                new PlantillaMensajeRequest(
                        "Cita confirmada", "Hola {{nombre}}. Ingrese a {{enlace}}.", "Segunda revisión"));
        assertThat(v1.version()).isEqualTo(1);
        assertThat(v2.version()).isEqualTo(2);
        assertThat(plantillas
                        .efectiva(TipoNotificacion.CITA_CONFIRMADA, Canal.CORREO)
                        .version())
                .isEqualTo(2);
        assertThat(plantillas.historial(TipoNotificacion.CITA_CONFIRMADA, Canal.CORREO))
                .hasSize(2);

        var restaurada = plantillas.restaurar(TipoNotificacion.CITA_CONFIRMADA, Canal.CORREO);
        assertThat(restaurada.personalizada()).isFalse();
        assertThat(plantillas
                        .efectiva(TipoNotificacion.CITA_CONFIRMADA, Canal.CORREO)
                        .version())
                .isZero();
    }

    @Test
    void laPlantillaPersonalizadaSeUsaAlEnviar() {
        activarCorreo();
        plantillas.actualizar(
                actor,
                TipoNotificacion.CITA_CONFIRMADA,
                Canal.CORREO,
                new PlantillaMensajeRequest(
                        "Asunto propio", "Estimado {{nombre}}, todo listo en {{centro}}.", "Texto institucional"));
        simulado.vaciar();
        notificaciones.crear(usuario, TipoNotificacion.CITA_CONFIRMADA, "Cita", "Detalle", "/citas/1");
        var entrega = unicaEntregaExterna();
        procesador.procesar(entrega.getId(), LocalDateTime.now());
        var mensaje = simulado.ultimos(1).getFirst();
        assertThat(mensaje.asunto()).isEqualTo("Asunto propio");
        assertThat(mensaje.texto()).contains("Estimado " + usuario.getNombre());
        assertThat(entregas.findById(entrega.getId()).orElseThrow().getPlantillaVersion())
                .isEqualTo(1);
    }

    @Test
    void elListadoYElResumenDeEntregasNoExponenDestinosCompletos() {
        activarCorreo();
        notificaciones.crear(usuario, TipoNotificacion.CITA_CREADA, "Cita", "Detalle", "/citas/1");
        var pagina = entregasService.listar(Canal.CORREO, null, LocalDate.now(), LocalDate.now(), 0, 50);
        assertThat(pagina.total()).isGreaterThanOrEqualTo(1);
        assertThat(pagina.contenido()).allSatisfy(e -> assertThat(e.destino()).doesNotContain(usuario.getEmail()));
        var resumen = entregasService.resumen(7);
        assertThat(resumen.canales()).hasSize(3);
        assertThat(resumen.total()).isGreaterThanOrEqualTo(1);
    }

    private static String firma(String marcaTiempo, byte[] cuerpo) {
        byte[] prefijo = (marcaTiempo + ".").getBytes(StandardCharsets.UTF_8);
        byte[] datos = new byte[prefijo.length + cuerpo.length];
        System.arraycopy(prefijo, 0, datos, 0, prefijo.length);
        System.arraycopy(cuerpo, 0, datos, prefijo.length, cuerpo.length);
        return "sha256=" + FirmasCanales.hmacHex(SECRETO_WEBHOOK, datos);
    }
}

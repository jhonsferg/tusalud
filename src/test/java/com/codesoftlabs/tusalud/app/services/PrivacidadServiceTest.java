package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.BancoDatosRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.DecisionArco;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.FinalidadRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.IncidenteActualizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.IncidenteRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.PoliticaRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.ResolucionArcoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.SolicitudArcoRequest;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.FinalidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.IncidenteResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.SolicitudArcoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.SolicitudArcoResponse;
import com.codesoftlabs.tusalud.app.entities.ContactoCanal;
import com.codesoftlabs.tusalud.app.entities.EntregaNotificacion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PoliticaRetencion;
import com.codesoftlabs.tusalud.app.entities.SesionUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.AccionRetencion;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.CampoRectificable;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.EstadoArco;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.EstadoIncidente;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.SeveridadIncidente;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.TipoArco;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.TipoIncidente;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.AuditoriaClinicaRepository;
import com.codesoftlabs.tusalud.app.repositories.ContactoCanalRepository;
import com.codesoftlabs.tusalud.app.repositories.EntregaNotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.PoliticaRetencionRepository;
import com.codesoftlabs.tusalud.app.repositories.SesionUsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.privacidad.AccesosDatosService;
import com.codesoftlabs.tusalud.app.services.privacidad.ConsentimientosPrivacidadService;
import com.codesoftlabs.tusalud.app.services.privacidad.IncidentesDatosService;
import com.codesoftlabs.tusalud.app.services.privacidad.PoliticaPrivacidadService;
import com.codesoftlabs.tusalud.app.services.privacidad.RegistroTratamientosService;
import com.codesoftlabs.tusalud.app.services.privacidad.RetencionService;
import com.codesoftlabs.tusalud.app.services.privacidad.SolicitudArcoService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
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
class PrivacidadServiceTest {

    @Autowired
    private PoliticaPrivacidadService politicas;

    @Autowired
    private ConsentimientosPrivacidadService consentimientos;

    @Autowired
    private SolicitudArcoService solicitudes;

    @Autowired
    private RegistroTratamientosService registro;

    @Autowired
    private RetencionService retencion;

    @Autowired
    private IncidentesDatosService incidentes;

    @Autowired
    private AccesosDatosService accesos;

    @Autowired
    private AuditoriaClinicaService auditoria;

    @Autowired
    private AuditoriaClinicaRepository auditoriaRepo;

    @Autowired
    private ContactoCanalRepository contactos;

    @Autowired
    private EntregaNotificacionRepository entregas;

    @Autowired
    private SesionUsuarioRepository sesiones;

    @Autowired
    private PoliticaRetencionRepository politicasRetencion;

    @Autowired
    private NotificacionRepository notificaciones;

    @Autowired
    private ServiceTestFixture fixture;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private UsuarioPrincipal admin;
    private UsuarioPrincipal calidad;
    private UsuarioPrincipal titular;
    private UsuarioPrincipal otroPaciente;
    private Paciente paciente;
    private FinalidadResponse obligatoria;
    private FinalidadResponse opcional;

    @BeforeEach
    void preparar() {
        admin = fixture.principal(fixture.usuario("adm", RolNombre.ROLE_ADMIN));
        calidad = fixture.principal(fixture.usuario("cal", RolNombre.ROLE_CALIDAD));
        paciente = fixture.paciente();
        titular = fixture.principal(paciente.getUsuario());
        otroPaciente = fixture.principal(fixture.paciente().getUsuario());
        obligatoria = registro.guardarFinalidad(
                null,
                new FinalidadRequest(
                        "ATENCION-" + ServiceTestFixture.siguiente(),
                        "Atención de salud",
                        "Prestación del servicio de salud",
                        null,
                        "Ejecución de la relación asistencial",
                        true,
                        "Datos de salud",
                        null,
                        true));
        opcional = registro.guardarFinalidad(
                null,
                new FinalidadRequest(
                        "MKT-" + ServiceTestFixture.siguiente(),
                        "Comunicaciones promocionales",
                        "Envío de campañas",
                        null,
                        "Consentimiento del titular",
                        false,
                        "Contacto",
                        null,
                        true));
    }

    private String lorem() {
        return "Esta política describe el tratamiento de datos personales de la clínica, sus finalidades, los plazos"
                + " de conservación y los derechos de acceso, rectificación, cancelación y oposición.";
    }

    @Test
    void laPoliticaSePublicaUnaSolaVezYSuAceptacionQuedaRegistradaPorUsuario() {
        var borrador = politicas.crear(admin, new PoliticaRequest("Política de privacidad", lorem(), LocalDate.now()));
        assertThat(borrador.publicada()).isFalse();
        assertThat(borrador.hash()).hasSize(64);
        assertThat(politicas.vigentePublica()).isEmpty();
        assertThat(politicas.estado(titular).aceptada()).isTrue();

        politicas.publicar(admin, borrador.id());

        assertThat(politicas.vigentePublica())
                .hasValueSatisfying(p -> assertThat(p.version()).isEqualTo(borrador.version()));
        assertThat(politicas.estado(titular).aceptada()).isFalse();
        politicas.aceptar(titular, com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.OrigenPrivacidad.APP);
        politicas.aceptar(titular, com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.OrigenPrivacidad.APP);
        assertThat(politicas.estado(titular).aceptada()).isTrue();
        assertThat(politicas.estado(otroPaciente).aceptada()).isFalse();
        assertThatThrownBy(() -> politicas.actualizar(
                        admin, borrador.id(), new PoliticaRequest("Otro título", lorem(), LocalDate.now())))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> politicas.publicar(admin, borrador.id())).isInstanceOf(ConflictException.class);
        assertThat(politicas.listar().get(0).aceptaciones()).isEqualTo(1);
    }

    @Test
    void laNuevaVersionExigeUnaNuevaAceptacion() {
        var v1 = politicas.crear(
                admin,
                new PoliticaRequest("Política v1", lorem(), LocalDate.now().minusDays(10)));
        politicas.publicar(admin, v1.id());
        politicas.aceptar(titular, com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.OrigenPrivacidad.APP);

        var v2 = politicas.crear(admin, new PoliticaRequest("Política v2", lorem() + " Cambios.", LocalDate.now()));
        politicas.publicar(admin, v2.id());

        var estado = politicas.estado(titular);
        assertThat(estado.versionVigente()).isEqualTo(v2.version());
        assertThat(estado.aceptada()).isFalse();
    }

    @Test
    void elConsentimientoSeOtorgaYRevocaConHistorialYLaFinalidadObligatoriaNoSeRevoca() {
        assertThat(consentimientos.mios(titular.getId()))
                .anySatisfy(e -> {
                    assertThat(e.finalidadId()).isEqualTo(opcional.id());
                    assertThat(e.estado()).isEqualTo("SIN_REGISTRO");
                })
                .anySatisfy(e -> {
                    assertThat(e.finalidadId()).isEqualTo(obligatoria.id());
                    assertThat(e.estado()).isEqualTo("NO_REQUIERE");
                });

        assertThat(consentimientos.otorgar(titular, opcional.id(), null).estado())
                .isEqualTo("OTORGADO");
        assertThat(consentimientos
                        .revocar(titular, opcional.id(), "Ya no deseo recibirlas")
                        .estado())
                .isEqualTo("REVOCADO");

        assertThat(consentimientos.historial(titular.getId())).hasSize(2);
        assertThat(consentimientos.historial(titular.getId()).get(0).accion()).isEqualTo("REVOCAR");
        assertThatThrownBy(() -> consentimientos.revocar(titular, obligatoria.id(), null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("necesaria");
        assertThatThrownBy(() -> consentimientos.otorgar(titular, obligatoria.id(), null))
                .isInstanceOf(ConflictException.class);
        assertThat(consentimientos.mios(otroPaciente.getId()))
                .noneMatch(e -> "OTORGADO".equals(e.estado()) || "REVOCADO".equals(e.estado()));
    }

    @Test
    void laSolicitudDeAccesoGeneraLaExportacionConLosDatosDelTitularYSoloEllaPuedeDescargarla() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2), EstadoCita.PENDIENTE);
        SolicitudArcoResponse s = solicitudes.crear(
                titular, new SolicitudArcoRequest(TipoArco.ACCESO, "Quiero una copia de mis datos", null, null, null));
        assertThat(s.estado()).isEqualTo(EstadoArco.RECIBIDA);
        assertThat(s.codigo()).startsWith("ARCO-");
        assertThat(s.venceEn()).isAfter(LocalDate.now());
        assertThatThrownBy(() -> solicitudes.exportar(titular, s.id())).isInstanceOf(ConflictException.class);

        solicitudes.tomar(calidad, s.id());
        SolicitudArcoResponse resuelta = solicitudes.resolver(
                calidad,
                s.id(),
                new ResolucionArcoRequest(DecisionArco.ATENDER, "Se adjunta la copia de sus datos personales"));

        assertThat(resuelta.estado()).isEqualTo(EstadoArco.ATENDIDA);
        assertThat(resuelta.tieneExportacion()).isTrue();
        String json = new String(solicitudes.exportar(titular, s.id()), StandardCharsets.UTF_8);
        assertThat(json)
                .contains(paciente.getDni())
                .contains(paciente.getUsuario().getEmail())
                .contains("citas");
        assertThat(new String(solicitudes.exportar(admin, s.id()), StandardCharsets.UTF_8))
                .isEqualTo(json);
        assertThatThrownBy(() -> solicitudes.exportar(otroPaciente, s.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> solicitudes.detalle(otroPaciente, s.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        SolicitudArcoDetalle detalle = solicitudes.detalle(titular, s.id());
        assertThat(detalle.eventos()).extracting(e -> e.accion()).contains("RECIBIDA", "ASIGNADA", "ATENDIDA");
        assertThat(notificaciones.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
                        titular.getId(),
                        TipoNotificacion.ARCO_RESUELTA,
                        LocalDate.now().atStartOfDay()))
                .isTrue();
        assertThat(notificaciones.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
                        calidad.getId(),
                        TipoNotificacion.ARCO_RECIBIDA,
                        LocalDate.now().atStartOfDay()))
                .isTrue();
    }

    @Test
    void soloSeAdmiteUnaSolicitudAbiertaPorTipoYUnaResueltaNoVuelveAResolverse() {
        solicitudes.crear(
                titular, new SolicitudArcoRequest(TipoArco.ACCESO, "Quiero una copia de mis datos", null, null, null));
        assertThatThrownBy(() -> solicitudes.crear(
                        titular,
                        new SolicitudArcoRequest(TipoArco.ACCESO, "Otra solicitud de acceso", null, null, null)))
                .isInstanceOf(ConflictException.class);
        SolicitudArcoResponse otra = solicitudes.crear(
                otroPaciente,
                new SolicitudArcoRequest(TipoArco.ACCESO, "Quiero una copia de mis datos", null, null, null));
        solicitudes.resolver(
                calidad,
                otra.id(),
                new ResolucionArcoRequest(DecisionArco.RECHAZAR, "No se pudo verificar la identidad"));

        assertThatThrownBy(() -> solicitudes.resolver(
                        calidad,
                        otra.id(),
                        new ResolucionArcoRequest(DecisionArco.ATENDER, "Resolución duplicada aquí")))
                .isInstanceOf(ConflictException.class);
        assertThat(solicitudes.bandeja(EstadoArco.RECHAZADA, null))
                .extracting(s -> s.id())
                .contains(otra.id());
        assertThat(solicitudes.mias(titular)).hasSize(1);
    }

    @Test
    void elPlazoCuentaDiasHabilesSinSabadosNiDomingos() {
        LocalDate viernes = LocalDate.of(2026, 11, 20);
        assertThat(viernes.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);

        assertThat(SolicitudArcoService.sumarDiasHabiles(viernes, 1)).isEqualTo(LocalDate.of(2026, 11, 23));
        assertThat(SolicitudArcoService.sumarDiasHabiles(viernes, 8)).isEqualTo(LocalDate.of(2026, 12, 2));
        assertThat(SolicitudArcoService.sumarDiasHabiles(viernes, 10)).isEqualTo(LocalDate.of(2026, 12, 4));
    }

    @Test
    void laRectificacionAplicaElCampoSolicitadoYValidaElValor() {
        assertThatThrownBy(() -> solicitudes.crear(
                        titular,
                        new SolicitudArcoRequest(
                                TipoArco.RECTIFICACION,
                                "Mi teléfono está mal registrado",
                                CampoRectificable.TELEFONO,
                                "abc",
                                null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> solicitudes.crear(
                        titular,
                        new SolicitudArcoRequest(
                                TipoArco.RECTIFICACION,
                                "Mi teléfono está mal registrado",
                                CampoRectificable.TELEFONO,
                                null,
                                null)))
                .isInstanceOf(DatosInvalidosException.class);
        SolicitudArcoResponse s = solicitudes.crear(
                titular,
                new SolicitudArcoRequest(
                        TipoArco.RECTIFICACION,
                        "Mi teléfono está mal registrado",
                        CampoRectificable.TELEFONO,
                        "+51 999 111 222",
                        null));

        solicitudes.resolver(
                calidad, s.id(), new ResolucionArcoRequest(DecisionArco.ATENDER, "Dato corregido en el sistema"));

        assertThat(paciente.getUsuario().getTelefono()).isEqualTo("+51 999 111 222");
    }

    @Test
    void laRectificacionDeDatosClinicosSoloSeDerivaConResolucionParcial() {
        SolicitudArcoResponse s = solicitudes.crear(
                titular,
                new SolicitudArcoRequest(
                        TipoArco.RECTIFICACION, "El diagnóstico de mi última consulta es inexacto", null, null, null));

        assertThatThrownBy(() -> solicitudes.resolver(
                        calidad, s.id(), new ResolucionArcoRequest(DecisionArco.ATENDER, "Se corrigió el diagnóstico")))
                .isInstanceOf(DatosInvalidosException.class);

        SolicitudArcoResponse parcial = solicitudes.resolver(
                calidad,
                s.id(),
                new ResolucionArcoRequest(
                        DecisionArco.ATENDER_PARCIALMENTE, "Se derivó al médico tratante para el addendum con rastro"));
        assertThat(parcial.estado()).isEqualTo(EstadoArco.PARCIAL);
    }

    @Test
    void laCancelacionConservaLaHistoriaClinicaYEliminaLoCancelable() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2), EstadoCita.PENDIENTE);
        contactos.save(ContactoCanal.builder()
                .usuario(paciente.getUsuario())
                .canal(Canal.CORREO)
                .destino("titular@example.com")
                .build());
        consentimientos.otorgar(titular, opcional.id(), null);

        var previa = solicitudes.previaCancelacion(titular.getId());
        assertThat(previa.hayBloqueos()).isTrue();
        assertThat(previa.categorias())
                .anySatisfy(b -> {
                    assertThat(b.categoria()).isEqualTo("Historia clínica y citas");
                    assertThat(b.estado()).isEqualTo("BLOQUEADO");
                    assertThat(b.baseLegal()).contains("por confirmar");
                })
                .anySatisfy(b -> {
                    assertThat(b.categoria()).isEqualTo("Canales de notificación");
                    assertThat(b.estado()).isEqualTo("CANCELABLE");
                });

        SolicitudArcoResponse s = solicitudes.crear(
                titular,
                new SolicitudArcoRequest(
                        TipoArco.CANCELACION, "Deseo que eliminen mis datos personales", null, null, null));
        SolicitudArcoResponse resuelta = solicitudes.resolver(
                calidad, s.id(), new ResolucionArcoRequest(DecisionArco.ATENDER, "Se canceló lo que la ley permite"));

        assertThat(resuelta.estado()).isEqualTo(EstadoArco.PARCIAL);
        assertThat(contactos.findByUsuarioId(titular.getId())).isEmpty();
        assertThat(consentimientos.mios(titular.getId()))
                .filteredOn(e -> e.finalidadId().equals(opcional.id()))
                .extracting(e -> e.estado())
                .containsExactly("REVOCADO");
        assertThat(solicitudes.detalle(titular, s.id()).resultado()).isNotEmpty();
    }

    @Test
    void laOposicionRevocaLaFinalidadOpcionalYNoAdmiteLaObligatoria() {
        consentimientos.otorgar(titular, opcional.id(), null);
        assertThatThrownBy(() -> solicitudes.crear(
                        titular,
                        new SolicitudArcoRequest(
                                TipoArco.OPOSICION, "Me opongo al tratamiento de mis datos", null, null, null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> solicitudes.crear(
                        titular,
                        new SolicitudArcoRequest(
                                TipoArco.OPOSICION,
                                "Me opongo al tratamiento de mis datos",
                                null,
                                null,
                                obligatoria.id())))
                .isInstanceOf(ConflictException.class);
        SolicitudArcoResponse s = solicitudes.crear(
                titular,
                new SolicitudArcoRequest(
                        TipoArco.OPOSICION, "Me opongo a recibir promociones", null, null, opcional.id()));

        solicitudes.resolver(
                calidad, s.id(), new ResolucionArcoRequest(DecisionArco.ATENDER, "Se revocó la finalidad"));

        assertThat(consentimientos.mios(titular.getId()))
                .filteredOn(e -> e.finalidadId().equals(opcional.id()))
                .extracting(e -> e.estado())
                .containsExactly("REVOCADO");
        assertThat(consentimientos.historial(titular.getId()).get(0).origen()).isEqualTo("SOLICITUD_ARCO");
    }

    @Test
    void laRetencionAnonimizaLoVencidoYNuncaBorraLaHistoriaClinica() {
        sembrarRetencion();
        UUID usuarioId = titular.getId();
        entregas.save(EntregaNotificacion.builder()
                .usuarioId(usuarioId)
                .canal(Canal.CORREO)
                .tipo(TipoNotificacion.CITA_CREADA)
                .destinoMascara("t***@example.com")
                .claveIdempotencia("k-" + UUID.randomUUID())
                .creadoEn(LocalDateTime.now().minusDays(400))
                .build());
        entregas.save(EntregaNotificacion.builder()
                .usuarioId(usuarioId)
                .canal(Canal.CORREO)
                .tipo(TipoNotificacion.CITA_CREADA)
                .destinoMascara("t***@example.com")
                .claveIdempotencia("k-" + UUID.randomUUID())
                .creadoEn(LocalDateTime.now().minusDays(2))
                .build());
        sesiones.save(SesionUsuario.builder()
                .usuario(paciente.getUsuario())
                .tokenId(UUID.randomUUID())
                .ip("10.0.0.1")
                .dispositivo("Chrome en Windows")
                .creadaEn(LocalDateTime.now().minusDays(300))
                .ultimoAcceso(LocalDateTime.now().minusDays(300))
                .expiraEn(LocalDateTime.now().minusDays(290))
                .revocada(true)
                .build());
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        fixture.cita(medico, paciente, LocalDateTime.now().minusYears(30), EstadoCita.ATENDIDA);
        long sesionesAntes = sesiones.count();

        var resultado = retencion.ejecutarTodas();
        em.flush();
        em.clear();

        assertThat(resultado).isNotEmpty();
        assertThat(entregas.findAll())
                .filteredOn(e -> e.getUsuarioId().equals(usuarioId))
                .extracting(e -> e.getDestinoMascara())
                .containsExactlyInAnyOrder("anonimizado", "t***@example.com");
        assertThat(sesiones.count()).isEqualTo(sesionesAntes - 1);
        PoliticaRetencion historia =
                politicasRetencion.findByTipoDato("HISTORIA_CLINICA").orElseThrow();
        assertThat(historia.getAccion()).isEqualTo(AccionRetencion.MARCAR);
        assertThat(historia.getUltimaEjecucion()).isNotNull();
        assertThat(retencion.historial()).isNotEmpty();
    }

    private void sembrarRetencion() {
        String[][] datos = {
            {"ENTREGAS_NOTIFICACION", "ANONIMIZAR", "180"},
            {"SESIONES_DISPOSITIVO", "DEPURAR", "90"},
            {"EVENTOS_SEGURIDAD", "ANONIMIZAR", "365"},
            {"INTERCAMBIOS_SUNAT", "DEPURAR", "1825"},
            {"HISTORIA_CLINICA", "MARCAR", "7300"}
        };
        for (String[] d : datos) {
            politicasRetencion.save(PoliticaRetencion.builder()
                    .tipoDato(d[0])
                    .nombre(d[0])
                    .descripcion("Política de prueba")
                    .plazoDias(Integer.parseInt(d[2]))
                    .accion(AccionRetencion.valueOf(d[1]))
                    .baseLegal("Prueba")
                    .activa(true)
                    .build());
        }
    }

    @Test
    void lasPoliticasDeRetencionSeAjustanPeroLaHistoriaClinicaSoloMarca() {
        sembrarRetencion();
        PoliticaRetencion entregasPolitica =
                politicasRetencion.findByTipoDato("ENTREGAS_NOTIFICACION").orElseThrow();
        var actualizada = retencion.actualizar(
                entregasPolitica.getId(),
                new com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.PoliticaRetencionRequest(30, true));
        assertThat(actualizada.plazoDias()).isEqualTo(30);
        assertThat(retencion.listar()).extracting(p -> p.tipoDato()).contains("HISTORIA_CLINICA", "INTERCAMBIOS_SUNAT");
    }

    @Test
    void elIncidenteGravePideNotificarALaAutoridadEnElPlazoYNoSeCierraSinHacerlo() {
        IncidenteResponse i = incidentes.registrar(
                admin,
                new IncidenteRequest(
                        TipoIncidente.ACCESO_NO_AUTORIZADO,
                        SeveridadIncidente.ALTA,
                        "Un usuario consultó historias sin relación asistencial",
                        "Historias clínicas",
                        12,
                        null,
                        null));

        assertThat(i.codigo()).startsWith("INC-");
        assertThat(i.estado()).isEqualTo(EstadoIncidente.ABIERTO);
        assertThat(i.notificarAutoridadAntesDe()).isAfter(i.detectadoEn().plusHours(47));
        assertThat(i.vencido()).isFalse();
        assertThat(notificaciones.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
                        calidad.getId(),
                        TipoNotificacion.INCIDENTE_DATOS,
                        LocalDate.now().atStartOfDay()))
                .isTrue();
        assertThatThrownBy(() -> incidentes.actualizar(
                        admin,
                        i.id(),
                        new IncidenteActualizacionRequest(
                                EstadoIncidente.CERRADO, "Accesos revocados", "Rol mal asignado", false, false, null)))
                .isInstanceOf(ConflictException.class);

        IncidenteResponse cerrado = incidentes.actualizar(
                admin,
                i.id(),
                new IncidenteActualizacionRequest(
                        EstadoIncidente.CERRADO, "Accesos revocados", "Rol mal asignado", true, true, null));

        assertThat(cerrado.estado()).isEqualTo(EstadoIncidente.CERRADO);
        assertThat(cerrado.autoridadNotificadaEn()).isNotNull();
        assertThat(cerrado.titularesNotificadosEn()).isNotNull();
        assertThat(cerrado.cerradoEn()).isNotNull();
        assertThatThrownBy(() -> incidentes.actualizar(
                        admin,
                        i.id(),
                        new IncidenteActualizacionRequest(EstadoIncidente.ABIERTO, null, null, false, false, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unIncidenteAntiguoSinNotificarApareceVencido() {
        IncidenteResponse i = incidentes.registrar(
                admin,
                new IncidenteRequest(
                        TipoIncidente.PERDIDA,
                        SeveridadIncidente.MEDIA,
                        "Se extravió un equipo con datos cifrados",
                        "Contactos de pacientes",
                        3,
                        LocalDateTime.now().minusDays(5),
                        "Equipo bloqueado de forma remota"));

        assertThat(i.vencido()).isTrue();
        assertThat(incidentes.vencidos()).isEqualTo(1);
        assertThat(incidentes.abiertos()).isEqualTo(1);
        assertThat(registro.estado().incidentesVencidos()).isEqualTo(1);
        assertThatThrownBy(() -> incidentes.registrar(
                        admin,
                        new IncidenteRequest(
                                TipoIncidente.OTRO,
                                SeveridadIncidente.BAJA,
                                "Incidente con fecha futura inválida",
                                "Ninguno",
                                0,
                                LocalDateTime.now().plusDays(1),
                                null)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void elRegistroDeTratamientosValidaCodigosYRucsDeEncargados() {
        var banco = registro.guardarBanco(
                null,
                new BancoDatosRequest(
                        "PACIENTES",
                        "Pacientes e historias clínicas",
                        "Datos de pacientes",
                        "Atención de salud",
                        "Identificación, salud",
                        true,
                        "Clínica TuSalud",
                        "Servidor propio",
                        "Cifrado y control de acceso",
                        false,
                        null,
                        null,
                        true));
        assertThat(banco.contieneSensibles()).isTrue();
        assertThatThrownBy(() -> registro.guardarBanco(
                        null,
                        new BancoDatosRequest(
                                "PACIENTES",
                                "Duplicado",
                                "x",
                                "x",
                                "x",
                                false,
                                "x",
                                "x",
                                "x",
                                false,
                                null,
                                null,
                                true)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> registro.guardarEncargado(
                        null,
                        new com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.EncargadoRequest(
                                "Proveedor SMS",
                                "20123456789",
                                "Envío de mensajes",
                                "Teléfonos",
                                "Perú",
                                false,
                                null,
                                null,
                                true)))
                .isInstanceOf(DatosInvalidosException.class);
        var encargado = registro.guardarEncargado(
                null,
                new com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.EncargadoRequest(
                        "Proveedor SMS",
                        "20100070970",
                        "Envío de mensajes",
                        "Teléfonos",
                        "Perú",
                        false,
                        LocalDate.now().minusDays(1),
                        null,
                        true));
        assertThat(encargado.contratoVencido()).isTrue();
        assertThat(registro.registro().bancos()).hasSize(1);
        assertThat(registro.registro().encargados()).hasSize(1);
        assertThat(registro.registro().finalidades()).hasSize(2);
    }

    @Test
    void elTitularVeQuienAccedioASusDatos() {
        auditoria.registrar(
                fixture.principal(fixture.usuario("med", RolNombre.ROLE_MEDICO)),
                paciente.getId(),
                AuditoriaClinicaService.HISTORIA_CONSULTADA,
                "Consulta de rutina");
        auditoria.registrar(titular, paciente.getId(), AuditoriaClinicaService.DOCUMENTO_CONSULTADO, "Descarga propia");

        var lista = accesos.misAccesos(titular);

        assertThat(lista).hasSize(2);
        assertThat(lista).extracting(a -> a.realizadoPor()).contains("Usted");
        assertThat(lista).extracting(a -> a.accion()).contains("Consulta de su historia clínica");
        assertThat(accesos.misAccesos(otroPaciente)).isEmpty();
        assertThat(auditoriaRepo.count()).isGreaterThanOrEqualTo(2);
    }
}

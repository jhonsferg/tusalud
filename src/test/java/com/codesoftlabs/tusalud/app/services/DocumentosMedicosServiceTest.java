package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.request.DiagnosticoRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.DocumentoMedicoRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ReferenciaEstadoRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.DocumentoMedicoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.VerificacionPublicaResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.EstablecimientoSalud;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoDocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoReferencia;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.NivelEstablecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.TipoDocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDiagnostico;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.EstablecimientoSaludRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.DocumentoMedicoService;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.VerificacionPublicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class DocumentosMedicosServiceTest {

    @Autowired
    private DocumentoMedicoService servicio;

    @Autowired
    private VerificacionPublicaService verificacion;

    @Autowired
    private AtencionService atencionService;

    @Autowired
    private EstablecimientoSaludRepository establecimientos;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal principalMedico;
    private UsuarioPrincipal principalPaciente;
    private Cita cita;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new java.math.BigDecimal("100.00"));
        paciente = fixture.paciente();
        principalMedico = fixture.principal(medico.getUsuario());
        principalPaciente = fixture.principal(paciente.getUsuario());
        cita = fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
    }

    private DocumentoMedicoRequest certificado(boolean autoriza, java.util.UUID atencionId) {
        Map<String, Object> contenido = new HashMap<>();
        contenido.put("motivo", "Cuadro gripal con fiebre");
        contenido.put("hallazgos", "Faringe congestiva");
        contenido.put("vigenciaDias", 3);
        return new DocumentoMedicoRequest(
                paciente.getId(), TipoDocumentoMedico.CERTIFICADO, atencionId, null, null, null, autoriza, contenido);
    }

    private DocumentoMedicoRequest descanso(LocalDate desde, LocalDate hasta) {
        Map<String, Object> contenido = new HashMap<>();
        contenido.put("tipoDescanso", "ENFERMEDAD");
        contenido.put("desde", desde.toString());
        contenido.put("hasta", hasta.toString());
        contenido.put("contingencia", "Enfermedad común");
        contenido.put("indicacion", "Reposo domiciliario");
        return new DocumentoMedicoRequest(
                paciente.getId(), TipoDocumentoMedico.DESCANSO, null, null, null, null, false, contenido);
    }

    private java.util.UUID atencionConDiagnostico() {
        AtencionResponse atencion = atencionService.registrar(
                cita.getId(),
                new AtencionRequest(
                        "Cuadro febril",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(new DiagnosticoRequest("J06.9", TipoDiagnostico.DEFINITIVO, true, null))),
                principalMedico);
        return atencion.id();
    }

    @Test
    void emiteUnCertificadoInmutableConNumeroCodigoYHashes() {
        DocumentoMedicoDetalle borrador = servicio.crearBorrador(principalMedico, certificado(false, null));
        assertThat(borrador.resumen().estado()).isEqualTo(EstadoDocumentoMedico.BORRADOR);
        assertThat(borrador.resumen().numero()).isNull();

        DocumentoMedicoDetalle emitido =
                servicio.emitir(principalMedico, borrador.resumen().id());

        assertThat(emitido.resumen().estado()).isEqualTo(EstadoDocumentoMedico.EMITIDO);
        assertThat(emitido.resumen().numero()).matches("CM-\\d{4}-000001");
        assertThat(emitido.codigoVerificacion()).matches("^[A-Z2-9]{4}(-[A-Z2-9]{4}){3}$");
        assertThat(servicio.pdf(principalMedico, emitido.resumen().id()).contenido())
                .startsWith("%PDF".getBytes());
        assertThatThrownBy(
                        () -> servicio.emitir(principalMedico, emitido.resumen().id()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> servicio.actualizarBorrador(
                        principalMedico,
                        emitido.resumen().id(),
                        new com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests
                                .ActualizarDocumentoRequest(false, Map.of("motivo", "Otro"))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void numeraDeFormaCorrelativaPorTipo() {
        var primero = servicio.emitir(
                principalMedico,
                servicio.crearBorrador(principalMedico, certificado(false, null))
                        .resumen()
                        .id());
        var segundo = servicio.emitir(
                principalMedico,
                servicio.crearBorrador(principalMedico, certificado(false, null))
                        .resumen()
                        .id());
        var descanso = servicio.emitir(
                principalMedico,
                servicio.crearBorrador(
                                principalMedico,
                                descanso(LocalDate.now(), LocalDate.now().plusDays(2)))
                        .resumen()
                        .id());

        assertThat(primero.resumen().numero()).endsWith("000001");
        assertThat(segundo.resumen().numero()).endsWith("000002");
        assertThat(descanso.resumen().numero()).startsWith("DM-").endsWith("000001");
    }

    @Test
    void anularConservaElDocumentoYPermiteReemplazarlo() {
        var emitido = servicio.emitir(
                principalMedico,
                servicio.crearBorrador(principalMedico, certificado(false, null))
                        .resumen()
                        .id());
        var anulado = servicio.anular(principalMedico, emitido.resumen().id(), "Error en los hallazgos");

        assertThat(anulado.resumen().estado()).isEqualTo(EstadoDocumentoMedico.ANULADO);
        assertThat(anulado.resumen().motivoAnulacion()).isEqualTo("Error en los hallazgos");
        assertThat(servicio.pdf(principalMedico, anulado.resumen().id()).contenido())
                .startsWith("%PDF".getBytes());

        DocumentoMedicoRequest reemplazo = new DocumentoMedicoRequest(
                paciente.getId(),
                TipoDocumentoMedico.CERTIFICADO,
                null,
                null,
                emitido.resumen().id(),
                null,
                false,
                certificado(false, null).contenido());
        var nuevo = servicio.crearBorrador(principalMedico, reemplazo);
        assertThat(nuevo.documentoOrigenId()).isEqualTo(emitido.resumen().id());
        assertThatThrownBy(() -> servicio.crearBorrador(principalMedico, reemplazo))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(
                        () -> servicio.anular(principalMedico, nuevo.resumen().id(), "No emitido"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void soloElMedicoEmisorEmiteYAnula() {
        var borrador = servicio.crearBorrador(principalMedico, certificado(false, null));
        Medico otro = fixture.medicoConHorarioLunes(new java.math.BigDecimal("90.00"));
        fixture.cita(otro, paciente, LocalDateTime.now().plusDays(3), EstadoCita.CONFIRMADA);
        UsuarioPrincipal principalOtro = fixture.principal(otro.getUsuario());

        assertThatThrownBy(
                        () -> servicio.emitir(principalOtro, borrador.resumen().id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(
                        () -> servicio.obtener(principalOtro, borrador.resumen().id()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void incluyeElDiagnosticoSoloConAutorizacionDelPaciente() {
        java.util.UUID atencionId = atencionConDiagnostico();
        var sin = servicio.emitir(
                principalMedico,
                servicio.crearBorrador(principalMedico, certificado(false, atencionId))
                        .resumen()
                        .id());
        var con = servicio.emitir(
                principalMedico,
                servicio.crearBorrador(principalMedico, certificado(true, atencionId))
                        .resumen()
                        .id());

        assertThat(sin.contenido()).doesNotContainKey("diagnosticos");
        assertThat(con.contenido()).containsKey("diagnosticos");
        assertThat(String.valueOf(con.contenido().get("diagnosticos"))).contains("J06.9");
        assertThatThrownBy(() -> servicio.crearBorrador(principalMedico, certificado(true, null)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void validaElDescansoPorDuracionRetroactividadYSolape() {
        assertThatThrownBy(() -> servicio.emitir(
                        principalMedico,
                        servicio.crearBorrador(
                                        principalMedico,
                                        descanso(
                                                LocalDate.now(), LocalDate.now().plusDays(40)))
                                .resumen()
                                .id()))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("30 días");
        assertThatThrownBy(() -> servicio.emitir(
                        principalMedico,
                        servicio.crearBorrador(
                                        principalMedico,
                                        descanso(
                                                LocalDate.now().minusDays(10),
                                                LocalDate.now().minusDays(8)))
                                .resumen()
                                .id()))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("días atrás");

        servicio.emitir(
                principalMedico,
                servicio.crearBorrador(
                                principalMedico,
                                descanso(LocalDate.now(), LocalDate.now().plusDays(4)))
                        .resumen()
                        .id());
        assertThatThrownBy(() -> servicio.emitir(
                        principalMedico,
                        servicio.crearBorrador(
                                        principalMedico,
                                        descanso(
                                                LocalDate.now().plusDays(2),
                                                LocalDate.now().plusDays(6)))
                                .resumen()
                                .id()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void elInformeExigeLaAutorizacionDelPaciente() {
        Map<String, Object> contenido = new HashMap<>();
        contenido.put("destinatario", "Seguro del paciente");
        contenido.put("resumen", "Paciente con evolución favorable");
        var borrador = servicio.crearBorrador(
                principalMedico,
                new DocumentoMedicoRequest(
                        paciente.getId(), TipoDocumentoMedico.INFORME, null, null, null, null, false, contenido));
        assertThatThrownBy(() ->
                        servicio.emitir(principalMedico, borrador.resumen().id()))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("autorización");
    }

    @Test
    void rechazaCamposDesconocidosYTextosExcesivos() {
        Map<String, Object> contenido = new HashMap<>(certificado(false, null).contenido());
        contenido.put("estado", "EMITIDO");
        contenido.put("numero", "CM-2000-000001");
        var borrador = servicio.crearBorrador(
                principalMedico,
                new DocumentoMedicoRequest(
                        paciente.getId(), TipoDocumentoMedico.CERTIFICADO, null, null, null, null, false, contenido));
        assertThat(borrador.contenido()).doesNotContainKeys("estado", "numero");

        Map<String, Object> largo = new HashMap<>(certificado(false, null).contenido());
        largo.put("motivo", "x".repeat(600));
        assertThatThrownBy(() -> servicio.crearBorrador(
                        principalMedico,
                        new DocumentoMedicoRequest(
                                paciente.getId(),
                                TipoDocumentoMedico.CERTIFICADO,
                                null,
                                null,
                                null,
                                null,
                                false,
                                largo)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void referenciaYContrarreferenciaSiguenSuCicloDeEstados() {
        EstablecimientoSalud destino = establecimientos.save(EstablecimientoSalud.builder()
                .nombre("Hospital Demo")
                .nivel(NivelEstablecimiento.TERCER_NIVEL)
                .build());
        Map<String, Object> contenido = new HashMap<>();
        contenido.put("destinoId", destino.getId().toString());
        contenido.put("especialidad", "Cardiología");
        contenido.put("motivo", "Dolor torácico atípico");
        contenido.put("resumenClinico", "Paciente de 54 años con dolor torácico");
        contenido.put("prioridad", "PREFERENTE");
        var referencia = servicio.emitir(
                principalMedico,
                servicio.crearBorrador(
                                principalMedico,
                                new DocumentoMedicoRequest(
                                        paciente.getId(),
                                        TipoDocumentoMedico.REFERENCIA,
                                        null,
                                        null,
                                        null,
                                        null,
                                        false,
                                        contenido))
                        .resumen()
                        .id());
        assertThat(referencia.resumen().referencia().estado()).isEqualTo(EstadoReferencia.ENVIADA);

        assertThatThrownBy(() -> servicio.cambiarEstadoReferencia(
                        principalMedico,
                        referencia.resumen().id(),
                        new ReferenciaEstadoRequest(EstadoReferencia.ATENDIDA, null)))
                .isInstanceOf(ConflictException.class);
        assertThat(servicio.cambiarEstadoReferencia(
                                principalMedico,
                                referencia.resumen().id(),
                                new ReferenciaEstadoRequest(EstadoReferencia.RECIBIDA, null))
                        .estado())
                .isEqualTo(EstadoReferencia.RECIBIDA);

        Map<String, Object> contra = new HashMap<>();
        contra.put("resumen", "Se descartó causa cardiaca");
        var contrarreferencia = servicio.emitir(
                principalMedico,
                servicio.crearBorrador(
                                principalMedico,
                                new DocumentoMedicoRequest(
                                        paciente.getId(),
                                        TipoDocumentoMedico.CONTRARREFERENCIA,
                                        null,
                                        null,
                                        null,
                                        referencia.resumen().referencia().id(),
                                        false,
                                        contra))
                        .resumen()
                        .id());
        assertThat(contrarreferencia.resumen().numero()).startsWith("CR-");
        assertThat(servicio.obtener(principalMedico, referencia.resumen().id())
                        .resumen()
                        .referencia()
                        .estado())
                .isEqualTo(EstadoReferencia.ATENDIDA);
    }

    @Test
    void elPacienteVeSoloLoEmitidoYPropio() {
        var borrador = servicio.crearBorrador(principalMedico, certificado(false, null));
        assertThat(servicio.listarMios(principalPaciente)).isEmpty();
        assertThatThrownBy(() ->
                        servicio.obtener(principalPaciente, borrador.resumen().id()))
                .isInstanceOf(ForbiddenOperationException.class);

        var emitido = servicio.emitir(principalMedico, borrador.resumen().id());
        assertThat(servicio.listarMios(principalPaciente)).hasSize(1);
        assertThat(servicio.pdf(principalPaciente, emitido.resumen().id()).contenido())
                .startsWith("%PDF".getBytes());

        UsuarioPrincipal ajeno = fixture.principal(fixture.paciente().getUsuario());
        assertThatThrownBy(() -> servicio.obtener(ajeno, emitido.resumen().id()))
                .isInstanceOf(ForbiddenOperationException.class);
        UsuarioPrincipal recepcion = fixture.principal(fixture.usuario("rec", RolNombre.ROLE_RECEPCION));
        assertThatThrownBy(() -> servicio.obtener(recepcion, emitido.resumen().id()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void laVerificacionPublicaNoExponeDatosDelPacienteYNoDiferenciaCodigosInvalidos() {
        var emitido = servicio.emitir(
                principalMedico,
                servicio.crearBorrador(principalMedico, certificado(false, null))
                        .resumen()
                        .id());

        VerificacionPublicaResponse valido = verificacion.verificar("10.0.0.1", emitido.codigoVerificacion());
        assertThat(valido.resultado()).isEqualTo("VALIDO");
        assertThat(valido.medico()).isEqualTo(medico.getUsuario().getNombreCompleto());
        assertThat(valido.colegiatura()).isEqualTo(medico.getColegiatura());
        assertThat(valido.toString())
                .doesNotContain(paciente.getDni())
                .doesNotContain(paciente.getUsuario().getEmail());

        servicio.anular(principalMedico, emitido.resumen().id(), "Se emitió por error");
        assertThat(verificacion
                        .verificar("10.0.0.1", emitido.codigoVerificacion().toLowerCase())
                        .resultado())
                .isEqualTo("ANULADO");

        VerificacionPublicaResponse inexistente = verificacion.verificar("10.0.0.1", "AAAA-BBBB-CCCC-DDDD");
        VerificacionPublicaResponse basura = verificacion.verificar("10.0.0.1", "no es un codigo");
        assertThat(inexistente).isEqualTo(basura);
        assertThat(inexistente.resultado()).isEqualTo("NO_ENCONTRADO");
        assertThat(inexistente.tipo()).isNull();
    }

    @Test
    void laVerificacionPublicaTieneLimiteDeTasaPorDireccion() {
        ReflectionTestUtils.setField(verificacion, "porMinuto", 3);
        for (int i = 0; i < 3; i++) {
            verificacion.verificar("10.9.9.9", "AAAA-BBBB-CCCC-DDDD");
        }
        assertThatThrownBy(() -> verificacion.verificar("10.9.9.9", "AAAA-BBBB-CCCC-DDDD"))
                .isInstanceOf(com.codesoftlabs.tusalud.app.exceptions.DemasiadosIntentosException.class);
        assertThat(verificacion.verificar("10.9.9.8", "AAAA-BBBB-CCCC-DDDD").resultado())
                .isEqualTo("NO_ENCONTRADO");
        ReflectionTestUtils.setField(verificacion, "porMinuto", 20);
    }
}

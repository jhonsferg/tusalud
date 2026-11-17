package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AperturaCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CancelarOrdenImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroRequest;
import com.codesoftlabs.tusalud.app.dto.request.EstudioImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.InformeImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.OrdenImagenCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ProgramarImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.RealizarImagenRequest;
import com.codesoftlabs.tusalud.app.dto.response.EstudioImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenImagenResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PacienteCompartido;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.ModalidadImagen;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.AuditoriaClinicaRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteCompartidoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.CajaService;
import com.codesoftlabs.tusalud.app.services.interfaces.ImagenesService;
import com.codesoftlabs.tusalud.app.services.interfaces.ImagenesService.ArchivoEntrante;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.math.BigDecimal;
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
class ImagenesServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 74, 70};
    private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};

    @Autowired
    private ImagenesService imagenesService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private PacienteCompartidoRepository compartidoRepository;

    @Autowired
    private AuditoriaClinicaRepository auditoriaRepository;

    @Autowired
    private CajaService cajaService;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal medicoPrincipal;
    private UsuarioPrincipal pacientePrincipal;
    private UsuarioPrincipal imagenes;
    private UsuarioPrincipal admin;
    private UsuarioPrincipal recepcion;
    private EstudioImagenResponse radiografia;
    private EstudioImagenResponse ecografia;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        medicoPrincipal = fixture.principal(medico.getUsuario());
        pacientePrincipal = fixture.principal(paciente.getUsuario());
        imagenes = fixture.principal(fixture.usuario("img", RolNombre.ROLE_IMAGENES));
        admin = fixture.principal(fixture.usuario("admin", RolNombre.ROLE_ADMIN));
        recepcion = fixture.principal(fixture.usuario("recep", RolNombre.ROLE_RECEPCION));
        fixture.cita(medico, paciente, LocalDateTime.now().minusDays(2), EstadoCita.ATENDIDA);
        radiografia = imagenesService.crearEstudio(estudio(
                "T-RX" + ServiceTestFixture.siguiente(), "Radiografía de prueba", ModalidadImagen.RX, "80.00", 20));
        ecografia = imagenesService.crearEstudio(estudio(
                "T-ECO" + ServiceTestFixture.siguiente(), "Ecografía de prueba", ModalidadImagen.ECO, "120.00", 30));
    }

    private static EstudioImagenRequest estudio(
            String codigo, String nombre, ModalidadImagen modalidad, String precio, int duracion) {
        return new EstudioImagenRequest(
                codigo, nombre, modalidad, "Tórax", "Ayuno de 6 horas", new BigDecimal(precio), duracion, true);
    }

    private OrdenImagenResponse solicitar(PrioridadOrden prioridad, EstudioImagenResponse... estudios) {
        return imagenesService.crearOrden(
                medicoPrincipal,
                new OrdenImagenCreateRequest(
                        paciente.getId(),
                        null,
                        prioridad,
                        "Tos persistente",
                        java.util.Arrays.stream(estudios)
                                .map(EstudioImagenResponse::id)
                                .toList()));
    }

    private static LocalDateTime manana(int hora) {
        return LocalDate.now().plusDays(1).atTime(hora, 0);
    }

    private OrdenImagenResponse programada(String sala, int hora) {
        OrdenImagenResponse orden = solicitar(PrioridadOrden.NORMAL, radiografia);
        return imagenesService.programar(imagenes, orden.id(), new ProgramarImagenRequest(manana(hora), sala));
    }

    private OrdenImagenResponse realizada() {
        OrdenImagenResponse orden = programada("Sala 1", 9);
        imagenesService.subirArchivos(imagenes, orden.id(), List.of(new ArchivoEntrante("toma.png", PNG)));
        return imagenesService.realizar(imagenes, orden.id(), new RealizarImagenRequest("Sin incidencias"));
    }

    private static InformeImagenRequest informe(boolean critico, String motivo) {
        return new InformeImagenRequest("Campos pulmonares limpios", "Sin alteraciones", critico, "cmp-45210", motivo);
    }

    @Test
    void elCatalogoNoAceptaCodigosRepetidos() {
        EstudioImagenRequest repetido = estudio(radiografia.codigo(), "Otro", ModalidadImagen.RX, "50.00", 10);

        assertThatThrownBy(() -> imagenesService.crearEstudio(repetido)).isInstanceOf(ConflictException.class);
        assertThat(imagenesService.listarEstudios("ecografía de prueba", ModalidadImagen.ECO, true))
                .extracting(EstudioImagenResponse::id)
                .containsExactly(ecografia.id());
    }

    @Test
    void elMedicoSoloSolicitaParaPacientesConRelacion() {
        Paciente ajeno = fixture.paciente();
        OrdenImagenCreateRequest solicitud = new OrdenImagenCreateRequest(
                ajeno.getId(), null, PrioridadOrden.NORMAL, "Dolor", List.of(radiografia.id()));

        assertThatThrownBy(() -> imagenesService.crearOrden(medicoPrincipal, solicitud))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> imagenesService.crearOrden(imagenes, solicitud))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void solicitarCalculaElTotalGeneraElCodigoYNotifica() {
        OrdenImagenResponse orden = solicitar(PrioridadOrden.URGENTE, radiografia, ecografia);

        assertThat(orden.estado()).isEqualTo(EstadoOrdenImagen.SOLICITADA);
        assertThat(orden.codigo()).startsWith("IMG-");
        assertThat(orden.total()).isEqualByComparingTo("200.00");
        assertThat(orden.estudios()).hasSize(2);
        assertThat(notificacionService.listar(pacientePrincipal, false, 10))
                .anyMatch(n -> n.tipo() == TipoNotificacion.ORDEN_IMAGEN);
        assertThat(notificacionService.listar(imagenes, false, 10))
                .anyMatch(n ->
                        n.tipo() == TipoNotificacion.ORDEN_IMAGEN && n.titulo().contains("urgente"));
    }

    @Test
    void unEstudioInactivoNoSePuedeSolicitar() {
        imagenesService.actualizarEstudio(
                radiografia.id(),
                new EstudioImagenRequest(
                        radiografia.codigo(),
                        radiografia.nombre(),
                        ModalidadImagen.RX,
                        "Tórax",
                        null,
                        new BigDecimal("80.00"),
                        20,
                        false));

        assertThatThrownBy(() -> solicitar(PrioridadOrden.NORMAL, radiografia)).isInstanceOf(ConflictException.class);
    }

    @Test
    void programarValidaLaFechaYQueLaSalaEsteLibre() {
        OrdenImagenResponse primera = programada("Sala 1", 9);
        OrdenImagenResponse segunda = solicitar(PrioridadOrden.NORMAL, radiografia);

        assertThat(primera.estado()).isEqualTo(EstadoOrdenImagen.PROGRAMADA);
        assertThatThrownBy(() -> imagenesService.programar(
                        imagenes,
                        segunda.id(),
                        new ProgramarImagenRequest(manana(9).plusMinutes(10), "Sala 1")))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> imagenesService.programar(
                        imagenes,
                        segunda.id(),
                        new ProgramarImagenRequest(LocalDateTime.now().minusDays(1), "Sala 2")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThat(imagenesService
                        .programar(imagenes, segunda.id(), new ProgramarImagenRequest(manana(9), "Sala 2"))
                        .sala())
                .isEqualTo("Sala 2");
        assertThat(imagenesService
                        .programar(
                                imagenes,
                                primera.id(),
                                new ProgramarImagenRequest(manana(9).plusMinutes(5), "Sala 1"))
                        .estado())
                .isEqualTo(EstadoOrdenImagen.PROGRAMADA);
    }

    @Test
    void registrarLaTomaExigeAlMenosUnaImagen() {
        OrdenImagenResponse orden = programada("Sala 1", 9);

        assertThatThrownBy(() -> imagenesService.realizar(imagenes, orden.id(), null))
                .isInstanceOf(DatosInvalidosException.class);
        imagenesService.subirArchivos(imagenes, orden.id(), List.of(new ArchivoEntrante("toma.jpg", JPEG)));
        OrdenImagenResponse realizada = imagenesService.realizar(imagenes, orden.id(), null);

        assertThat(realizada.estado()).isEqualTo(EstadoOrdenImagen.REALIZADA);
        assertThat(realizada.realizadaPor()).isNotBlank();
    }

    @Test
    void losArchivosSeValidanPorContenidoTamanoYCantidad() {
        OrdenImagenResponse orden = programada("Sala 1", 9);
        byte[] falsoPng = "no soy una imagen".getBytes();
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);

        assertThatThrownBy(() -> imagenesService.subirArchivos(
                        imagenes, orden.id(), List.of(new ArchivoEntrante("falsa.png", falsoPng))))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> imagenesService.subirArchivos(
                        imagenes, orden.id(), List.of(new ArchivoEntrante("grande.png", grande))))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> imagenesService.subirArchivos(
                        imagenes, orden.id(), List.of(new ArchivoEntrante("vacia.png", new byte[0]))))
                .isInstanceOf(DatosInvalidosException.class);

        var subidos = imagenesService.subirArchivos(
                imagenes,
                orden.id(),
                List.of(
                        new ArchivoEntrante("..\\..\\etc/pass wd.png", PNG),
                        new ArchivoEntrante("informe.pdf", PDF),
                        new ArchivoEntrante("camara.jpg", JPEG)));
        assertThat(subidos)
                .extracting(a -> a.tipoContenido())
                .containsExactly("image/png", "application/pdf", "image/jpeg");
        assertThat(subidos.getFirst().nombre()).isEqualTo("pass wd.png");

        List<ArchivoEntrante> nueve = java.util.Collections.nCopies(9, new ArchivoEntrante("x.png", PNG));
        imagenesService.subirArchivos(imagenes, orden.id(), nueve);
        assertThatThrownBy(() -> imagenesService.subirArchivos(
                        imagenes, orden.id(), List.of(new ArchivoEntrante("trece.png", PNG))))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void noSeCarganImagenesEnUnaOrdenSolicitadaNiInformada() {
        OrdenImagenResponse solicitada = solicitar(PrioridadOrden.NORMAL, radiografia);
        assertThatThrownBy(() -> imagenesService.subirArchivos(
                        imagenes, solicitada.id(), List.of(new ArchivoEntrante("a.png", PNG))))
                .isInstanceOf(ConflictException.class);

        OrdenImagenResponse realizada = realizada();
        imagenesService.informar(imagenes, realizada.id(), informe(false, null));
        assertThatThrownBy(() -> imagenesService.subirArchivos(
                        imagenes, realizada.id(), List.of(new ArchivoEntrante("b.png", PNG))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void informarCierraLaOrdenNotificaYConservaElHistorialDeCorrecciones() {
        OrdenImagenResponse orden = realizada();

        OrdenImagenResponse informada = imagenesService.informar(imagenes, orden.id(), informe(false, null));

        assertThat(informada.estado()).isEqualTo(EstadoOrdenImagen.INFORMADA);
        assertThat(informada.informe().version()).isEqualTo(1);
        assertThat(informada.informe().firmaColegiatura()).isEqualTo("CMP-45210");
        assertThat(notificacionService.listar(pacientePrincipal, false, 10))
                .anyMatch(n -> n.tipo() == TipoNotificacion.ESTUDIO_INFORMADO);
        assertThat(notificacionService.listar(medicoPrincipal, false, 10))
                .anyMatch(n -> n.tipo() == TipoNotificacion.ESTUDIO_INFORMADO);

        assertThatThrownBy(() -> imagenesService.informar(imagenes, orden.id(), informe(false, null)))
                .isInstanceOf(DatosInvalidosException.class);
        OrdenImagenResponse corregida =
                imagenesService.informar(imagenes, orden.id(), informe(true, "Segunda lectura"));

        assertThat(corregida.informe().version()).isEqualTo(2);
        assertThat(corregida.hallazgoCritico()).isTrue();
        assertThat(imagenesService.historialInformes(imagenes, orden.id())).hasSize(2);
        assertThat(imagenesService
                        .historialInformes(medicoPrincipal, orden.id())
                        .getFirst()
                        .motivoCorreccion())
                .isEqualTo("Segunda lectura");
        assertThat(notificacionService.listar(medicoPrincipal, false, 10))
                .anyMatch(n -> n.tipo() == TipoNotificacion.HALLAZGO_CRITICO);
        assertThat(imagenesService.miFirma(imagenes).colegiatura()).isEqualTo("CMP-45210");
    }

    @Test
    void noSeInformaUnaOrdenSinRealizar() {
        OrdenImagenResponse orden = programada("Sala 1", 9);

        assertThatThrownBy(() -> imagenesService.informar(imagenes, orden.id(), informe(false, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void elPacienteSoloVeElInformeCuandoElEstudioEstaInformado() {
        OrdenImagenResponse orden = realizada();

        OrdenImagenResponse antes = imagenesService.obtenerOrden(pacientePrincipal, orden.id());
        assertThat(antes.indicacionClinica()).isNull();
        assertThat(antes.archivos()).isEmpty();
        assertThat(antes.informe()).isNull();
        assertThat(antes.dni()).isNull();
        var archivoId = imagenesService
                .obtenerOrden(imagenes, orden.id())
                .archivos()
                .getFirst()
                .id();
        assertThatThrownBy(() -> imagenesService.descargarArchivo(pacientePrincipal, orden.id(), archivoId))
                .isInstanceOf(ForbiddenOperationException.class);

        imagenesService.informar(imagenes, orden.id(), informe(true, null));
        OrdenImagenResponse despues = imagenesService.obtenerOrden(pacientePrincipal, orden.id());

        assertThat(despues.informe().conclusion()).isEqualTo("Sin alteraciones");
        assertThat(despues.informe().hallazgoCritico()).isNull();
        assertThat(despues.hallazgoCritico()).isNull();
        assertThat(despues.archivos()).hasSize(1);
        assertThat(imagenesService
                        .descargarArchivo(pacientePrincipal, orden.id(), archivoId)
                        .tipoContenido())
                .isEqualTo("image/png");
        assertThat(imagenesService.misEstudios(pacientePrincipal))
                .extracting(OrdenImagenResponse::id)
                .contains(orden.id());
        assertThatThrownBy(() -> imagenesService.historialInformes(pacientePrincipal, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void elPacienteVeLaPreparacionMientrasEstaProgramado() {
        OrdenImagenResponse orden = programada("Sala 1", 9);

        OrdenImagenResponse vista = imagenesService.obtenerOrden(pacientePrincipal, orden.id());

        assertThat(vista.estudios().getFirst().preparacion()).isEqualTo("Ayuno de 6 horas");
        assertThat(vista.programadaPara()).isEqualTo(manana(9));
        assertThat(vista.sala()).isEqualTo("Sala 1");
    }

    @Test
    void nadieMasAccedeALaOrden() {
        OrdenImagenResponse orden = realizada();
        imagenesService.informar(imagenes, orden.id(), informe(false, null));
        UsuarioPrincipal otroPaciente = fixture.principal(fixture.paciente().getUsuario());
        Medico otroMedico = fixture.medicoConHorarioLunes(new BigDecimal("90.00"));
        UsuarioPrincipal sinRelacion = fixture.principal(otroMedico.getUsuario());
        UsuarioPrincipal farmacia = fixture.principal(fixture.usuario("farm", RolNombre.ROLE_FARMACIA));

        assertThatThrownBy(() -> imagenesService.obtenerOrden(otroPaciente, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> imagenesService.obtenerOrden(sinRelacion, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> imagenesService.obtenerOrden(farmacia, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> imagenesService.ordenesDePaciente(sinRelacion, paciente.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void unMedicoConPacienteCompartidoVeLaOrdenYQuedaAuditado() {
        OrdenImagenResponse orden = realizada();
        Medico colega = fixture.medicoConHorarioLunes(new BigDecimal("90.00"));
        UsuarioPrincipal colegaPrincipal = fixture.principal(colega.getUsuario());
        compartidoRepository.save(PacienteCompartido.builder()
                .paciente(paciente)
                .medicoOrigen(medico)
                .medicoDestino(colega)
                .nota("Interconsulta")
                .build());

        OrdenImagenResponse vista = imagenesService.obtenerOrden(colegaPrincipal, orden.id());

        assertThat(vista.indicacionClinica()).isEqualTo("Tos persistente");
        assertThat(imagenesService.ordenesDePaciente(colegaPrincipal, paciente.getId()))
                .extracting(OrdenImagenResponse::id)
                .contains(orden.id());
        assertThat(auditoriaRepository.findAll())
                .anyMatch(a -> AuditoriaClinicaService.IMAGENES_CONSULTADAS.equals(a.getAccion())
                        && a.getPaciente().getId().equals(paciente.getId()));
    }

    @Test
    void administracionYRecepcionSoloVenLaAgendaSinDatosClinicos() {
        OrdenImagenResponse orden = realizada();
        imagenesService.informar(imagenes, orden.id(), informe(false, null));

        for (UsuarioPrincipal actor : List.of(admin, recepcion)) {
            OrdenImagenResponse vista = imagenesService.obtenerOrden(actor, orden.id());
            assertThat(vista.indicacionClinica()).isNull();
            assertThat(vista.dni()).isNull();
            assertThat(vista.informe()).isNull();
            assertThat(vista.archivos()).isEmpty();
            assertThat(vista.estudios()).isEmpty();
            assertThat(vista.total()).isNull();
            assertThat(vista.sala()).isEqualTo("Sala 1");
        }
        var archivoId = imagenesService
                .obtenerOrden(imagenes, orden.id())
                .archivos()
                .getFirst()
                .id();
        assertThatThrownBy(() -> imagenesService.descargarArchivo(admin, orden.id(), archivoId))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void laAgendaListaLasOrdenesProgramadasSinDatosClinicos() {
        programada("Sala 1", 9);
        LocalDate dia = LocalDate.now().plusDays(1);

        var agenda = imagenesService.agenda(dia, dia);

        assertThat(agenda).hasSize(1);
        assertThat(agenda.getFirst().sala()).isEqualTo("Sala 1");
        assertThat(agenda.getFirst().duracionMinutos()).isEqualTo(20);
        assertThat(agenda.getFirst().modalidades()).containsExactly(ModalidadImagen.RX);
        assertThatThrownBy(() -> imagenesService.agenda(dia, dia.minusDays(1)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> imagenesService.agenda(dia, dia.plusDays(60)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void cancelarRespetaQuienPuedeYEnQueEstado() {
        OrdenImagenResponse solicitada = solicitar(PrioridadOrden.NORMAL, radiografia);
        OrdenImagenResponse programada = programada("Sala 1", 14);

        assertThat(imagenesService
                        .cancelar(medicoPrincipal, solicitada.id(), new CancelarOrdenImagenRequest("Error"))
                        .estado())
                .isEqualTo(EstadoOrdenImagen.CANCELADA);
        assertThatThrownBy(() -> imagenesService.cancelar(medicoPrincipal, programada.id(), null))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(imagenesService.cancelar(imagenes, programada.id(), null).estado())
                .isEqualTo(EstadoOrdenImagen.CANCELADA);
        OrdenImagenResponse realizada = realizada();
        assertThatThrownBy(() -> imagenesService.cancelar(imagenes, realizada.id(), null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void laCajaCobraElEstudioRealizadoSinDatosClinicos() {
        OrdenImagenResponse orden = realizada();
        UsuarioPrincipal cajero = fixture.principal(fixture.usuario("caja", RolNombre.ROLE_CAJA));
        cajaService.abrir(cajero, new AperturaCajaRequest(new BigDecimal("50.00")));

        var pendientes = cajaService.pendientes(null);
        assertThat(pendientes)
                .anyMatch(c -> c.origen() == OrigenComprobante.IMAGEN
                        && c.referenciaId().equals(orden.id())
                        && c.descripcion().contains(orden.codigo()));

        var comprobante = cajaService.cobrar(
                cajero,
                new CobroRequest(
                        OrigenComprobante.IMAGEN,
                        orden.id(),
                        null,
                        TipoComprobante.BOLETA,
                        TipoDocumentoCliente.SIN_DOC,
                        null,
                        "Cliente",
                        MetodoPagoCaja.EFECTIVO,
                        null));

        assertThat(comprobante.total()).isEqualByComparingTo("80.00");
        assertThat(cajaService.pendientes(null))
                .noneMatch(c -> c.origen() == OrigenComprobante.IMAGEN
                        && c.referenciaId().equals(orden.id()));
        cajaService.anular(cajero, comprobante.id(), "Error de cobro");
        assertThat(cajaService.pendientes(null))
                .anyMatch(c -> c.origen() == OrigenComprobante.IMAGEN
                        && c.referenciaId().equals(orden.id()));
    }

    @Test
    void noSeCobraUnEstudioSinRealizar() {
        OrdenImagenResponse orden = solicitar(PrioridadOrden.NORMAL, radiografia);
        UsuarioPrincipal cajero = fixture.principal(fixture.usuario("caja", RolNombre.ROLE_CAJA));
        cajaService.abrir(cajero, new AperturaCajaRequest(new BigDecimal("50.00")));

        assertThatThrownBy(() -> cajaService.cobrar(
                        cajero,
                        new CobroRequest(
                                OrigenComprobante.IMAGEN,
                                orden.id(),
                                null,
                                TipoComprobante.BOLETA,
                                TipoDocumentoCliente.SIN_DOC,
                                null,
                                "Cliente",
                                MetodoPagoCaja.EFECTIVO,
                                null)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void elResumenCuentaLasOrdenesPorEstado() {
        solicitar(PrioridadOrden.URGENTE, radiografia);
        realizada();

        var resumen = imagenesService.resumen();

        assertThat(resumen.solicitadas()).isGreaterThanOrEqualTo(1);
        assertThat(resumen.porInformar()).isGreaterThanOrEqualTo(1);
        assertThat(resumen.urgentes()).isGreaterThanOrEqualTo(1);
    }
}

package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.documentos.DocumentoGenerado;
import com.codesoftlabs.tusalud.app.documentos.FormatoDocumento;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.dto.request.AperturaCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroRequest;
import com.codesoftlabs.tusalud.app.dto.request.EstudioImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.ExamenLabRequest;
import com.codesoftlabs.tusalud.app.dto.request.ExportacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.InformeImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.OrdenImagenCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.OrdenLabCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ProgramarImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.RealizarImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.ResultadosLabRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprobanteResponse;
import com.codesoftlabs.tusalud.app.dto.response.ExamenLabResponse;
import com.codesoftlabs.tusalud.app.dto.response.ExportacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenLabResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Exportacion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaExamen;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoExportacion;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.ModalidadImagen;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMuestra;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoParametro;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.DemasiadosIntentosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.AuditoriaClinicaRepository;
import com.codesoftlabs.tusalud.app.repositories.ExportacionRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService.Descarga;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.CajaService;
import com.codesoftlabs.tusalud.app.services.interfaces.DocumentoService;
import com.codesoftlabs.tusalud.app.services.interfaces.ExportacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.ImagenesService;
import com.codesoftlabs.tusalud.app.services.interfaces.ImagenesService.ArchivoEntrante;
import com.codesoftlabs.tusalud.app.services.interfaces.LaboratorioService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.PlantillaDocumentoService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
class DocumentoServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private ExportacionService exportacionService;

    @Autowired
    private PlantillaDocumentoService plantillaService;

    @Autowired
    private AtencionService atencionService;

    @Autowired
    private CajaService cajaService;

    @Autowired
    private LaboratorioService laboratorioService;

    @Autowired
    private ImagenesService imagenesService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private ExportacionRepository exportacionRepository;

    @Autowired
    private AuditoriaClinicaRepository auditoriaRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private Paciente otroPaciente;
    private UsuarioPrincipal medicoPrincipal;
    private UsuarioPrincipal otroMedico;
    private UsuarioPrincipal pacientePrincipal;
    private UsuarioPrincipal otroPacientePrincipal;
    private UsuarioPrincipal cajero;
    private UsuarioPrincipal otroCajero;
    private UsuarioPrincipal admin;
    private UsuarioPrincipal farmacia;
    private UsuarioPrincipal laboratorio;
    private UsuarioPrincipal imagenes;
    private UsuarioPrincipal gerencia;
    private UsuarioPrincipal finanzas;
    private UsuarioPrincipal rrhh;
    private Cita citaAtendida;
    private AtencionResponse atencion;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        otroPaciente = fixture.paciente();
        medicoPrincipal = fixture.principal(medico.getUsuario());
        otroMedico = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("90.00")).getUsuario());
        pacientePrincipal = fixture.principal(paciente.getUsuario());
        otroPacientePrincipal = fixture.principal(otroPaciente.getUsuario());
        cajero = fixture.principal(fixture.usuario("caja", RolNombre.ROLE_CAJA));
        otroCajero = fixture.principal(fixture.usuario("caja2", RolNombre.ROLE_CAJA));
        admin = fixture.principal(fixture.usuario("admin", RolNombre.ROLE_ADMIN));
        farmacia = fixture.principal(fixture.usuario("farm", RolNombre.ROLE_FARMACIA));
        laboratorio = fixture.principal(fixture.usuario("lab", RolNombre.ROLE_LABORATORIO));
        imagenes = fixture.principal(fixture.usuario("img", RolNombre.ROLE_IMAGENES));
        gerencia = fixture.principal(fixture.usuario("ger", RolNombre.ROLE_GERENCIA));
        finanzas = fixture.principal(fixture.usuario("fin", RolNombre.ROLE_FINANZAS));
        rrhh = fixture.principal(fixture.usuario("rrhh", RolNombre.ROLE_RRHH));
        fixture.cita(medico, paciente, LocalDateTime.now().minusDays(3), EstadoCita.ATENDIDA);
        Cita cita =
                fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2).withNano(0), EstadoCita.CONFIRMADA);
        atencion = atencionService.registrar(
                cita.getId(),
                new AtencionRequest(
                        "Hipertensión leve",
                        "Dieta baja en sal",
                        "Control en 30 días",
                        null,
                        new BigDecimal("72.5"),
                        new BigDecimal("170.0"),
                        "120/80",
                        new BigDecimal("36.5"),
                        72,
                        List.of(new RecetaItemRequest("Losartán 50 mg", "1 tableta", "Cada 24 horas", "30 días"))),
                medicoPrincipal);
        citaAtendida = cita;
    }

    private static String texto(DocumentoGenerado documento) throws IOException {
        assertThat(documento.formato()).isEqualTo(FormatoDocumento.PDF);
        assertThat(new String(documento.contenido(), 0, 5, StandardCharsets.ISO_8859_1))
                .isEqualTo("%PDF-");
        PdfReader lector = new PdfReader(documento.contenido());
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder todo = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                todo.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return todo.toString();
        } finally {
            lector.close();
        }
    }

    private DocumentoGenerado directo(UsuarioPrincipal actor, TipoDocumento tipo, UUID id) {
        return documentoService.generarDirecto(actor, tipo, id);
    }

    private UUID comprobanteDeCita(UsuarioPrincipal cajeroActual) {
        Cita atendida = fixture.cita(medico, paciente, LocalDateTime.now().minusDays(1), EstadoCita.ATENDIDA);
        cajaService.abrir(cajeroActual, new AperturaCajaRequest(new BigDecimal("100.00")));
        ComprobanteResponse comprobante = cajaService.cobrar(
                cajeroActual,
                new CobroRequest(
                        OrigenComprobante.CITA,
                        atendida.getId(),
                        null,
                        TipoComprobante.BOLETA,
                        TipoDocumentoCliente.DNI,
                        "12345678",
                        "José Núñez",
                        MetodoPagoCaja.YAPE,
                        null));
        return comprobante.id();
    }

    @Test
    void elComprobanteSeImprimeSoloPorSuCajeroSuPacienteYElAdministrador() throws Exception {
        UUID id = comprobanteDeCita(cajero);

        String contenido = texto(directo(cajero, TipoDocumento.COMPROBANTE, id));
        assertThat(contenido)
                .contains("Boleta B001-")
                .contains("José Núñez")
                .contains("IGV 18 %")
                .contains("S/ 100.00");
        assertThat(contenido).contains("Código de verificación").contains("Generado por");
        assertThat(texto(directo(pacientePrincipal, TipoDocumento.COMPROBANTE, id)))
                .contains("Boleta B001-");
        assertThat(texto(directo(admin, TipoDocumento.COMPROBANTE, id))).contains("Boleta B001-");

        assertThatThrownBy(() -> directo(otroCajero, TipoDocumento.COMPROBANTE, id))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> directo(otroPacientePrincipal, TipoDocumento.COMPROBANTE, id))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> directo(medicoPrincipal, TipoDocumento.COMPROBANTE, id))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void unComprobanteAnuladoLlevaLaMarcaYElMotivo() throws Exception {
        UUID id = comprobanteDeCita(cajero);
        cajaService.anular(cajero, id, "Error de digitación");

        String contenido = texto(directo(cajero, TipoDocumento.COMPROBANTE, id));

        assertThat(contenido).contains("COMPROBANTE ANULADO").contains("Error de digitación");
    }

    @Test
    void laRecetaLaImprimenElMedicoQueLaEmitioElPacienteYLaFarmacia() throws Exception {
        String contenido = texto(directo(medicoPrincipal, TipoDocumento.RECETA, atencion.id()));
        assertThat(contenido)
                .contains("Receta médica")
                .contains("Losartán 50 mg")
                .contains("Hipertensión leve");
        assertThat(texto(directo(pacientePrincipal, TipoDocumento.RECETA, atencion.id())))
                .contains("Losartán 50 mg");
        assertThat(texto(directo(farmacia, TipoDocumento.RECETA, atencion.id())))
                .contains("Losartán 50 mg");

        assertThatThrownBy(() -> directo(otroMedico, TipoDocumento.RECETA, atencion.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> directo(otroPacientePrincipal, TipoDocumento.RECETA, atencion.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> directo(admin, TipoDocumento.RECETA, atencion.id()))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("permiso");
        assertThatThrownBy(() -> directo(cajero, TipoDocumento.RECETA, atencion.id()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void laConstanciaSoloLaGeneranElPacienteYElMedicoDeLaCita() throws Exception {
        String contenido = texto(directo(pacientePrincipal, TipoDocumento.CONSTANCIA, citaAtendida.getId()));
        assertThat(contenido)
                .contains("Constancia")
                .contains(medico.getUsuario().getNombreCompleto());
        assertThat(texto(directo(medicoPrincipal, TipoDocumento.CONSTANCIA, citaAtendida.getId())))
                .contains("Constancia");

        assertThatThrownBy(() -> directo(otroPacientePrincipal, TipoDocumento.CONSTANCIA, citaAtendida.getId()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> directo(admin, TipoDocumento.CONSTANCIA, citaAtendida.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void elResumenDeHistoriaSeAuditaYSoloLoVenElMedicoConRelacionYElPaciente() throws Exception {
        long antes = auditoriaRepository.count();

        String paraMedico = texto(directo(medicoPrincipal, TipoDocumento.RESUMEN_HISTORIA, paciente.getId()));
        assertThat(paraMedico).contains("Resumen de historia clínica").contains("Hipertensión leve");
        assertThat(auditoriaRepository.count()).isGreaterThan(antes);
        assertThat(auditoriaRepository.findAll())
                .anyMatch(registro -> AuditoriaClinicaService.DOCUMENTO_GENERADO.equals(registro.getAccion()));

        assertThat(texto(directo(pacientePrincipal, TipoDocumento.RESUMEN_HISTORIA, paciente.getId())))
                .contains("Hipertensión leve");
        assertThatThrownBy(() -> directo(pacientePrincipal, TipoDocumento.RESUMEN_HISTORIA, otroPaciente.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> directo(otroMedico, TipoDocumento.RESUMEN_HISTORIA, paciente.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> directo(admin, TipoDocumento.RESUMEN_HISTORIA, paciente.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void losResultadosDeLaboratorioSoloSeImprimenCompletadosYResaltanLoFueraDeRango() throws Exception {
        ExamenLabResponse glucosa = laboratorioService.crearExamen(new ExamenLabRequest(
                "T-GLU" + ServiceTestFixture.siguiente(),
                "Glucosa de prueba",
                CategoriaExamen.BIOQUIMICA,
                TipoMuestra.SANGRE,
                new BigDecimal("15.00"),
                4,
                "Ayuno",
                true,
                List.of(new ExamenLabRequest.ParametroRequest(
                        "Glucosa", "mg/dL", TipoParametro.NUMERICO, new BigDecimal("70"), new BigDecimal("100"), 1))));
        OrdenLabResponse orden = laboratorioService.crearOrden(
                medicoPrincipal,
                new OrdenLabCreateRequest(
                        paciente.getId(), null, PrioridadOrden.NORMAL, "Control", List.of(glucosa.id())));

        assertThatThrownBy(() -> directo(medicoPrincipal, TipoDocumento.RESULTADOS_LABORATORIO, orden.id()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> directo(pacientePrincipal, TipoDocumento.RESULTADOS_LABORATORIO, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);

        laboratorioService.tomarMuestra(laboratorio, orden.id());
        laboratorioService.registrarResultados(
                laboratorio,
                orden.id(),
                orden.items().get(0).id(),
                new ResultadosLabRequest(
                        List.of(new ResultadosLabRequest.ResultadoValor(
                                glucosa.parametros().get(0).id(), "180")),
                        null));
        laboratorioService.validar(laboratorio, orden.id());

        String contenido = texto(directo(pacientePrincipal, TipoDocumento.RESULTADOS_LABORATORIO, orden.id()));
        assertThat(contenido)
                .contains("Resultados de laboratorio")
                .contains("180")
                .contains("Fuera de rango");
        assertThat(texto(directo(medicoPrincipal, TipoDocumento.RESULTADOS_LABORATORIO, orden.id())))
                .contains("Glucosa");
        assertThat(texto(directo(laboratorio, TipoDocumento.RESULTADOS_LABORATORIO, orden.id())))
                .contains("Glucosa");
        assertThatThrownBy(() -> directo(otroPacientePrincipal, TipoDocumento.RESULTADOS_LABORATORIO, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> directo(admin, TipoDocumento.RESULTADOS_LABORATORIO, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void elInformeDeImagenesLlevaLaFirmaYSoloSeImprimeInformado() throws Exception {
        var estudio = imagenesService.crearEstudio(new EstudioImagenRequest(
                "T-RX" + ServiceTestFixture.siguiente(),
                "Radiografía de tórax",
                ModalidadImagen.RX,
                "Tórax",
                "Sin preparación",
                new BigDecimal("80.00"),
                20,
                true));
        OrdenImagenResponse orden = imagenesService.crearOrden(
                medicoPrincipal,
                new OrdenImagenCreateRequest(
                        paciente.getId(), null, PrioridadOrden.NORMAL, "Tos persistente", List.of(estudio.id())));
        assertThatThrownBy(() -> directo(medicoPrincipal, TipoDocumento.INFORME_IMAGEN, orden.id()))
                .isInstanceOf(ConflictException.class);

        imagenesService.programar(
                imagenes,
                orden.id(),
                new ProgramarImagenRequest(
                        LocalDateTime.now().plusDays(1).withHour(9).withMinute(0), "Sala 1"));
        imagenesService.subirArchivos(imagenes, orden.id(), List.of(new ArchivoEntrante("toma.png", PNG)));
        imagenesService.realizar(imagenes, orden.id(), new RealizarImagenRequest("Sin incidencias"));
        imagenesService.informar(
                imagenes,
                orden.id(),
                new InformeImagenRequest("Campos pulmonares limpios", "Sin alteraciones", false, "cmp-45210", null));

        String contenido = texto(directo(pacientePrincipal, TipoDocumento.INFORME_IMAGEN, orden.id()));
        assertThat(contenido).contains("Informe de imágenes").contains("Campos pulmonares limpios");
        assertThat(contenido).contains("Sin alteraciones").contains("CMP");
        assertThat(texto(directo(medicoPrincipal, TipoDocumento.INFORME_IMAGEN, orden.id())))
                .contains("Radiografía");
        assertThat(texto(directo(imagenes, TipoDocumento.INFORME_IMAGEN, orden.id())))
                .contains("Radiografía");
        assertThatThrownBy(() -> directo(otroPacientePrincipal, TipoDocumento.INFORME_IMAGEN, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> directo(admin, TipoDocumento.INFORME_IMAGEN, orden.id()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void unTipoDeExportacionNoSeGeneraDeFormaDirectaNiViceversa() {
        assertThatThrownBy(() ->
                        documentoService.generarDirecto(gerencia, TipoDocumento.REPORTE_EJECUTIVO, UUID.randomUUID()))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() ->
                        documentoService.generar(medicoPrincipal, TipoDocumento.RECETA, FormatoDocumento.PDF, Map.of()))
                .isInstanceOf(DatosInvalidosException.class);
    }

    private ExportacionResponse exportar(
            UsuarioPrincipal actor, String tipo, String formato, Map<String, String> parametros) {
        return exportacionService.solicitar(actor, new ExportacionRequest(tipo, formato, parametros));
    }

    @Test
    void unReporteSeExportaAPdfYExcelYSoloLoDescargaQuienLoSolicito() throws Exception {
        ExportacionResponse pdf = exportar(gerencia, "reporte-ejecutivo", "PDF", Map.of());
        assertThat(pdf.estado()).as(pdf.error()).isEqualTo(EstadoExportacion.LISTA);
        assertThat(pdf.nombre()).endsWith(".pdf");
        assertThat(pdf.expiraEn()).isAfter(LocalDateTime.now().plusDays(6));
        Descarga descarga = exportacionService.descargar(gerencia, pdf.id());
        byte[] contenido = descarga.objeto().flujo().readAllBytes();
        assertThat(new String(contenido, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");

        ExportacionResponse excel = exportar(gerencia, "reporte-ejecutivo", "XLSX", Map.of("agrupacion", "SEMANA"));
        assertThat(excel.estado()).as(excel.error()).isEqualTo(EstadoExportacion.LISTA);
        byte[] libro = exportacionService
                .descargar(gerencia, excel.id())
                .objeto()
                .flujo()
                .readAllBytes();
        try (XSSFWorkbook leido = new XSSFWorkbook(new java.io.ByteArrayInputStream(libro))) {
            assertThat(leido.getSheet("Indicadores")).isNotNull();
        }

        assertThat(exportacionService.mias(gerencia)).hasSize(2);
        assertThat(exportacionService.mias(finanzas)).isEmpty();
        assertThatThrownBy(() -> exportacionService.descargar(finanzas, pdf.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> exportacionService.obtener(admin, pdf.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> exportacionService.eliminar(admin, pdf.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(notificacionService.listar(gerencia, false, 10))
                .anyMatch(n -> n.tipo() == TipoNotificacion.EXPORTACION_LISTA);

        exportacionService.eliminar(gerencia, pdf.id());
        assertThat(exportacionService.mias(gerencia)).hasSize(1);
    }

    @Test
    void cadaRolSoloExportaSusReportesYFormatosValidos() {
        assertThat(exportar(finanzas, "reporte-financiero", "XLSX", Map.of()).estado())
                .isEqualTo(EstadoExportacion.LISTA);
        assertThatThrownBy(() -> exportar(finanzas, "reporte-operativo", "PDF", Map.of()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> exportar(rrhh, "reporte-ejecutivo", "PDF", Map.of()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> exportar(gerencia, "kardex", "PDF", Map.of())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> exportar(gerencia, "listado-comprobantes", "XLSX", Map.of()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> exportar(gerencia, "receta", "PDF", Map.of()))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> exportar(gerencia, "no-existe", "PDF", Map.of()))
                .isInstanceOf(DatosInvalidosException.class);
        assertThat(exportar(cajero, "listado-comprobantes", "XLSX", Map.of("q", "=cmd"))
                        .estado())
                .isEqualTo(EstadoExportacion.LISTA);
        assertThat(exportar(medicoPrincipal, "mis-citas", "XLSX", Map.of()).estado())
                .isEqualTo(EstadoExportacion.LISTA);
    }

    @Test
    void rechazaParametrosDesconocidosFechasInvalidasYRangosExcesivos() {
        assertThatThrownBy(() -> exportar(gerencia, "reporte-ejecutivo", "PDF", Map.of("sql", "1")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("sql");
        assertThatThrownBy(() -> exportar(gerencia, "reporte-ejecutivo", "PDF", Map.of("desde", "ayer")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> exportar(
                        gerencia, "reporte-ejecutivo", "PDF", Map.of("desde", "2020-01-01", "hasta", "2026-01-01")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("366");
        assertThatThrownBy(() -> exportar(gerencia, "kardex", "XLSX", Map.of("productoId", "no-es-uuid")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void limitaLasExportacionesPorHora() {
        for (int i = 0; i < 20; i++) {
            exportar(gerencia, "reporte-ejecutivo", "XLSX", Map.of());
        }
        assertThatThrownBy(() -> exportar(gerencia, "reporte-ejecutivo", "XLSX", Map.of()))
                .isInstanceOf(DemasiadosIntentosException.class)
                .hasMessageContaining("por hora");
    }

    @Test
    void laPurgaBorraLasExportacionesVencidasYMarcaLasAtascadas() {
        ExportacionResponse lista = exportar(gerencia, "reporte-ejecutivo", "XLSX", Map.of());
        Exportacion vencida = exportacionRepository.findConArchivo(lista.id()).orElseThrow();
        vencida.setExpiraEn(LocalDateTime.now().minusMinutes(1));
        exportacionRepository.saveAndFlush(vencida);
        Exportacion atascada = exportacionRepository.saveAndFlush(Exportacion.builder()
                .tipo(TipoDocumento.REPORTE_CALIDAD)
                .formato(FormatoDocumento.PDF)
                .estado(EstadoExportacion.PROCESANDO)
                .solicitadoPor(vencida.getSolicitadoPor())
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        exportacionService.purgar();

        assertThat(exportacionService.obtener(gerencia, lista.id()).estado()).isEqualTo(EstadoExportacion.EXPIRADA);
        assertThatThrownBy(() -> exportacionService.descargar(gerencia, lista.id()))
                .isInstanceOf(ConflictException.class);
        assertThat(exportacionService.obtener(gerencia, atascada.getId()).estado())
                .isEqualTo(EstadoExportacion.ERROR);
    }

    @Test
    void elAdministradorPersonalizaYRestauraUnaPlantillaYElPdfLaUsa() throws Exception {
        String original = new String(plantillaService.descargar("constancia").contenido(), StandardCharsets.UTF_8);
        assertThat(plantillaService.listar())
                .filteredOn(p -> p.codigo().equals("constancia"))
                .allMatch(p -> !p.personalizada() && p.version() == 0);

        var personalizada = plantillaService.reemplazar(
                admin,
                "constancia",
                original.replace("TuSalud", "Clínica Ñandú").getBytes(StandardCharsets.UTF_8),
                "Cambio de marca");
        assertThat(personalizada.personalizada()).isTrue();
        assertThat(personalizada.version()).isEqualTo(1);
        assertThat(texto(directo(pacientePrincipal, TipoDocumento.CONSTANCIA, citaAtendida.getId())))
                .contains("Clínica Ñandú");

        var restaurada = plantillaService.restaurar(admin, "constancia", null);
        assertThat(restaurada.personalizada()).isFalse();
        assertThat(restaurada.version()).isEqualTo(2);
        assertThat(texto(directo(pacientePrincipal, TipoDocumento.CONSTANCIA, citaAtendida.getId())))
                .doesNotContain("Clínica Ñandú");
        assertThat(plantillaService.historial("constancia")).hasSize(2);
        assertThatThrownBy(() -> plantillaService.restaurar(admin, "constancia", null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rechazaPlantillasPeligrosasInvalidasOQueNoExisten() {
        String original = new String(plantillaService.descargar("receta").contenido(), StandardCharsets.UTF_8);
        byte[] conScriptlet = original.replace("name=\"receta\"", "name=\"receta\" scriptletClass=\"x.Y\"")
                .getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> plantillaService.reemplazar(admin, "receta", conScriptlet, null))
                .isInstanceOf(DatosInvalidosException.class);
        byte[] conCodigo = original.replace("$P{titulo}", "System.exit(1)").getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> plantillaService.reemplazar(admin, "receta", conCodigo, null))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() ->
                        plantillaService.reemplazar(admin, "receta", "<html/>".getBytes(StandardCharsets.UTF_8), null))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> plantillaService.descargar("../etc/passwd")).isInstanceOf(RuntimeException.class);
        assertThat(plantillaService.historial("receta")).isEmpty();
    }
}

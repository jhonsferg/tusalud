package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.response.ArchivoResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.RangoNoSatisfacibleException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ArchivoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService.Descarga;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.storage.PropositoArchivo;
import com.codesoftlabs.tusalud.app.storage.StorageService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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
class ArchivoServiceTest {

    private static final byte[] PNG = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Autowired
    private ArchivoService archivoService;

    @Autowired
    private ArchivoRepository archivoRepository;

    @Autowired
    private StorageService almacen;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal principalMedico;
    private UsuarioPrincipal principalPaciente;
    private UsuarioPrincipal principalOtroPaciente;
    private UsuarioPrincipal principalMedicoAjeno;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        fixture.cita(medico, paciente, LocalDateTime.now().minusDays(1), EstadoCita.ATENDIDA);
        principalMedico = UsuarioPrincipal.from(medico.getUsuario());
        principalPaciente = UsuarioPrincipal.from(paciente.getUsuario());
        principalOtroPaciente = UsuarioPrincipal.from(fixture.paciente().getUsuario());
        principalMedicoAjeno = UsuarioPrincipal.from(
                fixture.medicoConHorarioLunes(new BigDecimal("80.00")).getUsuario());
    }

    private UsuarioPrincipal personal(RolNombre rol) {
        return fixture.principal(fixture.usuario("arch", rol));
    }

    private static byte[] pdf(int longitud) {
        byte[] bytes = new byte[longitud];
        Arrays.fill(bytes, (byte) 'x');
        byte[] cabecera = "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(cabecera, 0, bytes, 0, Math.min(cabecera.length, longitud));
        return bytes;
    }

    private ArchivoResponse subirDocumento(byte[] contenido) {
        return archivoService.subir(
                principalMedico, PropositoArchivo.DOCUMENTO_PACIENTE, "informe.pdf", contenido, paciente.getId(), null);
    }

    private static String leer(Descarga descarga) throws IOException {
        try (var objeto = descarga.objeto()) {
            return new String(objeto.flujo().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void guardaElAvatarEnSuBucketConHuellaYTipoDetectado() {
        ArchivoResponse respuesta =
                archivoService.subir(principalPaciente, PropositoArchivo.AVATAR, "foto.PNG", PNG, null, null);

        assertThat(respuesta.tipoContenido()).isEqualTo("image/png");
        assertThat(respuesta.tamano()).isEqualTo(PNG.length);
        assertThat(respuesta.sha256()).hasSize(64);
        var archivo = archivoRepository.findActivo(respuesta.id()).orElseThrow();
        assertThat(archivo.getBucket()).isEqualTo("avatares");
        assertThat(almacen.existe(archivo.getBucket(), archivo.getClave())).isTrue();
        assertThat(archivo.getClave()).doesNotContain("foto");
    }

    @Test
    void rechazaEjecutablesYMarcadoAunqueSeDisfracenDeImagen() {
        byte[] exe = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0};
        assertThatThrownBy(() ->
                        archivoService.subir(principalPaciente, PropositoArchivo.AVATAR, "foto.png", exe, null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("no está permitido");
        byte[] svg = "<svg xmlns='http://www.w3.org/2000/svg'><script>alert(1)</script></svg>".getBytes();
        assertThatThrownBy(() ->
                        archivoService.subir(principalPaciente, PropositoArchivo.AVATAR, "foto.png", svg, null, null))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() ->
                        archivoService.subir(principalPaciente, PropositoArchivo.AVATAR, "foto.svg", svg, null, null))
                .isInstanceOf(DatosInvalidosException.class);
        byte[] html = "<html><body onload=alert(1)></body></html>".getBytes();
        assertThatThrownBy(() ->
                        archivoService.subir(principalPaciente, PropositoArchivo.AVATAR, "foto.html", html, null, null))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void exigeQueLaExtensionCoincidaConElContenido() {
        assertThatThrownBy(() ->
                        archivoService.subir(principalPaciente, PropositoArchivo.AVATAR, "foto.pdf", PNG, null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("no coincide con su extensión");
        assertThatThrownBy(() -> archivoService.subir(
                        principalPaciente, PropositoArchivo.AVATAR, "sinextension", PNG, null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("extensión");
    }

    @Test
    void aplicaLosTiposYLosTamanosPorPropositoYSanitizaElNombre() {
        assertThatThrownBy(() -> archivoService.subir(
                        principalPaciente, PropositoArchivo.AVATAR, "doc.pdf", pdf(100), null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("No se admiten");
        assertThatThrownBy(() -> archivoService.subir(
                        principalPaciente, PropositoArchivo.AVATAR, "vacio.png", new byte[0], null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("vacío");
        ArchivoResponse limpio = archivoService.subir(
                principalPaciente, PropositoArchivo.AVATAR, "..\\..\\etc/mi foto;rm.png", PNG, null, null);
        assertThat(limpio.nombre()).isEqualTo("mi foto_rm.png");
    }

    @Test
    void lasImagenesDiagnosticasYLosReportesNoSeSubenPorElEndpointGenerico() {
        assertThatThrownBy(() -> archivoService.subir(
                        principalMedico, PropositoArchivo.IMAGEN_DIAGNOSTICA, "x.png", PNG, null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("orden de imagen");
        assertThatThrownBy(() -> archivoService.subir(
                        personal(RolNombre.ROLE_ADMIN), PropositoArchivo.REPORTE, "r.pdf", pdf(20), null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("genera el sistema");
    }

    @Test
    void elMedicoConRelacionAdjuntaYElPacienteDuenoDescargaSuDocumento() throws IOException {
        ArchivoResponse documento = subirDocumento(pdf(50));

        assertThat(documento.referenciaTipo()).isEqualTo(ArchivoService.REFERENCIA_PACIENTE);
        assertThat(documento.referenciaId()).isEqualTo(paciente.getId());
        Descarga propia = archivoService.descargar(principalPaciente, documento.id(), null);
        assertThat(leer(propia)).startsWith("%PDF-1.4");
        assertThat(leer(archivoService.descargar(principalMedico, documento.id(), null)))
                .startsWith("%PDF");
    }

    @Test
    void nadieMasAccedeAlDocumentoDeUnPaciente() {
        ArchivoResponse documento = subirDocumento(pdf(50));

        assertThatThrownBy(() -> archivoService.descargar(principalOtroPaciente, documento.id(), null))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> archivoService.descargar(principalMedicoAjeno, documento.id(), null))
                .isInstanceOf(ForbiddenOperationException.class);
        for (RolNombre rol : List.of(
                RolNombre.ROLE_ADMIN,
                RolNombre.ROLE_RECEPCION,
                RolNombre.ROLE_CAJA,
                RolNombre.ROLE_FARMACIA,
                RolNombre.ROLE_GERENCIA)) {
            assertThatThrownBy(() -> archivoService.descargar(personal(rol), documento.id(), null))
                    .as(rol.name())
                    .isInstanceOf(ForbiddenOperationException.class);
        }
        assertThatThrownBy(() -> archivoService.metadatos(principalOtroPaciente, documento.id()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void soloElMedicoConRelacionAdjuntaDocumentos() {
        assertThatThrownBy(() -> archivoService.subir(
                        principalPaciente,
                        PropositoArchivo.DOCUMENTO_PACIENTE,
                        "propio.pdf",
                        pdf(30),
                        paciente.getId(),
                        null))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> archivoService.subir(
                        principalMedicoAjeno,
                        PropositoArchivo.DOCUMENTO_PACIENTE,
                        "ajeno.pdf",
                        pdf(30),
                        paciente.getId(),
                        null))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> archivoService.subir(
                        principalMedico, PropositoArchivo.DOCUMENTO_PACIENTE, "sin.pdf", pdf(30), null, null))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void lasLecturasDeDocumentosAjenosQuedanAuditadas() {
        ArchivoResponse documento = subirDocumento(pdf(50));

        archivoService.descargar(principalMedico, documento.id(), null);
        archivoService.descargar(principalPaciente, documento.id(), null);

        assertThat(archivoRepository.findActivosDeReferencia(ArchivoService.REFERENCIA_PACIENTE, paciente.getId()))
                .hasSize(1);
        assertThat(AuditoriaClinicaService.DOCUMENTO_CONSULTADO).isEqualTo("DOCUMENTO_CONSULTADO");
    }

    @Test
    void cualquierUsuarioVeLosAvataresPeroNoLosPuedeEliminarPorElEndpointGenerico() throws IOException {
        ArchivoResponse avatar =
                archivoService.subir(principalPaciente, PropositoArchivo.AVATAR, "foto.png", PNG, null, null);

        assertThat(archivoService
                        .descargar(principalMedicoAjeno, avatar.id(), null)
                        .longitud())
                .isEqualTo(PNG.length);
        assertThatThrownBy(() -> archivoService.eliminar(principalPaciente, avatar.id()))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void sirveRangosParcialesDeSufijoYAbiertosYRechazaLosInvalidos() throws IOException {
        byte[] contenido = pdf(100);
        ArchivoResponse documento = subirDocumento(contenido);

        Descarga inicio = archivoService.descargar(principalMedico, documento.id(), "bytes=0-8");
        assertThat(inicio.parcial()).isTrue();
        assertThat(inicio.desde()).isZero();
        assertThat(inicio.hasta()).isEqualTo(8);
        assertThat(leer(inicio)).isEqualTo("%PDF-1.4\n");

        Descarga sufijo = archivoService.descargar(principalMedico, documento.id(), "bytes=-5");
        assertThat(sufijo.desde()).isEqualTo(95);
        assertThat(leer(sufijo)).isEqualTo("xxxxx");

        Descarga abierto = archivoService.descargar(principalMedico, documento.id(), "bytes=90-");
        assertThat(abierto.hasta()).isEqualTo(99);
        assertThat(abierto.longitud()).isEqualTo(10);
        abierto.objeto().close();

        Descarga conExceso = archivoService.descargar(principalMedico, documento.id(), "bytes=95-500");
        assertThat(conExceso.hasta()).isEqualTo(99);
        conExceso.objeto().close();

        Descarga ignorado = archivoService.descargar(principalMedico, documento.id(), "palabras=1-2");
        assertThat(ignorado.parcial()).isFalse();
        assertThat(ignorado.longitud()).isEqualTo(100);
        ignorado.objeto().close();

        assertThatThrownBy(() -> archivoService.descargar(principalMedico, documento.id(), "bytes=500-600"))
                .isInstanceOf(RangoNoSatisfacibleException.class)
                .satisfies(ex -> assertThat(((RangoNoSatisfacibleException) ex).getTamano())
                        .isEqualTo(100));
        assertThatThrownBy(() -> archivoService.descargar(principalMedico, documento.id(), "bytes=-"))
                .isInstanceOf(RangoNoSatisfacibleException.class);
        assertThatThrownBy(() -> archivoService.descargar(principalMedico, documento.id(), "bytes=-0"))
                .isInstanceOf(RangoNoSatisfacibleException.class);
    }

    @Test
    void eliminarQuitaElRegistroYElObjetoDelAlmacen() {
        ArchivoResponse documento = subirDocumento(pdf(40));
        var archivo = archivoRepository.findActivo(documento.id()).orElseThrow();
        assertThat(almacen.existe(archivo.getBucket(), archivo.getClave())).isTrue();

        assertThatThrownBy(() -> archivoService.eliminar(principalOtroPaciente, documento.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> archivoService.eliminar(principalPaciente, documento.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        archivoService.eliminar(principalMedico, documento.id());

        assertThat(almacen.existe(archivo.getBucket(), archivo.getClave())).isFalse();
        assertThatThrownBy(() -> archivoService.descargar(principalMedico, documento.id(), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void laListaPorReferenciaSoloIncluyeLoQueElUsuarioPuedeVer() {
        subirDocumento(pdf(40));
        subirDocumento(pdf(60));

        assertThat(archivoService.listar(principalMedico, ArchivoService.REFERENCIA_PACIENTE, paciente.getId()))
                .hasSize(2);
        assertThat(archivoService.listar(principalPaciente, ArchivoService.REFERENCIA_PACIENTE, paciente.getId()))
                .hasSize(2);
        assertThat(archivoService.listar(principalOtroPaciente, ArchivoService.REFERENCIA_PACIENTE, paciente.getId()))
                .isEmpty();
        assertThat(archivoService.listar(
                        personal(RolNombre.ROLE_ADMIN), ArchivoService.REFERENCIA_PACIENTE, paciente.getId()))
                .isEmpty();
        assertThatThrownBy(() -> archivoService.listar(principalMedico, " ", null))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void lasPlantillasLasGestionanAdminYRrhhYLasLeenQuienGeneraReportes() {
        String jrxml =
                "<?xml version=\"1.0\"?><jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\""
                        + " name=\"ingresos\"><title/></jasperReport>";
        ArchivoResponse plantilla = archivoService.subir(
                personal(RolNombre.ROLE_RRHH),
                PropositoArchivo.PLANTILLA,
                "ingresos.jrxml",
                jrxml.getBytes(StandardCharsets.UTF_8),
                null,
                "PLANTILLA_REPORTE");

        assertThat(plantilla.tipoContenido()).isEqualTo("application/xml");
        assertThatThrownBy(() -> archivoService.subir(
                        principalMedico,
                        PropositoArchivo.PLANTILLA,
                        "x.jrxml",
                        jrxml.getBytes(StandardCharsets.UTF_8),
                        null,
                        null))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(archivoService.metadatos(personal(RolNombre.ROLE_FINANZAS), plantilla.id()))
                .isNotNull();
        assertThat(archivoService.metadatos(personal(RolNombre.ROLE_ADMIN), plantilla.id()))
                .isNotNull();
        assertThatThrownBy(() -> archivoService.metadatos(principalPaciente, plantilla.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> archivoService.metadatos(principalMedico, plantilla.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> archivoService.eliminar(principalMedico, plantilla.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        archivoService.eliminar(personal(RolNombre.ROLE_ADMIN), plantilla.id());
    }

    @Test
    void losReportesLosVeSuDuenoElAdminYLosRolesDeSuArea() {
        Usuario gerente = fixture.usuario("ger", RolNombre.ROLE_GERENCIA);
        Usuario finanzas = fixture.usuario("fin", RolNombre.ROLE_FINANZAS);
        var reporte = archivoService.almacenar(
                gerente, gerente, PropositoArchivo.REPORTE, "ingresos.pdf", pdf(60), "REPORTE_FINANCIERO", null);

        assertThat(archivoService.metadatos(fixture.principal(gerente), reporte.getId()))
                .isNotNull();
        assertThat(archivoService.metadatos(fixture.principal(finanzas), reporte.getId()))
                .isNotNull();
        assertThat(archivoService.metadatos(personal(RolNombre.ROLE_ADMIN), reporte.getId()))
                .isNotNull();
        for (RolNombre rol : List.of(RolNombre.ROLE_OPERACIONES, RolNombre.ROLE_CALIDAD, RolNombre.ROLE_MEDICO)) {
            assertThatThrownBy(() -> archivoService.metadatos(personal(rol), reporte.getId()))
                    .as(rol.name())
                    .isInstanceOf(ForbiddenOperationException.class);
        }
        assertThatThrownBy(() -> archivoService.metadatos(principalPaciente, reporte.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void siElObjetoFaltaEnElAlmacenResponde404() {
        ArchivoResponse documento = subirDocumento(pdf(40));
        var archivo = archivoRepository.findActivo(documento.id()).orElseThrow();
        almacen.eliminar(archivo.getBucket(), archivo.getClave());

        assertThatThrownBy(() -> archivoService.descargar(principalMedico, documento.id(), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unIdInexistenteResponde404() {
        assertThatThrownBy(() -> archivoService.descargar(principalMedico, UUID.randomUUID(), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

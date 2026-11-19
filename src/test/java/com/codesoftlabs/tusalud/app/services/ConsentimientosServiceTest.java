package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ConsentimientoPlantillaRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ConsentimientoSolicitudRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.FirmaRequest;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ConsentimientoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ConsentimientoPlantillaResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoConsentimiento;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.FirmanteTipo;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.ViaFirma;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.ConsentimientoService;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.VerificacionPublicaService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;
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
class ConsentimientosServiceTest {

    @Autowired
    private ConsentimientoService servicio;

    @Autowired
    private VerificacionPublicaService verificacion;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal principalMedico;
    private UsuarioPrincipal principalPaciente;
    private ConsentimientoPlantillaResponse plantilla;
    private ConsentimientoPlantillaResponse plantillaConTestigo;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new java.math.BigDecimal("100.00"));
        paciente = fixture.paciente();
        principalMedico = fixture.principal(medico.getUsuario());
        principalPaciente = fixture.principal(paciente.getUsuario());
        fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
        plantilla = servicio.crearPlantilla(plantilla("TEST_SIN", false));
        plantillaConTestigo = servicio.crearPlantilla(plantilla("TEST_CON", true));
    }

    private static ConsentimientoPlantillaRequest plantilla(String codigo, boolean testigo) {
        return new ConsentimientoPlantillaRequest(
                codigo,
                "Procedimiento de prueba " + codigo,
                "Descripción del procedimiento",
                "Riesgos conocidos",
                "Beneficios esperados",
                "Alternativas disponibles",
                testigo,
                30,
                true);
    }

    private static String firma(boolean conTrazos) {
        BufferedImage imagen = new BufferedImage(300, 100, BufferedImage.TYPE_INT_ARGB);
        if (conTrazos) {
            Graphics2D g = imagen.createGraphics();
            g.setColor(Color.BLACK);
            g.setStroke(new java.awt.BasicStroke(3f));
            g.drawLine(10, 80, 100, 20);
            g.drawLine(100, 20, 180, 85);
            g.drawLine(180, 85, 280, 30);
            g.dispose();
        }
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", salida);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private UUID solicitar(ConsentimientoPlantillaResponse p) {
        return servicio.solicitar(
                        principalMedico,
                        new ConsentimientoSolicitudRequest(
                                p.id(), paciente.getId(), null, null, null, "Sutura de herida"))
                .id();
    }

    private FirmaRequest firmaPaciente(String dni, String testigoNombre, String testigoDni) {
        return new FirmaRequest(
                firma(true),
                FirmanteTipo.PACIENTE,
                paciente.getUsuario().getNombreCompleto(),
                dni,
                null,
                testigoNombre,
                testigoDni,
                ViaFirma.APP_PACIENTE);
    }

    @Test
    void solicitaYElPacienteLoVeComoPendienteConElTextoVersionado() {
        UUID id = solicitar(plantilla);

        var pendiente = servicio.listarMios(principalPaciente);
        assertThat(pendiente).hasSize(1);
        assertThat(pendiente.get(0).estado()).isEqualTo(EstadoConsentimiento.PENDIENTE_FIRMA);
        ConsentimientoDetalle detalle = servicio.obtener(principalPaciente, id);
        assertThat(detalle.plantillaVersion()).isEqualTo(1);
        assertThat(detalle.textoAceptado()).contains("Riesgos conocidos").contains("Sutura de herida");
    }

    @Test
    void firmaConElDniDelPacienteGuardaLaFirmaYGeneraElPdf() {
        UUID id = solicitar(plantilla);

        ConsentimientoDetalle firmado =
                servicio.firmar(principalPaciente, id, firmaPaciente(paciente.getDni(), null, null));

        assertThat(firmado.resumen().estado()).isEqualTo(EstadoConsentimiento.FIRMADO);
        assertThat(firmado.resumen().firmadoVia()).isEqualTo(ViaFirma.APP_PACIENTE);
        assertThat(firmado.codigoVerificacion()).matches("^[A-Z2-9]{4}(-[A-Z2-9]{4}){3}$");
        assertThat(firmado.resumen().venceEn()).isAfter(LocalDateTime.now().plusDays(29));
        assertThat(servicio.pdf(principalPaciente, id).contenido()).startsWith("%PDF".getBytes());
        assertThat(verificacion
                        .verificar("10.1.1.1", firmado.codigoVerificacion())
                        .resultado())
                .isEqualTo("VALIDO");
        assertThatThrownBy(() -> servicio.firmar(principalPaciente, id, firmaPaciente(paciente.getDni(), null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rechazaDniAjenoFirmaVaciaYTestigoFaltante() {
        UUID id = solicitar(plantilla);
        assertThatThrownBy(() -> servicio.firmar(principalPaciente, id, firmaPaciente("99999999", null, null)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("DNI");

        FirmaRequest vacia = new FirmaRequest(
                firma(false),
                FirmanteTipo.PACIENTE,
                "Paciente",
                paciente.getDni(),
                null,
                null,
                null,
                ViaFirma.APP_PACIENTE);
        assertThatThrownBy(() -> servicio.firmar(principalPaciente, id, vacia))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("vacía");

        UUID conTestigo = solicitar(plantillaConTestigo);
        assertThatThrownBy(() ->
                        servicio.firmar(principalPaciente, conTestigo, firmaPaciente(paciente.getDni(), null, null)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("testigo");
        assertThat(servicio.firmar(
                                principalPaciente,
                                conTestigo,
                                firmaPaciente(paciente.getDni(), "Rosa Medina", "45678901"))
                        .testigoNombre())
                .isEqualTo("Rosa Medina");
    }

    @Test
    void elRepresentanteLegalFirmaConSuParentesco() {
        UUID id = solicitar(plantilla);
        FirmaRequest representante = new FirmaRequest(
                firma(true),
                FirmanteTipo.REPRESENTANTE,
                "Carmen Soto",
                "40123456",
                "Madre",
                null,
                null,
                ViaFirma.APP_PACIENTE);
        ConsentimientoDetalle firmado = servicio.firmar(principalPaciente, id, representante);
        assertThat(firmado.firmanteTipo()).isEqualTo(FirmanteTipo.REPRESENTANTE);
        assertThat(firmado.firmanteParentesco()).isEqualTo("Madre");

        UUID otro = solicitar(plantilla);
        FirmaRequest sinParentesco = new FirmaRequest(
                firma(true),
                FirmanteTipo.REPRESENTANTE,
                "Carmen Soto",
                "40123456",
                null,
                null,
                null,
                ViaFirma.APP_PACIENTE);
        assertThatThrownBy(() -> servicio.firmar(principalPaciente, otro, sinParentesco))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("parentesco");
    }

    @Test
    void elMedicoFirmaEnElConsultorioPeroNoPorLaViaDelPaciente() {
        UUID id = solicitar(plantilla);
        FirmaRequest viaPaciente = new FirmaRequest(
                firma(true),
                FirmanteTipo.PACIENTE,
                "Paciente",
                paciente.getDni(),
                null,
                null,
                null,
                ViaFirma.APP_PACIENTE);
        assertThatThrownBy(() -> servicio.firmar(principalMedico, id, viaPaciente))
                .isInstanceOf(ForbiddenOperationException.class);
        FirmaRequest consultorio = new FirmaRequest(
                firma(true),
                FirmanteTipo.PACIENTE,
                "Paciente",
                paciente.getDni(),
                null,
                null,
                null,
                ViaFirma.CONSULTORIO);
        assertThat(servicio.firmar(principalMedico, id, consultorio).resumen().firmadoVia())
                .isEqualTo(ViaFirma.CONSULTORIO);
        UsuarioPrincipal ajeno = fixture.principal(fixture.paciente().getUsuario());
        UUID otro = solicitar(plantilla);
        assertThatThrownBy(() -> servicio.firmar(ajeno, otro, viaPaciente))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void revocaConMotivoYLaVerificacionLoMuestraComoAnulado() {
        UUID id = solicitar(plantilla);
        ConsentimientoDetalle firmado =
                servicio.firmar(principalPaciente, id, firmaPaciente(paciente.getDni(), null, null));
        ConsentimientoDetalle revocado = servicio.revocar(principalPaciente, id, "Cambié de opinión");

        assertThat(revocado.resumen().estado()).isEqualTo(EstadoConsentimiento.REVOCADO);
        assertThat(revocado.motivoRevocacion()).isEqualTo("Cambié de opinión");
        assertThat(verificacion
                        .verificar("10.2.2.2", firmado.codigoVerificacion())
                        .resultado())
                .isEqualTo("ANULADO");
        assertThat(servicio.pdf(principalPaciente, id).contenido()).startsWith("%PDF".getBytes());
        assertThatThrownBy(() -> servicio.revocar(principalPaciente, id, "Otra vez"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void actualizarElTextoDeLaPlantillaSubeLaVersionSinTocarLoYaAceptado() {
        UUID id = solicitar(plantilla);
        servicio.actualizarPlantilla(
                plantilla.id(),
                new ConsentimientoPlantillaRequest(
                        "TEST_SIN",
                        "Nuevo nombre",
                        "Texto cambiado",
                        "Riesgos conocidos",
                        "Beneficios",
                        "Alternativas",
                        false,
                        30,
                        true));

        assertThat(servicio.plantillas(fixture.principal(fixture.usuario("adm", RolNombre.ROLE_ADMIN))))
                .anySatisfy(p -> {
                    if (p.codigo().equals("TEST_SIN")) {
                        assertThat(p.version()).isEqualTo(2);
                    }
                });
        ConsentimientoDetalle anterior = servicio.obtener(principalPaciente, id);
        assertThat(anterior.plantillaVersion()).isEqualTo(1);
        assertThat(anterior.textoAceptado())
                .contains("Descripción del procedimiento")
                .doesNotContain("Texto cambiado");
    }

    @Test
    void soloElMedicoConRelacionYElTitularLeen() {
        UUID id = solicitar(plantilla);
        Medico sinRelacion = fixture.medicoConHorarioLunes(new java.math.BigDecimal("80.00"));
        UsuarioPrincipal principalSin = fixture.principal(sinRelacion.getUsuario());
        assertThatThrownBy(() -> servicio.obtener(principalSin, id)).isInstanceOf(ForbiddenOperationException.class);
        UsuarioPrincipal recepcion = fixture.principal(fixture.usuario("rec", RolNombre.ROLE_RECEPCION));
        assertThatThrownBy(() -> servicio.obtener(recepcion, id)).isInstanceOf(ForbiddenOperationException.class);
        assertThat(servicio.obtener(principalMedico, id).resumen().id()).isEqualTo(id);
    }
}

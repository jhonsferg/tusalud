package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.request.HceRequests.AdendaRequest;
import com.codesoftlabs.tusalud.app.dto.request.HceRequests.CorreccionRequest;
import com.codesoftlabs.tusalud.app.dto.request.HceRequests.FusionPacientesRequest;
import com.codesoftlabs.tusalud.app.dto.request.HceRequests.PoliticaConservacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.CierreResponse;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.HistorialAtencionResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.HceEnums.CampoCorregible;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.VerificacionPublicaService;
import com.codesoftlabs.tusalud.app.services.hce.HceConservacionService;
import com.codesoftlabs.tusalud.app.services.hce.HceFusionService;
import com.codesoftlabs.tusalud.app.services.hce.HceImpresionService;
import com.codesoftlabs.tusalud.app.services.hce.HceService;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class HceServiceTest {

    @Autowired
    private HceService hce;

    @Autowired
    private HceConservacionService conservacion;

    @Autowired
    private com.codesoftlabs.tusalud.app.repositories.PoliticaConservacionRepository politicasRepositorio;

    @Autowired
    private HceFusionService fusion;

    @Autowired
    private HceImpresionService impresion;

    @Autowired
    private VerificacionPublicaService verificacion;

    @Autowired
    private AtencionService atencionService;

    @Autowired
    private AtencionRepository atencionRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private Atencion atencion;
    private UsuarioPrincipal principalMedico;
    private UsuarioPrincipal principalPaciente;
    private UsuarioPrincipal principalAdmin;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        var cita = fixture.cita(medico, paciente, LocalDateTime.now().minusHours(1), EstadoCita.ATENDIDA);
        atencion = atencionRepository.save(Atencion.builder()
                .cita(cita)
                .paciente(paciente)
                .medico(medico)
                .fecha(LocalDateTime.now())
                .diagnostico("Faringitis aguda")
                .tratamiento("Reposo")
                .build());
        principalMedico = fixture.principal(medico.getUsuario());
        principalPaciente = fixture.principal(paciente.getUsuario());
        principalAdmin = fixture.principal(fixture.usuario("adm", RolNombre.ROLE_ADMIN));
    }

    @Test
    void cerrarFirmaLaAtencionYSePuedeVerificar() {
        CierreResponse cierre = hce.cerrar(principalMedico, atencion.getId());

        assertThat(cierre.cerrada()).isTrue();
        assertThat(cierre.firmaValida()).isTrue();
        assertThat(cierre.hashCorto()).hasSize(16);
        assertThat(hce.estado(principalPaciente, atencion.getId()).cerrada()).isTrue();
    }

    @Test
    void noSeCierraDosVecesNiPorOtroMedico() {
        hce.cerrar(principalMedico, atencion.getId());
        Medico otro = fixture.medicoConHorarioLunes(new BigDecimal("90.00"));

        assertThatThrownBy(() -> hce.cerrar(principalMedico, atencion.getId())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> hce.cerrar(fixture.principal(otro.getUsuario()), atencion.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void unaAtencionCerradaNoSePuedeEditarPeroSiRectificarConRastro() {
        hce.cerrar(principalMedico, atencion.getId());

        assertThatThrownBy(() -> atencionService.actualizar(
                        atencion.getCita().getId(),
                        new AtencionRequest("Otro", "Otro", null, null, null, null, null, null, null, null, null),
                        principalMedico))
                .isInstanceOf(ConflictException.class);

        hce.corregir(
                principalMedico,
                atencion.getId(),
                new CorreccionRequest(CampoCorregible.DIAGNOSTICO, "Amigdalitis aguda", "Error de digitación"));
        hce.adenda(principalMedico, atencion.getId(), new AdendaRequest("Paciente refiere mejoría", "Seguimiento"));

        HistorialAtencionResponse historial = hce.historial(principalPaciente, atencion.getId());
        assertThat(historial.original().diagnostico()).isEqualTo("Faringitis aguda");
        assertThat(historial.vigente().diagnostico()).isEqualTo("Amigdalitis aguda");
        assertThat(historial.versiones()).hasSize(2);
        assertThat(historial.versiones().get(0).valorAnterior()).isEqualTo("Faringitis aguda");
        assertThat(historial.versiones().get(0).numero()).isEqualTo(1);
        assertThat(historial.versiones().get(1).numero()).isEqualTo(2);
        assertThat(historial.versiones().get(1).tipo()).isEqualTo("ADENDA");
    }

    @Test
    void laCorreccionExigeCierreCambioRealYMotivo() {
        assertThatThrownBy(() -> hce.adenda(principalMedico, atencion.getId(), new AdendaRequest("Texto", "Motivo")))
                .isInstanceOf(BusinessLogicException.class);
        hce.cerrar(principalMedico, atencion.getId());

        assertThatThrownBy(() -> hce.corregir(
                        principalMedico,
                        atencion.getId(),
                        new CorreccionRequest(CampoCorregible.TRATAMIENTO, "Reposo", "Sin cambio")))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void unaCorreccionSobreOtraConservaLaCadenaDeValores() {
        hce.cerrar(principalMedico, atencion.getId());
        hce.corregir(
                principalMedico,
                atencion.getId(),
                new CorreccionRequest(CampoCorregible.TRATAMIENTO, "Reposo e hidratación", "Aclaración"));
        hce.corregir(
                principalMedico,
                atencion.getId(),
                new CorreccionRequest(CampoCorregible.TRATAMIENTO, "Reposo, hidratación y analgésico", "Se añadió"));

        HistorialAtencionResponse historial = hce.historial(principalMedico, atencion.getId());
        assertThat(historial.versiones().get(1).valorAnterior()).isEqualTo("Reposo e hidratación");
        assertThat(historial.vigente().tratamiento()).isEqualTo("Reposo, hidratación y analgésico");
        assertThat(historial.original().tratamiento()).isEqualTo("Reposo");
    }

    @Test
    void otroPacienteNoVeLaHistoria() {
        hce.cerrar(principalMedico, atencion.getId());
        UsuarioPrincipal ajeno = fixture.principal(fixture.paciente().getUsuario());

        assertThatThrownBy(() -> hce.historial(ajeno, atencion.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void laConservacionNuncaDesbloqueaLaDepuracion() {
        politicasRepositorio.save(com.codesoftlabs.tusalud.app.entities.PoliticaConservacion.builder()
                .tipoRegistro("HISTORIA_CLINICA")
                .descripcion("Historia clínica")
                .aniosMinimos(20)
                .porConfirmar(true)
                .depuracionBloqueada(false)
                .build());
        var politicas = conservacion.listar();
        assertThat(politicas).isNotEmpty();

        var actualizada = conservacion.actualizar(
                principalAdmin, "HISTORIA_CLINICA", new PoliticaConservacionRequest(25, "Validado", false));

        assertThat(actualizada.aniosMinimos()).isEqualTo(25);
        assertThat(actualizada.porConfirmar()).isFalse();
        assertThat(actualizada.depuracionBloqueada()).isTrue();
    }

    @Test
    void laFusionMueveLosRegistrosYDejaConstancia() {
        Paciente origen = fixture.paciente();
        var citaOrigen = fixture.cita(medico, origen, LocalDateTime.now().plusDays(3), EstadoCita.CONFIRMADA);
        entityManager.flush();
        UUID citaId = citaOrigen.getId();

        var respuesta = fusion.fusionar(
                principalAdmin, new FusionPacientesRequest(origen.getId(), paciente.getId(), "Registro duplicado"));
        entityManager.clear();

        assertThat(respuesta.resumen()).contains("citas");
        assertThat(citaRepository.findById(citaId).orElseThrow().getPaciente().getId())
                .isEqualTo(paciente.getId());
        assertThat(fusion.historial()).hasSize(1);
    }

    @Test
    void laFusionRechazaElMismoPaciente() {
        assertThatThrownBy(() -> fusion.fusionar(
                        principalAdmin, new FusionPacientesRequest(paciente.getId(), paciente.getId(), "Mismo")))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void laImpresionGeneraPdfConCodigoVerificable() {
        hce.cerrar(principalMedico, atencion.getId());

        byte[] pdf = impresion.imprimir(principalPaciente, paciente.getId());

        assertThat(pdf).startsWith("%PDF".getBytes());
        entityManager.flush();
        List<?> codigos = entityManager
                .createQuery("SELECT i.codigoVerificacion FROM HceImpresion i WHERE i.paciente.id = :p")
                .setParameter("p", paciente.getId())
                .getResultList();
        assertThat(codigos).hasSize(1);
        assertThat(verificacion.verificar("10.0.0.1", codigos.get(0).toString()).resultado())
                .isEqualTo("VALIDO");
    }
}

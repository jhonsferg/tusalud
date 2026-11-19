package com.codesoftlabs.tusalud.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEventoSeguridad;
import com.codesoftlabs.tusalud.app.repositories.EventoSeguridadRepository;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class Cie10SeguridadTest {

    private static final String ATENCION_JSON =
            "{\"diagnosticos\":[{\"codigo\":\"I10\",\"tipo\":\"DEFINITIVO\",\"principal\":true},"
                    + "{\"codigo\":\"E11.9\",\"tipo\":\"PRESUNTIVO\",\"principal\":false}]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private EventoSeguridadRepository eventoRepository;

    private Medico medico;
    private Paciente paciente;
    private Cita cita;
    private String tokenMedico;
    private String tokenPaciente;
    private String tokenAdmin;
    private String tokenCalidad;
    private String tokenGerencia;
    private String tokenOperaciones;
    private String tokenFinanzas;
    private String tokenCaja;
    private String tokenRecepcion;
    private String tokenFarmacia;
    private String tokenEnfermeria;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        cita = fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
        tokenMedico = token(UsuarioPrincipal.from(medico.getUsuario()));
        tokenPaciente = token(UsuarioPrincipal.from(paciente.getUsuario()));
        tokenAdmin = token(UsuarioPrincipal.from(fixture.usuario("admin", RolNombre.ROLE_ADMIN)));
        tokenCalidad = token(UsuarioPrincipal.from(fixture.usuario("calidad", RolNombre.ROLE_CALIDAD)));
        tokenGerencia = token(UsuarioPrincipal.from(fixture.usuario("gerencia", RolNombre.ROLE_GERENCIA)));
        tokenOperaciones = token(UsuarioPrincipal.from(fixture.usuario("oper", RolNombre.ROLE_OPERACIONES)));
        tokenFinanzas = token(UsuarioPrincipal.from(fixture.usuario("fin", RolNombre.ROLE_FINANZAS)));
        tokenCaja = token(UsuarioPrincipal.from(fixture.usuario("caja", RolNombre.ROLE_CAJA)));
        tokenRecepcion = token(UsuarioPrincipal.from(fixture.usuario("recep", RolNombre.ROLE_RECEPCION)));
        tokenFarmacia = token(UsuarioPrincipal.from(fixture.usuario("farm", RolNombre.ROLE_FARMACIA)));
        tokenEnfermeria = token(UsuarioPrincipal.from(fixture.usuario("enf", RolNombre.ROLE_ENFERMERIA)));
    }

    private String token(UsuarioPrincipal principal) {
        return "Bearer " + jwtUtils.generateToken(principal);
    }

    private static MockMultipartFile csv(String contenido) {
        return new MockMultipartFile("archivo", "cie10.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void laBusquedaDeDiagnosticosEsSoloParaMedicosYRolesDeAnalisis() throws Exception {
        for (String token : new String[] {tokenMedico, tokenAdmin, tokenCalidad, tokenGerencia, tokenOperaciones}) {
            mockMvc.perform(get("/api/v1/cie10").param("q", "hipertension").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].codigo", hasItem("I10")));
        }
        for (String token :
                new String[] {tokenPaciente, tokenCaja, tokenRecepcion, tokenFarmacia, tokenEnfermeria, tokenFinanzas
                }) {
            mockMvc.perform(get("/api/v1/cie10").param("q", "hipertension").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/cie10/I10").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/cie10").param("q", "hipertension")).andExpect(status().isUnauthorized());
    }

    @Test
    void soloElMedicoVeSusDiagnosticosFrecuentes() throws Exception {
        mockMvc.perform(get("/api/v1/cie10/mis-frecuentes").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk());
        for (String token : new String[] {tokenAdmin, tokenPaciente, tokenCalidad, tokenRecepcion}) {
            mockMvc.perform(get("/api/v1/cie10/mis-frecuentes").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void laAdministracionDelCatalogoEsSoloDelAdministrador() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cie10/resumen").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActivos").isNumber());
        mockMvc.perform(get("/api/v1/admin/cie10/plantilla.csv").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk());
        for (String token : new String[] {tokenMedico, tokenPaciente, tokenCalidad, tokenGerencia, tokenRecepcion}) {
            mockMvc.perform(get("/api/v1/admin/cie10/resumen").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(multipart("/api/v1/admin/cie10/importaciones/vista-previa")
                            .file(csv("codigo,descripcion\nZ99.9,Prueba\n"))
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(multipart("/api/v1/admin/cie10/importaciones")
                            .file(csv("codigo,descripcion\nZ99.9,Prueba\n"))
                            .param("etiqueta", "Intruso")
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void elAdministradorPrevisualizaEImportaYQuedaRegistradoComoEventoDeSeguridad() throws Exception {
        long antes = eventoRepository.count();
        mockMvc.perform(multipart("/api/v1/admin/cie10/importaciones/vista-previa")
                        .file(csv("codigo,descripcion\nZ99.9,Prueba\nMAL,Inválido\n"))
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicada", is(false)))
                .andExpect(jsonPath("$.altas", is(1)))
                .andExpect(jsonPath("$.totalErrores", is(1)));
        assertThat(eventoRepository.count()).isEqualTo(antes);

        mockMvc.perform(multipart("/api/v1/admin/cie10/importaciones")
                        .file(csv("codigo,descripcion\nZ99.9,Prueba\n"))
                        .param("etiqueta", "Importación de prueba")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicada", is(true)))
                .andExpect(jsonPath("$.altas", is(1)));
        assertThat(eventoRepository.findAll())
                .anyMatch(e -> e.getTipo() == TipoEventoSeguridad.CATALOGO_CIE10_IMPORTADO);

        mockMvc.perform(multipart("/api/v1/admin/cie10/importaciones")
                        .file(csv("codigo,descripcion\nZ99.9,Prueba\n"))
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elReporteDeMorbilidadSoloLoVenCalidadOperacionesGerenciaYAdministracion() throws Exception {
        for (String token : new String[] {tokenCalidad, tokenOperaciones, tokenGerencia, tokenAdmin}) {
            mockMvc.perform(get("/api/v1/reportes/morbilidad").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.umbralSupresion", is(5)));
        }
        for (String token : new String[] {
            tokenMedico, tokenPaciente, tokenFinanzas, tokenCaja, tokenRecepcion, tokenFarmacia, tokenEnfermeria
        }) {
            mockMvc.perform(get("/api/v1/reportes/morbilidad").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/reportes/morbilidad")
                        .param("desde", "2020-01-01")
                        .param("hasta", "2026-01-01")
                        .header(HttpHeaders.AUTHORIZATION, tokenCalidad))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lasAtencionesRequierenDiagnosticosCodificadosPorHttp() throws Exception {
        mockMvc.perform(post("/api/v1/citas/" + cita.getId() + "/atencion")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnostico\":\"Solo texto libre\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post("/api/v1/citas/" + cita.getId() + "/atencion")
                                .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"diagnosticos\":[{\"codigo\":\"no es un codigo\",\"tipo\":\"DEFINITIVO\",\"principal\":true}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/citas/" + cita.getId() + "/atencion")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ATENCION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diagnosticos[0].codigo", is("I10")))
                .andExpect(jsonPath("$.diagnosticos[0].principal", is(true)))
                .andExpect(jsonPath("$.diagnostico", is("Hipertensión esencial (primaria)")));
    }

    @Test
    void soloElMedicoYElPacienteDeLaCitaVenLosDiagnosticos() throws Exception {
        mockMvc.perform(post("/api/v1/citas/" + cita.getId() + "/atencion")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ATENCION_JSON))
                .andExpect(status().isCreated());
        for (String token : new String[] {tokenMedico, tokenPaciente}) {
            mockMvc.perform(get("/api/v1/citas/" + cita.getId() + "/atencion").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.diagnosticos.length()", is(2)));
        }
        for (String token :
                new String[] {tokenAdmin, tokenCaja, tokenRecepcion, tokenFarmacia, tokenEnfermeria, tokenCalidad}) {
            mockMvc.perform(get("/api/v1/citas/" + cita.getId() + "/atencion").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/historia-clinica").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atenciones[0].diagnosticos[0].codigo", is("I10")));
    }
}

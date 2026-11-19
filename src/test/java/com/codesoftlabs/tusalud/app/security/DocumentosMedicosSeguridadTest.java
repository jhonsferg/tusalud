package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class DocumentosMedicosSeguridadTest {

    private static final String CERTIFICADO =
            "{\"pacienteId\":\"%s\",\"tipo\":\"CERTIFICADO\",\"autorizaDiagnostico\":false,"
                    + "\"contenido\":{\"motivo\":\"Control\",\"vigenciaDias\":2}}";

    private static final String PLANTILLA =
            "{\"codigo\":\"SEG_TEST\",\"nombre\":\"Prueba\",\"texto\":\"t\",\"riesgos\":\"r\","
                    + "\"beneficios\":\"b\",\"alternativas\":\"a\",\"requiereTestigo\":false,\"vigenciaDias\":10,\"activa\":true}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medicoEntidad;
    private Paciente pacienteEntidad;
    private String medico;
    private String medicoSinRelacion;
    private String paciente;
    private String otroPaciente;
    private String admin;
    private String recepcion;
    private String caja;
    private String enfermeria;
    private String rrhh;

    @BeforeEach
    void setUp() {
        medicoEntidad = fixture.medicoConHorarioLunes(new java.math.BigDecimal("100.00"));
        pacienteEntidad = fixture.paciente();
        fixture.cita(medicoEntidad, pacienteEntidad, LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
        medico = token(medicoEntidad.getUsuario());
        medicoSinRelacion = token(
                fixture.medicoConHorarioLunes(new java.math.BigDecimal("90.00")).getUsuario());
        paciente = token(pacienteEntidad.getUsuario());
        otroPaciente = token(fixture.paciente().getUsuario());
        admin = token(fixture.usuario("dm-adm", RolNombre.ROLE_ADMIN));
        recepcion = token(fixture.usuario("dm-rec", RolNombre.ROLE_RECEPCION));
        caja = token(fixture.usuario("dm-caj", RolNombre.ROLE_CAJA));
        enfermeria = token(fixture.usuario("dm-enf", RolNombre.ROLE_ENFERMERIA));
        rrhh = token(fixture.usuario("dm-rrh", RolNombre.ROLE_RRHH));
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    private String crearBorradorComoMedico() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/documentos-medicos")
                        .header("Authorization", medico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CERTIFICADO.formatted(pacienteEntidad.getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(cuerpo, "$.resumen.id");
    }

    @Test
    void sinTokenLosEndpointsPrivadosRetornan401() throws Exception {
        mockMvc.perform(get("/api/v1/documentos-medicos/mios")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/consentimientos/mios")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/establecimientos-salud")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/documentos-medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laVerificacionEsPublicaYNoDistingueCodigosInexistentes() throws Exception {
        mockMvc.perform(get("/api/v1/verificacion/AAAA-BBBB-CCCC-DDDD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("NO_ENCONTRADO")))
                .andExpect(jsonPath("$.tipo", nullValue()))
                .andExpect(jsonPath("$.medico", nullValue()));
        mockMvc.perform(get("/api/v1/verificacion/basura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("NO_ENCONTRADO")));
    }

    @Test
    void soloElMedicoCreaDocumentosYElPacienteNo() throws Exception {
        String cuerpo = CERTIFICADO.formatted(pacienteEntidad.getId());
        for (String token : new String[] {paciente, admin, recepcion, caja, enfermeria, rrhh}) {
            mockMvc.perform(post("/api/v1/documentos-medicos")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/v1/documentos-medicos")
                        .header("Authorization", medicoSinRelacion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    void administracionRecepcionCajaYRrhhNoLeenContenidoClinico() throws Exception {
        String id = crearBorradorComoMedico();
        mockMvc.perform(post("/api/v1/documentos-medicos/" + id + "/emitir").header("Authorization", medico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.estado", is("EMITIDO")));
        for (String token : new String[] {admin, recepcion, caja, enfermeria, rrhh}) {
            mockMvc.perform(get("/api/v1/documentos-medicos/" + id).header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/documentos-medicos/" + id + "/pdf").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/documentos-medicos/" + id).header("Authorization", medicoSinRelacion))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/documentos-medicos/" + id).header("Authorization", otroPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/documentos-medicos/" + id).header("Authorization", paciente))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/documentos-medicos/" + id + "/pdf").header("Authorization", paciente))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Type", "application/pdf"));
    }

    @Test
    void elPacienteNoVeBorradoresNiPuedeEmitirNiAnular() throws Exception {
        String id = crearBorradorComoMedico();
        mockMvc.perform(get("/api/v1/documentos-medicos/" + id).header("Authorization", paciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/documentos-medicos/" + id + "/emitir").header("Authorization", paciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/documentos-medicos/" + id + "/anular")
                        .header("Authorization", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"No me corresponde\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unIdQueNoEsUuidRespondeBadRequestYUnoInexistenteNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/documentos-medicos/no-es-uuid").header("Authorization", medico))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/documentos-medicos/" + UUID.randomUUID()).header("Authorization", medico))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloElAdministradorGestionaPlantillasDeConsentimientoYEstablecimientos() throws Exception {
        for (String token : new String[] {medico, paciente, recepcion}) {
            mockMvc.perform(post("/api/v1/consentimientos/plantillas")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PLANTILLA))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/establecimientos-salud")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"X\",\"nivel\":\"PRIMER_NIVEL\",\"activo\":true}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/v1/consentimientos/plantillas")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTILLA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version", is(1)));
        mockMvc.perform(post("/api/v1/establecimientos-salud")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Posta Norte\",\"nivel\":\"PRIMER_NIVEL\",\"activo\":true}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/consentimientos/plantillas").header("Authorization", paciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/consentimientos/plantillas").header("Authorization", medico))
                .andExpect(status().isOk());
    }

    @Test
    void laFirmaDeUnConsentimientoAjenoEsRechazada() throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/consentimientos/plantillas")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTILLA))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String plantillaId = com.jayway.jsonpath.JsonPath.read(respuesta, "$.id");
        String solicitud = mockMvc.perform(post("/api/v1/consentimientos")
                        .header("Authorization", medico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantillaId\":\"%s\",\"pacienteId\":\"%s\",\"procedimiento\":\"Prueba\"}"
                                .formatted(plantillaId, pacienteEntidad.getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(solicitud, "$.id");
        String firma = "{\"firmaPng\":\"AAAA\",\"firmanteTipo\":\"PACIENTE\",\"nombre\":\"X\","
                + "\"dni\":\"12345678\",\"via\":\"APP_PACIENTE\"}";
        mockMvc.perform(post("/api/v1/consentimientos/" + id + "/firma")
                        .header("Authorization", otroPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firma))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/consentimientos/" + id).header("Authorization", otroPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/consentimientos/plantillas/" + plantillaId)
                        .header("Authorization", medico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTILLA))
                .andExpect(status().isForbidden());
    }
}

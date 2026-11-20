package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.Cie10Pruebas;
import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaItemRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class DocumentosSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private AtencionService atencionService;

    private Medico medico;
    private Paciente paciente;
    private AtencionResponse atencion;
    private String tokenMedico;
    private String tokenMedicoAjeno;
    private String tokenPaciente;
    private String tokenOtroPaciente;
    private String tokenAdmin;
    private String tokenGerencia;
    private String tokenFinanzas;
    private String tokenCaja;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        Cita cita =
                fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2).withNano(0), EstadoCita.CONFIRMADA);
        atencion = atencionService.registrar(
                cita.getId(),
                new AtencionRequest(
                        "Hipertensión leve",
                        "Dieta",
                        "Control",
                        null,
                        null,
                        null,
                        "120/80",
                        null,
                        72,
                        List.of(new RecetaItemRequest("Losartán 50 mg", "1 tableta", "Cada 24 horas", "30 días")),
                        Cie10Pruebas.principal()),
                UsuarioPrincipal.from(medico.getUsuario()));
        tokenMedico = token(UsuarioPrincipal.from(medico.getUsuario()));
        tokenMedicoAjeno = token(UsuarioPrincipal.from(
                fixture.medicoConHorarioLunes(new BigDecimal("80.00")).getUsuario()));
        tokenPaciente = token(UsuarioPrincipal.from(paciente.getUsuario()));
        tokenOtroPaciente = token(UsuarioPrincipal.from(fixture.paciente().getUsuario()));
        tokenAdmin = token(UsuarioPrincipal.from(fixture.usuario("adm", RolNombre.ROLE_ADMIN)));
        tokenGerencia = token(UsuarioPrincipal.from(fixture.usuario("ger", RolNombre.ROLE_GERENCIA)));
        tokenFinanzas = token(UsuarioPrincipal.from(fixture.usuario("fin", RolNombre.ROLE_FINANZAS)));
        tokenCaja = token(UsuarioPrincipal.from(fixture.usuario("caj", RolNombre.ROLE_CAJA)));
    }

    private String token(UsuarioPrincipal principal) {
        return "Bearer " + jwtUtils.generateToken(principal);
    }

    private String exportacion(String token, String json) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/exportaciones")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted())
                .andReturn();
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void sinTokenNoSeAccedeANada() throws Exception {
        mockMvc.perform(get("/api/v1/documentos/receta/" + atencion.id())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/exportaciones")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/exportaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"reporte-ejecutivo\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/plantillas")).andExpect(status().isUnauthorized());
    }

    @Test
    void laRecetaEnPdfSeSirveSinCacheYSoloAQuienCorresponde() throws Exception {
        mockMvc.perform(get("/api/v1/documentos/receta/" + atencion.id())
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string("Content-Security-Policy", containsString("sandbox")));
        mockMvc.perform(get("/api/v1/documentos/receta/" + atencion.id())
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/documentos/receta/" + atencion.id())
                        .header(HttpHeaders.AUTHORIZATION, tokenOtroPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/documentos/receta/" + atencion.id())
                        .header(HttpHeaders.AUTHORIZATION, tokenMedicoAjeno))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/documentos/receta/" + atencion.id()).header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/documentos/receta/" + atencion.id())
                        .header(HttpHeaders.AUTHORIZATION, tokenGerencia))
                .andExpect(status().isForbidden());
    }

    @Test
    void validaElTipoYElIdentificadorDelDocumento() throws Exception {
        mockMvc.perform(get("/api/v1/documentos/inexistente/" + atencion.id())
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/documentos/reporte-ejecutivo/" + atencion.id())
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/documentos/receta/no-es-un-uuid").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/documentos/receta/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isNotFound());
    }

    @Test
    void unaExportacionSeSolicitaConAceptadoYSoloLaDescargaElSolicitante() throws Exception {
        String id = exportacion(tokenGerencia, "{\"tipo\":\"reporte-ejecutivo\",\"formato\":\"XLSX\"}");

        mockMvc.perform(get("/api/v1/exportaciones/" + id).header(HttpHeaders.AUTHORIZATION, tokenGerencia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("LISTA")))
                .andExpect(jsonPath("$.formato", is("XLSX")));
        mockMvc.perform(get("/api/v1/exportaciones/" + id + "/descargar")
                        .header(HttpHeaders.AUTHORIZATION, tokenGerencia))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mockMvc.perform(get("/api/v1/exportaciones").header(HttpHeaders.AUTHORIZATION, tokenGerencia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(id)));

        mockMvc.perform(get("/api/v1/exportaciones/" + id).header(HttpHeaders.AUTHORIZATION, tokenFinanzas))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/exportaciones/" + id + "/descargar").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/exportaciones/" + id).header(HttpHeaders.AUTHORIZATION, tokenFinanzas))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/exportaciones").header(HttpHeaders.AUTHORIZATION, tokenFinanzas))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
        mockMvc.perform(delete("/api/v1/exportaciones/" + id).header(HttpHeaders.AUTHORIZATION, tokenGerencia))
                .andExpect(status().isNoContent());
    }

    @Test
    void lasExportacionesRespetanRolesYValidanLaSolicitud() throws Exception {
        mockMvc.perform(post("/api/v1/exportaciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"reporte-ejecutivo\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/exportaciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenFinanzas)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"reporte-operativo\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/exportaciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenCaja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"reporte-ejecutivo\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/exportaciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenGerencia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"reporte-ejecutivo\",\"parametros\":{\"sql\":\"1\"}}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/exportaciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenGerencia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/exportaciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenGerencia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"receta\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void soloElAdministradorGestionaLasPlantillas() throws Exception {
        for (String token : List.of(tokenMedico, tokenPaciente, tokenGerencia, tokenCaja)) {
            mockMvc.perform(get("/api/v1/plantillas").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/plantillas/receta/descargar").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/plantillas/receta/restaurar").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/plantillas").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(23)));
        mockMvc.perform(get("/api/v1/plantillas/receta/descargar").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jasperReport")));
        mockMvc.perform(get("/api/v1/plantillas/desconocida/descargar").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void laSubidaDePlantillasRechazaContenidoPeligrosoYAceptaUnaValida() throws Exception {
        String original = mockMvc.perform(
                        get("/api/v1/plantillas/constancia/descargar").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/api/v1/plantillas/constancia")
                        .file(new MockMultipartFile(
                                "archivo",
                                "constancia.jrxml",
                                "application/xml",
                                original.replace("name=\"constancia\"", "name=\"constancia\" scriptletClass=\"x.Y\"")
                                        .getBytes(StandardCharsets.UTF_8)))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/v1/plantillas/constancia")
                        .file(new MockMultipartFile(
                                "archivo",
                                "constancia.jrxml",
                                "application/xml",
                                original.replace("TuSalud", "Clínica Segura").getBytes(StandardCharsets.UTF_8)))
                        .param("motivo", "Cambio de marca")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalizada", is(true)))
                .andExpect(jsonPath("$.version", is(1)));

        mockMvc.perform(multipart("/api/v1/plantillas/constancia")
                        .file(new MockMultipartFile(
                                "archivo", "x.jrxml", "application/xml", original.getBytes(StandardCharsets.UTF_8)))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/plantillas/constancia/restaurar").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalizada", is(false)));
        mockMvc.perform(get("/api/v1/plantillas/constancia/historial").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }

    @Test
    void elMetodoPutSinMultipartNoSeAceptaParaPlantillas() throws Exception {
        mockMvc.perform(put("/api/v1/plantillas/constancia")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}

package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Archivo;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.repositories.ArchivoRepository;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
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
class ArchivosSeguridadTest {

    private static final String BASE = "/api/v1/archivos";
    private static final byte[] PNG = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private ArchivoRepository archivoRepository;

    private Medico medico;
    private Paciente paciente;
    private String tokenMedico;
    private String tokenPaciente;
    private String tokenOtroPaciente;
    private String tokenMedicoAjeno;
    private String tokenAdmin;
    private String tokenRecepcion;
    private String tokenImagenes;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        fixture.cita(medico, paciente, LocalDateTime.now().minusDays(1), EstadoCita.ATENDIDA);
        tokenMedico = token(UsuarioPrincipal.from(medico.getUsuario()));
        tokenPaciente = token(UsuarioPrincipal.from(paciente.getUsuario()));
        tokenOtroPaciente = token(UsuarioPrincipal.from(fixture.paciente().getUsuario()));
        tokenMedicoAjeno = token(UsuarioPrincipal.from(
                fixture.medicoConHorarioLunes(new BigDecimal("80.00")).getUsuario()));
        tokenAdmin = token(UsuarioPrincipal.from(fixture.usuario("adm", RolNombre.ROLE_ADMIN)));
        tokenRecepcion = token(UsuarioPrincipal.from(fixture.usuario("rec", RolNombre.ROLE_RECEPCION)));
        tokenImagenes = token(UsuarioPrincipal.from(fixture.usuario("img", RolNombre.ROLE_IMAGENES)));
    }

    private String token(UsuarioPrincipal principal) {
        return "Bearer " + jwtUtils.generateToken(principal);
    }

    private static byte[] pdf(int longitud) {
        byte[] bytes = new byte[longitud];
        Arrays.fill(bytes, (byte) 'x');
        byte[] cabecera = "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(cabecera, 0, bytes, 0, cabecera.length);
        return bytes;
    }

    private UUID subirAvatar(String token) throws Exception {
        MvcResult resultado = mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", PNG))
                        .param("proposito", "AVATAR")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
    }

    private UUID subirDocumento(byte[] contenido) throws Exception {
        MvcResult resultado = mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("archivo", "informe.pdf", "application/pdf", contenido))
                        .param("proposito", "DOCUMENTO_PACIENTE")
                        .param("referenciaId", paciente.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenciaTipo", is("PACIENTE")))
                .andReturn();
        return UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
    }

    @Test
    void sinTokenNoSeAccedeANada() throws Exception {
        mockMvc.perform(get(BASE + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/" + UUID.randomUUID() + "/metadatos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE)
                        .param("referenciaTipo", "PACIENTE")
                        .param("referenciaId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", PNG))
                        .param("proposito", "AVATAR"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laDescargaSeSirveSoloConSesionYConCabecerasSeguras() throws Exception {
        UUID id = subirAvatar(tokenPaciente);

        mockMvc.perform(get(BASE + "/" + id).header(HttpHeaders.AUTHORIZATION, tokenMedicoAjeno))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.ETAG, notNullValue()));
    }

    @Test
    void elEtagPermiteRevalidarSinReenviarElContenido() throws Exception {
        UUID id = subirAvatar(tokenPaciente);
        MvcResult primera = mockMvc.perform(get(BASE + "/" + id).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andReturn();
        String etag = primera.getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());
        mockMvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, tokenOtroPaciente)
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());
        UUID documento = subirDocumento(pdf(40));
        String etagDocumento = mockMvc.perform(
                        get(BASE + "/" + documento).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.ETAG);
        mockMvc.perform(get(BASE + "/" + documento)
                        .header(HttpHeaders.AUTHORIZATION, tokenOtroPaciente)
                        .header(HttpHeaders.IF_NONE_MATCH, etagDocumento))
                .andExpect(status().isForbidden());
    }

    @Test
    void elRangoDevuelveContenidoParcialYUnRangoInvalidoDa416() throws Exception {
        UUID id = subirDocumento(pdf(100));

        mockMvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .header(HttpHeaders.RANGE, "bytes=0-8"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 0-8/100"))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "9"))
                .andExpect(content().string("%PDF-1.4\n"));
        mockMvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .header(HttpHeaders.RANGE, "bytes=900-"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes */100"));
    }

    @Test
    void losPdfSeMuestranEnLineaYLosOfficeSeDescargan() throws Exception {
        UUID pdf = subirDocumento(pdf(40));
        mockMvc.perform(get(BASE + "/" + pdf).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        String csv = "codigo,nombre\n1,Ana\n";
        MvcResult resultado = mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("archivo", "lista.csv", "text/csv", csv.getBytes()))
                        .param("proposito", "DOCUMENTO_PACIENTE")
                        .param("referenciaId", paciente.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
        mockMvc.perform(get(BASE + "/" + id).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }

    @Test
    void rechazaArchivosPeligrososYSobredimensionados() throws Exception {
        byte[] exe = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0};
        mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", exe))
                        .param("proposito", "AVATAR")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile(
                                "archivo", "foto.svg", "image/svg+xml", "<svg xmlns='x'><script/></svg>".getBytes()))
                        .param("proposito", "AVATAR")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("archivo", "foto.pdf", "application/pdf", PNG))
                        .param("proposito", "AVATAR")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isBadRequest());
        byte[] grande = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, grande, 0, 8);
        mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("archivo", "grande.png", "image/png", grande))
                        .param("proposito", "AVATAR")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("2 MB")));
        mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", PNG))
                        .param("proposito", "INEXISTENTE")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart(BASE).param("proposito", "AVATAR").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laFotoDePerfilSeSubeComoArchivoSeDescargaYSeQuita() throws Exception {
        MvcResult resultado = mockMvc.perform(
                        multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/perfil/avatar")
                                .file(new MockMultipartFile("archivo", "yo.png", "image/png", PNG))
                                .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarId", notNullValue()))
                .andReturn();
        String avatarId = JsonPath.read(resultado.getResponse().getContentAsString(), "$.avatarId");
        mockMvc.perform(get(BASE + "/" + avatarId).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG));
        mockMvc.perform(get("/api/v1/perfil").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(jsonPath("$.avatarId", is(avatarId)));

        mockMvc.perform(delete("/api/v1/perfil/avatar").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/" + avatarId).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isNotFound());
    }

    @Test
    void laFotoDePerfilYaNoAceptaDataUrlsEnJson() throws Exception {
        mockMvc.perform(put("/api/v1/perfil/avatar")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imagen\":\"data:image/png;base64,iVBORw0KGgo=\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void unIdQueNoEsUuidResponde400YUnoInexistente404() throws Exception {
        mockMvc.perform(get(BASE + "/no-es-uuid").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isNotFound());
    }

    @Test
    void elDocumentoDeUnPacienteSoloLoVenElDuenoYElMedicoConRelacion() throws Exception {
        UUID id = subirDocumento(pdf(60));

        mockMvc.perform(get(BASE + "/" + id).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + id).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk());
        for (String intruso : new String[] {tokenOtroPaciente, tokenMedicoAjeno, tokenAdmin, tokenRecepcion}) {
            mockMvc.perform(get(BASE + "/" + id).header(HttpHeaders.AUTHORIZATION, intruso))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(BASE + "/" + id + "/metadatos").header(HttpHeaders.AUTHORIZATION, intruso))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(BASE + "/" + id).header(HttpHeaders.AUTHORIZATION, intruso))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(BASE)
                        .param("referenciaTipo", "PACIENTE")
                        .param("referenciaId", paciente.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, tokenOtroPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
        mockMvc.perform(get(BASE)
                        .param("referenciaTipo", "PACIENTE")
                        .param("referenciaId", paciente.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void soloElMedicoConRelacionAdjuntaDocumentosDeUnPaciente() throws Exception {
        for (String quien : new String[] {tokenPaciente, tokenMedicoAjeno, tokenAdmin, tokenRecepcion}) {
            mockMvc.perform(multipart(BASE)
                            .file(new MockMultipartFile("archivo", "x.pdf", "application/pdf", pdf(30)))
                            .param("proposito", "DOCUMENTO_PACIENTE")
                            .param("referenciaId", paciente.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, quien))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void imagenesYReportesNoSeSubenPorElEndpointGenerico() throws Exception {
        for (String proposito : new String[] {"IMAGEN_DIAGNOSTICA", "REPORTE"}) {
            mockMvc.perform(multipart(BASE)
                            .file(new MockMultipartFile("archivo", "x.png", "image/png", PNG))
                            .param("proposito", proposito)
                            .header(HttpHeaders.AUTHORIZATION, tokenImagenes))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void lasPlantillasSoloLasCargaAdminOrrhh() throws Exception {
        String jrxml =
                "<?xml version=\"1.0\"?><jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\""
                        + " name=\"r\"><title/></jasperReport>";
        mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("archivo", "r.jrxml", "application/xml", jrxml.getBytes()))
                        .param("proposito", "PLANTILLA")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("archivo", "r.jrxml", "application/xml", jrxml.getBytes()))
                        .param("proposito", "PLANTILLA")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoContenido", is("application/xml")));
    }

    private UUID estudioOrdenYArchivo(boolean informar, UUID[] ordenSalida) throws Exception {
        MvcResult estudio = mockMvc.perform(post("/api/v1/imagenes/estudios")
                        .header(HttpHeaders.AUTHORIZATION, tokenImagenes)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"A-RX" + ServiceTestFixture.siguiente()
                                + "\",\"nombre\":\"Radiografía\",\"modalidad\":\"RX\",\"region\":\"Tórax\","
                                + "\"precio\":80.00,\"duracionMinutos\":20}"))
                .andExpect(status().isCreated())
                .andReturn();
        String estudioId = JsonPath.read(estudio.getResponse().getContentAsString(), "$.id");
        MvcResult orden = mockMvc.perform(post("/api/v1/imagenes/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + paciente.getId() + "\",\"prioridad\":\"NORMAL\","
                                + "\"indicacionClinica\":\"Tos persistente\",\"estudioIds\":[\"" + estudioId + "\"]}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID ordenId = UUID.fromString(JsonPath.read(orden.getResponse().getContentAsString(), "$.id"));
        ordenSalida[0] = ordenId;
        mockMvc.perform(put("/api/v1/imagenes/ordenes/" + ordenId + "/programar")
                        .header(HttpHeaders.AUTHORIZATION, tokenImagenes)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programadaPara\":\"" + LocalDate.now().plusDays(2)
                                + "T09:00:00\",\"sala\":\"Sala 1\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/v1/imagenes/ordenes/" + ordenId + "/archivos")
                        .file(new MockMultipartFile("archivos", "toma.png", "image/png", PNG))
                        .header(HttpHeaders.AUTHORIZATION, tokenImagenes))
                .andExpect(status().isCreated());
        if (informar) {
            mockMvc.perform(put("/api/v1/imagenes/ordenes/" + ordenId + "/realizar")
                            .header(HttpHeaders.AUTHORIZATION, tokenImagenes)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
            mockMvc.perform(put("/api/v1/imagenes/ordenes/" + ordenId + "/informe")
                            .header(HttpHeaders.AUTHORIZATION, tokenImagenes)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"hallazgos\":\"Sin lesiones\",\"conclusion\":\"Normal\","
                                    + "\"hallazgoCritico\":false,\"firmaColegiatura\":\"CMP-1234\"}"))
                    .andExpect(status().isOk());
        }
        Archivo archivo = archivoRepository
                .findActivosDeReferencia("ORDEN_IMAGEN", ordenId)
                .getFirst();
        return archivo.getId();
    }

    @Test
    void laImagenDiagnosticaSigueLasReglasDeLaOrdenTambienPorElEndpointGenerico() throws Exception {
        UUID archivo = estudioOrdenYArchivo(false, new UUID[1]);

        mockMvc.perform(get(BASE + "/" + archivo).header(HttpHeaders.AUTHORIZATION, tokenImagenes))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG));
        mockMvc.perform(get(BASE + "/" + archivo).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk());
        for (String intruso :
                new String[] {tokenPaciente, tokenOtroPaciente, tokenMedicoAjeno, tokenAdmin, tokenRecepcion}) {
            mockMvc.perform(get(BASE + "/" + archivo).header(HttpHeaders.AUTHORIZATION, intruso))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(delete(BASE + "/" + archivo).header(HttpHeaders.AUTHORIZATION, tokenImagenes))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elPacienteVeSuImagenSoloCuandoLaOrdenEstaInformada() throws Exception {
        UUID archivo = estudioOrdenYArchivo(true, new UUID[1]);

        mockMvc.perform(get(BASE + "/" + archivo).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + archivo).header(HttpHeaders.AUTHORIZATION, tokenOtroPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/" + archivo).header(HttpHeaders.AUTHORIZATION, tokenMedicoAjeno))
                .andExpect(status().isForbidden());
    }
}

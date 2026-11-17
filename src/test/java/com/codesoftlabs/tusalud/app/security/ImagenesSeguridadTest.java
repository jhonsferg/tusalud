package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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

import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class ImagenesSeguridadTest {

    private static final String BASE = "/api/v1/imagenes";
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private String tokenMedico;
    private String tokenPaciente;
    private String tokenImagenes;
    private String tokenAdmin;
    private String tokenRecepcion;
    private String tokenCaja;
    private String tokenFarmacia;
    private String tokenOtroPaciente;
    private String tokenMedicoSinRelacion;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        fixture.cita(medico, paciente, LocalDateTime.now().minusDays(1), EstadoCita.ATENDIDA);
        tokenMedico = token(UsuarioPrincipal.from(medico.getUsuario()));
        tokenPaciente = token(UsuarioPrincipal.from(paciente.getUsuario()));
        tokenImagenes = token(UsuarioPrincipal.from(fixture.usuario("img", RolNombre.ROLE_IMAGENES)));
        tokenAdmin = token(UsuarioPrincipal.from(fixture.usuario("admin", RolNombre.ROLE_ADMIN)));
        tokenRecepcion = token(UsuarioPrincipal.from(fixture.usuario("recep", RolNombre.ROLE_RECEPCION)));
        tokenCaja = token(UsuarioPrincipal.from(fixture.usuario("caja", RolNombre.ROLE_CAJA)));
        tokenFarmacia = token(UsuarioPrincipal.from(fixture.usuario("farm", RolNombre.ROLE_FARMACIA)));
        tokenOtroPaciente = token(UsuarioPrincipal.from(fixture.paciente().getUsuario()));
        tokenMedicoSinRelacion = token(UsuarioPrincipal.from(
                fixture.medicoConHorarioLunes(new BigDecimal("80.00")).getUsuario()));
    }

    private String token(UsuarioPrincipal principal) {
        return "Bearer " + jwtUtils.generateToken(principal);
    }

    private UUID crearEstudio(String codigo) throws Exception {
        MvcResult resultado = mockMvc.perform(post(BASE + "/estudios")
                        .header(HttpHeaders.AUTHORIZATION, tokenImagenes)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"nombre\":\"Radiografía de prueba\","
                                + "\"modalidad\":\"RX\",\"region\":\"Tórax\",\"precio\":80.00,"
                                + "\"duracionMinutos\":20}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
    }

    private UUID solicitar(UUID estudioId) throws Exception {
        MvcResult resultado = mockMvc.perform(post(BASE + "/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + paciente.getId() + "\",\"prioridad\":\"NORMAL\","
                                + "\"indicacionClinica\":\"Tos persistente\",\"estudioIds\":[\"" + estudioId + "\"]}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
    }

    private void programar(UUID orden) throws Exception {
        mockMvc.perform(put(BASE + "/ordenes/" + orden + "/programar")
                        .header(HttpHeaders.AUTHORIZATION, tokenImagenes)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programadaPara\":\"" + LocalDate.now().plusDays(2) + "T09:00:00\","
                                + "\"sala\":\"Sala 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("PROGRAMADA")));
    }

    private UUID subir(UUID orden, byte[] contenido, String nombre) throws Exception {
        MvcResult resultado = mockMvc.perform(multipart(BASE + "/ordenes/" + orden + "/archivos")
                        .file(new MockMultipartFile("archivos", nombre, "image/png", contenido))
                        .header(HttpHeaders.AUTHORIZATION, tokenImagenes))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$[0].id"));
    }

    private UUID ordenInformada() throws Exception {
        UUID estudio = crearEstudio("T-RX" + ServiceTestFixture.siguiente());
        UUID orden = solicitar(estudio);
        programar(orden);
        subir(orden, PNG, "toma.png");
        mockMvc.perform(put(BASE + "/ordenes/" + orden + "/realizar")
                        .header(HttpHeaders.AUTHORIZATION, tokenImagenes)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("REALIZADA")));
        mockMvc.perform(put(BASE + "/ordenes/" + orden + "/informe")
                        .header(HttpHeaders.AUTHORIZATION, tokenImagenes)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hallazgos\":\"Sin lesiones\",\"conclusion\":\"Normal\","
                                + "\"hallazgoCritico\":false,\"firmaColegiatura\":\"CMP-1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("INFORMADA")))
                .andExpect(jsonPath("$.informe.version", is(1)));
        return orden;
    }

    @Test
    void sinTokenNoSeAccedeANada() throws Exception {
        mockMvc.perform(get(BASE + "/estudios")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/ordenes/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/ordenes/" + UUID.randomUUID() + "/archivos/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadaRolSoloHaceLoQueLeCorresponde() throws Exception {
        UUID estudio = crearEstudio("T-RX" + ServiceTestFixture.siguiente());
        String solicitud = "{\"pacienteId\":\"" + paciente.getId() + "\",\"prioridad\":\"NORMAL\","
                + "\"indicacionClinica\":\"Dolor\",\"estudioIds\":[\"" + estudio + "\"]}";
        for (String token : new String[] {tokenPaciente, tokenImagenes, tokenAdmin, tokenRecepcion, tokenCaja}) {
            mockMvc.perform(post(BASE + "/ordenes")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(solicitud))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post(BASE + "/estudios")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"X-1\",\"nombre\":\"X\",\"modalidad\":\"RX\",\"region\":\"Tórax\","
                                + "\"precio\":10,\"duracionMinutos\":10}"))
                .andExpect(status().isForbidden());
        for (String token : new String[] {tokenPaciente, tokenRecepcion, tokenCaja, tokenFarmacia}) {
            mockMvc.perform(get(BASE + "/ordenes").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(BASE + "/estudios").header(HttpHeaders.AUTHORIZATION, tokenFarmacia))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/mis-estudios").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/agenda")
                        .param("desde", LocalDate.now().toString())
                        .param("hasta", LocalDate.now().toString())
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());
    }

    @Test
    void elFlujoCompletoPorHttpTerminaConElPacienteViendoSuEstudio() throws Exception {
        UUID orden = ordenInformada();

        mockMvc.perform(get(BASE + "/mis-estudios").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado", is("INFORMADA")))
                .andExpect(jsonPath("$[0].informe.conclusion", is("Normal")))
                .andExpect(jsonPath("$[0].indicacionClinica").doesNotExist())
                .andExpect(jsonPath("$[0].dni").doesNotExist())
                .andExpect(jsonPath("$[0].archivos", hasSize(1)));
        mockMvc.perform(get(BASE + "/ordenes/" + orden).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicacionClinica", is("Tos persistente")))
                .andExpect(jsonPath("$.informe.firmaColegiatura", is("CMP-1234")));
        mockMvc.perform(get(BASE + "/ordenes/" + orden + "/informes").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get(BASE + "/mi-firma").header(HttpHeaders.AUTHORIZATION, tokenImagenes))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colegiatura", is("CMP-1234")));
    }

    @Test
    void lasImagenesSeSirvenSoloConAccesoYConCabecerasSeguras() throws Exception {
        UUID orden = ordenInformada();
        MvcResult lista = mockMvc.perform(
                        get(BASE + "/ordenes/" + orden).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andReturn();
        String archivo = JsonPath.read(lista.getResponse().getContentAsString(), "$.archivos[0].id");
        String ruta = BASE + "/ordenes/" + orden + "/archivos/" + archivo;

        mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Content-Security-Policy", containsString("sandbox")))
                .andExpect(content().bytes(PNG));
        mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk());
        mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, tokenImagenes))
                .andExpect(status().isOk());
        for (String token :
                new String[] {tokenOtroPaciente, tokenMedicoSinRelacion, tokenAdmin, tokenRecepcion, tokenFarmacia}) {
            mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
        }
    }

    @Test
    void ningunOtroActorVeLaOrdenCompleta() throws Exception {
        UUID orden = ordenInformada();

        for (String token : new String[] {tokenOtroPaciente, tokenMedicoSinRelacion, tokenCaja, tokenFarmacia}) {
            mockMvc.perform(get(BASE + "/ordenes/" + orden).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(BASE + "/pacientes/" + paciente.getId() + "/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedicoSinRelacion))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/pacientes/" + paciente.getId() + "/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void administracionYRecepcionVenSoloLaAgenda() throws Exception {
        UUID orden = ordenInformada();

        for (String token : new String[] {tokenAdmin, tokenRecepcion}) {
            mockMvc.perform(get(BASE + "/ordenes/" + orden).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sala", is("Sala 1")))
                    .andExpect(jsonPath("$.indicacionClinica").doesNotExist())
                    .andExpect(jsonPath("$.informe").doesNotExist())
                    .andExpect(jsonPath("$.dni").doesNotExist());
            mockMvc.perform(get(BASE + "/agenda")
                            .param("desde", LocalDate.now().toString())
                            .param("hasta", LocalDate.now().plusDays(3).toString())
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].sala", is("Sala 1")))
                    .andExpect(jsonPath("$[0].indicacionClinica").doesNotExist());
        }
    }

    @Test
    void lasCargasInvalidasSeRechazan() throws Exception {
        UUID estudio = crearEstudio("T-RX" + ServiceTestFixture.siguiente());
        UUID orden = solicitar(estudio);
        programar(orden);

        mockMvc.perform(multipart(BASE + "/ordenes/" + orden + "/archivos")
                        .file(new MockMultipartFile("archivos", "falsa.png", "image/png", "texto".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, tokenImagenes))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart(BASE + "/ordenes/" + orden + "/archivos")
                        .file(new MockMultipartFile("archivos", "toma.png", "image/png", PNG))
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart(BASE + "/ordenes/" + orden + "/archivos")
                        .file(new MockMultipartFile("archivos", "toma.png", "image/png", PNG))
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isForbidden());
        UUID archivo = subir(orden, PNG, "toma.png");
        mockMvc.perform(delete(BASE + "/ordenes/" + orden + "/archivos/" + archivo)
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/ordenes/" + orden + "/archivos/" + archivo)
                        .header(HttpHeaders.AUTHORIZATION, tokenImagenes))
                .andExpect(status().isNoContent());
    }

    @Test
    void unIdMalFormadoRespondeCuatrocientosYUnoInexistenteCuatrocientosCuatro() throws Exception {
        mockMvc.perform(get(BASE + "/ordenes/no-es-un-uuid").header(HttpHeaders.AUTHORIZATION, tokenImagenes))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
        mockMvc.perform(get(BASE + "/ordenes/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, tokenImagenes))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(BASE + "/ordenes/" + UUID.randomUUID() + "/informe")
                        .header(HttpHeaders.AUTHORIZATION, tokenImagenes)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hallazgos\":\"\",\"conclusion\":\"\",\"firmaColegiatura\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void laOrdenNoExponeCamposInternosAlPaciente() throws Exception {
        UUID estudio = crearEstudio("T-RX" + ServiceTestFixture.siguiente());
        UUID orden = solicitar(estudio);

        mockMvc.perform(get(BASE + "/ordenes/" + orden).header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("SOLICITADA")))
                .andExpect(jsonPath("$.indicacionClinica").doesNotExist())
                .andExpect(jsonPath("$.total").doesNotExist())
                .andExpect(jsonPath("$.medicoNombre", notNullValue()))
                .andExpect(jsonPath("$.hallazgoCritico").doesNotExist());
    }
}

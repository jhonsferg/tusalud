package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.MedicamentoRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.PrincipioActivoRequest;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.RecetaItem;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaControl;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.codesoftlabs.tusalud.app.services.receta.CatalogoMedicamentosService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
class RecetasSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private AtencionRepository atencionRepository;

    @Autowired
    private CatalogoMedicamentosService catalogo;

    private Medico medicoEntidad;
    private Paciente pacienteEntidad;
    private Atencion atencion;
    private UUID medicamento;
    private String medico;
    private String medicoSinRelacion;
    private String paciente;
    private String otroPaciente;
    private String admin;
    private String farmacia;
    private String recepcion;
    private String caja;
    private String enfermeria;
    private String laboratorio;

    @BeforeEach
    void setUp() {
        medicoEntidad = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        pacienteEntidad = fixture.paciente();
        var cita =
                fixture.cita(medicoEntidad, pacienteEntidad, LocalDateTime.now().minusHours(1), EstadoCita.ATENDIDA);
        atencion = atencionRepository.save(Atencion.builder()
                .cita(cita)
                .paciente(pacienteEntidad)
                .medico(medicoEntidad)
                .fecha(LocalDateTime.now())
                .diagnostico("Diagnóstico")
                .receta(new ArrayList<>(List.of(
                        RecetaItem.builder().medicamento("Línea").dosis("1").build())))
                .build());
        int n = ServiceTestFixture.siguiente();
        UUID principio = catalogo.crearPrincipio(
                        new PrincipioActivoRequest("Seguridad " + n, "Clase", null, null, null, null, null, null, null))
                .id();
        medicamento = catalogo.crearMedicamento(new MedicamentoRequest(
                        principio,
                        null,
                        "Tableta",
                        "10 mg",
                        "Oral",
                        null,
                        CategoriaControl.NINGUNA,
                        true,
                        null,
                        null,
                        true,
                        true))
                .id();
        medico = token(medicoEntidad.getUsuario());
        medicoSinRelacion =
                token(fixture.medicoConHorarioLunes(new BigDecimal("90.00")).getUsuario());
        paciente = token(pacienteEntidad.getUsuario());
        otroPaciente = token(fixture.paciente().getUsuario());
        admin = token(fixture.usuario("rx-adm", RolNombre.ROLE_ADMIN));
        farmacia = token(fixture.usuario("rx-far", RolNombre.ROLE_FARMACIA));
        recepcion = token(fixture.usuario("rx-rec", RolNombre.ROLE_RECEPCION));
        caja = token(fixture.usuario("rx-caj", RolNombre.ROLE_CAJA));
        enfermeria = token(fixture.usuario("rx-enf", RolNombre.ROLE_ENFERMERIA));
        laboratorio = token(fixture.usuario("rx-lab", RolNombre.ROLE_LABORATORIO));
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    private String emision() {
        return "{\"atencionId\":\"" + atencion.getId() + "\",\"items\":[{\"atencionPosicion\":0,\"medicamentoId\":\""
                + medicamento + "\",\"cantidad\":10}]}";
    }

    private String emitirComoMedico() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/recetas")
                        .header("Authorization", medico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emision()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(cuerpo, "$.id");
    }

    @Test
    void sinTokenLosEndpointsRetornan401() throws Exception {
        mockMvc.perform(get("/api/v1/medicamentos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/recetas/mias")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/recetas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void soloElMedicoDeLaAtencionEmiteLaReceta() throws Exception {
        for (String token : new String[] {paciente, admin, farmacia, recepcion, caja, enfermeria, laboratorio}) {
            mockMvc.perform(post("/api/v1/recetas")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emision()))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/v1/recetas")
                        .header("Authorization", medicoSinRelacion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emision()))
                .andExpect(status().isForbidden());
        emitirComoMedico();
    }

    @Test
    void recepcionCajaYAdministracionNoVenElContenidoDeLaReceta() throws Exception {
        String id = emitirComoMedico();
        for (String token : new String[] {recepcion, caja, admin, enfermeria, laboratorio}) {
            mockMvc.perform(get("/api/v1/recetas/" + id).header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/recetas/" + id + "/pdf").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/recetas/farmacia").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void elPacienteVeSuRecetaYNoLaDeOtro() throws Exception {
        String id = emitirComoMedico();

        mockMvc.perform(get("/api/v1/recetas/" + id).header("Authorization", paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("EMITIDA")))
                .andExpect(jsonPath("$.firmaValida", is(true)));
        mockMvc.perform(get("/api/v1/recetas/" + id).header("Authorization", otroPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/recetas/mias").header("Authorization", otroPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
        mockMvc.perform(get("/api/v1/recetas/" + id + "/pdf").header("Authorization", paciente))
                .andExpect(status().isOk());
    }

    @Test
    void unMedicoSinRelacionNoVeLaRecetaNiLasDelPaciente() throws Exception {
        String id = emitirComoMedico();

        mockMvc.perform(get("/api/v1/recetas/" + id).header("Authorization", medicoSinRelacion))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/recetas/paciente/" + pacienteEntidad.getId())
                        .header("Authorization", medicoSinRelacion))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/recetas/paciente/" + pacienteEntidad.getId())
                        .header("Authorization", medico))
                .andExpect(status().isOk());
    }

    @Test
    void laFarmaciaVeSoloLoNecesarioParaDispensarSinDiagnostico() throws Exception {
        String id = emitirComoMedico();

        String cuerpo = mockMvc.perform(get("/api/v1/recetas/farmacia/" + id).header("Authorization", farmacia))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain("diagnostico")
                .doesNotContain("indicaciones")
                .doesNotContain("firma");
        mockMvc.perform(get("/api/v1/recetas/farmacia/" + id).header("Authorization", medico))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/recetas/farmacia/" + id).header("Authorization", paciente))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloLaFarmaciaDispensa() throws Exception {
        String id = emitirComoMedico();
        String cuerpo =
                "{\"items\":[{\"itemId\":\"" + UUID.randomUUID() + "\",\"cantidad\":1}],\"metodoPago\":\"EFECTIVO\"}";
        for (String token : new String[] {medico, paciente, admin, recepcion, caja, enfermeria}) {
            mockMvc.perform(post("/api/v1/recetas/farmacia/" + id + "/dispensar")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void soloAdministracionGestionaElCatalogo() throws Exception {
        String principio = "{\"dci\":\"Prueba seguridad\"}";
        for (String token : new String[] {medico, paciente, farmacia, recepcion, caja, enfermeria}) {
            mockMvc.perform(post("/api/v1/medicamentos/principios-activos")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(principio))
                    .andExpect(status().isForbidden());
            mockMvc.perform(multipart("/api/v1/medicamentos/importaciones")
                            .file(new MockMultipartFile("archivo", "c.csv", "text/csv", "dci".getBytes()))
                            .header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/medicamentos?q=Seguridad").header("Authorization", medico))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/medicamentos").header("Authorization", paciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/medicamentos").header("Authorization", recepcion))
                .andExpect(status().isForbidden());
    }

    @Test
    void laVerificacionPublicaReconoceLaRecetaSinExponerDatosDelPaciente() throws Exception {
        String id = emitirComoMedico();
        String codigo = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/v1/recetas/" + id).header("Authorization", medico))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.codigoVerificacion");

        String cuerpo = mockMvc.perform(get("/api/v1/verificacion/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("VALIDO")))
                .andExpect(jsonPath("$.tipo", is("Receta electrónica")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(cuerpo)
                .doesNotContain(pacienteEntidad.getDni())
                .doesNotContain("diagnostico");
    }
}

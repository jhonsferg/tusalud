package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import java.math.BigDecimal;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class AccesoDatosClinicosSeguridadTest {

    private static final String DATOS =
            "{\"grupoSanguineo\":\"O+\",\"alergias\":\"Penicilina\",\"antecedentes\":\"Asma\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Medico ajeno;
    private Paciente paciente;
    private Cita cita;
    private String tokenMedico;
    private String tokenAjeno;
    private String tokenPaciente;
    private String tokenOtroPaciente;
    private String tokenAdmin;
    private String tokenCaja;
    private String tokenRecepcion;
    private String tokenFarmacia;
    private String tokenLaboratorio;
    private String tokenEnfermeria;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        ajeno = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        cita = fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
        tokenMedico = token(UsuarioPrincipal.from(medico.getUsuario()));
        tokenAjeno = token(UsuarioPrincipal.from(ajeno.getUsuario()));
        tokenPaciente = token(UsuarioPrincipal.from(paciente.getUsuario()));
        tokenOtroPaciente = token(UsuarioPrincipal.from(fixture.paciente().getUsuario()));
        tokenAdmin = token(UsuarioPrincipal.from(fixture.usuario("admin", RolNombre.ROLE_ADMIN)));
        tokenCaja = token(UsuarioPrincipal.from(fixture.usuario("caja", RolNombre.ROLE_CAJA)));
        tokenRecepcion = token(UsuarioPrincipal.from(fixture.usuario("recep", RolNombre.ROLE_RECEPCION)));
        tokenFarmacia = token(UsuarioPrincipal.from(fixture.usuario("farm", RolNombre.ROLE_FARMACIA)));
        tokenLaboratorio = token(UsuarioPrincipal.from(fixture.usuario("lab", RolNombre.ROLE_LABORATORIO)));
        tokenEnfermeria = token(UsuarioPrincipal.from(fixture.usuario("enf", RolNombre.ROLE_ENFERMERIA)));
    }

    private String token(UsuarioPrincipal principal) {
        return "Bearer " + jwtUtils.generateToken(principal);
    }

    private String historia(UUID pacienteId) {
        return "/api/v1/pacientes/" + pacienteId + "/historia-clinica";
    }

    private String datosMedicos(UUID pacienteId) {
        return "/api/v1/pacientes/" + pacienteId + "/datos-medicos";
    }

    @Test
    void elPacienteYElRestoDelPersonalNoEditanLosDatosMedicos() throws Exception {
        for (String token : new String[] {
            tokenPaciente, tokenAdmin, tokenCaja, tokenRecepcion, tokenFarmacia, tokenLaboratorio, tokenEnfermeria
        }) {
            mockMvc.perform(put(datosMedicos(paciente.getId()))
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(DATOS))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void elEndpointDeEdicionPropiaDelPacienteYaNoExiste() throws Exception {
        mockMvc.perform(put("/api/v1/historia-clinica/datos-medicos")
                        .header(HttpHeaders.AUTHORIZATION, tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS))
                .andExpect(status().is4xxClientError())
                .andExpect(status().is(org.hamcrest.Matchers.not(200)));
        mockMvc.perform(get("/api/v1/historia-clinica").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente.grupoSanguineo").doesNotExist());
    }

    @Test
    void unMedicoSinRelacionNoEditaNiConsultaDatosDeUnPacienteAjeno() throws Exception {
        mockMvc.perform(put(datosMedicos(paciente.getId()))
                        .header(HttpHeaders.AUTHORIZATION, tokenAjeno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(historia(paciente.getId())).header(HttpHeaders.AUTHORIZATION, tokenAjeno))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(historia(UUID.randomUUID())).header(HttpHeaders.AUTHORIZATION, tokenAjeno))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/medicos/me/pacientes").header(HttpHeaders.AUTHORIZATION, tokenAjeno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void elMedicoConRelacionEditaLosDatosMedicosYQuedaConstancia() throws Exception {
        mockMvc.perform(put(datosMedicos(paciente.getId()))
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupoSanguineo", is("O+")))
                .andExpect(jsonPath("$.alergias", is("Penicilina")))
                .andExpect(jsonPath(
                        "$.datosMedicosActualizadoPor", is(medico.getUsuario().getNombreCompleto())))
                .andExpect(jsonPath("$.datosMedicosActualizadoEn", notNullValue()));
        mockMvc.perform(get("/api/v1/historia-clinica").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente.grupoSanguineo", is("O+")))
                .andExpect(jsonPath("$.paciente.datosMedicosActualizadoPor", notNullValue()));
        mockMvc.perform(put(datosMedicos(paciente.getId()))
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grupoSanguineo\":\"Z+\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void soloElMedicoConRelacionLeeLaHistoriaClinica() throws Exception {
        mockMvc.perform(get(historia(paciente.getId())).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente.pacienteId", is(paciente.getId().toString())));
        for (String token : new String[] {
            tokenAdmin,
            tokenCaja,
            tokenRecepcion,
            tokenFarmacia,
            tokenLaboratorio,
            tokenEnfermeria,
            tokenPaciente,
            tokenOtroPaciente
        }) {
            mockMvc.perform(get(historia(paciente.getId())).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void laAtencionDeUnaCitaSoloLaVenSuMedicoYSuPaciente() throws Exception {
        mockMvc.perform(post("/api/v1/citas/" + cita.getId() + "/atencion")
                        .header(HttpHeaders.AUTHORIZATION, tokenAjeno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnostico\":\"Intruso\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/citas/" + cita.getId() + "/atencion")
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnostico\":\"Faringitis\"}"))
                .andExpect(status().isCreated());

        for (String token : new String[] {tokenMedico, tokenPaciente}) {
            mockMvc.perform(get("/api/v1/citas/" + cita.getId() + "/atencion").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.diagnostico", is("Faringitis")));
        }
        for (String token : new String[] {
            tokenAdmin, tokenAjeno, tokenOtroPaciente, tokenCaja, tokenRecepcion, tokenFarmacia, tokenLaboratorio
        }) {
            mockMvc.perform(get("/api/v1/citas/" + cita.getId() + "/atencion").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(put("/api/v1/citas/" + cita.getId() + "/atencion")
                        .header(HttpHeaders.AUTHORIZATION, tokenAjeno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnostico\":\"Falsificado\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void otrosPacientesYMedicosAjenosNoVenLaCitaNiPuedenPagarlaNiCancelarla() throws Exception {
        for (String token : new String[] {tokenAjeno, tokenOtroPaciente}) {
            mockMvc.perform(get("/api/v1/citas/" + cita.getId()).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(put("/api/v1/citas/" + cita.getId() + "/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, tokenOtroPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/citas/" + cita.getId() + "/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, tokenAjeno))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/citas/" + cita.getId() + "/atender").header(HttpHeaders.AUTHORIZATION, tokenAjeno))
                .andExpect(status().isForbidden());
    }

    @Test
    void laBusquedaDePacientesDevuelveSoloLoNecesarioSegunElRol() throws Exception {
        mockMvc.perform(get("/api/v1/recepcion/pacientes")
                        .param("q", paciente.getDni())
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email", notNullValue()))
                .andExpect(jsonPath("$[0].fechaNacimiento", notNullValue()));
        for (String token : new String[] {tokenCaja, tokenFarmacia, tokenEnfermeria}) {
            mockMvc.perform(get("/api/v1/recepcion/pacientes")
                            .param("q", paciente.getDni())
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].pacienteId", is(paciente.getId().toString())))
                    .andExpect(jsonPath("$[0].dni", is(paciente.getDni())))
                    .andExpect(jsonPath("$[0].email").doesNotExist())
                    .andExpect(jsonPath("$[0].fechaNacimiento").doesNotExist())
                    .andExpect(jsonPath("$[0].historialClinicoNumero").doesNotExist());
        }
        mockMvc.perform(get("/api/v1/recepcion/pacientes")
                        .param("q", paciente.getDni())
                        .header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").doesNotExist());
        mockMvc.perform(get("/api/v1/recepcion/pacientes")
                        .param("q", paciente.getDni())
                        .header(HttpHeaders.AUTHORIZATION, tokenAjeno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        for (String token : new String[] {tokenPaciente, tokenLaboratorio}) {
            mockMvc.perform(get("/api/v1/recepcion/pacientes")
                            .param("q", paciente.getDni())
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/recepcion/pacientes")
                        .param("q", "9")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void caja_farmaciaYLaboratorioNoAccedenALosDatosClinicosDeOtrosModulos() throws Exception {
        mockMvc.perform(get("/api/v1/laboratorio/ordenes/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, tokenFarmacia))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/laboratorio/ordenes/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/laboratorio/ordenes").header(HttpHeaders.AUTHORIZATION, tokenCaja))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/farmacia/recetas").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/farmacia/recetas").header(HttpHeaders.AUTHORIZATION, tokenLaboratorio))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/farmacia/recetas").header(HttpHeaders.AUTHORIZATION, tokenFarmacia))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/caja/comprobantes").header(HttpHeaders.AUTHORIZATION, tokenFarmacia))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/hospitalizacion/ingresos/" + UUID.randomUUID() + "/evoluciones")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isForbidden());
    }

    @Test
    void laRecepcionNoVeElMotivoDeLaConsultaEnSusListados() throws Exception {
        LocalDateTime hoy = LocalDateTime.now();
        fixture.cita(medico, fixture.paciente(), hoy.plusMinutes(5), EstadoCita.CONFIRMADA);
        mockMvc.perform(get("/api/v1/recepcion/citas-hoy").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motivo").doesNotExist());
        mockMvc.perform(post("/api/v1/recepcion/turnos")
                        .header(HttpHeaders.AUTHORIZATION, tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + paciente.getId() + "\",\"medicoId\":\"" + medico.getId()
                                + "\",\"prioridad\":\"NORMAL\",\"motivo\":\"Dolor torácico\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/recepcion/cola").header(HttpHeaders.AUTHORIZATION, tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motivo").doesNotExist());
        mockMvc.perform(get("/api/v1/recepcion/cola").header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motivo", is("Dolor torácico")));
    }

    @Test
    void elContactoDelMedicoSoloLoVeElAdministradorYElPropioMedico() throws Exception {
        String ruta = "/api/v1/medicos/" + medico.getId();
        mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(medico.getUsuario().getEmail())));
        mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, tokenMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", notNullValue()));
        for (String token : new String[] {tokenPaciente, tokenAjeno, tokenCaja}) {
            mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombreCompleto", notNullValue()))
                    .andExpect(jsonPath("$.colegiatura", notNullValue()))
                    .andExpect(jsonPath("$.email").doesNotExist())
                    .andExpect(jsonPath("$.telefono").doesNotExist());
        }
        mockMvc.perform(get("/api/v1/medicos").header(HttpHeaders.AUTHORIZATION, tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }
}

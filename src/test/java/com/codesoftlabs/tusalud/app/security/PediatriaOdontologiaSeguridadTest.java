package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.EsquemaDosis;
import com.codesoftlabs.tusalud.app.entities.EsquemaVacunacion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.Vacuna;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.repositories.EsquemaDosisRepository;
import com.codesoftlabs.tusalud.app.repositories.EsquemaVacunacionRepository;
import com.codesoftlabs.tusalud.app.repositories.VacunaRepository;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import java.time.LocalDate;
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
class PediatriaOdontologiaSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private VacunaRepository vacunas;

    @Autowired
    private EsquemaVacunacionRepository esquemas;

    @Autowired
    private EsquemaDosisRepository dosis;

    @Autowired
    private com.codesoftlabs.tusalud.app.repositories.PacienteRepository pacientes;

    private Medico medicoEntidad;
    private Paciente pacienteEntidad;
    private Vacuna bcg;
    private String medico;
    private String medicoSinRelacion;
    private String paciente;
    private String otroPaciente;
    private String admin;
    private String recepcion;
    private String caja;
    private String enfermeria;
    private String rrhh;
    private String gerencia;

    @BeforeEach
    void setUp() {
        bcg = vacunas.save(Vacuna.builder().codigo("BCG-S").nombre("BCG").build());
        EsquemaVacunacion esquema = esquemas.save(EsquemaVacunacion.builder()
                .nombre("Esquema de seguridad")
                .vigenteDesde(LocalDate.of(2000, 1, 1))
                .build());
        dosis.save(EsquemaDosis.builder()
                .esquema(esquema)
                .vacuna(bcg)
                .numeroDosis(1)
                .etiqueta("Única")
                .edadMinimaDias(0)
                .edadRecomendadaDias(0)
                .edadMaximaDias(365)
                .build());
        medicoEntidad = fixture.medicoConHorarioLunes(new java.math.BigDecimal("100.00"));
        pacienteEntidad = fixture.paciente();
        fixture.cita(medicoEntidad, pacienteEntidad, LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
        medico = token(medicoEntidad.getUsuario());
        medicoSinRelacion = token(
                fixture.medicoConHorarioLunes(new java.math.BigDecimal("90.00")).getUsuario());
        paciente = token(pacienteEntidad.getUsuario());
        otroPaciente = token(fixture.paciente().getUsuario());
        admin = token(fixture.usuario("po-adm", RolNombre.ROLE_ADMIN));
        recepcion = token(fixture.usuario("po-rec", RolNombre.ROLE_RECEPCION));
        caja = token(fixture.usuario("po-caj", RolNombre.ROLE_CAJA));
        enfermeria = token(fixture.usuario("po-enf", RolNombre.ROLE_ENFERMERIA));
        rrhh = token(fixture.usuario("po-rrh", RolNombre.ROLE_RRHH));
        gerencia = token(fixture.usuario("po-ger", RolNombre.ROLE_GERENCIA));
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    private String cuerpoDosis(UUID pacienteId) {
        return "{\"pacienteId\":\"%s\",\"vacunaId\":\"%s\",\"numeroDosis\":1,\"fechaAplicacion\":\"%s\",\"lote\":\"L-1\"}"
                .formatted(pacienteId, bcg.getId(), LocalDate.now());
    }

    @Test
    void sinTokenLosEndpointsPrivadosRetornan401() throws Exception {
        mockMvc.perform(get("/api/v1/vacunacion/esquema")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/vacunacion/mio")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/crecimiento/mio")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/odontologia/mio/odontograma")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/vacunacion/dosis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/odontologia/planes/por-cobrar")).andExpect(status().isUnauthorized());
    }

    @Test
    void losRolesAdministrativosNoVenDatosClinicosDeVacunacionNiCrecimiento() throws Exception {
        String ruta = "/api/v1/vacunacion/pacientes/" + pacienteEntidad.getId() + "/carne";
        String curvas = "/api/v1/crecimiento/pacientes/" + pacienteEntidad.getId();
        for (String token : new String[] {admin, recepcion, caja, rrhh, gerencia}) {
            mockMvc.perform(get(ruta).header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(get(curvas).header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/vacunacion/pacientes").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void elOdontogramaYLosPlanesSoloLosVenMedicoConRelacionYElPropioPaciente() throws Exception {
        String ruta = "/api/v1/odontologia/pacientes/" + pacienteEntidad.getId() + "/odontograma";
        for (String token :
                new String[] {admin, recepcion, caja, enfermeria, rrhh, gerencia, medicoSinRelacion, otroPaciente}) {
            mockMvc.perform(get(ruta).header("Authorization", token)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(ruta).header("Authorization", medico)).andExpect(status().isOk());
        mockMvc.perform(get(ruta).header("Authorization", paciente)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/odontologia/mio/odontograma").header("Authorization", paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteId", is(pacienteEntidad.getId().toString())));
        mockMvc.perform(get("/api/v1/odontologia/pacientes/" + pacienteEntidad.getId() + "/planes")
                        .header("Authorization", enfermeria))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloElMedicoEscribeEnElOdontogramaYLosPlanes() throws Exception {
        String cambio = "{\"pieza\":16,\"superficie\":\"O\",\"estado\":\"CARIES\"}";
        String ruta = "/api/v1/odontologia/pacientes/" + pacienteEntidad.getId() + "/odontograma/cambios";
        for (String token : new String[] {paciente, enfermeria, caja, admin, medicoSinRelacion}) {
            mockMvc.perform(post(ruta)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cambio))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post(ruta)
                        .header("Authorization", medico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cambio))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado", is("CARIES")));
        mockMvc.perform(post("/api/v1/odontologia/planes")
                        .header("Authorization", caja)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + pacienteEntidad.getId() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCobroDePlanesEsExclusivoDeCajaYElCatalogoDeAdministracion() throws Exception {
        for (String token : new String[] {medico, paciente, enfermeria, admin}) {
            mockMvc.perform(get("/api/v1/odontologia/planes/por-cobrar").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/odontologia/planes/por-cobrar").header("Authorization", caja))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/odontologia/procedimientos").header("Authorization", paciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/odontologia/procedimientos").header("Authorization", admin))
                .andExpect(status().isOk());
        String nuevo = "{\"codigo\":\"X1\",\"nombre\":\"X\",\"categoria\":\"C\",\"precio\":10,\"requierePieza\":false,"
                + "\"requiereSuperficie\":false,\"requiereConsentimiento\":false,\"activo\":true}";
        mockMvc.perform(post("/api/v1/odontologia/procedimientos")
                        .header("Authorization", medico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nuevo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/odontologia/procedimientos")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nuevo))
                .andExpect(status().isCreated());
    }

    @Test
    void soloElAdministradorCreaEsquemasDeVacunacion() throws Exception {
        String cuerpo = "{\"nombre\":\"Nuevo\",\"vigenteDesde\":\"2031-01-01\",\"porValidar\":true,\"dosis\":"
                + "[{\"vacunaCodigo\":\"BCG-S\",\"numeroDosis\":1,\"etiqueta\":\"Única\",\"edadMinimaDias\":0,\"edadRecomendadaDias\":0}]}";
        for (String token : new String[] {medico, enfermeria, paciente, caja}) {
            mockMvc.perform(post("/api/v1/vacunacion/esquemas")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/vacunacion/esquemas").header("Authorization", medico))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/vacunacion/esquemas")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());
    }

    @Test
    void registrarDosisYMedicionesNoEstaPermitidoAlPacienteNiAlMedicoSinRelacion() throws Exception {
        for (String token : new String[] {paciente, otroPaciente, medicoSinRelacion, caja, admin}) {
            mockMvc.perform(post("/api/v1/vacunacion/dosis")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoDosis(pacienteEntidad.getId())))
                    .andExpect(status().isForbidden());
        }
        Paciente bebe = pacientes.save(Paciente.builder()
                .dni("5" + String.format("%07d", ServiceTestFixture.siguiente()))
                .historialClinicoNumero("PO-HC-" + ServiceTestFixture.siguiente())
                .fechaNacimiento(LocalDate.now().minusDays(30))
                .sexo(com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente.F)
                .usuario(fixture.usuario("bebe", RolNombre.ROLE_PACIENTE))
                .build());
        fixture.cita(medicoEntidad, bebe, LocalDateTime.now().plusDays(1), EstadoCita.CONFIRMADA);
        mockMvc.perform(post("/api/v1/vacunacion/dosis")
                        .header("Authorization", medico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDosis(bebe.getId())))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/crecimiento/mediciones")
                        .header("Authorization", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + pacienteEntidad.getId() + "\",\"fecha\":\"" + LocalDate.now()
                                + "\",\"pesoKg\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCarneDeUnPacienteNoLoVeOtroPacienteNiUnMedicoAjeno() throws Exception {
        String ruta = "/api/v1/vacunacion/pacientes/" + pacienteEntidad.getId() + "/carne";

        mockMvc.perform(get(ruta).header("Authorization", otroPaciente)).andExpect(status().isForbidden());
        mockMvc.perform(get(ruta).header("Authorization", medicoSinRelacion)).andExpect(status().isForbidden());
        mockMvc.perform(get(ruta + "/pdf").header("Authorization", otroPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ruta).header("Authorization", medico)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/vacunacion/mio").header("Authorization", paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteId", is(pacienteEntidad.getId().toString())));
    }

    @Test
    void laEnfermeriaSoloVeLosPacientesConAtencionVigente() throws Exception {
        String ruta = "/api/v1/vacunacion/pacientes/" + pacienteEntidad.getId() + "/carne";

        mockMvc.perform(get(ruta).header("Authorization", enfermeria)).andExpect(status().isForbidden());
        fixture.cita(medicoEntidad, pacienteEntidad, LocalDateTime.now().plusHours(3), EstadoCita.CONFIRMADA);
        mockMvc.perform(get(ruta).header("Authorization", enfermeria)).andExpect(status().isOk());
    }

    @Test
    void elPdfDelCarneSeEntregaSinCacheYSeVerificaDePublicoSinDatosDelPaciente() throws Exception {
        String ruta = "/api/v1/vacunacion/pacientes/" + pacienteEntidad.getId() + "/carne/pdf";

        mockMvc.perform(get(ruta).header("Authorization", medico))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mockMvc.perform(get(ruta).header("Authorization", paciente)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/verificacion/AAAA-BBBB-CCCC-DDDD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("NO_ENCONTRADO")));
        mockMvc.perform(get("/api/v1/verificacion/AAAA-BBBB-CCCC-DDDD"))
                .andExpect(content().string(not(containsString(pacienteEntidad.getDni()))));
    }
}

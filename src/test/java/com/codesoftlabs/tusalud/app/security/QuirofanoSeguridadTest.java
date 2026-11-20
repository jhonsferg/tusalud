package com.codesoftlabs.tusalud.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class QuirofanoSeguridadTest {

    private static final String BASE = "/api/v1/quirofano";
    private static final String ESTERILIZACION = "/api/v1/esterilizacion";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private String medico;
    private String paciente;
    private String otroPaciente;
    private String admin;
    private String recepcion;
    private String caja;
    private String enfermeria;
    private String calidad;
    private String gerencia;
    private String rrhh;
    private String finanzas;
    private String laboratorio;
    private String farmacia;
    private String almacen;
    private Paciente pacienteEntidad;
    private String desde;
    private String hasta;

    @BeforeEach
    void setUp() {
        Medico medicoEntidad = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        pacienteEntidad = fixture.paciente();
        fixture.cita(medicoEntidad, pacienteEntidad, LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
        medico = token(medicoEntidad.getUsuario());
        paciente = token(pacienteEntidad.getUsuario());
        otroPaciente = token(fixture.paciente().getUsuario());
        admin = token(fixture.usuario("qs-adm", RolNombre.ROLE_ADMIN));
        recepcion = token(fixture.usuario("qs-rec", RolNombre.ROLE_RECEPCION));
        caja = token(fixture.usuario("qs-caj", RolNombre.ROLE_CAJA));
        enfermeria = token(fixture.usuario("qs-enf", RolNombre.ROLE_ENFERMERIA));
        calidad = token(fixture.usuario("qs-cal", RolNombre.ROLE_CALIDAD));
        gerencia = token(fixture.usuario("qs-ger", RolNombre.ROLE_GERENCIA));
        rrhh = token(fixture.usuario("qs-rrh", RolNombre.ROLE_RRHH));
        finanzas = token(fixture.usuario("qs-fin", RolNombre.ROLE_FINANZAS));
        laboratorio = token(fixture.usuario("qs-lab", RolNombre.ROLE_LABORATORIO));
        farmacia = token(fixture.usuario("qs-far", RolNombre.ROLE_FARMACIA));
        almacen = token(fixture.usuario("qs-alm", RolNombre.ROLE_ALMACEN));
        desde = LocalDateTime.now().minusDays(1).withNano(0).toString();
        hasta = LocalDateTime.now().plusDays(7).withNano(0).toString();
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    private int estado(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        MvcResult resultado = mockMvc.perform(request).andReturn();
        return resultado.getResponse().getStatus();
    }

    private void permitido(String token, String ruta) throws Exception {
        assertThat(estado(get(ruta).header("Authorization", token)))
                .as("%s con %s", ruta, token.substring(0, 12))
                .isEqualTo(200);
    }

    private void prohibido(String token, String ruta) throws Exception {
        mockMvc.perform(get(ruta).header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    void sinSesionNoSeAccedeANada() throws Exception {
        assertThat(estado(get(BASE + "/quirofanos"))).isIn(401, 403);
        assertThat(estado(get(ESTERILIZACION + "/lotes"))).isIn(401, 403);
    }

    @Test
    void laAgendaQuirurgicaSoloLaVenElMedicoYLaEnfermeria() throws Exception {
        String ruta = BASE + "/cirugias/agenda?desde=" + desde + "&hasta=" + hasta;
        permitido(medico, ruta);
        permitido(enfermeria, ruta);
        for (String token : new String[] {
            paciente, admin, recepcion, caja, calidad, gerencia, rrhh, finanzas, laboratorio, farmacia, almacen
        }) {
            prohibido(token, ruta);
        }
    }

    @Test
    void elCatalogoDeQuirofanosLoAdministraSoloAdministracion() throws Exception {
        permitido(medico, BASE + "/quirofanos");
        permitido(enfermeria, BASE + "/quirofanos");
        permitido(admin, BASE + "/quirofanos");
        for (String token : new String[] {paciente, recepcion, caja, calidad, gerencia, finanzas}) {
            prohibido(token, BASE + "/quirofanos");
        }
        String cuerpo = "{\"codigo\":\"QS-1\",\"nombre\":\"Sala\",\"estado\":\"DISPONIBLE\"}";
        for (String token : new String[] {medico, enfermeria, recepcion}) {
            mockMvc.perform(post(BASE + "/quirofanos")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post(BASE + "/quirofanos")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());
    }

    @Test
    void soloElMedicoSolicitaCirugiasYSoloElPacienteVeLasSuyas() throws Exception {
        String cuerpo =
                "{\"pacienteId\":\"%s\",\"diagnosticoCodigo\":\"%s\",\"procedimiento\":\"Apendicectomía\",\"tipo\":\"ELECTIVA\",\"tipoAnestesia\":\"GENERAL\"}"
                        .formatted(pacienteEntidad.getId(), "K80");
        for (String token : new String[] {paciente, enfermeria, admin, recepcion, caja}) {
            mockMvc.perform(post(BASE + "/cirugias")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }
        permitido(paciente, BASE + "/cirugias/mias");
        permitido(otroPaciente, BASE + "/cirugias/mias");
        for (String token : new String[] {medico, enfermeria, admin, recepcion, caja}) {
            prohibido(token, BASE + "/cirugias/mias");
        }
    }

    @Test
    void elPacienteNoAccedeAlDetalleNiAlHistorialDeUnaCirugia() throws Exception {
        UUID id = UUID.randomUUID();
        prohibido(paciente, BASE + "/cirugias/" + id);
        prohibido(paciente, BASE + "/cirugias/" + id + "/historial");
        prohibido(recepcion, BASE + "/cirugias/" + id);
        prohibido(caja, BASE + "/cirugias/" + id);
        prohibido(admin, BASE + "/cirugias/" + id);
    }

    @Test
    void lasAccionesClinicasDeLaCirugiaSeRestringenPorRol() throws Exception {
        UUID id = UUID.randomUUID();
        String nota = "{\"hallazgos\":\"a\",\"tecnica\":\"b\",\"diagnosticoPostoperatorio\":\"c\"}";
        String anestesia = "{\"asa\":1,\"inicio\":\"2030-01-01T10:00:00\",\"fin\":\"2030-01-01T11:00:00\"}";
        String lista = "{\"momento\":\"ANTES_INDUCCION\",\"items\":[]}";
        String programacion =
                "{\"quirofanoId\":\"%s\",\"inicio\":\"2030-01-01T10:00:00\",\"duracionMin\":60,\"equipo\":[{\"usuarioId\":\"%s\",\"rol\":\"CIRUJANO\"}]}"
                        .formatted(id, id);
        for (String accion : new String[] {"iniciar", "finalizar"}) {
            for (String token : new String[] {enfermeria, paciente, admin, recepcion, caja}) {
                mockMvc.perform(post(BASE + "/cirugias/" + id + "/" + accion).header("Authorization", token))
                        .andExpect(status().isForbidden());
            }
        }
        for (String token : new String[] {enfermeria, paciente, admin, recepcion, caja}) {
            mockMvc.perform(put(BASE + "/cirugias/" + id + "/nota-operatoria")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(nota))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(BASE + "/cirugias/" + id + "/registro-anestesico")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(anestesia))
                    .andExpect(status().isForbidden());
        }
        for (String token : new String[] {paciente, admin, recepcion, caja, calidad}) {
            mockMvc.perform(post(BASE + "/cirugias/" + id + "/checklist")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(lista))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(BASE + "/cirugias/" + id + "/programar")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(programacion))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void elDetalleDeUnaCirugiaInexistenteEs404ParaElPersonal() throws Exception {
        UUID id = UUID.randomUUID();
        assertThat(estado(get(BASE + "/cirugias/" + id).header("Authorization", medico)))
                .isEqualTo(404);
        assertThat(estado(get(BASE + "/cirugias/" + id).header("Authorization", enfermeria)))
                .isEqualTo(404);
    }

    @Test
    void laEsterilizacionSoloLaOperaEnfermeriaYLaConsultaCalidad() throws Exception {
        permitido(enfermeria, ESTERILIZACION + "/ciclos");
        permitido(calidad, ESTERILIZACION + "/ciclos");
        permitido(enfermeria, ESTERILIZACION + "/alertas");
        permitido(calidad, ESTERILIZACION + "/alertas");
        for (String token : new String[] {medico, paciente, admin, recepcion, caja, gerencia, rrhh, finanzas}) {
            prohibido(token, ESTERILIZACION + "/ciclos");
            prohibido(token, ESTERILIZACION + "/alertas");
        }
        for (String token : new String[] {medico, calidad, admin, recepcion, caja, paciente}) {
            mockMvc.perform(post(ESTERILIZACION + "/ciclos")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"esterilizadorId\":\"" + UUID.randomUUID()
                                    + "\",\"iniciadoEn\":\"2020-01-01T10:00:00\",\"temperaturaC\":134,\"duracionMin\":30,\"sets\":[{\"setId\":\""
                                    + UUID.randomUUID() + "\",\"cantidad\":1}]}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post(ESTERILIZACION + "/ciclos")
                        .header("Authorization", enfermeria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"esterilizadorId\":\"" + UUID.randomUUID()
                                + "\",\"iniciadoEn\":\"2020-01-01T10:00:00\",\"temperaturaC\":134,\"duracionMin\":30,\"sets\":[{\"setId\":\""
                                + UUID.randomUUID() + "\",\"cantidad\":1}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void losLotesLosVeElEquipoQuirurgicoYElCatalogoLoAdministraAdministracion() throws Exception {
        permitido(medico, ESTERILIZACION + "/lotes?soloUtilizables=true");
        permitido(enfermeria, ESTERILIZACION + "/lotes");
        permitido(calidad, ESTERILIZACION + "/lotes");
        for (String token : new String[] {paciente, recepcion, caja, admin, gerencia}) {
            prohibido(token, ESTERILIZACION + "/lotes");
        }
        permitido(admin, ESTERILIZACION + "/esterilizadores");
        permitido(medico, ESTERILIZACION + "/sets");
        String cuerpo = "{\"codigo\":\"S-1\",\"nombre\":\"Set\",\"vigenciaDias\":30}";
        for (String token : new String[] {medico, enfermeria, calidad, recepcion}) {
            mockMvc.perform(post(ESTERILIZACION + "/sets")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post(ESTERILIZACION + "/sets")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());
    }

    @Test
    void laTrazabilidadNoSeExponeAlPersonalMedicoNiAlPaciente() throws Exception {
        UUID id = UUID.randomUUID();
        for (String token : new String[] {medico, paciente, recepcion, caja, admin}) {
            prohibido(token, ESTERILIZACION + "/lotes/" + id + "/trazabilidad");
            prohibido(token, ESTERILIZACION + "/ciclos/" + id + "/trazabilidad");
        }
        assertThat(estado(get(ESTERILIZACION + "/lotes/" + id + "/trazabilidad").header("Authorization", calidad)))
                .isEqualTo(404);
    }
}

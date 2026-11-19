package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ModoFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ProveedorFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.repositories.ConfiguracionFiscalRepository;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import java.time.LocalDateTime;
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
class FacturacionPrivacidadSeguridadTest {

    private static final String CONFIGURACION = "{\"habilitada\":true,\"ruc\":\"20131312955\","
            + "\"razonSocial\":\"Clínica TuSalud SAC\",\"nombreComercial\":null,\"direccion\":\"Av. Salud 123\","
            + "\"ubigeo\":\"150101\",\"departamento\":\"Lima\",\"provincia\":\"Lima\",\"distrito\":\"Lima\","
            + "\"codigoLocal\":\"0000\",\"proveedor\":\"SIMULACION\",\"modo\":\"BETA\",\"urlServicio\":null,"
            + "\"afectacionServicios\":\"GRAVADO\",\"boletasPorResumen\":false,\"diasLimiteBaja\":7}";

    private static final String SOLICITUD =
            "{\"tipo\":\"ACCESO\",\"descripcion\":\"Quiero una copia de mis datos\",\"campo\":null,"
                    + "\"valorPropuesto\":null,\"finalidadId\":null}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private ConfiguracionFiscalRepository configuraciones;

    private Paciente pacienteEntidad;
    private String admin;
    private String finanzas;
    private String caja;
    private String gerencia;
    private String calidad;
    private String recepcion;
    private String medico;
    private String paciente;
    private String otroPaciente;

    @BeforeEach
    void preparar() {
        pacienteEntidad = fixture.paciente();
        admin = token(fixture.usuario("fp-adm", RolNombre.ROLE_ADMIN));
        finanzas = token(fixture.usuario("fp-fin", RolNombre.ROLE_FINANZAS));
        caja = token(fixture.usuario("fp-caj", RolNombre.ROLE_CAJA));
        gerencia = token(fixture.usuario("fp-ger", RolNombre.ROLE_GERENCIA));
        calidad = token(fixture.usuario("fp-cal", RolNombre.ROLE_CALIDAD));
        recepcion = token(fixture.usuario("fp-rec", RolNombre.ROLE_RECEPCION));
        medico = token(fixture.usuario("fp-med", RolNombre.ROLE_MEDICO));
        paciente = token(pacienteEntidad.getUsuario());
        otroPaciente = token(fixture.paciente().getUsuario());
        configuraciones.save(ConfiguracionFiscal.builder()
                .sede("PRINCIPAL")
                .habilitada(true)
                .ruc("20131312955")
                .razonSocial("Clínica TuSalud SAC")
                .direccion("Av. Salud 123")
                .ubigeo("150101")
                .departamento("Lima")
                .provincia("Lima")
                .distrito("Lima")
                .proveedor(ProveedorFiscal.SIMULACION)
                .modo(ModoFiscal.BETA)
                .actualizadoEn(LocalDateTime.now())
                .build());
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    @Test
    void sinTokenLosEndpointsFiscalesYDePrivacidadRetornan401MenosLaPoliticaPublica() throws Exception {
        mockMvc.perform(get("/api/v1/facturacion/estado")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/facturacion/documentos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/privacidad/consentimientos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/privacidad/gestion/solicitudes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/privacidad/politica")).andExpect(status().isNotFound());
    }

    @Test
    void laConfiguracionFiscalSoloLaVenAdministracionYFinanzas() throws Exception {
        for (String token : new String[] {caja, gerencia, recepcion, medico, paciente, calidad}) {
            mockMvc.perform(get("/api/v1/facturacion/configuracion").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/v1/facturacion/configuracion")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CONFIGURACION))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/facturacion/series").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
        for (String token : new String[] {admin, finanzas}) {
            mockMvc.perform(put("/api/v1/facturacion/configuracion")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CONFIGURACION))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ruc", is("20131312955")))
                    .andExpect(jsonPath("$.certificadoDePrueba", notNullValue()));
        }
    }

    @Test
    void elRucDelEmisorInvalidoSeRechazaConUnMensajeClaro() throws Exception {
        mockMvc.perform(put("/api/v1/facturacion/configuracion")
                        .header("Authorization", finanzas)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONFIGURACION.replace("20131312955", "20123456789")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laBandejaLaVenCajaFinanzasGerenciaYAdminPeroNoElPersonalClinicoNiElPaciente() throws Exception {
        for (String token : new String[] {admin, finanzas, caja, gerencia}) {
            mockMvc.perform(get("/api/v1/facturacion/documentos").header("Authorization", token))
                    .andExpect(status().isOk());
        }
        for (String token : new String[] {recepcion, medico, paciente, calidad}) {
            mockMvc.perform(get("/api/v1/facturacion/documentos").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void lasNotasLasBajasYLosArchivosFirmadosSonSoloDeAdministracionYFinanzas() throws Exception {
        String nota = "{\"documentoId\":\"00000000-0000-4000-8000-000000000001\",\"tipo\":\"NOTA_CREDITO\","
                + "\"motivoCodigo\":\"01\",\"descripcion\":\"Anulación de la operación\",\"items\":null}";
        for (String token : new String[] {caja, gerencia, recepcion, medico, paciente}) {
            mockMvc.perform(post("/api/v1/facturacion/notas")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(nota))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/facturacion/documentos/00000000-0000-4000-8000-000000000001/xml")
                            .header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/facturacion/documentos/00000000-0000-4000-8000-000000000001/cdr")
                            .header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/facturacion/documentos/00000000-0000-4000-8000-000000000001/baja")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"motivo\":\"Error en la emisión\"}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/v1/facturacion/notas")
                        .header("Authorization", finanzas)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nota))
                .andExpect(status().isNotFound());
    }

    @Test
    void elRegistroDeVentasEsDeFinanzasAdministracionYGerencia() throws Exception {
        for (String token : new String[] {admin, finanzas, gerencia}) {
            mockMvc.perform(get("/api/v1/facturacion/registro-ventas").header("Authorization", token))
                    .andExpect(status().isOk());
        }
        for (String token : new String[] {caja, recepcion, paciente, calidad}) {
            mockMvc.perform(get("/api/v1/facturacion/registro-ventas").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/facturacion/registro-ventas/excel").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void elPacienteSoloVeSusComprobantesYNoLaBandejaDeLaClinica() throws Exception {
        mockMvc.perform(get("/api/v1/facturacion/mis-comprobantes").header("Authorization", paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
        for (String token : new String[] {admin, finanzas, caja, medico}) {
            mockMvc.perform(get("/api/v1/facturacion/mis-comprobantes").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void laPrivacidadDelTitularEsSoloDelPacienteYLaGestionDeAdminYCalidad() throws Exception {
        for (String token : new String[] {admin, finanzas, caja, medico, recepcion}) {
            mockMvc.perform(get("/api/v1/privacidad/consentimientos").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/privacidad/solicitudes")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SOLICITUD))
                    .andExpect(status().isForbidden());
        }
        for (String token : new String[] {paciente, medico, finanzas, caja, recepcion, gerencia}) {
            mockMvc.perform(get("/api/v1/privacidad/gestion/solicitudes").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/privacidad/gestion/registro").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/privacidad/gestion/incidentes").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/privacidad/gestion/retencion/ejecutar")
                            .header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
        for (String token : new String[] {admin, calidad}) {
            mockMvc.perform(get("/api/v1/privacidad/gestion/solicitudes").header("Authorization", token))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/privacidad/gestion/registro").header("Authorization", token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void unaSolicitudSoloLaVeSuTitularYElPersonalDePrivacidad() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/privacidad/solicitudes")
                        .header("Authorization", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado", is("RECIBIDA")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.id");

        mockMvc.perform(get("/api/v1/privacidad/solicitudes/" + id).header("Authorization", otroPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/privacidad/solicitudes/" + id + "/exportacion")
                        .header("Authorization", otroPaciente))
                .andExpect(status().isForbidden());
        for (String token : new String[] {paciente, admin, calidad}) {
            mockMvc.perform(get("/api/v1/privacidad/solicitudes/" + id).header("Authorization", token))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/v1/privacidad/solicitudes/" + id).header("Authorization", medico))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/privacidad/solicitudes/mias").header("Authorization", otroPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void elPacienteNoPuedeResolverSuPropiaSolicitud() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/privacidad/solicitudes")
                        .header("Authorization", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.id");

        mockMvc.perform(post("/api/v1/privacidad/gestion/solicitudes/" + id + "/resolver")
                        .header("Authorization", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"ATENDER\",\"resolucion\":\"Me la apruebo a mí mismo\"}"))
                .andExpect(status().isForbidden());
    }
}

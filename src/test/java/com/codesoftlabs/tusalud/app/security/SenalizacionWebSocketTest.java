package com.codesoftlabs.tusalud.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.config.TelemedicinaProperties;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoTeleconsulta;
import com.codesoftlabs.tusalud.app.repositories.TeleconsultaRepository;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.codesoftlabs.tusalud.app.services.interfaces.TeleconsultaService;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ServiceTestFixture.class)
@TestPropertySource(properties = "app.telemedicina.max-mensajes-por-ventana=25")
class SenalizacionWebSocketTest {

    @LocalServerPort
    private int puerto;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private TeleconsultaService servicio;

    @Autowired
    private TeleconsultaRepository teleconsultaRepository;

    @Autowired
    private TelemedicinaProperties propiedades;

    private final StandardWebSocketClient cliente = new StandardWebSocketClient();
    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal principalMedico;
    private UsuarioPrincipal principalPaciente;
    private UUID teleconsultaId;

    private static final class Cliente extends TextWebSocketHandler {
        final BlockingQueue<String> mensajes = new LinkedBlockingQueue<>();
        final BlockingQueue<CloseStatus> cierres = new LinkedBlockingQueue<>();
        WebSocketSession sesion;

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            sesion = session;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            mensajes.add(message.getPayload());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            cierres.add(status);
        }

        void enviar(String json) throws Exception {
            sesion.sendMessage(new TextMessage(json));
        }

        String siguienteDeTipo(String tipo) throws Exception {
            for (int i = 0; i < 20; i++) {
                String m = mensajes.poll(5, TimeUnit.SECONDS);
                assertThat(m).as("mensaje de tipo " + tipo).isNotNull();
                if (m.contains("\"t\":\"" + tipo + "\"")) {
                    return m;
                }
            }
            throw new AssertionError("No llegó un mensaje de tipo " + tipo);
        }
    }

    @Autowired
    private PlatformTransactionManager transacciones;

    @BeforeEach
    void setUp() {
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
            paciente = fixture.paciente();
            principalMedico = fixture.principal(medico.getUsuario());
            principalPaciente = fixture.principal(paciente.getUsuario());
            Cita cita = fixture.cita(medico, paciente, LocalDateTime.now().plusMinutes(3), EstadoCita.CONFIRMADA);
            servicio.crearParaCita(cita);
            teleconsultaId = teleconsultaRepository
                    .findByCitaId(cita.getId())
                    .orElseThrow()
                    .getId();
        });
        servicio.aceptarConsentimiento(principalPaciente, propiedades.versionConsentimiento());
    }

    private Cliente conectar(UsuarioPrincipal usuario) throws Exception {
        String ticket = servicio.emitirTicket(teleconsultaId, usuario).ticket();
        return conectarConTicket(ticket);
    }

    private Cliente conectarConTicket(String ticket) throws Exception {
        Cliente handler = new Cliente();
        cliente.execute(
                        handler,
                        new WebSocketHttpHeaders(),
                        URI.create("ws://localhost:" + puerto + "/ws/teleconsulta?ticket=" + ticket))
                .get(5, TimeUnit.SECONDS);
        return handler;
    }

    @Test
    void elFlujoCompletoDeSalaDeEsperaAdmisionSenalesChatYFinalizacion() throws Exception {
        Cliente paciente = conectar(principalPaciente);
        assertThat(paciente.siguienteDeTipo("estado"))
                .contains("\"admitido\":false")
                .contains("\"medicoEnSala\":false");

        Cliente medico = conectar(principalMedico);
        assertThat(medico.siguienteDeTipo("estado")).contains("\"pacienteEnSala\":true");

        paciente.enviar("{\"t\":\"admitir\"}");
        assertThat(paciente.siguienteDeTipo("error")).contains("Solo el médico");

        medico.enviar("{\"t\":\"admitir\"}");
        assertThat(medico.siguienteDeTipo("estado")).contains("\"admitido\":true");
        assertThat(paciente.siguienteDeTipo("estado")).contains("\"admitido\":true");

        medico.enviar("{\"t\":\"offer\",\"sdp\":\"v=0 oferta\"}");
        assertThat(paciente.siguienteDeTipo("offer"))
                .contains("\"de\":\"MEDICO\"")
                .contains("v=0 oferta");
        paciente.enviar("{\"t\":\"answer\",\"sdp\":\"v=0 respuesta\"}");
        assertThat(medico.siguienteDeTipo("answer")).contains("v=0 respuesta");
        paciente.enviar("{\"t\":\"ice\",\"candidate\":{\"candidate\":\"candidate:1\",\"sdpMid\":\"0\"}}");
        assertThat(medico.siguienteDeTipo("ice")).contains("candidate:1");

        paciente.enviar("{\"t\":\"chat\",\"texto\":\"Buenos días doctor\"}");
        assertThat(medico.siguienteDeTipo("chat"))
                .contains("Buenos días doctor")
                .contains("\"de\":\"PACIENTE\"");

        paciente.enviar("{\"t\":\"calidad\",\"reconexiones\":2,\"candidato\":\"host\"}");
        paciente.enviar("{\"t\":\"finalizar\"}");
        assertThat(paciente.siguienteDeTipo("error")).contains("Solo el médico");

        medico.enviar("{\"t\":\"finalizar\"}");
        assertThat(medico.siguienteDeTipo("finalizada")).isNotNull();
        assertThat(paciente.siguienteDeTipo("finalizada")).isNotNull();

        var t = teleconsultaRepository.findById(teleconsultaId).orElseThrow();
        assertThat(t.getEstado()).isEqualTo(EstadoTeleconsulta.FINALIZADA);
        assertThat(t.getReconexiones()).isEqualTo(2);
        assertThat(t.getTipoCandidato()).isEqualTo("host");
        assertThat(t.getFinReal()).isNotNull();
    }

    @Test
    void lasSenalesNoPasanHastaQueElMedicoAdmite() throws Exception {
        Cliente paciente = conectar(principalPaciente);
        Cliente medico = conectar(principalMedico);
        paciente.siguienteDeTipo("estado");
        medico.siguienteDeTipo("estado");
        medico.enviar("{\"t\":\"offer\",\"sdp\":\"v=0\"}");
        assertThat(medico.siguienteDeTipo("error")).contains("aún no está lista");
        medico.enviar("{\"t\":\"chat\",\"texto\":\"hola\"}");
        assertThat(paciente.mensajes.poll(500, TimeUnit.MILLISECONDS)).doesNotContain("chat");
    }

    @Test
    void unTicketSoloSirveUnaVezYUnoInvalidoSeRechaza() throws Exception {
        String ticket = servicio.emitirTicket(teleconsultaId, principalPaciente).ticket();
        conectarConTicket(ticket);
        assertThatThrownBy(() -> conectarConTicket(ticket))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("401");
        assertThatThrownBy(() -> conectarConTicket("ticket-inventado")).isInstanceOf(ExecutionException.class);
        assertThatThrownBy(() -> conectarConTicket("")).isInstanceOf(ExecutionException.class);
    }

    @Test
    void unTerceroNoObtieneTicketNiEntra() {
        UsuarioPrincipal tercero = new TransactionTemplate(transacciones)
                .execute(estado -> fixture.principal(fixture.paciente().getUsuario()));
        assertThatThrownBy(() -> servicio.emitirTicket(teleconsultaId, tercero))
                .isInstanceOf(com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException.class);
    }

    @Test
    void unMensajeConTipoNoPermitidoOMalFormadoSeResponde() throws Exception {
        Cliente medico = conectar(principalMedico);
        medico.siguienteDeTipo("estado");
        medico.enviar("{\"t\":\"ejecutar\"}");
        assertThat(medico.siguienteDeTipo("error")).contains("no permitido");
        medico.enviar("no es json");
        assertThat(medico.siguienteDeTipo("error")).contains("inválido");
    }

    @Test
    void elExcesoDeMensajesCierraLaConexion() throws Exception {
        Cliente medico = conectar(principalMedico);
        medico.siguienteDeTipo("estado");
        for (int i = 0; i < 40; i++) {
            if (!medico.sesion.isOpen()) {
                break;
            }
            medico.enviar("{\"t\":\"ping\"}");
        }
        CloseStatus cierre = medico.cierres.poll(5, TimeUnit.SECONDS);
        assertThat(cierre).isNotNull();
        assertThat(cierre.getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
    }

    @Test
    void reingresarCierraLaSesionAnteriorDelMismoRol() throws Exception {
        Cliente primero = conectar(principalPaciente);
        primero.siguienteDeTipo("estado");
        Cliente segundo = conectar(principalPaciente);
        segundo.siguienteDeTipo("estado");
        assertThat(primero.siguienteDeTipo("reemplazada")).isNotNull();
        assertThat(primero.cierres.poll(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void unMensajeDemasiadoGrandeCierraLaConexion() throws Exception {
        Cliente medico = conectar(principalMedico);
        medico.siguienteDeTipo("estado");
        medico.enviar("{\"t\":\"chat\",\"texto\":\"" + "x".repeat(40_000) + "\"}");
        assertThat(medico.cierres.poll(5, TimeUnit.SECONDS)).isNotNull();
    }
}

package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CitaMostradorRequest;
import com.codesoftlabs.tusalud.app.dto.request.PacienteMostradorRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteMostradorResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteRegistradoResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AdmisionService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class AdmisionServiceTest {

    @Autowired
    private AdmisionService admisionService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
    }

    private static String dniUnico() {
        return String.format("8%07d", ServiceTestFixture.siguiente());
    }

    private PacienteMostradorRequest registro(String dni, String email) {
        return new PacienteMostradorRequest(
                "Lucía", "Paredes", dni, LocalDate.of(1992, 4, 15), "987654321", email, null);
    }

    private LocalDateTime proximoLunes(int hora, int minuto) {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(hora, minuto);
    }

    private CitaMostradorRequest cita(Paciente delPaciente, LocalDateTime fechaHora) {
        return new CitaMostradorRequest(
                delPaciente.getId(),
                medico.getId(),
                medico.getEspecialidades().iterator().next().getId(),
                fechaHora,
                "Control de mostrador");
    }

    @Test
    void registraUnPacienteConContrasenaTemporalYHistoriaClinica() {
        String dni = dniUnico();

        PacienteRegistradoResponse creado =
                admisionService.registrarPaciente(registro(dni, "Lucia.Paredes@Correo.com"));

        assertThat(creado.passwordTemporal())
                .hasSize(10)
                .matches(".*[A-Z].*")
                .matches(".*[a-z].*")
                .matches(".*\\d.*");
        assertThat(creado.paciente().dni()).isEqualTo(dni);
        assertThat(creado.paciente().email()).isEqualTo("lucia.paredes@correo.com");
        assertThat(creado.paciente().historialClinicoNumero()).startsWith("HC-");
        assertThat(creado.paciente().nombreCompleto()).isEqualTo("Lucía Paredes");
        Usuario usuario = usuarioRepository
                .findByEmailWithRoles("lucia.paredes@correo.com")
                .orElseThrow();
        assertThat(passwordEncoder.matches(creado.passwordTemporal(), usuario.getPassword()))
                .isTrue();
        assertThat(usuario.getRoles()).extracting(rol -> rol.getNombre()).containsExactly(RolNombre.ROLE_PACIENTE);
    }

    @Test
    void sinEmailGeneraUnoInternoConElDni() {
        String dni = dniUnico();

        PacienteRegistradoResponse creado = admisionService.registrarPaciente(registro(dni, null));

        assertThat(creado.paciente().email()).isEqualTo(dni + "@pacientes.tusalud.local");
    }

    @Test
    void rechazaDniYEmailRepetidos() {
        String dni = dniUnico();
        admisionService.registrarPaciente(registro(dni, "repetido.mostrador@correo.com"));

        assertThatThrownBy(() -> admisionService.registrarPaciente(registro(dni, "otro.correo@correo.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("DNI");
        assertThatThrownBy(
                        () -> admisionService.registrarPaciente(registro(dniUnico(), "repetido.mostrador@correo.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("email");
    }

    @Test
    void buscaPacientesPorDniYPorNombre() {
        Usuario usuario = paciente.getUsuario();
        usuario.setNombre("Zacarías");
        usuario.setApellido("Quiñonezmostrador");
        usuarioRepository.save(usuario);
        UsuarioPrincipal recepcion = principalDe(RolNombre.ROLE_RECEPCION);

        assertThat(admisionService.buscarPacientes(paciente.getDni(), recepcion))
                .extracting(PacienteMostradorResponse::pacienteId)
                .contains(paciente.getId());
        assertThat(admisionService.buscarPacientes("quiñonezMOSTRADOR", recepcion))
                .extracting(PacienteMostradorResponse::pacienteId)
                .containsExactly(paciente.getId());
        assertThat(admisionService.buscarPacientes("zzz-inexistente-zzz", recepcion))
                .isEmpty();
    }

    @Test
    void laBusquedaConMenosDeDosCaracteresNoDevuelveNada() {
        for (int i = 0; i < 32; i++) {
            fixture.paciente();
        }
        UsuarioPrincipal recepcion = principalDe(RolNombre.ROLE_RECEPCION);

        assertThat(admisionService.buscarPacientes(null, recepcion)).isEmpty();
        assertThat(admisionService.buscarPacientes("  ", recepcion)).isEmpty();
        assertThat(admisionService.buscarPacientes("9", recepcion)).isEmpty();
        assertThat(admisionService.buscarPacientes("90", recepcion)).hasSize(30);
    }

    @Test
    void recepcionVeLosDatosDeContactoYElRestoDeRolesSoloNombreDniYTelefono() {
        paciente.getUsuario().setTelefono("987654321");
        usuarioRepository.save(paciente.getUsuario());

        PacienteMostradorResponse completo = admisionService
                .buscarPacientes(paciente.getDni(), principalDe(RolNombre.ROLE_RECEPCION))
                .get(0);
        assertThat(completo.email()).isNotNull();
        assertThat(completo.fechaNacimiento()).isNotNull();
        assertThat(completo.historialClinicoNumero()).isNotNull();

        for (RolNombre rol : List.of(RolNombre.ROLE_CAJA, RolNombre.ROLE_FARMACIA, RolNombre.ROLE_ENFERMERIA)) {
            PacienteMostradorResponse minimo = admisionService
                    .buscarPacientes(paciente.getDni(), principalDe(rol))
                    .get(0);
            assertThat(minimo.pacienteId()).isEqualTo(paciente.getId());
            assertThat(minimo.nombreCompleto()).isNotBlank();
            assertThat(minimo.dni()).isEqualTo(paciente.getDni());
            assertThat(minimo.telefono()).isEqualTo("987654321");
            assertThat(minimo.email()).isNull();
            assertThat(minimo.fechaNacimiento()).isNull();
            assertThat(minimo.historialClinicoNumero()).isNull();
        }
    }

    @Test
    void elMedicoSoloEncuentraASusPacientes() {
        Paciente propio = fixture.paciente();
        fixture.cita(medico, propio, proximoLunes(10, 0), EstadoCita.PENDIENTE);
        UsuarioPrincipal principalMedico = fixture.principal(medico.getUsuario());

        assertThat(admisionService.buscarPacientes(propio.getDni(), principalMedico))
                .singleElement()
                .satisfies(encontrado -> {
                    assertThat(encontrado.pacienteId()).isEqualTo(propio.getId());
                    assertThat(encontrado.email()).isNull();
                    assertThat(encontrado.fechaNacimiento()).isNull();
                });
        assertThat(admisionService.buscarPacientes(paciente.getDni(), principalMedico))
                .isEmpty();
    }

    private UsuarioPrincipal principalDe(RolNombre rol) {
        return fixture.principal(fixture.usuario("rol", rol));
    }

    @Test
    void reservaUnaCitaANombreDelPacienteYLeAvisa() {
        CitaResponse reservada = admisionService.reservarCita(cita(paciente, proximoLunes(9, 0)));

        assertThat(reservada.estado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(reservada.pacienteId()).isEqualTo(paciente.getId());
        assertThat(reservada.medicoId()).isEqualTo(medico.getId());
        assertThat(reservada.precio()).isEqualByComparingTo("100.00");
        List<NotificacionResponse> avisos =
                notificacionService.listar(fixture.principal(paciente.getUsuario()), false, 10);
        assertThat(avisos).extracting(NotificacionResponse::tipo).contains(TipoNotificacion.CITA_CREADA);
    }

    @Test
    void laCitaDeMostradorRespetaLosConflictosDeHorario() {
        LocalDateTime lunes = proximoLunes(10, 0);
        admisionService.reservarCita(cita(paciente, lunes));

        assertThatThrownBy(() -> admisionService.reservarCita(cita(fixture.paciente(), lunes)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya tiene una cita");

        Medico otro = fixture.medicoConHorarioLunes(new BigDecimal("90.00"));
        CitaMostradorRequest cruzada = new CitaMostradorRequest(
                paciente.getId(),
                otro.getId(),
                otro.getEspecialidades().iterator().next().getId(),
                lunes,
                "Cruce");
        assertThatThrownBy(() -> admisionService.reservarCita(cruzada)).isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> admisionService.reservarCita(cita(paciente, proximoLunes(15, 0))))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void reservarParaUnPacienteInexistenteResponde404() {
        CitaMostradorRequest solicitud = new CitaMostradorRequest(
                UUID.randomUUID(),
                medico.getId(),
                medico.getEspecialidades().iterator().next().getId(),
                proximoLunes(9, 30),
                "Control");

        assertThatThrownBy(() -> admisionService.reservarCita(solicitud)).isInstanceOf(ResourceNotFoundException.class);
    }
}

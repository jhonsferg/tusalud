package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.PasswordChangeRequest;
import com.codesoftlabs.tusalud.app.dto.request.PerfilUpdateRequest;
import com.codesoftlabs.tusalud.app.dto.response.PerfilResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.PerfilService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class PerfilServiceImplTest {

    @Autowired
    private PerfilService perfilService;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void obtenerDevuelveLosDatosDelPaciente() {
        Paciente paciente = fixture.paciente();

        PerfilResponse perfil = perfilService.obtener(fixture.principal(paciente.getUsuario()));

        assertThat(perfil.email()).isEqualTo(paciente.getUsuario().getEmail());
        assertThat(perfil.dni()).isEqualTo(paciente.getDni());
        assertThat(perfil.historialClinicoNumero()).isEqualTo(paciente.getHistorialClinicoNumero());
        assertThat(perfil.medicoId()).isNull();
    }

    @Test
    void obtenerDevuelveLosDatosDelMedico() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("120.00"));

        PerfilResponse perfil = perfilService.obtener(fixture.principal(medico.getUsuario()));

        assertThat(perfil.colegiatura()).isEqualTo(medico.getColegiatura());
        assertThat(perfil.especialidad()).isEqualTo(medico.getEspecialidad().getNombre());
        assertThat(perfil.precioConsulta()).isEqualByComparingTo("120.00");
        assertThat(perfil.dni()).isNull();
    }

    @Test
    void actualizarCambiaLosDatosDelPaciente() {
        Paciente paciente = fixture.paciente();

        PerfilResponse perfil = perfilService.actualizar(
                fixture.principal(paciente.getUsuario()),
                new PerfilUpdateRequest("  Ana ", "Ríos", "912345678", LocalDate.of(1990, 1, 2), "ignorado"));

        assertThat(perfil.nombre()).isEqualTo("Ana");
        assertThat(perfil.apellido()).isEqualTo("Ríos");
        assertThat(perfil.telefono()).isEqualTo("912345678");
        assertThat(perfil.fechaNacimiento()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(perfil.consultorio()).isNull();
    }

    @Test
    void actualizarCambiaElConsultorioDelMedicoYLimpiaElTelefonoVacio() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));

        PerfilResponse perfil = perfilService.actualizar(
                fixture.principal(medico.getUsuario()),
                new PerfilUpdateRequest("Luis", "García", " ", null, "Consultorio 999"));

        assertThat(perfil.consultorio()).isEqualTo("Consultorio 999");
        assertThat(perfil.telefono()).isNull();
    }

    @Test
    void cambiarPasswordGuardaLaNuevaContrasenaCifrada() {
        Usuario usuario = fixture.usuario("pwd", RolNombre.ROLE_PACIENTE);

        perfilService.cambiarPassword(
                fixture.principal(usuario), new PasswordChangeRequest(ServiceTestFixture.PASSWORD, "NuevaClave456!"));

        Usuario guardado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NuevaClave456!", guardado.getPassword()))
                .isTrue();
    }

    @Test
    void cambiarPasswordConActualIncorrectaLanzaError() {
        Usuario usuario = fixture.usuario("pwd2", RolNombre.ROLE_PACIENTE);

        assertThatThrownBy(() -> perfilService.cambiarPassword(
                        fixture.principal(usuario), new PasswordChangeRequest("Incorrecta123!", "NuevaClave456!")))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("contraseña actual");
    }

    @Test
    void cambiarPasswordConLaMismaContrasenaLanzaError() {
        Usuario usuario = fixture.usuario("pwd3", RolNombre.ROLE_PACIENTE);

        assertThatThrownBy(() -> perfilService.cambiarPassword(
                        fixture.principal(usuario),
                        new PasswordChangeRequest(ServiceTestFixture.PASSWORD, ServiceTestFixture.PASSWORD)))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("distinta");
    }
}

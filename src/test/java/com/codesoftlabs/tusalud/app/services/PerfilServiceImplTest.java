package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.PasswordChangeRequest;
import com.codesoftlabs.tusalud.app.dto.request.PerfilUpdateRequest;
import com.codesoftlabs.tusalud.app.dto.request.PreferenciasRequest;
import com.codesoftlabs.tusalud.app.dto.response.PerfilResponse;
import com.codesoftlabs.tusalud.app.dto.response.PreferenciasResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.repositories.ArchivoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.PerfilService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;
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

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private ArchivoRepository archivoRepository;

    private static final byte[] PNG = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

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
        assertThat(perfil.especialidad())
                .isEqualTo(medico.getEspecialidades().iterator().next().getNombre());
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

    @Test
    void cambiarPasswordNotificaContrasenaCambiada() {
        Usuario usuario = fixture.usuario("pwd4", RolNombre.ROLE_PACIENTE);

        perfilService.cambiarPassword(
                fixture.principal(usuario), new PasswordChangeRequest(ServiceTestFixture.PASSWORD, "NuevaClave789!"));

        assertThat(notificacionService.listar(fixture.principal(usuario), false, 10))
                .singleElement()
                .satisfies(n -> assertThat(n.tipo()).isEqualTo(TipoNotificacion.CONTRASENA_CAMBIADA));
    }

    @Test
    void actualizarAvatarGuardaLaImagenEnElAlmacen() {
        Usuario usuario = fixture.usuario("avatar", RolNombre.ROLE_PACIENTE);

        PerfilResponse perfil = perfilService.actualizarAvatar(fixture.principal(usuario), "foto.png", PNG);

        assertThat(perfil.avatarId()).isNotNull();
        assertThat(perfilService.obtener(fixture.principal(usuario)).avatarId()).isEqualTo(perfil.avatarId());
    }

    @Test
    void actualizarAvatarReemplazaYQuitaLaFotoAnterior() {
        Usuario usuario = fixture.usuario("avatar5", RolNombre.ROLE_PACIENTE);
        UUID primero = perfilService
                .actualizarAvatar(fixture.principal(usuario), "uno.png", PNG)
                .avatarId();

        UUID segundo = perfilService
                .actualizarAvatar(fixture.principal(usuario), "dos.png", PNG)
                .avatarId();

        assertThat(segundo).isNotEqualTo(primero);
        assertThat(archivoRepository.findActivo(primero)).isEmpty();
        assertThat(archivoRepository.findActivo(segundo)).isPresent();
    }

    @Test
    void actualizarAvatarRechazaTiposNoPermitidos() {
        Usuario usuario = fixture.usuario("avatar2", RolNombre.ROLE_PACIENTE);

        assertThatThrownBy(() -> perfilService.actualizarAvatar(
                        fixture.principal(usuario), "foto.gif", "GIF89a......".getBytes()))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("no está permitido");
        assertThatThrownBy(() -> perfilService.actualizarAvatar(fixture.principal(usuario), "foto.jpg", PNG))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("no coincide con su extensión");
        assertThatThrownBy(() -> perfilService.actualizarAvatar(
                        fixture.principal(usuario), "foto.png", "<svg xmlns='x'/>".getBytes()))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void actualizarAvatarRechazaImagenMayorA2Mb() {
        Usuario usuario = fixture.usuario("avatar3", RolNombre.ROLE_PACIENTE);
        byte[] grande = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, grande, 0, 8);

        assertThatThrownBy(() -> perfilService.actualizarAvatar(fixture.principal(usuario), "grande.png", grande))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("2 MB");
    }

    @Test
    void eliminarAvatarQuitaLaImagen() {
        Usuario usuario = fixture.usuario("avatar4", RolNombre.ROLE_PACIENTE);
        UUID id = perfilService
                .actualizarAvatar(fixture.principal(usuario), "foto.png", PNG)
                .avatarId();

        perfilService.eliminarAvatar(fixture.principal(usuario));

        assertThat(perfilService.obtener(fixture.principal(usuario)).avatarId()).isNull();
        assertThat(archivoRepository.findActivo(id)).isEmpty();
    }

    @Test
    void preferenciasSinGuardarDevuelvenValoresPorDefecto() {
        Usuario usuario = fixture.usuario("pref", RolNombre.ROLE_PACIENTE);

        PreferenciasResponse preferencias = perfilService.obtenerPreferencias(fixture.principal(usuario));

        assertThat(preferencias.recordatoriosActivos()).isTrue();
        assertThat(preferencias.minutosAntelacionRecordatorio()).isEqualTo(1440);
        assertThat(preferencias.alertasInicioSesion()).isTrue();
        assertThat(preferencias.tourCompletado()).isFalse();
    }

    @Test
    void actualizarPreferenciasGuardaLosValores() {
        Usuario usuario = fixture.usuario("pref2", RolNombre.ROLE_MEDICO);

        perfilService.actualizarPreferencias(
                fixture.principal(usuario), new PreferenciasRequest(false, 2880, false, true));
        PreferenciasResponse guardadas = perfilService.obtenerPreferencias(fixture.principal(usuario));

        assertThat(guardadas).isEqualTo(new PreferenciasResponse(false, 2880, false, true));
    }
}

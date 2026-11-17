package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.response.EstadisticasResponse;
import com.codesoftlabs.tusalud.app.dto.response.UsuarioAdminResponse;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.EstadisticasService;
import com.codesoftlabs.tusalud.app.services.interfaces.UsuarioAdminService;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class AdminServiciosTest {

    @Autowired
    private UsuarioAdminService usuarioAdminService;

    @Autowired
    private EstadisticasService estadisticasService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ServiceTestFixture fixture;

    @Test
    void listaUsuariosFiltrandoPorRolYBusqueda() {
        Usuario medico = fixture.usuario("adminlist", RolNombre.ROLE_MEDICO);
        Usuario paciente = fixture.usuario("adminlist", RolNombre.ROLE_PACIENTE);

        assertThat(usuarioAdminService.listar(RolNombre.ROLE_MEDICO, medico.getEmail()))
                .extracting(UsuarioAdminResponse::id)
                .containsExactly(medico.getId());
        assertThat(usuarioAdminService.listar(null, paciente.getEmail().toUpperCase()))
                .extracting(UsuarioAdminResponse::id)
                .containsExactly(paciente.getId());
        assertThat(usuarioAdminService.listar(RolNombre.ROLE_ADMIN, medico.getEmail()))
                .isEmpty();
    }

    @Test
    void desactivaYReactivaUnaCuenta() {
        Usuario admin = fixture.usuario("admin", RolNombre.ROLE_ADMIN);
        Usuario paciente = fixture.usuario("desactivar", RolNombre.ROLE_PACIENTE);

        UsuarioAdminResponse inactivo =
                usuarioAdminService.cambiarEstado(paciente.getId(), EstadoUsuario.INACTIVO, fixture.principal(admin));
        assertThat(inactivo.estado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(usuarioRepository.findById(paciente.getId()).orElseThrow().isActivo())
                .isFalse();

        UsuarioAdminResponse activo =
                usuarioAdminService.cambiarEstado(paciente.getId(), EstadoUsuario.ACTIVO, fixture.principal(admin));
        assertThat(activo.estado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void unAdministradorNoPuedeDesactivarseNiSeAceptanIdsInexistentes() {
        Usuario admin = fixture.usuario("admin", RolNombre.ROLE_ADMIN);

        assertThatThrownBy(() -> usuarioAdminService.cambiarEstado(
                        admin.getId(), EstadoUsuario.INACTIVO, fixture.principal(admin)))
                .isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(
                        () -> usuarioAdminService.cambiarEstado(-1L, EstadoUsuario.INACTIVO, fixture.principal(admin)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void lasEstadisticasIncluyenSeisMesesYTotalesCoherentes() {
        fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        fixture.paciente();

        EstadisticasResponse estadisticas = estadisticasService.obtener();

        assertThat(estadisticas.totalMedicos()).isPositive();
        assertThat(estadisticas.totalPacientes()).isPositive();
        assertThat(estadisticas.ingresosPorMes()).hasSize(6);
        assertThat(estadisticas.ingresosPorMes().get(5).mes())
                .isEqualTo(YearMonth.now().toString());
        assertThat(estadisticas.ingresosTotales()).isNotNull();
        assertThat(estadisticas.topMedicos()).hasSizeLessThanOrEqualTo(5);
    }
}

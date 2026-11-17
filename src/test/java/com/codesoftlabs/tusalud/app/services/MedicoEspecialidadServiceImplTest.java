package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.EspecialidadRequest;
import com.codesoftlabs.tusalud.app.dto.request.HorarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.MedicoCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.MedicoEspecialidadesRequest;
import com.codesoftlabs.tusalud.app.dto.response.EspecialidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.HorarioResponse;
import com.codesoftlabs.tusalud.app.dto.response.MedicoResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.services.interfaces.EspecialidadService;
import com.codesoftlabs.tusalud.app.services.interfaces.MedicoService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
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
class MedicoEspecialidadServiceImplTest {

    @Autowired
    private EspecialidadService especialidadService;

    @Autowired
    private MedicoService medicoService;

    @Autowired
    private ServiceTestFixture fixture;

    @Test
    void especialidadCrudConValidaciones() {
        String nombre = "SvcTest Neurologia " + ServiceTestFixture.siguiente();
        EspecialidadResponse creada = especialidadService.crear(
                new EspecialidadRequest("  " + nombre + " ", "Desc", new BigDecimal("95.50")));

        assertThat(creada.nombre()).isEqualTo(nombre);
        assertThat(creada.precioConsulta()).isEqualByComparingTo("95.50");
        assertThatThrownBy(() ->
                        especialidadService.crear(new EspecialidadRequest(nombre.toUpperCase(), null, BigDecimal.TEN)))
                .isInstanceOf(ConflictException.class);
        EspecialidadResponse actualizada = especialidadService.actualizar(
                creada.id(), new EspecialidadRequest(nombre, "Nueva", new BigDecimal("110.00")));
        assertThat(actualizada.descripcion()).isEqualTo("Nueva");
        assertThat(actualizada.precioConsulta()).isEqualByComparingTo("110.00");

        especialidadService.eliminar(creada.id());
        assertThatThrownBy(() -> especialidadService.obtener(creada.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void noSeEliminaEspecialidadConMedicos() {
        Medico medico = fixture.medicoConHorarioLunes(BigDecimal.TEN);

        assertThatThrownBy(() -> especialidadService.eliminar(
                        medico.getEspecialidades().iterator().next().getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void crearMedicoYValidarDuplicados() {
        UUID especialidadId = fixture.especialidad().getId();
        int n = ServiceTestFixture.siguiente();
        MedicoCreateRequest request = new MedicoCreateRequest(
                "Ana",
                "Torres",
                "svc.test.ana" + n + "@test.com",
                "Medico123!",
                "912345678",
                "SVC-CMP-" + n,
                "204",
                List.of(especialidadId));

        MedicoResponse medico = medicoService.crear(request);

        assertThat(medico.id()).isNotNull();
        assertThat(medico.especialidades()).extracting(EspecialidadResponse::id).containsExactly(especialidadId);
        assertThat(medicoService.listar(especialidadId))
                .extracting(MedicoResponse::id)
                .containsExactly(medico.id());
        assertThatThrownBy(() -> medicoService.crear(request)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> medicoService.listar(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarEspecialidadesReemplazaLasDelMedico() {
        Medico medico = fixture.medicoConHorarioLunes(BigDecimal.TEN);
        UUID nueva = fixture.especialidad().getId();
        UUID otra = fixture.especialidad().getId();

        MedicoResponse actualizado = medicoService.actualizarEspecialidades(
                medico.getId(), new MedicoEspecialidadesRequest(List.of(nueva, otra, nueva)));

        assertThat(actualizado.especialidades())
                .extracting(EspecialidadResponse::id)
                .containsExactlyInAnyOrder(nueva, otra);
        assertThat(medicoService.listar(nueva)).extracting(MedicoResponse::id).contains(medico.getId());
        assertThatThrownBy(() -> medicoService.actualizarEspecialidades(
                        medico.getId(), new MedicoEspecialidadesRequest(List.of(UUID.randomUUID()))))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> medicoService.actualizarEspecialidades(
                        UUID.randomUUID(), new MedicoEspecialidadesRequest(List.of(nueva))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void agregarYEliminarHorarioConValidaciones() {
        Medico medico = fixture.medicoConHorarioLunes(BigDecimal.TEN);
        UUID id = medico.getId();

        assertThatThrownBy(() -> medicoService.agregarHorario(
                        id, new HorarioRequest(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(9, 0), 30)))
                .isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(() -> medicoService.agregarHorario(
                        id, new HorarioRequest(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(10, 20), 30)))
                .isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(() -> medicoService.agregarHorario(
                        id, new HorarioRequest(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0), 30)))
                .isInstanceOf(ConflictException.class);

        HorarioResponse horario = medicoService.agregarHorario(
                id, new HorarioRequest(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0), 20));
        assertThat(medicoService.listarHorarios(id)).hasSize(2);

        assertThatThrownBy(() -> medicoService.eliminarHorario(
                        fixture.medicoConHorarioLunes(BigDecimal.ONE).getId(), horario.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        medicoService.eliminarHorario(id, horario.id());
        assertThat(medicoService.listarHorarios(id)).hasSize(1);
    }
}

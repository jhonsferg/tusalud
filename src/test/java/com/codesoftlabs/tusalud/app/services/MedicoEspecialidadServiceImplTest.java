package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.EspecialidadRequest;
import com.codesoftlabs.tusalud.app.dto.request.HorarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.MedicoCreateRequest;
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
        EspecialidadResponse creada = especialidadService.crear(new EspecialidadRequest("  " + nombre + " ", "Desc"));

        assertThat(creada.nombre()).isEqualTo(nombre);
        assertThatThrownBy(() -> especialidadService.crear(new EspecialidadRequest(nombre.toUpperCase(), null)))
                .isInstanceOf(ConflictException.class);
        assertThat(especialidadService
                        .actualizar(creada.id(), new EspecialidadRequest(nombre, "Nueva"))
                        .descripcion())
                .isEqualTo("Nueva");

        especialidadService.eliminar(creada.id());
        assertThatThrownBy(() -> especialidadService.obtener(creada.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void noSeEliminaEspecialidadConMedicos() {
        Medico medico = fixture.medicoConHorarioLunes(BigDecimal.TEN);

        assertThatThrownBy(() ->
                        especialidadService.eliminar(medico.getEspecialidad().getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void crearMedicoYValidarDuplicados() {
        Long especialidadId = fixture.especialidad().getId();
        int n = ServiceTestFixture.siguiente();
        MedicoCreateRequest request = new MedicoCreateRequest(
                "Ana",
                "Torres",
                "svc.test.ana" + n + "@test.com",
                "Medico123!",
                "912345678",
                "SVC-CMP-" + n,
                "204",
                new BigDecimal("120.00"),
                especialidadId);

        MedicoResponse medico = medicoService.crear(request);

        assertThat(medico.id()).isNotNull();
        assertThat(medico.especialidadId()).isEqualTo(especialidadId);
        assertThat(medicoService.listar(especialidadId))
                .extracting(MedicoResponse::id)
                .containsExactly(medico.id());
        assertThatThrownBy(() -> medicoService.crear(request)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> medicoService.listar(-1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void agregarYEliminarHorarioConValidaciones() {
        Medico medico = fixture.medicoConHorarioLunes(BigDecimal.TEN);
        Long id = medico.getId();

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

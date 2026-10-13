package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.EspecialidadRequest;
import com.codesoftlabs.tusalud.app.dto.response.EspecialidadResponse;
import com.codesoftlabs.tusalud.app.entities.Especialidad;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.EspecialidadRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.EspecialidadService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EspecialidadServiceImpl implements EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    @Override
    public List<EspecialidadResponse> listar() {
        return especialidadRepository.findAllOrdenadas().stream()
                .map(EspecialidadResponse::from)
                .toList();
    }

    @Override
    public EspecialidadResponse obtener(Long id) {
        return EspecialidadResponse.from(buscar(id));
    }

    @Override
    @Transactional
    public EspecialidadResponse crear(EspecialidadRequest request) {
        String nombre = request.nombre().trim();
        if (especialidadRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe una especialidad con el nombre " + nombre);
        }
        Especialidad especialidad = Especialidad.builder()
                .nombre(nombre)
                .descripcion(limpiar(request.descripcion()))
                .build();
        especialidad = especialidadRepository.save(especialidad);
        log.info("Especialidad creada: {} (id {})", nombre, especialidad.getId());
        return EspecialidadResponse.from(especialidad);
    }

    @Override
    @Transactional
    public EspecialidadResponse actualizar(Long id, EspecialidadRequest request) {
        Especialidad especialidad = buscar(id);
        String nombre = request.nombre().trim();
        if (especialidadRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictException("Ya existe una especialidad con el nombre " + nombre);
        }
        especialidad.setNombre(nombre);
        especialidad.setDescripcion(limpiar(request.descripcion()));
        log.info("Especialidad actualizada: id {}", id);
        return EspecialidadResponse.from(especialidad);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Especialidad especialidad = buscar(id);
        if (especialidadRepository.tieneMedicosAsignados(id)) {
            throw new ConflictException("No se puede eliminar la especialidad " + especialidad.getNombre()
                    + " porque tiene médicos asignados");
        }
        especialidadRepository.delete(especialidad);
        log.info("Especialidad eliminada: id {}", id);
    }

    private Especialidad buscar(Long id) {
        return especialidadRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad", "id", id));
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}

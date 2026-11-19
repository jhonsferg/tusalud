package com.codesoftlabs.tusalud.app.services.documentosmedicos;

import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.EstablecimientoRequest;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.EstablecimientoResponse;
import com.codesoftlabs.tusalud.app.entities.EstablecimientoSalud;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.EstablecimientoSaludRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstablecimientoSaludService {

    private final EstablecimientoSaludRepository repositorio;

    public List<EstablecimientoResponse> listar(boolean soloActivos) {
        return (soloActivos ? repositorio.findByActivoTrueOrderByNombreAsc() : repositorio.findAllByOrderByNombreAsc())
                .stream().map(DocumentosMedicosMapper::establecimiento).toList();
    }

    @Transactional
    public EstablecimientoResponse crear(EstablecimientoRequest request) {
        String ipress = limpiar(request.codigoIpress());
        if (ipress != null && repositorio.existsByCodigoIpress(ipress)) {
            throw new ConflictException("Ya existe un establecimiento con el código IPRESS " + ipress);
        }
        EstablecimientoSalud nuevo = repositorio.save(EstablecimientoSalud.builder()
                .codigoIpress(ipress)
                .nombre(request.nombre().strip())
                .nivel(request.nivel())
                .distrito(limpiar(request.distrito()))
                .contacto(limpiar(request.contacto()))
                .activo(request.activo())
                .build());
        return DocumentosMedicosMapper.establecimiento(nuevo);
    }

    @Transactional
    public EstablecimientoResponse actualizar(UUID id, EstablecimientoRequest request) {
        EstablecimientoSalud existente =
                repositorio.findById(id).orElseThrow(() -> new ResourceNotFoundException("Establecimiento", "id", id));
        String ipress = limpiar(request.codigoIpress());
        if (ipress != null && !ipress.equals(existente.getCodigoIpress()) && repositorio.existsByCodigoIpress(ipress)) {
            throw new ConflictException("Ya existe un establecimiento con el código IPRESS " + ipress);
        }
        existente.setCodigoIpress(ipress);
        existente.setNombre(request.nombre().strip());
        existente.setNivel(request.nivel());
        existente.setDistrito(limpiar(request.distrito()));
        existente.setContacto(limpiar(request.contacto()));
        existente.setActivo(request.activo());
        return DocumentosMedicosMapper.establecimiento(existente);
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}

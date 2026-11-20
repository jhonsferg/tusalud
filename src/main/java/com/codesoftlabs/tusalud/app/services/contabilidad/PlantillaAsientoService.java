package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.CuentaLineaPlantillaRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.PlantillaAsientoRequest;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.PlantillaAsientoResponse;
import com.codesoftlabs.tusalud.app.entities.CuentaContable;
import com.codesoftlabs.tusalud.app.entities.PlantillaAsiento;
import com.codesoftlabs.tusalud.app.entities.PlantillaAsientoLinea;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CuentaContableRepository;
import com.codesoftlabs.tusalud.app.repositories.PlantillaAsientoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlantillaAsientoService {

    private static final String ENTIDAD = "PLANTILLA";

    private final PlantillaAsientoRepository plantillas;
    private final CuentaContableRepository cuentas;
    private final UsuarioRepository usuarios;
    private final ContabilidadAuditor auditor;

    public List<PlantillaAsientoResponse> listar() {
        return plantillas.todasConLineas().stream()
                .map(ContabilidadMapper::plantilla)
                .toList();
    }

    @Transactional
    public PlantillaAsientoResponse actualizar(UsuarioPrincipal actor, UUID id, PlantillaAsientoRequest request) {
        PlantillaAsiento plantilla =
                plantillas.conLineas(id).orElseThrow(() -> new ResourceNotFoundException("Plantilla", "id", id));
        for (CuentaLineaPlantillaRequest cambio : request.cuentas()) {
            PlantillaAsientoLinea linea = plantilla.getLineas().stream()
                    .filter(l -> l.getId().equals(cambio.lineaId()))
                    .findFirst()
                    .orElseThrow(() -> new ConflictException("La línea no pertenece a la plantilla"));
            CuentaContable cuenta = cuentas.findById(cambio.cuentaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cuenta", "id", cambio.cuentaId()));
            if (!cuenta.isActiva() || !cuenta.isAceptaMovimientos()) {
                throw new ConflictException("La cuenta " + cuenta.getCodigo() + " no admite movimientos");
            }
            linea.setCuenta(cuenta);
        }
        plantilla.setNombre(request.nombre().strip());
        plantilla.setActiva(request.activa());
        auditor.registrar(
                usuarios.getReferenceById(actor.getId()),
                ENTIDAD,
                id,
                "ACTUALIZAR",
                plantilla.getEvento() + " activa " + request.activa());
        return ContabilidadMapper.plantilla(plantilla);
    }
}

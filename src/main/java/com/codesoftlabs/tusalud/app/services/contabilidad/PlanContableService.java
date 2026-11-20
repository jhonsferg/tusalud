package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.ActualizarCuentaContableRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.CuentaContableRequest;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.CuentaContableResponse;
import com.codesoftlabs.tusalud.app.entities.CuentaContable;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AsientoLineaRepository;
import com.codesoftlabs.tusalud.app.repositories.CuentaContableRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanContableService {

    private static final String ENTIDAD = "CUENTA";

    private final CuentaContableRepository cuentas;
    private final AsientoLineaRepository lineas;
    private final UsuarioRepository usuarios;
    private final ContabilidadAuditor auditor;

    public List<CuentaContableResponse> listar(boolean soloActivas) {
        return cuentas.findAllByOrderByCodigoAsc().stream()
                .filter(c -> !soloActivas || c.isActiva())
                .map(ContabilidadMapper::cuenta)
                .toList();
    }

    @Transactional
    public CuentaContableResponse crear(UsuarioPrincipal actor, CuentaContableRequest request) {
        String codigo = request.codigo().strip();
        if (cuentas.existsByCodigo(codigo)) {
            throw new ConflictException("Ya existe la cuenta " + codigo);
        }
        CuentaContable cuenta = cuentas.save(CuentaContable.builder()
                .codigo(codigo)
                .nombre(request.nombre().strip())
                .tipo(request.tipo())
                .naturaleza(request.tipo().naturalezaPorDefecto())
                .aceptaMovimientos(request.aceptaMovimientos())
                .activa(true)
                .sistema(false)
                .creadaEn(LocalDateTime.now())
                .build());
        auditor.registrar(
                usuarios.getReferenceById(actor.getId()),
                ENTIDAD,
                cuenta.getId(),
                "CREAR",
                codigo + " " + cuenta.getNombre());
        return ContabilidadMapper.cuenta(cuenta);
    }

    @Transactional
    public CuentaContableResponse actualizar(UsuarioPrincipal actor, UUID id, ActualizarCuentaContableRequest request) {
        CuentaContable cuenta =
                cuentas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cuenta", "id", id));
        if (cuenta.isSistema()
                && (cuenta.isAceptaMovimientos() != request.aceptaMovimientos()
                        || cuenta.isActiva() != request.activa())) {
            throw new ConflictException("Las cuentas del sistema solo permiten cambiar su nombre");
        }
        if (cuenta.isAceptaMovimientos() != request.aceptaMovimientos() && lineas.existsByCuentaId(id)) {
            throw new ConflictException("La cuenta ya tiene movimientos y no puede cambiar su naturaleza de registro");
        }
        cuenta.setNombre(request.nombre().strip());
        cuenta.setAceptaMovimientos(request.aceptaMovimientos());
        cuenta.setActiva(request.activa());
        auditor.registrar(
                usuarios.getReferenceById(actor.getId()),
                ENTIDAD,
                id,
                "ACTUALIZAR",
                cuenta.getCodigo() + " activa " + request.activa());
        return ContabilidadMapper.cuenta(cuenta);
    }
}

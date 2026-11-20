package com.codesoftlabs.tusalud.app.services.planilla;

import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ConceptoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.EmpleadoConceptoRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ConceptoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.EmpleadoConceptoResponse;
import com.codesoftlabs.tusalud.app.entities.ConceptoPlanilla;
import com.codesoftlabs.tusalud.app.entities.Empleado;
import com.codesoftlabs.tusalud.app.entities.EmpleadoConcepto;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.ModoConcepto;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoConcepto;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ConceptoPlanillaRepository;
import com.codesoftlabs.tusalud.app.repositories.EmpleadoConceptoRepository;
import com.codesoftlabs.tusalud.app.repositories.EmpleadoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConceptosPlanillaService {

    private static final String ENTIDAD = "CONCEPTO";
    private static final BigDecimal TOPE_PORCENTAJE = BigDecimal.valueOf(100);

    private final ConceptoPlanillaRepository conceptos;
    private final EmpleadoConceptoRepository asignaciones;
    private final EmpleadoRepository empleados;
    private final UsuarioRepository usuarioRepository;
    private final PlanillaAuditor auditor;

    public List<ConceptoResponse> listar() {
        return conceptos.findAllByOrderByOrdenAscNombreAsc().stream()
                .map(PlanillaMapper::concepto)
                .toList();
    }

    @Transactional
    public ConceptoResponse crear(UsuarioPrincipal actor, ConceptoRequest request) {
        String codigo = request.codigo().trim().toUpperCase();
        if (conceptos.existsByCodigo(codigo)) {
            throw new ConflictException("Ya existe un concepto con el código " + codigo);
        }
        validarValor(request.modo(), request.valorDefecto());
        int orden = conceptos.findAllByOrderByOrdenAscNombreAsc().stream()
                        .mapToInt(ConceptoPlanilla::getOrden)
                        .max()
                        .orElse(0)
                + 10;
        ConceptoPlanilla concepto = conceptos.save(ConceptoPlanilla.builder()
                .codigo(codigo)
                .nombre(request.nombre().trim())
                .tipo(soloConfigurable(request.tipo()))
                .modo(request.modo())
                .valorDefecto(request.valorDefecto())
                .afectoRenta(Boolean.TRUE.equals(request.afectoRenta()))
                .afectoPension(Boolean.TRUE.equals(request.afectoPension()))
                .afectoEssalud(Boolean.TRUE.equals(request.afectoEssalud()))
                .activo(request.activo() == null || request.activo())
                .sistema(false)
                .orden(orden)
                .build());
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()), ENTIDAD, concepto.getId(), "CREAR", codigo);
        return PlanillaMapper.concepto(concepto);
    }

    @Transactional
    public ConceptoResponse actualizar(UsuarioPrincipal actor, UUID id, ConceptoRequest request) {
        ConceptoPlanilla concepto = concepto(id);
        if (concepto.isSistema()) {
            throw new ConflictException("Los conceptos del sistema no se pueden modificar");
        }
        validarValor(request.modo(), request.valorDefecto());
        concepto.setNombre(request.nombre().trim());
        concepto.setTipo(soloConfigurable(request.tipo()));
        concepto.setModo(request.modo());
        concepto.setValorDefecto(request.valorDefecto());
        concepto.setAfectoRenta(Boolean.TRUE.equals(request.afectoRenta()));
        concepto.setAfectoPension(Boolean.TRUE.equals(request.afectoPension()));
        concepto.setAfectoEssalud(Boolean.TRUE.equals(request.afectoEssalud()));
        if (request.activo() != null) {
            concepto.setActivo(request.activo());
        }
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()), ENTIDAD, id, "ACTUALIZAR", concepto.getCodigo());
        return PlanillaMapper.concepto(concepto);
    }

    public List<EmpleadoConceptoResponse> deEmpleado(UUID empleadoId) {
        empleado(empleadoId);
        return asignaciones.deEmpleado(empleadoId).stream()
                .map(PlanillaMapper::asignacion)
                .toList();
    }

    @Transactional
    public EmpleadoConceptoResponse asignar(UsuarioPrincipal actor, UUID empleadoId, EmpleadoConceptoRequest request) {
        Empleado empleado = empleado(empleadoId);
        ConceptoPlanilla concepto = concepto(request.conceptoId());
        if (concepto.isSistema()) {
            throw new ConflictException("Los conceptos del sistema se calculan solos y no se asignan");
        }
        if (!concepto.isActivo()) {
            throw new ConflictException("El concepto está inactivo");
        }
        if (request.vigenteHasta() != null && request.vigenteHasta().isBefore(request.vigenteDesde())) {
            throw new DatosInvalidosException("La vigencia final no puede ser anterior a la inicial");
        }
        validarValor(concepto.getModo(), request.valor());
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        EmpleadoConcepto asignacion = asignaciones.save(EmpleadoConcepto.builder()
                .empleado(empleado)
                .concepto(concepto)
                .valor(request.valor())
                .vigenteDesde(request.vigenteDesde())
                .vigenteHasta(request.vigenteHasta())
                .activo(true)
                .creadoPor(usuario)
                .build());
        auditor.registrar(
                usuario,
                ENTIDAD,
                empleadoId,
                "ASIGNAR",
                concepto.getCodigo() + " " + request.valor() + " desde " + request.vigenteDesde());
        return PlanillaMapper.asignacion(asignacion);
    }

    @Transactional
    public void quitar(UsuarioPrincipal actor, UUID empleadoId, UUID asignacionId) {
        EmpleadoConcepto asignacion = asignaciones
                .findById(asignacionId)
                .filter(a -> a.getEmpleado().getId().equals(empleadoId))
                .orElseThrow(() -> new ResourceNotFoundException("Asignación de concepto", "id", asignacionId));
        asignacion.setActivo(false);
        if (asignacion.getVigenteHasta() == null || asignacion.getVigenteHasta().isAfter(LocalDate.now())) {
            asignacion.setVigenteHasta(
                    LocalDate.now().isBefore(asignacion.getVigenteDesde())
                            ? asignacion.getVigenteDesde()
                            : LocalDate.now());
        }
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()),
                ENTIDAD,
                empleadoId,
                "QUITAR",
                asignacion.getConcepto().getCodigo());
    }

    private ConceptoPlanilla concepto(UUID id) {
        return conceptos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Concepto", "id", id));
    }

    private Empleado empleado(UUID id) {
        return empleados.findByIdConUsuario(id).orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", id));
    }

    private static TipoConcepto soloConfigurable(TipoConcepto tipo) {
        if (tipo == TipoConcepto.APORTE_EMPLEADOR) {
            throw new ConflictException("Los aportes del empleador los calcula el sistema");
        }
        return tipo;
    }

    private static void validarValor(ModoConcepto modo, BigDecimal valor) {
        if (modo == ModoConcepto.PORCENTAJE_BASICO && valor.compareTo(TOPE_PORCENTAJE) > 0) {
            throw new DatosInvalidosException("El porcentaje no puede superar 100");
        }
    }
}

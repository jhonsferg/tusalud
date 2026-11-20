package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.PeriodoResponse;
import com.codesoftlabs.tusalud.app.entities.PeriodoContable;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoPeriodo;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AsientoRepository;
import com.codesoftlabs.tusalud.app.repositories.PeriodoContableRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PeriodoContableService {

    private static final String ENTIDAD = "PERIODO";

    private final PeriodoContableRepository periodos;
    private final AsientoRepository asientos;
    private final UsuarioRepository usuarios;
    private final ContabilidadAuditor auditor;

    public List<PeriodoResponse> listar() {
        return periodos.findAllByOrderByAnioDescMesDesc().stream()
                .map(this::respuesta)
                .toList();
    }

    public PeriodoResponse obtener(UUID id) {
        return respuesta(periodo(id));
    }

    public Optional<PeriodoContable> deFecha(LocalDate fecha) {
        return periodos.findByAnioAndMes(fecha.getYear(), fecha.getMonthValue());
    }

    @Transactional
    public PeriodoResponse abrir(UsuarioPrincipal actor, int anio, int mes) {
        if (periodos.findByAnioAndMes(anio, mes).isPresent()) {
            throw new ConflictException("El periodo " + String.format("%04d-%02d", anio, mes) + " ya existe");
        }
        PeriodoContable periodo = crear(anio, mes);
        auditor.registrar(
                usuarios.getReferenceById(actor.getId()), ENTIDAD, periodo.getId(), "ABRIR", periodo.etiqueta());
        return respuesta(periodo);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public PeriodoContable abiertoParaAutomatico(LocalDate fecha) {
        PeriodoContable periodo = periodos.findByAnioAndMes(fecha.getYear(), fecha.getMonthValue())
                .orElseGet(() -> crear(fecha.getYear(), fecha.getMonthValue()));
        if (!periodo.abierto()) {
            throw new ConflictException("El periodo " + periodo.etiqueta() + " está cerrado");
        }
        return periodo;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public PeriodoContable abiertoParaManual(LocalDate fecha) {
        PeriodoContable periodo = periodos.findByAnioAndMes(fecha.getYear(), fecha.getMonthValue())
                .orElseThrow(() -> new ConflictException(
                        "El periodo " + String.format("%04d-%02d", fecha.getYear(), fecha.getMonthValue())
                                + " no está abierto. Ábralo antes de registrar asientos"));
        if (!periodo.abierto()) {
            throw new ConflictException("El periodo " + periodo.etiqueta() + " está cerrado");
        }
        return periodo;
    }

    @Transactional
    public PeriodoResponse cerrar(UsuarioPrincipal actor, UUID id) {
        PeriodoContable periodo =
                periodos.bloquear(id).orElseThrow(() -> new ResourceNotFoundException("Periodo", "id", id));
        if (!periodo.abierto()) {
            throw new ConflictException("El periodo " + periodo.etiqueta() + " ya está cerrado");
        }
        if (asientos.contarBorradores(id) > 0) {
            throw new ConflictException("El periodo tiene asientos en borrador. Contabilícelos o elimínelos");
        }
        if (asientos.contarRegistradosPor(id, actor.getId()) > 0) {
            throw new ForbiddenOperationException("Quien registró asientos del periodo no puede cerrarlo");
        }
        Usuario usuario = usuarios.getReferenceById(actor.getId());
        periodo.setEstado(EstadoPeriodo.CERRADO);
        periodo.setCerradoEn(LocalDateTime.now());
        periodo.setCerradoPor(usuario);
        auditor.registrar(usuario, ENTIDAD, id, "CERRAR", periodo.etiqueta());
        return respuesta(periodo);
    }

    @Transactional
    public PeriodoResponse reabrir(UsuarioPrincipal actor, UUID id, String motivo) {
        PeriodoContable periodo =
                periodos.bloquear(id).orElseThrow(() -> new ResourceNotFoundException("Periodo", "id", id));
        if (periodo.abierto()) {
            throw new ConflictException("El periodo " + periodo.etiqueta() + " ya está abierto");
        }
        Usuario usuario = usuarios.getReferenceById(actor.getId());
        periodo.setEstado(EstadoPeriodo.ABIERTO);
        periodo.setReabiertoEn(LocalDateTime.now());
        periodo.setReabiertoPor(usuario);
        periodo.setMotivoReapertura(motivo.strip());
        auditor.registrar(usuario, ENTIDAD, id, "REABRIR", periodo.etiqueta() + ": " + motivo.strip());
        return respuesta(periodo);
    }

    public Optional<PeriodoContable> actual() {
        return deFecha(LocalDate.now());
    }

    private PeriodoContable crear(int anio, int mes) {
        return periodos.saveAndFlush(PeriodoContable.builder()
                .anio(anio)
                .mes(mes)
                .estado(EstadoPeriodo.ABIERTO)
                .abiertoEn(LocalDateTime.now())
                .build());
    }

    private PeriodoContable periodo(UUID id) {
        return periodos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Periodo", "id", id));
    }

    private PeriodoResponse respuesta(PeriodoContable p) {
        long total = asientos.countByPeriodoIdAndEstadoNot(p.getId(), EstadoAsiento.BORRADOR);
        long borradores = asientos.contarBorradores(p.getId());
        return new PeriodoResponse(
                p.getId(),
                p.getAnio(),
                p.getMes(),
                p.etiqueta(),
                p.getEstado(),
                p.inicio(),
                p.fin(),
                total,
                borradores,
                p.getCerradoEn(),
                p.getCerradoPor() == null ? null : p.getCerradoPor().getNombreCompleto(),
                p.getReabiertoEn(),
                p.getReabiertoPor() == null ? null : p.getReabiertoPor().getNombreCompleto(),
                p.getMotivoReapertura());
    }
}

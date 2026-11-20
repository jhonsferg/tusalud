package com.codesoftlabs.tusalud.app.services.planilla;

import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ParametroRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ParametroResponse;
import com.codesoftlabs.tusalud.app.entities.ParametroPlanilla;
import com.codesoftlabs.tusalud.app.entities.Planilla;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoPlanilla;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ParametroPlanillaRepository;
import com.codesoftlabs.tusalud.app.repositories.PlanillaRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParametrosPlanillaService {

    private static final String ENTIDAD = "PARAMETRO";

    private final ParametroPlanillaRepository repositorio;
    private final PlanillaRepository planillaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanillaAuditor auditor;

    public List<ParametroResponse> listar(String codigo, boolean soloVigentes) {
        LocalDate hoy = LocalDate.now();
        List<ParametroPlanilla> lista = soloVigentes ? repositorio.vigentesEn(hoy) : repositorio.historial(codigo);
        return lista.stream().map(p -> PlanillaMapper.parametro(p, hoy)).toList();
    }

    public CalculadoraPlanilla.Parametros vigentesEn(LocalDate fecha) {
        Map<String, BigDecimal> valores = new HashMap<>();
        for (ParametroPlanilla p : repositorio.vigentesEn(fecha)) {
            valores.put(p.getCodigo(), p.getValor());
        }
        return new CalculadoraPlanilla.Parametros(valores);
    }

    @Transactional
    public ParametroResponse nuevaVersion(UsuarioPrincipal actor, ParametroRequest request) {
        String codigo = request.codigo().trim().toUpperCase();
        if (!repositorio.existsByCodigo(codigo)) {
            throw new ResourceNotFoundException("Parámetro de planilla", "código", codigo);
        }
        if (request.vigenteDesde().getDayOfMonth() != 1) {
            throw new ConflictException("La vigencia debe comenzar el primer día de un mes");
        }
        Planilla ultima = planillaRepository.findAllByOrderByAnioDescMesDesc().stream()
                .filter(p -> p.getEstado() != EstadoPlanilla.BORRADOR && p.getEstado() != EstadoPlanilla.CALCULADA)
                .findFirst()
                .orElse(null);
        if (ultima != null) {
            LocalDate minimo =
                    LocalDate.of(ultima.getAnio(), ultima.getMes(), 1).plusMonths(1);
            if (request.vigenteDesde().isBefore(minimo)) {
                throw new ConflictException("Ya existe una planilla aprobada hasta " + ultima.getCodigo()
                        + ", la nueva vigencia debe comenzar desde " + minimo);
            }
        }
        ParametroPlanilla anterior = repositorio
                .findFirstByCodigoAndVigenteHastaIsNullOrderByVigenteDesdeDesc(codigo)
                .orElse(null);
        if (anterior != null && !request.vigenteDesde().isAfter(anterior.getVigenteDesde())) {
            throw new ConflictException(
                    "La nueva vigencia debe ser posterior a la vigente desde " + anterior.getVigenteDesde());
        }
        if (repositorio.existsByCodigoAndVigenteDesde(codigo, request.vigenteDesde())) {
            throw new ConflictException("Ya existe una versión de " + codigo + " para esa vigencia");
        }
        String descripcion =
                request.descripcion() == null || request.descripcion().isBlank()
                        ? (anterior == null ? codigo : anterior.getDescripcion())
                        : request.descripcion().trim();
        if (anterior != null) {
            anterior.setVigenteHasta(request.vigenteDesde().minusDays(1));
        }
        ParametroPlanilla nuevo = repositorio.save(ParametroPlanilla.builder()
                .codigo(codigo)
                .valor(request.valor())
                .vigenteDesde(request.vigenteDesde())
                .descripcion(descripcion)
                .porValidar(true)
                .creadoPor(usuarioRepository.getReferenceById(actor.getId()))
                .build());
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()),
                ENTIDAD,
                nuevo.getId(),
                "NUEVA_VERSION",
                codigo + " = " + request.valor() + " desde " + request.vigenteDesde());
        return PlanillaMapper.parametro(nuevo, LocalDate.now());
    }

    @Transactional
    public ParametroResponse validar(UsuarioPrincipal actor, UUID id) {
        ParametroPlanilla parametro =
                repositorio.findById(id).orElseThrow(() -> new ResourceNotFoundException("Parámetro", "id", id));
        if (!parametro.isPorValidar()) {
            throw new ConflictException("El parámetro ya fue validado");
        }
        parametro.setPorValidar(false);
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()),
                ENTIDAD,
                id,
                "VALIDAR",
                parametro.getCodigo() + " " + parametro.getValor());
        return PlanillaMapper.parametro(parametro, LocalDate.now());
    }
}

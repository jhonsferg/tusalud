package com.codesoftlabs.tusalud.app.services.seguros;

import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.AseguradoraRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.ConvenioRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.ReglaRequest;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.AseguradoraResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.ConvenioResponse;
import com.codesoftlabs.tusalud.app.entities.Aseguradora;
import com.codesoftlabs.tusalud.app.entities.Convenio;
import com.codesoftlabs.tusalud.app.entities.ReglaCobertura;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCobertura;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AseguradoraRepository;
import com.codesoftlabs.tusalud.app.repositories.ConvenioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SegurosCatalogoService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final AseguradoraRepository aseguradoraRepository;
    private final ConvenioRepository convenioRepository;

    public List<AseguradoraResponse> aseguradoras() {
        return aseguradoraRepository.findAllByOrderByNombreAsc().stream()
                .map(SegurosMapper::aseguradora)
                .toList();
    }

    @Transactional
    public AseguradoraResponse crearAseguradora(AseguradoraRequest request) {
        String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
        if (aseguradoraRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe una aseguradora con el código " + codigo);
        }
        Aseguradora creada = aseguradoraRepository.save(Aseguradora.builder()
                .codigo(codigo)
                .nombre(request.nombre().trim())
                .tipo(request.tipo())
                .ruc(vacioANulo(request.ruc()))
                .contacto(vacioANulo(request.contacto()))
                .activa(request.activa() == null || request.activa())
                .build());
        log.info("Aseguradora {} creada", creada.getId());
        return SegurosMapper.aseguradora(creada);
    }

    @Transactional
    public AseguradoraResponse actualizarAseguradora(UUID id, AseguradoraRequest request) {
        Aseguradora aseguradora = aseguradoraRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aseguradora", "id", id));
        String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
        if (aseguradoraRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new ConflictException("Ya existe una aseguradora con el código " + codigo);
        }
        aseguradora.setCodigo(codigo);
        aseguradora.setNombre(request.nombre().trim());
        aseguradora.setTipo(request.tipo());
        aseguradora.setRuc(vacioANulo(request.ruc()));
        aseguradora.setContacto(vacioANulo(request.contacto()));
        if (request.activa() != null) {
            aseguradora.setActiva(request.activa());
        }
        return SegurosMapper.aseguradora(aseguradora);
    }

    public List<ConvenioResponse> convenios() {
        LocalDate hoy = LocalDate.now();
        return convenioRepository.todos().stream()
                .map(c -> SegurosMapper.convenio(c, hoy))
                .toList();
    }

    public ConvenioResponse convenio(UUID id) {
        return SegurosMapper.convenio(buscar(id), LocalDate.now());
    }

    @Transactional
    public ConvenioResponse crearConvenio(ConvenioRequest request) {
        Aseguradora aseguradora = aseguradoraRepository
                .findById(request.aseguradoraId())
                .orElseThrow(() -> new ResourceNotFoundException("Aseguradora", "id", request.aseguradoraId()));
        Convenio convenio = Convenio.builder().aseguradora(aseguradora).build();
        aplicar(convenio, request);
        Convenio creado = convenioRepository.save(convenio);
        log.info("Convenio {} creado", creado.getId());
        return SegurosMapper.convenio(creado, LocalDate.now());
    }

    @Transactional
    public ConvenioResponse actualizarConvenio(UUID id, ConvenioRequest request) {
        Convenio convenio = buscar(id);
        if (!convenio.getAseguradora().getId().equals(request.aseguradoraId())) {
            throw new DatosInvalidosException("No se puede cambiar la aseguradora de un convenio existente");
        }
        aplicar(convenio, request);
        return SegurosMapper.convenio(convenio, LocalDate.now());
    }

    private Convenio buscar(UUID id) {
        return convenioRepository
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convenio", "id", id));
    }

    private void aplicar(Convenio convenio, ConvenioRequest request) {
        if (request.vigenciaHasta().isBefore(request.vigenciaDesde())) {
            throw new DatosInvalidosException("La vigencia final no puede ser anterior a la inicial");
        }
        Set<CategoriaServicio> vistas = EnumSet.noneOf(CategoriaServicio.class);
        for (ReglaRequest regla : request.reglas()) {
            if (!vistas.add(regla.categoria())) {
                throw new DatosInvalidosException("La categoría " + regla.categoria() + " está repetida");
            }
            validar(regla);
        }
        convenio.setNombre(request.nombre().trim());
        convenio.setVigenciaDesde(request.vigenciaDesde());
        convenio.setVigenciaHasta(request.vigenciaHasta());
        convenio.setTopeAnual(
                request.topeAnual() == null ? null : request.topeAnual().setScale(2, RoundingMode.HALF_UP));
        if (request.activo() != null) {
            convenio.setActivo(request.activo());
        }
        Map<CategoriaServicio, ReglaCobertura> existentes = new EnumMap<>(CategoriaServicio.class);
        convenio.getReglas().forEach(r -> existentes.put(r.getCategoria(), r));
        for (ReglaRequest regla : request.reglas()) {
            BigDecimal valor = regla.valor().setScale(2, RoundingMode.HALF_UP);
            BigDecimal tope = regla.topePorServicio() == null
                    ? null
                    : regla.topePorServicio().setScale(2, RoundingMode.HALF_UP);
            ReglaCobertura actual = existentes.get(regla.categoria());
            if (actual == null) {
                convenio.getReglas()
                        .add(ReglaCobertura.builder()
                                .convenio(convenio)
                                .categoria(regla.categoria())
                                .tipo(regla.tipo())
                                .valor(valor)
                                .topePorServicio(tope)
                                .requierePreautorizacion(regla.requierePreautorizacion())
                                .build());
            } else {
                actual.setTipo(regla.tipo());
                actual.setValor(valor);
                actual.setTopePorServicio(tope);
                actual.setRequierePreautorizacion(regla.requierePreautorizacion());
            }
        }
        convenio.getReglas().removeIf(r -> !vistas.contains(r.getCategoria()));
        Set<String> exclusiones = new LinkedHashSet<>();
        if (request.exclusiones() != null) {
            for (String codigo : request.exclusiones()) {
                if (codigo != null && !codigo.isBlank()) {
                    exclusiones.add(codigo.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        convenio.getExclusiones().clear();
        convenio.getExclusiones().addAll(exclusiones);
    }

    private static void validar(ReglaRequest regla) {
        if (regla.tipo() == TipoCobertura.PORCENTAJE
                && (regla.valor().compareTo(BigDecimal.ZERO) < 0
                        || regla.valor().compareTo(CIEN) > 0)) {
            throw new DatosInvalidosException("El porcentaje de cobertura debe estar entre 0 y 100");
        }
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}

package com.codesoftlabs.tusalud.app.services.seguros;

import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.LiquidacionCrearRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.LiquidacionPagoRequest;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.CarteraResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.CuentaAseguradoraResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.LiquidacionResponse;
import com.codesoftlabs.tusalud.app.entities.Aseguradora;
import com.codesoftlabs.tusalud.app.entities.CuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.Liquidacion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoLiquidacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AseguradoraRepository;
import com.codesoftlabs.tusalud.app.repositories.CuentaAseguradoraRepository;
import com.codesoftlabs.tusalud.app.repositories.LiquidacionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiquidacionService {

    private static final int DIAS_MAXIMOS = 366;

    private final LiquidacionRepository liquidacionRepository;
    private final CuentaAseguradoraRepository cuentaRepository;
    private final AseguradoraRepository aseguradoraRepository;
    private final UsuarioRepository usuarioRepository;

    public List<CarteraResponse> cartera() {
        LocalDate hoy = LocalDate.now();
        Map<UUID, Acumulado> porAseguradora = new LinkedHashMap<>();
        for (CuentaAseguradora cuenta : cuentaRepository.porEstado(EstadoCuentaAseguradora.PENDIENTE)) {
            porAseguradora
                    .computeIfAbsent(cuenta.getAseguradora().getId(), id -> new Acumulado(cuenta.getAseguradora()))
                    .sumar(cuenta, hoy);
        }
        return porAseguradora.values().stream()
                .map(Acumulado::respuesta)
                .sorted(Comparator.comparing(CarteraResponse::pendiente).reversed())
                .toList();
    }

    public List<CuentaAseguradoraResponse> cuentas(UUID aseguradoraId) {
        LocalDate hoy = LocalDate.now();
        return cuentaRepository.deAseguradora(aseguradoraId).stream()
                .map(c -> SegurosMapper.cuenta(c, hoy))
                .toList();
    }

    public List<LiquidacionResponse> listar() {
        return liquidacionRepository.todas().stream()
                .map(SegurosMapper::liquidacion)
                .toList();
    }

    public LiquidacionResponse obtener(UUID id) {
        return SegurosMapper.liquidacion(buscar(id));
    }

    public List<CuentaAseguradoraResponse> cuentasDe(UUID liquidacionId) {
        buscar(liquidacionId);
        LocalDate hoy = LocalDate.now();
        return cuentaRepository.deLiquidacion(liquidacionId).stream()
                .map(c -> SegurosMapper.cuenta(c, hoy))
                .toList();
    }

    @Transactional
    public LiquidacionResponse crear(UsuarioPrincipal actor, LiquidacionCrearRequest request) {
        if (request.hasta().isBefore(request.desde())) {
            throw new DatosInvalidosException("La fecha final no puede ser anterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(request.desde(), request.hasta()) > DIAS_MAXIMOS) {
            throw new DatosInvalidosException("El periodo no puede superar " + DIAS_MAXIMOS + " días");
        }
        Aseguradora aseguradora = aseguradoraRepository
                .findById(request.aseguradoraId())
                .orElseThrow(() -> new ResourceNotFoundException("Aseguradora", "id", request.aseguradoraId()));
        List<CuentaAseguradora> cuentas = cuentaRepository.pendientesEnRango(
                aseguradora.getId(),
                request.desde().atStartOfDay(),
                request.hasta().plusDays(1).atStartOfDay());
        if (cuentas.isEmpty()) {
            throw new DatosInvalidosException("No hay cuentas pendientes de esa aseguradora en el periodo");
        }
        BigDecimal total = cuentas.stream()
                .map(CuentaAseguradora::getMontoCubierto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        String prefijo = String.format(
                "LQ-%d%02d-", request.hasta().getYear(), request.hasta().getMonthValue());
        Liquidacion liquidacion = liquidacionRepository.save(Liquidacion.builder()
                .codigo(prefijo + String.format("%03d", liquidacionRepository.countByCodigoStartingWith(prefijo) + 1))
                .aseguradora(aseguradora)
                .desde(request.desde())
                .hasta(request.hasta())
                .total(total)
                .cantidadCuentas(cuentas.size())
                .estado(EstadoLiquidacion.BORRADOR)
                .creadaPor(usuarioRepository.getReferenceById(actor.getId()))
                .creadaEn(LocalDateTime.now())
                .build());
        for (CuentaAseguradora cuenta : cuentas) {
            cuenta.setEstado(EstadoCuentaAseguradora.LIQUIDADA);
            cuenta.setLiquidacion(liquidacion);
        }
        log.info("Liquidación {} creada por {}", liquidacion.getId(), actor.getId());
        return SegurosMapper.liquidacion(buscar(liquidacion.getId()));
    }

    @Transactional
    public LiquidacionResponse enviar(UsuarioPrincipal actor, UUID id) {
        Liquidacion liquidacion = buscar(id);
        if (liquidacion.getEstado() != EstadoLiquidacion.BORRADOR) {
            throw new ConflictException("Solo se envía una liquidación en borrador");
        }
        liquidacion.setEstado(EstadoLiquidacion.ENVIADA);
        liquidacion.setEnviadaEn(LocalDateTime.now());
        log.info("Liquidación {} enviada por {}", id, actor.getId());
        return SegurosMapper.liquidacion(liquidacion);
    }

    @Transactional
    public LiquidacionResponse registrarPago(UsuarioPrincipal actor, UUID id, LiquidacionPagoRequest request) {
        Liquidacion liquidacion = buscar(id);
        if (liquidacion.getEstado() != EstadoLiquidacion.ENVIADA
                && liquidacion.getEstado() != EstadoLiquidacion.OBSERVADA) {
            throw new ConflictException("Solo se registra el pago de una liquidación enviada u observada");
        }
        BigDecimal pagado = request.montoPagado().setScale(2, RoundingMode.HALF_UP);
        if (pagado.compareTo(liquidacion.getTotal()) > 0) {
            throw new DatosInvalidosException("El monto pagado no puede superar el total de la liquidación");
        }
        BigDecimal glosa = liquidacion.getTotal().subtract(pagado);
        String motivo =
                request.motivoGlosa() == null ? "" : request.motivoGlosa().trim();
        if (glosa.signum() > 0 && motivo.length() < 5) {
            throw new DatosInvalidosException("Indique el motivo de la glosa");
        }
        liquidacion.setMontoPagado(pagado);
        liquidacion.setMontoGlosa(glosa);
        liquidacion.setMotivoGlosa(glosa.signum() > 0 ? motivo : null);
        if (glosa.signum() == 0) {
            liquidacion.setEstado(EstadoLiquidacion.PAGADA);
            liquidacion.setPagadaEn(LocalDateTime.now());
            cuentaRepository.deLiquidacion(id).forEach(cuenta -> cuenta.setEstado(EstadoCuentaAseguradora.PAGADA));
        } else {
            liquidacion.setEstado(EstadoLiquidacion.OBSERVADA);
        }
        log.info("Pago de la liquidación {} registrado por {}", id, actor.getId());
        return SegurosMapper.liquidacion(liquidacion);
    }

    private Liquidacion buscar(UUID id) {
        return liquidacionRepository
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación", "id", id));
    }

    private static final class Acumulado {

        private final Aseguradora aseguradora;
        private BigDecimal total = BigDecimal.ZERO;
        private long cuentas;
        private BigDecimal t0 = BigDecimal.ZERO;
        private BigDecimal t31 = BigDecimal.ZERO;
        private BigDecimal t61 = BigDecimal.ZERO;
        private BigDecimal t90 = BigDecimal.ZERO;

        Acumulado(Aseguradora aseguradora) {
            this.aseguradora = aseguradora;
        }

        void sumar(CuentaAseguradora cuenta, LocalDate hoy) {
            BigDecimal monto = cuenta.getMontoCubierto();
            long dias = ChronoUnit.DAYS.between(cuenta.getFecha().toLocalDate(), hoy);
            total = total.add(monto);
            cuentas++;
            if (dias <= 30) {
                t0 = t0.add(monto);
            } else if (dias <= 60) {
                t31 = t31.add(monto);
            } else if (dias <= 90) {
                t61 = t61.add(monto);
            } else {
                t90 = t90.add(monto);
            }
        }

        CarteraResponse respuesta() {
            return new CarteraResponse(aseguradora.getId(), aseguradora.getNombre(), total, cuentas, t0, t31, t61, t90);
        }
    }
}

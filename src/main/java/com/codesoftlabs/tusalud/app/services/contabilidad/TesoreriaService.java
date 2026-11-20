package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.CuentaTesoreriaRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.MovimientoTesoreriaRequest;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.CuentaTesoreriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.MovimientoResponse;
import com.codesoftlabs.tusalud.app.entities.Asiento;
import com.codesoftlabs.tusalud.app.entities.CuentaContable;
import com.codesoftlabs.tusalud.app.entities.CuentaTesoreria;
import com.codesoftlabs.tusalud.app.entities.MovimientoTesoreria;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaContable;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaTesoreria;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoMovimientoTesoreria;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CuentaContableRepository;
import com.codesoftlabs.tusalud.app.repositories.CuentaTesoreriaRepository;
import com.codesoftlabs.tusalud.app.repositories.MovimientoTesoreriaRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TesoreriaService {

    private static final String ENTIDAD = "TESORERIA";
    private static final int LIMITE = 300;

    private final CuentaTesoreriaRepository cuentasTesoreria;
    private final MovimientoTesoreriaRepository movimientos;
    private final CuentaContableRepository cuentasContables;
    private final AsientoService asientoService;
    private final UsuarioRepository usuarios;
    private final ContabilidadAuditor auditor;

    public List<CuentaTesoreriaResponse> cuentas(boolean soloActivas) {
        List<CuentaTesoreria> lista = soloActivas ? cuentasTesoreria.activas() : cuentasTesoreria.todas();
        return lista.stream()
                .map(c -> ContabilidadMapper.tesoreria(c, saldo(c)))
                .toList();
    }

    public BigDecimal saldoTotal() {
        return cuentasTesoreria.activas().stream().map(this::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal saldo(CuentaTesoreria cuenta) {
        return cuenta.getSaldoInicial()
                .add(movimientos.saldoMovimientos(
                        cuenta.getId(), LocalDate.now().plusYears(1)));
    }

    @Transactional
    public CuentaTesoreriaResponse crearCuenta(UsuarioPrincipal actor, CuentaTesoreriaRequest request) {
        if (cuentasTesoreria.existsByNombreIgnoreCase(request.nombre().strip())) {
            throw new ConflictException("Ya existe una cuenta de tesorería con ese nombre");
        }
        CuentaContable contable = contableValida(request.cuentaContableId());
        CuentaTesoreria cuenta = CuentaTesoreria.builder()
                .nombre(request.nombre().strip())
                .tipo(request.tipo())
                .banco(vacioANulo(request.banco()))
                .ultimos4(request.ultimos4())
                .moneda("PEN")
                .cuentaContable(contable)
                .saldoInicial(request.saldoInicial().setScale(2, RoundingMode.HALF_UP))
                .predeterminada(false)
                .activa(true)
                .creadaEn(LocalDateTime.now())
                .build();
        cuenta = cuentasTesoreria.save(cuenta);
        if (request.predeterminada()) {
            marcarPredeterminada(cuenta);
        }
        auditor.registrar(
                usuarios.getReferenceById(actor.getId()), ENTIDAD, cuenta.getId(), "CREAR_CUENTA", cuenta.getNombre());
        return ContabilidadMapper.tesoreria(cuenta, saldo(cuenta));
    }

    @Transactional
    public CuentaTesoreriaResponse actualizarCuenta(UsuarioPrincipal actor, UUID id, CuentaTesoreriaRequest request) {
        CuentaTesoreria cuenta = cuentaTesoreria(id);
        CuentaContable contable = contableValida(request.cuentaContableId());
        boolean activa = request.activa() == null ? cuenta.isActiva() : request.activa();
        if (!cuenta.getCuentaContable().getId().equals(contable.getId())
                && !movimientos
                        .filtrar(id, null, null, null, PageRequest.of(0, 1))
                        .isEmpty()) {
            throw new ConflictException("La cuenta ya tiene movimientos y no puede cambiar su cuenta contable");
        }
        cuenta.setNombre(request.nombre().strip());
        cuenta.setBanco(vacioANulo(request.banco()));
        cuenta.setUltimos4(request.ultimos4());
        cuenta.setCuentaContable(contable);
        cuenta.setActiva(activa);
        if (request.predeterminada() && activa) {
            marcarPredeterminada(cuenta);
        } else if (!request.predeterminada()) {
            cuenta.setPredeterminada(false);
        }
        auditor.registrar(
                usuarios.getReferenceById(actor.getId()), ENTIDAD, id, "ACTUALIZAR_CUENTA", cuenta.getNombre());
        return ContabilidadMapper.tesoreria(cuenta, saldo(cuenta));
    }

    public List<MovimientoResponse> listarMovimientos(
            UUID cuentaId, LocalDate desde, LocalDate hasta, Boolean conciliado) {
        return movimientos.filtrar(cuentaId, desde, hasta, conciliado, PageRequest.of(0, LIMITE)).stream()
                .map(ContabilidadMapper::movimiento)
                .toList();
    }

    @Transactional
    public MovimientoResponse registrar(UsuarioPrincipal actor, MovimientoTesoreriaRequest request) {
        CuentaTesoreria cuenta = cuentaTesoreria(request.cuentaId());
        if (!cuenta.isActiva()) {
            throw new ConflictException("La cuenta de tesorería está inactiva");
        }
        if (request.fecha().isAfter(LocalDate.now())) {
            throw new ConflictException("La fecha del movimiento no puede ser futura");
        }
        CuentaContable contrapartida = cuentasContables
                .findById(request.contrapartidaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta", "id", request.contrapartidaId()));
        if (!contrapartida.isActiva() || !contrapartida.isAceptaMovimientos()) {
            throw new ConflictException("La cuenta " + contrapartida.getCodigo() + " no admite movimientos");
        }
        if (contrapartida.getId().equals(cuenta.getCuentaContable().getId())) {
            throw new ConflictException("La contrapartida no puede ser la cuenta contable de la propia tesorería");
        }
        BigDecimal monto = request.monto().setScale(2, RoundingMode.HALF_UP);
        Usuario usuario = usuarios.getReferenceById(actor.getId());
        MovimientoTesoreria movimiento = movimientos.save(MovimientoTesoreria.builder()
                .cuenta(cuenta)
                .fecha(request.fecha())
                .tipo(request.tipo())
                .monto(monto)
                .descripcion(request.descripcion().strip())
                .referencia(vacioANulo(request.referencia()))
                .origen(OrigenMovimiento.MANUAL)
                .conciliado(false)
                .registradoPor(usuario)
                .registradoEn(LocalDateTime.now())
                .build());
        boolean ingreso = request.tipo() == TipoMovimientoTesoreria.INGRESO;
        Asiento asiento = asientoService.registrarAutomatico(
                OrigenAsiento.MOVIMIENTO_TESORERIA,
                movimiento.getId(),
                request.fecha(),
                (ingreso ? "Ingreso en " : "Egreso de ") + cuenta.getNombre() + ": " + movimiento.getDescripcion(),
                List.of(
                        new AsientoService.LineaCalculada(
                                cuenta.getCuentaContable(),
                                ingreso ? monto : BigDecimal.ZERO,
                                ingreso ? BigDecimal.ZERO : monto,
                                movimiento.getDescripcion()),
                        new AsientoService.LineaCalculada(
                                contrapartida,
                                ingreso ? BigDecimal.ZERO : monto,
                                ingreso ? monto : BigDecimal.ZERO,
                                movimiento.getDescripcion())));
        movimiento.setAsiento(asiento);
        auditor.registrar(
                usuario,
                ENTIDAD,
                movimiento.getId(),
                "MOVIMIENTO",
                request.tipo() + " S/ " + monto + " " + cuenta.getNombre());
        return ContabilidadMapper.movimiento(movimiento);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void movimientoAutomatico(
            OrigenMovimiento origen,
            UUID referenciaId,
            LocalDate fecha,
            TipoMovimientoTesoreria tipo,
            BigDecimal monto,
            String descripcion,
            String referencia,
            Asiento asiento) {
        CuentaTesoreria banco = cuentasTesoreria
                .findFirstByTipoAndPredeterminadaTrueAndActivaTrue(TipoCuentaTesoreria.BANCO)
                .orElse(null);
        if (banco == null
                || movimientos.findByOrigenAndReferenciaId(origen, referenciaId).isPresent()) {
            return;
        }
        movimientos.save(MovimientoTesoreria.builder()
                .cuenta(banco)
                .fecha(fecha)
                .tipo(tipo)
                .monto(monto)
                .descripcion(recortar(descripcion))
                .referencia(referencia)
                .origen(origen)
                .referenciaId(referenciaId)
                .asiento(asiento)
                .conciliado(false)
                .registradoEn(LocalDateTime.now())
                .build());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void anularMovimientoAutomatico(
            OrigenMovimiento origen, UUID referenciaId, LocalDate fecha, String descripcion) {
        MovimientoTesoreria original =
                movimientos.findByOrigenAndReferenciaId(origen, referenciaId).orElse(null);
        if (original == null) {
            return;
        }
        if (!original.isConciliado()) {
            movimientos.delete(original);
            return;
        }
        TipoMovimientoTesoreria inverso = original.getTipo() == TipoMovimientoTesoreria.INGRESO
                ? TipoMovimientoTesoreria.EGRESO
                : TipoMovimientoTesoreria.INGRESO;
        movimientos.save(MovimientoTesoreria.builder()
                .cuenta(original.getCuenta())
                .fecha(fecha)
                .tipo(inverso)
                .monto(original.getMonto())
                .descripcion(recortar(descripcion))
                .origen(OrigenMovimiento.MANUAL)
                .conciliado(false)
                .registradoEn(LocalDateTime.now())
                .build());
    }

    private void marcarPredeterminada(CuentaTesoreria cuenta) {
        for (CuentaTesoreria otra : cuentasTesoreria.findByPredeterminadaTrueAndTipo(cuenta.getTipo())) {
            otra.setPredeterminada(false);
        }
        cuenta.setPredeterminada(true);
    }

    private CuentaContable contableValida(UUID id) {
        CuentaContable contable =
                cuentasContables.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cuenta", "id", id));
        if (!contable.isActiva()
                || !contable.isAceptaMovimientos()
                || contable.getTipo() != TipoCuentaContable.ACTIVO) {
            throw new ConflictException("La cuenta contable debe ser un activo que admita movimientos");
        }
        return contable;
    }

    private CuentaTesoreria cuentaTesoreria(UUID id) {
        return cuentasTesoreria
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta de tesorería", "id", id));
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private static String recortar(String texto) {
        return texto.length() <= 200 ? texto : texto.substring(0, 200);
    }
}

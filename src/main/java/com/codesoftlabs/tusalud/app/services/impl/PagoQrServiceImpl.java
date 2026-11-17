package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.config.PagosProperties;
import com.codesoftlabs.tusalud.app.dto.request.PagoQrRequest;
import com.codesoftlabs.tusalud.app.dto.response.PagoQrResponse;
import com.codesoftlabs.tusalud.app.dto.response.ValidacionCuponResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.IntencionPago;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIntencionPago;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.IntencionPagoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CuponService;
import com.codesoftlabs.tusalud.app.services.interfaces.PagoQrService;
import com.codesoftlabs.tusalud.app.services.interfaces.PagoService;
import com.codesoftlabs.tusalud.app.services.pagos.ProveedorBilletera;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PagoQrServiceImpl implements PagoQrService {

    private static final Set<EstadoCita> ESTADOS_PAGABLES = EnumSet.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

    private final CitaRepository citaRepository;
    private final IntencionPagoRepository intencionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuponService cuponService;
    private final PagoService pagoService;
    private final PagosProperties propiedades;
    private final List<ProveedorBilletera> proveedores;

    @Override
    public PagoQrResponse crear(PagoQrRequest request, UsuarioPrincipal actor) {
        Cita cita = citaRepository
                .findByIdWithDetails(request.citaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", request.citaId()));
        if (!cita.getPaciente().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Solo el paciente de la cita puede pagarla");
        }
        if (!ESTADOS_PAGABLES.contains(cita.getEstado())) {
            throw new BusinessLogicException("No se puede pagar una cita en estado " + cita.getEstado());
        }
        if (cita.getPago() != null) {
            throw new ConflictException("La cita ya tiene un pago registrado");
        }
        ProveedorBilletera proveedor = proveedor(request.billetera());

        BigDecimal monto = cita.getMonto();
        BigDecimal descuento = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String codigoCupon = null;
        if (request.codigoCupon() != null && !request.codigoCupon().isBlank()) {
            ValidacionCuponResponse validacion = cuponService.validar(request.codigoCupon(), monto);
            if (!validacion.valido()) {
                throw new BusinessLogicException(validacion.mensaje());
            }
            descuento = validacion.descuento();
            codigoCupon = validacion.codigo();
        }

        intencionRepository.findByCitaIdAndEstado(cita.getId(), EstadoIntencionPago.PENDIENTE).stream()
                .filter(anterior -> anterior.getUsuario().getId().equals(actor.getId()))
                .forEach(anterior -> anterior.setEstado(EstadoIntencionPago.CANCELADO));

        IntencionPago intencion = intencionRepository.saveAndFlush(IntencionPago.builder()
                .codigo(UUID.randomUUID().toString())
                .cita(cita)
                .usuario(usuarioRepository.getReferenceById(actor.getId()))
                .billetera(request.billetera())
                .monto(monto)
                .descuento(descuento)
                .total(monto.subtract(descuento).setScale(2, RoundingMode.HALF_UP))
                .codigoCupon(codigoCupon)
                .estado(EstadoIntencionPago.PENDIENTE)
                .expiraEn(LocalDateTime.now().plusMinutes(propiedades.expiracionQrMinutos()))
                .build());
        log.info(
                "Intención de pago {} creada para la cita {} con {}",
                intencion.getCodigo(),
                cita.getId(),
                intencion.getBilletera());
        return respuesta(intencion);
    }

    @Override
    public PagoQrResponse obtener(String codigo, UsuarioPrincipal actor) {
        IntencionPago intencion = buscar(codigo, actor);
        expirarSiCorresponde(intencion);
        return respuesta(intencion);
    }

    @Override
    public PagoQrResponse simularConfirmacion(String codigo, UsuarioPrincipal actor) {
        if (!propiedades.simulacionHabilitada()) {
            throw new ForbiddenOperationException("La simulación de pagos con billeteras no está habilitada");
        }
        IntencionPago intencion = buscar(codigo, actor);
        expirarSiCorresponde(intencion);
        if (intencion.getEstado() == EstadoIntencionPago.CONFIRMADO) {
            return respuesta(intencion);
        }
        if (intencion.getEstado() != EstadoIntencionPago.PENDIENTE) {
            throw new BusinessLogicException("El código QR ya no es válido: " + intencion.getEstado());
        }
        if (!proveedor(intencion.getBilletera()).confirmar(intencion)) {
            throw new BusinessLogicException("La billetera no confirmó el pago");
        }
        Cita cita = citaRepository
                .findByIdWithDetails(intencion.getCita().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cita", "id", intencion.getCita().getId()));
        intencion.setPago(pagoService.registrarPagoBilletera(
                cita, nombre(intencion), intencion.getMonto(), intencion.getDescuento(), intencion.getCodigoCupon()));
        intencion.setEstado(EstadoIntencionPago.CONFIRMADO);
        return respuesta(intencion);
    }

    @Override
    public PagoQrResponse cancelar(String codigo, UsuarioPrincipal actor) {
        IntencionPago intencion = buscar(codigo, actor);
        expirarSiCorresponde(intencion);
        if (intencion.getEstado() == EstadoIntencionPago.CONFIRMADO) {
            throw new ConflictException("El pago ya fue confirmado y no se puede cancelar");
        }
        if (intencion.getEstado() == EstadoIntencionPago.PENDIENTE) {
            intencion.setEstado(EstadoIntencionPago.CANCELADO);
        }
        return respuesta(intencion);
    }

    private IntencionPago buscar(String codigo, UsuarioPrincipal actor) {
        return intencionRepository
                .findByCodigoAndUsuarioId(codigo, actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pago con QR", "código", codigo));
    }

    private void expirarSiCorresponde(IntencionPago intencion) {
        if (intencion.vencida(LocalDateTime.now())) {
            intencion.setEstado(EstadoIntencionPago.EXPIRADO);
        }
    }

    private ProveedorBilletera proveedor(com.codesoftlabs.tusalud.app.entities.enums.Billetera billetera) {
        return proveedores.stream()
                .filter(p -> p.soporta(billetera))
                .findFirst()
                .orElseThrow(() -> new BusinessLogicException("La billetera " + billetera + " no está disponible"));
    }

    private PagoQrResponse respuesta(IntencionPago intencion) {
        return PagoQrResponse.from(
                intencion, proveedor(intencion.getBilletera()).generarQr(intencion));
    }

    private static String nombre(IntencionPago intencion) {
        String nombre = intencion.getBilletera().name();
        return nombre.charAt(0) + nombre.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}

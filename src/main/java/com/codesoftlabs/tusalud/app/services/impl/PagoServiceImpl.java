package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.PagoRequest;
import com.codesoftlabs.tusalud.app.dto.response.PagoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Cupon;
import com.codesoftlabs.tusalud.app.entities.MetodoPago;
import com.codesoftlabs.tusalud.app.entities.Pago;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPago;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.MetodoPagoRepository;
import com.codesoftlabs.tusalud.app.repositories.PagoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CuponService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.PagoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PagoServiceImpl implements PagoService {

    private static final Set<EstadoCita> ESTADOS_PAGABLES = EnumSet.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final LocalDate INICIO_POR_DEFECTO = LocalDate.of(2000, 1, 1);
    private static final LocalDate FIN_POR_DEFECTO = LocalDate.of(9999, 12, 31);

    private final CitaRepository citaRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final PagoRepository pagoRepository;
    private final CuponService cuponService;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public PagoResponse pagarCita(Long citaId, PagoRequest request, UsuarioPrincipal actor) {
        Cita cita = citaRepository
                .findByIdWithDetails(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", citaId));
        if (!cita.getPaciente().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Solo el paciente de la cita puede pagarla");
        }
        if (!ESTADOS_PAGABLES.contains(cita.getEstado())) {
            throw new BusinessLogicException("No se puede pagar una cita en estado " + cita.getEstado());
        }
        if (cita.getPago() != null) {
            throw new ConflictException("La cita ya tiene un pago registrado");
        }

        MetodoPago metodo = metodoPagoRepository
                .findByIdAndUsuarioId(request.metodoPagoId(), actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago", "id", request.metodoPagoId()));
        if (metodo.estaVencida(YearMonth.now())) {
            throw new BusinessLogicException("El método de pago seleccionado está vencido");
        }

        BigDecimal monto = cita.getMonto();
        BigDecimal descuento = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String codigoCupon = null;
        if (request.codigoCupon() != null && !request.codigoCupon().isBlank()) {
            Cupon cupon = cuponService.consumir(request.codigoCupon());
            descuento = cupon.calcularDescuento(monto);
            codigoCupon = cupon.getCodigo();
        }
        BigDecimal total = monto.subtract(descuento).setScale(2, RoundingMode.HALF_UP);

        Pago pago = pagoRepository.save(Pago.builder()
                .cita(cita)
                .metodoPago(metodo)
                .metodoDescripcion(metodo.descripcion())
                .monto(monto)
                .descuento(descuento)
                .total(total)
                .codigoCupon(codigoCupon)
                .estado(EstadoPago.PAGADO)
                .comprobante(siguienteComprobante())
                .fechaPago(LocalDateTime.now())
                .build());
        cita.setPago(pago);

        notificacionService.crear(
                cita.getPaciente().getUsuario(),
                TipoNotificacion.PAGO_REGISTRADO,
                "Pago registrado",
                "Se registró el pago de S/ " + total.toPlainString() + " por su cita del "
                        + cita.getFechaHora().format(FORMATO_FECHA) + ". Comprobante " + pago.getComprobante() + ".",
                "/citas/" + cita.getId());
        log.info("Pago {} registrado para la cita {} por {}", pago.getComprobante(), cita.getId(), actor.getEmail());
        return PagoResponse.from(pago);
    }

    @Override
    @Transactional
    public Pago registrarPagoBilletera(
            Cita cita, String descripcion, BigDecimal monto, BigDecimal descuento, String codigoCupon) {
        if (cita.getPago() != null) {
            throw new ConflictException("La cita ya tiene un pago registrado");
        }
        if (codigoCupon != null) {
            cuponService.consumir(codigoCupon);
        }
        BigDecimal total = monto.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        Pago pago = pagoRepository.save(Pago.builder()
                .cita(cita)
                .metodoDescripcion(descripcion)
                .monto(monto)
                .descuento(descuento)
                .total(total)
                .codigoCupon(codigoCupon)
                .estado(EstadoPago.PAGADO)
                .comprobante(siguienteComprobante())
                .fechaPago(LocalDateTime.now())
                .build());
        cita.setPago(pago);
        notificacionService.crear(
                cita.getPaciente().getUsuario(),
                TipoNotificacion.PAGO_REGISTRADO,
                "Pago registrado",
                "Se registró el pago de S/ " + total.toPlainString() + " con " + descripcion + " por su cita del "
                        + cita.getFechaHora().format(FORMATO_FECHA) + ". Comprobante " + pago.getComprobante() + ".",
                "/citas/" + cita.getId());
        log.info("Pago {} con {} registrado para la cita {}", pago.getComprobante(), descripcion, cita.getId());
        return pago;
    }

    @Override
    public List<PagoResponse> listarMisPagos(UsuarioPrincipal actor) {
        return pagoRepository.findDelPaciente(actor.getId()).stream()
                .map(PagoResponse::from)
                .toList();
    }

    @Override
    public PagoResponse obtenerMiPago(UsuarioPrincipal actor, Long id) {
        Pago pago = pagoRepository
                .findByIdConDetalle(id)
                .filter(p -> p.getCita().getPaciente().getUsuario().getId().equals(actor.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Pago", "id", id));
        return PagoResponse.from(pago);
    }

    @Override
    public List<PagoResponse> listarTodos(LocalDate inicio, LocalDate fin) {
        LocalDate desde = inicio != null ? inicio : INICIO_POR_DEFECTO;
        LocalDate hasta = fin != null ? fin : FIN_POR_DEFECTO;
        if (desde.isAfter(hasta)) {
            throw new BusinessLogicException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        return pagoRepository
                .findEntreFechas(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
                .stream()
                .map(PagoResponse::from)
                .toList();
    }

    private String siguienteComprobante() {
        return String.format("B001-%06d", pagoRepository.count() + 1);
    }
}

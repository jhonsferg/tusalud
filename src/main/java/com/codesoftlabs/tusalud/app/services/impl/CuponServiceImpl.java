package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.CuponRequest;
import com.codesoftlabs.tusalud.app.dto.response.CuponResponse;
import com.codesoftlabs.tusalud.app.dto.response.ValidacionCuponResponse;
import com.codesoftlabs.tusalud.app.entities.Cupon;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCupon;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CuponRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.CuponService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuponServiceImpl implements CuponService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final CuponRepository cuponRepository;

    @Override
    public List<CuponResponse> listar() {
        return cuponRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(CuponResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public CuponResponse crear(CuponRequest request) {
        String codigo = normalizar(request.codigo());
        if (cuponRepository.existsByCodigo(codigo)) {
            throw new ConflictException("Ya existe un cupón con el código " + codigo);
        }
        Cupon cupon = Cupon.builder().codigo(codigo).build();
        aplicar(cupon, request);
        return CuponResponse.from(cuponRepository.save(cupon));
    }

    @Override
    @Transactional
    public CuponResponse actualizar(UUID id, CuponRequest request) {
        Cupon cupon = buscar(id);
        String codigo = normalizar(request.codigo());
        if (cuponRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new ConflictException("Ya existe un cupón con el código " + codigo);
        }
        cupon.setCodigo(codigo);
        aplicar(cupon, request);
        return CuponResponse.from(cupon);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        Cupon cupon = buscar(id);
        if (cupon.getUsosActuales() > 0) {
            throw new ConflictException("No se puede eliminar un cupón que ya fue utilizado");
        }
        cuponRepository.delete(cupon);
    }

    @Override
    public ValidacionCuponResponse validar(String codigo, BigDecimal monto) {
        String normalizado = codigo == null ? "" : normalizar(codigo);
        if (normalizado.isBlank()) {
            return invalido("", "Ingrese un código de cupón", monto);
        }
        Optional<Cupon> encontrado = cuponRepository.findByCodigo(normalizado);
        if (encontrado.isEmpty()) {
            return invalido(normalizado, "El cupón no existe", monto);
        }
        String motivo = encontrado.get().motivoNoVigente(LocalDate.now());
        if (motivo != null) {
            return invalido(normalizado, motivo, monto);
        }
        BigDecimal descuento = encontrado.get().calcularDescuento(monto);
        BigDecimal total = monto.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        return new ValidacionCuponResponse(normalizado, true, "Cupón aplicado correctamente", descuento, total);
    }

    @Override
    @Transactional
    public Cupon consumir(String codigo) {
        String normalizado = normalizar(codigo);
        Cupon cupon = cuponRepository
                .findByCodigo(normalizado)
                .orElseThrow(() -> new BusinessLogicException("El cupón " + normalizado + " no existe"));
        String motivo = cupon.motivoNoVigente(LocalDate.now());
        if (motivo != null) {
            throw new BusinessLogicException(motivo);
        }
        cupon.setUsosActuales(cupon.getUsosActuales() + 1);
        return cupon;
    }

    private Cupon buscar(UUID id) {
        return cuponRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cupón", "id", id));
    }

    private void aplicar(Cupon cupon, CuponRequest request) {
        if (request.fechaFin().isBefore(request.fechaInicio())) {
            throw new DatosInvalidosException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        if (request.tipo() == TipoCupon.PORCENTAJE
                && (request.valor().compareTo(BigDecimal.ONE) < 0
                        || request.valor().compareTo(CIEN) > 0)) {
            throw new DatosInvalidosException("El porcentaje del cupón debe estar entre 1 y 100");
        }
        if (request.usosMaximos() != null && cupon.getUsosActuales() > request.usosMaximos()) {
            throw new DatosInvalidosException("Los usos máximos no pueden ser menores a los usos ya registrados ("
                    + cupon.getUsosActuales() + ")");
        }
        cupon.setDescripcion(vacioANulo(request.descripcion()));
        cupon.setTipo(request.tipo());
        cupon.setValor(request.valor().setScale(2, RoundingMode.HALF_UP));
        cupon.setFechaInicio(request.fechaInicio());
        cupon.setFechaFin(request.fechaFin());
        cupon.setUsosMaximos(request.usosMaximos());
        cupon.setActivo(request.activo() == null || request.activo());
    }

    private static ValidacionCuponResponse invalido(String codigo, String mensaje, BigDecimal monto) {
        return new ValidacionCuponResponse(
                codigo, false, mensaje, BigDecimal.ZERO.setScale(2), monto.setScale(2, RoundingMode.HALF_UP));
    }

    private static String normalizar(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}

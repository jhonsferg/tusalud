package com.codesoftlabs.tusalud.app.services.seguros;

import com.codesoftlabs.tusalud.app.entities.Comprobante;
import com.codesoftlabs.tusalud.app.entities.CuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Poliza;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.repositories.CuentaAseguradoraRepository;
import com.codesoftlabs.tusalud.app.repositories.PolizaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoberturaCobroService {

    private final PolizaRepository polizaRepository;
    private final CuentaAseguradoraRepository cuentaRepository;
    private final CoberturaCalculadora calculadora;

    public record Desglose(Poliza poliza, CoberturaCalculadora.Resultado resultado) {

        public boolean cubre() {
            return resultado.cubierto().signum() > 0;
        }
    }

    public Optional<Desglose> evaluar(
            Paciente paciente, CategoriaServicio categoria, List<CoberturaCalculadora.Linea> lineas) {
        LocalDate hoy = LocalDate.now();
        for (Poliza poliza : polizaRepository.usablesDelPaciente(paciente.getId(), hoy)) {
            if (!poliza.usableEn(hoy)) {
                continue;
            }
            BigDecimal consumido = cuentaRepository.consumido(
                    paciente.getId(),
                    poliza.getConvenio().getId(),
                    hoy.withDayOfYear(1).atStartOfDay(),
                    hoy.withDayOfYear(1).plusYears(1).atStartOfDay());
            return Optional.of(
                    new Desglose(poliza, calculadora.calcular(poliza.getConvenio(), categoria, lineas, consumido)));
        }
        return Optional.empty();
    }

    @Transactional
    public CuentaAseguradora registrarCuenta(
            Comprobante comprobante,
            Paciente paciente,
            Desglose desglose,
            CategoriaServicio categoria,
            OrigenComprobante origen,
            UUID referenciaId,
            String descripcion) {
        Poliza poliza = desglose.poliza();
        return cuentaRepository.save(CuentaAseguradora.builder()
                .aseguradora(poliza.getConvenio().getAseguradora())
                .convenio(poliza.getConvenio())
                .comprobante(comprobante)
                .paciente(paciente)
                .categoria(categoria)
                .origen(origen)
                .referenciaId(referenciaId)
                .descripcion(descripcion.length() > 200 ? descripcion.substring(0, 200) : descripcion)
                .montoTotal(desglose.resultado().total())
                .montoCubierto(desglose.resultado().cubierto())
                .copago(desglose.resultado().copago())
                .polizaNumero(poliza.getNumero())
                .fecha(LocalDateTime.now())
                .estado(EstadoCuentaAseguradora.PENDIENTE)
                .build());
    }

    public void exigirRevertible(Comprobante comprobante) {
        cuentaRepository.findByComprobante(comprobante.getId()).ifPresent(cuenta -> {
            if (cuenta.getEstado() != EstadoCuentaAseguradora.PENDIENTE
                    && cuenta.getEstado() != EstadoCuentaAseguradora.REVERTIDA) {
                throw new BusinessLogicException(
                        "El comprobante tiene una cuenta ya liquidada con la aseguradora y no puede anularse");
            }
        });
    }

    @Transactional
    public void revertir(Comprobante comprobante) {
        cuentaRepository.findByComprobante(comprobante.getId()).ifPresent(cuenta -> {
            if (cuenta.getEstado() == EstadoCuentaAseguradora.PENDIENTE) {
                cuenta.setEstado(EstadoCuentaAseguradora.REVERTIDA);
            }
        });
    }
}

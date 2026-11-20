package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.AsientoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.LineaAsientoRequest;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.LibroElectronico;
import com.codesoftlabs.tusalud.app.entities.CuentaContable;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaContable;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.repositories.AsientoRepository;
import com.codesoftlabs.tusalud.app.repositories.CuentaContableRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.contabilidad.AsientoService;
import com.codesoftlabs.tusalud.app.services.contabilidad.LibrosElectronicosService;
import com.codesoftlabs.tusalud.app.services.contabilidad.LibrosElectronicosService.Libro;
import com.codesoftlabs.tusalud.app.services.contabilidad.OrigenesContables;
import com.codesoftlabs.tusalud.app.services.contabilidad.PeriodoContableService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class LibrosElectronicosTest {

    @Autowired
    private AsientoRepository asientoRepository;

    @Autowired
    private AsientoService asientos;

    @Autowired
    private PeriodoContableService periodos;

    @Autowired
    private CuentaContableRepository cuentas;

    @Autowired
    private ServiceTestFixture fixture;

    private OrigenesContables origenes(boolean conEmisor) {
        return new OrigenesContables() {
            @Override
            public List<CompraContable> comprasEntre(LocalDate desde, LocalDate hasta) {
                return List.of(new CompraContable(
                        UUID.randomUUID(),
                        "FACTURA",
                        "F001",
                        "123",
                        desde.plusDays(2),
                        desde.plusDays(32),
                        "20512345678",
                        "Biomédica | del Perú",
                        new BigDecimal("100.00"),
                        new BigDecimal("18.00"),
                        new BigDecimal("118.00")));
            }

            @Override
            public List<VencimientoPorPagar> porPagar() {
                return List.of();
            }

            @Override
            public List<VencimientoPorCobrar> porCobrar(int plazoDias) {
                return List.of();
            }

            @Override
            public Optional<Emisor> emisor() {
                return conEmisor ? Optional.of(new Emisor("20100070970", "TuSalud")) : Optional.empty();
            }
        };
    }

    private CuentaContable cuenta(String codigo, TipoCuentaContable tipo) {
        return cuentas.save(CuentaContable.builder()
                .codigo(codigo)
                .nombre("Cuenta " + codigo)
                .tipo(tipo)
                .naturaleza(tipo.naturalezaPorDefecto())
                .aceptaMovimientos(true)
                .activa(true)
                .creadaEn(LocalDateTime.now())
                .build());
    }

    @Test
    void sinConfiguracionFiscalNoSeGeneranLosLibros() {
        LibrosElectronicosService servicio = new LibrosElectronicosService(asientoRepository, origenes(false));

        assertThatThrownBy(() -> servicio.generar(Libro.DIARIO, 2026, 3)).isInstanceOf(ConflictException.class);
    }

    @Test
    void elDiarioYElMayorSalenEnTextoPlanoSeparadoPorBarras() {
        UsuarioPrincipal finanzas = fixture.principal(fixture.usuario("le", RolNombre.ROLE_FINANZAS));
        LocalDate hoy = LocalDate.now();
        periodos.abrir(finanzas, hoy.getYear(), hoy.getMonthValue());
        CuentaContable caja = cuenta("1011", TipoCuentaContable.ACTIVO);
        CuentaContable capital = cuenta("5011", TipoCuentaContable.PATRIMONIO);
        var borrador = asientos.crear(
                finanzas,
                new AsientoRequest(
                        hoy,
                        "Aporte | inicial",
                        List.of(
                                new LineaAsientoRequest(caja.getId(), new BigDecimal("100.00"), null, null),
                                new LineaAsientoRequest(capital.getId(), null, new BigDecimal("100.00"), null))));
        asientos.contabilizar(finanzas, borrador.id());
        LibrosElectronicosService servicio = new LibrosElectronicosService(asientoRepository, origenes(true));

        LibroElectronico diario = servicio.generar(Libro.DIARIO, hoy.getYear(), hoy.getMonthValue());
        LibroElectronico mayor = servicio.generar(Libro.MAYOR, hoy.getYear(), hoy.getMonthValue());

        assertThat(diario.estado()).isEqualTo("POR_VALIDAR");
        assertThat(diario.registros()).isEqualTo(2);
        assertThat(diario.nombreArchivo())
                .isEqualTo(
                        String.format("LE20100070970%04d%02d00050100001111.txt", hoy.getYear(), hoy.getMonthValue()));
        String[] filas = diario.contenido().split("\n");
        assertThat(filas).hasSize(2);
        assertThat(filas[0]).startsWith(String.format("%04d%02d00|ASI-", hoy.getYear(), hoy.getMonthValue()));
        assertThat(filas[0]).contains("|1011|").contains("Aporte / inicial").endsWith("|");
        assertThat(filas[0]).contains("|100.00|0.00|");
        assertThat(mayor.registros()).isEqualTo(2);
        assertThat(mayor.nombreArchivo()).contains("060100");
    }

    @Test
    void elRegistroDeComprasListaLosComprobantesDelMes() {
        LibrosElectronicosService servicio = new LibrosElectronicosService(asientoRepository, origenes(true));

        LibroElectronico compras = servicio.generar(Libro.COMPRAS, 2026, 3);

        assertThat(compras.registros()).isEqualTo(1);
        assertThat(compras.contenido())
                .contains("|01|F001||123|6|20512345678|Biomédica / del Perú|100.00|18.00|118.00|PEN|1|");
        assertThat(compras.nombreArchivo()).contains("080100");
    }
}

package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.AsientoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.ConciliarRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.CuentaTesoreriaRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.ImportarExtractoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.LineaAsientoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.MovimientoTesoreriaRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.PlantillaAsientoRequest;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.AsientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.AsientoResumen;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.BalanceComprobacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.CuentaTesoreriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.PeriodoResponse;
import com.codesoftlabs.tusalud.app.entities.CuentaContable;
import com.codesoftlabs.tusalud.app.entities.PlantillaAsiento;
import com.codesoftlabs.tusalud.app.entities.PlantillaAsientoLinea;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.MetodoPagoProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.ConceptoMonto;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoLineaExtracto;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoPeriodo;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EventoContable;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.LadoAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.NaturalezaCuenta;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaContable;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaTesoreria;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoMovimientoTesoreria;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.CuentaContableRepository;
import com.codesoftlabs.tusalud.app.repositories.PlantillaAsientoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.contabilidad.AsientoService;
import com.codesoftlabs.tusalud.app.services.contabilidad.AsientosPendientesService;
import com.codesoftlabs.tusalud.app.services.contabilidad.ConciliacionService;
import com.codesoftlabs.tusalud.app.services.contabilidad.Contabilizador;
import com.codesoftlabs.tusalud.app.services.contabilidad.FlujoCajaService;
import com.codesoftlabs.tusalud.app.services.contabilidad.LibrosContablesService;
import com.codesoftlabs.tusalud.app.services.contabilidad.PeriodoContableService;
import com.codesoftlabs.tusalud.app.services.contabilidad.PlantillaAsientoService;
import com.codesoftlabs.tusalud.app.services.contabilidad.TesoreriaService;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CobroAnulado;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CobroRegistrado;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CompraAnulada;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CompraRegistrada;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.PagoProveedorRegistrado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class ContabilidadServiceTest {

    @Autowired
    private AsientoService asientos;

    @Autowired
    private PeriodoContableService periodos;

    @Autowired
    private Contabilizador contabilizador;

    @Autowired
    private LibrosContablesService libros;

    @Autowired
    private TesoreriaService tesoreria;

    @Autowired
    private ConciliacionService conciliacion;

    @Autowired
    private FlujoCajaService flujo;

    @Autowired
    private PlantillaAsientoService plantillaService;

    @Autowired
    private AsientosPendientesService pendientes;

    @Autowired
    private CuentaContableRepository cuentas;

    @Autowired
    private PlantillaAsientoRepository plantillas;

    @Autowired
    private ServiceTestFixture fixture;

    private UsuarioPrincipal finanzas;
    private UsuarioPrincipal gerencia;
    private CuentaContable caja;
    private CuentaContable bancos;
    private CuentaContable cxcAseguradora;
    private CuentaContable proveedores;
    private CuentaContable igv;
    private CuentaContable compras;
    private CuentaContable ingresos;
    private CuentaContable capital;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void setUp() {
        finanzas = fixture.principal(fixture.usuario("fin", RolNombre.ROLE_FINANZAS));
        gerencia = fixture.principal(fixture.usuario("ger", RolNombre.ROLE_GERENCIA));
        caja = cuenta("1011", "Caja", TipoCuentaContable.ACTIVO);
        bancos = cuenta("1041", "Bancos", TipoCuentaContable.ACTIVO);
        cxcAseguradora = cuenta("1213", "Por cobrar a aseguradoras", TipoCuentaContable.ACTIVO);
        proveedores = cuenta("4212", "Facturas por pagar", TipoCuentaContable.PASIVO);
        igv = cuenta("40111", "IGV", TipoCuentaContable.PASIVO);
        compras = cuenta("6011", "Compras", TipoCuentaContable.GASTO);
        ingresos = cuenta("7041", "Servicios de salud", TipoCuentaContable.INGRESO);
        capital = cuenta("5011", "Capital social", TipoCuentaContable.PATRIMONIO);
        cuentas.save(CuentaContable.builder()
                .codigo("10")
                .nombre("Efectivo")
                .tipo(TipoCuentaContable.ACTIVO)
                .naturaleza(NaturalezaCuenta.DEUDORA)
                .aceptaMovimientos(false)
                .activa(true)
                .creadaEn(java.time.LocalDateTime.now())
                .build());
        plantilla(
                EventoContable.COBRO_EFECTIVO,
                linea(LadoAsiento.DEBE, ConceptoMonto.TOTAL, caja),
                linea(LadoAsiento.DEBE, ConceptoMonto.CUBIERTO, cxcAseguradora),
                linea(LadoAsiento.HABER, ConceptoMonto.INGRESO_BRUTO, ingresos),
                linea(LadoAsiento.HABER, ConceptoMonto.IGV, igv));
        plantilla(
                EventoContable.COBRO_ELECTRONICO,
                linea(LadoAsiento.DEBE, ConceptoMonto.TOTAL, bancos),
                linea(LadoAsiento.DEBE, ConceptoMonto.CUBIERTO, cxcAseguradora),
                linea(LadoAsiento.HABER, ConceptoMonto.INGRESO_BRUTO, ingresos),
                linea(LadoAsiento.HABER, ConceptoMonto.IGV, igv));
        plantilla(
                EventoContable.COMPRA_PROVEEDOR,
                linea(LadoAsiento.DEBE, ConceptoMonto.SUBTOTAL, compras),
                linea(LadoAsiento.DEBE, ConceptoMonto.IGV, igv),
                linea(LadoAsiento.HABER, ConceptoMonto.TOTAL, proveedores));
        plantilla(
                EventoContable.PAGO_PROVEEDOR_EFECTIVO,
                linea(LadoAsiento.DEBE, ConceptoMonto.MONTO, proveedores),
                linea(LadoAsiento.HABER, ConceptoMonto.MONTO, caja));
        plantilla(
                EventoContable.PAGO_PROVEEDOR_BANCO,
                linea(LadoAsiento.DEBE, ConceptoMonto.MONTO, proveedores),
                linea(LadoAsiento.HABER, ConceptoMonto.MONTO, bancos));
    }

    private CuentaContable cuenta(String codigo, String nombre, TipoCuentaContable tipo) {
        return cuentas.save(CuentaContable.builder()
                .codigo(codigo)
                .nombre(nombre)
                .tipo(tipo)
                .naturaleza(tipo.naturalezaPorDefecto())
                .aceptaMovimientos(true)
                .activa(true)
                .sistema(true)
                .creadaEn(java.time.LocalDateTime.now())
                .build());
    }

    private record Plano(LadoAsiento lado, ConceptoMonto concepto, CuentaContable cuenta) {}

    private static Plano linea(LadoAsiento lado, ConceptoMonto concepto, CuentaContable cuenta) {
        return new Plano(lado, concepto, cuenta);
    }

    private void plantilla(EventoContable evento, Plano... lineas) {
        PlantillaAsiento plantilla = PlantillaAsiento.builder()
                .evento(evento)
                .nombre(evento.name())
                .activa(true)
                .build();
        int posicion = 0;
        for (Plano p : lineas) {
            plantilla
                    .getLineas()
                    .add(PlantillaAsientoLinea.builder()
                            .plantilla(plantilla)
                            .posicion(posicion++)
                            .lado(p.lado())
                            .concepto(p.concepto())
                            .cuenta(p.cuenta())
                            .build());
        }
        plantillas.save(plantilla);
    }

    private PeriodoResponse abrirPeriodoActual() {
        return periodos.abrir(finanzas, hoy.getYear(), hoy.getMonthValue());
    }

    private LineaAsientoRequest debe(CuentaContable cuenta, String monto) {
        return new LineaAsientoRequest(cuenta.getId(), new BigDecimal(monto), null, null);
    }

    private LineaAsientoRequest haber(CuentaContable cuenta, String monto) {
        return new LineaAsientoRequest(cuenta.getId(), null, new BigDecimal(monto), null);
    }

    private AsientoResponse asientoContabilizado(String glosa, String monto, CuentaContable d, CuentaContable h) {
        AsientoResponse borrador =
                asientos.crear(finanzas, new AsientoRequest(hoy, glosa, List.of(debe(d, monto), haber(h, monto))));
        return asientos.contabilizar(finanzas, borrador.id());
    }

    private CobroRegistrado cobro(MetodoPagoCaja metodo, String subtotal, String igvMonto, String cubierto) {
        BigDecimal sub = new BigDecimal(subtotal);
        BigDecimal impuesto = new BigDecimal(igvMonto);
        return new CobroRegistrado(
                UUID.randomUUID(),
                "B001-" + ServiceTestFixture.siguiente(),
                hoy,
                metodo,
                sub,
                impuesto,
                sub.add(impuesto),
                cubierto == null ? null : new BigDecimal(cubierto));
    }

    @Test
    void unAsientoManualNaceEnBorradorYNoSeRegistraSinPeriodoAbierto() {
        AsientoRequest request =
                new AsientoRequest(hoy, "Aporte", List.of(debe(caja, "100.00"), haber(capital, "100.00")));

        assertThatThrownBy(() -> asientos.crear(finanzas, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Ábralo");

        abrirPeriodoActual();
        AsientoResponse creado = asientos.crear(finanzas, request);

        assertThat(creado.estado()).isEqualTo(EstadoAsiento.BORRADOR);
        assertThat(creado.numero()).isNull();
        assertThat(creado.totalDebe()).isEqualByComparingTo("100.00");
        assertThat(creado.lineas()).hasSize(2);
    }

    @Test
    void contabilizarExigeQueElAsientoCuadreYLoNumera() {
        abrirPeriodoActual();
        AsientoResponse descuadrado = asientos.crear(
                finanzas, new AsientoRequest(hoy, "Descuadre", List.of(debe(caja, "100.00"), haber(capital, "90.00"))));

        assertThatThrownBy(() -> asientos.contabilizar(finanzas, descuadrado.id()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no cuadra");

        AsientoResponse bueno = asientoContabilizado("Aporte", "100.00", caja, capital);
        assertThat(bueno.estado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(bueno.numero()).matches("ASI-" + hoy.getYear() + "-\\d{6}");
        assertThat(bueno.contabilizadoPor()).isNotBlank();
    }

    @Test
    void unaLineaNoPuedeTenerDebeYHaberAlMismoTiempo() {
        abrirPeriodoActual();
        LineaAsientoRequest mala =
                new LineaAsientoRequest(caja.getId(), new BigDecimal("5"), new BigDecimal("5"), null);

        assertThatThrownBy(() -> asientos.crear(
                        finanzas, new AsientoRequest(hoy, "Mala", List.of(mala, haber(capital, "5.00")))))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void unaCuentaQueNoAceptaMovimientosSeRechazaEnElAsiento() {
        abrirPeriodoActual();
        CuentaContable agregada = cuentas.findByCodigo("10").orElseThrow();

        assertThatThrownBy(() -> asientos.crear(
                        finanzas,
                        new AsientoRequest(hoy, "Agregada", List.of(debe(agregada, "5.00"), haber(capital, "5.00")))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no admite movimientos");
    }

    @Test
    void unAsientoContabilizadoEsInmutableYSeCorrigeConUnContraasiento() {
        abrirPeriodoActual();
        AsientoResponse original = asientoContabilizado("Aporte", "250.00", caja, capital);

        assertThatThrownBy(() -> asientos.eliminar(finanzas, original.id())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> asientos.actualizar(
                        finanzas,
                        original.id(),
                        new AsientoRequest(hoy, "Otro", List.of(debe(caja, "1.00"), haber(capital, "1.00")))))
                .isInstanceOf(ConflictException.class);

        AsientoResponse reversa = asientos.reversar(finanzas, original.id(), "Registrado por error");

        assertThat(reversa.origen()).isEqualTo(OrigenAsiento.REVERSA);
        assertThat(reversa.revierteAId()).isEqualTo(original.id());
        assertThat(reversa.lineas().get(0).haber()).isEqualByComparingTo("250.00");
        assertThat(reversa.lineas().get(1).debe()).isEqualByComparingTo("250.00");
        AsientoResponse refrescado = asientos.obtener(original.id());
        assertThat(refrescado.estado()).isEqualTo(EstadoAsiento.REVERTIDO);
        assertThat(refrescado.reversaId()).isEqualTo(reversa.id());

        assertThatThrownBy(() -> asientos.reversar(finanzas, original.id(), "Otra vez"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> asientos.reversar(finanzas, reversa.id(), "Reversa de reversa"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void elPeriodoNoSeCierraConBorradoresNiPorQuienRegistroAsientos() {
        PeriodoResponse periodo = abrirPeriodoActual();
        AsientoResponse borrador = asientos.crear(
                finanzas, new AsientoRequest(hoy, "Borrador", List.of(debe(caja, "10.00"), haber(capital, "10.00"))));

        assertThatThrownBy(() -> periodos.cerrar(gerencia, periodo.id()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("borrador");

        asientos.contabilizar(finanzas, borrador.id());

        assertThatThrownBy(() -> periodos.cerrar(finanzas, periodo.id()))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("no puede cerrarlo");

        PeriodoResponse cerrado = periodos.cerrar(gerencia, periodo.id());
        assertThat(cerrado.estado()).isEqualTo(EstadoPeriodo.CERRADO);
        assertThat(cerrado.cerradoPor()).isNotBlank();
    }

    @Test
    void unPeriodoCerradoRechazaAsientosYSePuedeReabrirConMotivo() {
        PeriodoResponse periodo = abrirPeriodoActual();
        periodos.cerrar(gerencia, periodo.id());

        assertThatThrownBy(() -> asientos.crear(
                        finanzas,
                        new AsientoRequest(hoy, "Tarde", List.of(debe(caja, "1.00"), haber(capital, "1.00")))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cerrado");
        assertThatThrownBy(() -> periodos.cerrar(gerencia, periodo.id())).isInstanceOf(ConflictException.class);

        PeriodoResponse reabierto = periodos.reabrir(gerencia, periodo.id(), "Corrección de cierre");
        assertThat(reabierto.estado()).isEqualTo(EstadoPeriodo.ABIERTO);
        assertThat(reabierto.motivoReapertura()).isEqualTo("Corrección de cierre");
        assertThat(asientos.crear(
                                finanzas,
                                new AsientoRequest(
                                        hoy, "Ya abierto", List.of(debe(caja, "1.00"), haber(capital, "1.00"))))
                        .estado())
                .isEqualTo(EstadoAsiento.BORRADOR);
    }

    @Test
    void noSeAbreDosVecesElMismoPeriodo() {
        abrirPeriodoActual();

        assertThatThrownBy(this::abrirPeriodoActual).isInstanceOf(ConflictException.class);
    }

    @Test
    void elCobroEnEfectivoGeneraUnAsientoBalanceadoYEsIdempotente() {
        CobroRegistrado hecho = cobro(MetodoPagoCaja.EFECTIVO, "100.00", "18.00", null);

        contabilizador.procesar(hecho);
        contabilizador.procesar(hecho);

        List<AsientoResumen> lista = asientos.listar(null, null, OrigenAsiento.COBRO, null, null, null);
        assertThat(lista).hasSize(1);
        AsientoResponse asiento = asientos.obtener(lista.get(0).id());
        assertThat(asiento.estado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(asiento.totalDebe()).isEqualByComparingTo("118.00");
        assertThat(asiento.totalHaber()).isEqualByComparingTo("118.00");
        assertThat(asiento.lineas()).extracting("cuentaCodigo").containsExactly("1011", "7041", "40111");
    }

    @Test
    void elCobroConCoberturaDeAseguradoraRegistraLaCuentaPorCobrar() {
        CobroRegistrado hecho = cobro(MetodoPagoCaja.EFECTIVO, "25.42", "4.58", "70.00");

        contabilizador.procesar(hecho);

        AsientoResponse asiento = asientos.obtener(asientos.listar(null, null, OrigenAsiento.COBRO, null, null, null)
                .get(0)
                .id());
        assertThat(asiento.lineas()).extracting("cuentaCodigo").containsExactly("1011", "1213", "7041", "40111");
        assertThat(asiento.totalDebe()).isEqualByComparingTo("100.00");
        assertThat(asiento.totalHaber()).isEqualByComparingTo("100.00");
    }

    @Test
    void anularUnCobroGeneraLaReversaDelAsiento() {
        CobroRegistrado hecho = cobro(MetodoPagoCaja.TARJETA, "100.00", "18.00", null);
        contabilizador.procesar(hecho);

        contabilizador.procesar(new CobroAnulado(hecho.comprobanteId(), hecho.codigo(), hoy, "Error de cobro"));

        List<AsientoResumen> cobros = asientos.listar(null, null, OrigenAsiento.COBRO, null, null, null);
        assertThat(cobros).extracting(AsientoResumen::estado).containsExactly(EstadoAsiento.REVERTIDO);
        assertThat(asientos.listar(null, null, OrigenAsiento.REVERSA, null, null, null))
                .hasSize(1);
        BalanceComprobacionResponse balance = libros.balanceComprobacion(hoy.withDayOfMonth(1), hoy);
        assertThat(balance.cuadra()).isTrue();
        assertThat(balance.totalDeudor()).isEqualByComparingTo("0.00");
    }

    @Test
    void laCompraYElPagoAlProveedorCierranLaCuentaPorPagar() {
        UUID comprobante = UUID.randomUUID();
        contabilizador.procesar(new CompraRegistrada(
                comprobante,
                "F001-77",
                "Biomédica del Perú",
                hoy,
                new BigDecimal("1000.00"),
                new BigDecimal("180.00"),
                new BigDecimal("1180.00")));
        contabilizador.procesar(new PagoProveedorRegistrado(
                UUID.randomUUID(),
                comprobante,
                "F001-77",
                "Biomédica del Perú",
                hoy,
                new BigDecimal("1180.00"),
                MetodoPagoProveedor.EFECTIVO));

        BalanceComprobacionResponse balance = libros.balanceComprobacion(hoy.withDayOfMonth(1), hoy);

        assertThat(balance.cuadra()).isTrue();
        assertThat(balance.filas().stream()
                        .filter(f -> f.codigo().equals("4212"))
                        .findFirst()
                        .orElseThrow()
                        .finalAcreedor())
                .isEqualByComparingTo("0.00");
        assertThat(balance.filas().stream()
                        .filter(f -> f.codigo().equals("1011"))
                        .findFirst()
                        .orElseThrow()
                        .finalAcreedor())
                .isEqualByComparingTo("1180.00");
    }

    @Test
    void anularLaCompraDelProveedorReviertaSuAsiento() {
        UUID comprobante = UUID.randomUUID();
        contabilizador.procesar(new CompraRegistrada(
                comprobante,
                "F001-80",
                "Proveedor",
                hoy,
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                new BigDecimal("118.00")));

        contabilizador.procesar(new CompraAnulada(comprobante, "F001-80", hoy, "Duplicado"));

        assertThat(asientos.listar(null, EstadoAsiento.REVERTIDO, OrigenAsiento.COMPRA_PROVEEDOR, null, null, null))
                .hasSize(1);
    }

    @Test
    void unaPlantillaInactivaImpideContabilizarYDejaElMotivo() {
        PlantillaAsiento plantilla =
                plantillas.conLineas(EventoContable.COBRO_EFECTIVO).orElseThrow();
        plantilla.setActiva(false);

        assertThatThrownBy(() -> contabilizador.procesar(cobro(MetodoPagoCaja.EFECTIVO, "10.00", "1.80", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("inactiva");
    }

    @Test
    void elMayorAcumulaElSaldoConLaNaturalezaDeLaCuenta() {
        abrirPeriodoActual();
        asientoContabilizado("Aporte inicial", "500.00", caja, capital);
        asientoContabilizado("Segundo aporte", "300.00", caja, capital);

        var mayorCaja = libros.mayor(caja.getId(), hoy.withDayOfMonth(1), hoy);
        var mayorCapital = libros.mayor(capital.getId(), hoy.withDayOfMonth(1), hoy);

        assertThat(mayorCaja.movimientos()).hasSize(2);
        assertThat(mayorCaja.saldoFinal()).isEqualByComparingTo("800.00");
        assertThat(mayorCaja.movimientos().get(0).saldo()).isEqualByComparingTo("500.00");
        assertThat(mayorCapital.saldoFinal()).isEqualByComparingTo("800.00");
    }

    @Test
    void elBalanceDeComprobacionCuadraYAgrupaLasCuentasPadre() {
        abrirPeriodoActual();
        asientoContabilizado("Aporte", "500.00", caja, capital);
        asientoContabilizado("Depósito", "200.00", bancos, caja);

        BalanceComprobacionResponse balance = libros.balanceComprobacion(hoy.withDayOfMonth(1), hoy);

        assertThat(balance.cuadra()).isTrue();
        assertThat(balance.totalDebe()).isEqualByComparingTo("700.00");
        assertThat(balance.totalHaber()).isEqualByComparingTo("700.00");
        var padre = balance.filas().stream()
                .filter(f -> f.codigo().equals("10"))
                .findFirst()
                .orElseThrow();
        assertThat(padre.agregada()).isTrue();
        assertThat(padre.finalDeudor()).isEqualByComparingTo("500.00");
    }

    @Test
    void elEstadoDeSituacionYElDeResultadosCuadran() {
        abrirPeriodoActual();
        asientoContabilizado("Aporte", "1000.00", caja, capital);
        contabilizador.procesar(cobro(MetodoPagoCaja.EFECTIVO, "100.00", "18.00", null));
        contabilizador.procesar(new CompraRegistrada(
                UUID.randomUUID(),
                "F001-90",
                "Proveedor",
                hoy,
                new BigDecimal("40.00"),
                new BigDecimal("7.20"),
                new BigDecimal("47.20")));

        var situacion = libros.estadoSituacion(hoy);
        var resultados = libros.estadoResultados(hoy.withDayOfMonth(1), hoy);

        assertThat(situacion.cuadra()).isTrue();
        assertThat(situacion.activo().total()).isEqualByComparingTo("1118.00");
        assertThat(resultados.ingresos().total()).isEqualByComparingTo("100.00");
        assertThat(resultados.gastos().total()).isEqualByComparingTo("40.00");
        assertThat(resultados.resultado()).isEqualByComparingTo("60.00");
        assertThat(situacion.patrimonio().lineas()).extracting("nombre").contains("Resultado del ejercicio");
    }

    @Test
    void elRangoInvalidoSeRechaza() {
        assertThatThrownBy(() -> libros.balanceComprobacion(hoy, hoy.minusDays(1)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void lasPlantillasDeAsientoSeConfiguranSoloConCuentasValidas() {
        PlantillaAsiento plantilla =
                plantillas.conLineas(EventoContable.PAGO_PROVEEDOR_EFECTIVO).orElseThrow();
        UUID linea = plantilla.getLineas().get(1).getId();
        CuentaContable inactiva = cuenta("1099", "Inactiva", TipoCuentaContable.ACTIVO);
        inactiva.setActiva(false);

        assertThatThrownBy(() ->
                        plantillaService.actualizar(
                                finanzas,
                                plantilla.getId(),
                                new PlantillaAsientoRequest(
                                        "Pago",
                                        true,
                                        List.of(new com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests
                                                .CuentaLineaPlantillaRequest(linea, inactiva.getId())))))
                .isInstanceOf(ConflictException.class);

        var actualizada = plantillaService.actualizar(
                finanzas,
                plantilla.getId(),
                new PlantillaAsientoRequest(
                        "Pago desde bancos",
                        true,
                        List.of(
                                new com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests
                                        .CuentaLineaPlantillaRequest(linea, bancos.getId()))));
        assertThat(actualizada.nombre()).isEqualTo("Pago desde bancos");
        assertThat(actualizada.lineas().get(1).cuentaCodigo()).isEqualTo("1041");
    }

    private CuentaTesoreriaResponse cuentaBancaria(boolean predeterminada) {
        return tesoreria.crearCuenta(
                finanzas,
                new CuentaTesoreriaRequest(
                        "BCP " + ServiceTestFixture.siguiente(),
                        TipoCuentaTesoreria.BANCO,
                        "BCP",
                        "1234",
                        bancos.getId(),
                        new BigDecimal("1000.00"),
                        predeterminada,
                        null));
    }

    @Test
    void unMovimientoManualDeTesoreriaGeneraSuAsientoYMueveElSaldo() {
        CuentaTesoreriaResponse banco = cuentaBancaria(true);

        var movimiento = tesoreria.registrar(
                finanzas,
                new MovimientoTesoreriaRequest(
                        banco.id(),
                        hoy,
                        TipoMovimientoTesoreria.INGRESO,
                        new BigDecimal("250.00"),
                        "Aporte de socios",
                        "OP-1",
                        capital.getId()));

        assertThat(movimiento.asientoId()).isNotNull();
        assertThat(tesoreria.cuentas(false).stream()
                        .filter(c -> c.id().equals(banco.id()))
                        .findFirst()
                        .orElseThrow()
                        .saldo())
                .isEqualByComparingTo("1250.00");
        AsientoResponse asiento = asientos.obtener(movimiento.asientoId());
        assertThat(asiento.origen()).isEqualTo(OrigenAsiento.MOVIMIENTO_TESORERIA);
        assertThat(asiento.lineas().get(0).cuentaCodigo()).isEqualTo("1041");
    }

    @Test
    void laContrapartidaNoPuedeSerLaMismaCuentaDeLaTesoreria() {
        CuentaTesoreriaResponse banco = cuentaBancaria(false);

        assertThatThrownBy(() -> tesoreria.registrar(
                        finanzas,
                        new MovimientoTesoreriaRequest(
                                banco.id(),
                                hoy,
                                TipoMovimientoTesoreria.EGRESO,
                                new BigDecimal("10.00"),
                                "Mal",
                                null,
                                bancos.getId())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void soloUnaCuentaBancariaEsPredeterminada() {
        CuentaTesoreriaResponse primera = cuentaBancaria(true);
        CuentaTesoreriaResponse segunda = cuentaBancaria(true);

        List<CuentaTesoreriaResponse> todas = tesoreria.cuentas(false);

        assertThat(todas.stream()
                        .filter(CuentaTesoreriaResponse::predeterminada)
                        .map(CuentaTesoreriaResponse::id))
                .containsExactly(segunda.id());
        assertThat(primera.predeterminada()).isTrue();
    }

    @Test
    void losPagosPorBancoYLosCobrosElectronicosAlimentanLaTesoreriaPredeterminada() {
        CuentaTesoreriaResponse banco = cuentaBancaria(true);
        CobroRegistrado cobro = cobro(MetodoPagoCaja.YAPE, "100.00", "18.00", null);
        contabilizador.procesar(cobro);
        contabilizador.procesar(cobro);
        contabilizador.procesar(new PagoProveedorRegistrado(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "F001-5",
                "Proveedor",
                hoy,
                new BigDecimal("40.00"),
                MetodoPagoProveedor.TRANSFERENCIA));

        var movimientos = tesoreria.listarMovimientos(banco.id(), null, null, null);

        assertThat(movimientos).hasSize(2);
        assertThat(movimientos.stream().map(m -> m.tipo()))
                .containsExactlyInAnyOrder(TipoMovimientoTesoreria.INGRESO, TipoMovimientoTesoreria.EGRESO);
        assertThat(tesoreria.saldoTotal()).isEqualByComparingTo("1078.00");

        contabilizador.procesar(new CobroAnulado(cobro.comprobanteId(), cobro.codigo(), hoy, "Anulado"));
        assertThat(tesoreria.listarMovimientos(banco.id(), null, null, null)).hasSize(1);
    }

    private String extracto(String... lineas) {
        return "fecha,descripcion,referencia,monto\n" + String.join("\n", lineas);
    }

    @Test
    void laConciliacionImportaElExtractoSugiereCoincidenciasYConcilia() {
        CuentaTesoreriaResponse banco = cuentaBancaria(true);
        tesoreria.registrar(
                finanzas,
                new MovimientoTesoreriaRequest(
                        banco.id(),
                        hoy,
                        TipoMovimientoTesoreria.INGRESO,
                        new BigDecimal("250.00"),
                        "Depósito OP-777",
                        "OP-777",
                        capital.getId()));
        tesoreria.registrar(
                finanzas,
                new MovimientoTesoreriaRequest(
                        banco.id(),
                        hoy.minusDays(1),
                        TipoMovimientoTesoreria.EGRESO,
                        new BigDecimal("80.00"),
                        "Comisión",
                        null,
                        capital.getId()));

        var importado = conciliacion.importar(
                finanzas,
                new ImportarExtractoRequest(
                        banco.id(),
                        "extracto.csv",
                        extracto(
                                hoy + ",Abono OP-777,OP-777,250.00",
                                hoy.minusDays(1) + ",Cargo por comisión,,-80.00",
                                hoy + ",Cargo desconocido,,-15.50")));

        assertThat(importado.lineas()).isEqualTo(3);
        var sugerencias = conciliacion.sugerencias(banco.id());
        assertThat(sugerencias).hasSize(2);
        assertThat(sugerencias.get(0).puntaje())
                .isGreaterThan(sugerencias.get(1).puntaje());

        for (var sugerencia : sugerencias) {
            conciliacion.conciliar(
                    finanzas,
                    new ConciliarRequest(
                            sugerencia.linea().id(), sugerencia.movimiento().id()));
        }

        var resumen = conciliacion.resumen(banco.id());
        assertThat(resumen.lineasPendientes()).isEqualTo(1);
        assertThat(resumen.movimientosSinConciliar()).isZero();
        assertThat(conciliacion.lineas(banco.id(), EstadoLineaExtracto.CONCILIADA))
                .hasSize(2);

        var conciliada =
                conciliacion.lineas(banco.id(), EstadoLineaExtracto.CONCILIADA).get(0);
        var deshecha = conciliacion.deshacer(finanzas, conciliada.id());
        assertThat(deshecha.estado()).isEqualTo(EstadoLineaExtracto.PENDIENTE);
        assertThat(conciliacion.movimientosSinConciliar(banco.id())).hasSize(1);
    }

    @Test
    void importarDosVecesElMismoExtractoNoDuplicaLasLineas() {
        CuentaTesoreriaResponse banco = cuentaBancaria(true);
        String contenido = extracto(hoy + ",Abono,,10.00", hoy + ",Abono,,10.00", hoy + ",Cargo,,-5.00");

        var primero = conciliacion.importar(finanzas, new ImportarExtractoRequest(banco.id(), "a.csv", contenido));
        var segundo = conciliacion.importar(finanzas, new ImportarExtractoRequest(banco.id(), "a.csv", contenido));

        assertThat(primero.lineas()).isEqualTo(3);
        assertThat(segundo.lineas()).isZero();
        assertThat(segundo.duplicadas()).isEqualTo(3);
        assertThat(conciliacion.lineas(banco.id(), null)).hasSize(3);
    }

    @Test
    void elExtractoAceptaPuntoYComaYFechasLatinas() {
        CuentaTesoreriaResponse banco = cuentaBancaria(true);

        var importado = conciliacion.importar(
                finanzas,
                new ImportarExtractoRequest(
                        banco.id(),
                        "latino.csv",
                        "fecha;descripcion;referencia;monto\n05/03/2026;Abono cliente;REF1;1234.50\n06/03/2026;Cargo;;-20.00"));

        assertThat(importado.lineas()).isEqualTo(2);
        assertThat(importado.desde()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    void unExtractoConErroresSeRechazaCompletoConElNumeroDeLinea() {
        CuentaTesoreriaResponse banco = cuentaBancaria(true);

        assertThatThrownBy(() -> conciliacion.importar(
                        finanzas,
                        new ImportarExtractoRequest(
                                banco.id(), "malo.csv", extracto(hoy + ",Bien,,10.00", "no-es-fecha,Mal,,abc"))))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("línea 3");
        assertThat(conciliacion.lineas(banco.id(), null)).isEmpty();
    }

    @Test
    void noSeConcilianImportesDistintos() {
        CuentaTesoreriaResponse banco = cuentaBancaria(true);
        var movimiento = tesoreria.registrar(
                finanzas,
                new MovimientoTesoreriaRequest(
                        banco.id(),
                        hoy,
                        TipoMovimientoTesoreria.INGRESO,
                        new BigDecimal("100.00"),
                        "Depósito",
                        null,
                        capital.getId()));
        conciliacion.importar(
                finanzas, new ImportarExtractoRequest(banco.id(), "x.csv", extracto(hoy + ",Abono,,99.00")));
        var linea =
                conciliacion.lineas(banco.id(), EstadoLineaExtracto.PENDIENTE).get(0);

        assertThatThrownBy(() -> conciliacion.conciliar(finanzas, new ConciliarRequest(linea.id(), movimiento.id())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("importes");
    }

    @Test
    void elFlujoDeCajaProyectaElNumeroDeSemanasPedido() {
        cuentaBancaria(true);

        var proyeccion = flujo.proyectar(6);

        assertThat(proyeccion.detalle()).hasSize(6);
        assertThat(proyeccion.saldoInicial()).isEqualByComparingTo("1000.00");
        assertThatThrownBy(() -> flujo.proyectar(0)).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> flujo.proyectar(27)).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void unHechoQueFallaQuedaComoPendienteConSuMotivo() {
        UUID referencia = UUID.randomUUID();
        List<UUID> antes = new ArrayList<>();
        pendientes.listar(true).forEach(p -> antes.add(p.id()));

        pendientes.despachar(
                new CobroRegistrado(
                        referencia,
                        "B001-FALLA",
                        hoy,
                        MetodoPagoCaja.EFECTIVO,
                        new BigDecimal("1.00"),
                        new BigDecimal("0.18"),
                        new BigDecimal("1.18"),
                        null),
                referencia);

        var abiertos = pendientes.listar(true);
        assertThat(abiertos.stream().filter(p -> p.referenciaId().equals(referencia)))
                .hasSize(1)
                .allSatisfy(p -> {
                    assertThat(p.evento()).isEqualTo("CobroRegistrado");
                    assertThat(p.error()).isNotBlank();
                });
    }
}

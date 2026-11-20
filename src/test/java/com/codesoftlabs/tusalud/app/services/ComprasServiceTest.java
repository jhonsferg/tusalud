package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.ComprobanteProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.CotizacionItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.CotizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.DevolucionItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.DevolucionRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.OrdenCompraRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.OrdenItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.PagoProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.RecepcionItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.RecepcionRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.RequisicionItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.RequisicionRequest;
import com.codesoftlabs.tusalud.app.dto.request.ProductoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ComparativaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ComprobanteProveedorResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.CotizacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.OrdenCompraDetalle;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RecepcionDetalle;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RequisicionDetalle;
import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ProveedorResponse;
import com.codesoftlabs.tusalud.app.entities.Lote;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoOrdenCompra;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoRequisicion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.MetodoPagoProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.NivelAprobacion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.TipoComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.LoteRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.compras.ComprasAuditor;
import com.codesoftlabs.tusalud.app.services.compras.CotizacionService;
import com.codesoftlabs.tusalud.app.services.compras.CuentasPorPagarService;
import com.codesoftlabs.tusalud.app.services.compras.OrdenCompraService;
import com.codesoftlabs.tusalud.app.services.compras.RecepcionCompraService;
import com.codesoftlabs.tusalud.app.services.compras.ReportesComprasService;
import com.codesoftlabs.tusalud.app.services.compras.RequisicionService;
import com.codesoftlabs.tusalud.app.services.interfaces.InventarioService;
import com.codesoftlabs.tusalud.app.services.interfaces.ProductoService;
import com.codesoftlabs.tusalud.app.services.interfaces.ProveedorService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class ComprasServiceTest {

    @Autowired
    private RequisicionService requisiciones;

    @Autowired
    private CotizacionService cotizaciones;

    @Autowired
    private OrdenCompraService ordenes;

    @Autowired
    private RecepcionCompraService recepciones;

    @Autowired
    private CuentasPorPagarService cuentas;

    @Autowired
    private ReportesComprasService reportes;

    @Autowired
    private ComprasAuditor auditor;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private UsuarioPrincipal farmacia;
    private UsuarioPrincipal almacen;
    private UsuarioPrincipal otroAlmacen;
    private UsuarioPrincipal finanzas;
    private UsuarioPrincipal gerencia;

    @BeforeEach
    void setUp() {
        farmacia = fixture.principal(fixture.usuario("farm", RolNombre.ROLE_FARMACIA));
        almacen = fixture.principal(fixture.usuario("alm", RolNombre.ROLE_ALMACEN));
        otroAlmacen = fixture.principal(fixture.usuario("alm2", RolNombre.ROLE_ALMACEN));
        finanzas = fixture.principal(fixture.usuario("fin", RolNombre.ROLE_FINANZAS));
        gerencia = fixture.principal(fixture.usuario("ger", RolNombre.ROLE_GERENCIA));
    }

    private ProductoResponse producto(String nombre, int stockMinimo) {
        return productoService.crear(new ProductoRequest(
                "c-" + ServiceTestFixture.siguiente(),
                nombre,
                null,
                TipoProducto.MEDICAMENTO,
                "Principio",
                "Caja",
                "unidad",
                new BigDecimal("2.50"),
                stockMinimo,
                false,
                true));
    }

    private ProveedorResponse proveedor(String nombre, int condicionPago) {
        return proveedorService.crear(new ProveedorRequest(
                ServiceTestFixture.rucValido(),
                nombre,
                null,
                null,
                null,
                null,
                true,
                condicionPago,
                5,
                null,
                null,
                null));
    }

    private RequisicionDetalle requisicionEnviada(UsuarioPrincipal solicitante, UUID productoId, int cantidad) {
        RequisicionDetalle borrador = requisiciones.crear(
                solicitante,
                new RequisicionRequest(
                        null, "Reposición", false, List.of(new RequisicionItemRequest(productoId, cantidad, null))));
        return requisiciones.enviar(solicitante, borrador.resumen().id());
    }

    private OrdenCompraDetalle ordenAprobada(
            UsuarioPrincipal comprador,
            UsuarioPrincipal aprobador,
            UUID proveedorId,
            UUID productoId,
            int cantidad,
            String precio) {
        OrdenCompraDetalle orden = ordenes.crear(
                comprador,
                new OrdenCompraRequest(
                        proveedorId,
                        null,
                        null,
                        true,
                        30,
                        null,
                        null,
                        List.of(new OrdenItemRequest(productoId, cantidad, new BigDecimal(precio)))));
        ordenes.enviarAprobacion(comprador, orden.resumen().id());
        return ordenes.aprobar(aprobador, orden.resumen().id());
    }

    private RecepcionRequest recepcion(UUID ordenItemId, int recibida, int rechazada, String lote, String motivo) {
        return new RecepcionRequest(
                "T001-1",
                null,
                List.of(new RecepcionItemRequest(
                        ordenItemId,
                        recibida,
                        rechazada,
                        recibida > 0 ? lote : null,
                        recibida > 0 ? LocalDate.now().plusMonths(8) : null,
                        "Anaquel A",
                        motivo)));
    }

    @Test
    void elCicloCompletoPasaDeLaRequisicionAlPago() {
        ProductoResponse paracetamol = producto("Paracetamol compras", 10);
        ProveedorResponse barato = proveedor("Droguería Barata", 30);
        ProveedorResponse caro = proveedor("Droguería Cara", 15);

        RequisicionDetalle requisicion = requisicionEnviada(farmacia, paracetamol.id(), 100);
        UUID requisicionId = requisicion.resumen().id();
        assertThat(requisicion.resumen().estado()).isEqualTo(EstadoRequisicion.ENVIADA);
        assertThat(requisicion.resumen().codigo())
                .startsWith("REQ-" + LocalDate.now().getYear());

        CotizacionResponse cotBarata = cotizaciones.registrar(
                almacen,
                requisicionId,
                new CotizacionRequest(
                        barato.id(),
                        LocalDate.now().plusDays(10),
                        3,
                        30,
                        null,
                        List.of(new CotizacionItemRequest(paracetamol.id(), new BigDecimal("1.20")))));
        cotizaciones.registrar(
                almacen,
                requisicionId,
                new CotizacionRequest(
                        caro.id(),
                        null,
                        2,
                        15,
                        null,
                        List.of(new CotizacionItemRequest(paracetamol.id(), new BigDecimal("1.50")))));
        ComparativaResponse comparativa = cotizaciones.comparativa(requisicionId);
        assertThat(comparativa.columnas()).hasSize(2);
        assertThat(comparativa.columnas().stream().filter(c -> c.mejorTotal()).count())
                .isEqualTo(1);
        assertThat(comparativa.filas().get(0).mejorCotizacionId()).isEqualTo(cotBarata.id());
        assertThat(requisiciones.obtener(almacen, requisicionId).resumen().estado())
                .isEqualTo(EstadoRequisicion.EN_COTIZACION);

        cotizaciones.seleccionar(almacen, cotBarata.id());
        OrdenCompraDetalle borrador = ordenes.desdeCotizacion(almacen, cotBarata.id());
        assertThat(borrador.resumen().codigo()).isNull();
        assertThat(borrador.subtotal()).isEqualByComparingTo("120.00");
        assertThat(borrador.igv()).isEqualByComparingTo("21.60");
        assertThat(borrador.total()).isEqualByComparingTo("141.60");

        OrdenCompraDetalle enviada =
                ordenes.enviarAprobacion(almacen, borrador.resumen().id());
        assertThat(enviada.resumen().estado()).isEqualTo(EstadoOrdenCompra.PENDIENTE_APROBACION);
        assertThat(enviada.resumen().nivelAprobacion()).isEqualTo(NivelAprobacion.FINANZAS);

        OrdenCompraDetalle aprobada =
                ordenes.aprobar(finanzas, borrador.resumen().id());
        assertThat(aprobada.resumen().estado()).isEqualTo(EstadoOrdenCompra.APROBADA);
        assertThat(aprobada.resumen().codigo())
                .startsWith("OC-" + LocalDate.now().getYear() + "-");
        assertThat(aprobada.hash()).hasSize(64);
        assertThat(requisiciones.obtener(almacen, requisicionId).resumen().estado())
                .isEqualTo(EstadoRequisicion.ATENDIDA);

        UUID itemId = aprobada.items().get(0).id();
        RecepcionDetalle primera =
                recepciones.registrar(almacen, aprobada.resumen().id(), recepcion(itemId, 60, 0, "LOTE-A", null));
        assertThat(primera.resumen().conDiscrepancia()).isFalse();
        assertThat(ordenes.obtener(aprobada.resumen().id()).resumen().estado()).isEqualTo(EstadoOrdenCompra.PARCIAL);
        assertThat(inventarioService
                        .stockDe(List.of(paracetamol.id()))
                        .get(paracetamol.id())
                        .stock())
                .isEqualTo(60);

        RecepcionDetalle segunda = recepciones.registrar(
                almacen, aprobada.resumen().id(), recepcion(itemId, 35, 5, "LOTE-B", "Cajas dañadas"));
        assertThat(segunda.resumen().conDiscrepancia()).isTrue();
        OrdenCompraDetalle recibida = ordenes.obtener(aprobada.resumen().id());
        assertThat(recibida.resumen().estado()).isEqualTo(EstadoOrdenCompra.RECIBIDA);
        assertThat(recibida.resumen().porcentajeRecibido()).isEqualTo(100);
        assertThat(inventarioService
                        .stockDe(List.of(paracetamol.id()))
                        .get(paracetamol.id())
                        .stock())
                .isEqualTo(95);
        assertThat(recepciones.listar(aprobada.resumen().id(), true, null, null))
                .hasSize(1);

        ComprobanteProveedorResponse factura = cuentas.registrar(
                finanzas,
                new ComprobanteProveedorRequest(
                        barato.id(),
                        aprobada.resumen().id(),
                        TipoComprobanteProveedor.FACTURA,
                        "f001",
                        "123",
                        barato.ruc(),
                        LocalDate.now(),
                        null,
                        new BigDecimal("114.00"),
                        new BigDecimal("20.52"),
                        null));
        assertThat(factura.total()).isEqualByComparingTo("134.52");
        assertThat(factura.fechaVencimiento()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(factura.numero()).isEqualTo("F001-123");

        ComprobanteProveedorResponse parcial = cuentas.pagar(
                finanzas,
                factura.id(),
                new PagoProveedorRequest(new BigDecimal("100.00"), MetodoPagoProveedor.TRANSFERENCIA, "OP-1", null));
        assertThat(parcial.estado()).isEqualTo(EstadoComprobanteProveedor.PARCIAL);
        assertThat(parcial.saldo()).isEqualByComparingTo("34.52");
        assertThat(parcial.pagos()).hasSize(1);
        ComprobanteProveedorResponse pagada = cuentas.pagar(
                finanzas,
                factura.id(),
                new PagoProveedorRequest(new BigDecimal("34.52"), MetodoPagoProveedor.EFECTIVO, null, null));
        assertThat(pagada.estado()).isEqualTo(EstadoComprobanteProveedor.PAGADO);
        assertThatThrownBy(() -> cuentas.pagar(
                        finanzas,
                        factura.id(),
                        new PagoProveedorRequest(new BigDecimal("1.00"), MetodoPagoProveedor.EFECTIVO, null, null)))
                .isInstanceOf(ConflictException.class);

        assertThat(auditor.deEntidad(aprobada.resumen().id()))
                .extracting(a -> a.accion())
                .containsExactlyInAnyOrder("CREAR", "ENVIAR_APROBACION", "APROBAR");
        assertThat(reportes.porProveedor(null, null, barato.id())).hasSize(1);
    }

    @Test
    void laSegregacionDeFuncionesImpideAutoaprobarYAutorrecibir() {
        ProductoResponse producto = producto("Gasas segregación", 5);
        ProveedorResponse proveedor = proveedor("Proveedor Segregación", 0);

        RequisicionDetalle requisicion = requisicionEnviada(almacen, producto.id(), 20);
        OrdenCompraDetalle orden = ordenes.crear(
                almacen,
                new OrdenCompraRequest(
                        proveedor.id(),
                        requisicion.resumen().id(),
                        null,
                        true,
                        0,
                        null,
                        null,
                        List.of(new OrdenItemRequest(producto.id(), 20, new BigDecimal("3.00")))));
        UUID id = orden.resumen().id();
        ordenes.enviarAprobacion(almacen, id);

        assertThatThrownBy(() -> ordenes.aprobar(almacen, id)).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> ordenes.aprobar(farmacia, id)).isInstanceOf(ForbiddenOperationException.class);

        OrdenCompraDetalle aprobada = ordenes.aprobar(finanzas, id);
        UUID itemId = aprobada.items().get(0).id();
        assertThatThrownBy(() -> recepciones.registrar(almacen, id, recepcion(itemId, 20, 0, "S-1", null)))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("solicita");
        assertThat(recepciones
                        .registrar(otroAlmacen, id, recepcion(itemId, 20, 0, "S-1", null))
                        .resumen()
                        .recibidaPorNombre())
                .isNotBlank();
    }

    @Test
    void lasOrdenesMayoresAlLimiteSoloLasApruebaLaGerencia() {
        ProductoResponse producto = producto("Equipo costoso", 1);
        ProveedorResponse proveedor = proveedor("Proveedor Costoso", 0);
        OrdenCompraDetalle orden = ordenes.crear(
                almacen,
                new OrdenCompraRequest(
                        proveedor.id(),
                        null,
                        null,
                        true,
                        0,
                        null,
                        null,
                        List.of(new OrdenItemRequest(producto.id(), 10, new BigDecimal("600.00")))));
        UUID id = orden.resumen().id();
        assertThat(ordenes.enviarAprobacion(almacen, id).resumen().nivelAprobacion())
                .isEqualTo(NivelAprobacion.GERENCIA);
        assertThatThrownBy(() -> ordenes.aprobar(finanzas, id))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("gerencia");
        assertThat(ordenes.aprobar(gerencia, id).resumen().estado()).isEqualTo(EstadoOrdenCompra.APROBADA);
    }

    @Test
    void unaOrdenAprobadaEsInmutableYSePuedeRechazarOAnular() {
        ProductoResponse producto = producto("Jeringas inmutables", 1);
        ProveedorResponse proveedor = proveedor("Proveedor Inmutable", 0);
        OrdenCompraDetalle aprobada = ordenAprobada(almacen, finanzas, proveedor.id(), producto.id(), 10, "2.00");
        OrdenCompraRequest cambio = new OrdenCompraRequest(
                proveedor.id(),
                null,
                null,
                true,
                0,
                null,
                null,
                List.of(new OrdenItemRequest(producto.id(), 99, new BigDecimal("9.00"))));
        assertThatThrownBy(() -> ordenes.actualizar(almacen, aprobada.resumen().id(), cambio))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> ordenes.aprobar(gerencia, aprobada.resumen().id()))
                .isInstanceOf(ConflictException.class);

        recepciones.registrar(
                almacen,
                aprobada.resumen().id(),
                recepcion(aprobada.items().get(0).id(), 4, 0, "I-1", null));
        assertThatThrownBy(() -> ordenes.anular(almacen, aprobada.resumen().id(), "Ya no se necesita"))
                .isInstanceOf(ConflictException.class);
        OrdenCompraDetalle cerrada = ordenes.cerrar(almacen, aprobada.resumen().id(), "El proveedor no entrega más");
        assertThat(cerrada.resumen().estado()).isEqualTo(EstadoOrdenCompra.CERRADA);

        OrdenCompraDetalle pendiente = ordenes.crear(
                almacen, new OrdenCompraRequest(proveedor.id(), null, null, false, 0, null, null, cambio.items()));
        ordenes.enviarAprobacion(almacen, pendiente.resumen().id());
        OrdenCompraDetalle rechazada =
                ordenes.rechazar(finanzas, pendiente.resumen().id(), "Precio fuera de rango");
        assertThat(rechazada.resumen().estado()).isEqualTo(EstadoOrdenCompra.RECHAZADA);
        assertThat(rechazada.resumen().codigo()).isNull();

        OrdenCompraDetalle borrador = ordenes.crear(
                almacen, new OrdenCompraRequest(proveedor.id(), null, null, true, 0, null, null, cambio.items()));
        assertThat(ordenes.anular(almacen, borrador.resumen().id(), "Duplicada")
                        .resumen()
                        .estado())
                .isEqualTo(EstadoOrdenCompra.ANULADA);
    }

    @Test
    void laRecepcionValidaCantidadesVencimientosYMotivos() {
        ProductoResponse producto = producto("Suero recepción", 1);
        ProveedorResponse proveedor = proveedor("Proveedor Recepción", 0);
        OrdenCompraDetalle aprobada = ordenAprobada(almacen, finanzas, proveedor.id(), producto.id(), 10, "2.00");
        UUID id = aprobada.resumen().id();
        UUID itemId = aprobada.items().get(0).id();

        assertThatThrownBy(() -> recepciones.registrar(almacen, id, recepcion(itemId, 11, 0, "R-1", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("pendiente");
        assertThatThrownBy(() -> recepciones.registrar(almacen, id, recepcion(itemId, 5, 1, "R-1", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("motivo");
        assertThatThrownBy(() -> recepciones.registrar(
                        almacen,
                        id,
                        new RecepcionRequest(
                                null,
                                null,
                                List.of(new RecepcionItemRequest(
                                        itemId, 5, 0, "R-2", LocalDate.now().minusDays(1), null, null)))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("vencido");
        assertThatThrownBy(() -> recepciones.registrar(
                        almacen,
                        id,
                        new RecepcionRequest(
                                null, null, List.of(new RecepcionItemRequest(itemId, 0, 0, null, null, null, null)))))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> recepciones.registrar(
                        almacen,
                        id,
                        new RecepcionRequest(
                                null,
                                null,
                                List.of(new RecepcionItemRequest(
                                        UUID.randomUUID(),
                                        1,
                                        0,
                                        "R-3",
                                        LocalDate.now().plusDays(90),
                                        null,
                                        null)))))
                .isInstanceOf(ConflictException.class);
        assertThat(ordenes.obtener(id).resumen().estado()).isEqualTo(EstadoOrdenCompra.APROBADA);
    }

    @Test
    void laDevolucionDescuentaElLoteYGeneraCreditoEnElComprobante() {
        ProductoResponse producto = producto("Guantes devolución", 1);
        ProveedorResponse proveedor = proveedor("Proveedor Devolución", 0);
        OrdenCompraDetalle aprobada = ordenAprobada(almacen, finanzas, proveedor.id(), producto.id(), 10, "10.00");
        RecepcionDetalle recepcion = recepciones.registrar(
                almacen,
                aprobada.resumen().id(),
                recepcion(aprobada.items().get(0).id(), 10, 0, "D-1", null));
        ComprobanteProveedorResponse factura = cuentas.registrar(
                finanzas,
                new ComprobanteProveedorRequest(
                        proveedor.id(),
                        aprobada.resumen().id(),
                        TipoComprobanteProveedor.FACTURA,
                        "F002",
                        "55",
                        proveedor.ruc(),
                        LocalDate.now(),
                        LocalDate.now().plusDays(10),
                        new BigDecimal("100.00"),
                        new BigDecimal("18.00"),
                        null));
        UUID recepcionItemId = recepcion.items().get(0).id();

        assertThatThrownBy(() -> recepciones.devolver(
                        almacen,
                        new DevolucionRequest(
                                recepcion.resumen().id(),
                                factura.id(),
                                "Defecto",
                                List.of(new DevolucionItemRequest(recepcionItemId, 11)))))
                .isInstanceOf(ConflictException.class);

        var devolucion = recepciones.devolver(
                almacen,
                new DevolucionRequest(
                        recepcion.resumen().id(),
                        factura.id(),
                        "Cajas aplastadas",
                        List.of(new DevolucionItemRequest(recepcionItemId, 3))));
        assertThat(devolucion.monto()).isEqualByComparingTo("35.40");
        assertThat(devolucion.codigo()).startsWith("DEV-");
        Lote lote = loteRepository.findById(recepcion.items().get(0).loteId()).orElseThrow();
        assertThat(lote.getCantidadActual()).isEqualTo(7);
        ComprobanteProveedorResponse conCredito = cuentas.obtener(factura.id());
        assertThat(conCredito.montoCredito()).isEqualByComparingTo("35.40");
        assertThat(conCredito.saldo()).isEqualByComparingTo("82.60");
        assertThat(recepciones.devoluciones(proveedor.id(), null, null)).hasSize(1);
    }

    @Test
    void elComprobanteDelProveedorValidaRucDuplicadosYMontos() {
        ProductoResponse producto = producto("Alcohol comprobante", 1);
        ProveedorResponse proveedor = proveedor("Proveedor Comprobante", 15);
        OrdenCompraDetalle aprobada = ordenAprobada(almacen, finanzas, proveedor.id(), producto.id(), 10, "10.00");

        ComprobanteProveedorRequest base = new ComprobanteProveedorRequest(
                proveedor.id(),
                aprobada.resumen().id(),
                TipoComprobanteProveedor.FACTURA,
                "F003",
                "1",
                proveedor.ruc(),
                LocalDate.now(),
                null,
                new BigDecimal("10.00"),
                new BigDecimal("1.80"),
                null);
        assertThatThrownBy(() -> cuentas.registrar(finanzas, base))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("aún no tiene mercadería");

        recepciones.registrar(
                almacen,
                aprobada.resumen().id(),
                recepcion(aprobada.items().get(0).id(), 10, 0, "C-1", null));

        assertThatThrownBy(() -> cuentas.registrar(
                        finanzas,
                        new ComprobanteProveedorRequest(
                                proveedor.id(),
                                null,
                                TipoComprobanteProveedor.FACTURA,
                                "F003",
                                "9",
                                "20123456780",
                                LocalDate.now(),
                                null,
                                new BigDecimal("10.00"),
                                null,
                                null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> cuentas.registrar(
                        finanzas,
                        new ComprobanteProveedorRequest(
                                proveedor.id(),
                                null,
                                TipoComprobanteProveedor.FACTURA,
                                "F003",
                                "9",
                                ServiceTestFixture.rucValido(),
                                LocalDate.now(),
                                null,
                                new BigDecimal("10.00"),
                                null,
                                null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no coincide");

        ComprobanteProveedorResponse primero = cuentas.registrar(finanzas, base);
        assertThat(primero.fechaVencimiento()).isEqualTo(LocalDate.now().plusDays(30));
        assertThatThrownBy(() -> cuentas.registrar(finanzas, base))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Ya se registró");
        assertThatThrownBy(() -> cuentas.registrar(
                        finanzas,
                        new ComprobanteProveedorRequest(
                                proveedor.id(),
                                aprobada.resumen().id(),
                                TipoComprobanteProveedor.FACTURA,
                                "F003",
                                "2",
                                proveedor.ruc(),
                                LocalDate.now(),
                                null,
                                new BigDecimal("500.00"),
                                new BigDecimal("90.00"),
                                null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("excede");
        assertThatThrownBy(() -> cuentas.registrar(
                        finanzas,
                        new ComprobanteProveedorRequest(
                                proveedor.id(),
                                null,
                                TipoComprobanteProveedor.BOLETA,
                                "B001",
                                "2",
                                proveedor.ruc(),
                                LocalDate.now().plusDays(2),
                                null,
                                new BigDecimal("5.00"),
                                null,
                                null)))
                .isInstanceOf(ConflictException.class);

        cuentas.pagar(
                finanzas,
                primero.id(),
                new PagoProveedorRequest(new BigDecimal("5.00"), MetodoPagoProveedor.CHEQUE, "CH-1", null));
        assertThatThrownBy(() -> cuentas.anular(finanzas, primero.id(), "Error de digitación"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> cuentas.pagar(
                        finanzas,
                        primero.id(),
                        new PagoProveedorRequest(new BigDecimal("999.00"), MetodoPagoProveedor.CHEQUE, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("excede");
    }

    @Test
    void lasCuentasPorPagarSeClasificanPorVencimiento() {
        ProveedorResponse proveedor = proveedor("Proveedor Vencimientos", 0);
        cuentas.registrar(
                finanzas,
                new ComprobanteProveedorRequest(
                        proveedor.id(),
                        null,
                        TipoComprobanteProveedor.FACTURA,
                        "F004",
                        "1",
                        proveedor.ruc(),
                        LocalDate.now().minusDays(50),
                        LocalDate.now().minusDays(20),
                        new BigDecimal("100.00"),
                        null,
                        null));
        cuentas.registrar(
                finanzas,
                new ComprobanteProveedorRequest(
                        proveedor.id(),
                        null,
                        TipoComprobanteProveedor.RECIBO,
                        "R001",
                        "2",
                        proveedor.ruc(),
                        LocalDate.now(),
                        LocalDate.now().plusDays(10),
                        new BigDecimal("40.00"),
                        null,
                        null));
        var resumen = cuentas.resumen();
        assertThat(resumen.totalVencido()).isGreaterThanOrEqualTo(new BigDecimal("100.00"));
        assertThat(resumen.tramos()).hasSize(5);
        assertThat(resumen.tramos().get(1).monto()).isGreaterThanOrEqualTo(new BigDecimal("100.00"));
        assertThat(cuentas.listar(null, proveedor.id(), null, null, true)).hasSize(1);
        assertThat(cuentas.listar(null, proveedor.id(), null, null, false)).hasSize(2);
        assertThat(cuentas.notificarVencidos(java.time.LocalDateTime.now())).isGreaterThanOrEqualTo(1);
        assertThat(cuentas.notificarVencidos(java.time.LocalDateTime.now())).isZero();
    }

    @Test
    void elCreditoDeUnaDevolucionSinComprobanteSeAplicaDespues() {
        ProductoResponse producto = producto("Mascarillas crédito", 1);
        ProveedorResponse proveedor = proveedor("Proveedor Crédito", 0);
        OrdenCompraDetalle aprobada = ordenAprobada(almacen, finanzas, proveedor.id(), producto.id(), 10, "10.00");
        RecepcionDetalle recepcion = recepciones.registrar(
                almacen,
                aprobada.resumen().id(),
                recepcion(aprobada.items().get(0).id(), 10, 0, "K-1", null));
        var devolucion = recepciones.devolver(
                almacen,
                new DevolucionRequest(
                        recepcion.resumen().id(),
                        null,
                        "Defecto de fábrica",
                        List.of(new DevolucionItemRequest(
                                recepcion.items().get(0).id(), 2))));
        assertThat(devolucion.comprobanteId()).isNull();
        ComprobanteProveedorResponse factura = cuentas.registrar(
                finanzas,
                new ComprobanteProveedorRequest(
                        proveedor.id(),
                        aprobada.resumen().id(),
                        TipoComprobanteProveedor.FACTURA,
                        "F005",
                        "7",
                        proveedor.ruc(),
                        LocalDate.now(),
                        null,
                        new BigDecimal("100.00"),
                        new BigDecimal("18.00"),
                        null));
        ProveedorResponse otro = proveedor("Otro proveedor", 0);
        ComprobanteProveedorResponse ajeno = cuentas.registrar(
                finanzas,
                new ComprobanteProveedorRequest(
                        otro.id(),
                        null,
                        TipoComprobanteProveedor.RECIBO,
                        "R9",
                        "1",
                        otro.ruc(),
                        LocalDate.now(),
                        null,
                        new BigDecimal("50.00"),
                        null,
                        null));
        assertThatThrownBy(() -> recepciones.aplicarCredito(finanzas, devolucion.id(), ajeno.id()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("otro proveedor");
        var aplicada = recepciones.aplicarCredito(finanzas, devolucion.id(), factura.id());
        assertThat(aplicada.comprobanteId()).isEqualTo(factura.id());
        assertThat(cuentas.obtener(factura.id()).saldo()).isEqualByComparingTo("94.40");
        assertThatThrownBy(() -> recepciones.aplicarCredito(finanzas, devolucion.id(), factura.id()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya tiene");
    }

    @Test
    void lasSugerenciasSalenDelStockBajo() {
        ProductoResponse bajo = producto("Producto con stock bajo", 40);
        assertThat(requisiciones.sugeridas()).anySatisfy(s -> {
            assertThat(s.productoId()).isEqualTo(bajo.id());
            assertThat(s.cantidadSugerida()).isEqualTo(80);
        });
    }

    @Test
    void lasRequisicionesDeFarmaciaSoloSeVenEntreSuSolicitante() {
        ProductoResponse producto = producto("Requisición ajena", 1);
        RequisicionDetalle propia = requisicionEnviada(farmacia, producto.id(), 5);
        UsuarioPrincipal otraFarmacia = fixture.principal(fixture.usuario("farm2", RolNombre.ROLE_FARMACIA));
        assertThatThrownBy(() ->
                        requisiciones.obtener(otraFarmacia, propia.resumen().id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(requisiciones.listar(otraFarmacia, null, null, null)).isEmpty();
        assertThat(requisiciones.listar(almacen, null, null, null))
                .extracting(r -> r.id())
                .contains(propia.resumen().id());
        assertThatThrownBy(
                        () -> requisiciones.rechazar(farmacia, propia.resumen().id(), "No procede"))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(requisiciones
                        .rechazar(almacen, propia.resumen().id(), "Sin presupuesto")
                        .resumen()
                        .estado())
                .isEqualTo(EstadoRequisicion.RECHAZADA);
        assertThatThrownBy(() -> requisiciones.anular(almacen, propia.resumen().id(), "x"))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void elProveedorExigeUnRucValido() {
        assertThatThrownBy(() -> proveedorService.crear(new ProveedorRequest(
                        "20123456780", "Proveedor inválido", null, null, null, null, true, 0, null, null, null, null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThat(proveedor("Proveedor válido", 7).condicionPagoDias()).isEqualTo(7);
    }
}

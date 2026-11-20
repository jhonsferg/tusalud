package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AjusteRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ProductoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.request.SalidaRequest;
import com.codesoftlabs.tusalud.app.dto.response.AlertasInventarioResponse;
import com.codesoftlabs.tusalud.app.dto.response.IngresoResponse;
import com.codesoftlabs.tusalud.app.dto.response.LoteResponse;
import com.codesoftlabs.tusalud.app.dto.response.MovimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ProveedorResponse;
import com.codesoftlabs.tusalud.app.entities.Lote;
import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoLote;
import com.codesoftlabs.tusalud.app.entities.enums.MotivoMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.MotivoSalida;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.LoteRepository;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.ProductoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AlertaInventarioService;
import com.codesoftlabs.tusalud.app.services.interfaces.AlmacenService;
import com.codesoftlabs.tusalud.app.services.interfaces.ProductoService;
import com.codesoftlabs.tusalud.app.services.interfaces.ProveedorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class AlmacenServiceTest {

    @Autowired
    private AlmacenService almacenService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private AlertaInventarioService alertaService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Usuario almacenista;
    private UsuarioPrincipal almacen;
    private UsuarioPrincipal laboratorio;

    @BeforeEach
    void setUp() {
        almacenista = fixture.usuario("almacen", RolNombre.ROLE_ALMACEN);
        almacen = fixture.principal(almacenista);
        laboratorio = fixture.principal(fixture.usuario("lab", RolNombre.ROLE_LABORATORIO));
    }

    private ProductoResponse producto(String sufijo, TipoProducto tipo, int stockMinimo) {
        return productoService.crear(new ProductoRequest(
                "t-" + sufijo + ServiceTestFixture.siguiente(),
                "Producto " + sufijo,
                null,
                tipo,
                "Principio",
                "Caja",
                "unidad",
                new BigDecimal("2.50"),
                stockMinimo,
                false,
                true));
    }

    private IngresoResponse ingreso(UUID productoId, String lote, int dias, int cantidad) {
        return almacenService.registrarIngreso(
                almacen,
                new IngresoRequest(
                        null,
                        "F001-1",
                        null,
                        List.of(new IngresoItemRequest(
                                productoId,
                                lote,
                                LocalDate.now().plusDays(dias),
                                cantidad,
                                new BigDecimal("1.00"),
                                "Estante"))));
    }

    private Lote vencer(UUID loteId, int diasAtras) {
        Lote lote = loteRepository.findById(loteId).orElseThrow();
        lote.setFechaVencimiento(LocalDate.now().minusDays(diasAtras));
        return loteRepository.saveAndFlush(lote);
    }

    @Test
    void creaProductosConCodigoUnicoEnMayusculas() {
        ProductoResponse creado = producto("a", TipoProducto.MEDICAMENTO, 5);

        assertThat(creado.codigo()).isEqualTo(creado.codigo().toUpperCase());
        assertThat(creado.stockActual()).isZero();
        assertThat(creado.bajoStock()).isTrue();
        assertThatThrownBy(() -> productoService.crear(new ProductoRequest(
                        creado.codigo().toLowerCase(),
                        "Otro",
                        null,
                        TipoProducto.INSUMO,
                        null,
                        null,
                        "unidad",
                        BigDecimal.ONE,
                        1,
                        null,
                        null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void elIngresoCreaLotesMovimientosYActualizaElStock() {
        ProductoResponse producto = producto("b", TipoProducto.MEDICAMENTO, 5);

        IngresoResponse ingreso = ingreso(producto.id(), "L-1", 200, 40);

        assertThat(ingreso.total()).isEqualByComparingTo("40.00");
        assertThat(ingreso.items()).hasSize(1);
        assertThat(ingreso.items().get(0).estado()).isEqualTo(EstadoLote.VIGENTE);
        ProductoResponse actualizado = productoService.obtener(almacen, producto.id());
        assertThat(actualizado.stockActual()).isEqualTo(40);
        assertThat(actualizado.bajoStock()).isFalse();
        assertThat(actualizado.proximoVencimiento()).isEqualTo(LocalDate.now().plusDays(200));
        List<MovimientoResponse> movimientos =
                almacenService.movimientos(producto.id(), TipoMovimiento.ENTRADA, null, null);
        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).motivo()).isEqualTo(MotivoMovimiento.COMPRA);
        assertThat(almacenService.listarIngresos())
                .extracting(IngresoResponse::id)
                .contains(ingreso.id());
    }

    @Test
    void rechazaLotesRepetidosProductosInactivosYProveedoresInactivos() {
        ProductoResponse producto = producto("c", TipoProducto.MEDICAMENTO, 5);
        ingreso(producto.id(), "L-1", 200, 10);

        assertThatThrownBy(() -> ingreso(producto.id(), "L-1", 300, 5)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> almacenService.registrarIngreso(
                        almacen,
                        new IngresoRequest(
                                null,
                                null,
                                null,
                                List.of(
                                        new IngresoItemRequest(
                                                producto.id(),
                                                "L-2",
                                                LocalDate.now().plusDays(10),
                                                1,
                                                BigDecimal.ONE,
                                                null),
                                        new IngresoItemRequest(
                                                producto.id(),
                                                "l-2",
                                                LocalDate.now().plusDays(10),
                                                1,
                                                BigDecimal.ONE,
                                                null)))))
                .isInstanceOf(ConflictException.class);

        Producto entidad = productoRepository.findById(producto.id()).orElseThrow();
        entidad.setActivo(false);
        productoRepository.saveAndFlush(entidad);
        assertThatThrownBy(() -> ingreso(producto.id(), "L-3", 100, 1)).isInstanceOf(ConflictException.class);

        ProveedorResponse proveedor = proveedorService.crear(new ProveedorRequest(
                ServiceTestFixture.rucValido(),
                "Proveedor",
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null));
        ProductoResponse otro = producto("d", TipoProducto.MEDICAMENTO, 1);
        assertThatThrownBy(() -> almacenService.registrarIngreso(
                        almacen,
                        new IngresoRequest(
                                proveedor.id(),
                                null,
                                null,
                                List.of(new IngresoItemRequest(
                                        otro.id(), "L-9", LocalDate.now().plusDays(5), 1, BigDecimal.ONE, null)))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void laSalidaDescuentaPorFefoYRegistraLosMovimientos() {
        ProductoResponse producto = producto("e", TipoProducto.MEDICAMENTO, 5);
        ingreso(producto.id(), "LARGO", 300, 10);
        ingreso(producto.id(), "CORTO", 40, 6);

        List<MovimientoResponse> movimientos = almacenService.registrarSalida(
                almacen, new SalidaRequest(producto.id(), 9, MotivoSalida.CONSUMO_INTERNO, "Uso en sala"));

        assertThat(movimientos).hasSize(2);
        assertThat(movimientos.get(0).numeroLote()).isEqualTo("CORTO");
        assertThat(movimientos.get(0).cantidad()).isEqualTo(6);
        assertThat(movimientos.get(1).numeroLote()).isEqualTo("LARGO");
        assertThat(movimientos.get(1).cantidad()).isEqualTo(3);
        assertThat(productoService.obtener(almacen, producto.id()).stockActual())
                .isEqualTo(7);
        assertThat(almacenService.movimientos(producto.id(), TipoMovimiento.SALIDA, null, null))
                .hasSize(2);
    }

    @Test
    void laSalidaRechazaStockInsuficienteYNoUsaLotesVencidos() {
        ProductoResponse producto = producto("f", TipoProducto.MEDICAMENTO, 1);
        IngresoResponse ingreso = ingreso(producto.id(), "VIEJO", 30, 20);
        vencer(ingreso.items().get(0).id(), 3);
        ingreso(producto.id(), "NUEVO", 100, 4);

        assertThatThrownBy(() -> almacenService.registrarSalida(
                        almacen, new SalidaRequest(producto.id(), 5, MotivoSalida.MERMA, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Stock insuficiente");
        assertThat(productoService.obtener(almacen, producto.id()).stockActual())
                .isEqualTo(4);

        List<MovimientoResponse> baja = almacenService.registrarSalida(
                almacen, new SalidaRequest(producto.id(), 20, MotivoSalida.VENCIMIENTO, "Baja"));
        assertThat(baja).hasSize(1);
        assertThat(baja.get(0).numeroLote()).isEqualTo("VIEJO");
        assertThat(productoService.obtener(almacen, producto.id()).stockActual())
                .isEqualTo(4);
    }

    @Test
    void elAjustePositivoSumaStockAUnLoteVigenteYRechazaLosVencidos() {
        ProductoResponse producto = producto("g", TipoProducto.INSUMO, 1);
        IngresoResponse ingreso = ingreso(producto.id(), "AJ", 100, 5);
        UUID loteId = ingreso.items().get(0).id();

        MovimientoResponse movimiento = almacenService.registrarAjuste(almacen, new AjusteRequest(loteId, 3, "Conteo"));

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimiento.AJUSTE);
        assertThat(movimiento.motivo()).isEqualTo(MotivoMovimiento.AJUSTE_POSITIVO);
        assertThat(productoService.obtener(almacen, producto.id()).stockActual())
                .isEqualTo(8);
        vencer(loteId, 1);
        assertThatThrownBy(() -> almacenService.registrarAjuste(almacen, new AjusteRequest(loteId, 1, null)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> almacenService.registrarAjuste(almacen, new AjusteRequest(UUID.randomUUID(), 1, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void validaElRangoDeFechasDelKardex() {
        assertThatThrownBy(() -> almacenService.movimientos(
                        null, null, LocalDate.now(), LocalDate.now().minusDays(1)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void lasAlertasSeparanStockBajoPorVencerYVencidos() {
        ProductoResponse bajo = producto("h", TipoProducto.MEDICAMENTO, 50);
        ingreso(bajo.id(), "B1", 300, 5);
        ProductoResponse pronto = producto("i", TipoProducto.MEDICAMENTO, 1);
        ingreso(pronto.id(), "P1", 20, 10);
        ProductoResponse vencido = producto("j", TipoProducto.MEDICAMENTO, 1);
        IngresoResponse viejo = ingreso(vencido.id(), "V1", 10, 10);
        vencer(viejo.items().get(0).id(), 5);

        AlertasInventarioResponse alertas = almacenService.alertas();

        assertThat(alertas.bajoStock()).extracting(ProductoResponse::id).contains(bajo.id());
        assertThat(alertas.porVencer()).extracting(LoteResponse::numeroLote).contains("P1");
        assertThat(alertas.porVencer().stream()
                        .filter(lote -> lote.numeroLote().equals("P1"))
                        .findFirst()
                        .orElseThrow()
                        .estado())
                .isEqualTo(EstadoLote.POR_VENCER);
        assertThat(alertas.vencidos()).extracting(LoteResponse::numeroLote).contains("V1");
        assertThat(almacenService.resumen().productosBajoStock()).isGreaterThanOrEqualTo(1);
        assertThat(almacenService.resumen().valorInventario()).isPositive();
    }

    @Test
    void notificaUnaSolaVezPorDiaAlPersonalDeAlmacen() {
        ProductoResponse bajo = producto("k", TipoProducto.MEDICAMENTO, 50);
        ingreso(bajo.id(), "K1", 300, 1);
        LocalDateTime ahora = LocalDateTime.now();

        int primeras = alertaService.notificarAlertas(ahora);
        int segundas = alertaService.notificarAlertas(ahora);

        assertThat(primeras).isGreaterThanOrEqualTo(1);
        assertThat(segundas).isZero();
        assertThat(notificacionRepository.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
                        almacenista.getId(),
                        TipoNotificacion.STOCK_BAJO,
                        ahora.toLocalDate().atStartOfDay()))
                .isTrue();
    }

    @Test
    void elLaboratorioSoloVeInsumosYReactivos() {
        ProductoResponse medicamento = producto("l", TipoProducto.MEDICAMENTO, 1);
        ProductoResponse reactivo = producto("m", TipoProducto.REACTIVO, 1);

        List<ProductoResponse> visibles = productoService.listar(laboratorio, null, null, null, null);

        assertThat(visibles)
                .extracting(ProductoResponse::id)
                .contains(reactivo.id())
                .doesNotContain(medicamento.id());
        assertThat(productoService.obtener(laboratorio, reactivo.id()).id()).isEqualTo(reactivo.id());
        assertThatThrownBy(() -> productoService.obtener(laboratorio, medicamento.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> productoService.lotes(laboratorio, medicamento.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void filtraProductosPorBajoStockYBuscaProveedoresPorRuc() {
        ProductoResponse bajo = producto("n", TipoProducto.MEDICAMENTO, 10);
        ProductoResponse holgado = producto("o", TipoProducto.MEDICAMENTO, 1);
        ingreso(holgado.id(), "H1", 200, 30);

        assertThat(productoService.listar(almacen, null, TipoProducto.MEDICAMENTO, true, null))
                .extracting(ProductoResponse::id)
                .contains(bajo.id())
                .doesNotContain(holgado.id());

        String ruc = ServiceTestFixture.rucValido();
        ProveedorResponse creado = proveedorService.crear(new ProveedorRequest(
                ruc, "Droguería Prueba", "Ana", "999", null, null, null, null, null, null, null, null));
        assertThat(creado.activo()).isTrue();
        assertThat(proveedorService.listar(ruc, true)).hasSize(1);
        assertThat(proveedorService.listar("drogue", false)).isEmpty();
        assertThatThrownBy(() -> proveedorService.crear(
                        new ProveedorRequest(ruc, "Otra", null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(ConflictException.class);
        ProveedorResponse otro = proveedorService.crear(new ProveedorRequest(
                ServiceTestFixture.rucValido(), "Otra", null, null, null, null, null, null, null, null, null, null));
        assertThatThrownBy(() -> proveedorService.actualizar(
                        otro.id(),
                        new ProveedorRequest(ruc, "Otra", null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(ConflictException.class);
    }
}

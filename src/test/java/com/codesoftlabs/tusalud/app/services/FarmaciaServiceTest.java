package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.DispensacionItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.DispensacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ProductoRequest;
import com.codesoftlabs.tusalud.app.dto.response.CatalogoFarmaciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DispensacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaFarmaciaResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.RecetaItem;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoDispensacion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoReceta;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoFarmacia;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AlmacenService;
import com.codesoftlabs.tusalud.app.services.interfaces.FarmaciaService;
import com.codesoftlabs.tusalud.app.services.interfaces.ProductoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class FarmaciaServiceTest {

    @Autowired
    private FarmaciaService farmaciaService;

    @Autowired
    private AlmacenService almacenService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private AtencionRepository atencionRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private UsuarioPrincipal almacen;
    private UsuarioPrincipal farmacia;
    private UsuarioPrincipal pacientePrincipal;
    private Paciente paciente;
    private Atencion atencion;
    private ProductoResponse conReceta;
    private ProductoResponse libre;

    @BeforeEach
    void setUp() {
        almacen = fixture.principal(fixture.usuario("almacen", RolNombre.ROLE_ALMACEN));
        farmacia = fixture.principal(fixture.usuario("farmacia", RolNombre.ROLE_FARMACIA));
        paciente = fixture.paciente();
        pacientePrincipal = fixture.principal(paciente.getUsuario());
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        atencion = atencionConReceta(medico, paciente, LocalDateTime.now().minusDays(2));
        conReceta = medicamento("rx", true, "3.00", 100);
        libre = medicamento("otc", false, "1.50", 50);
    }

    private Atencion atencionConReceta(Medico medico, Paciente paciente, LocalDateTime fecha) {
        var cita = fixture.cita(medico, paciente, fecha.minusMinutes(30), EstadoCita.ATENDIDA);
        List<RecetaItem> receta = new ArrayList<>(List.of(
                RecetaItem.builder().medicamento("Enalapril").dosis("10 mg").build(),
                RecetaItem.builder().medicamento("Naproxeno").dosis("550 mg").build()));
        return atencionRepository.save(Atencion.builder()
                .cita(cita)
                .paciente(paciente)
                .medico(medico)
                .fecha(fecha)
                .diagnostico("Diagnóstico")
                .indicaciones("Reposo")
                .receta(receta)
                .build());
    }

    private ProductoResponse medicamento(String sufijo, boolean requiereReceta, String precio, int stock) {
        ProductoResponse producto = productoService.crear(new ProductoRequest(
                "f-" + sufijo + ServiceTestFixture.siguiente(),
                "Med " + sufijo,
                null,
                TipoProducto.MEDICAMENTO,
                "Principio",
                "Caja",
                "tableta",
                new BigDecimal(precio),
                5,
                requiereReceta,
                true));
        almacenService.registrarIngreso(
                almacen,
                new IngresoRequest(
                        null,
                        null,
                        null,
                        List.of(new IngresoItemRequest(
                                producto.id(),
                                "LOT-" + producto.id(),
                                LocalDate.now().plusDays(365),
                                stock,
                                BigDecimal.ONE,
                                null))));
        return producto;
    }

    private DispensacionRequest dispensar(UUID atencionId, UUID pacienteId, DispensacionItemRequest... items) {
        return new DispensacionRequest(atencionId, pacienteId, List.of(items), MetodoPagoFarmacia.EFECTIVO, null);
    }

    private long stock(ProductoResponse producto) {
        return productoService.obtener(almacen, producto.id()).stockActual();
    }

    @Test
    void listaLasRecetasPendientesYBuscaPorDniONombre() {
        List<RecetaFarmaciaResponse> pendientes = farmaciaService.recetas(null, paciente.getDni());

        assertThat(pendientes).hasSize(1);
        RecetaFarmaciaResponse receta = pendientes.get(0);
        assertThat(receta.estado()).isEqualTo(EstadoReceta.PENDIENTE);
        assertThat(receta.items()).hasSize(2);
        assertThat(receta.items().get(1).id()).isEqualTo(1);
        assertThat(receta.items().get(0).indicaciones()).isEqualTo("Reposo");
        assertThat(farmaciaService.recetas(null, "zzz-sin-coincidencia")).isEmpty();
        assertThat(farmaciaService.recetas(EstadoReceta.DISPENSADA, paciente.getDni()))
                .isEmpty();
    }

    @Test
    void dispensaUnaRecetaDescuentaStockYPasaPorParcialAHastaDispensada() {
        DispensacionResponse primera = farmaciaService.dispensar(
                farmacia, dispensar(atencion.getId(), null, new DispensacionItemRequest(0, conReceta.id(), 10)));

        assertThat(primera.comprobante()).matches("F001-\\d{6}");
        assertThat(primera.estado()).isEqualTo(EstadoDispensacion.EMITIDA);
        assertThat(primera.total()).isEqualByComparingTo("30.00");
        assertThat(primera.pacienteId()).isEqualTo(paciente.getId());
        assertThat(primera.items()).hasSize(1);
        assertThat(stock(conReceta)).isEqualTo(90);
        assertThat(farmaciaService.recetas(EstadoReceta.PARCIAL, paciente.getDni()))
                .hasSize(1);
        assertThat(almacenService.movimientos(conReceta.id(), TipoMovimiento.SALIDA, null, null))
                .extracting(m -> m.referencia())
                .contains(primera.comprobante());
        assertThat(notificacionRepository.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
                        paciente.getUsuario().getId(),
                        TipoNotificacion.RECETA_DISPENSADA,
                        LocalDate.now().atStartOfDay()))
                .isTrue();

        farmaciaService.dispensar(
                farmacia, dispensar(atencion.getId(), null, new DispensacionItemRequest(1, libre.id(), 2)));
        assertThat(farmaciaService.recetas(EstadoReceta.DISPENSADA, paciente.getDni()))
                .hasSize(1);
        assertThat(farmaciaService.recetas(null, paciente.getDni())).isEmpty();
    }

    @Test
    void noPermiteDispensarDosVecesUnItemNiProductosConRecetaSinReceta() {
        farmaciaService.dispensar(
                farmacia, dispensar(atencion.getId(), null, new DispensacionItemRequest(0, conReceta.id(), 1)));

        assertThatThrownBy(() -> farmaciaService.dispensar(
                        farmacia, dispensar(atencion.getId(), null, new DispensacionItemRequest(0, conReceta.id(), 1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya fue dispensado");
        assertThatThrownBy(() -> farmaciaService.dispensar(
                        farmacia,
                        dispensar(null, paciente.getId(), new DispensacionItemRequest(null, conReceta.id(), 1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("requiere receta");
        assertThatThrownBy(() -> farmaciaService.dispensar(
                        farmacia, dispensar(atencion.getId(), null, new DispensacionItemRequest(7, libre.id(), 1))))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> farmaciaService.dispensar(
                        farmacia, dispensar(null, null, new DispensacionItemRequest(0, libre.id(), 1))))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void rechazaStockInsuficienteSinTocarElInventarioNiLaReceta() {
        long antes = stock(libre);

        assertThatThrownBy(() -> farmaciaService.dispensar(
                        farmacia,
                        dispensar(
                                atencion.getId(),
                                null,
                                new DispensacionItemRequest(0, libre.id(), 10),
                                new DispensacionItemRequest(1, conReceta.id(), 500))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(stock(libre)).isEqualTo(antes);
        assertThat(farmaciaService.recetas(EstadoReceta.PENDIENTE, paciente.getDni()))
                .hasSize(1);
    }

    @Test
    void ventaLibreParaClienteGeneralSoloConProductosSinReceta() {
        DispensacionResponse venta = farmaciaService.dispensar(
                farmacia, dispensar(null, null, new DispensacionItemRequest(null, libre.id(), 4)));

        assertThat(venta.pacienteNombre()).isEqualTo(DispensacionResponse.CLIENTE_GENERAL);
        assertThat(venta.pacienteId()).isNull();
        assertThat(venta.total()).isEqualByComparingTo("6.00");
        assertThat(stock(libre)).isEqualTo(46);
        assertThat(farmaciaService.listar(null, null, venta.comprobante())).hasSize(1);
        assertThat(farmaciaService.obtener(venta.id()).items()).hasSize(1);
        assertThatThrownBy(() -> farmaciaService.obtener(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void elPacienteDebeCoincidirConLaRecetaYLaRecetaNoDebeEstarVencida() {
        Paciente otro = fixture.paciente();
        assertThatThrownBy(() -> farmaciaService.dispensar(
                        farmacia,
                        dispensar(atencion.getId(), otro.getId(), new DispensacionItemRequest(0, conReceta.id(), 1))))
                .isInstanceOf(DatosInvalidosException.class);

        Medico medico = atencion.getMedico();
        Atencion vieja = atencionConReceta(medico, otro, LocalDateTime.now().minusDays(120));
        assertThatThrownBy(() -> farmaciaService.dispensar(
                        farmacia, dispensar(vieja.getId(), null, new DispensacionItemRequest(0, conReceta.id(), 1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("venció");
        assertThat(farmaciaService.recetas(null, otro.getDni())).isEmpty();
    }

    @Test
    void anularDevuelveElStockYReabreLaReceta() {
        DispensacionResponse dispensada = farmaciaService.dispensar(
                farmacia, dispensar(atencion.getId(), null, new DispensacionItemRequest(0, conReceta.id(), 25)));
        assertThat(stock(conReceta)).isEqualTo(75);

        DispensacionResponse anulada = farmaciaService.anular(farmacia, dispensada.id());

        assertThat(anulada.estado()).isEqualTo(EstadoDispensacion.ANULADA);
        assertThat(stock(conReceta)).isEqualTo(100);
        assertThat(farmaciaService.recetas(EstadoReceta.PENDIENTE, paciente.getDni()))
                .hasSize(1);
        assertThat(almacenService.movimientos(conReceta.id(), TipoMovimiento.ENTRADA, null, null))
                .extracting(m -> m.motivo().name())
                .contains("ANULACION_DISPENSACION");
        assertThatThrownBy(() -> farmaciaService.anular(farmacia, dispensada.id()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void elPacienteVeSoloSusDispensacionesYElResumenCuentaLasDelDia() {
        farmaciaService.dispensar(
                farmacia, dispensar(atencion.getId(), null, new DispensacionItemRequest(0, conReceta.id(), 2)));
        farmaciaService.dispensar(farmacia, dispensar(null, null, new DispensacionItemRequest(null, libre.id(), 2)));

        List<DispensacionResponse> propias = farmaciaService.misDispensaciones(pacientePrincipal);

        assertThat(propias).hasSize(1);
        assertThat(propias.get(0).items()).hasSize(1);
        assertThat(farmaciaService.resumen().dispensacionesHoy()).isGreaterThanOrEqualTo(2);
        assertThat(farmaciaService.resumen().ventasHoy()).isGreaterThanOrEqualTo(new BigDecimal("9.00"));
        assertThat(farmaciaService.resumen().recetasPendientes()).isGreaterThanOrEqualTo(1);
        assertThatThrownBy(() -> farmaciaService.misDispensaciones(farmacia))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void elCatalogoMuestraSoloMedicamentosActivosConStock() {
        productoService.crear(new ProductoRequest(
                "INS-" + ServiceTestFixture.siguiente(),
                "Insumo no vendible",
                null,
                TipoProducto.INSUMO,
                null,
                null,
                "unidad",
                BigDecimal.ONE,
                1,
                false,
                true));

        List<CatalogoFarmaciaResponse> catalogo = farmaciaService.catalogo("Med ");

        assertThat(catalogo).extracting(CatalogoFarmaciaResponse::id).contains(conReceta.id(), libre.id());
        assertThat(catalogo).allSatisfy(item -> assertThat(item.nombre()).startsWith("Med "));
        assertThat(catalogo.stream()
                        .filter(item -> item.id().equals(libre.id()))
                        .findFirst()
                        .orElseThrow()
                        .stockActual())
                .isEqualTo(50);
    }
}

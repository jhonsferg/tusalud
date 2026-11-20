package com.codesoftlabs.tusalud.app.documentos;

import com.codesoftlabs.tusalud.app.dto.response.AusenciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprobanteResponse;
import com.codesoftlabs.tusalud.app.dto.response.MovimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.dto.response.TurnoPersonalResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AlmacenService;
import com.codesoftlabs.tusalud.app.services.interfaces.AusenciaService;
import com.codesoftlabs.tusalud.app.services.interfaces.CajaService;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
import com.codesoftlabs.tusalud.app.services.interfaces.ProductoService;
import com.codesoftlabs.tusalud.app.services.interfaces.TurnoPersonalService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListadosExcel {

    public record Listado(List<HojaExcel> hojas, String nombreBase, String titulo) {}

    private final CajaService cajaService;
    private final AlmacenService almacenService;
    private final ProductoService productoService;
    private final AusenciaService ausenciaService;
    private final TurnoPersonalService turnoService;
    private final CitaService citaService;
    private final com.codesoftlabs.tusalud.app.services.compras.ReportesComprasService comprasService;
    private final com.codesoftlabs.tusalud.app.services.compras.OrdenCompraService ordenCompraService;
    private final com.codesoftlabs.tusalud.app.services.contabilidad.LibrosContablesService librosContables;
    private final com.codesoftlabs.tusalud.app.services.planilla.PlanillaService planillaService;
    private final com.codesoftlabs.tusalud.app.services.planilla.AsistenciaService asistenciaService;

    public Listado armar(UsuarioPrincipal actor, TipoDocumento tipo, ParametrosDocumento parametros) {
        return switch (tipo) {
            case LISTADO_COMPROBANTES -> comprobantes(parametros);
            case KARDEX -> kardex(parametros);
            case REPORTE_COMPRAS -> compras(parametros);
            case LIBRO_DIARIO -> libroDiario(parametros);
            case LIBRO_MAYOR -> libroMayor(parametros);
            case BALANCE_COMPROBACION -> balanceComprobacion(parametros);
            case STOCK -> stock(actor, parametros);
            case AUSENCIAS -> ausencias(actor, parametros);
            case TURNOS -> turnos(parametros);
            case REPORTE_PLANILLA -> reportePlanilla(actor, parametros);
            case ASISTENCIA_PERSONAL -> asistenciaPersonal(parametros);
            case MIS_CITAS -> misCitas(actor, parametros);
            default -> throw new IllegalArgumentException("El documento no es un listado: " + tipo);
        };
    }

    private Listado comprobantes(ParametrosDocumento p) {
        LocalDate[] rango = p.rango(LocalDate.now().minusDays(30), LocalDate.now());
        List<ComprobanteResponse> lista = cajaService.comprobantes(
                rango[0], rango[1], p.texto("q"), p.enumeracion("estado", EstadoComprobante.class));
        HojaExcel hoja = HojaExcel.de(
                "Comprobantes",
                "Código",
                "Tipo",
                "Fecha",
                "Cliente",
                "Documento",
                "Origen",
                "Método de pago",
                "Subtotal",
                "IGV",
                "Total",
                "Estado",
                "Cajero");
        for (ComprobanteResponse c : lista) {
            hoja.fila(
                    c.codigo(),
                    c.tipo().name(),
                    c.fecha(),
                    c.clienteNombre(),
                    c.clienteDoc() == null ? "" : c.clienteDoc(),
                    Etiquetas.origen(c.origen()),
                    Etiquetas.metodoPago(c.metodoPago()),
                    c.subtotal(),
                    c.igv(),
                    c.total(),
                    c.estado() == EstadoComprobante.ANULADO ? "Anulado" : "Emitido",
                    c.cajeroNombre());
        }
        return new Listado(List.of(hoja), "comprobantes-" + rango[0] + "-a-" + rango[1], "Listado de comprobantes");
    }

    private Listado compras(ParametrosDocumento p) {
        LocalDate[] rango = p.rango(LocalDate.now().minusDays(90), LocalDate.now());
        UUID proveedorId = p.uuid("proveedorId");
        HojaExcel resumen = HojaExcel.de(
                "Por proveedor",
                "Proveedor",
                "RUC",
                "Órdenes",
                "Monto ordenado",
                "Monto recibido",
                "Facturado",
                "Pagado",
                "Saldo");
        for (var r : comprasService.porProveedor(rango[0], rango[1], proveedorId)) {
            resumen.fila(
                    r.proveedorNombre(),
                    r.proveedorRuc(),
                    r.ordenes(),
                    r.montoOrdenado(),
                    r.montoRecibido(),
                    r.facturado(),
                    r.pagado(),
                    r.saldo());
        }
        HojaExcel ordenes = HojaExcel.de(
                "Órdenes",
                "Orden",
                "Estado",
                "Proveedor",
                "Total",
                "Creada por",
                "Requisición",
                "Creada",
                "Aprobada",
                "Recibido %");
        for (var o : ordenCompraService.listar(null, proveedorId, rango[0], rango[1])) {
            ordenes.fila(
                    o.codigo() == null ? "Borrador" : o.codigo(),
                    o.estado().name(),
                    o.proveedorNombre(),
                    o.total(),
                    o.creadaPorNombre(),
                    o.requisicionCodigo() == null ? "" : o.requisicionCodigo(),
                    o.creadaEn(),
                    o.aprobadaEn() == null ? "" : o.aprobadaEn(),
                    o.porcentajeRecibido());
        }
        return new Listado(
                List.of(resumen, ordenes), "compras-" + rango[0] + "-a-" + rango[1], "Compras por proveedor");
    }

    private Listado libroDiario(ParametrosDocumento p) {
        LocalDate[] rango = p.rango(LocalDate.now().withDayOfMonth(1), LocalDate.now());
        HojaExcel hoja = HojaExcel.de(
                "Libro diario", "Fecha", "Asiento", "Origen", "Estado", "Cuenta", "Nombre", "Glosa", "Debe", "Haber");
        for (var f : librosContables.diario(rango[0], rango[1])) {
            hoja.fila(
                    f.fecha(),
                    f.asiento(),
                    f.origen().name(),
                    f.estado().name(),
                    f.cuentaCodigo(),
                    f.cuentaNombre(),
                    f.glosa(),
                    f.debe(),
                    f.haber());
        }
        return new Listado(List.of(hoja), "libro-diario-" + rango[0] + "-a-" + rango[1], "Libro diario");
    }

    private Listado libroMayor(ParametrosDocumento p) {
        LocalDate[] rango = p.rango(LocalDate.now().withDayOfMonth(1), LocalDate.now());
        UUID cuentaId = p.uuid("cuentaId");
        if (cuentaId == null) {
            throw new com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException("Indique la cuenta del mayor");
        }
        var mayor = librosContables.mayor(cuentaId, rango[0], rango[1]);
        HojaExcel hoja = HojaExcel.de("Libro mayor", "Fecha", "Asiento", "Glosa", "Debe", "Haber", "Saldo");
        hoja.fila(rango[0], "", "Saldo inicial", "", "", mayor.saldoInicial());
        for (var f : mayor.movimientos()) {
            hoja.fila(f.fecha(), f.asiento(), f.glosa(), f.debe(), f.haber(), f.saldo());
        }
        return new Listado(
                List.of(hoja),
                "libro-mayor-" + mayor.cuenta().codigo() + "-" + rango[0] + "-a-" + rango[1],
                "Libro mayor de " + mayor.cuenta().codigo() + " "
                        + mayor.cuenta().nombre());
    }

    private Listado balanceComprobacion(ParametrosDocumento p) {
        LocalDate[] rango = p.rango(LocalDate.now().withDayOfMonth(1), LocalDate.now());
        var balance = librosContables.balanceComprobacion(rango[0], rango[1]);
        HojaExcel hoja = HojaExcel.de(
                "Balance de comprobación",
                "Cuenta",
                "Nombre",
                "Inicial deudor",
                "Inicial acreedor",
                "Debe",
                "Haber",
                "Saldo deudor",
                "Saldo acreedor");
        for (var f : balance.filas()) {
            hoja.fila(
                    f.codigo(),
                    f.nombre(),
                    f.inicialDeudor(),
                    f.inicialAcreedor(),
                    f.debe(),
                    f.haber(),
                    f.finalDeudor(),
                    f.finalAcreedor());
        }
        hoja.fila(
                "",
                "Totales",
                "",
                "",
                balance.totalDebe(),
                balance.totalHaber(),
                balance.totalDeudor(),
                balance.totalAcreedor());
        return new Listado(
                List.of(hoja), "balance-comprobacion-" + rango[0] + "-a-" + rango[1], "Balance de comprobación");
    }

    private Listado kardex(ParametrosDocumento p) {
        LocalDate[] rango = p.rango(LocalDate.now().minusDays(30), LocalDate.now());
        List<MovimientoResponse> lista = almacenService.movimientos(
                p.uuid("productoId"), p.enumeracion("tipo", TipoMovimiento.class), rango[0], rango[1]);
        HojaExcel hoja = HojaExcel.de(
                "Kardex", "Fecha", "Producto", "Lote", "Tipo", "Motivo", "Cantidad", "Usuario", "Referencia");
        for (MovimientoResponse m : lista) {
            hoja.fila(
                    m.fecha(),
                    m.productoNombre(),
                    m.numeroLote(),
                    m.tipo().name(),
                    m.motivo() == null ? "" : m.motivo().name(),
                    m.cantidad(),
                    m.usuarioNombre(),
                    m.referencia() == null ? "" : m.referencia());
        }
        return new Listado(List.of(hoja), "kardex-" + rango[0] + "-a-" + rango[1], "Kardex del almacén");
    }

    private Listado stock(UsuarioPrincipal actor, ParametrosDocumento p) {
        List<ProductoResponse> lista = productoService.listar(
                actor,
                p.texto("q"),
                p.enumeracion("tipo", TipoProducto.class),
                p.booleano("bajoStock"),
                p.booleano("activo"));
        HojaExcel hoja = HojaExcel.de(
                "Stock",
                "Código",
                "Producto",
                "Tipo",
                "Presentación",
                "Unidad",
                "Precio de venta",
                "Stock actual",
                "Stock mínimo",
                "Próximo vencimiento",
                "Requiere receta",
                "Activo");
        for (ProductoResponse r : lista) {
            hoja.fila(
                    r.codigo(),
                    r.nombre(),
                    r.tipo().name(),
                    r.presentacion() == null ? "" : r.presentacion(),
                    r.unidad() == null ? "" : r.unidad(),
                    r.precioVenta(),
                    r.stockActual(),
                    r.stockMinimo(),
                    r.proximoVencimiento(),
                    r.requiereReceta(),
                    r.activo());
        }
        return new Listado(List.of(hoja), "stock-" + LocalDate.now(), "Stock del almacén");
    }

    private Listado ausencias(UsuarioPrincipal actor, ParametrosDocumento p) {
        LocalDate[] rango =
                p.rango(LocalDate.now().minusDays(30), LocalDate.now().plusDays(90));
        List<AusenciaResponse> lista = ausenciaService.listar(
                p.enumeracion("estado", EstadoAusencia.class),
                p.enumeracion("area", AreaLaboral.class),
                rango[0],
                rango[1],
                actor);
        HojaExcel hoja = HojaExcel.de(
                "Ausencias", "Empleado", "Área", "Tipo", "Desde", "Hasta", "Días", "Estado", "Resuelto por", "Motivo");
        for (AusenciaResponse a : lista) {
            hoja.fila(
                    a.empleadoNombre(),
                    a.area().name(),
                    a.tipo().name(),
                    a.fechaInicio(),
                    a.fechaFin(),
                    a.dias(),
                    a.estado().name(),
                    a.resueltoPor() == null ? "" : a.resueltoPor(),
                    a.motivo() == null ? "" : a.motivo());
        }
        return new Listado(List.of(hoja), "ausencias-" + rango[0] + "-a-" + rango[1], "Ausencias del personal");
    }

    private Listado reportePlanilla(UsuarioPrincipal actor, ParametrosDocumento p) {
        java.util.UUID id = p.uuid("planillaId");
        if (id == null) {
            throw new IllegalArgumentException("Indique la planilla");
        }
        var detalle = planillaService.obtener(actor, id);
        HojaExcel hoja =
                HojaExcel.de("Planilla", "Boleta", "Empleado", "Cargo", "Ingresos", "Descuentos", "Neto", "Banco");
        for (var b : detalle.boletas()) {
            hoja.fila(
                    b.numero(),
                    b.empleadoNombre(),
                    b.cargo(),
                    b.totalIngresos(),
                    b.totalDescuentos(),
                    b.neto(),
                    b.banco() == null ? "" : b.banco());
        }
        var pl = detalle.planilla();
        return new Listado(List.of(hoja), "planilla-" + pl.codigo(), "Planilla " + pl.codigo());
    }

    private Listado asistenciaPersonal(ParametrosDocumento p) {
        LocalDate[] rango = p.rango(LocalDate.now().withDayOfMonth(1), LocalDate.now());
        HojaExcel hoja = HojaExcel.de(
                "Asistencia",
                "Fecha",
                "Empleado",
                "Estado",
                "Entrada",
                "Salida",
                "Tardanza min",
                "Trabajados min",
                "Extra min",
                "Destino extra",
                "Justificación");
        for (var d : asistenciaService.dias(p.uuid("empleadoId"), rango[0], rango[1])) {
            hoja.fila(
                    d.fecha(),
                    d.empleadoNombre(),
                    d.estado().name(),
                    d.entrada() == null ? "" : d.entrada().toLocalTime().toString(),
                    d.salida() == null ? "" : d.salida().toLocalTime().toString(),
                    d.minutosTardanza(),
                    d.minutosTrabajados(),
                    d.minutosExtra(),
                    d.extraDestino().name(),
                    d.justificacion() == null ? "" : d.justificacion());
        }
        return new Listado(List.of(hoja), "asistencia-" + rango[0] + "-a-" + rango[1], "Asistencia del personal");
    }

    private Listado turnos(ParametrosDocumento p) {
        LocalDate[] rango = p.rango(LocalDate.now(), LocalDate.now().plusDays(13));
        List<TurnoPersonalResponse> lista =
                turnoService.listar(rango[0], rango[1], p.enumeracion("area", AreaLaboral.class), p.uuid("empleadoId"));
        HojaExcel hoja =
                HojaExcel.de("Turnos", "Fecha", "Empleado", "Área", "Turno", "Inicio", "Fin", "Horas", "Notas");
        for (TurnoPersonalResponse t : lista) {
            hoja.fila(
                    t.fecha(),
                    t.empleadoNombre(),
                    t.area().name(),
                    t.plantillaNombre(),
                    Formato.hora(t.horaInicio()),
                    Formato.hora(t.horaFin()),
                    t.horas(),
                    t.notas() == null ? "" : t.notas());
        }
        return new Listado(List.of(hoja), "turnos-" + rango[0] + "-a-" + rango[1], "Turnos del personal");
    }

    private Listado misCitas(UsuarioPrincipal actor, ParametrosDocumento p) {
        LocalDate[] rango =
                p.rango(LocalDate.now().minusDays(30), LocalDate.now().plusDays(30));
        List<CitaResponse> lista = citaService.agendaMedico(actor, null, rango[0], rango[1]);
        HojaExcel hoja = HojaExcel.de(
                "Citas", "Fecha y hora", "Paciente", "DNI", "Especialidad", "Consultorio", "Estado", "Motivo");
        for (CitaResponse c : lista) {
            hoja.fila(
                    c.fechaHora(),
                    c.pacienteNombre(),
                    c.pacienteDni() == null ? "" : c.pacienteDni(),
                    c.especialidad(),
                    c.consultorio() == null ? "" : c.consultorio(),
                    Etiquetas.estadoCita(c.estado()),
                    c.motivo() == null ? "" : c.motivo());
        }
        return new Listado(List.of(hoja), "mis-citas-" + rango[0] + "-a-" + rango[1], "Mis citas");
    }
}

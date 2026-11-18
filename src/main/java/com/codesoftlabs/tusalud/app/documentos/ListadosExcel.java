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

    public Listado armar(UsuarioPrincipal actor, TipoDocumento tipo, ParametrosDocumento parametros) {
        return switch (tipo) {
            case LISTADO_COMPROBANTES -> comprobantes(parametros);
            case KARDEX -> kardex(parametros);
            case STOCK -> stock(actor, parametros);
            case AUSENCIAS -> ausencias(actor, parametros);
            case TURNOS -> turnos(parametros);
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

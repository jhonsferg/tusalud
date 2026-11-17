package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.enums.AreaCama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoFarmacia;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ReporteRepository {

    private static final String PKG = "com.codesoftlabs.tusalud.app.entities.enums.";

    @PersistenceContext
    private EntityManager em;

    public record CitaDia(LocalDate dia, EstadoCita estado, long total) {}

    public record ConteoDia(LocalDate dia, long total) {}

    public record CitaHora(LocalDate dia, int hora, long total) {}

    public record ConteoNombre(String nombre, long total) {}

    public record CitaEspecialidad(String especialidad, EstadoCita estado, long total) {}

    public record MedicoConteo(UUID medicoId, String nombre, long total) {}

    public record HorarioFila(UUID medicoId, DayOfWeek dia, LocalTime inicio, LocalTime fin, int duracion) {}

    public record MontoDia(LocalDate dia, String clave, long transacciones, BigDecimal total) {}

    public record ComprobanteDia(
            LocalDate dia, OrigenComprobante origen, MetodoPagoCaja metodo, long transacciones, BigDecimal total) {}

    public record DispensacionDia(LocalDate dia, MetodoPagoFarmacia metodo, long transacciones, BigDecimal total) {}

    public record MontoNombre(String nombre, long cantidad, BigDecimal total) {}

    public record Cantidad(long cantidad, BigDecimal monto) {}

    public record DiferenciaCaja(String cajero, BigDecimal diferencia) {}

    public record Intervalo(LocalDateTime inicio, LocalDateTime fin, AreaCama area) {}

    public record CamaEstado(AreaCama area, EstadoCama estado, long total) {}

    public record ProductoStock(String codigo, String nombre, int minimo, long stock) {}

    public record ValoracionMedico(UUID medicoId, String nombre, int puntuacion, long total) {}

    public record ValoracionEspecialidad(String especialidad, int puntuacion, long total) {}

    public record OrdenLab(PrioridadOrden prioridad, LocalDateTime fecha, LocalDateTime completada) {}

    public record NivelConteo(int nivel, long total) {}

    public List<CitaDia> citasPorDiaYEstado(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT CAST(c.fechaHora AS LocalDate), c.estado, COUNT(c) FROM Cita c
                WHERE c.fechaHora >= :d AND c.fechaHora < :h
                GROUP BY CAST(c.fechaHora AS LocalDate), c.estado
                """, d, h, null, f -> new CitaDia((LocalDate) f[0], (EstadoCita) f[1], ((Number) f[2]).longValue()));
    }

    public List<ConteoDia> citasSinAtenderPasadasPorDia(LocalDateTime d, LocalDateTime h, LocalDateTime ahora) {
        return filas(
                """
                SELECT CAST(c.fechaHora AS LocalDate), COUNT(c) FROM Cita c
                WHERE c.fechaHora >= :d AND c.fechaHora < :h AND c.fechaHora < :ahora
                  AND c.estado IN (%sEstadoCita.PENDIENTE, %sEstadoCita.CONFIRMADA)
                GROUP BY CAST(c.fechaHora AS LocalDate)
                """.formatted(PKG, PKG),
                d,
                h,
                ahora,
                f -> new ConteoDia((LocalDate) f[0], ((Number) f[1]).longValue()));
    }

    public List<CitaHora> citasPorDiaYHora(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT CAST(c.fechaHora AS LocalDate), EXTRACT(HOUR FROM c.fechaHora), COUNT(c) FROM Cita c
                WHERE c.fechaHora >= :d AND c.fechaHora < :h AND c.estado <> %sEstadoCita.CANCELADA
                GROUP BY CAST(c.fechaHora AS LocalDate), EXTRACT(HOUR FROM c.fechaHora)
                """.formatted(PKG),
                d,
                h,
                null,
                f -> new CitaHora((LocalDate) f[0], ((Number) f[1]).intValue(), ((Number) f[2]).longValue()));
    }

    public List<CitaEspecialidad> citasPorEspecialidadYEstado(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT e.nombre, c.estado, COUNT(c) FROM Cita c JOIN c.especialidad e
                WHERE c.fechaHora >= :d AND c.fechaHora < :h
                GROUP BY e.nombre, c.estado
                """,
                d,
                h,
                null,
                f -> new CitaEspecialidad((String) f[0], (EstadoCita) f[1], ((Number) f[2]).longValue()));
    }

    public List<ConteoNombre> citasSinAtenderPasadasPorEspecialidad(
            LocalDateTime d, LocalDateTime h, LocalDateTime ahora) {
        return filas(
                """
                SELECT e.nombre, COUNT(c) FROM Cita c JOIN c.especialidad e
                WHERE c.fechaHora >= :d AND c.fechaHora < :h AND c.fechaHora < :ahora
                  AND c.estado IN (%sEstadoCita.PENDIENTE, %sEstadoCita.CONFIRMADA)
                GROUP BY e.nombre
                """.formatted(PKG, PKG),
                d,
                h,
                ahora,
                f -> new ConteoNombre((String) f[0], ((Number) f[1]).longValue()));
    }

    public List<MedicoConteo> citasActivasPorMedico(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT m.id, u.nombre, u.apellido, COUNT(c) FROM Cita c JOIN c.medico m JOIN m.usuario u
                WHERE c.fechaHora >= :d AND c.fechaHora < :h AND c.estado <> %sEstadoCita.CANCELADA
                GROUP BY m.id, u.nombre, u.apellido
                """.formatted(PKG),
                d,
                h,
                null,
                f -> new MedicoConteo((UUID) f[0], f[1] + " " + f[2], ((Number) f[3]).longValue()));
    }

    public List<HorarioFila> horarios() {
        return em
                .createQuery(
                        "SELECT m.id, hh.diaSemana, hh.horaInicio, hh.horaFin, hh.duracionCitaMinutos "
                                + "FROM HorarioAtencion hh JOIN hh.medico m",
                        Object[].class)
                .getResultList()
                .stream()
                .map(f -> new HorarioFila(
                        (UUID) f[0], (DayOfWeek) f[1], (LocalTime) f[2], (LocalTime) f[3], ((Number) f[4]).intValue()))
                .toList();
    }

    public List<MontoDia> pagosEnLineaPorDia(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT CAST(p.fechaPago AS LocalDate), p.metodoDescripcion, COUNT(p), SUM(p.total) FROM Pago p
                WHERE p.estado = %sEstadoPago.PAGADO AND p.fechaPago >= :d AND p.fechaPago < :h
                  AND (p.comprobante IS NULL OR p.comprobante NOT LIKE 'CJ-%%')
                GROUP BY CAST(p.fechaPago AS LocalDate), p.metodoDescripcion
                """.formatted(PKG),
                d,
                h,
                null,
                f -> new MontoDia(
                        (LocalDate) f[0], f[1] == null ? "" : (String) f[1], ((Number) f[2]).longValue(), (BigDecimal)
                                f[3]));
    }

    public List<MontoNombre> pagosPorEspecialidad(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT e.nombre, COUNT(p), SUM(p.total) FROM Pago p JOIN p.cita c JOIN c.especialidad e
                WHERE p.estado = %sEstadoPago.PAGADO AND p.fechaPago >= :d AND p.fechaPago < :h
                GROUP BY e.nombre
                """.formatted(PKG),
                d,
                h,
                null,
                f -> new MontoNombre((String) f[0], ((Number) f[1]).longValue(), (BigDecimal) f[2]));
    }

    public List<MontoNombre> pagosPorMedico(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT u.nombre, u.apellido, COUNT(p), SUM(p.total) FROM Pago p JOIN p.cita c JOIN c.medico m JOIN m.usuario u
                WHERE p.estado = %sEstadoPago.PAGADO AND p.fechaPago >= :d AND p.fechaPago < :h
                GROUP BY m.id, u.nombre, u.apellido
                """.formatted(PKG),
                d,
                h,
                null,
                f -> new MontoNombre(f[0] + " " + f[1], ((Number) f[2]).longValue(), (BigDecimal) f[3]));
    }

    public List<ComprobanteDia> comprobantesEmitidosPorDia(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT CAST(c.fecha AS LocalDate), c.origen, c.metodoPago, COUNT(c), SUM(c.total) FROM Comprobante c
                WHERE c.estado = %sEstadoComprobante.EMITIDO AND c.origen <> %sOrigenComprobante.FARMACIA
                  AND c.fecha >= :d AND c.fecha < :h
                GROUP BY CAST(c.fecha AS LocalDate), c.origen, c.metodoPago
                """.formatted(PKG, PKG),
                d,
                h,
                null,
                f -> new ComprobanteDia(
                        (LocalDate) f[0],
                        (OrigenComprobante) f[1],
                        (MetodoPagoCaja) f[2],
                        ((Number) f[3]).longValue(),
                        (BigDecimal) f[4]));
    }

    public long comprobantesPorEstado(LocalDateTime d, LocalDateTime h, boolean anulados) {
        return em.createQuery(
                        "SELECT COUNT(c) FROM Comprobante c WHERE c.fecha >= :d AND c.fecha < :h AND c.estado = "
                                + PKG
                                + (anulados ? "EstadoComprobante.ANULADO" : "EstadoComprobante.EMITIDO"),
                        Long.class)
                .setParameter("d", d)
                .setParameter("h", h)
                .getSingleResult();
    }

    public List<DispensacionDia> dispensacionesPorDia(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT CAST(x.fecha AS LocalDate), x.metodoPago, COUNT(x), SUM(x.total) FROM Dispensacion x
                WHERE x.estado = %sEstadoDispensacion.EMITIDA AND x.fecha >= :d AND x.fecha < :h
                GROUP BY CAST(x.fecha AS LocalDate), x.metodoPago
                """.formatted(PKG),
                d,
                h,
                null,
                f -> new DispensacionDia(
                        (LocalDate) f[0], (MetodoPagoFarmacia) f[1], ((Number) f[2]).longValue(), (BigDecimal) f[3]));
    }

    public long dispensacionesAnuladas(LocalDateTime d, LocalDateTime h) {
        return em.createQuery(
                        "SELECT COUNT(x) FROM Dispensacion x WHERE x.fecha >= :d AND x.fecha < :h AND x.estado = "
                                + PKG
                                + "EstadoDispensacion.ANULADA",
                        Long.class)
                .setParameter("d", d)
                .setParameter("h", h)
                .getSingleResult();
    }

    public Cantidad hospitalizacionDevengada(LocalDateTime d, LocalDateTime h) {
        Object[] f = em.createQuery(
                        "SELECT COUNT(i), COALESCE(SUM(i.cargoAcumulado), 0) FROM IngresoHospitalario i "
                                + "WHERE i.estado = " + PKG
                                + "EstadoIngresoHosp.ALTA AND i.fechaAlta >= :d AND i.fechaAlta < :h",
                        Object[].class)
                .setParameter("d", d)
                .setParameter("h", h)
                .getSingleResult();
        return new Cantidad(((Number) f[0]).longValue(), (BigDecimal) f[1]);
    }

    public List<DiferenciaCaja> diferenciasDeCaja(LocalDateTime d, LocalDateTime h) {
        return filas("""
                SELECT u.nombre, u.apellido, s.diferencia FROM SesionCaja s JOIN s.cajero u
                WHERE s.estado = %sEstadoCaja.CERRADA AND s.cierre >= :d AND s.cierre < :h AND s.diferencia IS NOT NULL
                """.formatted(PKG), d, h, null, f -> new DiferenciaCaja(f[0] + " " + f[1], (BigDecimal) f[2]));
    }

    public Cantidad citasPorCobrar() {
        Object[] f = em.createQuery(
                        "SELECT COUNT(c), COALESCE(SUM(c.monto), 0) FROM Cita c LEFT JOIN c.pago p "
                                + "WHERE c.estado = " + PKG + "EstadoCita.ATENDIDA "
                                + "AND (p IS NULL OR p.estado = " + PKG + "EstadoPago.REEMBOLSADO)",
                        Object[].class)
                .getSingleResult();
        return new Cantidad(((Number) f[0]).longValue(), (BigDecimal) f[1]);
    }

    public Cantidad laboratorioPorCobrar() {
        Object[] f = em.createQuery(
                        "SELECT COUNT(o), COALESCE(SUM(o.total), 0) FROM OrdenLaboratorio o "
                                + "WHERE o.cobrado = false AND o.estado <> " + PKG + "EstadoOrdenLab.CANCELADA",
                        Object[].class)
                .getSingleResult();
        return new Cantidad(((Number) f[0]).longValue(), (BigDecimal) f[1]);
    }

    public List<LocalDateTime[]> esperasDeTurnos(LocalDateTime d, LocalDateTime h) {
        return filas("""
                SELECT t.creadoEn, t.llamadoEn FROM Turno t
                WHERE t.llamadoEn IS NOT NULL AND t.creadoEn >= :d AND t.creadoEn < :h
                """, d, h, null, f -> new LocalDateTime[] {(LocalDateTime) f[0], (LocalDateTime) f[1]});
    }

    public List<NivelConteo> emergenciasPorNivel(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT i.triajeNivel, COUNT(i) FROM IngresoHospitalario i
                WHERE i.tipo = %sTipoIngresoHosp.EMERGENCIA AND i.triajeNivel IS NOT NULL
                  AND i.fechaIngreso >= :d AND i.fechaIngreso < :h
                GROUP BY i.triajeNivel
                """.formatted(PKG),
                d,
                h,
                null,
                f -> new NivelConteo(((Number) f[0]).intValue(), ((Number) f[1]).longValue()));
    }

    public List<CamaEstado> camasPorAreaYEstado() {
        return em
                .createQuery(
                        "SELECT c.area, c.estado, COUNT(c) FROM Cama c WHERE c.activa = true GROUP BY c.area, c.estado",
                        Object[].class)
                .getResultList()
                .stream()
                .map(f -> new CamaEstado((AreaCama) f[0], (EstadoCama) f[1], ((Number) f[2]).longValue()))
                .toList();
    }

    public List<Intervalo> ingresosConCama(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT i.fechaIngreso, i.fechaAlta, c.area FROM IngresoHospitalario i JOIN i.cama c
                WHERE i.estado <> %sEstadoIngresoHosp.CANCELADO AND i.fechaIngreso < :h
                  AND (i.fechaAlta IS NULL OR i.fechaAlta >= :d)
                """.formatted(PKG),
                d,
                h,
                null,
                f -> new Intervalo((LocalDateTime) f[0], (LocalDateTime) f[1], (AreaCama) f[2]));
    }

    public List<LocalDateTime[]> estanciasConAlta(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT i.fechaIngreso, i.fechaAlta FROM IngresoHospitalario i
                WHERE i.estado = %sEstadoIngresoHosp.ALTA AND i.fechaAlta >= :d AND i.fechaAlta < :h
                """.formatted(PKG), d, h, null, f -> new LocalDateTime[] {(LocalDateTime) f[0], (LocalDateTime) f[1]});
    }

    public List<ProductoStock> productosEnStockCritico(LocalDate hoy) {
        return em.createQuery("""
                        SELECT p.codigo, p.nombre, p.stockMinimo, COALESCE(SUM(l.cantidadActual), 0)
                        FROM Producto p LEFT JOIN Lote l ON l.producto = p AND l.cantidadActual > 0 AND l.fechaVencimiento >= :hoy
                        WHERE p.activo = true
                        GROUP BY p.id, p.codigo, p.nombre, p.stockMinimo
                        HAVING COALESCE(SUM(l.cantidadActual), 0) <= p.stockMinimo
                        """, Object[].class).setParameter("hoy", hoy).getResultList().stream()
                .map(f -> new ProductoStock(
                        (String) f[0], (String) f[1], ((Number) f[2]).intValue(), ((Number) f[3]).longValue()))
                .toList();
    }

    public Cantidad lotesPorVencer(LocalDate desde, LocalDate hasta) {
        Object[] f = em.createQuery(
                        "SELECT COUNT(l), COALESCE(SUM(l.cantidadActual), 0) FROM Lote l "
                                + "WHERE l.cantidadActual > 0 AND l.fechaVencimiento >= :desde AND l.fechaVencimiento <= :hasta",
                        Object[].class)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getSingleResult();
        return new Cantidad(((Number) f[0]).longValue(), BigDecimal.valueOf(((Number) f[1]).longValue()));
    }

    public Cantidad lotesVencidos(LocalDate hoy) {
        Object[] f = em.createQuery(
                        "SELECT COUNT(l), COALESCE(SUM(l.cantidadActual), 0) FROM Lote l "
                                + "WHERE l.cantidadActual > 0 AND l.fechaVencimiento < :hoy",
                        Object[].class)
                .setParameter("hoy", hoy)
                .getSingleResult();
        return new Cantidad(((Number) f[0]).longValue(), BigDecimal.valueOf(((Number) f[1]).longValue()));
    }

    public Object[] valoracionPromedio(LocalDateTime d, LocalDateTime h) {
        return em.createQuery(
                        "SELECT AVG(v.puntuacion), COUNT(v) FROM Valoracion v WHERE v.createdAt >= :d AND v.createdAt < :h",
                        Object[].class)
                .setParameter("d", d)
                .setParameter("h", h)
                .getSingleResult();
    }

    public List<ValoracionMedico> valoracionesPorMedico(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT m.id, u.nombre, u.apellido, v.puntuacion, COUNT(v) FROM Valoracion v JOIN v.medico m JOIN m.usuario u
                WHERE v.createdAt >= :d AND v.createdAt < :h
                GROUP BY m.id, u.nombre, u.apellido, v.puntuacion
                """,
                d,
                h,
                null,
                f -> new ValoracionMedico(
                        (UUID) f[0], f[1] + " " + f[2], ((Number) f[3]).intValue(), ((Number) f[4]).longValue()));
    }

    public List<ValoracionEspecialidad> valoracionesPorEspecialidad(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT e.nombre, v.puntuacion, COUNT(v) FROM Valoracion v JOIN v.cita c JOIN c.especialidad e
                WHERE v.createdAt >= :d AND v.createdAt < :h
                GROUP BY e.nombre, v.puntuacion
                """,
                d,
                h,
                null,
                f -> new ValoracionEspecialidad(
                        (String) f[0], ((Number) f[1]).intValue(), ((Number) f[2]).longValue()));
    }

    public List<OrdenLab> ordenesCompletadas(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT o.prioridad, o.fecha, o.completadaEn FROM OrdenLaboratorio o
                WHERE o.estado = %sEstadoOrdenLab.COMPLETADA AND o.completadaEn >= :d AND o.completadaEn < :h
                """.formatted(PKG),
                d,
                h,
                null,
                f -> new OrdenLab((PrioridadOrden) f[0], (LocalDateTime) f[1], (LocalDateTime) f[2]));
    }

    public long ordenesCanceladas(LocalDateTime d, LocalDateTime h) {
        return em.createQuery(
                        "SELECT COUNT(o) FROM OrdenLaboratorio o WHERE o.fecha >= :d AND o.fecha < :h AND o.estado = "
                                + PKG
                                + "EstadoOrdenLab.CANCELADA",
                        Long.class)
                .setParameter("d", d)
                .setParameter("h", h)
                .getSingleResult();
    }

    public List<ConteoNombre> motivosAnulacionComprobantes(LocalDateTime d, LocalDateTime h) {
        return filas(
                """
                SELECT c.motivoAnulacion, COUNT(c) FROM Comprobante c
                WHERE c.estado = %sEstadoComprobante.ANULADO AND c.anuladoEn >= :d AND c.anuladoEn < :h
                GROUP BY c.motivoAnulacion
                """.formatted(PKG),
                d,
                h,
                null,
                f -> new ConteoNombre(f[0] == null ? "" : (String) f[0], ((Number) f[1]).longValue()));
    }

    private <T> List<T> filas(
            String jpql,
            LocalDateTime d,
            LocalDateTime h,
            LocalDateTime ahora,
            java.util.function.Function<Object[], T> mapa) {
        var consulta = em.createQuery(jpql, Object[].class).setParameter("d", d).setParameter("h", h);
        if (ahora != null) {
            consulta.setParameter("ahora", ahora);
        }
        return consulta.getResultList().stream().map(mapa).toList();
    }
}

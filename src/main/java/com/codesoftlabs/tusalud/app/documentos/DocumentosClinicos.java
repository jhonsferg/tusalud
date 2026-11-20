package com.codesoftlabs.tusalud.app.documentos;

import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.HistoriaClinicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenLabResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteHistoriaResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.Comprobante;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.ComprobanteRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
import com.codesoftlabs.tusalud.app.services.interfaces.HistoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.ImagenesService;
import com.codesoftlabs.tusalud.app.services.interfaces.LaboratorioService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DocumentosClinicos {

    private static final int ATENCIONES_RESUMEN = 30;

    private final ComprobanteRepository comprobanteRepository;
    private final AtencionRepository atencionRepository;
    private final PacienteRepository pacienteRepository;
    private final LaboratorioService laboratorioService;
    private final ImagenesService imagenesService;
    private final CitaService citaService;
    private final com.codesoftlabs.tusalud.app.repositories.LiquidacionRepository liquidacionRepository;
    private final com.codesoftlabs.tusalud.app.repositories.CuentaAseguradoraRepository cuentaAseguradoraRepository;
    private final HistoriaClinicaService historiaClinicaService;
    private final com.codesoftlabs.tusalud.app.repositories.OrdenCompraRepository ordenCompraRepository;
    private final com.codesoftlabs.tusalud.app.services.planilla.PlanillaService planillaService;

    @Transactional(readOnly = true)
    public DocumentoPreparado preparar(UsuarioPrincipal actor, TipoDocumento tipo, UUID id) {
        return switch (tipo) {
            case COMPROBANTE -> comprobante(actor, id);
            case LIQUIDACION -> liquidacion(id);
            case ORDEN_COMPRA -> ordenCompra(id);
            case BOLETA_PAGO -> boletaPago(actor, id);
            case RECETA -> receta(actor, id);
            case RESULTADOS_LABORATORIO -> resultadosLaboratorio(actor, id);
            case INFORME_IMAGEN -> informeImagen(actor, id);
            case CONSTANCIA -> constancia(actor, id);
            case RESUMEN_HISTORIA -> resumenHistoria(actor, id);
            default -> throw new IllegalArgumentException("El documento no es de generación directa: " + tipo);
        };
    }

    private DocumentoPreparado comprobante(UsuarioPrincipal actor, UUID id) {
        Comprobante comprobante = comprobanteRepository
                .findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante", "id", id));
        boolean admin = tiene(actor, RolNombre.ROLE_ADMIN);
        boolean cajero = tiene(actor, RolNombre.ROLE_CAJA)
                && comprobante.getCaja().getCajero().getId().equals(actor.getId());
        boolean paciente = comprobante.getPaciente() != null
                && comprobante.getPaciente().getUsuario().getId().equals(actor.getId())
                && tiene(actor, RolNombre.ROLE_PACIENTE);
        if (!admin && !cajero && !paciente) {
            throw new ForbiddenOperationException("No tiene acceso a este comprobante");
        }
        boolean factura = comprobante.getTipo().name().equals("FACTURA");
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.COMPROBANTE)
                .titulo((factura ? "Factura " : "Boleta ") + comprobante.getCodigo(), "Comprobante de pago")
                .par("Fecha", Formato.fechaHora(comprobante.getFecha()))
                .par("Cliente", comprobante.getClienteNombre())
                .par(
                        comprobante.getClienteTipoDoc().name().equals("SIN_DOC")
                                ? "Documento"
                                : comprobante.getClienteTipoDoc().name(),
                        comprobante.getClienteDoc() == null ? "Sin documento" : comprobante.getClienteDoc())
                .par("Método de pago", Etiquetas.metodoPago(comprobante.getMetodoPago()))
                .par("Origen", Etiquetas.origen(comprobante.getOrigen()))
                .par("Estado", comprobante.getEstado() == EstadoComprobante.ANULADO ? "Anulado" : "Emitido")
                .par("Cajero", comprobante.getCaja().getCajero().getNombreCompleto());
        comprobante
                .getItems()
                .forEach(item -> ficha.fila(
                        item.getDescripcion(),
                        String.valueOf(item.getCantidad()),
                        Formato.moneda(item.getPrecioUnitario()),
                        Formato.moneda(item.getSubtotal())));
        ficha.total("Subtotal", Formato.moneda(comprobante.getSubtotal()))
                .total("IGV 18 %", Formato.moneda(comprobante.getIgv()))
                .total("Total", Formato.moneda(comprobante.getTotal()));
        if (comprobante.getEstado() == EstadoComprobante.ANULADO) {
            ficha.banner("COMPROBANTE ANULADO")
                    .nota("Motivo de la anulación", Formato.texto(comprobante.getMotivoAnulacion()));
        }
        return new DocumentoPreparado(
                ficha, null, comprobante.getId().toString(), "comprobante-" + comprobante.getCodigo());
    }

    private DocumentoPreparado liquidacion(UUID id) {
        com.codesoftlabs.tusalud.app.entities.Liquidacion liquidacion = liquidacionRepository
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación", "id", id));
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.LIQUIDACION)
                .titulo("Liquidación " + liquidacion.getCodigo(), "Cuentas por cobrar a la aseguradora")
                .par("Aseguradora", liquidacion.getAseguradora().getNombre())
                .par("RUC", Formato.texto(liquidacion.getAseguradora().getRuc()))
                .par("Periodo", liquidacion.getDesde() + " al " + liquidacion.getHasta())
                .par("Estado", liquidacion.getEstado().name())
                .par("Cuentas", String.valueOf(liquidacion.getCantidadCuentas()))
                .par("Creada por", liquidacion.getCreadaPor().getNombreCompleto());
        cuentaAseguradoraRepository
                .deLiquidacion(id)
                .forEach(cuenta -> ficha.fila(
                        cuenta.getCategoria().name() + " · " + cuenta.getDescripcion(),
                        cuenta.getFecha().toLocalDate().toString(),
                        Formato.moneda(cuenta.getMontoCubierto()),
                        Formato.moneda(cuenta.getCopago())));
        ficha.total("Total liquidado", Formato.moneda(liquidacion.getTotal()));
        if (liquidacion.getMontoPagado() != null) {
            ficha.total("Pagado", Formato.moneda(liquidacion.getMontoPagado()));
            ficha.total("Glosa", Formato.moneda(liquidacion.getMontoGlosa()));
        }
        return new DocumentoPreparado(ficha, null, id.toString(), "liquidacion-" + liquidacion.getCodigo());
    }

    private DocumentoPreparado ordenCompra(UUID id) {
        com.codesoftlabs.tusalud.app.entities.OrdenCompra orden = ordenCompraRepository
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra", "id", id));
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.ORDEN_COMPRA)
                .titulo("Orden de compra " + orden.codigoVisible(), "Adquisición de insumos y medicamentos")
                .par("Proveedor", orden.getProveedor().getRazonSocial())
                .par("RUC", orden.getProveedor().getRuc())
                .par("Estado", orden.getEstado().name())
                .par("Creada por", orden.getCreadaPor().getNombreCompleto())
                .par(
                        "Aprobada por",
                        orden.getAprobadaPor() == null
                                ? "Pendiente"
                                : orden.getAprobadaPor().getNombreCompleto())
                .par(
                        "Aprobación",
                        orden.getAprobadaEn() == null ? "Pendiente" : Formato.fechaHora(orden.getAprobadaEn()))
                .par("Pago", orden.getCondicionPagoDias() == 0 ? "Contado" : orden.getCondicionPagoDias() + " días")
                .par(
                        "Entrega estimada",
                        orden.getFechaEntregaEstimada() == null
                                ? "Sin fecha"
                                : Formato.fecha(orden.getFechaEntregaEstimada()));
        orden.getItems()
                .forEach(item -> ficha.fila(
                        item.getProducto().getCodigo() + " · "
                                + item.getProducto().getNombre(),
                        String.valueOf(item.getCantidad()),
                        Formato.moneda(item.getPrecioUnitario()),
                        Formato.moneda(item.getSubtotal())));
        ficha.total("Subtotal", Formato.moneda(orden.getSubtotal()))
                .total("IGV 18 %", Formato.moneda(orden.getIgv()))
                .total("Total", Formato.moneda(orden.getTotal()));
        if (orden.getEstado() == com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoOrdenCompra.ANULADA) {
            ficha.banner("ORDEN ANULADA").nota("Motivo de la anulación", Formato.texto(orden.getMotivoResolucion()));
        } else if (orden.getHash() != null) {
            ficha.nota("Huella de la orden", orden.getHash());
        } else {
            ficha.banner("BORRADOR SIN APROBAR");
        }
        return new DocumentoPreparado(ficha, null, id.toString(), "orden-compra-" + orden.codigoVisible());
    }

    private DocumentoPreparado boletaPago(UsuarioPrincipal actor, UUID id) {
        com.codesoftlabs.tusalud.app.entities.BoletaPago boleta = planillaService.boletaParaDocumento(actor, id);
        com.codesoftlabs.tusalud.app.entities.Planilla planilla = boleta.getPlanilla();
        String periodo = String.format("%02d/%d", planilla.getMes(), planilla.getAnio());
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.BOLETA_PAGO)
                .titulo("Boleta de pago " + boleta.getNumero(), "Planilla " + periodo)
                .par("Trabajador", boleta.getEmpleadoNombre())
                .par("Cargo", boleta.getCargo())
                .par("Régimen", boleta.getRegimen().name())
                .par("Pensión", boleta.getSistemaPensionario().name())
                .par("Días laborados", String.valueOf(boleta.getDiasLaborados()))
                .par("Faltas", String.valueOf(boleta.getDiasFalta()))
                .par("Tardanza", boleta.getMinutosTardanza() + " min")
                .par("Horas extra", boleta.getMinutosExtraPagados() + " min");
        boleta.getLineas()
                .forEach(linea -> ficha.fila(
                        linea.getNombre(),
                        switch (linea.getTipo()) {
                            case INGRESO -> "Ingreso";
                            case DESCUENTO -> "Descuento";
                            case APORTE_EMPLEADOR -> "Aporte del empleador";
                        },
                        linea.getBase() == null ? "" : Formato.moneda(linea.getBase()),
                        Formato.moneda(linea.getMonto())));
        ficha.total("Total ingresos", Formato.moneda(boleta.getTotalIngresos()))
                .total("Total descuentos", Formato.moneda(boleta.getTotalDescuentos()))
                .total("Neto a pagar", Formato.moneda(boleta.getNeto()));
        ficha.nota("Huella de la boleta", boleta.getHash());
        if (planilla.getEstado() != com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoPlanilla.PAGADA
                && planilla.getEstado()
                        != com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoPlanilla.CERRADA) {
            ficha.banner("PLANILLA SIN PAGAR");
        }
        return new DocumentoPreparado(ficha, null, id.toString(), "boleta-" + boleta.getNumero());
    }

    private DocumentoPreparado receta(UsuarioPrincipal actor, UUID id) {
        Atencion atencion = atencionRepository
                .findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atención", "id", id));
        boolean medico = tiene(actor, RolNombre.ROLE_MEDICO)
                && atencion.getMedico().getUsuario().getId().equals(actor.getId());
        boolean paciente = tiene(actor, RolNombre.ROLE_PACIENTE)
                && atencion.getPaciente().getUsuario().getId().equals(actor.getId());
        boolean farmacia = tiene(actor, RolNombre.ROLE_FARMACIA);
        if (!medico && !paciente && !farmacia) {
            throw new ForbiddenOperationException("No tiene acceso a esta receta");
        }
        if (atencion.getReceta().isEmpty()) {
            throw new ConflictException("La atención no tiene receta");
        }
        Paciente datos = atencion.getPaciente();
        String doctor = atencion.getMedico().getUsuario().getNombreCompleto();
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.RECETA)
                .titulo(
                        "Receta médica",
                        "Atención del " + Formato.fecha(atencion.getFecha().toLocalDate()))
                .par("Paciente", datos.getUsuario().getNombreCompleto())
                .par("DNI", datos.getDni())
                .par("Médico", doctor)
                .par("Colegiatura", atencion.getMedico().getColegiatura())
                .par("Especialidad", atencion.getCita().getEspecialidad().getNombre())
                .par("Fecha", Formato.fechaHora(atencion.getFecha()))
                .par("Diagnóstico", diagnosticoCodificado(atencion));
        atencion.getReceta()
                .forEach(item -> ficha.fila(
                        item.getMedicamento(),
                        Formato.texto(item.getDosis()),
                        Formato.texto(item.getFrecuencia()),
                        Formato.texto(item.getDuracion())));
        if (atencion.getIndicaciones() != null && !atencion.getIndicaciones().isBlank()) {
            ficha.nota("Indicaciones", atencion.getIndicaciones());
        }
        ficha.firma(doctor, "CMP " + atencion.getMedico().getColegiatura());
        return new DocumentoPreparado(ficha, datos.getId(), atencion.getId().toString(), "receta-" + datos.getDni());
    }

    private DocumentoPreparado resultadosLaboratorio(UsuarioPrincipal actor, UUID id) {
        OrdenLabResponse orden = laboratorioService.obtenerOrden(actor, id);
        if (orden.estado() != EstadoOrdenLab.COMPLETADA) {
            throw new ConflictException("Los resultados solo se imprimen cuando la orden está completada");
        }
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.RESULTADOS_LABORATORIO)
                .titulo("Resultados de laboratorio", "Orden " + orden.codigo())
                .par("Paciente", orden.pacienteNombre())
                .par("DNI", Formato.texto(orden.dni()))
                .par("Médico solicitante", orden.medicoNombre())
                .par("Fecha de la orden", Formato.fechaHora(orden.fecha()))
                .par("Toma de muestra", Formato.fechaHora(orden.tomaMuestraEn()))
                .par("Completada", Formato.fechaHora(orden.completadaEn()));
        String validadoPor = null;
        for (OrdenLabResponse.ItemResponse item : orden.items()) {
            if (item.validadoPor() != null) {
                validadoPor = item.validadoPor();
            }
            if (item.resultados() == null || item.resultados().isEmpty()) {
                ficha.fila(item.examenNombre(), "", "", "", "", "Sin resultado");
                continue;
            }
            for (OrdenLabResponse.ResultadoResponse resultado : item.resultados()) {
                ficha.filaFuera(
                        resultado.fueraDeRango(),
                        item.examenNombre(),
                        resultado.parametro(),
                        Formato.texto(resultado.valor()),
                        Formato.texto(resultado.unidad()),
                        referencia(resultado),
                        resultado.fueraDeRango() ? "Fuera de rango" : "Normal");
            }
        }
        ficha.nota("Observaciones", "Los valores fuera del rango de referencia se resaltan en negrita.");
        if (validadoPor != null) {
            ficha.firma(validadoPor, "Validado en laboratorio");
        }
        return new DocumentoPreparado(ficha, orden.pacienteId(), orden.id().toString(), "resultados-" + orden.codigo());
    }

    private DocumentoPreparado informeImagen(UsuarioPrincipal actor, UUID id) {
        OrdenImagenResponse orden = imagenesService.obtenerOrden(actor, id);
        if (orden.estado() != EstadoOrdenImagen.INFORMADA || orden.informe() == null) {
            throw new ConflictException("El informe solo se imprime cuando el estudio está informado");
        }
        var informe = orden.informe();
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.INFORME_IMAGEN)
                .titulo("Informe de imágenes", "Orden " + orden.codigo())
                .par("Paciente", orden.pacienteNombre())
                .par("DNI", Formato.texto(orden.dni()))
                .par("Médico solicitante", orden.medicoNombre())
                .par("Fecha de la toma", Formato.fechaHora(orden.realizadaEn()))
                .par("Informado", Formato.fechaHora(orden.informadaEn()))
                .par("Versión", String.valueOf(informe.version()));
        orden.estudios()
                .forEach(estudio -> ficha.fila(
                        estudio.codigo(), estudio.nombre(), Formato.texto(estudio.modalidad()), estudio.region()));
        if (Boolean.TRUE.equals(informe.hallazgoCritico())) {
            ficha.banner("HALLAZGO CRÍTICO");
        }
        ficha.nota(
                "Hallazgos y conclusión",
                Formato.limpiar(informe.hallazgos()) + "\n\nConclusión: " + Formato.limpiar(informe.conclusion()));
        ficha.firma(informe.firmaNombre(), "CMP " + Formato.texto(informe.firmaColegiatura()));
        return new DocumentoPreparado(
                ficha, orden.pacienteId(), orden.id().toString(), "informe-imagen-" + orden.codigo());
    }

    private DocumentoPreparado constancia(UsuarioPrincipal actor, UUID id) {
        CitaResponse cita = citaService.obtenerCita(id, actor);
        if (cita.estado() == EstadoCita.CANCELADA) {
            throw new ConflictException("No se emite constancia de una cita cancelada");
        }
        boolean atendida = cita.estado() == EstadoCita.ATENDIDA;
        String texto =
                "Se deja constancia de que " + cita.pacienteNombre() + ", con DNI " + Formato.texto(cita.pacienteDni())
                        + ", " + (atendida ? "fue atendido(a)" : "tiene una cita programada")
                        + " el " + Formato.fechaHora(cita.fechaHora()) + " en la especialidad de " + cita.especialidad()
                        + " con " + cita.medicoNombre() + ".";
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.CONSTANCIA)
                .titulo(atendida ? "Constancia de atención" : "Constancia de cita", "TuSalud")
                .par("Paciente", cita.pacienteNombre())
                .par("DNI", Formato.texto(cita.pacienteDni()))
                .par("Médico", cita.medicoNombre())
                .par("Especialidad", cita.especialidad())
                .par("Fecha y hora", Formato.fechaHora(cita.fechaHora()))
                .par("Consultorio", Formato.texto(cita.consultorio()))
                .par("Estado", Etiquetas.estadoCita(cita.estado()))
                .nota("Constancia", texto)
                .firma(cita.medicoNombre(), cita.especialidad());
        return new DocumentoPreparado(
                ficha, cita.pacienteId(), cita.id().toString(), "constancia-" + cita.pacienteDni());
    }

    private DocumentoPreparado resumenHistoria(UsuarioPrincipal actor, UUID pacienteId) {
        HistoriaClinicaResponse historia;
        if (tiene(actor, RolNombre.ROLE_PACIENTE) && !tiene(actor, RolNombre.ROLE_MEDICO)) {
            Paciente propio = pacienteRepository
                    .findByUsuarioId(actor.getId())
                    .orElseThrow(() -> new ForbiddenOperationException("No tiene acceso a esta historia clínica"));
            if (!propio.getId().equals(pacienteId)) {
                throw new ForbiddenOperationException("No tiene acceso a esta historia clínica");
            }
            historia = historiaClinicaService.obtenerMia(actor);
        } else {
            historia = historiaClinicaService.obtenerDePaciente(pacienteId, actor);
        }
        PacienteHistoriaResponse datos = historia.paciente();
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.RESUMEN_HISTORIA)
                .titulo("Resumen de historia clínica", "Historia " + Formato.texto(datos.historialClinicoNumero()))
                .par("Paciente", datos.nombreCompleto())
                .par("DNI", datos.dni())
                .par("Edad", datos.edad() == null ? "" : datos.edad() + " años")
                .par("Grupo sanguíneo", Formato.texto(datos.grupoSanguineo()))
                .par(
                        "Alergias",
                        datos.alergias() == null || datos.alergias().isBlank() ? "Sin registrar" : datos.alergias())
                .par(
                        "Antecedentes",
                        datos.antecedentes() == null || datos.antecedentes().isBlank()
                                ? "Sin registrar"
                                : datos.antecedentes());
        List<AtencionResponse> atenciones =
                historia.atenciones().stream().limit(ATENCIONES_RESUMEN).toList();
        atenciones.forEach(atencion -> ficha.fila(
                Formato.fecha(atencion.fecha().toLocalDate()),
                atencion.medicoNombre(),
                atencion.especialidad(),
                diagnosticoCodificado(atencion),
                Formato.texto(atencion.tratamiento())));
        if (historia.atenciones().size() > ATENCIONES_RESUMEN) {
            ficha.nota(
                    "Alcance",
                    "El resumen muestra las " + ATENCIONES_RESUMEN + " atenciones más recientes de "
                            + historia.atenciones().size() + " registradas.");
        }
        return new DocumentoPreparado(
                ficha, datos.pacienteId(), datos.pacienteId().toString(), "historia-" + datos.dni());
    }

    private static String diagnosticoCodificado(Atencion atencion) {
        if (atencion.getDiagnosticos().isEmpty()) {
            return Formato.texto(atencion.getDiagnostico());
        }
        return atencion.getDiagnosticos().stream()
                .map(d -> d.getCodigo() + " " + d.getDescripcion())
                .collect(java.util.stream.Collectors.joining("; "));
    }

    private static String diagnosticoCodificado(AtencionResponse atencion) {
        if (atencion.diagnosticos() == null || atencion.diagnosticos().isEmpty()) {
            return Formato.texto(atencion.diagnostico());
        }
        return atencion.diagnosticos().stream()
                .map(d -> d.codigo() + " " + d.descripcion())
                .collect(java.util.stream.Collectors.joining("; "));
    }

    private static String referencia(OrdenLabResponse.ResultadoResponse resultado) {
        if (resultado.valorMin() != null && resultado.valorMax() != null) {
            return resultado.valorMin().stripTrailingZeros().toPlainString() + " - "
                    + resultado.valorMax().stripTrailingZeros().toPlainString();
        }
        if (resultado.valorMin() != null) {
            return ">= " + resultado.valorMin().stripTrailingZeros().toPlainString();
        }
        if (resultado.valorMax() != null) {
            return "<= " + resultado.valorMax().stripTrailingZeros().toPlainString();
        }
        return "";
    }

    private static boolean tiene(UsuarioPrincipal actor, RolNombre rol) {
        return actor.getRoles().contains(rol.name());
    }
}

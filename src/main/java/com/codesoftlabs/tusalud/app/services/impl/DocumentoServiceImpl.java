package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.documentos.CodigoVerificacion;
import com.codesoftlabs.tusalud.app.documentos.DocumentoGenerado;
import com.codesoftlabs.tusalud.app.documentos.DocumentoPreparado;
import com.codesoftlabs.tusalud.app.documentos.DocumentosClinicos;
import com.codesoftlabs.tusalud.app.documentos.DocumentosReportes;
import com.codesoftlabs.tusalud.app.documentos.FichaDocumento;
import com.codesoftlabs.tusalud.app.documentos.Formato;
import com.codesoftlabs.tusalud.app.documentos.FormatoDocumento;
import com.codesoftlabs.tusalud.app.documentos.LibroExcel;
import com.codesoftlabs.tusalud.app.documentos.ListadosExcel;
import com.codesoftlabs.tusalud.app.documentos.MotorPlantillas;
import com.codesoftlabs.tusalud.app.documentos.ParametrosDocumento;
import com.codesoftlabs.tusalud.app.documentos.ReporteArmado;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.entities.enums.AgrupacionReporte;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.DocumentoService;
import com.codesoftlabs.tusalud.app.storage.NombresArchivo;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentoServiceImpl implements DocumentoService {

    private final MotorPlantillas motor;
    private final DocumentosClinicos clinicos;
    private final DocumentosReportes reportes;
    private final ListadosExcel listados;
    private final AuditoriaClinicaService auditoriaClinica;

    @Override
    public DocumentoGenerado generarDirecto(UsuarioPrincipal actor, TipoDocumento tipo, UUID id) {
        if (!tipo.directo()) {
            throw new DatosInvalidosException("Este documento se genera con una exportación");
        }
        exigirPermiso(actor, tipo, FormatoDocumento.PDF);
        DocumentoPreparado preparado = clinicos.preparar(actor, tipo, id);
        FichaDocumento ficha = preparado.ficha();
        Instant ahora = Instant.now();
        String codigo = CodigoVerificacion.generar(tipo, preparado.referencia(), actor.getId(), ahora);
        sellar(ficha, actor, codigo);
        byte[] pdf = motor.renderizarPdf(ficha);
        if (tipo.clinico()
                && preparado.pacienteId() != null
                && !actor.getRoles().contains(RolNombre.ROLE_PACIENTE.name())) {
            auditoriaClinica.registrar(
                    actor,
                    preparado.pacienteId(),
                    AuditoriaClinicaService.DOCUMENTO_GENERADO,
                    tipo.codigo() + ":" + preparado.referencia());
        }
        return new DocumentoGenerado(
                pdf, nombreArchivo(preparado.nombreBase(), FormatoDocumento.PDF), FormatoDocumento.PDF);
    }

    @Override
    public DocumentoGenerado generar(
            UsuarioPrincipal actor, TipoDocumento tipo, FormatoDocumento formato, Map<String, String> parametros) {
        exigirPermiso(actor, tipo, formato);
        if (tipo.directo()) {
            throw new DatosInvalidosException("Este documento se genera por su identificador");
        }
        ParametrosDocumento p = new ParametrosDocumento(parametros);
        Instant ahora = Instant.now();
        if (tipo.esReporte()) {
            LocalDate[] rango = p.rango(LocalDate.now().minusDays(29), LocalDate.now());
            AgrupacionReporte agrupacion = p.enumeracion("agrupacion", AgrupacionReporte.class);
            ReporteArmado armado =
                    reportes.armar(tipo, rango[0], rango[1], agrupacion, formato == FormatoDocumento.PDF);
            String codigo = CodigoVerificacion.generar(tipo, armado.nombreBase(), actor.getId(), ahora);
            if (formato == FormatoDocumento.PDF) {
                sellar(armado.ficha(), actor, codigo);
                return new DocumentoGenerado(
                        motor.renderizarPdf(armado.ficha()), nombreArchivo(armado.nombreBase(), formato), formato);
            }
            byte[] libro = LibroExcel.construir(actor.getNombreCompleto(), codigo, armado.titulo(), armado.hojas());
            return new DocumentoGenerado(libro, nombreArchivo(armado.nombreBase(), formato), formato);
        }
        ListadosExcel.Listado listado = listados.armar(actor, tipo, p);
        String codigo = CodigoVerificacion.generar(tipo, listado.nombreBase(), actor.getId(), ahora);
        byte[] libro = LibroExcel.construir(actor.getNombreCompleto(), codigo, listado.titulo(), listado.hojas());
        return new DocumentoGenerado(libro, nombreArchivo(listado.nombreBase(), formato), formato);
    }

    @Override
    public void exigirPermiso(UsuarioPrincipal actor, TipoDocumento tipo, FormatoDocumento formato) {
        if (tipo.autonomo()) {
            throw new DatosInvalidosException("Este documento se emite desde su módulo");
        }
        boolean permitido =
                tipo.roles().stream().anyMatch(rol -> actor.getRoles().contains(rol.name()));
        if (!permitido) {
            throw new ForbiddenOperationException("No tiene permiso para generar este documento");
        }
        if (formato == null || !tipo.formatos().contains(formato)) {
            throw new DatosInvalidosException("Este documento no está disponible en el formato solicitado");
        }
    }

    private static void sellar(FichaDocumento ficha, UsuarioPrincipal actor, String codigo) {
        ficha.parametros().put("generadoPor", actor.getNombreCompleto());
        ficha.parametros().put("generadoEn", Formato.fechaHora(LocalDateTime.now()));
        ficha.parametros().put("codigo", codigo);
    }

    private static String nombreArchivo(String base, FormatoDocumento formato) {
        return NombresArchivo.sanitizar(base + "." + formato.extension());
    }
}

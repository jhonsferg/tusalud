package com.codesoftlabs.tusalud.app.services.documentosmedicos;

import com.codesoftlabs.tusalud.app.documentos.CodigoDocumentoClinico;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ActualizarDocumentoRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.DocumentoMedicoRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ReferenciaEstadoRequest;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.DocumentoMedicoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.DocumentoMedicoResumen;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.DocumentoMinimoResponse;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ReferenciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ResumenIngresoResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.AtencionDiagnostico;
import com.codesoftlabs.tusalud.app.entities.DocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.EstablecimientoSalud;
import com.codesoftlabs.tusalud.app.entities.EvolucionHospitalaria;
import com.codesoftlabs.tusalud.app.entities.IngresoHospitalario;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.ReferenciaClinica;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoDocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoReferencia;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.PrioridadReferencia;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.TipoDocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEvolucion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoRepository;
import com.codesoftlabs.tusalud.app.repositories.DocumentoMedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.EstablecimientoSaludRepository;
import com.codesoftlabs.tusalud.app.repositories.EvolucionHospitalariaRepository;
import com.codesoftlabs.tusalud.app.repositories.IngresoHospitalarioRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.ReferenciaClinicaRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import com.codesoftlabs.tusalud.app.storage.NombresArchivo;
import com.codesoftlabs.tusalud.app.storage.PropositoArchivo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentoMedicoService {

    public static final String REFERENCIA_ARCHIVO = "DOCUMENTO_MEDICO";
    private static final int MAXIMO_EVOLUCIONES = 8;
    private static final int MAXIMO_RESUMEN = 1900;

    private final DocumentoMedicoRepository documentos;
    private final ReferenciaClinicaRepository referencias;
    private final EstablecimientoSaludRepository establecimientos;
    private final ConsentimientoRepository consentimientos;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtencionRepository atencionRepository;
    private final IngresoHospitalarioRepository ingresoRepository;
    private final EvolucionHospitalariaRepository evolucionRepository;
    private final SecuenciasDocumentosMedicos secuencias;
    private final DocumentosMedicosPdf pdf;
    private final ArchivoService archivoService;
    private final RelacionAsistencialService relacion;
    private final AuditoriaClinicaService auditoria;
    private final NotificacionService notificaciones;

    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    @Value("${app.documentos-medicos.descanso-max-dias:30}")
    private int descansoMaxDias;

    @Value("${app.documentos-medicos.descanso-retroactivo-dias:3}")
    private int descansoRetroactivoDias;

    public record PdfDocumento(byte[] contenido, String nombre) {}

    @Transactional
    public DocumentoMedicoDetalle crearBorrador(UsuarioPrincipal actor, DocumentoMedicoRequest request) {
        Medico medico = medico(actor);
        Paciente paciente = paciente(request.pacienteId());
        relacion.exigirRelacion(actor, paciente.getId(), "No tiene relación asistencial con este paciente");
        if (request.tipo() == TipoDocumentoMedico.EPICRISIS) {
            throw new DatosInvalidosException("La epicrisis se genera desde el alta hospitalaria");
        }
        Atencion atencion = atencion(request.atencionId(), paciente);
        IngresoHospitalario ingreso = ingreso(request.ingresoId(), paciente);
        Map<String, Object> contenido = ContenidoDocumentoMedico.normalizar(request.tipo(), request.contenido());
        UUID referenciaId = null;
        if (request.tipo() == TipoDocumentoMedico.REFERENCIA) {
            EstablecimientoSalud destino = destino(contenido.get("destinoId"));
            contenido.put("destinoNombre", destino.getNombre());
        } else if (request.tipo() == TipoDocumentoMedico.CONTRARREFERENCIA) {
            ReferenciaClinica referencia = referenciaPendiente(request.referenciaId(), paciente);
            contenido.put("referenciaNumero", referencia.getDocumento().numeroCompleto());
            referenciaId = referencia.getId();
        }
        UUID origen = validarOrigen(request, medico, paciente);
        if (request.autorizaDiagnostico() && atencion == null && ingreso == null) {
            throw new DatosInvalidosException("Para incluir diagnósticos indique la atención de origen");
        }
        DocumentoMedico documento = documentos.save(DocumentoMedico.builder()
                .tipo(request.tipo())
                .paciente(paciente)
                .medico(medico)
                .atencion(atencion)
                .ingreso(ingreso)
                .documentoOrigenId(origen)
                .referenciaId(referenciaId)
                .autorizaDiagnostico(request.autorizaDiagnostico())
                .contenido(serializar(contenido))
                .build());
        return detalle(documentos.findConDetalle(documento.getId()).orElseThrow());
    }

    @Transactional
    public DocumentoMedicoDetalle actualizarBorrador(
            UsuarioPrincipal actor, UUID id, ActualizarDocumentoRequest request) {
        DocumentoMedico documento = propio(actor, id);
        exigirEstado(documento, EstadoDocumentoMedico.BORRADOR, "Solo se edita un borrador");
        Map<String, Object> contenido = ContenidoDocumentoMedico.normalizar(documento.getTipo(), request.contenido());
        if (documento.getTipo() == TipoDocumentoMedico.REFERENCIA) {
            contenido.put("destinoNombre", destino(contenido.get("destinoId")).getNombre());
        }
        Map<String, Object> actual = leer(documento.getContenido());
        if (documento.getTipo() == TipoDocumentoMedico.CONTRARREFERENCIA && actual.containsKey("referenciaNumero")) {
            contenido.put("referenciaNumero", actual.get("referenciaNumero"));
        }
        if (documento.getTipo() == TipoDocumentoMedico.EPICRISIS && actual.containsKey("diagnosticos")) {
            contenido.put("diagnosticos", actual.get("diagnosticos"));
        }
        documento.setAutorizaDiagnostico(request.autorizaDiagnostico());
        documento.setContenido(serializar(contenido));
        return detalle(documento);
    }

    @Transactional
    public DocumentoMedicoDetalle epicrisisBorrador(UsuarioPrincipal actor, UUID ingresoId) {
        Medico medico = medico(actor);
        IngresoHospitalario ingreso = ingresoRepository
                .findByIdConDetalle(ingresoId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso", "id", ingresoId));
        if (ingreso.getMedico() == null || !ingreso.getMedico().getId().equals(medico.getId())) {
            throw new ForbiddenOperationException("Solo el médico a cargo del ingreso emite su epicrisis");
        }
        if (ingreso.getEstado() != EstadoIngresoHosp.ALTA) {
            throw new ConflictException("La epicrisis se arma cuando el paciente ya recibió el alta");
        }
        for (DocumentoMedico existente : documentos.findDelIngreso(ingresoId)) {
            if (existente.getTipo() == TipoDocumentoMedico.EPICRISIS
                    && existente.getEstado() != EstadoDocumentoMedico.ANULADO) {
                return detalle(documentos.findConDetalle(existente.getId()).orElseThrow());
            }
        }
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("motivo", recortar(ingreso.getMotivo(), 500));
        contenido.put("diagnosticoIngreso", recortar(ingreso.getDiagnosticoIngreso(), 500));
        contenido.put("diagnosticoAlta", recortar(ingreso.getDiagnosticoAlta(), 500));
        contenido.put("resumenEvolucion", resumenEvolucion(ingreso));
        contenido.put("condicionAlta", "MEJORADO");
        contenido.put("indicaciones", recortar(ingreso.getIndicacionesAlta(), 1000));
        List<Map<String, Object>> diagnosticos = new ArrayList<>();
        if (ingreso.getCie10IngresoCodigo() != null) {
            diagnosticos.add(diagnostico(ingreso.getCie10IngresoCodigo(), ingreso.getCie10IngresoDescripcion(), false));
        }
        if (ingreso.getCie10AltaCodigo() != null) {
            diagnosticos.add(diagnostico(ingreso.getCie10AltaCodigo(), ingreso.getCie10AltaDescripcion(), true));
        }
        contenido.put("diagnosticos", diagnosticos);
        DocumentoMedico documento = documentos.save(DocumentoMedico.builder()
                .tipo(TipoDocumentoMedico.EPICRISIS)
                .paciente(ingreso.getPaciente())
                .medico(medico)
                .ingreso(ingreso)
                .autorizaDiagnostico(true)
                .contenido(serializar(contenido))
                .build());
        return detalle(documentos.findConDetalle(documento.getId()).orElseThrow());
    }

    @Transactional
    public DocumentoMedicoDetalle emitir(UsuarioPrincipal actor, UUID id) {
        DocumentoMedico documento = propio(actor, id);
        exigirEstado(documento, EstadoDocumentoMedico.BORRADOR, "El documento ya fue emitido");
        Map<String, Object> contenido = leer(documento.getContenido());
        validarAlEmitir(documento, contenido);
        incluirDiagnosticos(documento, contenido);
        int anio = LocalDate.now().getYear();
        documento.setSerie(documento.getTipo().serie());
        documento.setAnio(anio);
        documento.setNumero(secuencias.siguiente(documento.getTipo(), anio));
        documento.setCodigoVerificacion(codigoUnico());
        documento.setContenido(serializar(contenido));
        documento.setContenidoHash(
                CodigoDocumentoClinico.sha256(documento.getContenido() + "|" + documento.numeroCompleto()));
        documento.setEmitidoEn(LocalDateTime.now());
        documento.setEstado(EstadoDocumentoMedico.EMITIDO);
        byte[] archivo = pdf.renderizarDocumento(documento, contenido, null, actor.getNombreCompleto());
        Usuario emisor = usuario(actor.getId());
        documento.setPdfArchivo(archivoService.almacenar(
                emisor,
                documento.getPaciente().getUsuario(),
                PropositoArchivo.DOCUMENTO_CLINICO,
                NombresArchivo.sanitizar(documento.numeroCompleto() + ".pdf"),
                archivo,
                REFERENCIA_ARCHIVO,
                documento.getId()));
        documento.setPdfHash(CodigoDocumentoClinico.sha256(archivo));
        ReferenciaClinica referencia = null;
        if (documento.getTipo() == TipoDocumentoMedico.REFERENCIA) {
            referencia = referencias.save(ReferenciaClinica.builder()
                    .documento(documento)
                    .destino(destino(contenido.get("destinoId")))
                    .especialidad(String.valueOf(contenido.get("especialidad")))
                    .prioridad(PrioridadReferencia.valueOf(String.valueOf(contenido.get("prioridad"))))
                    .build());
        } else if (documento.getTipo() == TipoDocumentoMedico.CONTRARREFERENCIA
                && documento.getReferenciaId() != null) {
            ReferenciaClinica original =
                    referencias.findById(documento.getReferenciaId()).orElseThrow();
            original.setEstado(EstadoReferencia.ATENDIDA);
            original.setActualizadoEn(LocalDateTime.now());
        }
        notificaciones.crear(
                documento.getPaciente().getUsuario(),
                TipoNotificacion.DOCUMENTO_EMITIDO,
                "Nuevo documento médico",
                "Se emitió su " + documento.getTipo().etiqueta().toLowerCase() + " " + documento.numeroCompleto() + ".",
                "/mis-documentos");
        auditoria.registrar(
                actor,
                documento.getPaciente().getId(),
                "DOCUMENTO_MEDICO_EMITIDO",
                documento.getTipo().name() + ":" + documento.numeroCompleto());
        return detalle(documento, referencia);
    }

    @Transactional
    public DocumentoMedicoDetalle anular(UsuarioPrincipal actor, UUID id, String motivo) {
        DocumentoMedico documento = propio(actor, id);
        exigirEstado(documento, EstadoDocumentoMedico.EMITIDO, "Solo se anula un documento emitido");
        documento.setEstado(EstadoDocumentoMedico.ANULADO);
        documento.setAnuladoEn(LocalDateTime.now());
        documento.setAnuladoPor(usuario(actor.getId()));
        documento.setMotivoAnulacion(motivo.strip());
        notificaciones.crear(
                documento.getPaciente().getUsuario(),
                TipoNotificacion.DOCUMENTO_EMITIDO,
                "Documento médico anulado",
                "El documento " + documento.numeroCompleto() + " fue anulado por el médico emisor.",
                "/mis-documentos");
        auditoria.registrar(
                actor,
                documento.getPaciente().getId(),
                "DOCUMENTO_MEDICO_ANULADO",
                documento.getTipo().name() + ":" + documento.numeroCompleto());
        return detalle(documento);
    }

    @Transactional
    public DocumentoMedicoDetalle obtener(UsuarioPrincipal actor, UUID id) {
        DocumentoMedico documento =
                documentos.findConDetalle(id).orElseThrow(() -> new ResourceNotFoundException("Documento", "id", id));
        autorizarLectura(actor, documento);
        return detalle(documento);
    }

    @Transactional
    public PdfDocumento pdf(UsuarioPrincipal actor, UUID id) {
        DocumentoMedico documento =
                documentos.findConDetalle(id).orElseThrow(() -> new ResourceNotFoundException("Documento", "id", id));
        autorizarLectura(actor, documento);
        String nombre = NombresArchivo.sanitizar((documento.numeroCompleto() == null
                        ? "borrador-" + documento.getTipo().serie()
                        : documento.numeroCompleto())
                + ".pdf");
        if (documento.getEstado() == EstadoDocumentoMedico.EMITIDO && documento.getPdfArchivo() != null) {
            var descarga = archivoService.abrirSinAutorizar(documento.getPdfArchivo(), null);
            try (var objeto = descarga.objeto()) {
                return new PdfDocumento(objeto.flujo().readAllBytes(), nombre);
            } catch (IOException ex) {
                throw new IllegalStateException("No se pudo leer el documento", ex);
            }
        }
        String banner = documento.getEstado() == EstadoDocumentoMedico.ANULADO
                ? "DOCUMENTO ANULADO · " + documento.getMotivoAnulacion()
                : "BORRADOR · SIN VALIDEZ";
        byte[] bytes =
                pdf.renderizarDocumento(documento, leer(documento.getContenido()), banner, actor.getNombreCompleto());
        return new PdfDocumento(bytes, nombre);
    }

    public List<DocumentoMedicoResumen> listarMios(UsuarioPrincipal actor) {
        Paciente paciente = pacienteRepository
                .findByUsuarioId(actor.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Solo un paciente consulta sus documentos"));
        return resumir(documentos.findDelPaciente(paciente.getId()).stream()
                .filter(d -> d.getEstado() != EstadoDocumentoMedico.BORRADOR)
                .toList());
    }

    @Transactional
    public List<DocumentoMedicoResumen> listarDePaciente(UsuarioPrincipal actor, UUID pacienteId) {
        Medico medico = medico(actor);
        relacion.exigirRelacion(actor, pacienteId, "No tiene relación asistencial con este paciente");
        return resumir(documentos.findDelPaciente(pacienteId).stream()
                .filter(d -> d.getEstado() != EstadoDocumentoMedico.BORRADOR
                        || d.getMedico().getId().equals(medico.getId()))
                .toList());
    }

    public List<DocumentoMedicoResumen> listarEmitidosPorMi(UsuarioPrincipal actor) {
        return resumir(documentos.findDelMedico(medico(actor).getId()));
    }

    public ResumenIngresoResponse resumenIngreso(UsuarioPrincipal actor, UUID ingresoId) {
        IngresoHospitalario ingreso = ingresoRepository
                .findByIdConDetalle(ingresoId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso", "id", ingresoId));
        boolean enfermeria = actor.getRoles().contains(RolNombre.ROLE_ENFERMERIA.name());
        if (enfermeria) {
            if (ingreso.getEstado() != EstadoIngresoHosp.ACTIVO) {
                throw new ForbiddenOperationException("Solo se consultan los pacientes hospitalizados");
            }
        } else {
            relacion.exigirRelacion(
                    actor, ingreso.getPaciente().getId(), "No tiene relación asistencial con este paciente");
        }
        List<DocumentoMinimoResponse> docs = documentos.findDelIngreso(ingresoId).stream()
                .filter(d ->
                        d.getTipo() == TipoDocumentoMedico.EPICRISIS && d.getEstado() != EstadoDocumentoMedico.BORRADOR)
                .map(DocumentosMedicosMapper::minimo)
                .toList();
        var consents = consentimientos.findDelIngreso(ingresoId).stream()
                .map(DocumentosMedicosMapper::consentimiento)
                .toList();
        return new ResumenIngresoResponse(ingresoId, docs, consents);
    }

    @Transactional
    public ReferenciaResponse cambiarEstadoReferencia(
            UsuarioPrincipal actor, UUID documentoId, ReferenciaEstadoRequest request) {
        DocumentoMedico documento = propio(actor, documentoId);
        if (documento.getTipo() != TipoDocumentoMedico.REFERENCIA
                || documento.getEstado() != EstadoDocumentoMedico.EMITIDO) {
            throw new ConflictException("Solo una referencia emitida cambia de estado");
        }
        ReferenciaClinica referencia = referencias
                .findPorDocumento(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Referencia", "documento", documentoId));
        EstadoReferencia actual = referencia.getEstado();
        EstadoReferencia nuevo = request.estado();
        boolean valido = (actual == EstadoReferencia.ENVIADA
                        && (nuevo == EstadoReferencia.RECIBIDA || nuevo == EstadoReferencia.RECHAZADA))
                || (actual == EstadoReferencia.RECIBIDA
                        && (nuevo == EstadoReferencia.ATENDIDA || nuevo == EstadoReferencia.RECHAZADA));
        if (!valido) {
            throw new ConflictException("La referencia no puede pasar de " + actual + " a " + nuevo);
        }
        if (nuevo == EstadoReferencia.RECHAZADA
                && (request.motivoRechazo() == null || request.motivoRechazo().isBlank())) {
            throw new DatosInvalidosException("Indique el motivo del rechazo");
        }
        referencia.setEstado(nuevo);
        referencia.setMotivoRechazo(
                nuevo == EstadoReferencia.RECHAZADA ? request.motivoRechazo().strip() : null);
        referencia.setActualizadoEn(LocalDateTime.now());
        return DocumentosMedicosMapper.referencia(referencia);
    }

    private void autorizarLectura(UsuarioPrincipal actor, DocumentoMedico documento) {
        if (actor.getRoles().contains(RolNombre.ROLE_PACIENTE.name())) {
            boolean propio = documento.getPaciente().getUsuario().getId().equals(actor.getId());
            if (!propio || documento.getEstado() == EstadoDocumentoMedico.BORRADOR) {
                throw new ForbiddenOperationException("No tiene acceso a este documento");
            }
            return;
        }
        if (!actor.getRoles().contains(RolNombre.ROLE_MEDICO.name())) {
            throw new ForbiddenOperationException("No tiene acceso a este documento");
        }
        Medico medico = medico(actor);
        boolean autor = documento.getMedico().getId().equals(medico.getId());
        if (documento.getEstado() == EstadoDocumentoMedico.BORRADOR && !autor) {
            throw new ForbiddenOperationException("No tiene acceso a este documento");
        }
        relacion.exigirRelacion(
                actor, documento.getPaciente().getId(), "No tiene relación asistencial con este paciente");
        if (!autor) {
            auditoria.registrar(
                    actor,
                    documento.getPaciente().getId(),
                    "DOCUMENTO_MEDICO_CONSULTADO",
                    documento.getTipo().name() + ":" + documento.numeroCompleto());
        }
    }

    private void validarAlEmitir(DocumentoMedico documento, Map<String, Object> contenido) {
        switch (documento.getTipo()) {
            case DESCANSO -> validarDescanso(documento, contenido);
            case INFORME -> {
                if (!documento.isAutorizaDiagnostico()) {
                    throw new DatosInvalidosException(
                            "Registre la autorización del paciente para emitir el informe médico");
                }
            }
            case EPICRISIS -> {
                if (documento.getIngreso() == null || documento.getIngreso().getEstado() != EstadoIngresoHosp.ALTA) {
                    throw new ConflictException("La epicrisis requiere un ingreso con alta");
                }
            }
            case CONTRARREFERENCIA -> {
                ReferenciaClinica referencia =
                        referencias.findById(documento.getReferenciaId()).orElseThrow();
                if (referencia.getEstado() == EstadoReferencia.RECHAZADA
                        || referencia.getEstado() == EstadoReferencia.ATENDIDA) {
                    throw new ConflictException("La referencia ya fue cerrada");
                }
            }
            default -> {}
        }
    }

    private void validarDescanso(DocumentoMedico documento, Map<String, Object> contenido) {
        LocalDate desde = LocalDate.parse(String.valueOf(contenido.get("desde")));
        LocalDate hasta = LocalDate.parse(String.valueOf(contenido.get("hasta")));
        if (hasta.isBefore(desde)) {
            throw new DatosInvalidosException("La fecha de fin no puede ser anterior a la de inicio");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (dias > descansoMaxDias) {
            throw new DatosInvalidosException("Un descanso no puede superar los " + descansoMaxDias + " días");
        }
        if (desde.isBefore(LocalDate.now().minusDays(descansoRetroactivoDias))) {
            throw new DatosInvalidosException(
                    "El descanso no puede iniciar más de " + descansoRetroactivoDias + " días atrás");
        }
        for (DocumentoMedico otro :
                documentos.findDelPaciente(documento.getPaciente().getId())) {
            if (otro.getTipo() != TipoDocumentoMedico.DESCANSO
                    || otro.getEstado() != EstadoDocumentoMedico.EMITIDO
                    || otro.getId().equals(documento.getId())) {
                continue;
            }
            Map<String, Object> c = leer(otro.getContenido());
            LocalDate d2 = LocalDate.parse(String.valueOf(c.get("desde")));
            LocalDate h2 = LocalDate.parse(String.valueOf(c.get("hasta")));
            if (!desde.isAfter(h2) && !hasta.isBefore(d2)) {
                throw new ConflictException("El paciente ya tiene un descanso médico vigente en esas fechas");
            }
        }
    }

    private void incluirDiagnosticos(DocumentoMedico documento, Map<String, Object> contenido) {
        TipoDocumentoMedico tipo = documento.getTipo();
        if (tipo == TipoDocumentoMedico.EPICRISIS) {
            return;
        }
        contenido.remove("diagnosticos");
        if (!documento.isAutorizaDiagnostico() || documento.getAtencion() == null) {
            return;
        }
        List<Map<String, Object>> lista = new ArrayList<>();
        for (AtencionDiagnostico d : documento.getAtencion().getDiagnosticos()) {
            lista.add(diagnostico(d.getCodigo(), d.getDescripcion(), d.isPrincipal()));
        }
        if (lista.isEmpty()) {
            throw new DatosInvalidosException("La atención de origen no tiene diagnósticos codificados para incluir");
        }
        contenido.put("diagnosticos", lista);
    }

    private static Map<String, Object> diagnostico(String codigo, String descripcion, boolean principal) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("codigo", codigo);
        d.put("descripcion", descripcion == null ? "" : descripcion);
        d.put("principal", principal);
        return d;
    }

    private String resumenEvolucion(IngresoHospitalario ingreso) {
        List<EvolucionHospitalaria> medicas = evolucionRepository.findDelIngreso(ingreso.getId()).stream()
                .filter(e -> e.getTipo() == TipoEvolucion.MEDICA)
                .toList();
        int desde = Math.max(0, medicas.size() - MAXIMO_EVOLUCIONES);
        StringBuilder texto = new StringBuilder();
        for (EvolucionHospitalaria e : medicas.subList(desde, medicas.size())) {
            texto.append(e.getFecha().toLocalDate())
                    .append(": ")
                    .append(e.getNota())
                    .append("\n");
        }
        String resumen = texto.toString().strip();
        return resumen.isEmpty() ? "Sin evoluciones médicas registradas" : recortar(resumen, MAXIMO_RESUMEN);
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    private UUID validarOrigen(DocumentoMedicoRequest request, Medico medico, Paciente paciente) {
        if (request.documentoOrigenId() == null) {
            return null;
        }
        DocumentoMedico origen = documentos
                .findConDetalle(request.documentoOrigenId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento", "id", request.documentoOrigenId()));
        if (origen.getEstado() != EstadoDocumentoMedico.ANULADO
                || origen.getTipo() != request.tipo()
                || !origen.getPaciente().getId().equals(paciente.getId())
                || !origen.getMedico().getId().equals(medico.getId())) {
            throw new DatosInvalidosException("Solo se reemplaza un documento anulado propio del mismo tipo");
        }
        if (documentos.existsByDocumentoOrigenId(origen.getId())) {
            throw new ConflictException("El documento anulado ya fue reemplazado");
        }
        return origen.getId();
    }

    private ReferenciaClinica referenciaPendiente(UUID referenciaId, Paciente paciente) {
        if (referenciaId == null) {
            throw new DatosInvalidosException("Indique la referencia que se contrarrefiere");
        }
        ReferenciaClinica referencia = referencias
                .findById(referenciaId)
                .orElseThrow(() -> new ResourceNotFoundException("Referencia", "id", referenciaId));
        if (!referencia.getDocumento().getPaciente().getId().equals(paciente.getId())) {
            throw new DatosInvalidosException("La referencia no corresponde a este paciente");
        }
        if (referencia.getEstado() == EstadoReferencia.RECHAZADA
                || referencia.getEstado() == EstadoReferencia.ATENDIDA) {
            throw new ConflictException("La referencia ya fue cerrada");
        }
        return referencia;
    }

    private EstablecimientoSalud destino(Object destinoId) {
        UUID id = UUID.fromString(String.valueOf(destinoId));
        EstablecimientoSalud destino = establecimientos
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Establecimiento", "id", id));
        if (!destino.isActivo()) {
            throw new DatosInvalidosException("El establecimiento de destino no está activo");
        }
        return destino;
    }

    private String codigoUnico() {
        for (int intento = 0; intento < 5; intento++) {
            String codigo = CodigoDocumentoClinico.nuevo();
            if (documentos.findByCodigo(codigo).isEmpty()
                    && consentimientos.findByCodigo(codigo).isEmpty()) {
                return codigo;
            }
        }
        throw new IllegalStateException("No se pudo generar un código de verificación único");
    }

    private Atencion atencion(UUID id, Paciente paciente) {
        if (id == null) {
            return null;
        }
        Atencion atencion =
                atencionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Atención", "id", id));
        if (!atencion.getPaciente().getId().equals(paciente.getId())) {
            throw new DatosInvalidosException("La atención no corresponde a este paciente");
        }
        return atencion;
    }

    private IngresoHospitalario ingreso(UUID id, Paciente paciente) {
        if (id == null) {
            return null;
        }
        IngresoHospitalario ingreso = ingresoRepository
                .findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso", "id", id));
        if (!ingreso.getPaciente().getId().equals(paciente.getId())) {
            throw new DatosInvalidosException("El ingreso no corresponde a este paciente");
        }
        return ingreso;
    }

    private DocumentoMedico propio(UsuarioPrincipal actor, UUID id) {
        Medico medico = medico(actor);
        DocumentoMedico documento =
                documentos.findConDetalle(id).orElseThrow(() -> new ResourceNotFoundException("Documento", "id", id));
        if (!documento.getMedico().getId().equals(medico.getId())) {
            throw new ForbiddenOperationException("Solo el médico emisor gestiona este documento");
        }
        return documento;
    }

    private static void exigirEstado(DocumentoMedico documento, EstadoDocumentoMedico estado, String mensaje) {
        if (documento.getEstado() != estado) {
            throw new ConflictException(mensaje);
        }
    }

    private Medico medico(UsuarioPrincipal actor) {
        return medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ForbiddenOperationException("Solo el personal médico gestiona documentos"));
    }

    private Paciente paciente(UUID id) {
        return pacienteRepository
                .findByIdWithUsuario(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", id));
    }

    private Usuario usuario(UUID id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    private List<DocumentoMedicoResumen> resumir(List<DocumentoMedico> lista) {
        List<UUID> ids = lista.stream()
                .filter(d -> d.getTipo() == TipoDocumentoMedico.REFERENCIA)
                .map(DocumentoMedico::getId)
                .toList();
        Map<UUID, ReferenciaClinica> porDocumento = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            referencias
                    .findPorDocumentos(ids)
                    .forEach(r -> porDocumento.put(r.getDocumento().getId(), r));
        }
        return lista.stream()
                .map(d -> DocumentosMedicosMapper.resumen(d, porDocumento.get(d.getId())))
                .toList();
    }

    private DocumentoMedicoDetalle detalle(DocumentoMedico documento) {
        ReferenciaClinica referencia = documento.getTipo() == TipoDocumentoMedico.REFERENCIA
                ? referencias.findPorDocumento(documento.getId()).orElse(null)
                : null;
        return detalle(documento, referencia);
    }

    private DocumentoMedicoDetalle detalle(DocumentoMedico documento, ReferenciaClinica referencia) {
        List<UUID> contras = referencia == null
                ? List.of()
                : documentos.findContrarreferencias(referencia.getId()).stream()
                        .map(DocumentoMedico::getId)
                        .toList();
        return new DocumentoMedicoDetalle(
                DocumentosMedicosMapper.resumen(documento, referencia),
                leer(documento.getContenido()),
                documento.isAutorizaDiagnostico(),
                documento.getCodigoVerificacion(),
                documento.getDocumentoOrigenId(),
                documento.getReferenciaId(),
                documentos.existsByDocumentoOrigenId(documento.getId()),
                contras);
    }

    private String serializar(Map<String, Object> contenido) {
        try {
            return json.writeValueAsString(contenido);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el documento", ex);
        }
    }

    private Map<String, Object> leer(String contenido) {
        try {
            return new LinkedHashMap<>(json.readValue(contenido, new TypeReference<Map<String, Object>>() {}));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("El contenido del documento está dañado", ex);
        }
    }
}

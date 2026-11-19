package com.codesoftlabs.tusalud.app.services.documentosmedicos;

import com.codesoftlabs.tusalud.app.documentos.CodigoDocumentoClinico;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ConsentimientoPlantillaRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ConsentimientoSolicitudRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.FirmaRequest;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ConsentimientoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ConsentimientoPlantillaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ConsentimientoResumen;
import com.codesoftlabs.tusalud.app.entities.Archivo;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.Consentimiento;
import com.codesoftlabs.tusalud.app.entities.ConsentimientoPlantilla;
import com.codesoftlabs.tusalud.app.entities.IngresoHospitalario;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoConsentimiento;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.FirmanteTipo;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.ViaFirma;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoPlantillaRepository;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoRepository;
import com.codesoftlabs.tusalud.app.repositories.DocumentoMedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.IngresoHospitalarioRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import com.codesoftlabs.tusalud.app.storage.NombresArchivo;
import com.codesoftlabs.tusalud.app.storage.PropositoArchivo;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentimientoService {

    private static final String REFERENCIA_ARCHIVO = "CONSENTIMIENTO";
    private static final int PIXELES_MINIMOS_FIRMA = 80;

    private final ConsentimientoRepository consentimientos;
    private final ConsentimientoPlantillaRepository plantillas;
    private final DocumentoMedicoRepository documentos;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtencionRepository atencionRepository;
    private final IngresoHospitalarioRepository ingresoRepository;
    private final DocumentosMedicosPdf pdf;
    private final ArchivoService archivoService;
    private final RelacionAsistencialService relacion;
    private final AuditoriaClinicaService auditoria;
    private final NotificacionService notificaciones;

    public List<ConsentimientoPlantillaResponse> plantillas(UsuarioPrincipal actor) {
        boolean admin = actor.getRoles().contains(RolNombre.ROLE_ADMIN.name());
        return (admin ? plantillas.findAllByOrderByNombreAsc() : plantillas.findByActivaTrueOrderByNombreAsc())
                .stream().map(DocumentosMedicosMapper::plantilla).toList();
    }

    @Transactional
    public ConsentimientoPlantillaResponse crearPlantilla(ConsentimientoPlantillaRequest request) {
        String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
        if (plantillas.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe una plantilla con el código " + codigo);
        }
        ConsentimientoPlantilla plantilla = plantillas.save(ConsentimientoPlantilla.builder()
                .codigo(codigo)
                .nombre(request.nombre().strip())
                .texto(request.texto().strip())
                .riesgos(request.riesgos().strip())
                .beneficios(request.beneficios().strip())
                .alternativas(request.alternativas().strip())
                .requiereTestigo(request.requiereTestigo())
                .vigenciaDias(request.vigenciaDias())
                .activa(request.activa())
                .build());
        return DocumentosMedicosMapper.plantilla(plantilla);
    }

    @Transactional
    public ConsentimientoPlantillaResponse actualizarPlantilla(UUID id, ConsentimientoPlantillaRequest request) {
        ConsentimientoPlantilla plantilla = plantillas
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla de consentimiento", "id", id));
        boolean cambioTexto = !plantilla.getTexto().equals(request.texto().strip())
                || !plantilla.getRiesgos().equals(request.riesgos().strip())
                || !plantilla.getBeneficios().equals(request.beneficios().strip())
                || !plantilla.getAlternativas().equals(request.alternativas().strip());
        plantilla.setNombre(request.nombre().strip());
        plantilla.setTexto(request.texto().strip());
        plantilla.setRiesgos(request.riesgos().strip());
        plantilla.setBeneficios(request.beneficios().strip());
        plantilla.setAlternativas(request.alternativas().strip());
        plantilla.setRequiereTestigo(request.requiereTestigo());
        plantilla.setVigenciaDias(request.vigenciaDias());
        plantilla.setActiva(request.activa());
        if (cambioTexto) {
            plantilla.setVersion(plantilla.getVersion() + 1);
        }
        return DocumentosMedicosMapper.plantilla(plantilla);
    }

    @Transactional
    public ConsentimientoResumen solicitar(UsuarioPrincipal actor, ConsentimientoSolicitudRequest request) {
        Medico medico = medico(actor);
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.pacienteId()));
        relacion.exigirRelacion(actor, paciente.getId(), "No tiene relación asistencial con este paciente");
        ConsentimientoPlantilla plantilla = plantillas
                .findById(request.plantillaId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Plantilla de consentimiento", "id", request.plantillaId()));
        if (!plantilla.isActiva()) {
            throw new ConflictException("La plantilla de consentimiento no está activa");
        }
        Atencion atencion = null;
        if (request.atencionId() != null) {
            atencion = atencionRepository
                    .findById(request.atencionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Atención", "id", request.atencionId()));
            if (!atencion.getPaciente().getId().equals(paciente.getId())) {
                throw new DatosInvalidosException("La atención no corresponde a este paciente");
            }
        }
        IngresoHospitalario ingreso = null;
        if (request.ingresoId() != null) {
            ingreso = ingresoRepository
                    .findByIdConDetalle(request.ingresoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ingreso", "id", request.ingresoId()));
            if (!ingreso.getPaciente().getId().equals(paciente.getId())) {
                throw new DatosInvalidosException("El ingreso no corresponde a este paciente");
            }
        }
        Consentimiento consentimiento = consentimientos.save(Consentimiento.builder()
                .plantilla(plantilla)
                .plantillaVersion(plantilla.getVersion())
                .paciente(paciente)
                .medico(medico)
                .atencion(atencion)
                .ingreso(ingreso)
                .ordenImagenId(request.ordenImagenId())
                .procedimiento(request.procedimiento().strip())
                .textoAceptado(componerTexto(plantilla, request.procedimiento().strip()))
                .requiereTestigo(plantilla.isRequiereTestigo())
                .build());
        notificaciones.crear(
                paciente.getUsuario(),
                TipoNotificacion.CONSENTIMIENTO_PENDIENTE,
                "Consentimiento pendiente de firma",
                "Su médico le solicitó el consentimiento informado para " + consentimiento.getProcedimiento() + ".",
                "/mis-consentimientos");
        auditoria.registrar(actor, paciente.getId(), "CONSENTIMIENTO_SOLICITADO", plantilla.getCodigo());
        return DocumentosMedicosMapper.consentimiento(
                consentimientos.findConDetalle(consentimiento.getId()).orElseThrow());
    }

    @Transactional
    public ConsentimientoDetalle firmar(UsuarioPrincipal actor, UUID id, FirmaRequest request) {
        Consentimiento c = consentimientos
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consentimiento", "id", id));
        autorizarFirma(actor, c, request.via());
        if (c.getEstado() != EstadoConsentimiento.PENDIENTE_FIRMA) {
            throw new ConflictException("El consentimiento ya no está pendiente de firma");
        }
        validarFirmante(c, request);
        byte[] firma = decodificarFirma(request.firmaPng());
        Usuario subidoPor = usuario(actor.getId());
        Archivo archivoFirma = archivoService.almacenar(
                subidoPor,
                c.getPaciente().getUsuario(),
                PropositoArchivo.FIRMA_CONSENTIMIENTO,
                "firma-" + c.getId() + ".png",
                firma,
                REFERENCIA_ARCHIVO,
                c.getId());
        LocalDateTime ahora = LocalDateTime.now();
        c.setEstado(EstadoConsentimiento.FIRMADO);
        c.setFirmanteTipo(request.firmanteTipo());
        c.setFirmanteNombre(request.nombre().strip());
        c.setFirmanteDni(request.dni().strip().toUpperCase(Locale.ROOT));
        c.setFirmanteParentesco(
                request.firmanteTipo() == FirmanteTipo.REPRESENTANTE
                        ? request.parentesco().strip()
                        : null);
        if (c.isRequiereTestigo()) {
            c.setTestigoNombre(request.testigoNombre().strip());
            c.setTestigoDni(request.testigoDni().strip().toUpperCase(Locale.ROOT));
        }
        c.setFirmadoVia(request.via());
        c.setFirmaArchivo(archivoFirma);
        c.setFirmaHash(CodigoDocumentoClinico.sha256(firma));
        c.setFirmadoEn(ahora);
        c.setVenceEn(ahora.plusDays(c.getPlantilla().getVigenciaDias()));
        c.setCodigoVerificacion(codigoUnico());
        byte[] documento = pdf.renderizarConsentimiento(c, firma, null, actor.getNombreCompleto());
        c.setPdfArchivo(archivoService.almacenar(
                subidoPor,
                c.getPaciente().getUsuario(),
                PropositoArchivo.DOCUMENTO_CLINICO,
                NombresArchivo.sanitizar("consentimiento-" + c.getId() + ".pdf"),
                documento,
                REFERENCIA_ARCHIVO,
                c.getId()));
        c.setPdfHash(CodigoDocumentoClinico.sha256(documento));
        notificaciones.crear(
                c.getMedico().getUsuario(),
                TipoNotificacion.CONSENTIMIENTO_FIRMADO,
                "Consentimiento firmado",
                c.getPaciente().getUsuario().getNombreCompleto() + " firmó el consentimiento para "
                        + c.getProcedimiento() + ".",
                "/consentimientos");
        auditoria.registrar(
                actor,
                c.getPaciente().getId(),
                "CONSENTIMIENTO_FIRMADO",
                c.getPlantilla().getCodigo());
        return detalle(c);
    }

    @Transactional
    public ConsentimientoDetalle revocar(UsuarioPrincipal actor, UUID id, String motivo) {
        Consentimiento c = consentimientos
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consentimiento", "id", id));
        boolean titular = c.getPaciente().getUsuario().getId().equals(actor.getId());
        boolean solicitante = c.getMedico().getUsuario().getId().equals(actor.getId());
        if (!titular && !solicitante) {
            throw new ForbiddenOperationException("No puede revocar este consentimiento");
        }
        if (c.getEstado() != EstadoConsentimiento.FIRMADO) {
            throw new ConflictException("Solo se revoca un consentimiento firmado");
        }
        c.setEstado(EstadoConsentimiento.REVOCADO);
        c.setRevocadoEn(LocalDateTime.now());
        c.setRevocadoPor(usuario(actor.getId()));
        c.setMotivoRevocacion(motivo.strip());
        auditoria.registrar(
                actor,
                c.getPaciente().getId(),
                "CONSENTIMIENTO_REVOCADO",
                c.getPlantilla().getCodigo());
        return detalle(c);
    }

    public List<ConsentimientoResumen> listarMios(UsuarioPrincipal actor) {
        Paciente paciente = pacienteRepository
                .findByUsuarioId(actor.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Solo un paciente consulta sus consentimientos"));
        return consentimientos.findDelPaciente(paciente.getId()).stream()
                .map(DocumentosMedicosMapper::consentimiento)
                .toList();
    }

    public List<ConsentimientoResumen> listarDePaciente(UsuarioPrincipal actor, UUID pacienteId) {
        medico(actor);
        relacion.exigirRelacion(actor, pacienteId, "No tiene relación asistencial con este paciente");
        return consentimientos.findDelPaciente(pacienteId).stream()
                .map(DocumentosMedicosMapper::consentimiento)
                .toList();
    }

    @Transactional
    public ConsentimientoDetalle obtener(UsuarioPrincipal actor, UUID id) {
        Consentimiento c = consentimientos
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consentimiento", "id", id));
        autorizarLectura(actor, c);
        return detalle(c);
    }

    @Transactional
    public DocumentoMedicoService.PdfDocumento pdf(UsuarioPrincipal actor, UUID id) {
        Consentimiento c = consentimientos
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consentimiento", "id", id));
        autorizarLectura(actor, c);
        String nombre = NombresArchivo.sanitizar("consentimiento-" + c.getId() + ".pdf");
        if (c.getEstado() == EstadoConsentimiento.FIRMADO && c.getPdfArchivo() != null) {
            return new DocumentoMedicoService.PdfDocumento(leerArchivo(c.getPdfArchivo()), nombre);
        }
        String banner = switch (c.estadoEfectivo()) {
            case PENDIENTE_FIRMA -> "PENDIENTE DE FIRMA · SIN VALIDEZ";
            case REVOCADO -> "CONSENTIMIENTO REVOCADO · " + c.getMotivoRevocacion();
            case VENCIDO -> "CONSENTIMIENTO VENCIDO";
            default -> null;
        };
        byte[] firma = c.getFirmaArchivo() == null ? null : leerArchivo(c.getFirmaArchivo());
        return new DocumentoMedicoService.PdfDocumento(
                pdf.renderizarConsentimiento(c, firma, banner, actor.getNombreCompleto()), nombre);
    }

    private void autorizarFirma(UsuarioPrincipal actor, Consentimiento c, ViaFirma via) {
        boolean titular = c.getPaciente().getUsuario().getId().equals(actor.getId());
        boolean solicitante = c.getMedico().getUsuario().getId().equals(actor.getId());
        if (titular && via == ViaFirma.APP_PACIENTE) {
            return;
        }
        if (solicitante && via == ViaFirma.CONSULTORIO) {
            return;
        }
        throw new ForbiddenOperationException("No puede firmar este consentimiento");
    }

    private void autorizarLectura(UsuarioPrincipal actor, Consentimiento c) {
        if (c.getPaciente().getUsuario().getId().equals(actor.getId())) {
            return;
        }
        if (!actor.getRoles().contains(RolNombre.ROLE_MEDICO.name())) {
            throw new ForbiddenOperationException("No tiene acceso a este consentimiento");
        }
        relacion.exigirRelacion(actor, c.getPaciente().getId(), "No tiene relación asistencial con este paciente");
        if (!c.getMedico().getUsuario().getId().equals(actor.getId())) {
            auditoria.registrar(
                    actor,
                    c.getPaciente().getId(),
                    "CONSENTIMIENTO_CONSULTADO",
                    c.getPlantilla().getCodigo());
        }
    }

    private void validarFirmante(Consentimiento c, FirmaRequest request) {
        if (request.firmanteTipo() == FirmanteTipo.PACIENTE
                && !request.dni().trim().equalsIgnoreCase(c.getPaciente().getDni())) {
            throw new DatosInvalidosException("El DNI del firmante no coincide con el del paciente");
        }
        if (request.firmanteTipo() == FirmanteTipo.REPRESENTANTE
                && (request.parentesco() == null || request.parentesco().isBlank())) {
            throw new DatosInvalidosException("Indique el parentesco del representante legal");
        }
        if (c.isRequiereTestigo()
                && (request.testigoNombre() == null
                        || request.testigoNombre().isBlank()
                        || request.testigoDni() == null
                        || request.testigoDni().isBlank())) {
            throw new DatosInvalidosException("Este procedimiento exige los datos de un testigo");
        }
    }

    private static byte[] decodificarFirma(String firma) {
        String datos = firma.contains(",") ? firma.substring(firma.indexOf(',') + 1) : firma;
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(datos.strip());
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException("La firma no tiene un formato válido");
        }
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(bytes));
            if (imagen == null) {
                throw new DatosInvalidosException("La firma debe ser una imagen PNG");
            }
            int trazos = 0;
            for (int x = 0; x < imagen.getWidth(); x++) {
                for (int y = 0; y < imagen.getHeight(); y++) {
                    int argb = imagen.getRGB(x, y);
                    boolean opaco = (argb >>> 24) > 40;
                    boolean oscuro = ((argb >> 16 & 0xFF) + (argb >> 8 & 0xFF) + (argb & 0xFF)) / 3 < 200;
                    if (opaco && oscuro) {
                        trazos++;
                    }
                }
            }
            if (trazos < PIXELES_MINIMOS_FIRMA) {
                throw new DatosInvalidosException("La firma está vacía, dibújela en el recuadro");
            }
        } catch (IOException ex) {
            throw new DatosInvalidosException("La firma no se pudo leer");
        }
        return bytes;
    }

    private byte[] leerArchivo(Archivo archivo) {
        var descarga = archivoService.abrirSinAutorizar(archivo, null);
        try (var objeto = descarga.objeto()) {
            return objeto.flujo().readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el archivo", ex);
        }
    }

    private String codigoUnico() {
        for (int intento = 0; intento < 5; intento++) {
            String codigo = CodigoDocumentoClinico.nuevo();
            if (consentimientos.findByCodigo(codigo).isEmpty()
                    && documentos.findByCodigo(codigo).isEmpty()) {
                return codigo;
            }
        }
        throw new IllegalStateException("No se pudo generar un código de verificación único");
    }

    private static String componerTexto(ConsentimientoPlantilla p, String procedimiento) {
        return "PROCEDIMIENTO\n" + compactar(procedimiento) + "\n\nDESCRIPCIÓN\n" + compactar(p.getTexto())
                + "\n\nRIESGOS\n" + compactar(p.getRiesgos()) + "\n\nBENEFICIOS\n" + compactar(p.getBeneficios())
                + "\n\nALTERNATIVAS\n" + compactar(p.getAlternativas());
    }

    private static String compactar(String texto) {
        return texto.strip().replaceAll("\\n{2,}", "\n");
    }

    private Medico medico(UsuarioPrincipal actor) {
        return medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ForbiddenOperationException("Solo el personal médico solicita consentimientos"));
    }

    private Usuario usuario(UUID id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    private ConsentimientoDetalle detalle(Consentimiento c) {
        return new ConsentimientoDetalle(
                DocumentosMedicosMapper.consentimiento(c),
                c.getPlantillaVersion(),
                c.getTextoAceptado(),
                c.getFirmanteTipo(),
                c.getFirmanteNombre(),
                c.getFirmanteDni(),
                c.getFirmanteParentesco(),
                c.getTestigoNombre(),
                c.getTestigoDni(),
                c.getMotivoRevocacion(),
                c.getCodigoVerificacion(),
                c.getRevocadoEn());
    }
}

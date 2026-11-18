package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.ArchivoResponse;
import com.codesoftlabs.tusalud.app.entities.Archivo;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.RangoNoSatisfacibleException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ArchivoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.ImagenesService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import com.codesoftlabs.tusalud.app.storage.AlmacenamientoException;
import com.codesoftlabs.tusalud.app.storage.DetectorContenido;
import com.codesoftlabs.tusalud.app.storage.NombresArchivo;
import com.codesoftlabs.tusalud.app.storage.ObjetoNoEncontradoException;
import com.codesoftlabs.tusalud.app.storage.PropositoArchivo;
import com.codesoftlabs.tusalud.app.storage.StorageProperties;
import com.codesoftlabs.tusalud.app.storage.StorageService;
import com.codesoftlabs.tusalud.app.storage.StorageService.ObjetoAlmacenado;
import com.codesoftlabs.tusalud.app.storage.TipoContenido;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ArchivoServiceImpl implements ArchivoService {

    private static final Pattern PATRON_RANGO = Pattern.compile("^bytes=(\\d*)-(\\d*)$");
    private static final Set<RolNombre> LECTORES_PLANTILLA = EnumSet.of(
            RolNombre.ROLE_ADMIN,
            RolNombre.ROLE_RRHH,
            RolNombre.ROLE_GERENCIA,
            RolNombre.ROLE_FINANZAS,
            RolNombre.ROLE_OPERACIONES,
            RolNombre.ROLE_CALIDAD);

    private final ArchivoRepository archivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final StorageService almacen;
    private final StorageProperties propiedades;
    private final DetectorContenido detector;
    private final RelacionAsistencialService relacionAsistencial;
    private final AuditoriaClinicaService auditoriaClinica;
    private final ImagenesService imagenes;

    public ArchivoServiceImpl(
            ArchivoRepository archivoRepository,
            UsuarioRepository usuarioRepository,
            PacienteRepository pacienteRepository,
            StorageService almacen,
            StorageProperties propiedades,
            DetectorContenido detector,
            RelacionAsistencialService relacionAsistencial,
            AuditoriaClinicaService auditoriaClinica,
            @Lazy ImagenesService imagenes) {
        this.archivoRepository = archivoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.almacen = almacen;
        this.propiedades = propiedades;
        this.detector = detector;
        this.relacionAsistencial = relacionAsistencial;
        this.auditoriaClinica = auditoriaClinica;
        this.imagenes = imagenes;
    }

    @Override
    @Transactional
    public ArchivoResponse subir(
            UsuarioPrincipal actor,
            PropositoArchivo proposito,
            String nombre,
            byte[] contenido,
            UUID referenciaId,
            String referenciaTipo) {
        if (proposito == null) {
            throw new DatosInvalidosException("Indique el propósito del archivo");
        }
        Usuario subidoPor = buscarUsuario(actor.getId());
        Usuario propietario;
        String tipoReferencia = null;
        UUID idReferencia = null;
        switch (proposito) {
            case AVATAR -> propietario = subidoPor;
            case DOCUMENTO_PACIENTE -> {
                if (!tieneRol(actor, RolNombre.ROLE_MEDICO)) {
                    throw new ForbiddenOperationException("Solo el personal médico adjunta documentos del paciente");
                }
                if (referenciaId == null) {
                    throw new DatosInvalidosException("Indique el paciente al que pertenece el documento");
                }
                Paciente paciente = pacienteRepository
                        .findByIdWithUsuario(referenciaId)
                        .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", referenciaId));
                relacionAsistencial.exigirRelacion(
                        actor, paciente.getId(), "No tiene relación asistencial con este paciente");
                propietario = paciente.getUsuario();
                tipoReferencia = REFERENCIA_PACIENTE;
                idReferencia = paciente.getId();
            }
            case PLANTILLA -> {
                if (!tieneAlgunRol(actor, RolNombre.ROLE_ADMIN, RolNombre.ROLE_RRHH)) {
                    throw new ForbiddenOperationException("No puede cargar plantillas");
                }
                propietario = null;
                tipoReferencia = recortar(referenciaTipo, 40);
                idReferencia = referenciaId;
            }
            case IMAGEN_DIAGNOSTICA ->
                throw new DatosInvalidosException("Las imágenes diagnósticas se cargan desde la orden de imagen");
            case REPORTE -> throw new DatosInvalidosException("Los reportes los genera el sistema");
            default -> throw new DatosInvalidosException("Propósito de archivo no admitido");
        }
        Archivo archivo = almacenar(subidoPor, propietario, proposito, nombre, contenido, tipoReferencia, idReferencia);
        if (proposito == PropositoArchivo.DOCUMENTO_PACIENTE) {
            auditoriaClinica.registrar(
                    actor,
                    idReferencia,
                    AuditoriaClinicaService.DOCUMENTO_CARGADO,
                    "Documento " + archivo.getNombreOriginal());
        }
        return ArchivoResponse.from(archivo);
    }

    @Override
    @Transactional
    public Archivo almacenar(
            Usuario subidoPor,
            Usuario propietario,
            PropositoArchivo proposito,
            String nombre,
            byte[] contenido,
            String referenciaTipo,
            UUID referenciaId) {
        if (contenido == null || contenido.length == 0) {
            throw new DatosInvalidosException("El archivo está vacío");
        }
        if (contenido.length > proposito.tamanoMaximo()) {
            throw new DatosInvalidosException(
                    "El archivo supera el máximo de " + (proposito.tamanoMaximo() / (1024 * 1024)) + " MB");
        }
        String nombreSeguro = NombresArchivo.sanitizar(nombre);
        String extension = NombresArchivo.extension(nombreSeguro);
        if (extension.isEmpty()) {
            throw new DatosInvalidosException("El archivo debe tener una extensión");
        }
        TipoContenido tipo = detector.detectar(contenido)
                .orElseThrow(() -> new DatosInvalidosException(
                        "El tipo de archivo no está permitido o su contenido no es reconocible"));
        if (!proposito.tiposPermitidos().contains(tipo)) {
            throw new DatosInvalidosException("No se admiten archivos " + tipo.mime() + " para este uso");
        }
        if (!tipo.admiteExtension(extension)) {
            throw new DatosInvalidosException("El contenido del archivo no coincide con su extensión");
        }
        String bucket = proposito.bucket(propiedades.buckets());
        String clave = UUID.randomUUID().toString();
        almacen.guardar(bucket, clave, new ByteArrayInputStream(contenido), contenido.length, tipo.mime());
        try {
            Archivo archivo = Archivo.builder()
                    .bucket(bucket)
                    .clave(clave)
                    .nombreOriginal(nombreSeguro)
                    .tipoContenido(tipo.mime())
                    .tamano(contenido.length)
                    .sha256(sha256(contenido))
                    .proposito(proposito)
                    .propietario(propietario)
                    .subidoPor(subidoPor)
                    .referenciaTipo(referenciaTipo)
                    .referenciaId(referenciaId)
                    .createdAt(LocalDateTime.now())
                    .build();
            Archivo guardado = archivoRepository.saveAndFlush(archivo);
            log.info("Archivo {} guardado en {} ({} bytes, {})", guardado.getId(), bucket, contenido.length, proposito);
            return guardado;
        } catch (RuntimeException ex) {
            limpiar(bucket, clave);
            throw ex;
        }
    }

    @Override
    @Transactional
    public Descarga descargar(UsuarioPrincipal actor, UUID id, String rango) {
        Archivo archivo = buscar(id);
        autorizarLectura(actor, archivo, true);
        return abrirSinAutorizar(archivo, rango);
    }

    @Override
    public Descarga abrirSinAutorizar(Archivo archivo, String rango) {
        long tamano = archivo.getTamano();
        long desde = 0;
        long hasta = tamano - 1;
        boolean parcial = false;
        Matcher coincidencia = rango == null ? null : PATRON_RANGO.matcher(rango.trim());
        if (coincidencia != null && coincidencia.matches()) {
            String inicio = coincidencia.group(1);
            String fin = coincidencia.group(2);
            if (inicio.isEmpty() && fin.isEmpty()) {
                throw new RangoNoSatisfacibleException(tamano);
            }
            try {
                if (inicio.isEmpty()) {
                    long sufijo = Long.parseLong(fin);
                    if (sufijo <= 0) {
                        throw new RangoNoSatisfacibleException(tamano);
                    }
                    desde = Math.max(0, tamano - sufijo);
                } else {
                    desde = Long.parseLong(inicio);
                    hasta = fin.isEmpty() ? tamano - 1 : Math.min(Long.parseLong(fin), tamano - 1);
                }
            } catch (NumberFormatException ex) {
                throw new RangoNoSatisfacibleException(tamano);
            }
            if (desde >= tamano || desde > hasta) {
                throw new RangoNoSatisfacibleException(tamano);
            }
            parcial = true;
        }
        try {
            ObjetoAlmacenado objeto = parcial
                    ? almacen.abrir(archivo.getBucket(), archivo.getClave(), desde, hasta)
                    : almacen.abrir(archivo.getBucket(), archivo.getClave());
            return new Descarga(archivo, objeto, desde, hasta, parcial);
        } catch (ObjetoNoEncontradoException ex) {
            log.error("El archivo {} existe en la base pero no en el almacén", archivo.getId());
            throw new ResourceNotFoundException("Archivo", "id", archivo.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoResponse metadatos(UsuarioPrincipal actor, UUID id) {
        Archivo archivo = buscar(id);
        autorizarLectura(actor, archivo, false);
        return ArchivoResponse.from(archivo);
    }

    @Override
    @Transactional
    public void eliminar(UsuarioPrincipal actor, UUID id) {
        Archivo archivo = buscar(id);
        switch (archivo.getProposito()) {
            case AVATAR -> throw new DatosInvalidosException("La foto de perfil se quita desde el perfil");
            case IMAGEN_DIAGNOSTICA ->
                throw new DatosInvalidosException("Las imágenes diagnósticas se quitan desde la orden de imagen");
            case DOCUMENTO_PACIENTE -> {
                if (!tieneRol(actor, RolNombre.ROLE_MEDICO)
                        || archivo.getReferenciaId() == null
                        || !relacionAsistencial.tieneRelacion(actor, archivo.getReferenciaId())) {
                    throw new ForbiddenOperationException("No puede eliminar este documento");
                }
            }
            case REPORTE -> {
                boolean propio = archivo.getSubidoPor().getId().equals(actor.getId());
                if (!propio && !tieneRol(actor, RolNombre.ROLE_ADMIN)) {
                    throw new ForbiddenOperationException("No puede eliminar este reporte");
                }
            }
            case PLANTILLA -> {
                if (!tieneAlgunRol(actor, RolNombre.ROLE_ADMIN, RolNombre.ROLE_RRHH)) {
                    throw new ForbiddenOperationException("No puede eliminar plantillas");
                }
            }
            default -> throw new ForbiddenOperationException("No puede eliminar este archivo");
        }
        eliminarSinAutorizar(archivo);
    }

    @Override
    @Transactional
    public void eliminarSinAutorizar(Archivo archivo) {
        archivo.setEliminado(true);
        archivo.setEliminadoEn(LocalDateTime.now());
        archivoRepository.save(archivo);
        limpiar(archivo.getBucket(), archivo.getClave());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArchivoResponse> listar(UsuarioPrincipal actor, String referenciaTipo, UUID referenciaId) {
        if (referenciaTipo == null || referenciaTipo.isBlank() || referenciaId == null) {
            throw new DatosInvalidosException("Indique el tipo y el id de la referencia");
        }
        return archivoRepository.findActivosDeReferencia(referenciaTipo.trim(), referenciaId).stream()
                .filter(archivo -> puedeLeer(actor, archivo))
                .map(ArchivoResponse::from)
                .toList();
    }

    private boolean puedeLeer(UsuarioPrincipal actor, Archivo archivo) {
        try {
            autorizarLectura(actor, archivo, false);
            return true;
        } catch (ForbiddenOperationException | ResourceNotFoundException ex) {
            return false;
        }
    }

    private void autorizarLectura(UsuarioPrincipal actor, Archivo archivo, boolean auditar) {
        switch (archivo.getProposito()) {
            case AVATAR -> {}
            case IMAGEN_DIAGNOSTICA -> imagenes.exigirLecturaDeArchivo(actor, archivo.getId(), auditar);
            case DOCUMENTO_PACIENTE -> autorizarDocumento(actor, archivo, auditar);
            case REPORTE -> autorizarReporte(actor, archivo);
            case PLANTILLA -> {
                if (LECTORES_PLANTILLA.stream().noneMatch(rol -> tieneRol(actor, rol))) {
                    throw new ForbiddenOperationException("No tiene acceso a este archivo");
                }
            }
            default -> throw new ForbiddenOperationException("No tiene acceso a este archivo");
        }
    }

    private void autorizarDocumento(UsuarioPrincipal actor, Archivo archivo, boolean auditar) {
        UUID pacienteId = archivo.getReferenciaId();
        if (archivo.getPropietario() != null && archivo.getPropietario().getId().equals(actor.getId())) {
            return;
        }
        if (pacienteId != null
                && tieneRol(actor, RolNombre.ROLE_MEDICO)
                && relacionAsistencial.tieneRelacion(actor, pacienteId)) {
            if (auditar) {
                auditoriaClinica.registrar(
                        actor,
                        pacienteId,
                        AuditoriaClinicaService.DOCUMENTO_CONSULTADO,
                        "Documento " + archivo.getNombreOriginal());
            }
            return;
        }
        throw new ForbiddenOperationException("No tiene acceso a este archivo");
    }

    private void autorizarReporte(UsuarioPrincipal actor, Archivo archivo) {
        if ("EXPORTACION".equals(archivo.getReferenciaTipo())) {
            if (!archivo.getSubidoPor().getId().equals(actor.getId())) {
                throw new ForbiddenOperationException("No tiene acceso a este archivo");
            }
            return;
        }
        if (archivo.getSubidoPor().getId().equals(actor.getId()) || tieneRol(actor, RolNombre.ROLE_ADMIN)) {
            return;
        }
        String tipo = archivo.getReferenciaTipo() == null ? "" : archivo.getReferenciaTipo();
        boolean permitido = switch (tipo) {
            case "REPORTE_EJECUTIVO" -> tieneRol(actor, RolNombre.ROLE_GERENCIA);
            case "REPORTE_FINANCIERO" -> tieneAlgunRol(actor, RolNombre.ROLE_FINANZAS, RolNombre.ROLE_GERENCIA);
            case "REPORTE_OPERATIVO" -> tieneAlgunRol(actor, RolNombre.ROLE_OPERACIONES, RolNombre.ROLE_GERENCIA);
            case "REPORTE_CALIDAD" -> tieneAlgunRol(actor, RolNombre.ROLE_CALIDAD, RolNombre.ROLE_GERENCIA);
            default -> false;
        };
        if (!permitido) {
            throw new ForbiddenOperationException("No tiene acceso a este archivo");
        }
    }

    private Archivo buscar(UUID id) {
        return archivoRepository.findActivo(id).orElseThrow(() -> new ResourceNotFoundException("Archivo", "id", id));
    }

    private Usuario buscarUsuario(UUID id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    private void limpiar(String bucket, String clave) {
        try {
            almacen.eliminar(bucket, clave);
        } catch (AlmacenamientoException ex) {
            log.warn("No se pudo quitar el objeto {} del almacén: {}", clave, ex.getMessage());
        }
    }

    private static boolean tieneRol(UsuarioPrincipal actor, RolNombre rol) {
        return actor.getRoles().contains(rol.name());
    }

    private static boolean tieneAlgunRol(UsuarioPrincipal actor, RolNombre... roles) {
        for (RolNombre rol : roles) {
            if (tieneRol(actor, rol)) {
                return true;
            }
        }
        return false;
    }

    private static String recortar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }
}

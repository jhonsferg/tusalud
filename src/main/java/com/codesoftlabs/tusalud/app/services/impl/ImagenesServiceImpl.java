package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.CancelarOrdenImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.EstudioImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.InformeImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.OrdenImagenCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ProgramarImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.RealizarImagenRequest;
import com.codesoftlabs.tusalud.app.dto.response.AgendaImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.ArchivoImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.EstudioImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.ImagenesResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.InformeImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.MiFirmaImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenImagenResponse;
import com.codesoftlabs.tusalud.app.entities.Archivo;
import com.codesoftlabs.tusalud.app.entities.ArchivoImagen;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.EstudioImagen;
import com.codesoftlabs.tusalud.app.entities.InformeImagen;
import com.codesoftlabs.tusalud.app.entities.ItemOrdenImagen;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.OrdenImagen;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen;
import com.codesoftlabs.tusalud.app.entities.enums.ModalidadImagen;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ArchivoImagenRepository;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.EstudioImagenRepository;
import com.codesoftlabs.tusalud.app.repositories.InformeImagenRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.OrdenImagenRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.ImagenesService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import com.codesoftlabs.tusalud.app.storage.PropositoArchivo;
import com.codesoftlabs.tusalud.app.storage.StorageService.ObjetoAlmacenado;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ImagenesServiceImpl implements ImagenesService {

    static final int MAX_ARCHIVOS_POR_ORDEN = 12;
    static final int MAX_BYTES_ARCHIVO = 5 * 1024 * 1024;
    private static final int LIMITE_ORDENES = 200;
    private static final int VENTANA_CONSULTA = 1000;
    private static final int MAX_DIAS_AGENDA = 31;
    private static final Set<EstadoOrdenImagen> ESTADOS_PENDIENTES =
            EnumSet.of(EstadoOrdenImagen.SOLICITADA, EstadoOrdenImagen.PROGRAMADA, EstadoOrdenImagen.REALIZADA);
    private static final Set<EstadoOrdenImagen> ESTADOS_CARGA =
            EnumSet.of(EstadoOrdenImagen.PROGRAMADA, EstadoOrdenImagen.REALIZADA);

    private enum Acceso {
        PERSONAL,
        MEDICO,
        PACIENTE,
        ADMINISTRACION
    }

    private final EstudioImagenRepository estudioRepository;
    private final OrdenImagenRepository ordenRepository;
    private final ArchivoImagenRepository archivoRepository;
    private final InformeImagenRepository informeRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final AtencionRepository atencionRepository;
    private final UsuarioRepository usuarioRepository;
    private final RelacionAsistencialService relacionAsistencial;
    private final AuditoriaClinicaService auditoriaClinica;
    private final NotificacionService notificacionService;
    private final ArchivoService archivoService;
    private final com.codesoftlabs.tusalud.app.services.seguros.PreautorizacionService preautorizacionService;

    @Override
    @Transactional(readOnly = true)
    public List<EstudioImagenResponse> listarEstudios(String busqueda, ModalidadImagen modalidad, Boolean activo) {
        String termino = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        return estudioRepository.findAllByOrderByNombreAsc().stream()
                .filter(estudio -> modalidad == null || estudio.getModalidad() == modalidad)
                .filter(estudio -> activo == null || estudio.isActivo() == activo)
                .filter(estudio -> termino.isEmpty()
                        || estudio.getNombre().toLowerCase(Locale.ROOT).contains(termino)
                        || estudio.getCodigo().toLowerCase(Locale.ROOT).contains(termino)
                        || estudio.getRegion().toLowerCase(Locale.ROOT).contains(termino))
                .map(EstudioImagenResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EstudioImagenResponse obtenerEstudio(UUID id) {
        return EstudioImagenResponse.from(buscarEstudio(id));
    }

    @Override
    public EstudioImagenResponse crearEstudio(EstudioImagenRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        if (estudioRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe un estudio con el código " + codigo);
        }
        EstudioImagen estudio = EstudioImagen.builder().build();
        aplicar(estudio, request, codigo);
        return EstudioImagenResponse.from(estudioRepository.save(estudio));
    }

    @Override
    public EstudioImagenResponse actualizarEstudio(UUID id, EstudioImagenRequest request) {
        EstudioImagen estudio = buscarEstudio(id);
        String codigo = normalizarCodigo(request.codigo());
        estudioRepository
                .findByCodigoIgnoreCase(codigo)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ConflictException("Ya existe un estudio con el código " + codigo);
                });
        aplicar(estudio, request, codigo);
        estudioRepository.flush();
        return EstudioImagenResponse.from(estudio);
    }

    @Override
    public OrdenImagenResponse crearOrden(UsuarioPrincipal actor, OrdenImagenCreateRequest request) {
        Medico medico = medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ForbiddenOperationException("Solo los médicos pueden solicitar estudios"));
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.pacienteId()));
        relacionAsistencial.exigirRelacion(
                actor,
                paciente.getId(),
                "Solo puede solicitar estudios para pacientes que atiende o que otro médico compartió con usted");
        Atencion atencion = resolverAtencion(request.atencionId(), paciente, medico);
        List<EstudioImagen> estudios = resolverEstudios(request.estudioIds());
        BigDecimal total = estudios.stream().map(EstudioImagen::getPrecio).reduce(BigDecimal.ZERO, BigDecimal::add);

        OrdenImagen nueva = OrdenImagen.builder()
                .codigo("PEND-"
                        + java.util
                                .UUID
                                .randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 15))
                .fecha(LocalDateTime.now())
                .estado(EstadoOrdenImagen.SOLICITADA)
                .prioridad(request.prioridad())
                .paciente(paciente)
                .medico(medico)
                .atencion(atencion)
                .indicacionClinica(request.indicacionClinica().trim())
                .total(total)
                .build();
        estudios.forEach(estudio ->
                nueva.agregarItem(ItemOrdenImagen.builder().estudio(estudio).build()));
        OrdenImagen orden = ordenRepository.saveAndFlush(nueva);
        orden.setCodigo(String.format("IMG-%06d", ordenRepository.count()));

        notificarSolicitud(orden, paciente);
        return OrdenImagenResponse.completa(orden, List.of(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenImagenResponse> listarOrdenes(
            UsuarioPrincipal actor, EstadoOrdenImagen estado, String busqueda, PrioridadOrden prioridad) {
        boolean personal = tieneRol(actor, RolNombre.ROLE_IMAGENES);
        List<OrdenImagen> base = personal
                ? ordenRepository.findRecientes(PageRequest.of(0, VENTANA_CONSULTA))
                : ordenRepository.findDelSolicitante(actor.getId(), PageRequest.of(0, VENTANA_CONSULTA));
        String termino = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        return base.stream()
                .filter(orden -> estado == null || orden.getEstado() == estado)
                .filter(orden -> prioridad == null || orden.getPrioridad() == prioridad)
                .filter(orden -> termino.isEmpty() || coincide(orden, termino))
                .sorted(Comparator.comparingInt(ImagenesServiceImpl::rango)
                        .thenComparing(OrdenImagen::getFecha, Comparator.reverseOrder()))
                .limit(LIMITE_ORDENES)
                .map(OrdenImagenResponse::resumen)
                .toList();
    }

    @Override
    public List<OrdenImagenResponse> ordenesDePaciente(UsuarioPrincipal actor, UUID pacienteId) {
        relacionAsistencial.exigirRelacion(actor, pacienteId, "No tiene relación asistencial con este paciente");
        auditoriaClinica.registrar(
                actor, pacienteId, AuditoriaClinicaService.IMAGENES_CONSULTADAS, "Listado de estudios de imagen");
        return ordenRepository.findDePaciente(pacienteId).stream()
                .map(OrdenImagenResponse::resumen)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenImagenResponse> misEstudios(UsuarioPrincipal actor) {
        return ordenRepository.findDelPaciente(actor.getId()).stream()
                .filter(orden -> orden.getEstado() != EstadoOrdenImagen.CANCELADA)
                .map(orden -> {
                    boolean informada = orden.getEstado() == EstadoOrdenImagen.INFORMADA;
                    List<ArchivoImagenRepository.Metadatos> archivos =
                            informada ? archivoRepository.metadatosDeOrden(orden.getId()) : List.of();
                    InformeImagen informe = informada
                            ? informeRepository
                                    .findFirstByOrdenIdOrderByVersionDesc(orden.getId())
                                    .orElse(null)
                            : null;
                    return OrdenImagenResponse.paraPaciente(orden, archivos, informe);
                })
                .toList();
    }

    @Override
    public OrdenImagenResponse obtenerOrden(UsuarioPrincipal actor, UUID id) {
        OrdenImagen orden = buscarOrden(id);
        Acceso acceso = acceso(actor, orden, true);
        return switch (acceso) {
            case ADMINISTRACION -> OrdenImagenResponse.deAdministracion(orden);
            case PACIENTE ->
                OrdenImagenResponse.paraPaciente(
                        orden,
                        orden.getEstado() == EstadoOrdenImagen.INFORMADA
                                ? archivoRepository.metadatosDeOrden(id)
                                : List.of(),
                        informeActual(id));
            case PERSONAL, MEDICO ->
                OrdenImagenResponse.completa(orden, archivoRepository.metadatosDeOrden(id), informeActual(id));
        };
    }

    @Override
    public OrdenImagenResponse programar(UsuarioPrincipal actor, UUID id, ProgramarImagenRequest request) {
        OrdenImagen orden = buscarOrden(id);
        if (orden.getEstado() != EstadoOrdenImagen.SOLICITADA && orden.getEstado() != EstadoOrdenImagen.PROGRAMADA) {
            throw new ConflictException("Solo se puede programar una orden solicitada o ya programada");
        }
        preautorizacionService.exigirSiCorresponde(
                orden.getPaciente(),
                com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio.IMAGEN,
                orden.getId(),
                orden.getPrioridad() == com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden.URGENTE);
        LocalDateTime inicio = request.programadaPara();
        if (inicio.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new DatosInvalidosException("La fecha y hora de la toma no pueden estar en el pasado");
        }
        String sala = request.sala().trim();
        exigirSalaLibre(orden, sala, inicio);
        orden.setProgramadaPara(inicio);
        orden.setSala(sala);
        orden.setEstado(EstadoOrdenImagen.PROGRAMADA);
        notificacionService.crear(
                orden.getPaciente().getUsuario(),
                TipoNotificacion.ORDEN_IMAGEN,
                "Tu estudio de imagen fue programado",
                "El estudio " + orden.getCodigo() + " es el " + inicio.toLocalDate() + " a las "
                        + inicio.toLocalTime().withSecond(0).withNano(0) + " en " + sala
                        + ". Revisa la preparación indicada.",
                "/mis-estudios");
        return OrdenImagenResponse.completa(
                ordenRepository.save(orden), archivoRepository.metadatosDeOrden(id), informeActual(id));
    }

    @Override
    public OrdenImagenResponse realizar(UsuarioPrincipal actor, UUID id, RealizarImagenRequest request) {
        OrdenImagen orden = buscarOrden(id);
        if (orden.getEstado() != EstadoOrdenImagen.PROGRAMADA) {
            throw new ConflictException("Solo se puede registrar la toma de una orden programada");
        }
        if (archivoRepository.contarDeOrden(id) == 0) {
            throw new DatosInvalidosException("Suba al menos una imagen antes de registrar la toma");
        }
        Usuario tecnico = buscarUsuario(actor.getId());
        orden.setEstado(EstadoOrdenImagen.REALIZADA);
        orden.setRealizadaEn(LocalDateTime.now());
        orden.setRealizadaPor(tecnico);
        orden.setObservacionesToma(request == null ? null : vacioANulo(request.observaciones()));
        return OrdenImagenResponse.completa(
                ordenRepository.save(orden), archivoRepository.metadatosDeOrden(id), informeActual(id));
    }

    @Override
    public OrdenImagenResponse informar(UsuarioPrincipal actor, UUID id, InformeImagenRequest request) {
        OrdenImagen orden = buscarOrden(id);
        boolean correccion = orden.getEstado() == EstadoOrdenImagen.INFORMADA;
        if (orden.getEstado() != EstadoOrdenImagen.REALIZADA && !correccion) {
            throw new ConflictException("Solo se informa una orden cuya toma ya fue realizada");
        }
        String motivo = vacioANulo(request.motivoCorreccion());
        if (correccion && motivo == null) {
            throw new DatosInvalidosException("Para corregir un informe emitido debe indicar el motivo");
        }
        Usuario autor = buscarUsuario(actor.getId());
        int version = informeRepository
                .findFirstByOrdenIdOrderByVersionDesc(id)
                .map(informe -> informe.getVersion() + 1)
                .orElse(1);
        InformeImagen informe = informeRepository.save(InformeImagen.builder()
                .orden(orden)
                .version(version)
                .hallazgos(request.hallazgos().trim())
                .conclusion(request.conclusion().trim())
                .hallazgoCritico(request.hallazgoCritico())
                .firmaNombre(autor.getNombreCompleto())
                .firmaColegiatura(request.firmaColegiatura().trim().toUpperCase(Locale.ROOT))
                .autor(autor)
                .motivoCorreccion(correccion ? motivo : null)
                .createdAt(LocalDateTime.now())
                .build());
        boolean eraCritico = orden.isHallazgoCritico();
        orden.setEstado(EstadoOrdenImagen.INFORMADA);
        orden.setInformadaEn(
                correccion && orden.getInformadaEn() != null ? orden.getInformadaEn() : LocalDateTime.now());
        orden.setHallazgoCritico(request.hallazgoCritico());
        orden = ordenRepository.save(orden);
        if (!correccion) {
            notificarInforme(orden);
        }
        if (request.hallazgoCritico() && !eraCritico) {
            notificarCritico(orden);
        }
        return OrdenImagenResponse.completa(orden, archivoRepository.metadatosDeOrden(id), informe);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InformeImagenResponse> historialInformes(UsuarioPrincipal actor, UUID id) {
        OrdenImagen orden = buscarOrden(id);
        Acceso acceso = acceso(actor, orden, true);
        if (acceso != Acceso.PERSONAL && acceso != Acceso.MEDICO) {
            throw new ForbiddenOperationException("No tiene acceso al historial de informes de esta orden");
        }
        return informeRepository.findByOrdenIdOrderByVersionDesc(id).stream()
                .map(InformeImagenResponse::completo)
                .toList();
    }

    @Override
    public OrdenImagenResponse cancelar(UsuarioPrincipal actor, UUID id, CancelarOrdenImagenRequest request) {
        OrdenImagen orden = buscarOrden(id);
        if (orden.getEstado() != EstadoOrdenImagen.SOLICITADA && orden.getEstado() != EstadoOrdenImagen.PROGRAMADA) {
            throw new ConflictException("La orden ya no se puede cancelar");
        }
        boolean personal = tieneRol(actor, RolNombre.ROLE_IMAGENES) || tieneRol(actor, RolNombre.ROLE_ADMIN);
        boolean solicitante = orden.getMedico().getUsuario().getId().equals(actor.getId());
        if (!personal) {
            if (!solicitante) {
                throw new ForbiddenOperationException("Solo el médico solicitante puede cancelar esta orden");
            }
            if (orden.getEstado() != EstadoOrdenImagen.SOLICITADA) {
                throw new ForbiddenOperationException(
                        "El médico solo puede cancelar la orden mientras no esté programada");
            }
        }
        orden.setEstado(EstadoOrdenImagen.CANCELADA);
        orden.setMotivoCancelacion(request == null ? null : vacioANulo(request.motivo()));
        return OrdenImagenResponse.resumen(ordenRepository.save(orden));
    }

    @Override
    public List<ArchivoImagenResponse> subirArchivos(UsuarioPrincipal actor, UUID id, List<ArchivoEntrante> archivos) {
        OrdenImagen orden = buscarOrden(id);
        if (!ESTADOS_CARGA.contains(orden.getEstado())) {
            throw new ConflictException("Las imágenes se cargan con la orden programada o realizada");
        }
        if (archivos == null || archivos.isEmpty()) {
            throw new DatosInvalidosException("Debe adjuntar al menos un archivo");
        }
        long existentes = archivoRepository.contarDeOrden(id);
        if (existentes + archivos.size() > MAX_ARCHIVOS_POR_ORDEN) {
            throw new DatosInvalidosException(
                    "Una orden admite hasta " + MAX_ARCHIVOS_POR_ORDEN + " archivos y ya tiene " + existentes);
        }
        Usuario subidoPor = buscarUsuario(actor.getId());
        int posicion = archivoRepository.ultimaPosicion(id);
        List<ArchivoImagen> nuevos = new ArrayList<>();
        for (ArchivoEntrante entrante : archivos) {
            byte[] contenido = entrante.contenido();
            if (contenido == null || contenido.length == 0) {
                throw new DatosInvalidosException(
                        "El archivo " + nombreSeguro(entrante.nombreOriginal()) + " está vacío");
            }
            if (contenido.length > MAX_BYTES_ARCHIVO) {
                throw new DatosInvalidosException(
                        "El archivo " + nombreSeguro(entrante.nombreOriginal()) + " supera los 5 MB");
            }
            String tipo = detectarTipo(contenido);
            if (tipo == null) {
                throw new DatosInvalidosException(
                        "El archivo " + nombreSeguro(entrante.nombreOriginal()) + " no es JPEG, PNG ni PDF");
            }
            Archivo almacenado = archivoService.almacenar(
                    subidoPor,
                    orden.getPaciente().getUsuario(),
                    PropositoArchivo.IMAGEN_DIAGNOSTICA,
                    nombreSeguro(entrante.nombreOriginal()),
                    contenido,
                    ArchivoService.REFERENCIA_ORDEN_IMAGEN,
                    orden.getId());
            nuevos.add(ArchivoImagen.builder()
                    .orden(orden)
                    .archivo(almacenado)
                    .posicion(++posicion)
                    .build());
        }
        archivoRepository.saveAll(nuevos);
        archivoRepository.flush();
        return archivoRepository.metadatosDeOrden(id).stream()
                .map(ArchivoImagenResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoDescarga descargarArchivo(UsuarioPrincipal actor, UUID ordenId, UUID archivoId) {
        OrdenImagen orden = buscarOrden(ordenId);
        Acceso acceso = acceso(actor, orden, true);
        if (acceso == Acceso.ADMINISTRACION
                || (acceso == Acceso.PACIENTE && orden.getEstado() != EstadoOrdenImagen.INFORMADA)) {
            throw new ForbiddenOperationException("No tiene acceso a las imágenes de esta orden");
        }
        ArchivoImagen archivo = archivoRepository
                .findDeOrden(archivoId, ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo de imagen", "id", archivoId));
        Archivo almacenado = archivo.getArchivo();
        try (ObjetoAlmacenado objeto =
                archivoService.abrirSinAutorizar(almacenado, null).objeto()) {
            return new ArchivoDescarga(
                    almacenado.getNombreOriginal(),
                    almacenado.getTipoContenido(),
                    objeto.flujo().readAllBytes());
        } catch (IOException ex) {
            throw new DatosInvalidosException("No se pudo leer el archivo almacenado");
        }
    }

    @Override
    public void eliminarArchivo(UsuarioPrincipal actor, UUID ordenId, UUID archivoId) {
        OrdenImagen orden = buscarOrden(ordenId);
        if (!ESTADOS_CARGA.contains(orden.getEstado())) {
            throw new ConflictException("Las imágenes solo se quitan con la orden programada o realizada");
        }
        ArchivoImagen archivo = archivoRepository
                .findDeOrden(archivoId, ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo de imagen", "id", archivoId));
        Archivo almacenado = archivo.getArchivo();
        archivoRepository.delete(archivo);
        archivoRepository.flush();
        archivoService.eliminarSinAutorizar(almacenado);
    }

    @Override
    @Transactional(readOnly = true)
    public void exigirLecturaDeArchivo(UsuarioPrincipal actor, UUID archivoId, boolean auditar) {
        ArchivoImagen vinculo = archivoRepository
                .findPorArchivo(archivoId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo de imagen", "id", archivoId));
        OrdenImagen orden = vinculo.getOrden();
        Acceso acceso = acceso(actor, orden, auditar);
        if (acceso == Acceso.ADMINISTRACION
                || (acceso == Acceso.PACIENTE && orden.getEstado() != EstadoOrdenImagen.INFORMADA)) {
            throw new ForbiddenOperationException("No tiene acceso a las imágenes de esta orden");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgendaImagenResponse> agenda(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new DatosInvalidosException("El rango de fechas no es válido");
        }
        if (Duration.between(desde.atStartOfDay(), hasta.atStartOfDay()).toDays() > MAX_DIAS_AGENDA) {
            throw new DatosInvalidosException("El rango no puede superar " + MAX_DIAS_AGENDA + " días");
        }
        return ordenRepository
                .findProgramadasEntre(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
                .stream()
                .map(AgendaImagenResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ImagenesResumenResponse resumen() {
        LocalDate hoy = LocalDate.now();
        return new ImagenesResumenResponse(
                ordenRepository.countByEstado(EstadoOrdenImagen.SOLICITADA),
                ordenRepository.countByEstado(EstadoOrdenImagen.PROGRAMADA),
                ordenRepository.countByEstado(EstadoOrdenImagen.REALIZADA),
                ordenRepository.contarInformadasEntre(
                        EstadoOrdenImagen.INFORMADA,
                        hoy.atStartOfDay(),
                        hoy.plusDays(1).atStartOfDay()),
                ordenRepository.contarUrgentesPendientes());
    }

    @Override
    @Transactional(readOnly = true)
    public MiFirmaImagenResponse miFirma(UsuarioPrincipal actor) {
        String colegiatura = informeRepository
                .findFirstByAutorIdOrderByCreatedAtDesc(actor.getId())
                .map(InformeImagen::getFirmaColegiatura)
                .orElse(null);
        return new MiFirmaImagenResponse(actor.getNombreCompleto(), colegiatura);
    }

    private Acceso acceso(UsuarioPrincipal actor, OrdenImagen orden, boolean auditar) {
        if (tieneRol(actor, RolNombre.ROLE_IMAGENES)) {
            return Acceso.PERSONAL;
        }
        if (orden.getMedico().getUsuario().getId().equals(actor.getId())) {
            return Acceso.MEDICO;
        }
        if (tieneRol(actor, RolNombre.ROLE_MEDICO)
                && relacionAsistencial.tieneRelacion(actor, orden.getPaciente().getId())) {
            if (auditar) {
                auditoriaClinica.registrar(
                        actor,
                        orden.getPaciente().getId(),
                        AuditoriaClinicaService.IMAGENES_CONSULTADAS,
                        "Orden " + orden.getCodigo());
            }
            return Acceso.MEDICO;
        }
        if (orden.getPaciente().getUsuario().getId().equals(actor.getId())) {
            return Acceso.PACIENTE;
        }
        if (tieneRol(actor, RolNombre.ROLE_ADMIN) || tieneRol(actor, RolNombre.ROLE_RECEPCION)) {
            return Acceso.ADMINISTRACION;
        }
        throw new ForbiddenOperationException("No tiene acceso a esta orden de imagen");
    }

    private void exigirSalaLibre(OrdenImagen orden, String sala, LocalDateTime inicio) {
        int duracion = Math.max(duracionDe(orden), 5);
        LocalDateTime fin = inicio.plusMinutes(duracion);
        LocalDateTime desdeDia = inicio.toLocalDate().atStartOfDay();
        for (OrdenImagen otra : ordenRepository.findProgramadasEntre(desdeDia, desdeDia.plusDays(1))) {
            if (otra.getId().equals(orden.getId())
                    || otra.getSala() == null
                    || !otra.getSala().equalsIgnoreCase(sala)
                    || otra.getEstado() == EstadoOrdenImagen.CANCELADA) {
                continue;
            }
            LocalDateTime otraInicio = otra.getProgramadaPara();
            LocalDateTime otraFin = otraInicio.plusMinutes(Math.max(duracionDe(otra), 5));
            if (inicio.isBefore(otraFin) && otraInicio.isBefore(fin)) {
                throw new ConflictException("La sala " + sala + " ya está ocupada en ese horario");
            }
        }
    }

    private static int duracionDe(OrdenImagen orden) {
        return orden.getItems().stream()
                .mapToInt(item -> item.getEstudio().getDuracionMinutos())
                .sum();
    }

    private void aplicar(EstudioImagen estudio, EstudioImagenRequest request, String codigo) {
        estudio.setCodigo(codigo);
        estudio.setNombre(request.nombre().trim());
        estudio.setModalidad(request.modalidad());
        estudio.setRegion(request.region().trim());
        estudio.setPreparacion(vacioANulo(request.preparacion()));
        estudio.setPrecio(request.precio());
        estudio.setDuracionMinutos(request.duracionMinutos());
        estudio.setActivo(request.activo() == null || request.activo());
    }

    private Atencion resolverAtencion(UUID atencionId, Paciente paciente, Medico medico) {
        if (atencionId == null) {
            return null;
        }
        Atencion atencion = atencionRepository
                .findById(atencionId)
                .orElseThrow(() -> new ResourceNotFoundException("Atención", "id", atencionId));
        if (!atencion.getPaciente().getId().equals(paciente.getId())) {
            throw new DatosInvalidosException("La atención indicada no corresponde al paciente");
        }
        if (!atencion.getMedico().getId().equals(medico.getId())) {
            throw new DatosInvalidosException("La atención indicada no fue registrada por usted");
        }
        return atencion;
    }

    private List<EstudioImagen> resolverEstudios(List<UUID> ids) {
        Set<UUID> unicos = new LinkedHashSet<>(ids);
        Map<UUID, EstudioImagen> encontrados = estudioRepository.findAllById(unicos).stream()
                .collect(Collectors.toMap(EstudioImagen::getId, Function.identity()));
        List<EstudioImagen> estudios = new ArrayList<>();
        for (UUID id : unicos) {
            EstudioImagen estudio = encontrados.get(id);
            if (estudio == null) {
                throw new ResourceNotFoundException("Estudio", "id", id);
            }
            if (!estudio.isActivo()) {
                throw new ConflictException("El estudio " + estudio.getNombre() + " no está disponible");
            }
            estudios.add(estudio);
        }
        return estudios;
    }

    private void notificarSolicitud(OrdenImagen orden, Paciente paciente) {
        notificacionService.crear(
                paciente.getUsuario(),
                TipoNotificacion.ORDEN_IMAGEN,
                "Nuevo estudio de imagen",
                "El Dr. " + orden.getMedico().getUsuario().getNombreCompleto() + " solicitó "
                        + orden.getItems().size()
                        + (orden.getItems().size() == 1 ? " estudio" : " estudios") + " con el código "
                        + orden.getCodigo() + ". Te avisaremos cuando esté programado.",
                "/mis-estudios");
        if (orden.getPrioridad() == PrioridadOrden.URGENTE) {
            for (Usuario personal : ordenRepository.findUsuariosActivosPorRol(RolNombre.ROLE_IMAGENES)) {
                notificacionService.crear(
                        personal,
                        TipoNotificacion.ORDEN_IMAGEN,
                        "Orden urgente de imagen",
                        "La orden " + orden.getCodigo() + " es urgente y espera programación.",
                        "/imagenes/ordenes/" + orden.getId());
            }
        }
    }

    private void notificarInforme(OrdenImagen orden) {
        notificacionService.crear(
                orden.getPaciente().getUsuario(),
                TipoNotificacion.ESTUDIO_INFORMADO,
                "Tu informe de imagen está listo",
                "El estudio " + orden.getCodigo() + " ya tiene informe. Puedes revisarlo en Mis estudios.",
                "/mis-estudios");
        notificacionService.crear(
                orden.getMedico().getUsuario(),
                TipoNotificacion.ESTUDIO_INFORMADO,
                "Estudio de imagen informado",
                "La orden " + orden.getCodigo() + " de "
                        + orden.getPaciente().getUsuario().getNombreCompleto() + " ya tiene informe.",
                "/imagenes/ordenes/" + orden.getId());
    }

    private void notificarCritico(OrdenImagen orden) {
        notificacionService.crear(
                orden.getMedico().getUsuario(),
                TipoNotificacion.HALLAZGO_CRITICO,
                "Hallazgo crítico en un estudio de imagen",
                "El informe de la orden " + orden.getCodigo() + " de "
                        + orden.getPaciente().getUsuario().getNombreCompleto()
                        + " reporta un hallazgo crítico. Revísalo cuanto antes.",
                "/imagenes/ordenes/" + orden.getId());
    }

    private InformeImagen informeActual(UUID ordenId) {
        return informeRepository.findFirstByOrdenIdOrderByVersionDesc(ordenId).orElse(null);
    }

    private OrdenImagen buscarOrden(UUID id) {
        return ordenRepository
                .findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de imagen", "id", id));
    }

    private EstudioImagen buscarEstudio(UUID id) {
        return estudioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estudio", "id", id));
    }

    private Usuario buscarUsuario(UUID id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    private static boolean coincide(OrdenImagen orden, String termino) {
        return orden.getCodigo().toLowerCase(Locale.ROOT).contains(termino)
                || orden.getPaciente().getDni().contains(termino)
                || orden.getPaciente()
                        .getUsuario()
                        .getNombreCompleto()
                        .toLowerCase(Locale.ROOT)
                        .contains(termino);
    }

    private static int rango(OrdenImagen orden) {
        if (!ESTADOS_PENDIENTES.contains(orden.getEstado())) {
            return 2;
        }
        return orden.getPrioridad() == PrioridadOrden.URGENTE ? 0 : 1;
    }

    private static boolean tieneRol(UsuarioPrincipal actor, RolNombre rol) {
        return actor.getRoles().contains(rol.name());
    }

    private static String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    static String detectarTipo(byte[] contenido) {
        if (contenido.length >= 3
                && (contenido[0] & 0xFF) == 0xFF
                && (contenido[1] & 0xFF) == 0xD8
                && (contenido[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (contenido.length >= png.length && coincidePrefijo(contenido, png)) {
            return "image/png";
        }
        byte[] pdf = {0x25, 0x50, 0x44, 0x46, 0x2D};
        if (contenido.length >= pdf.length && coincidePrefijo(contenido, pdf)) {
            return "application/pdf";
        }
        return null;
    }

    private static boolean coincidePrefijo(byte[] contenido, byte[] prefijo) {
        for (int i = 0; i < prefijo.length; i++) {
            if (contenido[i] != prefijo[i]) {
                return false;
            }
        }
        return true;
    }

    static String nombreSeguro(String original) {
        String nombre = original == null ? "" : original;
        int barra = Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\'));
        if (barra >= 0) {
            nombre = nombre.substring(barra + 1);
        }
        nombre = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        nombre = nombre.replaceAll("[^A-Za-z0-9._ -]", "_")
                .replaceAll("\\.{2,}", ".")
                .trim();
        if (nombre.startsWith(".")) {
            nombre = "_" + nombre.substring(1);
        }
        if (nombre.isBlank()) {
            nombre = "imagen";
        }
        return nombre.length() > 100 ? nombre.substring(nombre.length() - 100) : nombre;
    }
}

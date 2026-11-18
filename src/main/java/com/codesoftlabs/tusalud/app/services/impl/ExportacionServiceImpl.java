package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.documentos.DocumentoGenerado;
import com.codesoftlabs.tusalud.app.documentos.DocumentosProperties;
import com.codesoftlabs.tusalud.app.documentos.FormatoDocumento;
import com.codesoftlabs.tusalud.app.documentos.ParametrosDocumento;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.dto.request.ExportacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ExportacionResponse;
import com.codesoftlabs.tusalud.app.entities.Archivo;
import com.codesoftlabs.tusalud.app.entities.Exportacion;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoExportacion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.DemasiadosIntentosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ExportacionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService.Descarga;
import com.codesoftlabs.tusalud.app.services.interfaces.DocumentoService;
import com.codesoftlabs.tusalud.app.services.interfaces.ExportacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.storage.PropositoArchivo;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class ExportacionServiceImpl implements ExportacionService {

    private static final Set<String> CLAVES_PERMITIDAS = Set.of(
            "desde",
            "hasta",
            "agrupacion",
            "q",
            "estado",
            "productoId",
            "tipo",
            "bajoStock",
            "activo",
            "area",
            "empleadoId");
    private static final String REFERENCIA = "EXPORTACION";
    private static final long MINUTOS_ATASCO = 30;

    private final ExportacionRepository exportacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoService documentoService;
    private final ArchivoService archivoService;
    private final NotificacionService notificacionService;
    private final DocumentosProperties propiedades;
    private final Executor ejecutor;
    private final ObjectMapper mapper = new ObjectMapper();

    public ExportacionServiceImpl(
            ExportacionRepository exportacionRepository,
            UsuarioRepository usuarioRepository,
            DocumentoService documentoService,
            ArchivoService archivoService,
            NotificacionService notificacionService,
            DocumentosProperties propiedades,
            @Qualifier("exportacionExecutor") Executor ejecutor) {
        this.exportacionRepository = exportacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentoService = documentoService;
        this.archivoService = archivoService;
        this.notificacionService = notificacionService;
        this.propiedades = propiedades;
        this.ejecutor = ejecutor;
    }

    @Override
    public ExportacionResponse solicitar(UsuarioPrincipal actor, ExportacionRequest request) {
        TipoDocumento tipo = TipoDocumento.desdeCodigo(request.tipo());
        if (tipo == null || tipo.directo()) {
            throw new DatosInvalidosException("El tipo de exportación no es válido");
        }
        FormatoDocumento formato =
                request.formato() == null || request.formato().isBlank()
                        ? tipo.formatos().iterator().next()
                        : FormatoDocumento.desdeTexto(request.formato());
        documentoService.exigirPermiso(actor, tipo, formato);
        Map<String, String> parametros = limpiarParametros(request.parametros());
        validarParametros(parametros);
        limitar(actor);
        Usuario usuario = usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
        Exportacion exportacion = exportacionRepository.save(Exportacion.builder()
                .tipo(tipo)
                .formato(formato)
                .parametros(serializar(parametros))
                .estado(EstadoExportacion.SOLICITADA)
                .solicitadoPor(usuario)
                .createdAt(LocalDateTime.now())
                .build());
        UUID id = exportacion.getId();
        ejecutor.execute(() -> procesar(id, actor));
        return ExportacionResponse.from(exportacionRepository.findConArchivo(id).orElse(exportacion));
    }

    @Override
    public List<ExportacionResponse> mias(UsuarioPrincipal actor) {
        return exportacionRepository.findDeUsuario(actor.getId()).stream()
                .map(ExportacionResponse::from)
                .toList();
    }

    @Override
    public ExportacionResponse obtener(UsuarioPrincipal actor, UUID id) {
        return ExportacionResponse.from(propia(actor, id));
    }

    @Override
    public Descarga descargar(UsuarioPrincipal actor, UUID id) {
        Exportacion exportacion = propia(actor, id);
        if (exportacion.getEstado() != EstadoExportacion.LISTA || exportacion.getArchivo() == null) {
            throw new ConflictException("La exportación todavía no está lista o ya expiró");
        }
        return archivoService.abrirSinAutorizar(exportacion.getArchivo(), null);
    }

    @Override
    public void eliminar(UsuarioPrincipal actor, UUID id) {
        Exportacion exportacion = propia(actor, id);
        if (exportacion.getEstado() == EstadoExportacion.PROCESANDO) {
            throw new ConflictException("No se puede eliminar una exportación en proceso");
        }
        Archivo archivo = exportacion.getArchivo();
        exportacionRepository.delete(exportacion);
        if (archivo != null) {
            archivoService.eliminarSinAutorizar(archivo);
        }
    }

    @Override
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS, initialDelay = 5)
    public void purgar() {
        LocalDateTime ahora = LocalDateTime.now();
        for (Exportacion exportacion : exportacionRepository.findVencidas(ahora)) {
            try {
                if (exportacion.getArchivo() != null) {
                    archivoService.eliminarSinAutorizar(exportacion.getArchivo());
                }
                exportacion.setEstado(EstadoExportacion.EXPIRADA);
                exportacion.setArchivo(null);
                exportacionRepository.save(exportacion);
            } catch (RuntimeException ex) {
                log.warn("No se pudo purgar la exportación {}: {}", exportacion.getId(), ex.getMessage());
            }
        }
        for (Exportacion exportacion : exportacionRepository.findAtascadas(ahora.minusMinutes(MINUTOS_ATASCO))) {
            exportacion.setEstado(EstadoExportacion.ERROR);
            exportacion.setError("La exportación no terminó, solicítela de nuevo");
            exportacion.setTerminadaEn(ahora);
            exportacionRepository.save(exportacion);
        }
    }

    void procesar(UUID id, UsuarioPrincipal actor) {
        Exportacion exportacion = exportacionRepository.findConArchivo(id).orElse(null);
        if (exportacion == null) {
            return;
        }
        exportacion.setEstado(EstadoExportacion.PROCESANDO);
        exportacion.setIniciadaEn(LocalDateTime.now());
        exportacionRepository.save(exportacion);
        try {
            DocumentoGenerado documento = documentoService.generar(
                    actor, exportacion.getTipo(), exportacion.getFormato(), deserializar(exportacion.getParametros()));
            Usuario usuario = exportacion.getSolicitadoPor();
            Archivo archivo = archivoService.almacenar(
                    usuario,
                    usuario,
                    PropositoArchivo.REPORTE,
                    documento.nombre(),
                    documento.contenido(),
                    REFERENCIA,
                    id);
            LocalDateTime ahora = LocalDateTime.now();
            exportacion.setArchivo(archivo);
            exportacion.setNombre(archivo.getNombreOriginal());
            exportacion.setTamano((long) documento.contenido().length);
            exportacion.setEstado(EstadoExportacion.LISTA);
            exportacion.setTerminadaEn(ahora);
            exportacion.setExpiraEn(ahora.plus(propiedades.retencionExportaciones()));
            exportacionRepository.save(exportacion);
            notificacionService.crear(
                    usuario,
                    TipoNotificacion.EXPORTACION_LISTA,
                    "Exportación lista",
                    "Su exportación " + archivo.getNombreOriginal() + " está lista para descargar.",
                    "/mis-exportaciones");
        } catch (DatosInvalidosException | BusinessLogicException | ForbiddenOperationException ex) {
            fallar(exportacion, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Falló la exportación {}", id, ex);
            fallar(exportacion, "No se pudo generar la exportación, inténtelo de nuevo");
        }
    }

    private void fallar(Exportacion exportacion, String mensaje) {
        exportacion.setEstado(EstadoExportacion.ERROR);
        exportacion.setError(mensaje == null ? "No se pudo generar la exportación" : recortar(mensaje));
        exportacion.setTerminadaEn(LocalDateTime.now());
        exportacionRepository.save(exportacion);
    }

    private Exportacion propia(UsuarioPrincipal actor, UUID id) {
        Exportacion exportacion = exportacionRepository
                .findConArchivo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exportación", "id", id));
        if (!exportacion.getSolicitadoPor().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("No tiene acceso a esta exportación");
        }
        return exportacion;
    }

    private void limitar(UsuarioPrincipal actor) {
        long activas = exportacionRepository.countBySolicitadoPorIdAndEstadoIn(
                actor.getId(), List.of(EstadoExportacion.SOLICITADA, EstadoExportacion.PROCESANDO));
        if (activas >= propiedades.maximoConcurrentes()) {
            throw new DemasiadosIntentosException("Ya tiene exportaciones en proceso, espere a que terminen");
        }
        long ultimaHora = exportacionRepository.countBySolicitadoPorIdAndCreatedAtAfter(
                actor.getId(), LocalDateTime.now().minusHours(1));
        if (ultimaHora >= propiedades.maximoPorHora()) {
            throw new DemasiadosIntentosException("Alcanzó el máximo de exportaciones por hora, inténtelo más tarde");
        }
    }

    private static Map<String, String> limpiarParametros(Map<String, String> entrada) {
        Map<String, String> limpio = new LinkedHashMap<>();
        if (entrada == null) {
            return limpio;
        }
        entrada.forEach((clave, valor) -> {
            if (!CLAVES_PERMITIDAS.contains(clave)) {
                throw new DatosInvalidosException("El parámetro " + clave + " no es válido");
            }
            if (valor != null && !valor.isBlank()) {
                limpio.put(clave, valor.strip());
            }
        });
        return limpio;
    }

    private static void validarParametros(Map<String, String> parametros) {
        ParametrosDocumento p = new ParametrosDocumento(parametros);
        p.rango(java.time.LocalDate.now().minusDays(29), java.time.LocalDate.now());
        p.uuid("productoId");
        p.uuid("empleadoId");
        p.booleano("bajoStock");
        p.booleano("activo");
    }

    private String serializar(Map<String, String> parametros) {
        return mapper.writeValueAsString(parametros);
    }

    private Map<String, String> deserializar(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return mapper.readValue(json, new TypeReference<Map<String, String>>() {});
    }

    private static String recortar(String texto) {
        return texto.length() > 290 ? texto.substring(0, 290) : texto;
    }
}

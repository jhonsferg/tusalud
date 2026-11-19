package com.codesoftlabs.tusalud.app.services.privacidad;

import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.IncidenteActualizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.IncidenteRequest;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.IncidenteResponse;
import com.codesoftlabs.tusalud.app.entities.IncidenteDatos;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.EstadoIncidente;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.SeveridadIncidente;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.IncidenteDatosRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentesDatosService {

    private final IncidenteDatosRepository incidentes;
    private final UsuarioRepository usuarios;
    private final NotificacionService notificaciones;

    @Value("${app.privacidad.horas-notificar-autoridad:48}")
    private int horasNotificar;

    public List<IncidenteResponse> listar() {
        LocalDateTime ahora = LocalDateTime.now();
        return incidentes.todos().stream()
                .map(i -> IncidenteResponse.de(i, ahora))
                .toList();
    }

    public IncidenteResponse obtener(UUID id) {
        return IncidenteResponse.de(cargar(id), LocalDateTime.now());
    }

    @Transactional
    public IncidenteResponse registrar(UsuarioPrincipal actor, IncidenteRequest r) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime detectado = r.detectadoEn() == null ? ahora : r.detectadoEn();
        if (detectado.isAfter(ahora.plusMinutes(5))) {
            throw new DatosInvalidosException("La fecha de detección no puede ser futura");
        }
        Usuario registrador = usuarios.getReferenceById(actor.getId());
        long total = incidentes.contarEntre(
                LocalDate.now().withDayOfYear(1).atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay());
        IncidenteDatos i = incidentes.save(IncidenteDatos.builder()
                .codigo(String.format("INC-%d-%04d", LocalDate.now().getYear(), total + 1))
                .tipo(r.tipo())
                .severidad(r.severidad())
                .estado(EstadoIncidente.ABIERTO)
                .descripcion(r.descripcion().trim())
                .datosAfectados(r.datosAfectados().trim())
                .titularesAfectados(r.titularesAfectados())
                .detectadoEn(detectado)
                .notificarAutoridadAntesDe(detectado.plusHours(horasNotificar))
                .contencion(
                        r.contencion() == null || r.contencion().isBlank()
                                ? null
                                : r.contencion().trim())
                .responsable(registrador)
                .registradoPor(registrador)
                .build());
        avisar(i);
        log.warn("Incidente de datos {} registrado, severidad {}", i.getCodigo(), i.getSeveridad());
        return IncidenteResponse.de(i, ahora);
    }

    @Transactional
    public IncidenteResponse actualizar(UsuarioPrincipal actor, UUID id, IncidenteActualizacionRequest r) {
        IncidenteDatos i = cargar(id);
        if (i.getEstado() == EstadoIncidente.CERRADO) {
            throw new ConflictException("El incidente ya está cerrado");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (r.contencion() != null && !r.contencion().isBlank()) {
            i.setContencion(r.contencion().trim());
        }
        if (r.causaRaiz() != null && !r.causaRaiz().isBlank()) {
            i.setCausaRaiz(r.causaRaiz().trim());
        }
        if (r.responsableId() != null) {
            i.setResponsable(usuarios.findById(r.responsableId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", r.responsableId())));
        }
        if (r.autoridadNotificada() && i.getAutoridadNotificadaEn() == null) {
            i.setAutoridadNotificadaEn(ahora);
        }
        if (r.titularesNotificados() && i.getTitularesNotificadosEn() == null) {
            i.setTitularesNotificadosEn(ahora);
        }
        if (r.estado() == EstadoIncidente.CERRADO) {
            boolean grave =
                    i.getSeveridad() == SeveridadIncidente.ALTA || i.getSeveridad() == SeveridadIncidente.CRITICA;
            if (grave && i.getAutoridadNotificadaEn() == null) {
                throw new ConflictException(
                        "Un incidente grave no se cierra sin registrar la notificación a la autoridad");
            }
            if (i.getCausaRaiz() == null) {
                throw new ConflictException("Registre la causa raíz antes de cerrar el incidente");
            }
            i.setCerradoEn(ahora);
        }
        i.setEstado(r.estado());
        log.info("Incidente {} actualizado por {} a {}", i.getCodigo(), actor.getId(), r.estado());
        return IncidenteResponse.de(i, ahora);
    }

    public long abiertos() {
        return incidentes.todos().stream()
                .filter(i -> i.getEstado() != EstadoIncidente.CERRADO)
                .count();
    }

    public long vencidos() {
        LocalDateTime ahora = LocalDateTime.now();
        return incidentes.todos().stream()
                .filter(i -> i.getEstado() != EstadoIncidente.CERRADO
                        && i.getAutoridadNotificadaEn() == null
                        && i.getNotificarAutoridadAntesDe().isBefore(ahora))
                .count();
    }

    private void avisar(IncidenteDatos i) {
        Set<UUID> vistos = new LinkedHashSet<>();
        List<Usuario> destinatarios = new ArrayList<>();
        for (RolNombre rol : List.of(RolNombre.ROLE_CALIDAD, RolNombre.ROLE_ADMIN)) {
            for (Usuario u : usuarios.findActivosPorRol(rol)) {
                if (vistos.add(u.getId())) {
                    destinatarios.add(u);
                }
            }
        }
        for (Usuario u : destinatarios) {
            notificaciones.crear(
                    u,
                    TipoNotificacion.INCIDENTE_DATOS,
                    "Incidente de datos personales",
                    "Incidente " + i.getCodigo() + " de severidad "
                            + i.getSeveridad().name().toLowerCase() + ". Plazo para notificar a la autoridad: "
                            + i.getNotificarAutoridadAntesDe() + ".",
                    "/privacidad/incidentes");
        }
    }

    private IncidenteDatos cargar(UUID id) {
        return incidentes
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incidente de datos", "id", id));
    }
}

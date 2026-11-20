package com.codesoftlabs.tusalud.app.services.odontologia;

import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.CambioPiezaRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.ProcedimientoRequest;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.CambioOdontogramaResponse;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.OdontogramaResponse;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.PiezaOdontograma;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.ProcedimientoResponse;
import com.codesoftlabs.tusalud.app.entities.OdontogramaEvento;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.ProcedimientoOdontologico;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoPieza;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.Superficie;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.OdontogramaEventoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.ProcedimientoOdontologicoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OdontogramaService {

    public static final String ACCION_CONSULTADO = "ODONTOGRAMA_CONSULTADO";
    public static final String ACCION_ACTUALIZADO = "ODONTOGRAMA_ACTUALIZADO";

    private final OdontogramaEventoRepository eventos;
    private final ProcedimientoOdontologicoRepository procedimientos;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarios;
    private final RelacionAsistencialService relacion;
    private final AuditoriaClinicaService auditoria;

    public List<ProcedimientoResponse> procedimientos(boolean soloActivos) {
        return (soloActivos
                        ? procedimientos.findByActivoTrueOrderByCategoriaAscNombreAsc()
                        : procedimientos.findAllByOrderByCategoriaAscNombreAsc())
                .stream().map(OdontogramaService::procedimiento).toList();
    }

    @Transactional
    public ProcedimientoResponse crearProcedimiento(ProcedimientoRequest request) {
        String codigo = request.codigo().strip().toUpperCase();
        if (procedimientos.findAllByOrderByCategoriaAscNombreAsc().stream()
                .anyMatch(p -> p.getCodigo().equals(codigo))) {
            throw new ConflictException("Ya existe un procedimiento con el código " + codigo);
        }
        ProcedimientoOdontologico nuevo =
                ProcedimientoOdontologico.builder().codigo(codigo).build();
        aplicar(nuevo, request);
        return procedimiento(procedimientos.save(nuevo));
    }

    @Transactional
    public ProcedimientoResponse actualizarProcedimiento(UUID id, ProcedimientoRequest request) {
        ProcedimientoOdontologico existente = procedimientos
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento odontológico", "id", id));
        aplicar(existente, request);
        return procedimiento(existente);
    }

    @Transactional
    public OdontogramaResponse obtener(UsuarioPrincipal actor, UUID pacienteId) {
        Paciente paciente = exigirLectura(actor, pacienteId);
        if (!esPaciente(actor)) {
            auditoria.registrar(actor, pacienteId, ACCION_CONSULTADO, "Odontograma");
        }
        return construir(paciente);
    }

    public OdontogramaResponse mio(UsuarioPrincipal actor) {
        return construir(propio(actor));
    }

    @Transactional
    public List<CambioOdontogramaResponse> historial(UsuarioPrincipal actor, UUID pacienteId, Integer pieza) {
        exigirLectura(actor, pacienteId);
        if (!esPaciente(actor)) {
            auditoria.registrar(actor, pacienteId, ACCION_CONSULTADO, "Historial del odontograma");
        }
        return eventos.historial(pacienteId).stream()
                .filter(e -> pieza == null || e.getPieza() == pieza)
                .map(OdontogramaService::cambio)
                .toList();
    }

    @Transactional
    public CambioOdontogramaResponse registrarCambio(
            UsuarioPrincipal actor, UUID pacienteId, CambioPiezaRequest request) {
        Paciente paciente = exigirEscritura(actor, pacienteId);
        OdontogramaEvento guardado = agregar(
                actor, paciente, request.pieza(), request.superficie(), request.estado(), request.observacion(), null);
        auditoria.registrar(
                actor,
                pacienteId,
                ACCION_ACTUALIZADO,
                "Pieza " + request.pieza() + " " + request.estado().name());
        return cambio(guardado);
    }

    @Transactional
    public OdontogramaEvento agregar(
            UsuarioPrincipal actor,
            Paciente paciente,
            int pieza,
            Superficie superficie,
            EstadoPieza estado,
            String observacion,
            UUID itemId) {
        if (!Dentadura.valida(pieza)) {
            throw new DatosInvalidosException("La pieza " + pieza + " no existe en la numeración FDI");
        }
        if (superficie != null && !estado.admiteSuperficie()) {
            throw new DatosInvalidosException("El estado " + estado.etiqueta() + " se registra en toda la pieza");
        }
        return eventos.save(OdontogramaEvento.builder()
                .paciente(paciente)
                .pieza((short) pieza)
                .superficie(superficie)
                .estado(estado)
                .observacion(observacion == null || observacion.isBlank() ? null : observacion.strip())
                .registradoPor(usuarios.getReferenceById(actor.getId()))
                .itemId(itemId)
                .build());
    }

    public Paciente exigirLectura(UsuarioPrincipal actor, UUID pacienteId) {
        if (esPaciente(actor)) {
            Paciente propio = propio(actor);
            if (!propio.getId().equals(pacienteId)) {
                throw new ForbiddenOperationException("Solo puede consultar su propio odontograma");
            }
            return propio;
        }
        return exigirEscritura(actor, pacienteId);
    }

    public Paciente exigirEscritura(UsuarioPrincipal actor, UUID pacienteId) {
        if (!actor.getRoles().contains(RolNombre.ROLE_MEDICO.name())) {
            throw new ForbiddenOperationException("Solo el personal médico gestiona el odontograma");
        }
        relacion.exigirRelacion(actor, pacienteId, "No tiene relación asistencial con este paciente");
        return pacienteRepository
                .findByIdWithUsuario(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", pacienteId));
    }

    public Paciente propio(UsuarioPrincipal actor) {
        return pacienteRepository
                .findByUsuarioId(actor.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Solo un paciente consulta sus propios datos"));
    }

    public boolean esPaciente(UsuarioPrincipal actor) {
        return actor.getRoles().contains(RolNombre.ROLE_PACIENTE.name());
    }

    private OdontogramaResponse construir(Paciente paciente) {
        List<OdontogramaEvento> historial = eventos.historial(paciente.getId());
        Map<Integer, List<OdontogramaEvento>> porPieza = new TreeMap<>();
        for (OdontogramaEvento e : historial) {
            porPieza.computeIfAbsent((int) e.getPieza(), k -> new ArrayList<>()).add(e);
        }
        List<PiezaOdontograma> piezas = new ArrayList<>();
        for (Map.Entry<Integer, List<OdontogramaEvento>> entrada : porPieza.entrySet()) {
            piezas.add(estadoDePieza(entrada.getKey(), entrada.getValue()));
        }
        Integer edad = paciente.getFechaNacimiento() == null
                ? null
                : Period.between(paciente.getFechaNacimiento(), LocalDate.now()).getYears();
        return new OdontogramaResponse(paciente.getId(), paciente.getUsuario().getNombreCompleto(), edad, piezas);
    }

    static PiezaOdontograma estadoDePieza(int pieza, List<OdontogramaEvento> orden) {
        OdontogramaEvento diente = null;
        for (OdontogramaEvento e : orden) {
            if (e.getSuperficie() == null) {
                diente = e;
            }
        }
        Map<Superficie, EstadoPieza> superficies = new EnumMap<>(Superficie.class);
        LocalDateTime desde = diente == null ? null : diente.getRegistradoEn();
        LocalDateTime ultimo = null;
        for (OdontogramaEvento e : orden) {
            if (ultimo == null || e.getRegistradoEn().isAfter(ultimo)) {
                ultimo = e.getRegistradoEn();
            }
            if (e.getSuperficie() != null
                    && (desde == null || !e.getRegistradoEn().isBefore(desde))) {
                superficies.put(e.getSuperficie(), e.getEstado());
            }
        }
        return new PiezaOdontograma(
                pieza,
                Dentadura.tipo(pieza),
                diente == null ? null : diente.getEstado(),
                diente == null ? null : diente.getObservacion(),
                superficies,
                ultimo);
    }

    private static void aplicar(ProcedimientoOdontologico p, ProcedimientoRequest r) {
        if (r.requiereSuperficie() && !r.requierePieza()) {
            throw new DatosInvalidosException("Un procedimiento por superficie también requiere indicar la pieza");
        }
        p.setNombre(r.nombre().strip());
        p.setCategoria(r.categoria().strip());
        p.setPrecio(r.precio());
        p.setRequierePieza(r.requierePieza());
        p.setRequiereSuperficie(r.requiereSuperficie());
        p.setEstadoResultante(r.estadoResultante());
        p.setRequiereConsentimiento(r.requiereConsentimiento());
        p.setActivo(r.activo());
    }

    private static ProcedimientoResponse procedimiento(ProcedimientoOdontologico p) {
        return new ProcedimientoResponse(
                p.getId(),
                p.getCodigo(),
                p.getNombre(),
                p.getCategoria(),
                p.getPrecio(),
                p.isRequierePieza(),
                p.isRequiereSuperficie(),
                p.getEstadoResultante(),
                p.isRequiereConsentimiento(),
                p.isActivo());
    }

    private static CambioOdontogramaResponse cambio(OdontogramaEvento e) {
        return new CambioOdontogramaResponse(
                e.getId(),
                e.getPieza(),
                e.getSuperficie(),
                e.getEstado(),
                e.getObservacion(),
                e.getRegistradoPor().getNombreCompleto(),
                e.getRegistradoEn(),
                e.getItemId());
    }
}

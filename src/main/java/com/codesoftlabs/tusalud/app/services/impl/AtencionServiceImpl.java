package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaItemRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.RecetaItem;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtencionServiceImpl implements AtencionService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CitaRepository citaRepository;
    private final AtencionRepository atencionRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public AtencionResponse registrar(UUID citaId, AtencionRequest request, UsuarioPrincipal medico) {
        Cita cita = buscarCita(citaId);
        if (!esMedicoDe(cita, medico)) {
            throw new ForbiddenOperationException("Solo el médico de la cita puede registrar su atención");
        }
        if (atencionRepository.existsByCitaId(citaId)) {
            throw new ConflictException("La cita ya tiene una atención registrada");
        }
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new BusinessLogicException("No se puede registrar la atención de una cita CANCELADA");
        }

        Atencion atencion = Atencion.builder()
                .cita(cita)
                .paciente(cita.getPaciente())
                .medico(cita.getMedico())
                .fecha(LocalDateTime.now())
                .build();
        aplicar(atencion, request);
        atencion = atencionRepository.save(atencion);

        if (cita.getEstado() != EstadoCita.ATENDIDA) {
            cita.setEstado(EstadoCita.ATENDIDA);
        }
        notificacionService.crear(
                cita.getPaciente().getUsuario(),
                TipoNotificacion.CITA_ATENDIDA,
                "Atención registrada",
                "Se registró la atención de su cita del " + cita.getFechaHora().format(FORMATO_FECHA) + " con "
                        + cita.getMedico().getUsuario().getNombreCompleto() + ".",
                "/citas/" + cita.getId());
        log.info("Atención {} registrada para la cita {}", atencion.getId(), citaId);
        return AtencionResponse.from(atencion);
    }

    @Override
    @Transactional
    public AtencionResponse actualizar(UUID citaId, AtencionRequest request, UsuarioPrincipal medico) {
        Cita cita = buscarCita(citaId);
        if (!esMedicoDe(cita, medico)) {
            throw new ForbiddenOperationException("Solo el médico de la cita puede editar su atención");
        }
        Atencion atencion = buscarAtencion(citaId);
        aplicar(atencion, request);
        return AtencionResponse.from(atencion);
    }

    @Override
    public AtencionResponse obtener(UUID citaId, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        boolean admin = actor.getRoles().contains(RolNombre.ROLE_ADMIN.name());
        boolean paciente = cita.getPaciente().getUsuario().getId().equals(actor.getId());
        if (!admin && !paciente && !esMedicoDe(cita, actor)) {
            throw new ForbiddenOperationException("No tiene permiso para ver la atención de esta cita");
        }
        return AtencionResponse.from(buscarAtencion(citaId));
    }

    private Cita buscarCita(UUID citaId) {
        return citaRepository
                .findByIdWithDetails(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", citaId));
    }

    private Atencion buscarAtencion(UUID citaId) {
        return atencionRepository
                .findByCitaIdConDetalle(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Atención", "cita", citaId));
    }

    private static boolean esMedicoDe(Cita cita, UsuarioPrincipal actor) {
        return cita.getMedico().getUsuario().getId().equals(actor.getId());
    }

    private static void aplicar(Atencion atencion, AtencionRequest request) {
        atencion.setDiagnostico(request.diagnostico().trim());
        atencion.setTratamiento(vacioANulo(request.tratamiento()));
        atencion.setIndicaciones(vacioANulo(request.indicaciones()));
        atencion.setObservaciones(vacioANulo(request.observaciones()));
        atencion.setPesoKg(request.pesoKg());
        atencion.setTallaCm(request.tallaCm());
        atencion.setPresionArterial(vacioANulo(request.presionArterial()));
        atencion.setTemperaturaC(request.temperaturaC());
        atencion.setFrecuenciaCardiaca(request.frecuenciaCardiaca());
        List<RecetaItem> receta = new ArrayList<>();
        if (request.receta() != null) {
            for (RecetaItemRequest item : request.receta()) {
                receta.add(RecetaItem.builder()
                        .medicamento(item.medicamento().trim())
                        .dosis(vacioANulo(item.dosis()))
                        .frecuencia(vacioANulo(item.frecuencia()))
                        .duracion(vacioANulo(item.duracion()))
                        .build());
            }
        }
        atencion.setReceta(receta);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}

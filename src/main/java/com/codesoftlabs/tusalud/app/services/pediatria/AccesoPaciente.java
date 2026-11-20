package com.codesoftlabs.tusalud.app.services.pediatria;

import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.PacienteClinicoResumen;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AccesoClinicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccesoPaciente {

    private final PacienteRepository pacienteRepository;
    private final AccesoClinicoRepository acceso;
    private final RelacionAsistencialService relacion;

    @Value("${app.pediatria.ventana-enfermeria-horas:24}")
    private int ventanaEnfermeriaHoras;

    public boolean esMedico(UsuarioPrincipal actor) {
        return actor.getRoles().contains(RolNombre.ROLE_MEDICO.name());
    }

    public boolean esEnfermeria(UsuarioPrincipal actor) {
        return actor.getRoles().contains(RolNombre.ROLE_ENFERMERIA.name());
    }

    public boolean esPaciente(UsuarioPrincipal actor) {
        return actor.getRoles().contains(RolNombre.ROLE_PACIENTE.name());
    }

    public Paciente paciente(UUID id) {
        return pacienteRepository
                .findByIdWithUsuario(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", id));
    }

    public Paciente propio(UsuarioPrincipal actor) {
        return pacienteRepository
                .findByUsuarioId(actor.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Solo un paciente consulta sus propios datos"));
    }

    public Paciente exigirLectura(UsuarioPrincipal actor, UUID pacienteId) {
        if (esPaciente(actor)) {
            Paciente propio = propio(actor);
            if (!propio.getId().equals(pacienteId)) {
                throw new ForbiddenOperationException("Solo puede consultar sus propios datos");
            }
            return paciente(pacienteId);
        }
        return exigirEscritura(actor, pacienteId);
    }

    public Paciente exigirEscritura(UsuarioPrincipal actor, UUID pacienteId) {
        if (esMedico(actor)) {
            relacion.exigirRelacion(actor, pacienteId, "No tiene relación asistencial con este paciente");
        } else if (esEnfermeria(actor)) {
            LocalDateTime ahora = LocalDateTime.now();
            boolean enAtencion = acceso.tieneCitaEntre(
                            pacienteId,
                            ahora.minusHours(ventanaEnfermeriaHoras),
                            ahora.plusHours(ventanaEnfermeriaHoras))
                    || acceso.tieneIngresoActivo(pacienteId);
            if (!enAtencion) {
                throw new ForbiddenOperationException("El paciente no tiene una atención ni un ingreso vigentes");
            }
        } else {
            throw new ForbiddenOperationException("No tiene permiso para acceder a este paciente");
        }
        return paciente(pacienteId);
    }

    public List<PacienteClinicoResumen> listar(UsuarioPrincipal actor, String texto) {
        String q = "%" + (texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT)) + "%";
        List<Paciente> pacientes;
        if (esMedico(actor)) {
            pacientes = acceso.pacientesDelMedico(actor.getId(), q);
        } else if (esEnfermeria(actor)) {
            LocalDateTime ahora = LocalDateTime.now();
            pacientes = acceso.pacientesEnAtencion(
                    ahora.minusHours(ventanaEnfermeriaHoras), ahora.plusHours(ventanaEnfermeriaHoras), q);
        } else {
            throw new ForbiddenOperationException("No tiene permiso para listar pacientes");
        }
        LocalDate hoy = LocalDate.now();
        return pacientes.stream().map(p -> resumen(p, hoy)).toList();
    }

    public static PacienteClinicoResumen resumen(Paciente p, LocalDate hoy) {
        Integer meses = p.getFechaNacimiento() == null
                ? null
                : CalculadoraEsquemaVacunacion.edadEnMeses(p.getFechaNacimiento(), hoy);
        return new PacienteClinicoResumen(
                p.getId(),
                p.getUsuario().getNombreCompleto(),
                p.getDni(),
                p.getFechaNacimiento(),
                meses,
                p.getSexo() == null ? null : p.getSexo().name());
    }
}

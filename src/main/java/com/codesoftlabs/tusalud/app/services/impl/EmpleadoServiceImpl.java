package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.EmpleadoRequest;
import com.codesoftlabs.tusalud.app.dto.response.EmpleadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RrhhResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.UsuarioDisponibleResponse;
import com.codesoftlabs.tusalud.app.entities.Ausencia;
import com.codesoftlabs.tusalud.app.entities.Empleado;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoContrato;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AusenciaRepository;
import com.codesoftlabs.tusalud.app.repositories.EmpleadoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaRrhhService;
import com.codesoftlabs.tusalud.app.services.interfaces.EmpleadoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmpleadoServiceImpl implements EmpleadoService {

    private static final int DIAS_AVISO_CONTRATO = 60;

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AusenciaRepository ausenciaRepository;
    private final AuditoriaRrhhService auditoria;

    @Override
    public List<EmpleadoResponse> listar(String q, AreaLaboral area, EstadoEmpleado estado, UsuarioPrincipal actor) {
        String termino = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return empleadoRepository.buscar(termino, area, estado).stream()
                .map(e -> responder(e, actor))
                .toList();
    }

    @Override
    public EmpleadoResponse obtener(UUID id, UsuarioPrincipal actor) {
        return responder(buscar(id), actor);
    }

    @Override
    @Transactional
    public EmpleadoResponse crear(EmpleadoRequest request, UsuarioPrincipal actor) {
        if (request.usuarioId() == null) {
            throw new DatosInvalidosException("Debe indicar el usuario del personal");
        }
        validarContrato(request);
        Usuario usuario = usuarioRepository
                .findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.usuarioId()));
        boolean soloPaciente = usuario.getRoles().stream().allMatch(r -> r.getNombre() == RolNombre.ROLE_PACIENTE);
        if (soloPaciente) {
            throw new DatosInvalidosException("Un paciente no puede tener una ficha laboral");
        }
        if (empleadoRepository.findByUsuarioId(usuario.getId()).isPresent()) {
            throw new ConflictException("El usuario ya tiene una ficha laboral");
        }
        Empleado empleado = empleadoRepository.save(Empleado.builder()
                .usuario(usuario)
                .cargo(request.cargo().trim())
                .area(request.area())
                .tipoContrato(request.tipoContrato())
                .fechaIngreso(request.fechaIngreso())
                .fechaFinContrato(request.fechaFinContrato())
                .estado(request.estado() == null ? EstadoEmpleado.ACTIVO : request.estado())
                .sueldo(request.sueldo())
                .contactoEmergenciaNombre(limpiar(request.contactoEmergenciaNombre()))
                .contactoEmergenciaTelefono(limpiar(request.contactoEmergenciaTelefono()))
                .diasVacacionesAnuales(request.diasVacacionesAnuales() == null ? 30 : request.diasVacacionesAnuales())
                .build());
        auditoria.registrar(
                actor,
                empleado.getId(),
                AuditoriaRrhhService.EMPLEADO_CREADO,
                "Ficha creada con cargo " + empleado.getCargo() + " en " + empleado.getArea());
        log.info("Ficha laboral {} creada por {}", empleado.getId(), actor.getId());
        return responder(empleado, actor);
    }

    @Override
    @Transactional
    public EmpleadoResponse actualizar(UUID id, EmpleadoRequest request, UsuarioPrincipal actor) {
        Empleado empleado = buscar(id);
        validarContrato(request);
        List<String> cambios = new ArrayList<>();
        if (!empleado.getCargo().equals(request.cargo().trim())) {
            cambios.add("cargo");
        }
        if (empleado.getArea() != request.area()) {
            cambios.add("área");
        }
        if (empleado.getTipoContrato() != request.tipoContrato()) {
            cambios.add("contrato");
        }
        if (empleado.getSueldo().compareTo(request.sueldo()) != 0) {
            cambios.add("sueldo");
        }
        EstadoEmpleado estado = request.estado() == null ? empleado.getEstado() : request.estado();
        if (empleado.getEstado() != estado) {
            cambios.add("estado " + empleado.getEstado() + " a " + estado);
        }
        empleado.setCargo(request.cargo().trim());
        empleado.setArea(request.area());
        empleado.setTipoContrato(request.tipoContrato());
        empleado.setFechaIngreso(request.fechaIngreso());
        empleado.setFechaFinContrato(request.fechaFinContrato());
        empleado.setEstado(estado);
        empleado.setSueldo(request.sueldo());
        empleado.setContactoEmergenciaNombre(limpiar(request.contactoEmergenciaNombre()));
        empleado.setContactoEmergenciaTelefono(limpiar(request.contactoEmergenciaTelefono()));
        if (request.diasVacacionesAnuales() != null) {
            empleado.setDiasVacacionesAnuales(request.diasVacacionesAnuales());
        }
        empleadoRepository.saveAndFlush(empleado);
        auditoria.registrar(
                actor,
                empleado.getId(),
                AuditoriaRrhhService.EMPLEADO_ACTUALIZADO,
                cambios.isEmpty() ? "Sin cambios relevantes" : "Cambios en " + String.join(", ", cambios));
        log.info("Ficha laboral {} actualizada por {}", empleado.getId(), actor.getId());
        return responder(empleado, actor);
    }

    @Override
    public EmpleadoResponse miFicha(UsuarioPrincipal actor) {
        Empleado empleado = empleadoRepository
                .findByUsuarioId(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No tienes una ficha laboral registrada"));
        return EmpleadoResponse.from(empleado, nombresRoles(empleado.getUsuario()), false, true);
    }

    @Override
    public List<UsuarioDisponibleResponse> usuariosDisponibles() {
        return empleadoRepository.usuariosSinFicha().stream()
                .map(u ->
                        new UsuarioDisponibleResponse(u.getId(), u.getNombreCompleto(), u.getEmail(), nombresRoles(u)))
                .toList();
    }

    @Override
    public RrhhResumenResponse resumen(UsuarioPrincipal actor) {
        long activos = 0;
        long licencia = 0;
        long cesados = 0;
        for (Object[] fila : empleadoRepository.contarPorEstado()) {
            long cantidad = ((Number) fila[1]).longValue();
            switch ((EstadoEmpleado) fila[0]) {
                case ACTIVO -> activos = cantidad;
                case LICENCIA -> licencia = cantidad;
                case CESADO -> cesados = cantidad;
            }
        }
        LocalDate hoy = LocalDate.now();
        List<RrhhResumenResponse.PorArea> porArea = empleadoRepository.contarPorArea().stream()
                .map(f -> new RrhhResumenResponse.PorArea((AreaLaboral) f[0], ((Number) f[1]).longValue()))
                .toList();
        List<RrhhResumenResponse.ContratoPorVencer> contratos =
                empleadoRepository.contratosPorVencer(hoy, hoy.plusDays(DIAS_AVISO_CONTRATO)).stream()
                        .map(e -> new RrhhResumenResponse.ContratoPorVencer(
                                e.getId(),
                                e.getUsuario().getNombreCompleto(),
                                e.getCargo(),
                                e.getFechaFinContrato(),
                                ChronoUnit.DAYS.between(hoy, e.getFechaFinContrato())))
                        .toList();
        List<Ausencia> ausentes = ausenciaRepository.aprobadasEn(hoy);
        boolean detalle = tieneRol(actor, RolNombre.ROLE_RRHH) || tieneRol(actor, RolNombre.ROLE_ADMIN);
        boolean verSueldo = tieneRol(actor, RolNombre.ROLE_RRHH) || tieneRol(actor, RolNombre.ROLE_GERENCIA);
        BigDecimal masa = verSueldo ? empleadoRepository.masaSalarial() : null;
        return new RrhhResumenResponse(
                activos + licencia + cesados,
                activos,
                licencia,
                cesados,
                ausenciaRepository.countByEstado(EstadoAusencia.SOLICITADA),
                masa,
                porArea,
                contratos,
                ausentes.size(),
                detalle
                        ? ausentes.stream()
                                .map(a -> new RrhhResumenResponse.AusenteHoy(
                                        a.getEmpleado().getId(),
                                        a.getEmpleado().getUsuario().getNombreCompleto(),
                                        a.getEmpleado().getArea(),
                                        a.getTipo(),
                                        a.getFechaFin()))
                                .toList()
                        : null);
    }

    private Empleado buscar(UUID id) {
        return empleadoRepository
                .findByIdConUsuario(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", id));
    }

    private EmpleadoResponse responder(Empleado empleado, UsuarioPrincipal actor) {
        boolean verSueldo = tieneRol(actor, RolNombre.ROLE_RRHH) || tieneRol(actor, RolNombre.ROLE_GERENCIA);
        boolean verContacto = tieneRol(actor, RolNombre.ROLE_RRHH) || tieneRol(actor, RolNombre.ROLE_GERENCIA);
        return EmpleadoResponse.from(empleado, nombresRoles(empleado.getUsuario()), verSueldo, verContacto);
    }

    private static List<String> nombresRoles(Usuario usuario) {
        return usuario.getRoles().stream()
                .map(r -> r.getNombre().name())
                .sorted()
                .toList();
    }

    private static void validarContrato(EmpleadoRequest request) {
        if (request.fechaFinContrato() != null && request.fechaFinContrato().isBefore(request.fechaIngreso())) {
            throw new DatosInvalidosException("La fecha de fin del contrato no puede ser anterior a la de ingreso");
        }
        if (request.tipoContrato() == TipoContrato.INDEFINIDO && request.fechaFinContrato() != null) {
            throw new DatosInvalidosException("Un contrato indefinido no tiene fecha de fin");
        }
        if (request.tipoContrato() != TipoContrato.INDEFINIDO && request.fechaFinContrato() == null) {
            throw new DatosInvalidosException("Este tipo de contrato requiere una fecha de fin");
        }
        if (request.fechaIngreso().isAfter(LocalDate.now().plusDays(90))) {
            throw new DatosInvalidosException("La fecha de ingreso no puede superar los 90 días a futuro");
        }
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static boolean tieneRol(UsuarioPrincipal actor, RolNombre rol) {
        return actor.getRoles().contains(rol.name());
    }
}

package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.CancelarOrdenLabRequest;
import com.codesoftlabs.tusalud.app.dto.request.ExamenLabRequest;
import com.codesoftlabs.tusalud.app.dto.request.OrdenLabCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ResultadosLabRequest;
import com.codesoftlabs.tusalud.app.dto.response.ExamenLabResponse;
import com.codesoftlabs.tusalud.app.dto.response.LabResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenLabResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.ExamenLab;
import com.codesoftlabs.tusalud.app.entities.ItemOrdenLab;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.OrdenLaboratorio;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.ParametroExamen;
import com.codesoftlabs.tusalud.app.entities.ResultadoLab;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaExamen;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoItemLab;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoParametro;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.ExamenLabRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.OrdenLaboratorioRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.LaboratorioService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
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
public class LaboratorioServiceImpl implements LaboratorioService {

    private static final int LIMITE_ORDENES = 200;
    private static final int VENTANA_CONSULTA = 1000;
    private static final Set<EstadoOrdenLab> ESTADOS_PENDIENTES =
            EnumSet.of(EstadoOrdenLab.SOLICITADA, EstadoOrdenLab.MUESTRA_TOMADA, EstadoOrdenLab.EN_PROCESO);

    private final ExamenLabRepository examenRepository;
    private final OrdenLaboratorioRepository ordenRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final AtencionRepository atencionRepository;
    private final RelacionAsistencialService relacionAsistencial;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional(readOnly = true)
    public List<ExamenLabResponse> listarExamenes(String busqueda, CategoriaExamen categoria, Boolean activo) {
        String termino = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        return examenRepository.findAllConParametros().stream()
                .filter(examen -> categoria == null || examen.getCategoria() == categoria)
                .filter(examen -> activo == null || examen.isActivo() == activo)
                .filter(examen -> termino.isEmpty()
                        || examen.getNombre().toLowerCase(Locale.ROOT).contains(termino)
                        || examen.getCodigo().toLowerCase(Locale.ROOT).contains(termino))
                .map(ExamenLabResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamenLabResponse obtenerExamen(UUID id) {
        return ExamenLabResponse.from(buscarExamen(id));
    }

    @Override
    public ExamenLabResponse crearExamen(ExamenLabRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        if (examenRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe un examen con el código " + codigo);
        }
        ExamenLab examen = ExamenLab.builder().build();
        aplicar(examen, request, codigo);
        return ExamenLabResponse.from(examenRepository.save(examen));
    }

    @Override
    public ExamenLabResponse actualizarExamen(UUID id, ExamenLabRequest request) {
        ExamenLab examen = buscarExamen(id);
        String codigo = normalizarCodigo(request.codigo());
        examenRepository
                .findByCodigoIgnoreCase(codigo)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ConflictException("Ya existe un examen con el código " + codigo);
                });
        aplicar(examen, request, codigo);
        examenRepository.flush();
        return ExamenLabResponse.from(examen);
    }

    @Override
    public OrdenLabResponse crearOrden(UsuarioPrincipal actor, OrdenLabCreateRequest request) {
        Medico medico = medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ForbiddenOperationException("Solo los médicos pueden solicitar exámenes"));
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.pacienteId()));
        relacionAsistencial.exigirRelacion(
                actor,
                paciente.getId(),
                "Solo puede solicitar exámenes para pacientes que atiende o que otro médico compartió con usted");
        Atencion atencion = resolverAtencion(request.atencionId(), paciente, medico);
        List<ExamenLab> examenes = resolverExamenes(request.examenIds());
        BigDecimal total = examenes.stream().map(ExamenLab::getPrecio).reduce(BigDecimal.ZERO, BigDecimal::add);

        OrdenLaboratorio nueva = OrdenLaboratorio.builder()
                .codigo("PEND-" + System.nanoTime())
                .fecha(LocalDateTime.now())
                .estado(EstadoOrdenLab.SOLICITADA)
                .prioridad(request.prioridad())
                .paciente(paciente)
                .medico(medico)
                .atencion(atencion)
                .indicaciones(vacioANulo(request.indicaciones()))
                .total(total)
                .build();
        examenes.forEach(examen -> nueva.agregarItem(ItemOrdenLab.builder()
                .examen(examen)
                .estado(EstadoItemLab.PENDIENTE)
                .build()));
        OrdenLaboratorio orden = ordenRepository.saveAndFlush(nueva);
        orden.setCodigo(String.format("LAB-%06d", ordenRepository.count()));

        notificarSolicitud(orden, paciente);
        return OrdenLabResponse.from(orden, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenLabResponse> listarOrdenes(
            UsuarioPrincipal actor, EstadoOrdenLab estado, String busqueda, PrioridadOrden prioridad) {
        boolean administracion = !tieneRol(actor, RolNombre.ROLE_LABORATORIO) && tieneRol(actor, RolNombre.ROLE_ADMIN);
        boolean personal = tieneRol(actor, RolNombre.ROLE_LABORATORIO) || tieneRol(actor, RolNombre.ROLE_ADMIN);
        List<OrdenLaboratorio> base = personal
                ? ordenRepository.findRecientes(PageRequest.of(0, VENTANA_CONSULTA))
                : ordenRepository.findDelSolicitante(actor.getId(), PageRequest.of(0, VENTANA_CONSULTA));
        String termino = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        return base.stream()
                .filter(orden -> estado == null || orden.getEstado() == estado)
                .filter(orden -> prioridad == null || orden.getPrioridad() == prioridad)
                .filter(orden -> termino.isEmpty() || coincide(orden, termino))
                .sorted(Comparator.comparingInt(LaboratorioServiceImpl::rango)
                        .thenComparing(OrdenLaboratorio::getFecha, Comparator.reverseOrder()))
                .limit(LIMITE_ORDENES)
                .map(orden ->
                        administracion ? OrdenLabResponse.deAdministracion(orden) : OrdenLabResponse.from(orden, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenLabResponse obtenerOrden(UsuarioPrincipal actor, UUID id) {
        OrdenLaboratorio orden = buscarOrden(id);
        boolean personal = tieneRol(actor, RolNombre.ROLE_LABORATORIO);
        boolean solicitante = orden.getMedico().getUsuario().getId().equals(actor.getId());
        boolean paciente = orden.getPaciente().getUsuario().getId().equals(actor.getId());
        if (personal || solicitante) {
            return OrdenLabResponse.from(orden, true);
        }
        if (paciente && orden.getEstado() == EstadoOrdenLab.COMPLETADA) {
            return OrdenLabResponse.from(orden, true);
        }
        throw new ForbiddenOperationException("No tiene acceso a esta orden de laboratorio");
    }

    @Override
    public OrdenLabResponse tomarMuestra(UsuarioPrincipal actor, UUID ordenId) {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        if (orden.getEstado() != EstadoOrdenLab.SOLICITADA) {
            throw new ConflictException("Solo se puede tomar la muestra de una orden solicitada");
        }
        orden.setEstado(EstadoOrdenLab.MUESTRA_TOMADA);
        orden.setTomaMuestraEn(LocalDateTime.now());
        return OrdenLabResponse.from(ordenRepository.save(orden), true);
    }

    @Override
    public OrdenLabResponse registrarResultados(
            UsuarioPrincipal actor, UUID ordenId, UUID itemId, ResultadosLabRequest request) {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        if (orden.getEstado() != EstadoOrdenLab.MUESTRA_TOMADA && orden.getEstado() != EstadoOrdenLab.EN_PROCESO) {
            throw new ConflictException(
                    "Los resultados se registran con la muestra tomada y antes de validar la orden");
        }
        ItemOrdenLab item = orden.getItems().stream()
                .filter(candidato -> candidato.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Examen de la orden", "id", itemId));
        if (item.getEstado() == EstadoItemLab.VALIDADO) {
            throw new ConflictException("El examen ya fue validado");
        }
        Map<UUID, ParametroExamen> parametros = item.getExamen().getParametros().stream()
                .collect(Collectors.toMap(ParametroExamen::getId, Function.identity()));
        Set<UUID> vistos = new HashSet<>();
        List<ResultadoLab> resultados = new ArrayList<>();
        for (ResultadosLabRequest.ResultadoValor valor : request.resultados()) {
            ParametroExamen parametro = parametros.get(valor.parametroId());
            if (parametro == null) {
                throw new DatosInvalidosException("El parámetro " + valor.parametroId() + " no pertenece al examen "
                        + item.getExamen().getNombre());
            }
            if (!vistos.add(valor.parametroId())) {
                throw new DatosInvalidosException("El parámetro " + parametro.getNombre() + " está repetido");
            }
            resultados.add(construirResultado(parametro, valor.valor().trim()));
        }
        item.reemplazarResultados(resultados);
        item.setObservaciones(vacioANulo(request.observaciones()));
        item.setFechaResultado(LocalDateTime.now());
        item.setEstado(EstadoItemLab.CON_RESULTADO);
        orden.setEstado(EstadoOrdenLab.EN_PROCESO);
        return OrdenLabResponse.from(ordenRepository.save(orden), true);
    }

    @Override
    public OrdenLabResponse validar(UsuarioPrincipal actor, UUID ordenId) {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        if (orden.getEstado() != EstadoOrdenLab.EN_PROCESO) {
            throw new ConflictException("Solo se puede validar una orden con resultados en proceso");
        }
        boolean completa = orden.getItems().stream().allMatch(item -> item.getEstado() == EstadoItemLab.CON_RESULTADO);
        if (!completa) {
            throw new ConflictException("Faltan resultados de uno o más exámenes de la orden");
        }
        Usuario validador = usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
        orden.getItems().forEach(item -> {
            item.setEstado(EstadoItemLab.VALIDADO);
            item.setValidadoPor(validador);
        });
        orden.setEstado(EstadoOrdenLab.COMPLETADA);
        orden.setCompletadaEn(LocalDateTime.now());
        orden = ordenRepository.save(orden);
        notificarResultado(orden);
        return OrdenLabResponse.from(orden, true);
    }

    @Override
    public OrdenLabResponse cancelar(UsuarioPrincipal actor, UUID ordenId, CancelarOrdenLabRequest request) {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        if (orden.getEstado() == EstadoOrdenLab.COMPLETADA || orden.getEstado() == EstadoOrdenLab.CANCELADA) {
            throw new ConflictException("La orden ya no se puede cancelar");
        }
        boolean personal = tieneRol(actor, RolNombre.ROLE_LABORATORIO) || tieneRol(actor, RolNombre.ROLE_ADMIN);
        boolean solicitante = orden.getMedico().getUsuario().getId().equals(actor.getId());
        if (!personal) {
            if (!solicitante) {
                throw new ForbiddenOperationException("Solo el médico solicitante puede cancelar esta orden");
            }
            if (orden.getEstado() != EstadoOrdenLab.SOLICITADA) {
                throw new ForbiddenOperationException(
                        "El médico solo puede cancelar la orden mientras no se haya tomado la muestra");
            }
        }
        orden.setEstado(EstadoOrdenLab.CANCELADA);
        orden.setMotivoCancelacion(request == null ? null : vacioANulo(request.motivo()));
        return OrdenLabResponse.from(ordenRepository.save(orden), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenLabResponse> misOrdenes(UsuarioPrincipal actor) {
        return ordenRepository.findDelPaciente(actor.getId()).stream()
                .map(orden -> OrdenLabResponse.from(orden, orden.getEstado() == EstadoOrdenLab.COMPLETADA))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LabResumenResponse resumen() {
        LocalDate hoy = LocalDate.now();
        return new LabResumenResponse(
                ordenRepository.countByEstado(EstadoOrdenLab.SOLICITADA),
                ordenRepository.countByEstado(EstadoOrdenLab.MUESTRA_TOMADA)
                        + ordenRepository.countByEstado(EstadoOrdenLab.EN_PROCESO),
                ordenRepository.contarCompletadasEntre(
                        EstadoOrdenLab.COMPLETADA,
                        hoy.atStartOfDay(),
                        hoy.plusDays(1).atStartOfDay()),
                ordenRepository.contarUrgentesPendientes());
    }

    private void aplicar(ExamenLab examen, ExamenLabRequest request, String codigo) {
        List<ExamenLabRequest.ParametroRequest> entrantes =
                request.parametros() == null ? List.of() : request.parametros();
        List<ParametroExamen> parametros = new ArrayList<>();
        for (int i = 0; i < entrantes.size(); i++) {
            ExamenLabRequest.ParametroRequest entrante = entrantes.get(i);
            boolean numerico = entrante.tipo() == TipoParametro.NUMERICO;
            if (numerico
                    && entrante.valorMin() != null
                    && entrante.valorMax() != null
                    && entrante.valorMin().compareTo(entrante.valorMax()) > 0) {
                throw new DatosInvalidosException(
                        "El valor mínimo de " + entrante.nombre() + " no puede superar al máximo");
            }
            parametros.add(ParametroExamen.builder()
                    .nombre(entrante.nombre().trim())
                    .unidad(vacioANulo(entrante.unidad()))
                    .tipo(entrante.tipo())
                    .valorMin(numerico ? entrante.valorMin() : null)
                    .valorMax(numerico ? entrante.valorMax() : null)
                    .orden(entrante.orden() == null ? i + 1 : entrante.orden())
                    .build());
        }
        examen.setCodigo(codigo);
        examen.setNombre(request.nombre().trim());
        examen.setCategoria(request.categoria());
        examen.setMuestra(request.muestra());
        examen.setPrecio(request.precio());
        examen.setTiempoEntregaHoras(request.tiempoEntregaHoras());
        examen.setIndicaciones(vacioANulo(request.indicaciones()));
        examen.setActivo(request.activo() == null || request.activo());
        examen.reemplazarParametros(parametros);
    }

    private ResultadoLab construirResultado(ParametroExamen parametro, String valor) {
        boolean fuera = false;
        if (parametro.getTipo() == TipoParametro.NUMERICO) {
            BigDecimal numero;
            try {
                numero = new BigDecimal(valor.replace(',', '.'));
            } catch (NumberFormatException ex) {
                throw new DatosInvalidosException("El valor de " + parametro.getNombre() + " debe ser numérico");
            }
            fuera = (parametro.getValorMin() != null && numero.compareTo(parametro.getValorMin()) < 0)
                    || (parametro.getValorMax() != null && numero.compareTo(parametro.getValorMax()) > 0);
        }
        return ResultadoLab.builder()
                .parametroId(parametro.getId())
                .parametroNombre(parametro.getNombre())
                .unidad(parametro.getUnidad())
                .valor(valor)
                .valorMin(parametro.getValorMin())
                .valorMax(parametro.getValorMax())
                .fueraDeRango(fuera)
                .build();
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

    private List<ExamenLab> resolverExamenes(List<UUID> ids) {
        Set<UUID> unicos = new LinkedHashSet<>(ids);
        Map<UUID, ExamenLab> encontrados = examenRepository.findAllByIdConParametros(unicos).stream()
                .collect(Collectors.toMap(ExamenLab::getId, Function.identity()));
        List<ExamenLab> examenes = new ArrayList<>();
        for (UUID id : unicos) {
            ExamenLab examen = encontrados.get(id);
            if (examen == null) {
                throw new ResourceNotFoundException("Examen", "id", id);
            }
            if (!examen.isActivo()) {
                throw new ConflictException("El examen " + examen.getNombre() + " no está disponible");
            }
            examenes.add(examen);
        }
        return examenes;
    }

    private void notificarSolicitud(OrdenLaboratorio orden, Paciente paciente) {
        notificacionService.crear(
                paciente.getUsuario(),
                TipoNotificacion.ORDEN_LABORATORIO,
                "Nueva orden de laboratorio",
                "El Dr. " + orden.getMedico().getUsuario().getNombreCompleto() + " solicitó "
                        + orden.getItems().size()
                        + (orden.getItems().size() == 1 ? " examen" : " exámenes") + " con el código "
                        + orden.getCodigo()
                        + ".",
                "/mis-resultados");
        for (Usuario tecnico : ordenRepository.findUsuariosActivosPorRol(RolNombre.ROLE_LABORATORIO)) {
            notificacionService.crear(
                    tecnico,
                    TipoNotificacion.ORDEN_LABORATORIO,
                    orden.getPrioridad() == PrioridadOrden.URGENTE
                            ? "Orden urgente de laboratorio"
                            : "Nueva orden de laboratorio",
                    "La orden " + orden.getCodigo() + " de "
                            + paciente.getUsuario().getNombreCompleto() + " espera la toma de muestra.",
                    "/laboratorio/ordenes/" + orden.getId());
        }
    }

    private void notificarResultado(OrdenLaboratorio orden) {
        notificacionService.crear(
                orden.getPaciente().getUsuario(),
                TipoNotificacion.RESULTADO_LABORATORIO,
                "Tus resultados de laboratorio están listos",
                "La orden " + orden.getCodigo() + " fue completada. Ya puedes revisar tus resultados.",
                "/mis-resultados");
        notificacionService.crear(
                orden.getMedico().getUsuario(),
                TipoNotificacion.RESULTADO_LABORATORIO,
                "Resultados de laboratorio disponibles",
                "La orden " + orden.getCodigo() + " de "
                        + orden.getPaciente().getUsuario().getNombreCompleto() + " fue completada.",
                "/laboratorio/ordenes/" + orden.getId());
    }

    private OrdenLaboratorio buscarOrden(UUID id) {
        return ordenRepository
                .findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de laboratorio", "id", id));
    }

    private ExamenLab buscarExamen(UUID id) {
        return examenRepository
                .findByIdConParametros(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", "id", id));
    }

    private static boolean coincide(OrdenLaboratorio orden, String termino) {
        return orden.getCodigo().toLowerCase(Locale.ROOT).contains(termino)
                || orden.getPaciente().getDni().contains(termino)
                || orden.getPaciente()
                        .getUsuario()
                        .getNombreCompleto()
                        .toLowerCase(Locale.ROOT)
                        .contains(termino);
    }

    private static int rango(OrdenLaboratorio orden) {
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
}

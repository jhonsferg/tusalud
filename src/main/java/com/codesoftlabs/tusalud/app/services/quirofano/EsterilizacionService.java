package com.codesoftlabs.tusalud.app.services.quirofano;

import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.CicloRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.EsterilizadorRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.IndicadoresRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.SetDelCicloRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.SetInstrumentalRequest;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.AlertaEsterilizacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.CicloResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.EsterilizadorResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.LoteEsterilResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.SetInstrumentalResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.TrazabilidadLoteResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.UsoLoteResponse;
import com.codesoftlabs.tusalud.app.entities.CicloEsterilizacion;
import com.codesoftlabs.tusalud.app.entities.CirugiaSet;
import com.codesoftlabs.tusalud.app.entities.Esterilizador;
import com.codesoftlabs.tusalud.app.entities.LoteEsteril;
import com.codesoftlabs.tusalud.app.entities.SetInstrumental;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoEfectivoLote;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoLoteEsteril;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoCiclo;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoIndicador;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CicloEsterilizacionRepository;
import com.codesoftlabs.tusalud.app.repositories.CirugiaSetRepository;
import com.codesoftlabs.tusalud.app.repositories.EsterilizadorRepository;
import com.codesoftlabs.tusalud.app.repositories.LoteEsterilRepository;
import com.codesoftlabs.tusalud.app.repositories.SetInstrumentalRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EsterilizacionService {

    private static final int DIAS_AVISO_VENCIMIENTO = 3;
    private static final int HORAS_CICLO_SIN_INDICADOR = 24;

    private final EsterilizadorRepository esterilizadores;
    private final SetInstrumentalRepository sets;
    private final CicloEsterilizacionRepository ciclos;
    private final LoteEsterilRepository lotes;
    private final CirugiaSetRepository usosDeLotes;
    private final UsuarioRepository usuarios;
    private final NotificacionService notificaciones;
    private final SecuenciasQuirofano secuencias;

    public List<EsterilizadorResponse> esterilizadores() {
        return esterilizadores.findAllByOrderByCodigoAsc().stream()
                .map(EsterilizacionService::esterilizador)
                .toList();
    }

    @Transactional
    public EsterilizadorResponse crearEsterilizador(EsterilizadorRequest request) {
        String codigo = request.codigo().strip().toUpperCase();
        if (esterilizadores.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe un esterilizador con el código " + codigo);
        }
        Esterilizador nuevo = Esterilizador.builder()
                .codigo(codigo)
                .nombre(request.nombre().strip())
                .tipo(request.tipo())
                .activo(request.activo() == null || request.activo())
                .build();
        return esterilizador(esterilizadores.save(nuevo));
    }

    @Transactional
    public EsterilizadorResponse actualizarEsterilizador(UUID id, EsterilizadorRequest request) {
        Esterilizador e = esterilizadores
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Esterilizador", "id", id));
        e.setNombre(request.nombre().strip());
        e.setTipo(request.tipo());
        if (request.activo() != null) {
            e.setActivo(request.activo());
        }
        return esterilizador(e);
    }

    public List<SetInstrumentalResponse> sets() {
        return sets.findAllByOrderByCodigoAsc().stream()
                .map(EsterilizacionService::set)
                .toList();
    }

    @Transactional
    public SetInstrumentalResponse crearSet(SetInstrumentalRequest request) {
        String codigo = request.codigo().strip().toUpperCase();
        if (sets.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe un set con el código " + codigo);
        }
        SetInstrumental nuevo = SetInstrumental.builder()
                .codigo(codigo)
                .nombre(request.nombre().strip())
                .descripcion(request.descripcion())
                .vigenciaDias(request.vigenciaDias())
                .activo(request.activo() == null || request.activo())
                .build();
        return set(sets.save(nuevo));
    }

    @Transactional
    public SetInstrumentalResponse actualizarSet(UUID id, SetInstrumentalRequest request) {
        SetInstrumental s = sets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Set", "id", id));
        s.setNombre(request.nombre().strip());
        s.setDescripcion(request.descripcion());
        s.setVigenciaDias(request.vigenciaDias());
        if (request.activo() != null) {
            s.setActivo(request.activo());
        }
        return set(s);
    }

    public List<CicloResponse> ciclos(int limite) {
        List<CicloEsterilizacion> recientes = ciclos.recientes(PageRequest.of(0, Math.max(1, Math.min(limite, 200))));
        Map<UUID, List<LoteEsteril>> porCiclo = new LinkedHashMap<>();
        for (LoteEsteril lote : lotes.recientes(PageRequest.of(0, 1000))) {
            porCiclo.computeIfAbsent(lote.getCiclo().getId(), k -> new ArrayList<>())
                    .add(lote);
        }
        LocalDateTime ahora = LocalDateTime.now();
        return recientes.stream()
                .map(c -> ciclo(c, porCiclo.getOrDefault(c.getId(), List.of()), ahora))
                .toList();
    }

    public CicloResponse ciclo(UUID id) {
        CicloEsterilizacion c =
                ciclos.findConDetalle(id).orElseThrow(() -> new ResourceNotFoundException("Ciclo", "id", id));
        return ciclo(c, lotes.deCiclo(id), LocalDateTime.now());
    }

    @Transactional
    public CicloResponse crearCiclo(UsuarioPrincipal actor, CicloRequest request) {
        Esterilizador esterilizador = esterilizadores
                .findById(request.esterilizadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Esterilizador", "id", request.esterilizadorId()));
        if (!esterilizador.isActivo()) {
            throw new ConflictException("El esterilizador no está activo");
        }
        Usuario responsable = usuarios.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
        CicloEsterilizacion ciclo = ciclos.save(CicloEsterilizacion.builder()
                .numero(secuencias.codigo("CE", SecuenciasQuirofano.CICLO))
                .esterilizador(esterilizador)
                .iniciadoEn(request.iniciadoEn())
                .temperaturaC(request.temperaturaC())
                .presionKpa(request.presionKpa())
                .duracionMin(request.duracionMin())
                .responsable(responsable)
                .observaciones(request.observaciones())
                .build());
        LocalDateTime fin = request.iniciadoEn().plusMinutes(request.duracionMin());
        List<LoteEsteril> creados = new ArrayList<>();
        for (SetDelCicloRequest item : request.sets()) {
            SetInstrumental set = sets.findById(item.setId())
                    .orElseThrow(() -> new ResourceNotFoundException("Set", "id", item.setId()));
            if (!set.isActivo()) {
                throw new ConflictException("El set " + set.getCodigo() + " no está activo");
            }
            for (int i = 0; i < item.cantidad(); i++) {
                creados.add(lotes.save(LoteEsteril.builder()
                        .codigo(secuencias.codigo("LE", SecuenciasQuirofano.LOTE))
                        .setInstrumental(set)
                        .ciclo(ciclo)
                        .esterilizadoEn(fin)
                        .venceEn(fin.plusDays(set.getVigenciaDias()))
                        .build()));
            }
        }
        return ciclo(ciclo, creados, LocalDateTime.now());
    }

    @Transactional
    public CicloResponse registrarIndicadores(UsuarioPrincipal actor, UUID id, IndicadoresRequest request) {
        CicloEsterilizacion c =
                ciclos.findConDetalle(id).orElseThrow(() -> new ResourceNotFoundException("Ciclo", "id", id));
        if (c.getResultado() != ResultadoCiclo.EN_PROCESO) {
            throw new ConflictException("El ciclo ya está cerrado y sus indicadores no se modifican");
        }
        c.setIndicadorQuimico(request.quimico());
        c.setIndicadorBiologico(request.biologico());
        if (request.observaciones() != null && !request.observaciones().isBlank()) {
            c.setObservaciones(request.observaciones().strip());
        }
        boolean noConforme = request.quimico() == ResultadoIndicador.NO_CONFORME
                || request.biologico() == ResultadoIndicador.NO_CONFORME;
        boolean conforme =
                request.quimico() == ResultadoIndicador.CONFORME && request.biologico() == ResultadoIndicador.CONFORME;
        if (noConforme) {
            c.setResultado(ResultadoCiclo.NO_CONFORME);
            c.setCerradoEn(LocalDateTime.now());
            notificarNoConforme(c);
        } else if (conforme) {
            c.setResultado(ResultadoCiclo.CONFORME);
            c.setCerradoEn(LocalDateTime.now());
        }
        return ciclo(c, lotes.deCiclo(id), LocalDateTime.now());
    }

    public List<LoteEsterilResponse> lotes(boolean soloUtilizables) {
        LocalDateTime ahora = LocalDateTime.now();
        return lotes.vigentes().stream()
                .map(l -> lote(l, ahora))
                .filter(l -> !soloUtilizables || l.utilizable())
                .toList();
    }

    public TrazabilidadLoteResponse trazabilidadLote(UUID id) {
        LoteEsteril l = lotes.findConDetalle(id).orElseThrow(() -> new ResourceNotFoundException("Lote", "id", id));
        return trazabilidad(l, usosDeLotes.deLotes(List.of(id)), LocalDateTime.now());
    }

    public List<TrazabilidadLoteResponse> trazabilidadCiclo(UUID cicloId) {
        List<LoteEsteril> delCiclo = lotes.deCiclo(cicloId);
        if (delCiclo.isEmpty() && ciclos.findById(cicloId).isEmpty()) {
            throw new ResourceNotFoundException("Ciclo", "id", cicloId);
        }
        Map<UUID, List<CirugiaSet>> usos =
                usosDeLotes.deLotes(delCiclo.stream().map(LoteEsteril::getId).toList()).stream()
                        .collect(Collectors.groupingBy(u -> u.getLote().getId()));
        LocalDateTime ahora = LocalDateTime.now();
        return delCiclo.stream()
                .map(l -> trazabilidad(l, usos.getOrDefault(l.getId(), List.of()), ahora))
                .toList();
    }

    public List<AlertaEsterilizacionResponse> alertas() {
        LocalDateTime ahora = LocalDateTime.now();
        List<AlertaEsterilizacionResponse> resultado = new ArrayList<>();
        List<LoteEsteril> vigentes = lotes.vigentes();
        Map<UUID, List<CirugiaSet>> usos =
                usosDeLotes.deLotes(vigentes.stream().map(LoteEsteril::getId).toList()).stream()
                        .collect(Collectors.groupingBy(u -> u.getLote().getId()));
        for (LoteEsteril l : vigentes) {
            EstadoEfectivoLote estado = efectivo(l, ahora);
            if (estado == EstadoEfectivoLote.BLOQUEADO
                    && !usos.getOrDefault(l.getId(), List.of()).isEmpty()) {
                resultado.add(new AlertaEsterilizacionResponse(
                        "LOTE_NO_CONFORME_USADO",
                        "CRITICA",
                        "El lote " + l.getCodigo() + " proviene de un ciclo no conforme y ya se usó en una cirugía",
                        l.getCodigo(),
                        l.getEsterilizadoEn()));
            } else if (estado == EstadoEfectivoLote.BLOQUEADO) {
                resultado.add(new AlertaEsterilizacionResponse(
                        "LOTE_BLOQUEADO",
                        "ALTA",
                        "El lote " + l.getCodigo() + " está bloqueado por un ciclo no conforme",
                        l.getCodigo(),
                        l.getEsterilizadoEn()));
            } else if (estado == EstadoEfectivoLote.VENCIDO) {
                resultado.add(new AlertaEsterilizacionResponse(
                        "LOTE_VENCIDO",
                        "MEDIA",
                        "La esterilidad del lote " + l.getCodigo() + " venció y debe reprocesarse",
                        l.getCodigo(),
                        l.getVenceEn()));
            } else if (estado == EstadoEfectivoLote.ESTERIL
                    && l.getVenceEn().isBefore(ahora.plusDays(DIAS_AVISO_VENCIMIENTO))) {
                resultado.add(new AlertaEsterilizacionResponse(
                        "LOTE_POR_VENCER",
                        "BAJA",
                        "La esterilidad del lote " + l.getCodigo() + " vence pronto",
                        l.getCodigo(),
                        l.getVenceEn()));
            }
        }
        for (CicloEsterilizacion c :
                ciclos.enProcesoAntesDe(ResultadoCiclo.EN_PROCESO, ahora.minusHours(HORAS_CICLO_SIN_INDICADOR))) {
            resultado.add(new AlertaEsterilizacionResponse(
                    "CICLO_SIN_INDICADOR",
                    "MEDIA",
                    "El ciclo " + c.getNumero() + " lleva más de " + HORAS_CICLO_SIN_INDICADOR
                            + " horas sin resultado de indicadores",
                    c.getNumero(),
                    c.getIniciadoEn()));
        }
        resultado.sort(Comparator.comparingInt((AlertaEsterilizacionResponse a) -> pesoSeveridad(a.severidad()))
                .thenComparing(AlertaEsterilizacionResponse::fecha));
        return resultado;
    }

    @Scheduled(cron = "${app.quirofano.alertas-cron:0 0 7 * * *}")
    @Transactional
    public void revisionDiaria() {
        List<AlertaEsterilizacionResponse> alertas = alertas();
        if (alertas.isEmpty()) {
            return;
        }
        long urgentes = alertas.stream()
                .filter(a -> a.severidad().equals("CRITICA") || a.severidad().equals("ALTA"))
                .count();
        String mensaje = alertas.size() + " alerta(s) de esterilización pendientes de revisión, " + urgentes
                + " de ellas urgentes.";
        destinatarios()
                .forEach(u -> notificaciones.crear(
                        u,
                        TipoNotificacion.ESTERILIZACION_ALERTA,
                        "Alertas de esterilización",
                        mensaje,
                        "/esterilizacion"));
        log.info("Alertas de esterilización notificadas: {}", alertas.size());
    }

    public static EstadoEfectivoLote efectivo(LoteEsteril lote, LocalDateTime ahora) {
        if (lote.getCiclo().getResultado() == ResultadoCiclo.NO_CONFORME) {
            return EstadoEfectivoLote.BLOQUEADO;
        }
        if (lote.getEstado() == EstadoLoteEsteril.USADO) {
            return EstadoEfectivoLote.USADO;
        }
        if (lote.getEstado() == EstadoLoteEsteril.EN_USO) {
            return EstadoEfectivoLote.EN_USO;
        }
        if (lote.getVenceEn().isBefore(ahora)) {
            return EstadoEfectivoLote.VENCIDO;
        }
        if (lote.getCiclo().getResultado() == ResultadoCiclo.EN_PROCESO) {
            return EstadoEfectivoLote.EN_ESPERA_DE_INDICADOR;
        }
        return EstadoEfectivoLote.ESTERIL;
    }

    public static LoteEsterilResponse lote(LoteEsteril l, LocalDateTime ahora) {
        EstadoEfectivoLote estado = efectivo(l, ahora);
        return new LoteEsterilResponse(
                l.getId(),
                l.getCodigo(),
                l.getSetInstrumental().getId(),
                l.getSetInstrumental().getNombre(),
                l.getCiclo().getId(),
                l.getCiclo().getNumero(),
                l.getEsterilizadoEn(),
                l.getVenceEn(),
                estado,
                estado == EstadoEfectivoLote.ESTERIL);
    }

    private void notificarNoConforme(CicloEsterilizacion c) {
        List<LoteEsteril> delCiclo = lotes.deCiclo(c.getId());
        boolean yaUsados = !usosDeLotes
                .deLotes(delCiclo.stream().map(LoteEsteril::getId).toList())
                .isEmpty();
        String mensaje = "El ciclo " + c.getNumero() + " resultó no conforme y " + delCiclo.size()
                + " lote(s) quedaron bloqueados."
                + (yaUsados ? " Hay lotes de este ciclo ya usados en cirugías: revise la trazabilidad." : "");
        destinatarios()
                .forEach(u -> notificaciones.crear(
                        u,
                        TipoNotificacion.ESTERILIZACION_ALERTA,
                        "Ciclo de esterilización no conforme",
                        mensaje,
                        "/esterilizacion"));
    }

    private List<Usuario> destinatarios() {
        List<Usuario> destino = new ArrayList<>(usuarios.activosPorRol(RolNombre.ROLE_ENFERMERIA));
        destino.addAll(usuarios.activosPorRol(RolNombre.ROLE_CALIDAD));
        return destino;
    }

    private static TrazabilidadLoteResponse trazabilidad(LoteEsteril l, List<CirugiaSet> usos, LocalDateTime ahora) {
        List<UsoLoteResponse> items = usos.stream()
                .map(u -> new UsoLoteResponse(
                        u.getCirugia().getCodigo(),
                        u.getCirugia().getId(),
                        u.getCirugia().getEstado(),
                        u.getRegistradoEn(),
                        u.getCirugia().getQuirofano() == null
                                ? null
                                : u.getCirugia().getQuirofano().getNombre()))
                .toList();
        return new TrazabilidadLoteResponse(lote(l, ahora), l.getCiclo().getResultado(), items);
    }

    private static CicloResponse ciclo(CicloEsterilizacion c, List<LoteEsteril> delCiclo, LocalDateTime ahora) {
        return new CicloResponse(
                c.getId(),
                c.getNumero(),
                c.getEsterilizador().getId(),
                c.getEsterilizador().getNombre(),
                c.getIniciadoEn(),
                c.getTemperaturaC(),
                c.getPresionKpa(),
                c.getDuracionMin(),
                c.getIndicadorQuimico(),
                c.getIndicadorBiologico(),
                c.getResultado(),
                c.getResponsable().getNombreCompleto(),
                c.getObservaciones(),
                c.getCerradoEn(),
                delCiclo.stream().map(l -> lote(l, ahora)).toList());
    }

    private static EsterilizadorResponse esterilizador(Esterilizador e) {
        return new EsterilizadorResponse(e.getId(), e.getCodigo(), e.getNombre(), e.getTipo(), e.isActivo());
    }

    private static SetInstrumentalResponse set(SetInstrumental s) {
        return new SetInstrumentalResponse(
                s.getId(), s.getCodigo(), s.getNombre(), s.getDescripcion(), s.getVigenciaDias(), s.isActivo());
    }

    private static int pesoSeveridad(String severidad) {
        return switch (severidad) {
            case "CRITICA" -> 0;
            case "ALTA" -> 1;
            case "MEDIA" -> 2;
            default -> 3;
        };
    }
}

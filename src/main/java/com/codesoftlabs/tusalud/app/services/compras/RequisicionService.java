package com.codesoftlabs.tusalud.app.services.compras;

import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.RequisicionItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.RequisicionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.OrdenReferencia;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RequisicionDetalle;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RequisicionItemResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RequisicionResumen;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.SugerenciaRequisicion;
import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.Requisicion;
import com.codesoftlabs.tusalud.app.entities.RequisicionItem;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.AreaSolicitante;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoOrdenCompra;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoRequisicion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.PrioridadRequisicion;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CotizacionRepository;
import com.codesoftlabs.tusalud.app.repositories.OrdenCompraRepository;
import com.codesoftlabs.tusalud.app.repositories.ProductoRepository;
import com.codesoftlabs.tusalud.app.repositories.RequisicionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AlmacenService;
import com.codesoftlabs.tusalud.app.services.interfaces.InventarioService;
import com.codesoftlabs.tusalud.app.services.interfaces.InventarioService.StockProducto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequisicionService {

    private static final int LIMITE = 200;
    private static final int DIAS_POR_DEFECTO = 90;
    private static final String ENTIDAD = "REQUISICION";
    private static final String ENLACE_ALMACEN = "/almacen/compras/requisiciones";

    private final RequisicionRepository requisicionRepository;
    private final CotizacionRepository cotizacionRepository;
    private final OrdenCompraRepository ordenRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlmacenService almacenService;
    private final InventarioService inventarioService;
    private final ComprasSecuencias secuencias;
    private final ComprasAuditor auditor;
    private final ComprasAvisos avisos;

    public List<RequisicionResumen> listar(
            UsuarioPrincipal actor, EstadoRequisicion estado, LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        UUID solicitante = soloPropias(actor) ? actor.getId() : null;
        return requisicionRepository
                .buscar(
                        estado,
                        solicitante,
                        inicio.atStartOfDay(),
                        fin.plusDays(1).atStartOfDay(),
                        PageRequest.of(0, LIMITE))
                .stream()
                .map(ComprasMapper::requisicion)
                .toList();
    }

    public List<SugerenciaRequisicion> sugeridas() {
        return almacenService.alertas().bajoStock().stream()
                .filter(ProductoResponse::activo)
                .map(p -> new SugerenciaRequisicion(
                        p.id(), p.codigo(), p.nombre(), p.unidad(), p.stockActual(), p.stockMinimo(), (int)
                                Math.max(1, (long) p.stockMinimo() * 2 - p.stockActual())))
                .toList();
    }

    public RequisicionDetalle obtener(UsuarioPrincipal actor, UUID id) {
        Requisicion requisicion = requisicion(id);
        verificarLectura(actor, requisicion);
        return detalle(requisicion);
    }

    @Transactional
    public RequisicionDetalle crear(UsuarioPrincipal actor, RequisicionRequest request) {
        Usuario usuario = usuario(actor);
        AreaSolicitante area = area(actor);
        int anio = LocalDate.now().getYear();
        Requisicion requisicion = Requisicion.builder()
                .codigo(ComprasSecuencias.formatear(
                        ComprasSecuencias.REQUISICION, anio, secuencias.siguiente(ComprasSecuencias.REQUISICION, anio)))
                .estado(EstadoRequisicion.BORRADOR)
                .prioridad(request.prioridad() == null ? PrioridadRequisicion.NORMAL : request.prioridad())
                .area(area)
                .solicitante(usuario)
                .motivo(limpiar(request.motivo()))
                .sugerida(Boolean.TRUE.equals(request.sugerida()))
                .creadaEn(LocalDateTime.now())
                .build();
        reemplazarItems(requisicion, request.items());
        requisicionRepository.save(requisicion);
        auditor.registrar(usuario, ENTIDAD, requisicion.getId(), "CREAR", requisicion.getCodigo());
        return detalle(requisicion);
    }

    @Transactional
    public RequisicionDetalle actualizar(UsuarioPrincipal actor, UUID id, RequisicionRequest request) {
        Requisicion requisicion = requisicionParaActualizar(id);
        soloSolicitante(actor, requisicion);
        exigirEstado(requisicion, "solo se edita en borrador", EstadoRequisicion.BORRADOR);
        requisicion.setPrioridad(request.prioridad() == null ? PrioridadRequisicion.NORMAL : request.prioridad());
        requisicion.setMotivo(limpiar(request.motivo()));
        requisicion.getItems().clear();
        requisicionRepository.flush();
        reemplazarItems(requisicion, request.items());
        auditor.registrar(usuario(actor), ENTIDAD, id, "EDITAR", requisicion.getCodigo());
        return detalle(requisicion);
    }

    @Transactional
    public RequisicionDetalle enviar(UsuarioPrincipal actor, UUID id) {
        Requisicion requisicion = requisicionParaActualizar(id);
        soloSolicitante(actor, requisicion);
        exigirEstado(requisicion, "ya fue enviada", EstadoRequisicion.BORRADOR);
        if (requisicion.getItems().isEmpty()) {
            throw new ConflictException("La requisición no tiene productos");
        }
        requisicion.setEstado(EstadoRequisicion.ENVIADA);
        requisicion.setEnviadaEn(LocalDateTime.now());
        Usuario usuario = usuario(actor);
        auditor.registrar(usuario, ENTIDAD, id, "ENVIAR", requisicion.getCodigo());
        avisos.avisarRol(
                RolNombre.ROLE_ALMACEN,
                actor.getId(),
                TipoNotificacion.REQUISICION_ENVIADA,
                "Requisición por atender",
                "La requisición " + requisicion.getCodigo() + " de " + usuario.getNombreCompleto()
                        + " espera cotización.",
                ENLACE_ALMACEN);
        return detalle(requisicion);
    }

    @Transactional
    public RequisicionDetalle rechazar(UsuarioPrincipal actor, UUID id, String motivo) {
        Requisicion requisicion = requisicionParaActualizar(id);
        if (requisicion.getSolicitante().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Quien solicita una requisición no puede rechazarla");
        }
        exigirEstado(
                requisicion,
                "no admite rechazo en su estado actual",
                EstadoRequisicion.ENVIADA,
                EstadoRequisicion.EN_COTIZACION);
        ordenesActivas(id).findAny().ifPresent(o -> {
            throw new ConflictException("La requisición tiene órdenes de compra activas");
        });
        Usuario usuario = usuario(actor);
        resolver(requisicion, usuario, EstadoRequisicion.RECHAZADA, motivo);
        auditor.registrar(usuario, ENTIDAD, id, "RECHAZAR", motivo);
        avisos.avisarUsuario(
                requisicion.getSolicitante(),
                TipoNotificacion.REQUISICION_RESUELTA,
                "Requisición rechazada",
                "La requisición " + requisicion.getCodigo() + " fue rechazada: " + motivo,
                "/compras/requisiciones");
        return detalle(requisicion);
    }

    @Transactional
    public RequisicionDetalle anular(UsuarioPrincipal actor, UUID id, String motivo) {
        Requisicion requisicion = requisicionParaActualizar(id);
        soloSolicitante(actor, requisicion);
        exigirEstado(
                requisicion,
                "no se puede anular en su estado actual",
                EstadoRequisicion.BORRADOR,
                EstadoRequisicion.ENVIADA,
                EstadoRequisicion.EN_COTIZACION);
        ordenesActivas(id).findAny().ifPresent(o -> {
            throw new ConflictException("La requisición tiene órdenes de compra activas");
        });
        Usuario usuario = usuario(actor);
        resolver(requisicion, usuario, EstadoRequisicion.ANULADA, motivo);
        auditor.registrar(usuario, ENTIDAD, id, "ANULAR", motivo);
        return detalle(requisicion);
    }

    private java.util.stream.Stream<com.codesoftlabs.tusalud.app.entities.OrdenCompra> ordenesActivas(UUID id) {
        return ordenRepository.findByRequisicionId(id).stream()
                .filter(o ->
                        o.getEstado() != EstadoOrdenCompra.ANULADA && o.getEstado() != EstadoOrdenCompra.RECHAZADA);
    }

    private void resolver(Requisicion requisicion, Usuario usuario, EstadoRequisicion estado, String motivo) {
        requisicion.setEstado(estado);
        requisicion.setResueltaEn(LocalDateTime.now());
        requisicion.setResueltaPor(usuario);
        requisicion.setMotivoResolucion(limpiar(motivo));
    }

    private void reemplazarItems(Requisicion requisicion, List<RequisicionItemRequest> items) {
        Set<UUID> vistos = new HashSet<>();
        for (RequisicionItemRequest item : items) {
            if (!vistos.add(item.productoId())) {
                throw new ConflictException("El producto está repetido en la requisición");
            }
            Producto producto = productoRepository
                    .findById(item.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", item.productoId()));
            if (!producto.isActivo()) {
                throw new ConflictException("El producto " + producto.getNombre() + " está inactivo");
            }
            requisicion.agregarItem(RequisicionItem.builder()
                    .producto(producto)
                    .cantidad(item.cantidad())
                    .observacion(limpiar(item.observacion()))
                    .build());
        }
    }

    private RequisicionDetalle detalle(Requisicion requisicion) {
        Map<UUID, StockProducto> stock = inventarioService.stockDe(requisicion.getItems().stream()
                .map(i -> i.getProducto().getId())
                .toList());
        List<RequisicionItemResponse> items = requisicion.getItems().stream()
                .map(i -> ComprasMapper.requisicionItem(i, stock))
                .toList();
        List<OrdenReferencia> ordenes = ordenRepository.findByRequisicionId(requisicion.getId()).stream()
                .map(o -> new OrdenReferencia(o.getId(), o.codigoVisible(), o.getEstado(), o.getTotal()))
                .toList();
        int cotizaciones =
                cotizacionRepository.deRequisicion(requisicion.getId()).size();
        return new RequisicionDetalle(
                ComprasMapper.requisicion(requisicion),
                items,
                requisicion.getMotivoResolucion(),
                requisicion.getResueltaPor() == null
                        ? null
                        : requisicion.getResueltaPor().getNombreCompleto(),
                requisicion.getResueltaEn(),
                cotizaciones,
                ordenes);
    }

    private void exigirEstado(Requisicion requisicion, String detalle, EstadoRequisicion... permitidos) {
        for (EstadoRequisicion estado : permitidos) {
            if (requisicion.getEstado() == estado) {
                return;
            }
        }
        throw new ConflictException("La requisición " + requisicion.getCodigo() + " " + detalle);
    }

    private void soloSolicitante(UsuarioPrincipal actor, Requisicion requisicion) {
        if (!requisicion.getSolicitante().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Solo quien solicitó la requisición puede modificarla");
        }
    }

    private void verificarLectura(UsuarioPrincipal actor, Requisicion requisicion) {
        if (soloPropias(actor) && !requisicion.getSolicitante().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("No tiene acceso a esta requisición");
        }
    }

    private boolean soloPropias(UsuarioPrincipal actor) {
        return actor.getRoles().contains(RolNombre.ROLE_FARMACIA.name())
                && !actor.getRoles().contains(RolNombre.ROLE_ALMACEN.name())
                && !actor.getRoles().contains(RolNombre.ROLE_ADMIN.name());
    }

    private AreaSolicitante area(UsuarioPrincipal actor) {
        if (actor.getRoles().contains(RolNombre.ROLE_ALMACEN.name())) {
            return AreaSolicitante.ALMACEN;
        }
        return AreaSolicitante.FARMACIA;
    }

    private Requisicion requisicion(UUID id) {
        return requisicionRepository
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisición", "id", id));
    }

    private Requisicion requisicionParaActualizar(UUID id) {
        requisicionRepository
                .findParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisición", "id", id));
        return requisicion(id);
    }

    private Usuario usuario(UsuarioPrincipal actor) {
        return usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}

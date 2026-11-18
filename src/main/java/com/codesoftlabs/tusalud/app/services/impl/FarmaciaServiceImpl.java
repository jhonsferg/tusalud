package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.DispensacionItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.DispensacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.CatalogoFarmaciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DispensacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaFarmaciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaItemFarmaciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenFarmaciaResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.Dispensacion;
import com.codesoftlabs.tusalud.app.entities.DispensacionItem;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.RecetaItem;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoDispensacion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoReceta;
import com.codesoftlabs.tusalud.app.entities.enums.MotivoMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.DispensacionRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.ProductoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.TextoSeguro;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.FarmaciaService;
import com.codesoftlabs.tusalud.app.services.interfaces.InventarioService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FarmaciaServiceImpl implements FarmaciaService {

    private static final int VIGENCIA_RECETA_DIAS = 90;
    private static final int LIMITE_CATALOGO = 300;
    private static final int LIMITE_DISPENSACIONES = 200;
    private static final int DIAS_POR_DEFECTO = 30;
    private static final Set<TipoProducto> TIPOS_VENDIBLES =
            EnumSet.of(TipoProducto.MEDICAMENTO, TipoProducto.MATERIAL);

    private final ProductoRepository productoRepository;
    private final AtencionRepository atencionRepository;
    private final DispensacionRepository dispensacionRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;
    private final NotificacionService notificacionService;
    private final ProductoResponseMapper mapper;

    @Override
    public List<CatalogoFarmaciaResponse> catalogo(String q) {
        List<Producto> productos = productoRepository.buscar(
                termino(q), TipoProducto.MEDICAMENTO, true, PageRequest.of(0, LIMITE_CATALOGO));
        return mapper.aRespuestas(productos).stream().map(this::aCatalogo).toList();
    }

    @Override
    public List<RecetaFarmaciaResponse> recetas(EstadoReceta estado, String q) {
        Set<EstadoReceta> estados =
                estado == null ? EnumSet.of(EstadoReceta.PENDIENTE, EstadoReceta.PARCIAL) : EnumSet.of(estado);
        String buscado = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        return recetasRecientes().stream()
                .filter(receta -> estados.contains(receta.estado()))
                .filter(receta -> buscado == null
                        || receta.dni().toLowerCase(Locale.ROOT).contains(buscado)
                        || receta.pacienteNombre().toLowerCase(Locale.ROOT).contains(buscado))
                .toList();
    }

    @Override
    @Transactional
    public DispensacionResponse dispensar(UsuarioPrincipal actor, DispensacionRequest request) {
        Usuario usuario = usuario(actor);
        Atencion atencion = null;
        Paciente paciente = null;
        if (request.atencionId() != null) {
            atencion = atencionRepository
                    .findByIdConDetalle(request.atencionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Atención", "id", request.atencionId()));
            if (atencion.getFecha().isBefore(LocalDateTime.now().minusDays(VIGENCIA_RECETA_DIAS))) {
                throw new ConflictException("La receta venció: tiene más de " + VIGENCIA_RECETA_DIAS + " días");
            }
            paciente = atencion.getPaciente();
            if (request.pacienteId() != null && !request.pacienteId().equals(paciente.getId())) {
                throw new DatosInvalidosException("El paciente no coincide con el de la receta");
            }
        } else if (request.pacienteId() != null) {
            paciente = pacienteRepository
                    .findByIdWithUsuario(request.pacienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.pacienteId()));
        }

        Set<Integer> posiciones = new HashSet<>();
        Dispensacion dispensacion = Dispensacion.builder()
                .fecha(LocalDateTime.now())
                .estado(EstadoDispensacion.EMITIDA)
                .paciente(paciente)
                .atencion(atencion)
                .usuario(usuario)
                .metodoPago(request.metodoPago())
                .observaciones(limpiar(request.observaciones()))
                .total(BigDecimal.ZERO)
                .comprobante("")
                .build();
        BigDecimal total = BigDecimal.ZERO;
        Map<UUID, Producto> productos = new LinkedHashMap<>();
        Map<UUID, Integer> demanda = new HashMap<>();
        for (DispensacionItemRequest item : request.items()) {
            Producto producto = productoRepository
                    .findById(item.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", item.productoId()));
            validarProducto(producto, item, atencion, posiciones);
            productos.put(producto.getId(), producto);
            demanda.merge(producto.getId(), item.cantidad(), Integer::sum);
            BigDecimal subtotal = producto.getPrecioVenta().multiply(BigDecimal.valueOf(item.cantidad()));
            total = total.add(subtotal);
            dispensacion.agregarItem(DispensacionItem.builder()
                    .producto(producto)
                    .cantidad(item.cantidad())
                    .precioUnitario(producto.getPrecioVenta())
                    .subtotal(subtotal)
                    .recetaPosicion(item.recetaItemId())
                    .build());
        }
        validarStock(productos, demanda);
        dispensacion.setTotal(total);
        dispensacion.setComprobante(String.format("F001-%06d", dispensacionRepository.count() + 1));
        dispensacion = dispensacionRepository.save(dispensacion);

        for (DispensacionItem item : dispensacion.getItems()) {
            inventarioService.descontar(
                    item.getProducto(),
                    item.getCantidad(),
                    MotivoMovimiento.DISPENSACION,
                    dispensacion.getComprobante(),
                    usuario,
                    null);
            if (item.getRecetaPosicion() != null) {
                marcarReceta(atencion, item.getRecetaPosicion(), true);
            }
        }
        if (paciente != null) {
            notificacionService.crear(
                    paciente.getUsuario(),
                    TipoNotificacion.RECETA_DISPENSADA,
                    "Medicamentos dispensados",
                    "Se dispensaron tus medicamentos por S/ " + total.toPlainString() + ". Comprobante "
                            + dispensacion.getComprobante() + ".",
                    "/mis-medicamentos");
        }
        return DispensacionResponse.from(dispensacion);
    }

    @Override
    public List<DispensacionResponse> listar(LocalDate desde, LocalDate hasta, String q) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        if (inicio.isAfter(fin)) {
            throw new DatosInvalidosException("La fecha inicial no puede ser posterior a la final");
        }
        return dispensacionRepository
                .buscar(
                        inicio.atStartOfDay(),
                        fin.plusDays(1).atStartOfDay(),
                        termino(q),
                        PageRequest.of(0, LIMITE_DISPENSACIONES))
                .stream()
                .map(DispensacionResponse::from)
                .toList();
    }

    @Override
    public DispensacionResponse obtener(UUID id) {
        return DispensacionResponse.from(buscar(id));
    }

    @Override
    @Transactional
    public DispensacionResponse anular(UsuarioPrincipal actor, UUID id, String motivo) {
        Usuario usuario = usuario(actor);
        Dispensacion dispensacion = buscar(id);
        String motivoLimpio = TextoSeguro.limpiar(motivo);
        if (motivoLimpio == null || motivoLimpio.length() < 5 || motivoLimpio.length() > 300) {
            throw new DatosInvalidosException("El motivo de la anulación debe tener entre 5 y 300 caracteres");
        }
        if (dispensacion.getEstado() == EstadoDispensacion.ANULADA) {
            throw new ConflictException("La dispensación " + dispensacion.getComprobante() + " ya fue anulada");
        }
        inventarioService.devolverPorReferencia(dispensacion.getComprobante(), usuario);
        for (DispensacionItem item : dispensacion.getItems()) {
            if (item.getRecetaPosicion() != null && dispensacion.getAtencion() != null) {
                marcarReceta(dispensacion.getAtencion(), item.getRecetaPosicion(), false);
            }
        }
        dispensacion.setEstado(EstadoDispensacion.ANULADA);
        dispensacion.setMotivoAnulacion(motivoLimpio);
        log.info(
                "Auditoría: dispensación {} anulada por {} con motivo registrado",
                dispensacion.getId(),
                usuario.getId());
        return DispensacionResponse.from(dispensacion);
    }

    @Override
    public List<DispensacionResponse> misDispensaciones(UsuarioPrincipal actor) {
        Paciente paciente = pacienteRepository
                .findByUsuarioId(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "usuario", actor.getId()));
        return dispensacionRepository.findByPacienteId(paciente.getId()).stream()
                .map(DispensacionResponse::from)
                .toList();
    }

    @Override
    public ResumenFarmaciaResponse resumen() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1);
        return new ResumenFarmaciaResponse(
                dispensacionRepository.sumaTotal(EstadoDispensacion.EMITIDA, inicio, fin),
                dispensacionRepository.countByEstadoAndFechaGreaterThanEqualAndFechaLessThan(
                        EstadoDispensacion.EMITIDA, inicio, fin),
                recetas(null, null).size());
    }

    private void validarProducto(
            Producto producto, DispensacionItemRequest item, Atencion atencion, Set<Integer> posiciones) {
        if (!producto.isActivo()) {
            throw new ConflictException("El producto " + producto.getNombre() + " está inactivo");
        }
        if (!TIPOS_VENDIBLES.contains(producto.getTipo())) {
            throw new DatosInvalidosException("El producto " + producto.getNombre() + " no se vende en farmacia");
        }
        Integer posicion = item.recetaItemId();
        if (posicion == null) {
            if (producto.isRequiereReceta()) {
                throw new ConflictException("El producto " + producto.getNombre() + " requiere receta");
            }
            return;
        }
        if (atencion == null) {
            throw new DatosInvalidosException("Para dispensar un ítem de receta debe indicar la atención");
        }
        List<RecetaItem> receta = atencion.getReceta();
        if (posicion < 0 || posicion >= receta.size()) {
            throw new DatosInvalidosException("El ítem de receta " + posicion + " no existe");
        }
        if (!posiciones.add(posicion)) {
            throw new DatosInvalidosException("El ítem de receta " + posicion + " está repetido");
        }
        if (receta.get(posicion).isDispensado()) {
            throw new ConflictException(
                    "El ítem de receta " + receta.get(posicion).getMedicamento() + " ya fue dispensado");
        }
    }

    private void validarStock(Map<UUID, Producto> productos, Map<UUID, Integer> demanda) {
        Map<UUID, InventarioService.StockProducto> stock = inventarioService.stockDe(demanda.keySet());
        for (Map.Entry<UUID, Integer> pedido : demanda.entrySet()) {
            long disponible = stock.containsKey(pedido.getKey())
                    ? stock.get(pedido.getKey()).stock()
                    : 0;
            if (disponible < pedido.getValue()) {
                throw new ConflictException("Stock insuficiente de "
                        + productos.get(pedido.getKey()).getNombre() + ": disponible " + disponible + ", solicitado "
                        + pedido.getValue());
            }
        }
    }

    private void marcarReceta(Atencion atencion, int posicion, boolean dispensado) {
        RecetaItem actual = atencion.getReceta().get(posicion);
        atencion.getReceta()
                .set(
                        posicion,
                        RecetaItem.builder()
                                .medicamento(actual.getMedicamento())
                                .dosis(actual.getDosis())
                                .frecuencia(actual.getFrecuencia())
                                .duracion(actual.getDuracion())
                                .dispensado(dispensado)
                                .build());
    }

    private List<RecetaFarmaciaResponse> recetasRecientes() {
        LocalDateTime desde = LocalDateTime.now().minusDays(VIGENCIA_RECETA_DIAS);
        return atencionRepository.findConRecetaDesde(desde).stream()
                .map(this::aReceta)
                .toList();
    }

    private RecetaFarmaciaResponse aReceta(Atencion atencion) {
        List<RecetaItem> receta = atencion.getReceta();
        List<RecetaItemFarmaciaResponse> items = new java.util.ArrayList<>();
        for (int i = 0; i < receta.size(); i++) {
            RecetaItem item = receta.get(i);
            items.add(new RecetaItemFarmaciaResponse(
                    i,
                    item.getMedicamento(),
                    item.getDosis(),
                    item.getFrecuencia(),
                    item.getDuracion(),
                    atencion.getIndicaciones(),
                    item.isDispensado()));
        }
        long dispensados =
                items.stream().filter(RecetaItemFarmaciaResponse::dispensado).count();
        EstadoReceta estado = dispensados == 0
                ? EstadoReceta.PENDIENTE
                : dispensados == items.size() ? EstadoReceta.DISPENSADA : EstadoReceta.PARCIAL;
        Paciente paciente = atencion.getPaciente();
        return new RecetaFarmaciaResponse(
                atencion.getId(),
                atencion.getCita().getId(),
                atencion.getFecha(),
                paciente.getId(),
                paciente.getUsuario().getNombreCompleto(),
                paciente.getDni(),
                atencion.getMedico().getUsuario().getNombreCompleto(),
                estado,
                items);
    }

    private CatalogoFarmaciaResponse aCatalogo(ProductoResponse producto) {
        return new CatalogoFarmaciaResponse(
                producto.id(),
                producto.codigo(),
                producto.nombre(),
                producto.principioActivo(),
                producto.presentacion(),
                producto.unidad(),
                producto.precioVenta(),
                producto.stockActual(),
                producto.requiereReceta());
    }

    private Dispensacion buscar(UUID id) {
        return dispensacionRepository
                .findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispensación", "id", id));
    }

    private Usuario usuario(UsuarioPrincipal actor) {
        return usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
    }

    private String termino(String q) {
        return q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}

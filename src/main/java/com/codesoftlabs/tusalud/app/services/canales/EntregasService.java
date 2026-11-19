package com.codesoftlabs.tusalud.app.services.canales;

import com.codesoftlabs.tusalud.app.canales.ProveedorSimulado;
import com.codesoftlabs.tusalud.app.config.CanalesProperties;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.EntregaResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.MensajeSimuladoResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.PaginaEntregasResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.ProveedoresResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.ResumenCanalResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.ResumenEntregasResponse;
import com.codesoftlabs.tusalud.app.entities.EntregaNotificacion;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.EntregaNotificacionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntregasService {

    private static final LocalDateTime MINIMO = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime MAXIMO = LocalDateTime.of(2100, 1, 1, 0, 0);

    private final EntregaNotificacionRepository entregas;
    private final ProveedorSimulado simulado;
    private final CanalesProperties propiedades;

    public PaginaEntregasResponse listar(
            Canal canal, EstadoEntrega estado, LocalDate desde, LocalDate hasta, int pagina, int tamano) {
        Page<EntregaNotificacion> resultado = entregas.buscar(
                canal,
                estado,
                desde == null ? MINIMO : desde.atStartOfDay(),
                hasta == null ? MAXIMO : hasta.plusDays(1).atStartOfDay(),
                PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 100)));
        return new PaginaEntregasResponse(
                resultado.getContent().stream().map(EntregasService::respuesta).toList(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    public ResumenEntregasResponse resumen(int dias) {
        int ventana = Math.min(Math.max(dias, 1), 90);
        Map<Canal, Map<EstadoEntrega, Long>> acumulado = new EnumMap<>(Canal.class);
        for (Canal canal : Canal.values()) {
            acumulado.put(canal, new EnumMap<>(EstadoEntrega.class));
        }
        for (Object[] fila : entregas.resumen(LocalDateTime.now().minusDays(ventana))) {
            acumulado.get((Canal) fila[0]).put((EstadoEntrega) fila[1], (Long) fila[2]);
        }
        List<ResumenCanalResponse> canales = acumulado.entrySet().stream()
                .map(e -> new ResumenCanalResponse(
                        e.getKey(),
                        e.getValue(),
                        e.getValue().values().stream()
                                .mapToLong(Long::longValue)
                                .sum()))
                .toList();
        return new ResumenEntregasResponse(
                ventana, canales.stream().mapToLong(ResumenCanalResponse::total).sum(), canales);
    }

    @Transactional
    public EntregaResponse reintentar(UUID id) {
        EntregaNotificacion entrega =
                entregas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entrega", "id", id));
        if (entrega.getEstado() != EstadoEntrega.FALLIDO) {
            throw new ConflictException("Solo se pueden reintentar las entregas fallidas");
        }
        entrega.setEstado(EstadoEntrega.PENDIENTE);
        entrega.setIntentos(0);
        entrega.setMotivo(null);
        entrega.setProximoIntento(LocalDateTime.now());
        return respuesta(entregas.saveAndFlush(entrega));
    }

    public ProveedoresResponse proveedores() {
        Map<Canal, String> mapa = new EnumMap<>(Canal.class);
        mapa.put(Canal.CORREO, propiedades.correo().smtp() ? "SMTP" : "SIMULADO");
        mapa.put(Canal.SMS, propiedades.sms().simulado() ? "SIMULADO" : "HTTP");
        mapa.put(Canal.WHATSAPP, propiedades.whatsapp().simulado() ? "SIMULADO" : "HTTP");
        return new ProveedoresResponse(mapa, propiedades.workerHabilitado(), propiedades.limitePorHora());
    }

    public List<MensajeSimuladoResponse> mensajesSimulados(int limite) {
        return simulado.ultimos(Math.min(Math.max(limite, 1), 100)).stream()
                .map(m -> new MensajeSimuladoResponse(m.id(), m.canal(), m.destino(), m.asunto(), m.texto(), m.fecha()))
                .toList();
    }

    public void vaciarMensajesSimulados() {
        simulado.vaciar();
    }

    static EntregaResponse respuesta(EntregaNotificacion e) {
        return new EntregaResponse(
                e.getId(),
                e.getTipo(),
                e.getCanal(),
                e.getDestinoMascara(),
                e.getEstado(),
                e.getIntentos(),
                e.getProximoIntento(),
                e.getMotivo(),
                e.getPlantillaVersion(),
                e.isUrgente(),
                e.getCreadoEn(),
                e.getEnviadoEn(),
                e.getEntregadoEn());
    }
}

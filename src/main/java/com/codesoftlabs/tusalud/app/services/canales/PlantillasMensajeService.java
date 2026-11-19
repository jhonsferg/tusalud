package com.codesoftlabs.tusalud.app.services.canales;

import com.codesoftlabs.tusalud.app.canales.PlantillasPorDefecto;
import com.codesoftlabs.tusalud.app.canales.PoliticaCanales;
import com.codesoftlabs.tusalud.app.canales.RenderizadorMensaje;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.PlantillaMensajeRequest;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.PlantillaMensajeResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.PlantillasMensajeResponse;
import com.codesoftlabs.tusalud.app.entities.PlantillaMensaje;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.PlantillaMensajeRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlantillasMensajeService {

    private static final int MAXIMO_MENSAJERIA = 320;

    public record Efectiva(String asunto, String cuerpo, int version) {}

    private final PlantillaMensajeRepository plantillas;
    private final UsuarioRepository usuarios;

    public PlantillasMensajeResponse listar() {
        Map<String, PlantillaMensaje> activas = new HashMap<>();
        plantillas.findByActivaTrue().forEach(p -> activas.put(p.getTipo() + ":" + p.getCanal(), p));
        List<PlantillaMensajeResponse> respuesta = PoliticaCanales.tiposExternos().stream()
                .sorted(Comparator.comparing((TipoNotificacion t) ->
                                PoliticaCanales.de(t).orElseThrow().categoria())
                        .thenComparing(t -> PoliticaCanales.de(t).orElseThrow().etiqueta()))
                .flatMap(tipo ->
                        Stream.of(Canal.values()).map(canal -> respuesta(tipo, canal, activas.get(tipo + ":" + canal))))
                .toList();
        return new PlantillasMensajeResponse(
                RenderizadorMensaje.VARIABLES.stream().sorted().toList(), respuesta);
    }

    public Efectiva efectiva(TipoNotificacion tipo, Canal canal) {
        PlantillaMensaje personalizada =
                plantillas.findByTipoAndCanalAndActivaTrue(tipo, canal).orElse(null);
        if (personalizada != null) {
            return new Efectiva(personalizada.getAsunto(), personalizada.getCuerpo(), personalizada.getVersion());
        }
        PlantillasPorDefecto.Contenido defecto = PlantillasPorDefecto.de(tipo, canal);
        if (defecto == null) {
            throw new ResourceNotFoundException("Plantilla de mensaje", "tipo", tipo);
        }
        return new Efectiva(defecto.asunto(), defecto.cuerpo(), 0);
    }

    @Transactional
    public PlantillaMensajeResponse actualizar(
            UsuarioPrincipal actor, TipoNotificacion tipo, Canal canal, PlantillaMensajeRequest request) {
        exigirExterno(tipo);
        String cuerpo = request.cuerpo().strip();
        String asunto = request.asunto() == null ? null : request.asunto().strip();
        if (canal == Canal.CORREO) {
            if (asunto == null || asunto.isBlank()) {
                throw new DatosInvalidosException("El correo necesita un asunto");
            }
        } else {
            asunto = null;
            if (cuerpo.length() > MAXIMO_MENSAJERIA) {
                throw new DatosInvalidosException(
                        "El mensaje no puede superar los " + MAXIMO_MENSAJERIA + " caracteres");
            }
        }
        List<String> desconocidas = Stream.of(asunto, cuerpo)
                .filter(t -> t != null)
                .flatMap(t -> RenderizadorMensaje.variablesDesconocidas(t).stream())
                .distinct()
                .toList();
        if (!desconocidas.isEmpty()) {
            throw new DatosInvalidosException("Variables no permitidas: " + String.join(", ", desconocidas)
                    + ". Use solo "
                    + String.join(
                            ", ",
                            RenderizadorMensaje.VARIABLES.stream().sorted().toList()));
        }
        int siguiente = plantillas.findByTipoAndCanalOrderByVersionDesc(tipo, canal).stream()
                        .findFirst()
                        .map(PlantillaMensaje::getVersion)
                        .orElse(0)
                + 1;
        plantillas.findByTipoAndCanalAndActivaTrue(tipo, canal).ifPresent(anterior -> {
            anterior.setActiva(false);
            plantillas.saveAndFlush(anterior);
        });
        PlantillaMensaje nueva = plantillas.saveAndFlush(PlantillaMensaje.builder()
                .tipo(tipo)
                .canal(canal)
                .version(siguiente)
                .asunto(asunto)
                .cuerpo(cuerpo)
                .activa(true)
                .motivo(request.motivo().strip())
                .creadoPor(usuarios.getReferenceById(actor.getId()))
                .build());
        return respuesta(tipo, canal, nueva);
    }

    @Transactional
    public PlantillaMensajeResponse restaurar(TipoNotificacion tipo, Canal canal) {
        exigirExterno(tipo);
        plantillas.findByTipoAndCanalAndActivaTrue(tipo, canal).ifPresent(activa -> {
            activa.setActiva(false);
            plantillas.saveAndFlush(activa);
        });
        return respuesta(tipo, canal, null);
    }

    public List<PlantillaMensajeResponse> historial(TipoNotificacion tipo, Canal canal) {
        exigirExterno(tipo);
        return plantillas.findByTipoAndCanalOrderByVersionDesc(tipo, canal).stream()
                .map(p -> respuesta(tipo, canal, p))
                .toList();
    }

    private PlantillaMensajeResponse respuesta(TipoNotificacion tipo, Canal canal, PlantillaMensaje personalizada) {
        var politica = PoliticaCanales.de(tipo).orElseThrow();
        if (personalizada != null) {
            return new PlantillaMensajeResponse(
                    tipo,
                    politica.etiqueta(),
                    politica.categoria(),
                    canal,
                    personalizada.getAsunto(),
                    personalizada.getCuerpo(),
                    personalizada.getVersion(),
                    true,
                    personalizada.getCreadoEn(),
                    personalizada.getCreadoPor() == null
                            ? null
                            : personalizada.getCreadoPor().getNombreCompleto(),
                    personalizada.getMotivo());
        }
        PlantillasPorDefecto.Contenido defecto = PlantillasPorDefecto.de(tipo, canal);
        return new PlantillaMensajeResponse(
                tipo,
                politica.etiqueta(),
                politica.categoria(),
                canal,
                defecto.asunto(),
                defecto.cuerpo(),
                0,
                false,
                null,
                null,
                null);
    }

    private static void exigirExterno(TipoNotificacion tipo) {
        if (!PoliticaCanales.externa(tipo)) {
            throw new DatosInvalidosException("El tipo " + tipo + " no se envía por canales externos");
        }
    }
}

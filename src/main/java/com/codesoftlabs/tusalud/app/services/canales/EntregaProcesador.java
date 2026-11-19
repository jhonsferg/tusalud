package com.codesoftlabs.tusalud.app.services.canales;

import com.codesoftlabs.tusalud.app.canales.CanalNotificacion;
import com.codesoftlabs.tusalud.app.canales.EnvioException;
import com.codesoftlabs.tusalud.app.canales.FirmasCanales;
import com.codesoftlabs.tusalud.app.canales.MensajeSaliente;
import com.codesoftlabs.tusalud.app.canales.PoliticaCanales;
import com.codesoftlabs.tusalud.app.canales.RenderizadorMensaje;
import com.codesoftlabs.tusalud.app.config.CanalesProperties;
import com.codesoftlabs.tusalud.app.entities.ContactoCanal;
import com.codesoftlabs.tusalud.app.entities.EntregaNotificacion;
import com.codesoftlabs.tusalud.app.entities.PreferenciaCanal;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.repositories.ContactoCanalRepository;
import com.codesoftlabs.tusalud.app.repositories.EntregaNotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.PreferenciaCanalRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntregaProcesador {

    private final EntregaNotificacionRepository entregas;
    private final ContactoCanalRepository contactos;
    private final PreferenciaCanalRepository preferencias;
    private final UsuarioRepository usuarios;
    private final PlantillasMensajeService plantillas;
    private final CanalesProperties propiedades;
    private final List<CanalNotificacion> canales;

    @Transactional
    public EstadoEntrega procesar(UUID id, LocalDateTime ahora) {
        EntregaNotificacion entrega = entregas.findById(id).orElse(null);
        if (entrega == null || entrega.getEstado() != EstadoEntrega.PENDIENTE) {
            return entrega == null ? null : entrega.getEstado();
        }
        if (entrega.getProximoIntento() != null && entrega.getProximoIntento().isAfter(ahora)) {
            return entrega.getEstado();
        }
        var politica = PoliticaCanales.de(entrega.getTipo()).orElse(null);
        ContactoCanal contacto = contactos
                .findByUsuarioIdAndCanal(entrega.getUsuarioId(), entrega.getCanal())
                .orElse(null);
        Usuario usuario = usuarios.findById(entrega.getUsuarioId()).orElse(null);
        if (politica == null || contacto == null || usuario == null || !contacto.puedeRecibir(politica.obligatoria())) {
            return cerrar(entrega, EstadoEntrega.FALLIDO, "El destino ya no está habilitado");
        }
        if (!politica.obligatoria() && !preferenciaActiva(entrega)) {
            return cerrar(entrega, EstadoEntrega.FALLIDO, "El usuario desactivó este aviso");
        }
        CanalNotificacion canal = canales.stream()
                .filter(c -> c.canal() == entrega.getCanal())
                .findFirst()
                .orElse(null);
        if (canal == null) {
            return cerrar(entrega, EstadoEntrega.FALLIDO, "El canal no está disponible");
        }
        MensajeSaliente mensaje = armar(entrega, contacto, usuario);
        entrega.setIntentos(entrega.getIntentos() + 1);
        try {
            String idExterno = canal.enviar(mensaje);
            entrega.setEstado(EstadoEntrega.ENVIADO);
            entrega.setIdExterno(idExterno);
            entrega.setEnviadoEn(ahora);
            entrega.setProximoIntento(null);
            entrega.setMotivo(null);
        } catch (EnvioException ex) {
            registrarFallo(entrega, ex, ahora);
        }
        entregas.saveAndFlush(entrega);
        return entrega.getEstado();
    }

    private MensajeSaliente armar(EntregaNotificacion entrega, ContactoCanal contacto, Usuario usuario) {
        PlantillasMensajeService.Efectiva plantilla = plantillas.efectiva(entrega.getTipo(), entrega.getCanal());
        entrega.setPlantillaVersion(plantilla.version());
        Map<String, String> valores = Map.of(
                "nombre", usuario.getNombre(),
                "enlace", propiedades.urlBase(),
                "centro", propiedades.centro());
        String cuerpo = RenderizadorMensaje.renderizar(plantilla.cuerpo(), valores);
        String enlaceBaja = propiedades.urlBase() + "/baja?t="
                + FirmasCanales.crearTokenBaja(propiedades.secreto(), usuario.getId(), entrega.getCanal());
        if (entrega.getCanal() == Canal.CORREO) {
            String asunto = RenderizadorMensaje.renderizar(plantilla.asunto(), valores);
            return new MensajeSaliente(
                    Canal.CORREO,
                    contacto.getDestino(),
                    asunto,
                    RenderizadorMensaje.textoCorreo(cuerpo, enlaceBaja),
                    RenderizadorMensaje.html(asunto, cuerpo, propiedades.centro(), enlaceBaja),
                    enlaceBaja,
                    entrega.getClaveIdempotencia());
        }
        return new MensajeSaliente(
                entrega.getCanal(),
                contacto.getDestino(),
                null,
                RenderizadorMensaje.textoMensajeria(cuerpo, enlaceBaja),
                null,
                enlaceBaja,
                entrega.getClaveIdempotencia());
    }

    private void registrarFallo(EntregaNotificacion entrega, EnvioException ex, LocalDateTime ahora) {
        entrega.setMotivo(recortar(ex.getMessage()));
        if (ex.permanente() || entrega.getIntentos() >= propiedades.reintentos().maximo()) {
            entrega.setEstado(EstadoEntrega.FALLIDO);
            entrega.setProximoIntento(null);
            log.warn("Entrega {} fallida tras {} intentos", entrega.getId(), entrega.getIntentos());
            return;
        }
        long espera = Math.min(
                (long) propiedades.reintentos().techoSeg(),
                (long) propiedades.reintentos().baseSeg() * (1L << (entrega.getIntentos() - 1)));
        entrega.setProximoIntento(ahora.plusSeconds(espera));
    }

    private boolean preferenciaActiva(EntregaNotificacion entrega) {
        return preferencias.findByUsuarioId(entrega.getUsuarioId()).stream()
                .filter(p -> p.getTipo() == entrega.getTipo() && p.getCanal() == entrega.getCanal())
                .findFirst()
                .map(PreferenciaCanal::isActivo)
                .orElse(PoliticaCanales.activoPorDefecto(entrega.getTipo(), entrega.getCanal()));
    }

    private EstadoEntrega cerrar(EntregaNotificacion entrega, EstadoEntrega estado, String motivo) {
        entrega.setEstado(estado);
        entrega.setMotivo(motivo);
        entrega.setProximoIntento(null);
        entregas.saveAndFlush(entrega);
        return estado;
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() > 200 ? texto.substring(0, 200) : texto;
    }
}

package com.codesoftlabs.tusalud.app.services.canales;

import com.codesoftlabs.tusalud.app.canales.CanalNotificacion;
import com.codesoftlabs.tusalud.app.canales.EnmascaradorContacto;
import com.codesoftlabs.tusalud.app.canales.EnvioException;
import com.codesoftlabs.tusalud.app.canales.FirmasCanales;
import com.codesoftlabs.tusalud.app.canales.MensajeSaliente;
import com.codesoftlabs.tusalud.app.canales.PoliticaCanales;
import com.codesoftlabs.tusalud.app.canales.RenderizadorMensaje;
import com.codesoftlabs.tusalud.app.config.CanalesProperties;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.PreferenciaCanalRequest;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.PreferenciasCanalesRequest;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.SolicitudContactoRequest;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.ContactoCanalResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.MisCanalesResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.SilencioResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.TipoPreferenciaResponse;
import com.codesoftlabs.tusalud.app.entities.ConfiguracionCanales;
import com.codesoftlabs.tusalud.app.entities.ContactoCanal;
import com.codesoftlabs.tusalud.app.entities.EntregaNotificacion;
import com.codesoftlabs.tusalud.app.entities.PreferenciaCanal;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoContacto;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.DemasiadosIntentosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.ConfiguracionCanalesRepository;
import com.codesoftlabs.tusalud.app.repositories.ContactoCanalRepository;
import com.codesoftlabs.tusalud.app.repositories.EntregaNotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.PreferenciaCanalRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactosCanalService {

    private static final Pattern TELEFONO = Pattern.compile("^\\+\\d{9,15}$");
    private static final Pattern TELEFONO_PERU = Pattern.compile("^9\\d{8}$");
    private static final Pattern TELEFONO_SIN_MAS = Pattern.compile("^\\d{10,15}$");
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final ContactoCanalRepository contactos;
    private final PreferenciaCanalRepository preferencias;
    private final ConfiguracionCanalesRepository configuraciones;
    private final UsuarioRepository usuarios;
    private final EntregaNotificacionRepository entregas;
    private final CanalesProperties propiedades;
    private final List<CanalNotificacion> canales;

    public MisCanalesResponse mis(UsuarioPrincipal actor) {
        Usuario usuario = cargar(actor.getId());
        Map<Canal, ContactoCanal> porCanal = new EnumMap<>(Canal.class);
        contactos.findByUsuarioId(usuario.getId()).forEach(c -> porCanal.put(c.getCanal(), c));
        List<ContactoCanalResponse> respuestaCanales = java.util.Arrays.stream(Canal.values())
                .map(canal -> contactoResponse(canal, porCanal.get(canal), usuario))
                .toList();

        Map<String, Boolean> guardadas = new LinkedHashMap<>();
        preferencias
                .findByUsuarioId(usuario.getId())
                .forEach(p -> guardadas.put(p.getTipo() + ":" + p.getCanal(), p.isActivo()));
        List<TipoPreferenciaResponse> respuestaPreferencias = PoliticaCanales.tiposExternos().stream()
                .map(tipo -> {
                    var politica = PoliticaCanales.de(tipo).orElseThrow();
                    Map<Canal, Boolean> activos = new EnumMap<>(Canal.class);
                    for (Canal canal : Canal.values()) {
                        activos.put(
                                canal,
                                politica.obligatoria()
                                        || guardadas.getOrDefault(
                                                tipo + ":" + canal, PoliticaCanales.activoPorDefecto(tipo, canal)));
                    }
                    return new TipoPreferenciaResponse(
                            tipo, politica.etiqueta(), politica.categoria(), politica.obligatoria(), activos);
                })
                .sorted(Comparator.comparing(TipoPreferenciaResponse::categoria)
                        .thenComparing(TipoPreferenciaResponse::etiqueta))
                .toList();

        ConfiguracionCanales configuracion =
                configuraciones.findByUsuarioId(usuario.getId()).orElse(null);
        SilencioResponse silencio = configuracion == null
                ? new SilencioResponse(true, ConfiguracionCanales.SILENCIO_DESDE, ConfiguracionCanales.SILENCIO_HASTA)
                : new SilencioResponse(
                        configuracion.isSilencioActivo(),
                        configuracion.getSilencioDesde(),
                        configuracion.getSilencioHasta());
        return new MisCanalesResponse(respuestaCanales, respuestaPreferencias, silencio);
    }

    @Transactional
    public ContactoCanalResponse solicitar(UsuarioPrincipal actor, Canal canal, SolicitudContactoRequest request) {
        if (!request.consentimiento()) {
            throw new DatosInvalidosException("Debe aceptar recibir mensajes por este canal para continuar");
        }
        Usuario usuario = cargar(actor.getId());
        String destino = destinoNormalizado(usuario, canal, request.destino());
        LocalDateTime ahora = LocalDateTime.now();
        ContactoCanal contacto = contactos
                .findByUsuarioIdAndCanal(usuario.getId(), canal)
                .orElseGet(() -> ContactoCanal.builder()
                        .usuario(usuario)
                        .canal(canal)
                        .destino(destino)
                        .build());
        if (contacto.estado() == EstadoContacto.ACTIVO && destino.equals(contacto.getDestino())) {
            throw new ConflictException("Este canal ya está activo");
        }
        limitarReenvios(contacto, ahora);

        String codigo = String.format("%06d", ALEATORIO.nextInt(1_000_000));
        contacto.setDestino(destino);
        contacto.setConsentimientoEn(ahora);
        contacto.setVerificadoEn(null);
        contacto.setBajaEn(null);
        contacto.setRebotadoEn(null);
        contacto.setCodigoHash(hashCodigo(usuario.getId(), canal, codigo));
        contacto.setCodigoExpira(ahora.plusMinutes(propiedades.codigo().ttlMin()));
        contacto.setIntentosCodigo(0);
        contacto.setUltimoEnvioCodigo(ahora);
        contacto.setActualizadoEn(ahora);
        ContactoCanal guardado = contactos.saveAndFlush(contacto);

        enviarCodigo(usuario, guardado, codigo, ahora);
        log.info("Código de verificación enviado por {} al usuario {}", canal, usuario.getId());
        return contactoResponse(canal, guardado, usuario);
    }

    @Transactional(noRollbackFor = {DatosInvalidosException.class, DemasiadosIntentosException.class})
    public ContactoCanalResponse verificar(UsuarioPrincipal actor, Canal canal, String codigo) {
        Usuario usuario = cargar(actor.getId());
        ContactoCanal contacto = contactos
                .findByUsuarioIdAndCanal(usuario.getId(), canal)
                .filter(c -> c.getCodigoHash() != null)
                .orElseThrow(() -> new DatosInvalidosException("No hay una verificación pendiente para este canal"));
        LocalDateTime ahora = LocalDateTime.now();
        if (contacto.getCodigoExpira() == null || contacto.getCodigoExpira().isBefore(ahora)) {
            contacto.setCodigoHash(null);
            throw new DatosInvalidosException("El código venció. Solicite uno nuevo");
        }
        if (contacto.getIntentosCodigo() >= propiedades.codigo().maxIntentos()) {
            contacto.setCodigoHash(null);
            throw new DemasiadosIntentosException("Demasiados intentos. Solicite un código nuevo");
        }
        if (!FirmasCanales.iguales(contacto.getCodigoHash(), hashCodigo(usuario.getId(), canal, codigo))) {
            contacto.setIntentosCodigo(contacto.getIntentosCodigo() + 1);
            throw new DatosInvalidosException("El código no es correcto");
        }
        contacto.setVerificadoEn(ahora);
        contacto.setCodigoHash(null);
        contacto.setCodigoExpira(null);
        contacto.setIntentosCodigo(0);
        contacto.setActualizadoEn(ahora);
        return contactoResponse(canal, contacto, usuario);
    }

    @Transactional
    public ContactoCanalResponse revocar(UsuarioPrincipal actor, Canal canal) {
        Usuario usuario = cargar(actor.getId());
        darDeBaja(usuario.getId(), canal);
        return contactoResponse(
                canal, contactos.findByUsuarioIdAndCanal(usuario.getId(), canal).orElse(null), usuario);
    }

    @Transactional
    public boolean darDeBaja(UUID usuarioId, Canal canal) {
        ContactoCanal contacto =
                contactos.findByUsuarioIdAndCanal(usuarioId, canal).orElse(null);
        if (contacto == null) {
            return false;
        }
        contacto.setBajaEn(LocalDateTime.now());
        contacto.setConsentimientoEn(null);
        contacto.setVerificadoEn(null);
        contacto.setCodigoHash(null);
        contacto.setCodigoExpira(null);
        contacto.setActualizadoEn(LocalDateTime.now());
        return true;
    }

    @Transactional
    public MisCanalesResponse actualizarPreferencias(UsuarioPrincipal actor, PreferenciasCanalesRequest request) {
        Usuario usuario = cargar(actor.getId());
        if (request.preferencias() != null) {
            Map<String, PreferenciaCanal> existentes = new LinkedHashMap<>();
            preferencias
                    .findByUsuarioId(usuario.getId())
                    .forEach(p -> existentes.put(p.getTipo() + ":" + p.getCanal(), p));
            for (PreferenciaCanalRequest item : request.preferencias()) {
                var politica = PoliticaCanales.de(item.tipo())
                        .orElseThrow(() -> new DatosInvalidosException(
                                "El tipo " + item.tipo() + " no se envía por canales externos"));
                if (politica.obligatoria() && !item.activo()) {
                    throw new ConflictException("Las alertas de seguridad no se pueden desactivar");
                }
                PreferenciaCanal existente = existentes.get(item.tipo() + ":" + item.canal());
                if (existente == null) {
                    preferencias.save(PreferenciaCanal.builder()
                            .usuario(usuario)
                            .tipo(item.tipo())
                            .canal(item.canal())
                            .activo(item.activo())
                            .build());
                } else {
                    existente.setActivo(item.activo());
                }
            }
        }
        if (request.silencio() != null) {
            if (request.silencio().desde().equals(request.silencio().hasta())) {
                throw new DatosInvalidosException("La ventana de silencio debe tener una hora de inicio y otra de fin");
            }
            ConfiguracionCanales configuracion = configuraciones
                    .findByUsuarioId(usuario.getId())
                    .orElseGet(() ->
                            ConfiguracionCanales.builder().usuario(usuario).build());
            configuracion.setSilencioActivo(request.silencio().activo());
            configuracion.setSilencioDesde(request.silencio().desde());
            configuracion.setSilencioHasta(request.silencio().hasta());
            configuraciones.save(configuracion);
        }
        preferencias.flush();
        return mis(actor);
    }

    private void limitarReenvios(ContactoCanal contacto, LocalDateTime ahora) {
        if (contacto.getUltimoEnvioCodigo() != null
                && Duration.between(contacto.getUltimoEnvioCodigo(), ahora).getSeconds()
                        < propiedades.codigo().esperaSeg()) {
            throw new DemasiadosIntentosException("Espere un momento antes de pedir otro código");
        }
        if (contacto.getVentanaEnviosDesde() == null
                || Duration.between(contacto.getVentanaEnviosDesde(), ahora).toMinutes() >= 60) {
            contacto.setVentanaEnviosDesde(ahora);
            contacto.setEnviosCodigoHora(0);
        }
        if (contacto.getEnviosCodigoHora() >= propiedades.codigo().reenviosPorHora()) {
            throw new DemasiadosIntentosException("Superó el máximo de códigos por hora. Intente más tarde");
        }
        contacto.setEnviosCodigoHora(contacto.getEnviosCodigoHora() + 1);
    }

    private void enviarCodigo(Usuario usuario, ContactoCanal contacto, String codigo, LocalDateTime ahora) {
        CanalNotificacion canal = canales.stream()
                .filter(c -> c.canal() == contacto.getCanal())
                .findFirst()
                .orElseThrow(() -> new BusinessLogicException("El canal no está disponible"));
        String enlaceBaja = propiedades.urlBase() + "/baja?t="
                + FirmasCanales.crearTokenBaja(propiedades.secreto(), usuario.getId(), contacto.getCanal());
        String cuerpo = "Hola " + usuario.getNombre() + ",\n\nSu código de verificación de " + propiedades.centro()
                + " es " + codigo + ". Vence en " + propiedades.codigo().ttlMin()
                + " minutos. No lo comparta con nadie.";
        String asunto = "Su código de verificación";
        MensajeSaliente mensaje = contacto.getCanal() == Canal.CORREO
                ? new MensajeSaliente(
                        Canal.CORREO,
                        contacto.getDestino(),
                        asunto,
                        RenderizadorMensaje.textoCorreo(cuerpo, enlaceBaja),
                        RenderizadorMensaje.html(asunto, cuerpo, propiedades.centro(), enlaceBaja),
                        enlaceBaja,
                        "verificacion:" + contacto.getId() + ":" + ahora)
                : new MensajeSaliente(
                        contacto.getCanal(),
                        contacto.getDestino(),
                        asunto,
                        propiedades.centro() + ": su código de verificación es " + codigo + ". Vence en "
                                + propiedades.codigo().ttlMin() + " minutos. No lo comparta.",
                        null,
                        enlaceBaja,
                        "verificacion:" + contacto.getId() + ":" + ahora);
        try {
            String idExterno = canal.enviar(mensaje);
            entregas.save(EntregaNotificacion.builder()
                    .usuarioId(usuario.getId())
                    .canal(contacto.getCanal())
                    .tipo(TipoNotificacion.SISTEMA)
                    .destinoMascara(EnmascaradorContacto.enmascarar(contacto.getCanal(), contacto.getDestino()))
                    .estado(EstadoEntrega.ENVIADO)
                    .intentos(1)
                    .idExterno(idExterno)
                    .claveIdempotencia(mensaje.claveIdempotencia())
                    .enviadoEn(ahora)
                    .urgente(true)
                    .build());
        } catch (EnvioException ex) {
            log.warn(
                    "No se pudo enviar el código por {} al usuario {}: {}",
                    contacto.getCanal(),
                    usuario.getId(),
                    ex.getMessage());
            throw new BusinessLogicException("No pudimos enviar el código por este canal. Intente más tarde");
        }
    }

    private String destinoNormalizado(Usuario usuario, Canal canal, String solicitado) {
        if (canal == Canal.CORREO) {
            if (solicitado != null
                    && !solicitado.isBlank()
                    && !solicitado.trim().equalsIgnoreCase(usuario.getEmail())) {
                throw new DatosInvalidosException("El correo del canal es el de su cuenta y no se puede cambiar aquí");
            }
            return usuario.getEmail();
        }
        if (solicitado == null || solicitado.isBlank()) {
            throw new DatosInvalidosException("Indique el número de teléfono");
        }
        String limpio = solicitado.replaceAll("[\\s\\-().]", "");
        if (TELEFONO_PERU.matcher(limpio).matches()) {
            limpio = "+51" + limpio;
        } else if (TELEFONO_SIN_MAS.matcher(limpio).matches()) {
            limpio = "+" + limpio;
        }
        if (!TELEFONO.matcher(limpio).matches()) {
            throw new DatosInvalidosException("El número de teléfono no es válido. Use el formato +51987654321");
        }
        return limpio;
    }

    private ContactoCanalResponse contactoResponse(Canal canal, ContactoCanal contacto, Usuario usuario) {
        if (contacto == null) {
            String destino = canal == Canal.CORREO ? usuario.getEmail() : null;
            return new ContactoCanalResponse(
                    canal,
                    canal.etiqueta(),
                    EnmascaradorContacto.enmascarar(canal, destino),
                    EstadoContacto.SIN_CONFIGURAR,
                    null,
                    null,
                    canal == Canal.CORREO);
        }
        return new ContactoCanalResponse(
                canal,
                canal.etiqueta(),
                EnmascaradorContacto.enmascarar(canal, contacto.getDestino()),
                contacto.estado(),
                contacto.getConsentimientoEn(),
                contacto.getVerificadoEn(),
                canal == Canal.CORREO);
    }

    private String hashCodigo(UUID usuarioId, Canal canal, String codigo) {
        return FirmasCanales.hmacHex(
                propiedades.secreto(),
                (usuarioId + ":" + canal + ":" + codigo).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Usuario cargar(UUID id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }
}

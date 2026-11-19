package com.codesoftlabs.tusalud.app.canales;

import com.codesoftlabs.tusalud.app.config.CanalesProperties;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class CanalCorreo implements CanalNotificacion {

    private final CanalesProperties propiedades;
    private final ObjectProvider<JavaMailSender> remitente;
    private final ProveedorSimulado simulado;

    public CanalCorreo(
            CanalesProperties propiedades, ObjectProvider<JavaMailSender> remitente, ProveedorSimulado simulado) {
        this.propiedades = propiedades;
        this.remitente = remitente;
        this.simulado = simulado;
    }

    @Override
    public Canal canal() {
        return Canal.CORREO;
    }

    @Override
    public String enviar(MensajeSaliente mensaje) {
        if (!propiedades.correo().smtp()) {
            return simulado.enviar(mensaje);
        }
        JavaMailSender sender = remitente.getIfAvailable();
        if (sender == null) {
            throw new EnvioException("El servidor de correo no está configurado", false);
        }
        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper ayudante = new MimeMessageHelper(mime, true, "UTF-8");
            ayudante.setFrom(new InternetAddress(
                    propiedades.correo().remitente(), propiedades.correo().nombreRemitente(), "UTF-8"));
            ayudante.setTo(mensaje.destino());
            ayudante.setSubject(mensaje.asunto());
            ayudante.setText(mensaje.texto(), mensaje.html());
            mime.setHeader("List-Unsubscribe", "<" + mensaje.enlaceBaja() + ">");
            mime.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
            mime.setHeader("Auto-Submitted", "auto-generated");
            sender.send(mime);
            String id = mime.getMessageID();
            return id == null ? "smtp-" + mensaje.claveIdempotencia() : id;
        } catch (MailParseException ex) {
            throw new EnvioException("La dirección de correo no es válida", true, ex);
        } catch (MailSendException ex) {
            throw new EnvioException("El servidor de correo rechazó el envío", esDireccionInvalida(ex), ex);
        } catch (MailException ex) {
            throw new EnvioException("No se pudo enviar el correo", false, ex);
        } catch (MessagingException | UnsupportedEncodingException ex) {
            throw new EnvioException("No se pudo armar el correo", true, ex);
        }
    }

    private static boolean esDireccionInvalida(MailSendException ex) {
        for (Exception causa : ex.getMessageExceptions()) {
            if (causa instanceof SendFailedException fallo) {
                Address[] invalidas = fallo.getInvalidAddresses();
                if (invalidas != null && invalidas.length > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}

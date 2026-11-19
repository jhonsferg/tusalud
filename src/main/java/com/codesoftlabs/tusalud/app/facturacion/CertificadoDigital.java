package com.codesoftlabs.tusalud.app.facturacion;

import com.codesoftlabs.tusalud.app.config.FacturacionProperties;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CertificadoDigital {

    public record Credencial(PrivateKey clavePrivada, X509Certificate certificado, boolean dePrueba) {}

    private final FacturacionProperties propiedades;
    private volatile Credencial credencial;

    public CertificadoDigital(FacturacionProperties propiedades) {
        this.propiedades = propiedades;
    }

    public Credencial credencial() {
        Credencial actual = credencial;
        if (actual == null) {
            synchronized (this) {
                actual = credencial;
                if (actual == null) {
                    actual = cargar();
                    credencial = actual;
                }
            }
        }
        return actual;
    }

    private Credencial cargar() {
        FacturacionProperties.Certificado config = propiedades.certificado();
        if (config.ruta() != null && !config.ruta().isBlank()) {
            return desdeArchivo(config);
        }
        if (!config.generarDePrueba()) {
            throw new IllegalStateException(
                    "No hay certificado digital configurado. Defina FACTURACION_CERTIFICADO_RUTA y FACTURACION_CERTIFICADO_CLAVE");
        }
        log.warn("Se usa un certificado digital de prueba autofirmado. No es válido ante SUNAT");
        return generarDePrueba();
    }

    private Credencial desdeArchivo(FacturacionProperties.Certificado config) {
        try (InputStream flujo = Files.newInputStream(Path.of(config.ruta()))) {
            char[] clave = config.clave() == null ? new char[0] : config.clave().toCharArray();
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            almacen.load(flujo, clave);
            String alias = config.alias();
            if (alias == null || alias.isBlank()) {
                Enumeration<String> alias0 = almacen.aliases();
                while (alias0.hasMoreElements()) {
                    String candidato = alias0.nextElement();
                    if (almacen.isKeyEntry(candidato)) {
                        alias = candidato;
                        break;
                    }
                }
            }
            PrivateKey privada = (PrivateKey) almacen.getKey(alias, clave);
            X509Certificate certificado = (X509Certificate) almacen.getCertificate(alias);
            if (privada == null || certificado == null) {
                throw new IllegalStateException("El almacén de certificados no contiene una clave privada");
            }
            return new Credencial(privada, certificado, false);
        } catch (java.io.IOException | java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo cargar el certificado digital", ex);
        }
    }

    private Credencial generarDePrueba() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            KeyPair par = generador.generateKeyPair();
            X500Name nombre = new X500Name("CN=TuSalud Certificado de Prueba, O=TuSalud, C=PE");
            Date desde = new Date(System.currentTimeMillis() - 3_600_000L);
            Date hasta = new Date(System.currentTimeMillis() + 365L * 24 * 3_600_000L);
            var constructor = new JcaX509v3CertificateBuilder(
                    nombre, BigInteger.valueOf(System.nanoTime()), desde, hasta, nombre, par.getPublic());
            var firmante = new JcaContentSignerBuilder("SHA256withRSA").build(par.getPrivate());
            X509Certificate certificado = new JcaX509CertificateConverter().getCertificate(constructor.build(firmante));
            return new Credencial(par.getPrivate(), certificado, true);
        } catch (java.security.GeneralSecurityException | org.bouncycastle.operator.OperatorCreationException ex) {
            throw new IllegalStateException("No se pudo generar el certificado de prueba", ex);
        }
    }
}

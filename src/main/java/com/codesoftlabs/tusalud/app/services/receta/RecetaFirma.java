package com.codesoftlabs.tusalud.app.services.receta;

import com.codesoftlabs.tusalud.app.documentos.CodigoDocumentoClinico;
import com.codesoftlabs.tusalud.app.facturacion.CertificadoDigital;
import com.codesoftlabs.tusalud.app.facturacion.CertificadoDigital.Credencial;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecetaFirma {

    public static final String ALGORITMO = "SHA256withRSA";

    public record Firmada(
            String firma, String algoritmo, String huella, String sujeto, String certificadoDer, boolean dePrueba) {}

    private final CertificadoDigital certificado;

    public Firmada firmar(String contenidoCanonico) {
        Credencial credencial = certificado.credencial();
        try {
            Signature firma = Signature.getInstance(ALGORITMO);
            firma.initSign(credencial.clavePrivada());
            firma.update(contenidoCanonico.getBytes(StandardCharsets.UTF_8));
            X509Certificate cert = credencial.certificado();
            return new Firmada(
                    Base64.getEncoder().encodeToString(firma.sign()),
                    ALGORITMO,
                    CodigoDocumentoClinico.sha256(cert.getEncoded()),
                    cert.getSubjectX500Principal().getName(),
                    Base64.getEncoder().encodeToString(cert.getEncoded()),
                    credencial.dePrueba());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo firmar la receta", ex);
        }
    }

    public boolean verificar(String contenidoCanonico, String firmaBase64, String certificadoDer) {
        try {
            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(
                            new ByteArrayInputStream(Base64.getDecoder().decode(certificadoDer)));
            Signature firma = Signature.getInstance(ALGORITMO);
            firma.initVerify(cert.getPublicKey());
            firma.update(contenidoCanonico.getBytes(StandardCharsets.UTF_8));
            return firma.verify(Base64.getDecoder().decode(firmaBase64));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            return false;
        }
    }
}

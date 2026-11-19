package com.codesoftlabs.tusalud.app.facturacion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class XmlFirmador {

    public static final String NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    public static final String ID_FIRMA = "SignatureSP";

    public record XmlFirmado(byte[] contenido, String digest) {}

    public XmlFirmado firmar(byte[] xml, CertificadoDigital.Credencial credencial) {
        try {
            Document documento = analizar(xml);
            NodeList contenidos = documento.getElementsByTagNameNS(NS_EXT, "ExtensionContent");
            if (contenidos.getLength() == 0) {
                throw new IllegalStateException("El documento no tiene espacio para la firma digital");
            }
            Element destino = (Element) contenidos.item(0);
            XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");
            Reference referencia = fabrica.newReference(
                    "",
                    fabrica.newDigestMethod(DigestMethod.SHA256, null),
                    Collections.singletonList(fabrica.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null,
                    null);
            SignedInfo info = fabrica.newSignedInfo(
                    fabrica.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fabrica.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
                    Collections.singletonList(referencia));
            KeyInfoFactory fabricaClaves = fabrica.getKeyInfoFactory();
            X509Data datos = fabricaClaves.newX509Data(List.of(credencial.certificado()));
            KeyInfo claves = fabricaClaves.newKeyInfo(Collections.singletonList(datos));
            DOMSignContext contexto = new DOMSignContext(credencial.clavePrivada(), destino);
            contexto.setDefaultNamespacePrefix("ds");
            XMLSignature firma = fabrica.newXMLSignature(info, claves, null, ID_FIRMA, null);
            firma.sign(contexto);
            String digest = Base64.getEncoder()
                    .encodeToString(firma.getSignedInfo().getReferences().get(0).getDigestValue());
            return new XmlFirmado(serializar(documento), digest);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el documento", ex);
        }
    }

    public boolean verificar(byte[] xml) {
        try {
            Document documento = analizar(xml);
            NodeList firmas = documento.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (firmas.getLength() != 1) {
                return false;
            }
            DOMValidateContext contexto = new DOMValidateContext(new SelectorCertificado(), firmas.item(0));
            XMLSignature firma = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(contexto);
            return firma.validate(contexto);
        } catch (Exception ex) {
            return false;
        }
    }

    public X509Certificate certificadoDe(byte[] xml) {
        try {
            Document documento = analizar(xml);
            NodeList nodos = documento.getElementsByTagNameNS(XMLSignature.XMLNS, "X509Certificate");
            if (nodos.getLength() == 0) {
                return null;
            }
            byte[] der = Base64.getMimeDecoder()
                    .decode(nodos.item(0).getTextContent().trim());
            return (X509Certificate) java.security.cert.CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
        } catch (Exception ex) {
            return null;
        }
    }

    public static Document analizar(byte[] xml) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);
        return fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    public static byte[] serializar(Node documento) throws Exception {
        var transformador = TransformerFactory.newInstance().newTransformer();
        transformador.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformador.setOutputProperty(OutputKeys.INDENT, "no");
        transformador.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        transformador.transform(new DOMSource(documento), new StreamResult(salida));
        return salida.toByteArray();
    }

    private static final class SelectorCertificado extends KeySelector {

        @Override
        public KeySelectorResult select(
                KeyInfo info, Purpose proposito, AlgorithmMethod metodo, XMLCryptoContext contexto)
                throws KeySelectorException {
            if (info == null) {
                throw new KeySelectorException("La firma no incluye el certificado");
            }
            for (Object elemento : info.getContent()) {
                if (elemento instanceof X509Data datos) {
                    for (Object contenido : datos.getContent()) {
                        if (contenido instanceof X509Certificate certificado) {
                            PublicKey clave = certificado.getPublicKey();
                            return () -> (Key) clave;
                        }
                    }
                }
            }
            throw new KeySelectorException("No se encontró el certificado en la firma");
        }
    }
}

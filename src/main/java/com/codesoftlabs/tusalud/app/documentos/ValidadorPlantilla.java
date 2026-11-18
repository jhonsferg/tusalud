package com.codesoftlabs.tusalud.app.documentos;

import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class ValidadorPlantilla {

    public static final int TAMANO_MAXIMO = 512 * 1024;
    private static final int ELEMENTOS_MAXIMOS = 2500;
    private static final int EXPRESION_MAXIMA = 400;

    private static final Set<String> ELEMENTOS = Set.of(
            "jasperReport",
            "style",
            "parameter",
            "field",
            "variable",
            "group",
            "groupHeader",
            "groupFooter",
            "background",
            "title",
            "pageHeader",
            "columnHeader",
            "detail",
            "columnFooter",
            "pageFooter",
            "lastPageFooter",
            "summary",
            "noData",
            "band",
            "element",
            "expression",
            "printWhenExpression",
            "conditionExpression",
            "conditionalStyle",
            "groupExpression",
            "defaultValueExpression",
            "initialValueExpression",
            "description",
            "text",
            "box",
            "pen",
            "topPen",
            "leftPen",
            "bottomPen",
            "rightPen",
            "paragraph");

    private static final Set<String> TIPOS_ELEMENTO =
            Set.of("staticText", "textField", "line", "rectangle", "ellipse", "image", "frame");

    private static final Set<String> CLASES_PERMITIDAS = Set.of(
            "java.lang.String",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Double",
            "java.lang.Boolean",
            "java.math.BigDecimal",
            "java.util.Date",
            "java.awt.Image");

    private static final Set<String> ATRIBUTOS_PROHIBIDOS = Set.of(
            "scriptletClass",
            "resourceBundle",
            "formatFactoryClass",
            "whenResourceMissingType",
            "evaluationGroup",
            "resetGroup",
            "incrementGroup",
            "incrementerFactoryClass",
            "calculation",
            "hyperlinkType",
            "hyperlinkTarget",
            "anchorName");

    private static final Set<String> EXPRESIONES = Set.of(
            "expression",
            "printWhenExpression",
            "conditionExpression",
            "groupExpression",
            "defaultValueExpression",
            "initialValueExpression");

    private static final Set<String> PALABRAS = Set.of(
            "X",
            "null",
            "true",
            "false",
            "isEmpty",
            "equals",
            "length",
            "trim",
            "toUpperCase",
            "toLowerCase",
            "substring",
            "valueOf",
            "String",
            "Integer",
            "Double",
            "Long",
            "java",
            "lang",
            "new");

    private static final Pattern LITERAL = Pattern.compile("\"(?:[^\"\\\\])*\"");
    private static final Pattern REFERENCIA = Pattern.compile("\\$[FPV]\\{[A-Za-z_][A-Za-z0-9_]*\\}");
    private static final Pattern CARACTERES = Pattern.compile("[A-Za-z0-9_\\s+\\-*/%().,?:=!<>&|]*");
    private static final Pattern IDENTIFICADOR = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern SOLO_PARAMETRO = Pattern.compile("\\s*\\$P\\{[A-Za-z_][A-Za-z0-9_]*\\}\\s*");
    private static final Pattern NOMBRE_DEFINICION = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,59}");

    private ValidadorPlantilla() {}

    public static void validar(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new DatosInvalidosException("La plantilla está vacía");
        }
        if (contenido.length > TAMANO_MAXIMO) {
            throw new DatosInvalidosException("La plantilla supera los 512 KB permitidos");
        }
        Document documento = analizar(contenido);
        Element raiz = documento.getDocumentElement();
        if (!"jasperReport".equals(raiz.getLocalName() == null ? raiz.getNodeName() : raiz.getLocalName())) {
            throw new DatosInvalidosException("La plantilla debe empezar con el elemento jasperReport");
        }
        if (raiz.getNamespaceURI() != null && !raiz.getNamespaceURI().isEmpty()) {
            throw new DatosInvalidosException("La raíz de la plantilla no debe declarar un espacio de nombres");
        }
        String lenguaje = raiz.getAttribute("language");
        if (!lenguaje.isEmpty() && !"java".equals(lenguaje)) {
            throw new DatosInvalidosException("Solo se admite el lenguaje de expresiones java");
        }
        int[] contador = {0};
        recorrer(raiz, contador);
    }

    private static Document analizar(byte[] contenido) {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
            fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            fabrica.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            fabrica.setXIncludeAware(false);
            fabrica.setExpandEntityReferences(false);
            DocumentBuilder constructor = fabrica.newDocumentBuilder();
            constructor.setErrorHandler(null);
            return constructor.parse(new ByteArrayInputStream(contenido));
        } catch (Exception ex) {
            throw new DatosInvalidosException("La plantilla no es un XML válido o usa construcciones no permitidas");
        }
    }

    private static void recorrer(Element elemento, int[] contador) {
        contador[0]++;
        if (contador[0] > ELEMENTOS_MAXIMOS) {
            throw new DatosInvalidosException("La plantilla tiene demasiados elementos");
        }
        String nombre = elemento.getLocalName() == null ? elemento.getNodeName() : elemento.getLocalName();
        if (!ELEMENTOS.contains(nombre)) {
            throw new DatosInvalidosException("La plantilla usa un elemento no permitido: " + nombre);
        }
        revisarAtributos(elemento, nombre);
        if (EXPRESIONES.contains(nombre)) {
            revisarExpresion(elemento, nombre);
        }
        NodeList hijos = elemento.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node hijo = hijos.item(i);
            if (hijo.getNodeType() == Node.ELEMENT_NODE) {
                recorrer((Element) hijo, contador);
            } else if (hijo.getNodeType() == Node.ENTITY_REFERENCE_NODE
                    || hijo.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
                throw new DatosInvalidosException("La plantilla usa entidades no permitidas");
            }
        }
    }

    private static void revisarAtributos(Element elemento, String nombre) {
        NamedNodeMap atributos = elemento.getAttributes();
        for (int i = 0; i < atributos.getLength(); i++) {
            Node atributo = atributos.item(i);
            String clave = atributo.getLocalName() == null ? atributo.getNodeName() : atributo.getLocalName();
            String valor = atributo.getNodeValue();
            if (clave.startsWith("xmlns") || "xmlns".equals(atributo.getPrefix())) {
                throw new DatosInvalidosException("La plantilla declara espacios de nombres no permitidos");
            }
            if (ATRIBUTOS_PROHIBIDOS.contains(clave)) {
                throw new DatosInvalidosException("La plantilla usa un atributo no permitido: " + clave);
            }
            if ("class".equals(clave) || "valueClass".equals(clave)) {
                if (!CLASES_PERMITIDAS.contains(valor)) {
                    throw new DatosInvalidosException("La plantilla usa un tipo de dato no permitido: " + valor);
                }
            }
            if ("kind".equals(clave) && "element".equals(nombre) && !TIPOS_ELEMENTO.contains(valor)) {
                throw new DatosInvalidosException("La plantilla usa un tipo de elemento no permitido: " + valor);
            }
            if ("name".equals(clave)
                    && ("parameter".equals(nombre) || "field".equals(nombre) || "variable".equals(nombre))
                    && !NOMBRE_DEFINICION.matcher(valor).matches()) {
                throw new DatosInvalidosException("La plantilla usa un nombre no permitido: " + valor);
            }
            if (valor.length() > 600) {
                throw new DatosInvalidosException("La plantilla tiene un atributo demasiado largo");
            }
        }
        if ("element".equals(nombre) && !elemento.hasAttribute("kind")) {
            throw new DatosInvalidosException("Cada elemento de la plantilla debe indicar su tipo");
        }
        if ("variable".equals(nombre)) {
            throw new DatosInvalidosException("La plantilla no puede declarar variables");
        }
        if ("group".equals(nombre) || "groupHeader".equals(nombre) || "groupFooter".equals(nombre)) {
            throw new DatosInvalidosException("La plantilla no puede declarar grupos");
        }
    }

    private static void revisarExpresion(Element elemento, String nombre) {
        String texto = elemento.getTextContent() == null ? "" : elemento.getTextContent();
        if (texto.length() > EXPRESION_MAXIMA) {
            throw new DatosInvalidosException("La plantilla tiene una expresión demasiado larga");
        }
        Node padre = elemento.getParentNode();
        if ("expression".equals(nombre)
                && padre instanceof Element contenedor
                && "image".equals(contenedor.getAttribute("kind"))
                && !SOLO_PARAMETRO.matcher(texto).matches()) {
            throw new DatosInvalidosException("Las imágenes solo pueden venir de parámetros de la plantilla");
        }
        validarExpresion(texto);
    }

    static void validarExpresion(String expresion) {
        if (expresion.indexOf('\\') >= 0) {
            throw new DatosInvalidosException("La plantilla usa secuencias de escape no permitidas en una expresión");
        }
        String sinLiterales = LITERAL.matcher(expresion).replaceAll("X");
        if (sinLiterales.indexOf('"') >= 0) {
            throw new DatosInvalidosException("La plantilla tiene una expresión con comillas sin cerrar");
        }
        Matcher referencias = REFERENCIA.matcher(sinLiterales);
        String sinReferencias = referencias.replaceAll("X");
        String sinVacios = sinReferencias;
        if (!CARACTERES.matcher(sinVacios).matches()) {
            throw new DatosInvalidosException("La plantilla usa caracteres no permitidos en una expresión");
        }
        Matcher identificadores = IDENTIFICADOR.matcher(sinVacios);
        while (identificadores.find()) {
            String palabra = identificadores.group();
            if (!PALABRAS.contains(palabra)) {
                throw new DatosInvalidosException("La plantilla usa una instrucción no permitida: " + palabra);
            }
        }
    }
}

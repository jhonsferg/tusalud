package com.codesoftlabs.tusalud.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpTelemetryFilterTest {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private SdkTracerProvider tracerProvider;
    private Tracer tracer;
    private final HttpTelemetryFilter filter = new HttpTelemetryFilter();

    @BeforeEach
    void setUp() {
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        tracer = tracerProvider.get("test");
    }

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    private SpanData ejecutar(
            String metodo, String ruta, String requestJson, String responseJson, FilterChainConfig config)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.addHeader("Authorization", "Bearer token-completo");
        request.addHeader("Cookie", "sesion=abc; otra=def");
        request.addHeader("X-Api-Key", "clave-api");
        request.addHeader("X-Custom", "uno");
        request.addHeader("X-Custom", "dos");
        request.setContent(requestJson.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            String leido = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(leido).isEqualTo(requestJson);
            res.setContentType(config.contentType());
            ((jakarta.servlet.http.HttpServletResponse) res).setHeader("Set-Cookie", "sesion=abc");
            res.getOutputStream().write(responseJson.getBytes(StandardCharsets.UTF_8));
        };

        Span span = tracer.spanBuilder(metodo + " " + ruta)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            filter.doFilter(request, response, chain);
        } finally {
            span.end();
        }
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(responseJson);
        return exporter.getFinishedSpanItems().get(0);
    }

    private record FilterChainConfig(String contentType) {
        static FilterChainConfig json() {
            return new FilterChainConfig("application/json");
        }
    }

    @Test
    void enmascaraHeadersSensiblesYConservaLosDemas() throws Exception {
        SpanData data = ejecutar("GET", "/api/v1/citas", "{}", "{}", FilterChainConfig.json());

        assertThat(data.getAttributes().get(AttributeKey.stringArrayKey("http.request.header.authorization")))
                .containsExactly(TelemetryRedactor.REDACTADO);
        assertThat(data.getAttributes().get(AttributeKey.stringArrayKey("http.request.header.cookie")))
                .containsExactly(TelemetryRedactor.REDACTADO);
        assertThat(data.getAttributes().get(AttributeKey.stringArrayKey("http.request.header.x-api-key")))
                .containsExactly(TelemetryRedactor.REDACTADO);
        assertThat(data.getAttributes().get(AttributeKey.stringArrayKey("http.response.header.set-cookie")))
                .containsExactly(TelemetryRedactor.REDACTADO);
        assertThat(data.getAttributes().get(AttributeKey.stringArrayKey("http.request.header.x-custom")))
                .containsExactly("uno", "dos");
        assertThat(data.getAttributes()
                        .get(AttributeKey.stringArrayKey("http.response.header.content-type"))
                        .get(0))
                .startsWith("application/json");
    }

    @Test
    void redactaCamposSensiblesEnCuerposJsonDeFormaRecursiva() throws Exception {
        String requestJson = "{\"titulo\":\"Reunión\",\"password\":\"Secreta123!\","
                + "\"datos\":{\"refreshToken\":\"abc\",\"codigoMfa\":\"123456\",\"nota\":\"ok\"},"
                + "\"lista\":[{\"numeroTarjeta\":\"4111111111111111\",\"cvv\":\"123\",\"etiqueta\":\"x\"}],"
                + "\"claveSecreta\":\"s\",\"codigosRecuperacion\":[\"a\",\"b\"]}";
        String responseJson = "{\"accessToken\":\"eyJhbGciOiJIUzUxMiJ9.secreto\",\"nombre\":\"Ñandú\"}";

        SpanData data =
                ejecutar("POST", "/api/v1/calendario/eventos", requestJson, responseJson, FilterChainConfig.json());

        String cuerpo = data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY);
        assertThat(cuerpo)
                .contains("\"titulo\":\"Reunión\"")
                .contains("\"nota\":\"ok\"")
                .contains("\"etiqueta\":\"x\"")
                .doesNotContain("Secreta123!")
                .doesNotContain("abc\"")
                .doesNotContain("123456")
                .doesNotContain("4111111111111111")
                .doesNotContain("\"cvv\":\"123\"");
        assertThat(cuerpo).contains("\"password\":\"[REDACTADO]\"");
        assertThat(cuerpo).contains("\"codigosRecuperacion\":\"[REDACTADO]\"");
        assertThat(cuerpo).contains("\"claveSecreta\":\"[REDACTADO]\"");

        String respuesta = data.getAttributes().get(HttpTelemetryFilter.RESPONSE_BODY);
        assertThat(respuesta)
                .contains("\"accessToken\":\"[REDACTADO]\"")
                .contains("Ñandú")
                .doesNotContain("eyJhbGciOiJIUzUxMiJ9");
        assertThat(data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY_SIZE))
                .isEqualTo(requestJson.getBytes(StandardCharsets.UTF_8).length);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/mfa/verificar",
                "/api/v1/perfil/password",
                "/api/v1/perfil",
                "/api/v1/metodos-pago",
                "/api/v1/pagos/qr/123",
                "/api/v1/admin/personal",
                "/api/v1/admin/usuarios",
                "/api/v1/recepcion/pacientes",
                "/api/v1/historia-clinica",
                "/api/v1/pacientes/3fa85f64-5717-4562-b3fc-2c963f66afa6/historia-clinica",
                "/api/v1/pacientes/3fa85f64-5717-4562-b3fc-2c963f66afa6/datos-medicos",
                "/api/v1/citas/3fa85f64-5717-4562-b3fc-2c963f66afa6/atencion",
                "/api/v1/laboratorio/ordenes",
                "/api/v1/hospitalizacion/ingresos",
                "/api/v1/farmacia/dispensaciones"
            })
    void noRegistraElCuerpoEnRutasSensiblesPeroSiLosTamanos(String ruta) throws Exception {
        String requestJson = "{\"dato\":\"valor-privado\"}";
        String responseJson = "{\"diagnostico\":\"confidencial\"}";

        SpanData data = ejecutar("POST", ruta, requestJson, responseJson, FilterChainConfig.json());

        assertThat(data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY)).isNull();
        assertThat(data.getAttributes().get(HttpTelemetryFilter.RESPONSE_BODY)).isNull();
        assertThat(data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY_SIZE))
                .isEqualTo(requestJson.getBytes(StandardCharsets.UTF_8).length);
        assertThat(data.getAttributes().get(HttpTelemetryFilter.RESPONSE_BODY_SIZE))
                .isEqualTo(responseJson.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void noRegistraCuerposQueNoSonJson() throws Exception {
        SpanData data = ejecutar(
                "GET", "/api/v1/citas/exportar", "{}", "%PDF-1.7 binario", new FilterChainConfig("application/pdf"));

        assertThat(data.getAttributes().get(HttpTelemetryFilter.RESPONSE_BODY)).isNull();
        assertThat(data.getAttributes().get(HttpTelemetryFilter.RESPONSE_BODY_SIZE))
                .isPositive();
    }

    @Test
    void noRegistraJsonInvalidoNiEventosEnStreaming() throws Exception {
        SpanData invalido = ejecutar("POST", "/api/v1/citas", "{no es json", "{}", FilterChainConfig.json());
        assertThat(invalido.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY))
                .isNull();

        exporter.reset();
        MockHttpServletRequest stream = new MockHttpServletRequest("GET", "/api/v1/notificaciones/stream");
        Span span = tracer.spanBuilder("GET stream").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            filter.doFilter(
                    stream,
                    new MockHttpServletResponse(),
                    (req, res) -> assertThat(req).isSameAs(stream));
        } finally {
            span.end();
        }
        SpanData data = exporter.getFinishedSpanItems().get(0);
        assertThat(data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY)).isNull();
        assertThat(data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY_SIZE))
                .isNull();
    }

    @Test
    void truncaLosCuerposLargos() throws Exception {
        String largo = "x".repeat(10_000);
        String requestJson = "{\"descripcion\":\"" + largo + "\"}";

        SpanData data = ejecutar("POST", "/api/v1/citas", requestJson, "{}", FilterChainConfig.json());

        String cuerpo = data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY);
        assertThat(cuerpo).endsWith(TelemetryRedactor.TRUNCADO);
        assertThat(cuerpo.length()).isEqualTo(TelemetryRedactor.MAX_CUERPO + TelemetryRedactor.TRUNCADO.length());
        assertThat(data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY_SIZE))
                .isEqualTo(requestJson.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void redactaPorFragmentoDeNombre() {
        assertThat(TelemetryRedactor.esCampoSensible("passwordActual")).isTrue();
        assertThat(TelemetryRedactor.esCampoSensible("nueva_password")).isTrue();
        assertThat(TelemetryRedactor.esCampoSensible("clientSecret")).isTrue();
        assertThat(TelemetryRedactor.esCampoSensible("jwtToken")).isTrue();
        assertThat(TelemetryRedactor.esCampoSensible("cvc")).isTrue();
        assertThat(TelemetryRedactor.esCampoSensible("ultimosDigitosTarjeta")).isTrue();
        assertThat(TelemetryRedactor.esCampoSensible("qr")).isTrue();
        assertThat(TelemetryRedactor.esCampoSensible("titulo")).isFalse();
        assertThat(TelemetryRedactor.esCampoSensible("fecha")).isFalse();
    }

    @Test
    void noIntervieneFueraDeLaApiNiSinSpanActivo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> assertThat(req).isSameAs(request));

        MockHttpServletRequest docs = new MockHttpServletRequest("GET", "/v3/api-docs");
        Span span = tracer.spanBuilder("GET /v3/api-docs").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            filter.doFilter(
                    docs,
                    new MockHttpServletResponse(),
                    (req, res) -> assertThat(req).isSameAs(docs));
        } finally {
            span.end();
        }
        assertThat(exporter.getFinishedSpanItems().get(0).getAttributes().get(HttpTelemetryFilter.REQUEST_BODY))
                .isNull();
        assertThat(List.of(exporter.getFinishedSpanItems().size())).containsExactly(1);
    }

    @Test
    void lasPeticionesMultipartPasanSinLeerNiGuardarSuCuerpo() throws Exception {
        byte[] contenido = "--limite contenido binario --limite--".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/imagenes/ordenes/1/archivos");
        request.setContentType("multipart/form-data; boundary=limite");
        request.setContent(contenido);
        MockHttpServletResponse response = new MockHttpServletResponse();
        java.util.concurrent.atomic.AtomicReference<jakarta.servlet.ServletRequest> recibida =
                new java.util.concurrent.atomic.AtomicReference<>();
        FilterChain chain = (req, res) -> {
            recibida.set(req);
            assertThat(req.getInputStream().readAllBytes()).isEqualTo(contenido);
        };

        Span span =
                tracer.spanBuilder("POST archivos").setSpanKind(SpanKind.SERVER).startSpan();
        try (Scope ignored = span.makeCurrent()) {
            filter.doFilter(request, response, chain);
        } finally {
            span.end();
        }

        assertThat(recibida.get()).isSameAs(request);
        SpanData data = exporter.getFinishedSpanItems().get(0);
        assertThat(data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY)).isNull();
        assertThat(data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY_SIZE))
                .isEqualTo((long) contenido.length);
    }
}

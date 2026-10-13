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

    @Test
    void registraHeadersYBodiesCompletosSinEnmascarar() throws Exception {
        String requestJson = "{\"email\":\"admin@tusalud.com\",\"password\":\"Admin123!\"}";
        String responseJson = "{\"token\":\"eyJhbGciOiJIUzUxMiJ9.secreto\",\"nombre\":\"Ñandú\"}";

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.addHeader("Authorization", "Bearer token-completo");
        request.addHeader("X-Custom", "uno");
        request.addHeader("X-Custom", "dos");
        request.setContent(requestJson.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            String leido = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(leido).isEqualTo(requestJson);
            res.setContentType("application/json");
            ((jakarta.servlet.http.HttpServletResponse) res).setHeader("Set-Cookie", "sesion=abc");
            res.getOutputStream().write(responseJson.getBytes(StandardCharsets.UTF_8));
        };

        Span span = tracer.spanBuilder("POST /api/v1/auth/login")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            filter.doFilter(request, response, chain);
        } finally {
            span.end();
        }

        SpanData data = exporter.getFinishedSpanItems().get(0);
        assertThat(data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY)).isEqualTo(requestJson);
        assertThat(data.getAttributes().get(HttpTelemetryFilter.RESPONSE_BODY)).isEqualTo(responseJson);
        assertThat(data.getAttributes().get(HttpTelemetryFilter.REQUEST_BODY_SIZE))
                .isEqualTo(requestJson.getBytes(StandardCharsets.UTF_8).length);
        assertThat(data.getAttributes().get(AttributeKey.stringArrayKey("http.request.header.authorization")))
                .containsExactly("Bearer token-completo");
        assertThat(data.getAttributes().get(AttributeKey.stringArrayKey("http.request.header.x-custom")))
                .containsExactly("uno", "dos");
        assertThat(data.getAttributes().get(AttributeKey.stringArrayKey("http.response.header.set-cookie")))
                .containsExactly("sesion=abc");
        assertThat(data.getAttributes()
                        .get(AttributeKey.stringArrayKey("http.response.header.content-type"))
                        .get(0))
                .startsWith("application/json");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(responseJson);
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
}

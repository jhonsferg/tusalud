package com.codesoftlabs.tusalud.app.config;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpTelemetryFilter extends OncePerRequestFilter {

    public static final String REQUEST_HEADER_PREFIX = "http.request.header.";
    public static final String RESPONSE_HEADER_PREFIX = "http.response.header.";
    public static final AttributeKey<String> REQUEST_BODY = AttributeKey.stringKey("http.request.body");
    public static final AttributeKey<String> RESPONSE_BODY = AttributeKey.stringKey("http.response.body");
    public static final AttributeKey<Long> REQUEST_BODY_SIZE = AttributeKey.longKey("http.request.body.size");
    public static final AttributeKey<Long> RESPONSE_BODY_SIZE = AttributeKey.longKey("http.response.body.size");

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Span span = Span.current();
        if (!span.getSpanContext().isValid()) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

        registrarHeadersRequest(span, cachedRequest);
        span.setAttribute(REQUEST_BODY, new String(cachedRequest.getBody(), cachedRequest.getBodyCharset()));
        span.setAttribute(REQUEST_BODY_SIZE, (long) cachedRequest.getBody().length);

        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            byte[] responseBody = cachedResponse.getContentAsByteArray();
            span.setAttribute(RESPONSE_BODY, new String(responseBody, charsetDe(cachedResponse.getContentType())));
            span.setAttribute(RESPONSE_BODY_SIZE, (long) responseBody.length);
            cachedResponse.copyBodyToResponse();
            registrarHeadersResponse(span, response, responseBody.length);
        }
    }

    private void registrarHeadersRequest(Span span, HttpServletRequest request) {
        for (String name : Collections.list(request.getHeaderNames())) {
            List<String> values = Collections.list(request.getHeaders(name));
            span.setAttribute(
                    AttributeKey.stringArrayKey(REQUEST_HEADER_PREFIX + name.toLowerCase(Locale.ROOT)), values);
        }
    }

    private void registrarHeadersResponse(Span span, HttpServletResponse response, int bodyLength) {
        Set<String> registrados = new HashSet<>();
        for (String name : new LinkedHashSet<>(response.getHeaderNames())) {
            String key = name.toLowerCase(Locale.ROOT);
            registrados.add(key);
            span.setAttribute(
                    AttributeKey.stringArrayKey(RESPONSE_HEADER_PREFIX + key),
                    new ArrayList<>(response.getHeaders(name)));
        }
        if (!registrados.contains("content-type") && response.getContentType() != null) {
            span.setAttribute(
                    AttributeKey.stringArrayKey(RESPONSE_HEADER_PREFIX + "content-type"),
                    List.of(response.getContentType()));
        }
        if (!registrados.contains("content-length") && !registrados.contains("transfer-encoding") && bodyLength > 0) {
            span.setAttribute(
                    AttributeKey.stringArrayKey(RESPONSE_HEADER_PREFIX + "content-length"),
                    List.of(String.valueOf(bodyLength)));
        }
    }

    private Charset charsetDe(String contentType) {
        if (contentType != null) {
            try {
                Charset charset = MediaType.parseMediaType(contentType).getCharset();
                if (charset != null) {
                    return charset;
                }
            } catch (InvalidMediaTypeException ignored) {
                return StandardCharsets.UTF_8;
            }
        }
        return StandardCharsets.UTF_8;
    }
}

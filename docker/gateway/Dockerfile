ARG NGINX_IMAGE=nginxinc/nginx-unprivileged:1.30-alpine-slim
FROM ${NGINX_IMAGE}
LABEL org.opencontainers.image.title="tusalud-gateway" \
      org.opencontainers.image.description="Gateway de TuSalud: frontend, API y almacenamiento detrás de un único punto de entrada" \
      org.opencontainers.image.source="https://github.com/jhonsferg/tusalud" \
      org.opencontainers.image.vendor="CodeSoftLabs"
ENV NGINX_ENVSUBST_TEMPLATE_DIR=/tmp/gateway-templates \
    NGINX_ENVSUBST_FILTER="^(API_|FRONT_|RATE_|GATEWAY_|NGINX_RESOLVER)"
RUN rm -f /etc/nginx/conf.d/default.conf
COPY templates/ /etc/nginx/gateway-templates/
COPY docker-entrypoint.d/05-gateway.envsh /docker-entrypoint.d/05-gateway.envsh
EXPOSE 8080 8443
HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1

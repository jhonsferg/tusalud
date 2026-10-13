ARG NODE_IMAGE=node:24-alpine
ARG NGINX_IMAGE=nginxinc/nginx-unprivileged:1.30-alpine-slim

FROM ${NODE_IMAGE} AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund --prefer-offline

FROM deps AS build
ARG BUILD_CONFIGURATION=production
ENV NG_CLI_ANALYTICS=false
COPY . .
RUN npx ng build --configuration ${BUILD_CONFIGURATION} \
    && find dist/tusalud/browser -type f \
        \( -name '*.js' -o -name '*.css' -o -name '*.html' -o -name '*.svg' -o -name '*.json' -o -name '*.txt' -o -name '*.ico' \) \
        -size +1k -exec gzip -9 -k -n {} +

FROM ${NGINX_IMAGE} AS runtime
LABEL org.opencontainers.image.title="tusalud-web" \
      org.opencontainers.image.description="Aplicación web de TuSalud" \
      org.opencontainers.image.source="https://github.com/jhonsferg/tusalud" \
      org.opencontainers.image.vendor="CodeSoftLabs"
ENV API_UPSTREAM=http://tusalud-api:8080 \
    NGINX_RESOLVER=127.0.0.11
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/tusalud/browser /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1

FROM node:24-alpine AS build
ARG BUILD_CONFIGURATION=production
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npx ng build --configuration ${BUILD_CONFIGURATION}

FROM nginx:1.30-alpine AS runtime

ENV API_UPSTREAM=http://tusalud-api:8080 \
    NGINX_RESOLVER=127.0.0.11

COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/tusalud/browser /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1

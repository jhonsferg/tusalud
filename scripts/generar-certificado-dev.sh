#!/bin/sh
set -eu
MSYS_NO_PATHCONV=1
export MSYS_NO_PATHCONV

destino="${1:-docker/gateway/certs}"
nombre="${GATEWAY_SERVER_NAME:-localhost}"
if [ "$nombre" = "_" ]; then
    nombre="localhost"
fi

mkdir -p "$destino"
ruta="$(cd "$destino" && (pwd -W 2>/dev/null || pwd))"

if command -v openssl >/dev/null 2>&1; then
    openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
        -keyout "$ruta/tls.key" -out "$ruta/tls.crt" \
        -subj "/CN=$nombre" \
        -addext "subjectAltName=DNS:$nombre,DNS:localhost,IP:127.0.0.1"
else
    docker run --rm -v "$ruta:/certs" alpine/openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
        -keyout /certs/tls.key -out /certs/tls.crt \
        -subj "/CN=$nombre" \
        -addext "subjectAltName=DNS:$nombre,DNS:localhost,IP:127.0.0.1"
fi

chmod 600 "$ruta/tls.key" 2>/dev/null || true
echo "Certificado autofirmado generado en $ruta para $nombre"
echo "Para activarlo: GATEWAY_TLS=on GATEWAY_SERVER_NAME=$nombre docker compose up -d gateway"

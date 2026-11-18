param(
    [string]$Destino = "docker/gateway/certs",
    [string]$Nombre = "localhost"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $Destino | Out-Null
$ruta = (Resolve-Path $Destino).Path

docker run --rm -v "${ruta}:/certs" alpine/openssl req -x509 -nodes -newkey rsa:2048 -days 365 `
    -keyout /certs/tls.key -out /certs/tls.crt `
    -subj "/CN=$Nombre" `
    -addext "subjectAltName=DNS:$Nombre,DNS:localhost,IP:127.0.0.1"

Write-Host "Certificado autofirmado generado en $ruta para $Nombre"
Write-Host "Para activarlo: GATEWAY_TLS=on GATEWAY_SERVER_NAME=$Nombre docker compose up -d gateway"

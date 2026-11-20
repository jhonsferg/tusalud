#!/bin/sh
set -eu

BASE="${BASE:-/root/tusalud}"
ENV_ARCHIVO="${BASE}/.env"
DB_NOMBRE="tusalud"
DB_USUARIO="tusalud_app"
STORAGE_USUARIO="tusalud_app"
BUCKETS="avatares imagenes documentos reportes plantillas"

mkdir -p "${BASE}"
chmod 700 "${BASE}"

aleatorio() {
    openssl rand -base64 "$1" | tr -d '\n=+/' | cut -c1-"$2"
}

if [ ! -f "${ENV_ARCHIVO}" ]; then
    umask 077
    {
        echo "TUSALUD_DB_NAME=${DB_NOMBRE}"
        echo "TUSALUD_DB_USER=${DB_USUARIO}"
        echo "TUSALUD_DB_PASSWORD=$(aleatorio 48 32)"
        echo "TUSALUD_STORAGE_ACCESS_KEY=${STORAGE_USUARIO}"
        echo "TUSALUD_STORAGE_SECRET_KEY=$(aleatorio 48 40)"
        echo "JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')"
        echo "MFA_CLAVE_CIFRADO=$(aleatorio 64 48)"
        echo "CANALES_SECRETO=$(aleatorio 64 48)"
        echo "CANALES_WEBHOOK_SECRETO=$(aleatorio 64 48)"
        echo "RESPALDO_TOKEN=$(aleatorio 64 48)"
        echo "TURN_SECRET=$(aleatorio 64 48)"
        echo "ADMIN_EMAIL=admin@tusalud.codesoftlabs.com"
        echo "ADMIN_PASSWORD=$(aleatorio 48 20)"
    } > "${ENV_ARCHIVO}"
    chmod 600 "${ENV_ARCHIVO}"
    echo "archivo de entorno creado"
else
    echo "archivo de entorno ya existe"
fi

. "${ENV_ARCHIVO}"

docker exec -i databases_postgres sh -c 'psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 -v pw="'"${TUSALUD_DB_PASSWORD}"'" -v rol="'"${DB_USUARIO}"'" -v bd="'"${DB_NOMBRE}"'"' <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'rol', :'pw')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'rol')
\gexec
SELECT format('ALTER ROLE %I PASSWORD %L', :'rol', :'pw')
\gexec
SELECT format('CREATE DATABASE %I OWNER %I ENCODING ''UTF8'' TEMPLATE template0', :'bd', :'rol')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'bd')
\gexec
SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'bd')
\gexec
SQL
echo "base de datos y usuario listos"

cat > "${BASE}/politica-minio.json" <<JSON
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:*"],
      "Resource": ["arn:aws:s3:::tusalud-*", "arn:aws:s3:::tusalud-*/*"]
    }
  ]
}
JSON
chmod 600 "${BASE}/politica-minio.json"

MINIO_USUARIO_RAIZ=$(docker inspect storages_minio --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^MINIO_ROOT_USER=//p')
MINIO_CLAVE_RAIZ=$(docker inspect storages_minio --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^MINIO_ROOT_PASSWORD=//p')

if [ ! -x "${BASE}/mc" ]; then
    mkdir -p "${BASE}/bin"
    chmod 777 "${BASE}/bin"
    docker run --rm -v "${BASE}/bin:/out" -e GOBIN=/out -e CGO_ENABLED=0 golang:alpine go install github.com/minio/mc@latest
    mv "${BASE}/bin/mc" "${BASE}/mc"
    chown root:root "${BASE}/mc"
    chmod 700 "${BASE}/mc" "${BASE}/bin"
fi

mc() {
    "${BASE}/mc" --config-dir "${BASE}/.mc" "$@"
}

mc alias set local http://127.0.0.1:9000 "${MINIO_USUARIO_RAIZ}" "${MINIO_CLAVE_RAIZ}" >/dev/null

for bucket in ${BUCKETS}; do
    mc mb --ignore-existing "local/tusalud-${bucket}"
done
mc admin user add local "${TUSALUD_STORAGE_ACCESS_KEY}" "${TUSALUD_STORAGE_SECRET_KEY}" || true
mc admin policy create local tusalud-rw "${BASE}/politica-minio.json" || true
mc admin policy attach local tusalud-rw --user "${TUSALUD_STORAGE_ACCESS_KEY}" || true
echo "buckets y usuario de almacenamiento listos"

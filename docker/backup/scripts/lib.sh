#!/bin/sh

RESPALDOS_DIR="${RESPALDOS_DIR:-/respaldos}"
DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-tusalud}"
STORAGE_BUCKETS="${STORAGE_BUCKETS:-avatares imagenes documentos reportes plantillas}"
RETENCION_DIARIOS="${RETENCION_DIARIOS:-7}"
RETENCION_SEMANALES="${RETENCION_SEMANALES:-4}"
RETENCION_MENSUALES="${RETENCION_MENSUALES:-6}"
ITERACIONES_CLAVE="${ITERACIONES_CLAVE:-200000}"

if [ -n "${RESPALDO_PASSPHRASE_FILE:-}" ] && [ -r "$RESPALDO_PASSPHRASE_FILE" ]; then
    RESPALDO_PASSPHRASE="$(cat "$RESPALDO_PASSPHRASE_FILE")"
fi
export RESPALDO_PASSPHRASE

if (set -o pipefail) 2>/dev/null; then
    set -o pipefail
fi

log() {
    printf '%s %s\n' "$(date +%Y-%m-%dT%H:%M:%S)" "$*"
}

fallar() {
    printf '%s ERROR %s\n' "$(date +%Y-%m-%dT%H:%M:%S)" "$*" >&2
}

exigir() {
    for nombre in "$@"; do
        eval "valor=\${$nombre:-}"
        if [ -z "$valor" ]; then
            fallar "falta la variable $nombre"
            exit 2
        fi
    done
}

ahora() {
    date +%Y-%m-%dT%H:%M:%S
}

segundos() {
    date +%s
}

sha256_de() {
    sha256sum "$1" | cut -d' ' -f1
}

tamano_de() {
    stat -c %s "$1"
}

cifrar() {
    openssl enc -aes-256-cbc -pbkdf2 -iter "$ITERACIONES_CLAVE" -salt -pass env:RESPALDO_PASSPHRASE
}

descifrar() {
    openssl enc -d -aes-256-cbc -pbkdf2 -iter "$ITERACIONES_CLAVE" -pass env:RESPALDO_PASSPHRASE
}

psql_base() {
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -X -q -t -A "$@"
}

configurar_rclone() {
    export RCLONE_CONFIG_STORAGE_TYPE=s3
    export RCLONE_CONFIG_STORAGE_PROVIDER=Other
    export RCLONE_CONFIG_STORAGE_ENDPOINT="$STORAGE_ENDPOINT"
    export RCLONE_CONFIG_STORAGE_ACCESS_KEY_ID="$STORAGE_ACCESS_KEY"
    export RCLONE_CONFIG_STORAGE_SECRET_ACCESS_KEY="$STORAGE_SECRET_KEY"
    export RCLONE_CONFIG_STORAGE_REGION="${STORAGE_REGION:-us-east-1}"
    export RCLONE_CONFIG_STORAGE_FORCE_PATH_STYLE=true
    export RCLONE_CONFIG_STORAGE_NO_CHECK_BUCKET=true
    if [ -n "${RESPALDO_REMOTO_ENDPOINT:-}" ]; then
        export RCLONE_CONFIG_REMOTO_TYPE=s3
        export RCLONE_CONFIG_REMOTO_PROVIDER=Other
        export RCLONE_CONFIG_REMOTO_ENDPOINT="$RESPALDO_REMOTO_ENDPOINT"
        export RCLONE_CONFIG_REMOTO_ACCESS_KEY_ID="${RESPALDO_REMOTO_ACCESS_KEY:-}"
        export RCLONE_CONFIG_REMOTO_SECRET_ACCESS_KEY="${RESPALDO_REMOTO_SECRET_KEY:-}"
        export RCLONE_CONFIG_REMOTO_REGION="${RESPALDO_REMOTO_REGION:-us-east-1}"
    fi
}

reportar() {
    if [ -z "${API_URL:-}" ] || [ -z "${RESPALDO_TOKEN:-}" ]; then
        log "no se reporta a la API: faltan API_URL o RESPALDO_TOKEN"
        return 0
    fi
    cuerpo=$(jq -n \
        --arg tipo "$1" --arg estado "$2" --arg alcance "$3" --arg categoria "$4" \
        --arg inicio "$5" --arg fin "$6" --arg bytes "$7" --arg archivo "$8" --arg checksum "$9" \
        --arg migraciones "${10}" --arg tablas "${11}" --arg objetos "${12}" --arg remota "${13}" --arg detalle "${14}" \
        '{
            tipo: $tipo, estado: $estado, alcance: $alcance,
            categoria: (if $categoria == "" then null else $categoria end),
            iniciadoEn: $inicio, finalizadoEn: $fin,
            bytesTotal: (if $bytes == "" then null else ($bytes | tonumber) end),
            archivo: (if $archivo == "" then null else $archivo end),
            checksum: (if $checksum == "" then null else $checksum end),
            migracionesValidadas: (if $migraciones == "" then null else ($migraciones | tonumber) end),
            tablasValidadas: (if $tablas == "" then null else ($tablas | tonumber) end),
            objetosAlmacen: (if $objetos == "" then null else ($objetos | tonumber) end),
            copiaRemota: ($remota == "true"),
            detalle: (if $detalle == "" then null else $detalle[0:500] end)
        }')
    if curl -fsS -m 30 -X POST "$API_URL/api/v1/respaldos/ejecuciones" \
        -H "Content-Type: application/json" -H "X-Respaldo-Token: $RESPALDO_TOKEN" -d "$cuerpo" > /dev/null; then
        log "resultado reportado a la API"
    else
        fallar "no se pudo reportar el resultado a la API"
    fi
}

conteos_json() {
    psql_base -c "SELECT json_build_object(
        'migraciones', (SELECT count(*) FROM flyway_schema_history WHERE success),
        'ultimaMigracion', (SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1),
        'tablas', (SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'),
        'conteos', json_build_object(
            'usuarios', (SELECT count(*) FROM usuarios),
            'pacientes', (SELECT count(*) FROM pacientes),
            'medicos', (SELECT count(*) FROM medicos),
            'citas', (SELECT count(*) FROM citas),
            'atenciones', (SELECT count(*) FROM atenciones),
            'archivos', (SELECT count(*) FROM archivos)))"
}

retener() {
    categoria="$1"
    conservar="$2"
    ruta="$RESPALDOS_DIR/$categoria"
    [ -d "$ruta" ] || return 0
    ls -1 "$ruta" | sort | awk -v conservar="$conservar" '{a[NR]=$0} END{for(i=1;i<=NR-conservar;i++)print a[i]}' | while read -r viejo; do
        log "retención: se elimina $categoria/$viejo"
        rm -rf "${ruta:?}/$viejo"
    done
}

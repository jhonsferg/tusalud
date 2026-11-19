#!/bin/sh
set -eu
. /opt/respaldos/lib.sh

uso() {
    cat << 'AYUDA'
Uso: restaurar.sh DIRECTORIO_DEL_RESPALDO [--base NOMBRE] [--almacen] --confirmar

  DIRECTORIO_DEL_RESPALDO  carpeta con manifiesto.json, por ejemplo /respaldos/diario/20260101-020000
  --base NOMBRE            base de destino. Por omisión es DB_NAME, que reemplaza los datos actuales.
                           Use otro nombre para restaurar a una base nueva y revisarla antes de cambiar.
  --almacen                además repone los objetos del almacén con los del respaldo.
  --confirmar              obligatorio. Sin esta opción no se hace ningún cambio.

Antes de restaurar sobre la base real, detenga la API.
AYUDA
}

ORIGEN="${1:-}"
[ -n "$ORIGEN" ] || { uso; exit 2; }
shift
DESTINO_BASE="$DB_NAME"
CON_ALMACEN=0
CONFIRMADO=0
while [ "$#" -gt 0 ]; do
    case "$1" in
        --base) DESTINO_BASE="$2"; shift 2 ;;
        --almacen) CON_ALMACEN=1; shift ;;
        --confirmar) CONFIRMADO=1; shift ;;
        *) uso; exit 2 ;;
    esac
done

exigir DB_USERNAME DB_PASSWORD RESPALDO_PASSPHRASE
[ -r "$ORIGEN/manifiesto.json" ] || { fallar "no existe manifiesto.json en $ORIGEN"; exit 5; }
if [ "$CONFIRMADO" != "1" ]; then
    fallar "falta --confirmar. No se hizo ningún cambio"
    exit 2
fi

[ "$(sha256_de "$ORIGEN/base.dump.enc")" = "$(jq -r '.base.sha256' "$ORIGEN/manifiesto.json")" ] \
    || { fallar "el checksum de la base no coincide"; exit 6; }
[ "$(sha256_de "$ORIGEN/almacen.tar.gz.enc")" = "$(jq -r '.almacen.sha256' "$ORIGEN/manifiesto.json")" ] \
    || { fallar "el checksum del almacén no coincide"; exit 6; }
log "checksums verificados"

export PGPASSWORD="$DB_PASSWORD"
if [ "$DESTINO_BASE" != "$DB_NAME" ]; then
    dropdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" --if-exists "$DESTINO_BASE"
    createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" "$DESTINO_BASE"
    log "base $DESTINO_BASE creada"
    descifrar < "$ORIGEN/base.dump.enc" | pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DESTINO_BASE" --no-owner --no-privileges --exit-on-error
else
    log "se reemplazan los datos de $DESTINO_BASE"
    descifrar < "$ORIGEN/base.dump.enc" | pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DESTINO_BASE" --clean --if-exists --no-owner --no-privileges --exit-on-error
fi
log "base restaurada"

if [ "$CON_ALMACEN" = "1" ]; then
    exigir STORAGE_ENDPOINT STORAGE_ACCESS_KEY STORAGE_SECRET_KEY
    configurar_rclone
    TEMPORAL="$(mktemp -d)"
    trap 'rm -rf "$TEMPORAL"' EXIT
    descifrar < "$ORIGEN/almacen.tar.gz.enc" | tar -xzf - -C "$TEMPORAL"
    for bucket in $(jq -r '.almacen.buckets[]' "$ORIGEN/manifiesto.json"); do
        if [ -d "$TEMPORAL/almacen/$bucket" ]; then
            rclone copy "$TEMPORAL/almacen/$bucket" "STORAGE:$bucket" --transfers 4 --retries 3 --quiet
            log "bucket $bucket repuesto"
        fi
    done
fi
log "restauración terminada"

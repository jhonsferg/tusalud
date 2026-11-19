#!/bin/sh
set -eu
. /opt/respaldos/lib.sh

exigir DB_USERNAME DB_PASSWORD RESPALDO_PASSPHRASE STORAGE_ENDPOINT STORAGE_ACCESS_KEY STORAGE_SECRET_KEY

BLOQUEO=/tmp/respaldo.bloqueo
if ! mkdir "$BLOQUEO" 2> /dev/null; then
    fallar "ya hay un respaldo en ejecución"
    exit 3
fi

INICIO="$(ahora)"
INICIO_S="$(segundos)"
SELLO="$(date +%Y%m%d-%H%M%S)"
CATEGORIA=DIARIO
if [ "$(date +%u)" = "7" ]; then
    CATEGORIA=SEMANAL
fi
if [ "$(date +%d)" = "01" ]; then
    CATEGORIA=MENSUAL
fi
DESTINO="$RESPALDOS_DIR/diario/$SELLO"
EXITO=0
ULTIMO_PASO="inicio"
REMOTA=false

al_salir() {
    codigo=$?
    rmdir "$BLOQUEO" 2> /dev/null || true
    rm -rf "/tmp/almacen-$SELLO"
    if [ "$EXITO" != "1" ]; then
        fallar "el respaldo falló en el paso: $ULTIMO_PASO"
        rm -rf "$DESTINO"
        reportar RESPALDO FALLIDO COMPLETO "$CATEGORIA" "$INICIO" "$(ahora)" "" "" "" "" "" "" false "Falló en el paso: $ULTIMO_PASO"
    fi
    exit "$codigo"
}
trap al_salir EXIT

mkdir -p "$DESTINO"
log "respaldo $SELLO iniciado, categoría $CATEGORIA"

ULTIMO_PASO="conteos previos"
CONTEOS="$(conteos_json)"

ULTIMO_PASO="volcado de la base"
PGPASSWORD="$DB_PASSWORD" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -Fc -Z 6 \
    | cifrar > "$DESTINO/base.dump.enc"
BASE_BYTES="$(tamano_de "$DESTINO/base.dump.enc")"
BASE_SHA="$(sha256_de "$DESTINO/base.dump.enc")"
log "base respaldada: $BASE_BYTES bytes"

ULTIMO_PASO="copia del almacén"
configurar_rclone
ALMACEN="/tmp/almacen-$SELLO"
mkdir -p "$ALMACEN/almacen"
for bucket in $STORAGE_BUCKETS; do
    mkdir -p "$ALMACEN/almacen/$bucket"
    rclone copy "STORAGE:$bucket" "$ALMACEN/almacen/$bucket" --transfers 4 --retries 3 --quiet
done
OBJETOS="$(find "$ALMACEN/almacen" -type f | wc -l | tr -d ' ')"
( cd "$ALMACEN" && find almacen -type f | sort | while read -r archivo; do sha256sum "$archivo"; done > SHA256SUMS )

ULTIMO_PASO="empaquetado y cifrado del almacén"
tar -C "$ALMACEN" -czf - almacen SHA256SUMS | cifrar > "$DESTINO/almacen.tar.gz.enc"
ALMACEN_BYTES="$(tamano_de "$DESTINO/almacen.tar.gz.enc")"
ALMACEN_SHA="$(sha256_de "$DESTINO/almacen.tar.gz.enc")"
log "almacén respaldado: $OBJETOS objetos, $ALMACEN_BYTES bytes"

ULTIMO_PASO="manifiesto"
FIN="$(ahora)"
jq -n \
    --argjson conteos "$CONTEOS" \
    --arg sello "$SELLO" --arg categoria "$CATEGORIA" --arg inicio "$INICIO" --arg fin "$FIN" \
    --arg baseSha "$BASE_SHA" --argjson baseBytes "$BASE_BYTES" \
    --arg almacenSha "$ALMACEN_SHA" --argjson almacenBytes "$ALMACEN_BYTES" --argjson objetos "$OBJETOS" \
    --arg buckets "$STORAGE_BUCKETS" --arg host "$(hostname)" \
    '{
        version: 1, sello: $sello, categoria: $categoria, iniciadoEn: $inicio, finalizadoEn: $fin, host: $host,
        cifrado: "AES-256-CBC con PBKDF2",
        base: {archivo: "base.dump.enc", formato: "pg_dump custom", bytes: $baseBytes, sha256: $baseSha,
               migraciones: $conteos.migraciones, ultimaMigracion: $conteos.ultimaMigracion,
               tablas: $conteos.tablas, conteos: $conteos.conteos},
        almacen: {archivo: "almacen.tar.gz.enc", bytes: $almacenBytes, sha256: $almacenSha, objetos: $objetos,
                  buckets: ($buckets | split(" "))}
    }' > "$DESTINO/manifiesto.json"

if [ "$CATEGORIA" = "SEMANAL" ] || [ "$CATEGORIA" = "MENSUAL" ]; then
    ULTIMO_PASO="copia semanal"
    mkdir -p "$RESPALDOS_DIR/semanal"
    cp -a "$DESTINO" "$RESPALDOS_DIR/semanal/$SELLO"
fi
if [ "$CATEGORIA" = "MENSUAL" ]; then
    ULTIMO_PASO="copia mensual"
    mkdir -p "$RESPALDOS_DIR/mensual"
    cp -a "$DESTINO" "$RESPALDOS_DIR/mensual/$SELLO"
fi

if [ -n "${RESPALDO_REMOTO_ENDPOINT:-}" ] && [ -n "${RESPALDO_REMOTO_BUCKET:-}" ]; then
    ULTIMO_PASO="copia remota"
    rclone copy "$DESTINO" "REMOTO:$RESPALDO_REMOTO_BUCKET/${RESPALDO_REMOTO_PREFIJO:-tusalud}/$SELLO" --retries 3 --quiet
    REMOTA=true
    log "copia remota completada"
fi

ULTIMO_PASO="retención"
retener diario "$RETENCION_DIARIOS"
retener semanal "$RETENCION_SEMANALES"
retener mensual "$RETENCION_MENSUALES"

EXITO=1
TOTAL=$((BASE_BYTES + ALMACEN_BYTES))
DURACION=$(( $(segundos) - INICIO_S ))
log "respaldo $SELLO completo en ${DURACION}s"
reportar RESPALDO EXITOSO COMPLETO "$CATEGORIA" "$INICIO" "$FIN" "$TOTAL" "diario/$SELLO" "$BASE_SHA" "" "" "$OBJETOS" "$REMOTA" "Base y almacén respaldados"

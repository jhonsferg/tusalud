#!/bin/sh
set -eu
. /opt/respaldos/lib.sh

exigir RESPALDO_PASSPHRASE

BLOQUEO=/tmp/prueba-restauracion.bloqueo
if ! mkdir "$BLOQUEO" 2> /dev/null; then
    fallar "ya hay una prueba de restauración en ejecución"
    exit 3
fi

INICIO="$(ahora)"
INICIO_S="$(segundos)"
ORIGEN="${1:-}"
if [ -z "$ORIGEN" ]; then
    ULTIMO="$(ls -1 "$RESPALDOS_DIR/diario" 2> /dev/null | sort | tail -n 1)"
    if [ -z "$ULTIMO" ]; then
        fallar "no hay respaldos para probar"
        rmdir "$BLOQUEO"
        reportar PRUEBA_RESTAURACION FALLIDO COMPLETO "" "$INICIO" "$(ahora)" "" "" "" "" "" "" false "No hay respaldos para probar"
        exit 4
    fi
    ORIGEN="$RESPALDOS_DIR/diario/$ULTIMO"
fi

TEMPORAL="/tmp/prueba-restauracion"
SOCKET="/tmp/prueba-restauracion-socket"
PUERTO=55432
EXITO=0
ULTIMO_PASO="inicio"
CLUSTER_ACTIVO=0

como_postgres() {
    su postgres -s /bin/sh -c "$*"
}

al_salir() {
    codigo=$?
    if [ "$CLUSTER_ACTIVO" = "1" ]; then
        como_postgres "pg_ctl -D $TEMPORAL/datos -m immediate -w stop" > /dev/null 2>&1 || true
    fi
    rm -rf "$TEMPORAL" "$SOCKET"
    rmdir "$BLOQUEO" 2> /dev/null || true
    if [ "$EXITO" != "1" ]; then
        fallar "la prueba de restauración falló en el paso: $ULTIMO_PASO"
        reportar PRUEBA_RESTAURACION FALLIDO COMPLETO "" "$INICIO" "$(ahora)" "" "$(basename "$ORIGEN")" "" "" "" "" false "Falló en el paso: $ULTIMO_PASO"
    fi
    exit "$codigo"
}
trap al_salir EXIT

log "prueba de restauración sobre $ORIGEN"

ULTIMO_PASO="manifiesto"
MANIFIESTO="$ORIGEN/manifiesto.json"
[ -r "$MANIFIESTO" ] || { fallar "no existe el manifiesto en $ORIGEN"; exit 5; }

ULTIMO_PASO="verificación de checksums"
BASE_ESPERADO="$(jq -r '.base.sha256' "$MANIFIESTO")"
ALMACEN_ESPERADO="$(jq -r '.almacen.sha256' "$MANIFIESTO")"
[ "$(sha256_de "$ORIGEN/base.dump.enc")" = "$BASE_ESPERADO" ] || { fallar "el checksum de la base no coincide"; exit 6; }
[ "$(sha256_de "$ORIGEN/almacen.tar.gz.enc")" = "$ALMACEN_ESPERADO" ] || { fallar "el checksum del almacén no coincide"; exit 6; }
log "checksums verificados"

ULTIMO_PASO="base temporal"
rm -rf "$TEMPORAL" "$SOCKET"
mkdir -p "$TEMPORAL/datos" "$SOCKET"
chown -R postgres:postgres "$TEMPORAL" "$SOCKET"
como_postgres "initdb -D $TEMPORAL/datos --auth=trust -U postgres --encoding=UTF8 --no-locale" > /dev/null
como_postgres "pg_ctl -D $TEMPORAL/datos -o \"-p $PUERTO -c listen_addresses='' -c unix_socket_directories=$SOCKET -c fsync=off -c synchronous_commit=off -c full_page_writes=off -c shared_buffers=64MB\" -w -l $TEMPORAL/servidor.log start" > /dev/null
CLUSTER_ACTIVO=1
PSQL="psql -h $SOCKET -p $PUERTO -U postgres -X -q -t -A"
createdb -h "$SOCKET" -p "$PUERTO" -U postgres prueba

ULTIMO_PASO="restauración de la base"
INICIO_BASE="$(segundos)"
descifrar < "$ORIGEN/base.dump.enc" > "$TEMPORAL/base.dump"
pg_restore --list "$TEMPORAL/base.dump" > /dev/null
pg_restore -h "$SOCKET" -p "$PUERTO" -U postgres -d prueba --no-owner --no-privileges --exit-on-error "$TEMPORAL/base.dump"
rm -f "$TEMPORAL/base.dump"
SEGUNDOS_BASE=$(( $(segundos) - INICIO_BASE ))
log "base restaurada en ${SEGUNDOS_BASE}s"

ULTIMO_PASO="validación de migraciones y tablas"
MIGRACIONES_ESPERADAS="$(jq -r '.base.migraciones' "$MANIFIESTO")"
ULTIMA_ESPERADA="$(jq -r '.base.ultimaMigracion' "$MANIFIESTO")"
TABLAS_ESPERADAS="$(jq -r '.base.tablas' "$MANIFIESTO")"
MIGRACIONES="$($PSQL -d prueba -c "SELECT count(*) FROM flyway_schema_history WHERE success")"
FALLIDAS="$($PSQL -d prueba -c "SELECT count(*) FROM flyway_schema_history WHERE NOT success")"
ULTIMA="$($PSQL -d prueba -c "SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1")"
TABLAS="$($PSQL -d prueba -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")"
[ "$FALLIDAS" = "0" ] || { fallar "hay migraciones fallidas en el respaldo"; exit 7; }
[ "$MIGRACIONES" = "$MIGRACIONES_ESPERADAS" ] || { fallar "migraciones restauradas $MIGRACIONES, esperadas $MIGRACIONES_ESPERADAS"; exit 7; }
[ "$ULTIMA" = "$ULTIMA_ESPERADA" ] || { fallar "última migración $ULTIMA, esperada $ULTIMA_ESPERADA"; exit 7; }
[ "$TABLAS" = "$TABLAS_ESPERADAS" ] || { fallar "tablas restauradas $TABLAS, esperadas $TABLAS_ESPERADAS"; exit 7; }
log "migraciones ($MIGRACIONES, última $ULTIMA) y tablas ($TABLAS) coinciden con el manifiesto"

ULTIMO_PASO="validación de conteos"
for tabla in usuarios pacientes medicos citas atenciones archivos; do
    esperado="$(jq -r ".base.conteos.$tabla" "$MANIFIESTO")"
    real="$($PSQL -d prueba -c "SELECT count(*) FROM $tabla")"
    tolerancia=$(( esperado / 100 + 5 ))
    diferencia=$(( real > esperado ? real - esperado : esperado - real ))
    if [ "$diferencia" -gt "$tolerancia" ]; then
        fallar "conteo de $tabla: restaurado $real, esperado $esperado"
        exit 8
    fi
    log "tabla $tabla: $real registros restaurados, esperados $esperado"
done
USUARIOS="$($PSQL -d prueba -c "SELECT count(*) FROM usuarios")"
[ "$USUARIOS" -gt 0 ] || { fallar "la base restaurada no tiene usuarios"; exit 8; }

ULTIMO_PASO="restauración del almacén"
mkdir -p "$TEMPORAL/almacen"
descifrar < "$ORIGEN/almacen.tar.gz.enc" | tar -xzf - -C "$TEMPORAL/almacen"
OBJETOS_ESPERADOS="$(jq -r '.almacen.objetos' "$MANIFIESTO")"
OBJETOS="$(find "$TEMPORAL/almacen/almacen" -type f 2> /dev/null | wc -l | tr -d ' ')"
[ "$OBJETOS" = "$OBJETOS_ESPERADOS" ] || { fallar "objetos restaurados $OBJETOS, esperados $OBJETOS_ESPERADOS"; exit 9; }
if [ "$OBJETOS" -gt 0 ]; then
    ( cd "$TEMPORAL/almacen" && sha256sum -c SHA256SUMS --quiet ) || { fallar "los objetos del almacén no coinciden con sus checksums"; exit 9; }
fi
log "almacén restaurado: $OBJETOS objetos verificados"

EXITO=1
BYTES=$(( $(tamano_de "$ORIGEN/base.dump.enc") + $(tamano_de "$ORIGEN/almacen.tar.gz.enc") ))
DURACION=$(( $(segundos) - INICIO_S ))
log "prueba de restauración correcta en ${DURACION}s"
reportar PRUEBA_RESTAURACION EXITOSO COMPLETO "" "$INICIO" "$(ahora)" "$BYTES" "$(basename "$ORIGEN")" "$BASE_ESPERADO" "$MIGRACIONES" "$TABLAS" "$OBJETOS" false "Base restaurada en ${SEGUNDOS_BASE}s con migraciones, tablas y conteos verificados, y $OBJETOS objetos del almacén con checksum"

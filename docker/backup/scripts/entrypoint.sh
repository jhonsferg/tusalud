#!/bin/sh
set -eu
. /opt/respaldos/lib.sh

if [ -z "${RESPALDO_PASSPHRASE:-}" ]; then
    fallar "defina RESPALDO_PASSPHRASE o RESPALDO_PASSPHRASE_FILE. Sin clave no se hacen respaldos"
    exit 2
fi

mkdir -p "$RESPALDOS_DIR/diario" "$RESPALDOS_DIR/semanal" "$RESPALDOS_DIR/mensual"

CRON_RESPALDO="${BACKUP_CRON:-0 2 * * *}"
CRON_PRUEBA="${TEST_CRON:-30 3 * * 0}"
mkdir -p /etc/crontabs
{
    echo "$CRON_RESPALDO /opt/respaldos/respaldar.sh > /proc/1/fd/1 2> /proc/1/fd/2"
    echo "$CRON_PRUEBA /opt/respaldos/probar-restauracion.sh > /proc/1/fd/1 2> /proc/1/fd/2"
} > /etc/crontabs/root

log "respaldos programados: base y almacén a las '$CRON_RESPALDO', prueba de restauración a las '$CRON_PRUEBA'"
log "retención: $RETENCION_DIARIOS diarios, $RETENCION_SEMANALES semanales y $RETENCION_MENSUALES mensuales"
exec crond -f -l 8 -L /dev/stdout

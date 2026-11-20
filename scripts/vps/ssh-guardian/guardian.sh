#!/bin/bash
set -u
LOG=/var/log/ssh-guardian.log
ESTADO=/var/lib/ssh-guardian
MARCA=$ESTADO/ssh-reabierto
SIMULAR=0
[ "${1:-}" = "--simular-fallo" ] && SIMULAR=1
[ "${GUARDIAN_SIMULAR_FALLO:-0}" = "1" ] && SIMULAR=1

mkdir -p "$ESTADO"; chmod 700 "$ESTADO"
touch "$LOG"; chmod 600 "$LOG"

log() {
    local linea
    linea="$(date -Is) $*"
    echo "$linea" >> "$LOG"
    logger -t ssh-guardian -- "$*"
    echo "$linea"
}

accion() {
    if [ "$SIMULAR" = "1" ]; then
        log "SIMULACION: ejecutaria: $*"
        return 0
    fi
    log "ejecutando: $*"
    "$@" >> "$LOG" 2>&1
}

fallos=()

if [ "$SIMULAR" = "1" ]; then
    fallos+=("fallo simulado de wg0")
else
    ip -o link show wg0 >/dev/null 2>&1 || fallos+=("la interfaz wg0 no existe")
    ip -o link show wg0 2>/dev/null | grep -qE '<[^>]*[<,]UP[,>]' || fallos+=("wg0 no esta UP")
    ip -o -4 addr show dev wg0 2>/dev/null | grep -q ' 10\.8\.0\.1/' || fallos+=("wg0 no tiene 10.8.0.1")
    [ "$(wg show wg0 listen-port 2>/dev/null)" = "51820" ] || fallos+=("el tunel wg0 no escucha en 51820")
fi

if ! ss -ltn 'sport = :2222' 2>/dev/null | grep -q LISTEN; then
    log "CRITICO: sshd no escucha en el puerto 2222"
    if [ "$SIMULAR" = "1" ]; then
        log "SIMULACION: ejecutaria: systemctl start ssh"
    elif sshd -t 2>/dev/null; then
        systemctl start ssh >> "$LOG" 2>&1 || true
    fi
fi

if ! nft list table inet geoblock >/dev/null 2>&1; then
    log "AVISO: la tabla nftables inet geoblock no esta cargada"
fi

if [ "${#fallos[@]}" -eq 0 ]; then
    log "OK: wg0 UP con 10.8.0.1, tunel escuchando en 51820 y sshd en 2222"
    exit 0
fi

log "FALLO: ${fallos[*]}"

if [ -f "$MARCA" ] && [ "$SIMULAR" != "1" ]; then
    log "el SSH ya estaba reabierto, no se repite"
    exit 1
fi

accion /etc/geoblock/revertir-ssh.sh
accion ufw allow 2222/tcp comment 'ssh-guardian temporal'

if [ "$SIMULAR" = "1" ]; then
    log "SIMULACION: escribiria la marca $MARCA"
else
    {
        date -Is
        echo "motivo: ${fallos[*]}"
        echo "para cerrar de nuevo: /etc/geoblock/guardian-cerrar.sh"
    } > "$MARCA"
    chmod 600 "$MARCA"
    log "SSH reabierto a IPs de Peru, marca escrita en $MARCA"
fi
exit 1

#!/bin/bash
set -u
ESPERADOS=/root/pre-reinicio/contenedores-esperados.txt
UA="Mozilla/5.0 Chrome/130"
FALLOS=()
AVISOS=()

fallo() { FALLOS+=("$1"); printf '  [FALLA] %s\n' "$1"; }
aviso() { AVISOS+=("$1"); printf '  [AVISO] %s\n' "$1"; }
bien() { printf '  [ok] %s\n' "$1"; }
seccion() { printf '\n== %s\n' "$1"; }

seccion "Sistema"
echo "  hora: $(date -Is)"
echo "  uptime: $(uptime -p) desde $(uptime -s)"
EN_USO=$(uname -r)
INSTALADO=$(ls /boot/vmlinuz-* 2>/dev/null | sed 's#.*/vmlinuz-##' | sort -V | tail -1)
echo "  kernel en uso: $EN_USO, el mas nuevo instalado: ${INSTALADO:-desconocido}"
if [ -n "$INSTALADO" ] && [ "$EN_USO" != "$INSTALADO" ]; then
    aviso "el kernel en uso no es el mas nuevo instalado"
fi
if [ -e /var/run/reboot-required ]; then
    aviso "el sistema todavia pide reinicio (/var/run/reboot-required)"
fi

seccion "Contenedores"
if [ -s "$ESPERADOS" ]; then
    while read -r c; do
        [ -n "$c" ] || continue
        estado=$(docker inspect -f '{{.State.Status}}' "$c" 2>/dev/null || echo ausente)
        salud=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-healthcheck{{end}}' "$c" 2>/dev/null || echo ausente)
        reinicios=$(docker inspect -f '{{.RestartCount}}' "$c" 2>/dev/null || echo 0)
        if [ "$estado" != "running" ]; then
            fallo "contenedor $c: estado $estado"
        elif [ "$salud" = "unhealthy" ] || [ "$salud" = "starting" ]; then
            fallo "contenedor $c: salud $salud"
        else
            bien "$c $estado $salud reinicios=$reinicios"
            [ "$reinicios" -gt 3 ] 2>/dev/null && aviso "$c reinicio $reinicios veces"
        fi
    done < "$ESPERADOS"
    extra=$(docker ps --format '{{.Names}}' | grep -vxFf "$ESPERADOS" || true)
    [ -n "$extra" ] && aviso "contenedores no esperados: $(echo $extra)"
else
    fallo "no existe $ESPERADOS"
fi
docker exec databases_postgres pg_isready -q 2>/dev/null && bien "postgres acepta conexiones" || fallo "postgres no acepta conexiones"
r=$(docker exec databases_redis redis-cli ping 2>&1 | head -1)
case "$r" in PONG*|*NOAUTH*) bien "redis responde" ;; *) fallo "redis no responde" ;; esac
curl -fsS -m 5 -o /dev/null http://127.0.0.1:9000/minio/health/live && bien "minio vivo" || fallo "minio no responde"

seccion "Unidades"
for u in docker containerd wg-quick@wg0 geoblock ssh ufw fail2ban auditd unattended-upgrades ntpsec cron nginx deployer swapfile.swap; do
    a=$(systemctl is-active "$u" 2>&1)
    if [ "$a" = "active" ]; then bien "$u activo"; else fallo "$u: $a"; fi
done
for t in geoblock-actualizar ssh-guardian certbot repo-watch log-collector server-report geoip-updater; do
    e=$(systemctl is-enabled "$t.timer" 2>&1)
    if [ "$e" = "enabled" ]; then bien "$t.timer habilitado"; else fallo "$t.timer: $e"; fi
done
fallidas=$(systemctl --failed --no-legend --plain | awk '{print $1}' | tr '\n' ' ')
[ -z "$fallidas" ] && bien "ninguna unidad fallida" || aviso "unidades fallidas: $fallidas"

seccion "WireGuard"
if ip -o -4 addr show dev wg0 | grep -q ' 10\.8\.0\.1/'; then bien "wg0 con 10.8.0.1"; else fallo "wg0 sin 10.8.0.1"; fi
ahora=$(date +%s)
mejor=999999
for h in $(wg show wg0 latest-handshakes 2>/dev/null | awk '{print $2}'); do
    [ "$h" -gt 0 ] && d=$((ahora - h)) && [ "$d" -lt "$mejor" ] && mejor=$d
done
if [ "$mejor" -le 180 ]; then bien "handshake reciente hace ${mejor}s"; else fallo "sin handshake reciente (mejor: ${mejor}s)"; fi

seccion "Firewall"
if nft list table inet geoblock >/dev/null 2>&1; then
    bien "tabla inet geoblock cargada"
    nft list counters table inet geoblock 2>/dev/null | grep -E 'counter|packets' | tr -s ' ' | paste -sd' ' | sed 's/^/  /'
else
    fallo "tabla inet geoblock no cargada"
fi
if [ -f /etc/geoblock/ssh-solo-vpn ]; then bien "SSH cerrado a Internet (ssh-solo-vpn)"; else aviso "ssh-solo-vpn no existe: el SSH esta abierto a Peru"; fi
ufw status | grep -q '^Status: active' && bien "ufw activo" || fallo "ufw inactivo"
ufw status | grep -qE '^2222/tcp on wg0 +ALLOW' && bien "ufw permite 2222 por wg0" || fallo "falta la regla de ufw 2222 en wg0"
ss -ltn 'sport = :2222' | grep -q LISTEN && bien "sshd escucha en 2222" || fallo "sshd no escucha en 2222"
if [ -e /var/lib/ssh-guardian/ssh-reabierto ]; then fallo "el guardian reabrio el SSH, revisar /var/log/ssh-guardian.log"; fi
echo "  guardian: $(tail -n 1 /var/log/ssh-guardian.log 2>/dev/null | cut -c1-140)"

seccion "fail2ban y auditd"
if fail2ban-client ping >/dev/null 2>&1; then
    bien "fail2ban responde: $(fail2ban-client status | sed -n 's/.*Jail list:\s*//p')"
    for j in $(fail2ban-client status | sed -n 's/.*Jail list:\s*//p' | tr -d ','); do
        fail2ban-client status "$j" >/dev/null 2>&1 || fallo "jail $j con error"
    done
else
    fallo "fail2ban no responde"
fi
n=$(auditctl -l 2>/dev/null | wc -l)
[ "$n" -gt 20 ] && bien "auditd con $n reglas cargadas" || fallo "auditd con pocas reglas ($n)"

seccion "nginx"
if docker exec nginx nginx -t 2>&1 | grep -q successful; then bien "nginx -t correcto"; else fallo "nginx -t fallo"; fi

seccion "Sitios"
http() {
    local esperado=$1 host=$2 ruta=$3
    shift 3
    local c
    c=$(curl -s -o /dev/null -m 15 -w '%{http_code}' --resolve "$host:443:127.0.0.1" "$@" "https://$host$ruta")
    if [ "$c" = "$esperado" ]; then bien "$host$ruta responde $c"; else fallo "$host$ruta responde $c y se esperaba $esperado"; fi
}
http 200 tusalud.codesoftlabs.com / -A "$UA"
http 400 api.tusalud.codesoftlabs.com /api/v1/auth/login -X POST -H 'Content-Type: application/json' -d '{}'
http 307 wayki.codesoftlabs.com / -A "$UA"
http 307 warlay.codesoftlabs.com / -A "$UA"
http 307 git.codesoftlabs.com / -A "$UA"
http 307 console.minio.codesoftlabs.com / -A "$UA"
http 404 hooks.codesoftlabs.com / -A "$UA"

seccion "Memoria y disco"
free -h | sed -n 1,3p | sed 's/^/  /'
s=$(swapon --noheadings --bytes 2>/dev/null | awk '{t+=$3} END {print t+0}')
[ "$s" -gt 0 ] && bien "swap activo" || fallo "swap inactivo"
uso=$(df --output=pcent / | tail -1 | tr -dc '0-9')
echo "  disco raiz: ${uso}% usado"
[ "$uso" -lt 85 ] && bien "espacio en disco suficiente" || fallo "disco raiz al ${uso}%"

seccion "Resultado"
for a in "${AVISOS[@]:-}"; do [ -n "$a" ] && echo "  aviso: $a"; done
if [ "${#FALLOS[@]}" -eq 0 ]; then
    echo "PASA"
    exit 0
fi
echo "FALLA: ${FALLOS[*]}"
exit 1

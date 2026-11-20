#!/bin/bash
set -euo pipefail

DIR=/etc/geoblock
BD=/etc/nginx/geoip/GeoLite2-Country.mmdb
PAIS=PE
IP_ADMIN=38.250.135.8
MIN_V4=500
MIN_V6=500

interfaz() {
    ip -4 route get 1.1.1.1 | sed -n 's/.* dev \([^ ]*\).*/\1/p' | head -1
}

extraer() {
    python3 "${DIR}/extraer_pais.py" "${BD}" "${PAIS}" "${DIR}/pe4.nuevo" "${DIR}/pe6.nuevo" >/dev/null
    local n4 n6
    n4=$(wc -l < "${DIR}/pe4.nuevo")
    n6=$(wc -l < "${DIR}/pe6.nuevo")
    if [ "${n4}" -lt "${MIN_V4}" ] || [ "${n6}" -lt "${MIN_V6}" ]; then
        echo "listas demasiado pequeñas: v4=${n4} v6=${n6}" >&2
        exit 1
    fi
    python3 - "${DIR}/pe4.nuevo" "${IP_ADMIN}" <<'PY'
import ipaddress, sys
redes = [ipaddress.ip_network(l.strip()) for l in open(sys.argv[1]) if l.strip()]
ip = ipaddress.ip_address(sys.argv[2])
sys.exit(0 if any(ip in r for r in redes) else 1)
PY
    mv "${DIR}/pe4.nuevo" "${DIR}/pe4.txt"
    mv "${DIR}/pe6.nuevo" "${DIR}/pe6.txt"
}

github() {
    if curl -fsS --max-time 30 -o "${DIR}/github-meta.nuevo" https://api.github.com/meta; then
        python3 - "${DIR}/github-meta.nuevo" "${DIR}/github4.txt" "${DIR}/github6.txt" <<'PY'
import json, sys
j = json.load(open(sys.argv[1]))
h = j.get("hooks", [])
if len(h) < 1:
    sys.exit(1)
open(sys.argv[2], "w").write("\n".join(x for x in h if ":" not in x) + "\n")
open(sys.argv[3], "w").write("\n".join(x for x in h if ":" in x) + "\n")
PY
        rm -f "${DIR}/github-meta.nuevo"
    fi
}

lista() {
    if [ -s "$1" ]; then
        paste -sd, "$1"
    fi
}

generar() {
    local ifz p4 p6 g4 g6
    ifz=$(interfaz)
    p4=$(lista "${DIR}/pe4.txt")
    p6=$(lista "${DIR}/pe6.txt")
    g4=$(lista "${DIR}/github4.txt" 2>/dev/null || true)
    g6=$(lista "${DIR}/github6.txt" 2>/dev/null || true)
    cat > "${DIR}/geoblock.nft" <<NFT
table inet geoblock
delete table inet geoblock
table inet geoblock {
    set pais4 {
        type ipv4_addr
        flags interval
        auto-merge
        elements = { ${p4} }
    }
    set pais6 {
        type ipv6_addr
        flags interval
        auto-merge
        elements = { ${p6} }
    }
    set github4 {
        type ipv4_addr
        flags interval
        auto-merge
        ${g4:+elements = { ${g4} \}}
    }
    set github6 {
        type ipv6_addr
        flags interval
        auto-merge
        ${g6:+elements = { ${g6} \}}
    }
    counter descartes_entrada { }
    counter descartes_reenvio { }

    chain entrada {
        type filter hook input priority -150; policy accept;
        iifname != "${ifz}" accept
        ct state established,related accept
        ip saddr @pais4 accept
        ip6 saddr @pais6 accept
        ip6 saddr fe80::/10 accept
        meta l4proto ipv6-icmp accept
        tcp dport 80 accept
        tcp dport 443 ip saddr @github4 accept
        tcp dport 443 ip6 saddr @github6 accept
        counter name descartes_entrada drop
    }

    chain reenvio {
        type filter hook forward priority -150; policy accept;
        iifname != "${ifz}" accept
        ct state established,related accept
        ip saddr @pais4 accept
        ip6 saddr @pais6 accept
        counter name descartes_reenvio drop
    }
}
NFT
}

aplicar() {
    generar
    nft -c -f "${DIR}/geoblock.nft"
    nft -f "${DIR}/geoblock.nft"
}

case "${1:-}" in
    aplicar)
        [ -s "${DIR}/pe4.txt" ] || extraer
        [ -s "${DIR}/github4.txt" ] || github || true
        aplicar
        ;;
    actualizar)
        extraer
        github || true
        aplicar
        ;;
    probar)
        [ -s "${DIR}/pe4.txt" ] || extraer
        [ -s "${DIR}/github4.txt" ] || github || true
        generar
        nft -c -f "${DIR}/geoblock.nft"
        echo "sintaxis correcta"
        ;;
    quitar)
        nft delete table inet geoblock 2>/dev/null || true
        ;;
    estado)
        nft list counters table inet geoblock
        ;;
    *)
        echo "uso: $0 aplicar|actualizar|probar|quitar|estado" >&2
        exit 2
        ;;
esac

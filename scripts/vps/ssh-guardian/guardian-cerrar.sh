#!/bin/bash
set -u
LOG=/var/log/ssh-guardian.log
ufw delete allow 2222/tcp >> "$LOG" 2>&1 || true
touch /etc/geoblock/ssh-solo-vpn
/etc/geoblock/geoblock.sh aplicar
rm -f /var/lib/ssh-guardian/ssh-reabierto
echo "$(date -Is) SSH cerrado de nuevo, solo por VPN" >> "$LOG"
logger -t ssh-guardian "SSH cerrado de nuevo, solo por VPN"

#!/bin/bash
set -euo pipefail
rm -f /etc/geoblock/ssh-solo-vpn
/etc/geoblock/geoblock.sh aplicar

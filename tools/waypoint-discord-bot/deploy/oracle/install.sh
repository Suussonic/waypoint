#!/usr/bin/env bash
set -euo pipefail

if [[ $(id -u) -ne 0 ]]; then
  echo 'Run this installer with sudo.' >&2
  exit 1
fi

bot_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
if [[ ! -f "$bot_root/Dockerfile" ]]; then
  echo 'Run this from an A.T.L.A.S. source checkout.' >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y docker.io ca-certificates
systemctl enable --now docker
install -d -m 0700 /etc/atlas /var/lib/atlas
docker build -t atlas-waypoint:current "$bot_root"
install -m 0644 "$bot_root/deploy/oracle/atlas.service" /etc/systemd/system/atlas.service
systemctl daemon-reload

if [[ -f /etc/atlas/atlas.env ]]; then
  chmod 0600 /etc/atlas/atlas.env
  systemctl enable atlas.service
  systemctl restart atlas.service
  echo 'A.T.L.A.S. service installed and started.'
else
  echo 'A.T.L.A.S. image and service installed. Add /etc/atlas/atlas.env (mode 0600), then run: sudo systemctl enable --now atlas.service'
fi

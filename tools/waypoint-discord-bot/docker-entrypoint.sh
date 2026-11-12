#!/bin/sh
set -eu

mkdir -p "$WAYPOINT_DATA_DIR"
chown -R node:node "$WAYPOINT_DATA_DIR"
exec su-exec node "$@"

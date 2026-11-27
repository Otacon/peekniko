#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 3 || $# -gt 4 ]]; then
  echo "Usage: $0 <version> <core-url> <core-checksum> [output]" >&2
  exit 1
fi

version="$1"
core_url="$2"
core_checksum="$3"
output="${4:-Package.swift}"

if [[ ! "$core_checksum" =~ ^[0-9a-f]{64}$ ]]; then
  echo "Invalid checksum '$core_checksum': expected 64 lowercase hex characters" >&2
  exit 1
fi

sed \
  -e "s|{{VERSION}}|${version}|g" \
  -e "s|{{CORE_URL}}|${core_url}|g" \
  -e "s|{{CORE_CHECKSUM}}|${core_checksum}|g" \
  "$(dirname "$0")/Package.swift.template" > "$output"

echo "Wrote $output for ${version}"

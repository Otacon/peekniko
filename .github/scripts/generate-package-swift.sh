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
source="$(dirname "$0")/../../library-ios/Package.swift"

if [[ ! "$core_checksum" =~ ^[0-9a-f]{64}$ ]]; then
  echo "Invalid checksum '$core_checksum': expected 64 lowercase hex characters" >&2
  exit 1
fi

for marker in '^let root = ' '^let core = Target\.binaryTarget\('; do
  if [[ "$(grep -c -E "$marker" "$source")" != 1 ]]; then
    echo "$source must contain exactly one line matching '$marker'" >&2
    exit 1
  fi
done

awk \
  -v version="$version" \
  -v core_url="$core_url" \
  -v core_checksum="$core_checksum" '
  NR == 1 {
    print
    print "// Generated from library-ios/Package.swift by .github/scripts/generate-package-swift.sh for Peekniko " version ". Do not edit."
    in_header = 1
    next
  }
  in_header && /^\/\// { next }
  { in_header = 0 }
  /^let root = / { print "let root = \"library-ios/\""; next }
  /^let core = Target\.binaryTarget\(/ {
    print "let core = Target.binaryTarget(name: \"PeeknikoCore\", url: \"" core_url "\", checksum: \"" core_checksum "\")"
    next
  }
  { print }
' "$source" > "$output"

echo "Wrote $output for ${version}"

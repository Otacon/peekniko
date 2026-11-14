#!/usr/bin/env bash
set -euo pipefail

gradle_args=(
  --no-parallel
  --max-workers=1
  -Dorg.gradle.internal.network.retry.max.attempts=10
  -Dorg.gradle.internal.network.retry.initial.backOff=5000
  -x publishIosArm64PublicationToMavenCentralRepository
  -x publishIosSimulatorArm64PublicationToMavenCentralRepository
  -x publishIosX64PublicationToMavenCentralRepository
  -x publishJsPublicationToMavenCentralRepository
  -x publishWasmJsPublicationToMavenCentralRepository
)

project="$1"

echo "Publishing Android/JVM artifacts for ${project}"
./gradlew "${gradle_args[@]}" "${project}:publishToMavenCentral"

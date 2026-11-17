#!/bin/bash
# usage: bash setup-cache.sh [dir]   (run after setup.sh)
set -e
B="https://github.com/Abhi13-coder/Just-mods/releases/download/claude-tools"
D="${1:-$HOME/android}"
mkdir -p "$D/gradle-home"
if curl -fsIL "$B/gradle-cache.tar.gz" >/dev/null; then
  curl -fL "$B/gradle-cache.tar.gz" | tar -xz -C "$D/gradle-home"
else
  for i in 00 01 02 03; do curl -fL "$B/gradle-cache.tar.gz.part$i" || break; done | tar -xz -C "$D/gradle-home"
fi
echo "org.gradle.offline=true" > "$D/gradle-home/gradle.properties"
echo "export GRADLE_USER_HOME=$D/gradle-home" >> "$D/env.sh"
echo "Done. source $D/env.sh then run gradle --offline"

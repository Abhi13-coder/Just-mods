#!/bin/bash
# usage: [COMP="jdk gradle ..."] bash setup.sh [dir]
set -e
B="https://github.com/Abhi13-coder/Just-mods/releases/download/claude-tools"
D="${1:-$HOME/android}"
COMP="${COMP:-jdk gradle platform-tools cmdline-tools build-tools platforms}"
mkdir -p "$D/sdk" "$D/jdk"
for c in $COMP; do
  echo "== $c"
  case "$c" in
    jdk)    curl -fL "$B/jdk.tar.gz" | tar -xz -C "$D/jdk" ;;
    gradle) curl -fL "$B/gradle.zip" -o /tmp/gradle.zip
            unzip -q -o /tmp/gradle.zip -d "$D" && rm -rf "$D/gradle"
            mv "$D"/gradle-[0-9]* "$D/gradle"; rm /tmp/gradle.zip ;;
    *)      curl -fL "$B/$c.tar.gz" | tar -xz -C "$D/sdk" ;;
  esac
done
cat > "$D/env.sh" <<ENV
export JAVA_HOME=$D/jdk
export ANDROID_HOME=$D/sdk
export ANDROID_SDK_ROOT=$D/sdk
export PATH=\$JAVA_HOME/bin:$D/gradle/bin:$D/sdk/platform-tools:$D/sdk/cmdline-tools/latest/bin:\$PATH
ENV
echo "Done. Run: source $D/env.sh"

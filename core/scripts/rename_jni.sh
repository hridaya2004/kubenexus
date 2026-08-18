#!/usr/bin/env bash
set -euo pipefail

AAR_FILE="${1:-kubenexus.aar}"
LIB_NAME="${2:-nexusclient}"

if [ ! -f "$AAR_FILE" ]; then
    echo "Error: $AAR_FILE not found" >&2
    exit 1
fi

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

unzip -q "$AAR_FILE" -d "$TMP_DIR/aar"

# Rename libgojni.so -> lib${LIB_NAME}.so across all JNI ABI folders
RENAMED=0
for so in "$TMP_DIR"/aar/jni/*/libgojni.so; do
    if [ -f "$so" ]; then
        dir=$(dirname "$so")
        mv "$so" "$dir/lib${LIB_NAME}.so"
        RENAMED=1
    fi
done

if [ "$RENAMED" -eq 1 ]; then
    # Recompile Seq.java to load the new library name
    mkdir -p "$TMP_DIR/seq/go"
    SEQ_JAVA=$(find "$(go env GOMODCACHE)/golang.org/x/mobile"* -name "Seq.java" 2>/dev/null | head -n 1)
    if [ -n "$SEQ_JAVA" ] && [ -f "$SEQ_JAVA" ]; then
        cp "$SEQ_JAVA" "$TMP_DIR/seq/go/Seq.java"
        sed -i "s/System.loadLibrary(\"gojni\")/System.loadLibrary(\"${LIB_NAME}\")/" "$TMP_DIR/seq/go/Seq.java"

        ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
        ANDROID_JAR=$(find "$ANDROID_HOME/platforms" -name "android.jar" 2>/dev/null | sort -V | tail -n 1)

        javac --release 8 -cp "$TMP_DIR/aar/classes.jar:${ANDROID_JAR:-}" "$TMP_DIR/seq/go/Seq.java"
        (cd "$TMP_DIR/seq" && jar uf "$TMP_DIR/aar/classes.jar" go/Seq*.class)
    fi

    # Repack AAR
    (cd "$TMP_DIR/aar" && zip -q -r "$TMP_DIR/repacked.aar" .)
    mv "$TMP_DIR/repacked.aar" "$AAR_FILE"
    echo "Successfully renamed JNI library to lib${LIB_NAME}.so inside $AAR_FILE"
fi

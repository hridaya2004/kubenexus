#!/usr/bin/env bash
# Renames gomobile's libgojni.so inside the AAR and patches go.Seq to load the new name.
#
# Usage: rename_jni.sh <aar> <lib-name> [sources-jar]
#
# go/Seq.java is taken from the sources jar that the same `gomobile bind` run emitted, so the
# patched class always matches the bindings in the AAR. Picking a Seq.java out of the module
# cache instead would silently pair the AAR with whichever x/mobile version happened to be there.
set -euo pipefail

AAR_FILE="${1:-kubenexus.aar}"
LIB_NAME="${2:-nexusclient}"
SOURCES_JAR="${3:-${AAR_FILE%.aar}-sources.jar}"

die() {
    echo "Error: $*" >&2
    exit 1
}

[ -f "$AAR_FILE" ] || die "$AAR_FILE not found"
[ -f "$SOURCES_JAR" ] || die "$SOURCES_JAR not found; it is needed to rebuild go/Seq.class"

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

unzip -q "$AAR_FILE" -d "$TMP_DIR/aar"

# Rename libgojni.so -> lib${LIB_NAME}.so across all JNI ABI folders
RENAMED=0
for so in "$TMP_DIR"/aar/jni/*/libgojni.so; do
    if [ -f "$so" ]; then
        dir=$(dirname "$so")
        mv "$so" "$dir/lib${LIB_NAME}.so"
        RENAMED=$((RENAMED + 1))
    fi
done
[ "$RENAMED" -gt 0 ] || die "no jni/*/libgojni.so found in $AAR_FILE"

# Recompile go.Seq so it loads the renamed library.
mkdir -p "$TMP_DIR/seq"
unzip -q "$SOURCES_JAR" 'go/Seq.java' -d "$TMP_DIR/seq" || die "go/Seq.java missing from $SOURCES_JAR"
SEQ_JAVA="$TMP_DIR/seq/go/Seq.java"

grep -q 'System.loadLibrary("gojni")' "$SEQ_JAVA" || die "go/Seq.java no longer calls System.loadLibrary(\"gojni\")"
# Portable in-place edit: BSD sed (macOS) and GNU sed disagree on `sed -i`.
sed "s/System.loadLibrary(\"gojni\")/System.loadLibrary(\"${LIB_NAME}\")/" "$SEQ_JAVA" >"$SEQ_JAVA.patched"
mv "$SEQ_JAVA.patched" "$SEQ_JAVA"
grep -q "System.loadLibrary(\"${LIB_NAME}\")" "$SEQ_JAVA" || die "patching go/Seq.java failed"

ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ANDROID_JAR=$(find "$ANDROID_HOME/platforms" -name "android.jar" 2>/dev/null | sort -V | tail -n 1)
[ -n "$ANDROID_JAR" ] || die "no android.jar under $ANDROID_HOME/platforms"

javac -nowarn --release 8 -Xlint:-options -cp "$TMP_DIR/aar/classes.jar:${ANDROID_JAR}" "$SEQ_JAVA"
(cd "$TMP_DIR/seq" && jar uf "$TMP_DIR/aar/classes.jar" go/Seq*.class)

# Repack AAR
(cd "$TMP_DIR/aar" && zip -q -r "$TMP_DIR/repacked.aar" .)
mv "$TMP_DIR/repacked.aar" "$AAR_FILE"
echo "Renamed libgojni.so to lib${LIB_NAME}.so for $RENAMED ABIs inside $AAR_FILE"

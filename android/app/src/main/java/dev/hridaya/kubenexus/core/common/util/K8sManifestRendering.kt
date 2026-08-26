package dev.hridaya.kubenexus.core.common.util

import net.mamoe.yamlkt.Yaml

/**
 * Single rendering path for guided-creation manifests: serializes an ordered
 * structure (LinkedHashMap/listOf trees whose insertion order defines key
 * order) into the exact text shown in the review step and applied verbatim
 * through the bridge.
 *
 * Output is always block-style YAML with exactly one trailing newline. Values
 * yamlkt's escaper considers unsafe unquoted - image refs containing ':'
 * or '/', for example - come out single-quoted; that is semantically identical
 * YAML for the API server, so no post-processing is done beyond the newline.
 */
internal fun renderK8sManifest(manifest: Map<String, Any>): String =
    Yaml.encodeToString(manifest).trimEnd('\n') + "\n"

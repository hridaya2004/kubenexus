package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.core.common.result.Result

interface ServiceRepository {

    /**
     * Creates the Service described by [manifestYaml] (the exact text the user
     * reviewed). The reviewed manifest carries its own namespace in
     * metadata.namespace, so none is passed separately. The manifest may be
     * YAML or JSON.
     */
    suspend fun createFromManifest(clusterId: String?, manifestYaml: String): Result<Unit>
}

package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.core.common.result.Result

interface DeploymentRepository {

    /**
     * Creates the workload described by [manifestYaml] (the exact text the user
     * reviewed) in its stated namespace. The manifest may be YAML or JSON.
     */
    suspend fun createFromManifest(clusterId: String?, manifestYaml: String): Result<Unit>
}

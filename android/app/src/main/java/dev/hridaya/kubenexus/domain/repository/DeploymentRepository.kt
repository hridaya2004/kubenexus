package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.DeploymentSummary

interface DeploymentRepository {

    /**
     * Creates the workload described by [manifestYaml] (the exact text the user
     * reviewed) in its stated namespace. The manifest may be YAML or JSON.
     */
    suspend fun createFromManifest(clusterId: String?, manifestYaml: String): Result<Unit>

    /**
     * Lists Deployment summaries for the active cluster. A null or blank
     * [namespace] lists across all namespaces.
     */
    suspend fun getDeployments(clusterId: String?, namespace: String?): Result<List<DeploymentSummary>>
}

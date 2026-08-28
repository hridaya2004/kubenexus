package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.DeploymentDetails
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import kotlinx.coroutines.flow.Flow

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

    /**
     * Cold stream of cached Deployments, refreshed by [syncDeployments].
     *
     * Offline-first: emits immediately from the Room cache and re-emits on every
     * sync, so screens stay responsive without waiting on the cluster. A null
     * or blank [namespace] (or "All Namespaces") streams the whole cluster.
     */
    fun getDeploymentsStream(clusterId: String?, namespace: String?): Flow<List<DeploymentSummary>>

    /**
     * Pulls the live Deployment list from the cluster and replaces the matching
     * cache scope in one transaction. A null or blank [namespace] syncs all.
     */
    suspend fun syncDeployments(clusterId: String?, namespace: String?): Result<Unit>

    /**
     * Describes one Deployment, including its conditions and events. Events are
     * best-effort: a failure loading them never fails the describe itself.
     */
    suspend fun getDeploymentDetails(
        clusterId: String?,
        namespace: String,
        name: String,
    ): Result<DeploymentDetails>

    /**
     * Cold stream of the last sync timestamp for this cluster's deployments cache,
     * read from the persisted sync_metadata table.
     */
    fun getLastRefreshedStream(clusterId: String?): Flow<Long?>
}

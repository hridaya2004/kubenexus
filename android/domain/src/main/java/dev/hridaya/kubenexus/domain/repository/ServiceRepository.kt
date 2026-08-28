package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.ServiceDetails
import dev.hridaya.kubenexus.domain.model.ServiceSummary
import kotlinx.coroutines.flow.Flow

interface ServiceRepository {

    /**
     * Creates the Service described by [manifestYaml] (the exact text the user
     * reviewed). The reviewed manifest carries its own namespace in
     * metadata.namespace, so none is passed separately. The manifest may be
     * YAML or JSON.
     */
    suspend fun createFromManifest(clusterId: String?, manifestYaml: String): Result<Unit>

    /**
     * Cold stream of cached Services, refreshed by [syncServices].
     *
     * Offline-first: emits immediately from the Room cache and re-emits on every
     * sync, so screens stay responsive without waiting on the cluster. A null
     * or blank [namespace] (or "All Namespaces") streams the whole cluster.
     */
    fun getServicesStream(clusterId: String?, namespace: String?): Flow<List<ServiceSummary>>

    /**
     * Pulls the live Service list from the cluster and replaces the matching
     * cache scope in one transaction. A null or blank [namespace] syncs all.
     */
    suspend fun syncServices(clusterId: String?, namespace: String?): Result<Unit>

    /**
     * Describes one Service, including its ports and events. Events are
     * best-effort: a failure loading them never fails the describe itself.
     */
    suspend fun getServiceDetails(
        clusterId: String?,
        namespace: String,
        name: String,
    ): Result<ServiceDetails>

    /**
     * Cold stream of the last sync timestamp for this cluster's services cache,
     * read from the persisted sync_metadata table.
     */
    fun getLastRefreshedStream(clusterId: String?): Flow<Long?>
}

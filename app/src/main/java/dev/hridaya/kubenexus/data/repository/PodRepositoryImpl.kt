package dev.hridaya.kubenexus.data.repository

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodStatus
import dev.hridaya.kubenexus.domain.repository.PodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class PodRepositoryImpl(
    private val dispatcherProvider: DispatcherProvider
) : PodRepository {

    override fun getPodsStream(clusterId: String?, namespace: String?): Flow<List<Pod>> = flow {
        if (clusterId == null) {
            emit(emptyList())
            return@flow
        }

        val allPods = listOf(
            Pod(
                id = "pod-1",
                name = "coredns-6f6b679f8f-z9p2x",
                namespace = "kube-system",
                status = PodStatus.RUNNING,
                readyContainers = "1/1",
                restarts = 0,
                age = "3d",
                ip = "10.244.0.2",
                node = "k8s-control-plane",
                image = "registry.k8s.io/coredns/coredns:v1.11.1"
            ),
            Pod(
                id = "pod-2",
                name = "local-path-provisioner-7577fdbbfb-k98sq",
                namespace = "kube-system",
                status = PodStatus.RUNNING,
                readyContainers = "1/1",
                restarts = 0,
                age = "3d",
                ip = "10.244.0.3",
                node = "k8s-control-plane",
                image = "rancher/local-path-provisioner:v0.0.26"
            ),
            Pod(
                id = "pod-3",
                name = "metrics-server-8fbfcd848-7vx5g",
                namespace = "kube-system",
                status = PodStatus.RUNNING,
                readyContainers = "1/1",
                restarts = 1,
                age = "2d",
                ip = "10.244.0.4",
                node = "k8s-control-plane",
                image = "registry.k8s.io/metrics-server/metrics-server:v0.7.0"
            ),
            Pod(
                id = "pod-4",
                name = "traefik-ingress-f4564c4f4-5d9p2",
                namespace = "kube-system",
                status = PodStatus.RUNNING,
                readyContainers = "1/1",
                restarts = 0,
                age = "3d",
                ip = "10.244.0.5",
                node = "k8s-worker-1",
                image = "traefik:v2.10"
            ),
            Pod(
                id = "pod-5",
                name = "web-frontend-7b98d4586f-q8n4b",
                namespace = "default",
                status = PodStatus.RUNNING,
                readyContainers = "1/1",
                restarts = 0,
                age = "18h",
                ip = "10.244.1.12",
                node = "k8s-worker-1",
                image = "nginx:1.25-alpine"
            ),
            Pod(
                id = "pod-6",
                name = "api-gateway-service-56f89b94bc-4lmnp",
                namespace = "default",
                status = PodStatus.RUNNING,
                readyContainers = "2/2",
                restarts = 0,
                age = "18h",
                ip = "10.244.1.14",
                node = "k8s-worker-1",
                image = "kubenexus/api-gateway:v1.2.0"
            ),
            Pod(
                id = "pod-7",
                name = "redis-cache-master-0",
                namespace = "default",
                status = PodStatus.RUNNING,
                readyContainers = "1/1",
                restarts = 0,
                age = "1d",
                ip = "10.244.1.15",
                node = "k8s-worker-1",
                image = "redis:7.2-alpine"
            ),
            Pod(
                id = "pod-8",
                name = "prometheus-server-79dbcf988-x8v21",
                namespace = "monitoring",
                status = PodStatus.RUNNING,
                readyContainers = "2/2",
                restarts = 0,
                age = "4d",
                ip = "10.244.2.5",
                node = "k8s-worker-2",
                image = "prom/prometheus:v2.48.0"
            ),
            Pod(
                id = "pod-9",
                name = "grafana-57556b856b-m9n2b",
                namespace = "monitoring",
                status = PodStatus.RUNNING,
                readyContainers = "1/1",
                restarts = 0,
                age = "4d",
                ip = "10.244.2.6",
                node = "k8s-worker-2",
                image = "grafana/grafana:10.2.2"
            )
        )

        val filtered = if (!namespace.isNullOrBlank() && namespace != "All Namespaces") {
            allPods.filter { it.namespace.equals(namespace, ignoreCase = true) }
        } else {
            allPods
        }

        emit(filtered)
    }.flowOn(dispatcherProvider.io)

    override fun getNamespacesStream(clusterId: String?): Flow<List<String>> = flow {
        if (clusterId == null) {
            emit(emptyList())
            return@flow
        }
        emit(listOf("All Namespaces", "default", "kube-system", "monitoring", "kube-public", "kube-node-lease"))
    }.flowOn(dispatcherProvider.io)
}

package dev.hridaya.kubenexus.presentation.deployments

import androidx.lifecycle.viewModelScope
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import dev.hridaya.kubenexus.domain.usecase.GetDeploymentsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeploymentsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeDeploymentsListRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDeploymentsListRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // The Route fires Refresh on lifecycle start, so tests drive loads through
    // the same action the real screen uses.
    private fun vmTest(block: suspend TestScope.(DeploymentsViewModel) -> Unit) = runTest(testDispatcher) {
        val viewModel = DeploymentsViewModel(
            clusterId = "c-1",
            namespace = "team-a",
            getDeploymentsUseCase = GetDeploymentsUseCase(fakeRepository),
        )
        try {
            viewModel.onAction(DeploymentsUiAction.Refresh)
            block(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private fun summary(name: String, ready: Int = 1) = DeploymentSummary(
        id = "team-a/$name",
        name = name,
        namespace = "team-a",
        desiredReplicas = 1,
        readyReplicas = ready,
        availableReplicas = ready,
        images = listOf("nginx:1.27"),
        creationTimestampMillis = 0L,
    )

    @Test
    fun `refresh loads deployments and clears loading state`() = vmTest { viewModel ->
        fakeRepository.result = Result.Success(listOf(summary("web"), summary("api")))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
        assertEquals(listOf("web", "api"), state.deployments.map { it.name })
    }

    @Test
    fun `load failure surfaces friendly error without raw message`() = vmTest { viewModel ->
        fakeRepository.result = Result.Error(AppError.Network(message = "dial tcp 10.0.0.1:6443 refused"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.deployments.isEmpty())
        assertEquals(
            "Couldn't load your deployments right now. Check that the cluster is reachable and try again.",
            state.errorMessage,
        )
    }

    @Test
    fun `retry after failure recovers the list`() = vmTest { viewModel ->
        fakeRepository.result = Result.Error(AppError.Unknown(message = "boom"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.deployments.isEmpty())

        fakeRepository.result = Result.Success(listOf(summary("web")))
        viewModel.onAction(DeploymentsUiAction.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertFalse(state.isRefreshing)
        assertEquals(1, state.deployments.size)
    }

    @Test
    fun `refresh failure keeps existing deployments and stops refreshing`() = vmTest { viewModel ->
        fakeRepository.result = Result.Success(listOf(summary("web")))
        advanceUntilIdle()

        fakeRepository.result = Result.Error(AppError.Unknown(message = "timeout"))
        viewModel.onAction(DeploymentsUiAction.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        // Previously loaded data stays visible alongside the error.
        assertEquals(1, state.deployments.size)
    }

    @Test
    fun `refresh passes cluster and namespace through to the use case`() = vmTest { viewModel ->
        fakeRepository.result = Result.Success(emptyList())
        advanceUntilIdle()

        viewModel.onAction(DeploymentsUiAction.Refresh)
        advanceUntilIdle()

        // One call from the helper's initial Refresh (the lifecycle-start
        // equivalent) plus one for the explicit refresh below.
        assertEquals(
            listOf(
                Pair<String?, String?>("c-1", "team-a"),
                Pair<String?, String?>("c-1", "team-a"),
            ),
            fakeRepository.calls,
        )
    }
}

private class FakeDeploymentsListRepository : DeploymentRepository {
    var result: Result<List<DeploymentSummary>> = Result.Success(emptyList())
    val calls = mutableListOf<Pair<String?, String?>>()

    override suspend fun getDeployments(
        clusterId: String?,
        namespace: String?,
    ): Result<List<DeploymentSummary>> {
        calls += clusterId to namespace
        return result
    }

    override suspend fun createFromManifest(clusterId: String?, manifestYaml: String): Result<Unit> {
        throw UnsupportedOperationException("not used in this test")
    }
}

package dev.hridaya.kubenexus.presentation.deployments

import androidx.lifecycle.viewModelScope
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import dev.hridaya.kubenexus.domain.usecase.CreateDeploymentUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class CreateDeploymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var fakeDeploymentRepository: FakeDeploymentRepository
    private lateinit var viewModel: CreateDeploymentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDeploymentRepository = FakeDeploymentRepository()
        viewModel = CreateDeploymentViewModel(
            clusterId = "c-1",
            initialNamespace = "team-a",
            createDeploymentUseCase = CreateDeploymentUseCase(fakeDeploymentRepository),
            dispatcherProvider = testDispatcherProvider,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vmTest(block: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        try {
            block()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private fun fillValidForm() {
        viewModel.onAction(CreateDeploymentUiAction.NameChanged("web-app"))
        viewModel.onAction(CreateDeploymentUiAction.ImageChanged("nginx:1.27"))
        viewModel.onAction(CreateDeploymentUiAction.ContainerPortChanged("8080"))
        // replicas keeps its "1" default and namespace its "team-a" initial value.
    }

    @Test
    fun `preview success moves to review step with generated yaml`() = vmTest {
        fillValidForm()
        assertTrue(viewModel.uiState.value.fieldErrors.isEmpty())

        viewModel.onAction(CreateDeploymentUiAction.PreviewSubmitted)

        val state = viewModel.uiState.value
        assertEquals(CreateDeploymentStep.REVIEW, state.step)
        assertNull(state.errorMessage)
        assertNotNull(state.generatedYaml)
        val yaml = state.generatedYaml!!
        assertTrue(yaml.contains("kind: Deployment"))
        assertTrue(yaml.contains("name: web-app"))
        assertTrue(yaml.contains("namespace: team-a"))
        assertTrue(yaml.contains("image: nginx:1.27"))
        assertTrue(yaml.contains("replicas: 1"))
        assertTrue(yaml.contains("containerPort: 8080"))
    }

    @Test
    fun `invalid draft surfaces field errors without clearing input`() = vmTest {
        viewModel.onAction(CreateDeploymentUiAction.NameChanged("Invalid_Name"))
        viewModel.onAction(CreateDeploymentUiAction.ImageChanged("nginx 1.27"))
        viewModel.onAction(CreateDeploymentUiAction.ReplicasChanged("9999"))
        viewModel.onAction(CreateDeploymentUiAction.ContainerPortChanged("abc"))

        val state = viewModel.uiState.value
        assertEquals(
            setOf("name", "image", "replicas", "containerPort"),
            state.fieldErrors.keys,
        )
        // Every keystroke survived validation - input is never discarded.
        assertEquals("Invalid_Name", state.name)
        assertEquals("nginx 1.27", state.image)
        assertEquals("9999", state.replicas)
        assertEquals("abc", state.containerPort)
        assertEquals("team-a", state.namespace)

        // Preview is blocked while the draft is invalid.
        viewModel.onAction(CreateDeploymentUiAction.PreviewSubmitted)
        assertEquals(CreateDeploymentStep.FORM, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.generatedYaml)
        assertNotNull(viewModel.uiState.value.errorMessage)

        // Errors clear live as the user fixes the input.
        viewModel.onAction(CreateDeploymentUiAction.NameChanged("web-app"))
        assertFalse(viewModel.uiState.value.fieldErrors.containsKey("name"))
        viewModel.onAction(CreateDeploymentUiAction.ReplicasChanged("3"))
        assertFalse(viewModel.uiState.value.fieldErrors.containsKey("replicas"))
    }

    @Test
    fun `apply success emits created effect and applies the reviewed yaml`() = vmTest {
        fillValidForm()
        viewModel.onAction(CreateDeploymentUiAction.PreviewSubmitted)
        val reviewedYaml = viewModel.uiState.value.generatedYaml
        assertNotNull(reviewedYaml)

        val effects = mutableListOf<CreateDeploymentUiEffect>()
        val collector = launch { viewModel.effects.collect { effects += it } }
        runCurrent()

        viewModel.onAction(CreateDeploymentUiAction.ApplySubmitted)
        advanceUntilIdle()
        collector.cancel()

        assertEquals(listOf(CreateDeploymentUiEffect.Created("web-app")), effects)
        assertEquals(1, fakeDeploymentRepository.appliedManifests.size)
        val (clusterId, manifest) = fakeDeploymentRepository.appliedManifests.single()
        assertEquals("c-1", clusterId)
        assertEquals(reviewedYaml, manifest)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(CreateDeploymentStep.REVIEW, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `apply failure keeps review step and input intact and shows error`() = vmTest {
        fakeDeploymentRepository.applyError =
            AppError.Unknown(message = "forbidden: deployments is forbidden")
        fillValidForm()
        viewModel.onAction(CreateDeploymentUiAction.PreviewSubmitted)
        val reviewedYaml = viewModel.uiState.value.generatedYaml
        assertNotNull(reviewedYaml)

        viewModel.onAction(CreateDeploymentUiAction.ApplySubmitted)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Still on review with everything the user entered preserved.
        assertEquals(CreateDeploymentStep.REVIEW, state.step)
        assertEquals(reviewedYaml, state.generatedYaml)
        assertEquals("web-app", state.name)
        assertEquals("nginx:1.27", state.image)
        assertEquals("8080", state.containerPort)
        assertEquals("team-a", state.namespace)
        assertFalse(state.isSubmitting)
        assertEquals("forbidden: deployments is forbidden", state.errorMessage)
    }
}

private class FakeDeploymentRepository : DeploymentRepository {
    var applyError: AppError? = null
    val appliedManifests = mutableListOf<Pair<String?, String>>()

    override suspend fun createFromManifest(
        clusterId: String?,
        manifestYaml: String,
    ): Result<Unit> {
        appliedManifests += clusterId to manifestYaml
        val error = applyError ?: return Result.Success(Unit)
        return Result.Error(error)
    }
}

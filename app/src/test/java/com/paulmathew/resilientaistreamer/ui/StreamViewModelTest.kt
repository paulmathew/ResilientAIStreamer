package com.paulmathew.resilientaistreamer.ui
import com.paulmathew.resilientaistreamer.domain.model.AiStreamEvent
import com.paulmathew.resilientaistreamer.domain.model.ImageInput
import com.paulmathew.resilientaistreamer.domain.model.StreamInterruptedException
import com.paulmathew.resilientaistreamer.domain.model.StreamResult
import com.paulmathew.resilientaistreamer.domain.model.TextExtractionResult
import com.paulmathew.resilientaistreamer.domain.repository.EventLogger
import com.paulmathew.resilientaistreamer.domain.repository.StreamRepository
import com.paulmathew.resilientaistreamer.domain.repository.TextExtractor
import com.paulmathew.resilientaistreamer.domain.usecase.InitiateAIStreamUseCase
import com.paulmathew.resilientaistreamer.ui.StreamViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StreamViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun interruptedStream_preservesPartialText_andResumes() =
        runTest(testDispatcher) {
            val logger = FakeEventLogger()

            val viewModel = StreamViewModel(
                textExtractor = FakeTextExtractor(),
                initiateAIStream =
                    InitiateAIStreamUseCase(FakeStreamRepository()),
                eventLogger = logger,
            )

            viewModel.onImageSelected(
                uriString = "content://test/image",
                displayName = "test-image.png",
            )

            viewModel.analyzeSelectedImage()
            advanceUntilIdle()

            val interruptedState = viewModel.uiState.value

            assertEquals(
                "Hello ",
                interruptedState.streamedResponse,
            )
            assertTrue(interruptedState.canResume)
            assertEquals(
                1,
                interruptedState.failedAtChunkIndex,
            )

            viewModel.resumeStream()
            advanceUntilIdle()

            val completedState = viewModel.uiState.value

            assertEquals(
                "Hello world",
                completedState.streamedResponse,
            )
            assertFalse(completedState.canResume)
            assertFalse(completedState.isStreaming)
            assertEquals(1, completedState.retryCount)
            assertEquals(2, completedState.receivedChunkCount)

            assertTrue(
                logger.events.any {
                    it.type ==
                            AiStreamEvent.Type.STREAM_INTERRUPTED
                },
            )

            assertTrue(
                logger.events.any {
                    it.type ==
                            AiStreamEvent.Type.STREAM_RETRY_STARTED
                },
            )

            assertTrue(
                logger.events.any {
                    it.type ==
                            AiStreamEvent.Type.STREAM_COMPLETED
                },
            )
        }
}

private class FakeTextExtractor : TextExtractor {

    override suspend fun extractText(
        input: ImageInput,
    ): TextExtractionResult {
        return TextExtractionResult(
            text = "Sample extracted text",
            blockCount = 1,
            latencyMs = 50,
        )
    }
}

private class FakeStreamRepository : StreamRepository {

    override fun streamResponse(
        extractedText: String,
        resumeFromChunkIndex: Int,
    ): Flow<StreamResult> = flow {
        if (resumeFromChunkIndex == 0) {
            emit(
                StreamResult.Chunk(
                    index = 0,
                    text = "Hello ",
                    latencyMs = 100,
                ),
            )

            throw StreamInterruptedException(
                failedAtChunkIndex = 1,
                recoveredText = "Hello ",
            )
        }

        emit(
            StreamResult.Chunk(
                index = 1,
                text = "world",
                latencyMs = 120,
            ),
        )

        emit(
            StreamResult.Completed(
                totalChunks = 2,
                totalLatencyMs = 220,
            ),
        )
    }
}

private class FakeEventLogger : EventLogger {

    val events = mutableListOf<AiStreamEvent>()

    override fun log(event: AiStreamEvent) {
        events += event
    }
}

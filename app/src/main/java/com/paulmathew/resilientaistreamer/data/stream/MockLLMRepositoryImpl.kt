package com.paulmathew.resilientaistreamer.data.stream

import com.paulmathew.resilientaistreamer.domain.model.StreamInterruptedException
import com.paulmathew.resilientaistreamer.domain.model.StreamResult
import com.paulmathew.resilientaistreamer.domain.repository.StreamRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.time.TimeSource

@Singleton
class MockLLMRepositoryImpl @Inject constructor() : StreamRepository {

    override fun streamResponse(
        extractedText: String,
        resumeFromChunkIndex: Int,
    ): Flow<StreamResult> = flow {
        val chunks = buildResponseChunks(extractedText)

        require(resumeFromChunkIndex in 0..chunks.size) {
            "Resume index $resumeFromChunkIndex is outside 0..${chunks.size}"
        }

        val streamStarted = TimeSource.Monotonic.markNow()
        val failureIndex = chooseFailureIndex(
            resumeFromChunkIndex = resumeFromChunkIndex,
            chunkCount = chunks.size,
        )

        for (index in resumeFromChunkIndex until chunks.size) {
            val chunkStarted = TimeSource.Monotonic.markNow()

            delay(simulatedDelayMs())

            if (index == failureIndex) {
                throw StreamInterruptedException(
                    failedAtChunkIndex = index,
                    recoveredText = chunks
                        .take(index)
                        .joinToString(separator = ""),
                )
            }

            emit(
                StreamResult.Chunk(
                    index = index,
                    text = chunks[index],
                    latencyMs = chunkStarted.elapsedNow().inWholeMilliseconds,
                ),
            )
        }

        emit(
            StreamResult.Completed(
                totalChunks = chunks.size,
                totalLatencyMs =
                    streamStarted.elapsedNow().inWholeMilliseconds,
            ),
        )
    }

    private fun buildResponseChunks(
        extractedText: String,
    ): List<String> {
        val normalizedText = extractedText
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_INPUT_CHARACTERS)

        val response = buildString {
            append("I analyzed the text extracted from the image. ")

            if (normalizedText.isBlank()) {
                append("No readable text was detected. ")
                append("Try selecting a clearer or higher-resolution image.")
            } else {
                append("The image appears to contain: ")
                append("\"$normalizedText\". ")
                append(
                    "This response is being delivered incrementally " +
                        "to demonstrate resilient AI streaming.",
                )
            }
        }

        return response
            .split(Regex("(?<=\\s)"))
            .filter(String::isNotEmpty)
    }

    private fun chooseFailureIndex(
        resumeFromChunkIndex: Int,
        chunkCount: Int,
    ): Int? {
        val hasRemainingChunks = resumeFromChunkIndex < chunkCount
        val shouldFail =
            hasRemainingChunks && Random.nextFloat() < FAILURE_RATE

        return if (shouldFail) {
            Random.nextInt(
                from = resumeFromChunkIndex,
                until = chunkCount,
            )
        } else {
            null
        }
    }

    private fun simulatedDelayMs(): Long {
        val isLatencySpike =
            Random.nextFloat() < LATENCY_SPIKE_RATE

        return if (isLatencySpike) {
            Random.nextLong(
                from = SPIKE_DELAY_MIN_MS,
                until = SPIKE_DELAY_MAX_MS,
            )
        } else {
            Random.nextLong(
                from = NORMAL_DELAY_MIN_MS,
                until = NORMAL_DELAY_MAX_MS,
            )
        }
    }

    private companion object {
        const val FAILURE_RATE = 0.30f
        const val LATENCY_SPIKE_RATE = 0.10f
        const val MAX_INPUT_CHARACTERS = 600

        const val NORMAL_DELAY_MIN_MS = 80L
        const val NORMAL_DELAY_MAX_MS = 241L

        const val SPIKE_DELAY_MIN_MS = 700L
        const val SPIKE_DELAY_MAX_MS = 1_201L
    }
}
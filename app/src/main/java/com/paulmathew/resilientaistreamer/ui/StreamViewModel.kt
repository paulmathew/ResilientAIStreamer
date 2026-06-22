package com.paulmathew.resilientaistreamer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulmathew.resilientaistreamer.domain.model.AiStreamEvent
import com.paulmathew.resilientaistreamer.domain.model.ImageInput
import com.paulmathew.resilientaistreamer.domain.model.StreamInterruptedException
import com.paulmathew.resilientaistreamer.domain.model.StreamResult
import com.paulmathew.resilientaistreamer.domain.repository.EventLogger
import com.paulmathew.resilientaistreamer.domain.repository.TextExtractor
import com.paulmathew.resilientaistreamer.domain.usecase.InitiateAIStreamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
    private val textExtractor: TextExtractor,
    private val initiateAIStream: InitiateAIStreamUseCase,
    private val eventLogger: EventLogger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreamUiState())
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private var processingJob: Job? = null

    fun onImageSelected(
        uriString: String,
        displayName: String? = null,
    ) {
        processingJob?.cancel()

        _uiState.value = StreamUiState(
            selectedImage = ImageInput(
                uriString = uriString,
                displayName = displayName,
            ),
        )

        logEvent(
            type = AiStreamEvent.Type.IMAGE_SELECTED,
            attributes = mapOf(
                "displayName" to (displayName ?: "unknown"),
            ),
        )
    }

    fun analyzeSelectedImage() {
        val image = _uiState.value.selectedImage

        if (image == null) {
            _uiState.update {
                it.copy(errorMessage = "Select an image first.")
            }
            return
        }

        if (processingJob?.isActive == true) return

        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    extractedText = "",
                    streamedResponse = "",
                    isExtracting = true,
                    isStreaming = false,
                    canResume = false,
                    failedAtChunkIndex = null,
                    warningMessage = null,
                    errorMessage = null,
                    retryCount = 0,
                    receivedChunkCount = 0,
                    lastChunkLatencyMs = 0,
                    totalStreamLatencyMs = 0,
                )
            }

            logEvent(AiStreamEvent.Type.TEXT_EXTRACTION_STARTED)

            try {
                val result = textExtractor.extractText(image)

                _uiState.update {
                    it.copy(
                        extractedText = result.text,
                        isExtracting = false,
                    )
                }

                logEvent(
                    type = AiStreamEvent.Type.TEXT_EXTRACTION_COMPLETED,
                    attributes = mapOf(
                        "latencyMs" to result.latencyMs.toString(),
                        "blockCount" to result.blockCount.toString(),
                        "characterCount" to result.text.length.toString(),
                    ),
                )

                if (result.text.isBlank()) {
                    _uiState.update {
                        it.copy(
                            errorMessage =
                                "No readable text was found in the image.",
                        )
                    }
                    return@launch
                }

                collectStream(resumeFromChunkIndex = 0)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        isStreaming = false,
                        errorMessage =
                            error.message ?: "Text extraction failed.",
                    )
                }
            }
        }
    }

    fun resumeStream() {
        val state = _uiState.value
        val resumeIndex = state.failedAtChunkIndex ?: return

        if (state.extractedText.isBlank()) return
        if (processingJob?.isActive == true) return

        _uiState.update {
            it.copy(retryCount = it.retryCount + 1)
        }

        logEvent(
            type = AiStreamEvent.Type.STREAM_RETRY_STARTED,
            attributes = mapOf(
                "resumeIndex" to resumeIndex.toString(),
                "retryCount" to _uiState.value.retryCount.toString(),
            ),
        )

        processingJob = viewModelScope.launch {
            collectStream(resumeFromChunkIndex = resumeIndex)
        }
    }

    private suspend fun collectStream(
        resumeFromChunkIndex: Int,
    ) {
        _uiState.update {
            it.copy(
                isStreaming = true,
                canResume = false,
                failedAtChunkIndex = null,
                warningMessage = null,
                errorMessage = null,
            )
        }

        logEvent(
            type = AiStreamEvent.Type.STREAM_STARTED,
            attributes = mapOf(
                "resumeIndex" to resumeFromChunkIndex.toString(),
            ),
        )

        initiateAIStream(
            extractedText = _uiState.value.extractedText,
            resumeFromChunkIndex = resumeFromChunkIndex,
        )
            .catch { error ->
                when (error) {
                    is CancellationException -> throw error

                    is StreamInterruptedException -> {
                        _uiState.update {
                            it.copy(
                                streamedResponse = error.recoveredText,
                                isStreaming = false,
                                canResume = true,
                                failedAtChunkIndex =
                                    error.failedAtChunkIndex,
                                warningMessage =
                                    "The stream was interrupted. " +
                                        "Your partial response was preserved.",
                            )
                        }

                        logEvent(
                            type = AiStreamEvent.Type.STREAM_INTERRUPTED,
                            attributes = mapOf(
                                "failedAtChunkIndex" to
                                    error.failedAtChunkIndex.toString(),
                                "recoveredCharacters" to
                                    error.recoveredText.length.toString(),
                            ),
                        )
                    }

                    else -> {
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                errorMessage =
                                    error.message ?: "Streaming failed.",
                            )
                        }
                    }
                }
            }
            .collect { result ->
                when (result) {
                    is StreamResult.Chunk -> handleChunk(result)
                    is StreamResult.Completed -> handleCompletion(result)
                }
            }
    }

    private fun handleChunk(chunk: StreamResult.Chunk) {
        _uiState.update {
            it.copy(
                streamedResponse = it.streamedResponse + chunk.text,
                receivedChunkCount = chunk.index + 1,
                lastChunkLatencyMs = chunk.latencyMs,
            )
        }

        logEvent(
            type = AiStreamEvent.Type.STREAM_CHUNK_RECEIVED,
            attributes = mapOf(
                "chunkIndex" to chunk.index.toString(),
                "latencyMs" to chunk.latencyMs.toString(),
            ),
        )

        if (chunk.latencyMs >= LATENCY_SPIKE_THRESHOLD_MS) {
            logEvent(
                type = AiStreamEvent.Type.STREAM_LATENCY_SPIKE,
                attributes = mapOf(
                    "chunkIndex" to chunk.index.toString(),
                    "latencyMs" to chunk.latencyMs.toString(),
                ),
            )
        }
    }

    private fun handleCompletion(
        result: StreamResult.Completed,
    ) {
        _uiState.update {
            it.copy(
                isStreaming = false,
                canResume = false,
                failedAtChunkIndex = null,
                warningMessage = null,
                totalStreamLatencyMs = result.totalLatencyMs,
            )
        }

        logEvent(
            type = AiStreamEvent.Type.STREAM_COMPLETED,
            attributes = mapOf(
                "totalChunks" to result.totalChunks.toString(),
                "totalLatencyMs" to result.totalLatencyMs.toString(),
                "retryCount" to _uiState.value.retryCount.toString(),
            ),
        )
    }

    private fun logEvent(
        type: AiStreamEvent.Type,
        attributes: Map<String, String> = emptyMap(),
    ) {
        eventLogger.log(
            AiStreamEvent(
                type = type,
                timestampMs = System.currentTimeMillis(),
                attributes = attributes,
            ),
        )
    }

    private companion object {
        const val LATENCY_SPIKE_THRESHOLD_MS = 600L
    }
}
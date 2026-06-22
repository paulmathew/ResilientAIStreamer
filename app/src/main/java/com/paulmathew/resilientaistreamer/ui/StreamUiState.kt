package com.paulmathew.resilientaistreamer.ui

import com.paulmathew.resilientaistreamer.domain.model.ImageInput

data class StreamUiState(
    val selectedImage: ImageInput? = null,
    val extractedText: String = "",
    val streamedResponse: String = "",
    val isExtracting: Boolean = false,
    val isStreaming: Boolean = false,
    val canResume: Boolean = false,
    val failedAtChunkIndex: Int? = null,
    val warningMessage: String? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val receivedChunkCount: Int = 0,
    val lastChunkLatencyMs: Long = 0,
    val totalStreamLatencyMs: Long = 0,
)
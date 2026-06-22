package com.paulmathew.resilientaistreamer.domain.model

sealed interface StreamResult {

    data class Chunk(
        val index: Int,
        val text: String,
        val latencyMs: Long,
    ) : StreamResult

    data class Completed(
        val totalChunks: Int,
        val totalLatencyMs: Long,
    ) : StreamResult
}
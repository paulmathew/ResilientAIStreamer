package com.paulmathew.resilientaistreamer.domain.model
data class AiStreamEvent(
    val type: Type,
    val timestampMs: Long,
    val attributes: Map<String, String> = emptyMap(),
) {
    enum class Type {
        IMAGE_SELECTED,
        TEXT_EXTRACTION_STARTED,
        TEXT_EXTRACTION_COMPLETED,
        STREAM_STARTED,
        STREAM_CHUNK_RECEIVED,
        STREAM_LATENCY_SPIKE,
        STREAM_INTERRUPTED,
        STREAM_RETRY_STARTED,
        STREAM_COMPLETED,
    }
}
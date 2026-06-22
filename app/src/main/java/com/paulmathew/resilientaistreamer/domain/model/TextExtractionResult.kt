package com.paulmathew.resilientaistreamer.domain.model

data class TextExtractionResult(
    val text: String,
    val blockCount: Int,
    val latencyMs: Long,
)

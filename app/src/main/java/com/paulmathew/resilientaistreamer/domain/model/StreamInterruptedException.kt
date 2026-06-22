package com.paulmathew.resilientaistreamer.domain.model

class StreamInterruptedException(
    val failedAtChunkIndex: Int,
    val recoveredText: String,
    message: String = "Stream interrupted at chunk $failedAtChunkIndex",
    cause: Throwable? = null,
) : Exception(message, cause)
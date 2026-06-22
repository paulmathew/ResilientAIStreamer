package com.paulmathew.resilientaistreamer.domain.repository

import com.paulmathew.resilientaistreamer.domain.model.StreamResult
import kotlinx.coroutines.flow.Flow

interface StreamRepository {
    fun streamResponse(
        extractedText: String,
        resumeFromChunkIndex: Int = 0,
    ): Flow<StreamResult>
}
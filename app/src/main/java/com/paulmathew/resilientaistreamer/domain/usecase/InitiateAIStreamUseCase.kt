package com.paulmathew.resilientaistreamer.domain.usecase

import com.paulmathew.resilientaistreamer.domain.model.StreamResult
import com.paulmathew.resilientaistreamer.domain.repository.StreamRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class InitiateAIStreamUseCase @Inject constructor(
    private val streamRepository: StreamRepository,
) {
    operator fun invoke(
        extractedText: String,
        resumeFromChunkIndex: Int = 0,
    ): Flow<StreamResult> {
        require(extractedText.isNotBlank()) {
            "Extracted text must not be blank"
        }
        require(resumeFromChunkIndex >= 0) {
            "Resume index must not be negative"
        }

        return streamRepository.streamResponse(
            extractedText = extractedText,
            resumeFromChunkIndex = resumeFromChunkIndex,
        )
    }
}
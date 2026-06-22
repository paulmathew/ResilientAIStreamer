package com.paulmathew.resilientaistreamer.domain.repository

import com.paulmathew.resilientaistreamer.domain.model.ImageInput
import com.paulmathew.resilientaistreamer.domain.model.TextExtractionResult

interface TextExtractor {
    suspend fun extractText(input: ImageInput): TextExtractionResult
}
package com.paulmathew.resilientaistreamer.data.ml

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.paulmathew.resilientaistreamer.domain.model.ImageInput
import com.paulmathew.resilientaistreamer.domain.model.TextExtractionResult
import com.paulmathew.resilientaistreamer.domain.repository.TextExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextExtractorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TextExtractor {

    private val recognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun extractText(
        input: ImageInput,
    ): TextExtractionResult = withContext(Dispatchers.IO) {
        val startedAtMs = SystemClock.elapsedRealtime()

        val image = InputImage.fromFilePath(
            context,
            Uri.parse(input.uriString),
        )

        val recognizedText = recognizer.process(image).await()

        TextExtractionResult(
            text = recognizedText.text.trim(),
            blockCount = recognizedText.textBlocks.size,
            latencyMs = SystemClock.elapsedRealtime() - startedAtMs,
        )
    }
}
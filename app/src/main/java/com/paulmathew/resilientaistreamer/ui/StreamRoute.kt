package com.paulmathew.resilientaistreamer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulmathew.resilientaistreamer.domain.model.ImageInput
import com.paulmathew.resilientaistreamer.ui.theme.ResilientAIStreamerTheme

@Composable
fun StreamRoute(
    viewModel: StreamViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            viewModel.onImageSelected(
                uriString = it.toString(),
                displayName = it.lastPathSegment,
            )
        }
    }

    StreamScreen(
        state = state,
        onSelectImage = {
            imagePicker.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                ),
            )
        },
        onAnalyze = viewModel::analyzeSelectedImage,
        onResume = viewModel::resumeStream,
    )
}

@Composable
fun StreamScreen(
    state: StreamUiState,
    onSelectImage: () -> Unit,
    onAnalyze: () -> Unit,
    onResume: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val isBusy = state.isExtracting || state.isStreaming

    LaunchedEffect(
        state.streamedResponse,
        scrollState.maxValue,
    ) {
        if (state.streamedResponse.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Resilient AI Streamer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "On-device text recognition with a resumable AI stream.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "1. Select an image",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                state.selectedImage?.let { image ->
                    Text(
                        text = image.displayName ?: "Image selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onSelectImage,
                        enabled = !isBusy,
                    ) {
                        Text("Select image")
                    }

                    Button(
                        onClick = onAnalyze,
                        enabled = state.selectedImage != null && !isBusy,
                    ) {
                        Text("Analyze")
                    }
                }

                if (state.isExtracting) {
                    OperationProgress("Extracting text on device…")
                }
            }
        }

        if (state.extractedText.isNotBlank()) {
            ContentCard(
                title = "2. Extracted text",
                content = state.extractedText,
            )
        }

        if (
            state.isStreaming ||
            state.streamedResponse.isNotBlank() ||
            state.canResume
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "3. Streaming response",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    if (state.streamedResponse.isBlank()) {
                        Text(
                            text = "Waiting for the first response chunk…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = state.streamedResponse,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    if (state.isStreaming) {
                        OperationProgress("Receiving response…")
                    }
                }
            }
        }

        state.warningMessage?.let { warning ->
            WarningBanner(
                message = warning,
                canResume = state.canResume,
                onResume = onResume,
            )
        }

        state.errorMessage?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        if (
            state.receivedChunkCount > 0 ||
            state.retryCount > 0 ||
            state.totalStreamLatencyMs > 0
        ) {
            DiagnosticsCard(state)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ContentCard(
    title: String,
    content: String,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun OperationProgress(
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun WarningBanner(
    message: String,
    canResume: Boolean,
    onResume: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )

            if (canResume) {
                Button(onClick = onResume) {
                    Text("Resume stream")
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(
    state: StreamUiState,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Diagnostics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            DiagnosticRow(
                label = "Chunks received",
                value = state.receivedChunkCount.toString(),
            )

            DiagnosticRow(
                label = "Last chunk latency",
                value = "${state.lastChunkLatencyMs} ms",
            )

            DiagnosticRow(
                label = "Retry count",
                value = state.retryCount.toString(),
            )

            if (state.totalStreamLatencyMs > 0) {
                DiagnosticRow(
                    label = "Completed stream time",
                    value = "${state.totalStreamLatencyMs} ms",
                )
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = value,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(
    name = "Initial",
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun InitialStreamScreenPreview() {
    ResilientAIStreamerTheme {
        StreamScreen(
            state = StreamUiState(),
            onSelectImage = {},
            onAnalyze = {},
            onResume = {},
        )
    }
}

@Preview(
    name = "Streaming",
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun StreamingScreenPreview() {
    ResilientAIStreamerTheme {
        StreamScreen(
            state = StreamUiState(
                selectedImage = ImageInput(
                    uriString = "content://preview/sample",
                    displayName = "receipt.jpg",
                ),
                extractedText =
                    "Order number 4821. Total amount ₹1,250.",
                streamedResponse =
                    "I analyzed the text extracted from the image. ",
                isStreaming = true,
                receivedChunkCount = 8,
                lastChunkLatencyMs = 142,
            ),
            onSelectImage = {},
            onAnalyze = {},
            onResume = {},
        )
    }
}

@Preview(
    name = "Interrupted",
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun InterruptedScreenPreview() {
    ResilientAIStreamerTheme {
        StreamScreen(
            state = StreamUiState(
                selectedImage = ImageInput(
                    uriString = "content://preview/sample",
                    displayName = "document.jpg",
                ),
                extractedText = "Resilient Android streaming demo",
                streamedResponse =
                    "I analyzed the text extracted from the image. ",
                canResume = true,
                failedAtChunkIndex = 9,
                warningMessage =
                    "The stream was interrupted. " +
                            "Your partial response was preserved.",
                retryCount = 1,
                receivedChunkCount = 9,
                lastChunkLatencyMs = 891,
            ),
            onSelectImage = {},
            onAnalyze = {},
            onResume = {},
        )
    }
}
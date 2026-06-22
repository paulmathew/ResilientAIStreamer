package com.paulmathew.resilientaistreamer.ui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            viewModel.onImageSelected(
                uriString = it.toString(),
                displayName = "Image selected",
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

    LaunchedEffect(state.streamedResponse, scrollState.maxValue) {
        if (state.streamedResponse.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroSection(state)

            InputCard(
                state = state,
                isBusy = isBusy,
                onSelectImage = onSelectImage,
                onAnalyze = onAnalyze,
            )

            if (state.extractedText.isNotBlank()) {
                ResultCard(
                    eyebrow = "ON-DEVICE OCR",
                    title = "Extracted text",
                    content = state.extractedText,
                    accent = MaterialTheme.colorScheme.secondary,
                )
            }

            if (
                state.isStreaming ||
                state.streamedResponse.isNotBlank() ||
                state.canResume
            ) {
                StreamResponseCard(state)
            }

            state.warningMessage?.let {
                RecoveryCard(
                    message = it,
                    canResume = state.canResume,
                    onResume = onResume,
                )
            }

            state.errorMessage?.let {
                ErrorCard(it)
            }

            if (
                state.receivedChunkCount > 0 ||
                state.retryCount > 0 ||
                state.totalStreamLatencyMs > 0
            ) {
                DiagnosticsSection(state)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HeroSection(
    state: StreamUiState,
) {
    val stage = when {
        state.streamedResponse.isNotBlank() ||
                state.isStreaming ||
                state.canResume -> 2

        state.selectedImage != null -> 1
        else -> 0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                shape = RoundedCornerShape(30.dp),
            )
            .padding(22.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StatusPill("ON-DEVICE • RESUMABLE")

            Text(
                text = "Resilient AI\nStreamer",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
            )

            Text(
                text =
                    "Extract text locally. Stream an AI response. " +
                            "Recover without losing progress.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Pipeline(stage)
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        shape = CircleShape,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 7.dp,
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun Pipeline(activeStage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Select", "Extract", "Stream")
            .forEachIndexed { index, label ->
                val selected = index <= activeStage

                Surface(
                    modifier = Modifier.weight(1f),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = CircleShape,
                ) {
                    Text(
                        text = "${index + 1}  $label",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 9.dp,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
    }
}

@Composable
private fun InputCard(
    state: StreamUiState,
    isBusy: Boolean,
    onSelectImage: () -> Unit,
    onAnalyze: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionHeading(
                eyebrow = "SOURCE",
                title = "Choose an image",
                description =
                    "Text recognition happens privately on this device.",
            )

            state.selectedImage?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                    ) {
                        Text(
                            text = "READY TO ANALYZE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = it.displayName ?: "Selected image",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onSelectImage,
                    enabled = !isBusy,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    ),
                ) {
                    Text(
                        if (state.selectedImage == null) {
                            "Select image"
                        } else {
                            "Change"
                        },
                    )
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onAnalyze,
                    enabled = state.selectedImage != null && !isBusy,
                ) {
                    Text("Analyze")
                }
            }

            if (state.isExtracting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Running on-device text recognition…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResultCard(
    eyebrow: String,
    title: String,
    content: String,
    accent: Color,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .heightIn(min = 150.dp)
                    .background(accent),
            )

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionHeading(
                    eyebrow = eyebrow,
                    title = title,
                )

                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun StreamResponseCard(
    state: StreamUiState,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeading(
                    eyebrow = "AI RESPONSE",
                    title = "Resilient stream",
                )

                if (state.isStreaming) {
                    StatusPill("LIVE")
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = when {
                        state.streamedResponse.isBlank() ->
                            "Waiting for the first response chunk…"

                        state.isStreaming ->
                            state.streamedResponse + " ▍"

                        else -> state.streamedResponse
                    },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (state.isStreaming) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text =
                        "${state.receivedChunkCount} chunks received • " +
                                "${state.lastChunkLatencyMs} ms latest",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecoveryCard(
    message: String,
    canResume: Boolean,
    onResume: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiary
            .copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Stream paused safely",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (canResume) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onResume,
                ) {
                    Text("Resume from failure point")
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun DiagnosticsSection(
    state: StreamUiState,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeading(
            eyebrow = "OBSERVABILITY",
            title = "Stream diagnostics",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                modifier = Modifier.weight(1f),
                value = state.receivedChunkCount.toString(),
                label = "Chunks",
            )

            MetricTile(
                modifier = Modifier.weight(1f),
                value = "${state.lastChunkLatencyMs}",
                label = "Latency ms",
            )

            MetricTile(
                modifier = Modifier.weight(1f),
                value = state.retryCount.toString(),
                label = "Retries",
            )
        }

        if (state.totalStreamLatencyMs > 0) {
            MetricTile(
                modifier = Modifier.fillMaxWidth(),
                value = "${state.totalStreamLatencyMs} ms",
                label = "Completed stream duration",
            )
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier,
    value: String,
    label: String,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeading(
    eyebrow: String,
    title: String,
    description: String? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
private fun ContentResolver.getDisplayName(
    uri: Uri,
): String? {
    return query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(
            OpenableColumns.DISPLAY_NAME,
        )

        if (nameIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)
        } else {
            null
        }
    }
}

@Preview(
    name = "Polished streaming state",
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun PolishedStreamingPreview() {
    ResilientAIStreamerTheme(darkTheme = true) {
        StreamScreen(
            state = StreamUiState(
                selectedImage = ImageInput(
                    uriString = "content://preview/receipt",
                    displayName = "receipt.jpg",
                ),
                extractedText =
                    "Order 4821. Total amount ₹1,250.",
                streamedResponse =
                    "I analyzed the text extracted from the image. " +
                            "The receipt contains an order total of ₹1,250.",
                isStreaming = true,
                receivedChunkCount = 14,
                lastChunkLatencyMs = 132,
            ),
            onSelectImage = {},
            onAnalyze = {},
            onResume = {},
        )
    }
}
package com.paulmathew.resilientaistreamer.data.logging

import android.util.Log
import com.paulmathew.resilientaistreamer.domain.model.AiStreamEvent
import com.paulmathew.resilientaistreamer.domain.repository.EventLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogcatEventLogger @Inject constructor() : EventLogger {

    override fun log(event: AiStreamEvent) {
        val message = buildString {
            append(event.type.name)

            if (event.attributes.isNotEmpty()) {
                append(" | ")
                append(
                    event.attributes.entries.joinToString {
                        "${it.key}=${it.value}"
                    },
                )
            }
        }

        when (event.type) {
            AiStreamEvent.Type.STREAM_INTERRUPTED,
            AiStreamEvent.Type.STREAM_LATENCY_SPIKE,
            -> Log.w(TAG, message)

            else -> Log.d(TAG, message)
        }
    }

    private companion object {
        const val TAG = "ResilientAIStreamer"
    }
}
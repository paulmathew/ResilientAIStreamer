package com.paulmathew.resilientaistreamer.domain.repository

import com.paulmathew.resilientaistreamer.domain.model.AiStreamEvent

interface EventLogger {
    fun log(event: AiStreamEvent)
}
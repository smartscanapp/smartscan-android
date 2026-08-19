package com.fpf.smartscan.core.search

enum class SignalType {
    VLM,
    VLM_CLUSTER,
    SENTENCE_TRANSFORMER
}

data class Signal(val scores: Map<Long, Float>, val type: SignalType)

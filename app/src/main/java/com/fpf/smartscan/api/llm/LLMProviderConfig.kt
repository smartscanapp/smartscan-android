package com.fpf.smartscan.api.llm

data class LLMProviderConfig (
    val model: String,
    val temperature: Float = 0.1f,
    val systemPrompt: String? = null,
    val maxTokens: Int = 4000,
    val stream: Boolean = false
)


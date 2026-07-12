package com.fpf.smartscan.concepts

object HighlightsCodec {
    private const val DELIMITER = "| "
    fun encode(highlights: List<String>): String = highlights.joinToString(DELIMITER)
    fun decode(description: String): List<String> = description
        .split(DELIMITER)
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
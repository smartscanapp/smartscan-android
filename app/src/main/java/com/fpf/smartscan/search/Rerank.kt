package com.fpf.smartscan.search

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

data class RerankSignal(
    val scores: Map<Long, Float>,
    val weight: Float = 1f
)

object Reranker {

    private const val EPS = 1e-3

    fun rerank(
        itemToSimMap: Map<Long, Float>,
        signals: List<RerankSignal>,
        strictness: Float = 0f,
        baseCutOffPercent: Float = 0.6f,
        maxCutOffPercent: Float = 0.85f
    ): List<Long> {

        val scoredItems = calculateRerankScores(itemToSimMap, signals)
        // Early return if only a single or no result
        if (scoredItems.size <= 1) return scoredItems.map { it.key }

        val minAllowedScore = calculateRelevanceCutoff(scoredItems, strictness, baseCutOffPercent, maxCutOffPercent)
        return scoredItems.filter { it.value >= minAllowedScore }.map { it.key }
    }

    fun calculateRerankScores(itemToSimMap: Map<Long, Float>, signals: List<RerankSignal>): Map<Long, Double> {
        val itemSpread = calculateSpread( itemToSimMap.values.map { it.toDouble() })
        val signalStrengths = signals.map { signal -> calculateSignalStrength(signal.scores, itemSpread) }

        val scoredItems = itemToSimMap.keys.map { itemId ->
            val itemScore = itemToSimMap[itemId]?.toDouble() ?: 0.0
            var score = itemScore
            signals.forEachIndexed { index, signal ->
                val signalScore = signal.scores[itemId]?.toDouble()?: 0.0
                score *= 1 + signalStrengths[index] * signalScore * signal.weight
            }
            itemId to score
        }.sortedByDescending { it.second }.associate { it.first to it.second }
        return scoredItems
    }

    fun calculateRelevanceCutoff(
        scoredItems: Map<Long, Double>,
         strictness: Float = 0f,
         baseCutOffPercent: Float = 0.6f,
         maxCutOffPercent: Float = 0.85f
    ): Double
    {
        val scores = scoredItems.map { it.value }.sorted()
        val maxScore = scores.max()
        val minScore = scores.min()
        val medianScore = scores[scores.size / 2]
        val meanScore = scores.average()
        val stdScore = sqrt(scores.map{(it - meanScore).pow(2)}.average())
        val centrality = medianScore - minScore
        val safeStrictness = strictness.coerceIn(0f, 1f)
        val strictnessDamping = (maxScore - medianScore) / (maxScore).coerceAtLeast(EPS)
        val cutOffPercent = (baseCutOffPercent * (1f + strictnessDamping.toFloat() * safeStrictness)).coerceIn(baseCutOffPercent, maxCutOffPercent)
        val baseCutOff = cutOffPercent * maxScore
        val dynamicCutOff = medianScore - stdScore - (1 - safeStrictness) * centrality.pow(1.0 + safeStrictness)
        val minAllowedScore = max(baseCutOff, dynamicCutOff)
        return minAllowedScore
    }

    fun calculateRelevanceCutoff(
        scoredItems: Map<Long, Float>,
        strictness: Float = 0f,
        baseCutOffPercent: Float = 0.6f,
        maxCutOffPercent: Float = 0.85f
    ): Double
    {
        val scoredItems = buildMap{scoredItems.forEach{put(it.key, it.value.toDouble())}}
        return calculateRelevanceCutoff(scoredItems, strictness, baseCutOffPercent, maxCutOffPercent)
    }

    fun calculateSpread(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0

        val sorted = values.sorted()
        val p10 = sorted[(sorted.size * 0.1).toInt()]
        val p90 = sorted[(sorted.size * 0.9).toInt()]
        return p90 - p10
    }

    fun calculateSignalStrength(signalScores: Map<Long, Float>, itemSpread: Double): Double {
        val signalSpread = calculateSpread(signalScores.values.map { it.toDouble() })
        return (signalSpread / itemSpread.coerceAtLeast(EPS)).pow(3).coerceAtLeast(1.0)
    }

}


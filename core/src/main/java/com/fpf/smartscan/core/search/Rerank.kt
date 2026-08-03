package com.fpf.smartscan.core.search

import android.util.Log
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class RerankSignal(val scores: Map<Long, Float>, val key: Int)

object Reranker {
    private const val TAG = "Reranker"
    private const val EPS = 1e-6
    fun rerank(signals: List<RerankSignal> = emptyList()): List<Long> {
        val scoredItems = calculateRerankScores(signals)
        if (scoredItems.size <= 1) return scoredItems.keys.toList()
        val minAllowedScore = calculateRelevanceCutoff(scoredItems.values.toList())
        return scoredItems.filter { it.value >= minAllowedScore }.keys.toList()
    }

    fun calculateRerankScores(signals: List<RerankSignal>): Map<Long, Double> {
        if (signals.isEmpty()) return emptyMap()
        val allItemsIds: MutableSet<Long> = mutableSetOf()
        signals.forEach { allItemsIds.addAll(it.scores.keys)}

        val totalResults = allItemsIds.size
        val normalizedSignals = signals.associate { signal ->
            signal.key to normalizeScores(signal.scores)
        }
        val signalStrengths = signals.associate { signal ->
            signal.key to calculateSignalStrength(signal.scores, totalResults)
        }

//        Log.d(TAG, "Signal strengths: $signalStrengths")
//        Log.d(TAG, "Signal signalWeights: $signalWeights")

        return allItemsIds.map { itemId ->
            var score = 0.0
            signals.asSequence()
                .forEach { signal ->
                    val signalScore = normalizedSignals[signal.key]?.get(itemId) ?: return@forEach
                    val strength = signalStrengths[signal.key] ?: return@forEach
                    score += strength * signalScore
                }
            itemId to score
        }
            .sortedByDescending { it.second }
            .toMap()
    }

    fun calculateSignalStrength(signalScores: Map<Long, Float>, totalResults: Int): Double {
        if (signalScores.isEmpty() || totalResults <= 0) return 0.0

        val values = signalScores.values.map { it.toDouble() }.sortedDescending()
        val topCount = maxOf(1, percentile(values, 0.2).toInt())
        val topMean = values.take(topCount).average()
        val bottomMean = values.drop(topCount).ifEmpty { listOf(0.0) }.average()
        val separation = (topMean - bottomMean).coerceAtLeast(0.0)
        val sparsityBonus = 1.0 / sqrt(values.size.toDouble())
        return (0.7 * separation + 0.3 * topMean) * (1.0 + sparsityBonus)
    }

    fun calculateRelevanceCutoff(scores: List<Double>, segmentPenaltyMultiplier: Double = 0.05): Double {
        if (scores.isEmpty()) return 0.0
        val n = scores.size
        val minSegmentLength = maxOf(10, sqrt(n.toDouble()).toInt())
        if (scores.size < minSegmentLength) return scores.last()

        // Prefix sums for O(1) linear regression error calculation.
        val prefixY = DoubleArray(n + 1)
        val prefixYY = DoubleArray(n + 1)
        val prefixX = DoubleArray(n + 1)
        val prefixXX = DoubleArray(n + 1)
        val prefixXY = DoubleArray(n + 1)

        for (i in scores.indices) {
            val x = i.toDouble()
            val y = scores[i]

            prefixY[i + 1] = prefixY[i] + y
            prefixYY[i + 1] = prefixYY[i] + y * y
            prefixX[i + 1] = prefixX[i] + x
            prefixXX[i + 1] = prefixXX[i] + x * x
            prefixXY[i + 1] = prefixXY[i] + x * y
        }

        fun segmentError(start: Int, end: Int): Double {
            val count = end - start + 1

            if (count <= 1) return 0.0

            val sumX = prefixX[end + 1] - prefixX[start]
            val sumY = prefixY[end + 1] - prefixY[start]
            val sumXX = prefixXX[end + 1] - prefixXX[start]
            val sumXY = prefixXY[end + 1] - prefixXY[start]
            val sumYY = prefixYY[end + 1] - prefixYY[start]

            val denominator = count * sumXX - sumX * sumX

            val slope = if (abs(denominator) < 1e-12) {
                    0.0
                } else {
                    (count * sumXY - sumX * sumY) / denominator
                }

            val intercept = (sumY - slope * sumX) / count

            val error = sumYY +
                        slope * slope * sumXX +
                        count * intercept * intercept +
                        2.0 * slope * intercept * sumX -
                        2.0 * slope * sumXY -
                        2.0 * intercept * sumY

            return maxOf(0.0, error)
        }

        // Penalty scales with the total fitting error.
        //This prevents both under-segmentation and excessive micro-segmentation.
        val totalError = segmentError(0, n - 1)
        val penalty = totalError * segmentPenaltyMultiplier

        //cost[end] = minimum fitting error up to end + segment penalties.
        val cost = DoubleArray(n) { Double.POSITIVE_INFINITY }
        val previousBoundary = IntArray(n) { -1 }

        for (end in scores.indices) {
            for (start in 0..end) {
                val segmentLength = end - start + 1

                if (segmentLength < minSegmentLength) continue

                val fittingError = segmentError(start, end)
                val previousCost = if (start == 0) 0.0 else cost[start - 1]
                val segmentPenalty = if (start == 0) 0.0 else penalty
                val candidateCost = previousCost + fittingError + segmentPenalty

                if (candidateCost < cost[end]) {
                    cost[end] = candidateCost
                    previousBoundary[end] = start - 1
                }
            }
        }

        val segments = mutableListOf<Pair<Int, Int>>()
        var end = n - 1

        while (end >= 0) {
            val previous = previousBoundary[end]
            val start = previous + 1
            segments.add(start to end)
            end = previous
        }

        segments.reverse()

//        segments.forEachIndexed { index, segment ->
//            Log.d(
//                TAG,
//                "Segment ${index + 1}: " +
//                        "start=${segment.first}, " +
//                        "finish=${segment.second}, " +
//                        "startScore=${scores[segment.first]}, " +
//                        "finishScore=${scores[segment.second]}"
//            )
//        }

        val cutoffIndex = when {
            segments.size == 1 -> n - 1
            scores[segments[0].second] <= scores[segments[0].first] * 0.5 -> segments[1].first
            else -> segments.last().first
        }
//        Log.d(TAG, "Relevance cutoff: index=$cutoffIndex, score=${scores[cutoffIndex]}")
        return scores[cutoffIndex]
    }

    @JvmName("calculateRelevanceCutoffFloatMap")
    fun calculateRelevanceCutoff(scoredItems: Map<Long, Float>): Double {
        return calculateRelevanceCutoff(scoredItems.values.map{it.toDouble()})
    }

    private fun normalizeScores(scores: Map<Long, Float>): Map<Long, Double> { if (scores.isEmpty()) return emptyMap()
        val maxScore = scores.values.maxOrNull()?.toDouble()?.coerceAtLeast(EPS) ?: return emptyMap()
        return scores.mapValues { (_, value) -> (value.toDouble() / maxScore).coerceIn(0.0, 1.0) }
    }

    private fun percentile(values: List<Double>, percent: Double): Double {
        if (values.isEmpty()) return 0.0
        return values[(values.lastIndex * percent.coerceIn(0.0, 1.0)).toInt()]
    }
}
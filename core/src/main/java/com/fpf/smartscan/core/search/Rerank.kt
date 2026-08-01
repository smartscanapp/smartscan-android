package com.fpf.smartscan.core.search

import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt

data class RerankSignal(val scores: Map<Long, Float>, val key: Int)

object Reranker {
    private const val TAG = "Reranker"
    private const val EPS = 1e-6
    private const val SIGNAL_ALPHA = 5.0

    fun rerank(signals: List<RerankSignal> = emptyList()): List<Long> {
        val scoredItems = calculateRerankScores(signals)
        if (scoredItems.size <= 1) return scoredItems.keys.toList()
        val windowSize = (0.05 * scoredItems.size).toInt().coerceAtLeast(1)
        val minAllowedScore = calculateRelevanceCutoff(scoredItems, windowSize = windowSize)
        Log.d(TAG, "cutoff=$minAllowedScore")
        return scoredItems.filter { it.value >= minAllowedScore }.keys.toList()
    }


    fun calculateRerankScores(signals: List<RerankSignal>): Map<Long, Double> {
        if (signals.isEmpty()) return emptyMap()

        val mainSignal = signals.maxByOrNull { it.scores.size } ?: return emptyMap()
        val totalResults = mainSignal.scores.size
        val normalizedSignals = signals.associate { signal ->
            signal.key to normalizeScores(signal.scores)
        }

        val signalStrengths = signals.associate { signal ->
            signal.key to calculateSignalStrength(signal.scores, totalResults)
        }
        Log.d(TAG, "Signal strengths: $signalStrengths")

        return mainSignal.scores.keys.map { itemId ->
            val mainScore = normalizedSignals[mainSignal.key]?.get(itemId)?: 1.0
            var score = mainScore * (signalStrengths[mainSignal.key]?.pow(2)?: 0.0)

            signals.asSequence()
                .filter { it.key != mainSignal.key }
                .forEach { signal ->
                    val signalScore = normalizedSignals[signal.key]?.get(itemId) ?: return@forEach
                    val strength = signalStrengths[signal.key] ?: return@forEach
                    score  *= 1.0 + SIGNAL_ALPHA * strength * signalScore.pow(2)
                }

            itemId to score
        }
            .sortedByDescending { it.second }
            .toMap()
    }


    private fun normalizeScores(scores: Map<Long, Float>): Map<Long, Double> { if (scores.isEmpty()) return emptyMap()
        val maxScore = scores.values.maxOrNull()?.toDouble()?.coerceAtLeast(EPS) ?: return emptyMap()
        return scores.mapValues { (_, value) -> (value.toDouble() / maxScore).coerceIn(0.0, 1.0) }
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

    fun calculateRelevanceCutoff(
        scoredItems: Map<Long, Double>,
        windowSize: Int = 10,
        relativeSlopeThreshold: Double = 0.05,
        increaseFactor: Double = 1.25,
        sustainedWindows: Int = 5
    ): Double {

        if(scoredItems.isEmpty()) return 0.0

        Log.d(
            TAG,
            "Window size=$windowSize, Relative slope threshold=$relativeSlopeThreshold, Increase factor=$increaseFactor"
        )

        val scores = scoredItems.values.toList()

        if (scores.size <= windowSize + 1) {
            Log.d(TAG, "Too few scores. Using fallback.")
            return fallback(scores)
        }

        Log.d(TAG, "Scores:\n${scores.joinToString("\n")}")

        val drops = scores.zipWithNext { a, b -> a - b }

        val rollingSlope = drops
            .windowed(windowSize, partialWindows =false)
            .map { it.average() }

        Log.d(TAG, "Rolling slopes:\n${rollingSlope.joinToString("\n")}")

        if (rollingSlope.isEmpty()) {
            Log.d(TAG, "No rolling slopes. Using fallback.")
            return fallback(scores)
        }

        val initialSlope = rollingSlope.first()

        Log.d(TAG, "Initial slope=$initialSlope")

        // Phase 1 -> Phase 2 detection.
        var plateauStart = -1
        var plateauLength = 0

        for (i in rollingSlope.indices) {
            val relativeSlope = rollingSlope[i] / initialSlope

            Log.d(
                TAG,
                "Window=$i, Slope=${rollingSlope[i]}, RelativeSlope=$relativeSlope"
            )

            if (relativeSlope <= relativeSlopeThreshold) {
                if (plateauStart == -1) {
                    plateauStart = i
                    Log.d(TAG, "Plateau started at window=$i")
                }

                plateauLength++
            } else if (plateauStart != -1) {
                break
            }
        }

        // Single-phase distribution.
        if (plateauStart == -1) {
            Log.d(TAG, "No plateau detected. Using fallback.")
            return fallback(scores)
        }

        val plateauSlope = rollingSlope
            .subList(plateauStart, plateauStart + plateauLength)
            .average()

        Log.d(
            TAG,
            "Plateau detected. Start=$plateauStart, Length=$plateauLength, AverageSlope=$plateauSlope"
        )

        // Phase 2 -> Phase 3 detection.
        var increaseCount = 0

        for (i in plateauStart + plateauLength until rollingSlope.size) {

            val slope = rollingSlope[i]
            val increaseRatio = slope / plateauSlope

            Log.d(
                TAG,
                "Window=$i, Slope=$slope, IncreaseRatio=$increaseRatio"
            )

            if (increaseRatio >= increaseFactor) {
                increaseCount++

                Log.d(
                    TAG,
                    "Slope increase detected ($increaseCount/$sustainedWindows)"
                )

                if (increaseCount >= sustainedWindows) {
                    Log.d(
                        TAG,
                        "Tail detected. Cutoff at index=$i, score=${scores[i]}"
                    )
                    return scores[i]
                }
            } else {
                increaseCount = 0
            }
        }

        Log.d(
            TAG,
            "No tail detected. Returning plateau cutoff at index=$plateauStart, score=${scores[plateauStart]}"
        )

        // Two-phase distribution.
        return scores[plateauStart]
    }

    private fun fallback(scores: List<Double>): Double = percentile(scores, 0.9)

    @JvmName("calculateRelevanceCutoffFloatMap")
    fun calculateRelevanceCutoff(
        scoredItems: Map<Long, Float>,
    ): Double
    {
        val scoredItems = buildMap{scoredItems.forEach{put(it.key, it.value.toDouble())}}
        return calculateRelevanceCutoff(scoredItems)
    }



    private fun percentile(values: List<Double>, percent: Double): Double {
        if (values.isEmpty()) return 0.0
        return values[(values.lastIndex * percent.coerceIn(0.0, 1.0)).toInt()]
    }

    @JvmName("percentileFloat")
    private fun percentile(values: List<Float>, percent: Double): Double {
        if (values.isEmpty()) return 0.0
        return values[(values.lastIndex * percent.coerceIn(0.0, 1.0)).toInt()].toDouble()
    }
}
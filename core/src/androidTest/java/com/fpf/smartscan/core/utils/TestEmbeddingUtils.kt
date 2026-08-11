package com.fpf.smartscan.core.utils

import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.toQInt8
import kotlin.random.Random

fun randomEmbedding(quantize: Boolean, dim: Int): Embedding {
    val floatArray = FloatArray(dim) { Random.nextFloat() * 2f - 1f }
    return if (quantize) Embedding.QInt8(floatArray.toQInt8()) else Embedding.F32(floatArray)
}

fun embedding(id: Long, date: Long, values: Embedding) = StoredEmbedding(id, date, values)

fun genEmbeds(n: Int, quantize: Boolean, dim: Int): List<StoredEmbedding> = List(n) { i ->
    embedding((i + 1).toLong(), ((i + 1) * 100).toLong(), randomEmbedding(quantize, dim))
}

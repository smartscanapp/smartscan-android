package com.fpf.smartscan.search

import com.fpf.smartscansdk.core.embeddings.QueryResult

fun parseQuery(query: String): Pair<String?, String>{
    val regex = Regex("""^#([a-zA-Z0-9_]+)""")
    val match = regex.find(query)
    val tag = match?.groupValues?.get(1)
    val actualQueryStart = if(!tag.isNullOrBlank()) tag.length + 1 else 0
    val actualQuery = query.substring(actualQueryStart).trim()
    return Pair(tag, actualQuery)
}
fun QueryResult.toSimsMap(): Map<Long, Float> = this.sims?.let(this.ids::zip)?.toMap() ?: emptyMap()

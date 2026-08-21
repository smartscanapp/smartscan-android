package com.fpf.smartscan.core.search

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.json.Json

fun getRecentSearches(sharedPrefs: SharedPreferences, key: String): List<String>{
    val searchesStr = sharedPrefs.getString(key, null).orEmpty()
    return if(searchesStr.isNotBlank()) Json.decodeFromString<List<String>>(searchesStr) else emptyList()
}

fun saveRecentSearches(searches: List<String>, sharedPrefs: SharedPreferences, key: String){
    val searchesStr = Json.encodeToString(searches)
    sharedPrefs.edit{ putString(key, searchesStr) }
}
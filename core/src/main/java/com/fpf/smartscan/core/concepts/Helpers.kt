package com.fpf.smartscan.core.concepts

import android.content.SharedPreferences
import androidx.core.content.edit

fun getAllowedTags(sharedPrefs: SharedPreferences, key: String): Set<Long>{
    val tagIds = sharedPrefs.getStringSet(key, emptySet())
        .orEmpty()
        .map { it.toLong() }
        .toSet()
    return tagIds
}

fun getAllowedClusters(sharedPrefs: SharedPreferences, key: String): Set<Long>{
    val clusterIds = sharedPrefs.getStringSet(key, emptySet())
        .orEmpty()
        .map { it.toLong() }
        .toSet()
    return clusterIds
}

fun setAllowedTags(sharedPrefs: SharedPreferences, key: String, tagIds: Set<Long>){
    sharedPrefs.edit{
        putStringSet(key, tagIds.map{it.toString()}.toSet())
    }
}
fun setAllowedClusters(sharedPrefs: SharedPreferences, key: String, clusterIds: Set<Long>){
    sharedPrefs.edit{
        putStringSet(key, clusterIds.map{it.toString()}.toSet())
    }
}
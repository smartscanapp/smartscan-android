package com.fpf.smartscan.core.concepts

import android.content.SharedPreferences
import androidx.core.content.edit

private object PrefsKeys {
    const val ALLOWED_TAG_COLLECTIONS = "ALLOWED_TAG_COLLECTIONS"
    const val ALLOWED_AUTO_COLLECTIONS = "ALLOWED_AUTO_COLLECTIONS"
}

fun getAllowedTags(sharedPrefs: SharedPreferences): Set<Long>{
    val tagIds = sharedPrefs.getStringSet(PrefsKeys.ALLOWED_TAG_COLLECTIONS, emptySet())
        .orEmpty()
        .map { it.toLong() }
        .toSet()
    return tagIds
}

fun getAllowedClusters(sharedPrefs: SharedPreferences): Set<Long>{
    val clusterIds = sharedPrefs.getStringSet(PrefsKeys.ALLOWED_AUTO_COLLECTIONS, emptySet())
        .orEmpty()
        .map { it.toLong() }
        .toSet()
    return clusterIds
}

fun setAllowedTags(sharedPrefs: SharedPreferences, tagIds: Set<Long>){
    sharedPrefs.edit{
        putStringSet(PrefsKeys.ALLOWED_TAG_COLLECTIONS, tagIds.map{it.toString()}.toSet())
    }
}
fun setAllowedClusters(sharedPrefs: SharedPreferences, clusterIds: Set<Long>){
    sharedPrefs.edit{
        putStringSet(PrefsKeys.ALLOWED_AUTO_COLLECTIONS, clusterIds.map{it.toString()}.toSet())
    }
}
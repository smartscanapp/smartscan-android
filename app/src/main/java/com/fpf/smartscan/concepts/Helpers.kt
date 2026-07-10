package com.fpf.smartscan.concepts

import android.content.SharedPreferences
import com.fpf.smartscan.constants.PrefsKeys

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
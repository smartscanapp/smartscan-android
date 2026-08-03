package com.fpf.smartscan.navigation

object Routes {
    const val EXPLORE = "explore"
    const val COLLECTIONS = "collections"
    const val COLLECTION_ITEMS = "collection_items"
    const val CONCEPTS = "concepts"
    const val CONCEPT_ITEMS = "concept_items"

    const val SETTINGS = "settings"
    const val SETTINGS_DETAIL = "settings_detail/{type}"
    const val DONATE = "donate"
    fun settingsDetail(type: String) = "settings_detail/$type"
}

object SettingsRoutes {
    const val MODELS = "models"
    const val MANAGE_MODELS = "manage_models"
    const val ALLOWED_FOLDERS = "allowed_folders"

    const val API = "api"
}

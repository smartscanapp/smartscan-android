package com.fpf.smartscan.errors

sealed class AppException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ClusterException(message: String = "Clustering failed", cause: Throwable? = null) : AppException(message, cause)

    class BackupException(override val message: String = "Backup failed", cause: Throwable? = null) : AppException(message, cause)

    class RestoreException(override val message: String = "Restore failed",  cause: Throwable? = null) : AppException(message, cause)
    class MissingApiKey(override val message: String = "Missing required API key") : AppException(message)

    class SearchException(override val message: String = "An unknown search error occurred", cause: Throwable? = null) : AppException(message, cause)

}
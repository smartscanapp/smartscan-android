package com.fpf.smartscan.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import java.io.File


object MediaStoreHelper {
    private data class MediaSpec(
        val collection: Uri,
        val idColumn: String,
        val dateColumn: String,
        val relativePathColumn: String,
        val sortOrder: String
    )

    fun queryVideoIdDateMap(
        context: Context,
        allowedDirs: List<Uri> = emptyList(),
        startDate: Long? = null,
        endDate: Long? = null
    ): Map<Long, Long> = queryMediaIdDateMap(context, MediaType.VIDEO, allowedDirs, startDate, endDate)

    fun queryImageIdDateMap(
        context: Context,
        allowedDirs: List<Uri> = emptyList(),
        startDate: Long? = null,
        endDate: Long? = null
    ): Map<Long, Long> = queryMediaIdDateMap(context, MediaType.IMAGE, allowedDirs, startDate, endDate)

    fun queryVideoIds(
        context: Context,
        allowedDirs: List<Uri> = emptyList(),
        startDate: Long? = null,
        endDate: Long? = null
    ): List<Long> = queryVideoIdDateMap(context, allowedDirs, startDate, endDate).keys.toList()

    fun queryImageIds(
        context: Context,
        allowedDirs: List<Uri> = emptyList(),
        startDate: Long? = null,
        endDate: Long? = null
    ): List<Long> = queryImageIdDateMap(context, allowedDirs, startDate, endDate).keys.toList()

    fun createTrashRequest(context: Context, uris: Collection<Uri>, trash: Boolean = true): IntentSenderRequest {
        val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris.toList(), trash)
        return IntentSenderRequest.Builder(pendingIntent.intentSender).build()
    }


    fun getImageToDateMap(context: Context, ids: List<Long>): Map<Long, Long> = getMediaToDateMap(context, MediaType.IMAGE, ids)

    fun getVideoToDateMap(context: Context, ids: List<Long>): Map<Long, Long> = getMediaToDateMap(context, MediaType.VIDEO, ids)


    fun mediaIdToUri(id: Long, mediaType: MediaType): Uri = when (mediaType) {
            MediaType.IMAGE -> getImageUriFromId(id)
            MediaType.VIDEO -> getVideoUriFromId(id)
    }

    fun getImageUriFromId(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

    fun getVideoUriFromId(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

    private fun queryMediaIdDateMap(
        context: Context,
        mediaType: MediaType,
        allowedDirs: List<Uri> = emptyList(),
        startDate: Long? = null,
        endDate: Long? = null
    ): Map<Long, Long> {

        val spec = mediaSpecFor(mediaType)
        val result = mutableMapOf<Long, Long>()

        val projection = arrayOf(
            spec.idColumn,
            spec.dateColumn
        )

        val dirParts = mutableListOf<String>()
        val dateParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()
        val envRoot = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')

        if (allowedDirs.isNotEmpty()) {
            for (uri in allowedDirs) {
                try {
                    if (DocumentsContract.isTreeUri(uri) ||
                        uri.authority == "com.android.externalstorage.documents"
                    ) {
                        val docId = DocumentsContract.getTreeDocumentId(uri)
                        val afterColon = docId.substringAfter(':', "")
                        if (afterColon.isNotEmpty()) {
                            val rel = afterColon.trim('/') + "/"
                            dirParts.add("${spec.relativePathColumn} LIKE ?")
                            selectionArgs.add("$rel%")
                            continue
                        }
                    }

                    if (uri.scheme == "file") {
                        val file = File(uri.path ?: continue)
                        val absPath = file.absolutePath.trimEnd('/') + "/"
                        if (absPath.startsWith(envRoot)) {
                            val rel = absPath.removePrefix(envRoot)
                                .trimStart('/')
                                .trimEnd('/') + "/"
                            dirParts.add("${spec.relativePathColumn} LIKE ?")
                            selectionArgs.add("$rel%")
                        }
                        continue
                    }

                    val seg = uri.path?.trim('/') ?: uri.lastPathSegment ?: continue
                    if (seg.isNotEmpty()) {
                        val folderLike = if (seg.endsWith("/")) seg else "$seg/"
                        dirParts.add("${spec.relativePathColumn} LIKE ?")
                        selectionArgs.add("%$folderLike%")
                    }
                } catch (_: Exception) {
                }
            }
        }

        if (startDate != null) {
            dateParts.add("${spec.dateColumn} >= ?")
            selectionArgs.add(startDate.toString())
        }

        if (endDate != null) {
            dateParts.add("${spec.dateColumn} <= ?")
            selectionArgs.add(endDate.toString())
        }

        val selectionParts = mutableListOf<String>()
        if (dirParts.isNotEmpty()) {
            selectionParts.add("(${dirParts.joinToString(" OR ")})")
        }
        selectionParts.addAll(dateParts)

        val selection = if (selectionParts.isEmpty()) null else selectionParts.joinToString(" AND ")
        val args = if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray()

        context.applicationContext.contentResolver.query(
            spec.collection,
            projection,
            selection,
            args,
            spec.sortOrder
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(spec.idColumn)
            val dateIdx = cursor.getColumnIndexOrThrow(spec.dateColumn)

            while (cursor.moveToNext()) {
                result[cursor.getLong(idIdx)] = cursor.getLong(dateIdx)
            }
        }

        return result
    }

    private fun getMediaToDateMap(context: Context, mediaType: MediaType, ids: List<Long>): Map<Long, Long> {
        val spec = mediaSpecFor(mediaType)
        val result = mutableMapOf<Long, Long>()
        val projection = arrayOf(spec.idColumn, spec.dateColumn)
        val chunkSize = 500

        ids.chunked(chunkSize).forEach { chunk ->
            val selection = "${spec.idColumn} IN (${chunk.joinToString(",")})"

            context.applicationContext.contentResolver.query(
                spec.collection,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(spec.idColumn)
                val dateIdx = cursor.getColumnIndexOrThrow(spec.dateColumn)

                while (cursor.moveToNext()) {
                    result[cursor.getLong(idIdx)] = cursor.getLong(dateIdx)
                }
            }
        }

        return result
    }

    private fun mediaSpecFor(type: MediaType): MediaSpec = when (type) {
        MediaType.IMAGE -> MediaSpec(
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            idColumn = MediaStore.Images.Media._ID,
            dateColumn = MediaStore.Images.Media.DATE_ADDED,
            relativePathColumn = MediaStore.Images.Media.RELATIVE_PATH,
            sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )

        MediaType.VIDEO -> MediaSpec(
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            idColumn = MediaStore.Video.Media._ID,
            dateColumn = MediaStore.Video.Media.DATE_ADDED,
            relativePathColumn = MediaStore.Video.Media.RELATIVE_PATH,
            sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )
    }
}
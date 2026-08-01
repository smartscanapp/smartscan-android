package com.fpf.smartscan.core.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fpf.smartscan.core.embeds.EmbeddingStoresFilesQuant
import com.fpf.smartscan.core.data.MediaDatabase
import com.fpf.smartscan.core.errors.AppException
import java.io.File

object BackupUtils {
    const val BACKUP_FILENAME = "smartscan_backup.zip"
    private const val HASH_FILENAME = "hash.txt"

    private const val TAG = "BackupUtils"
    
    suspend fun backup(context: Context, outputUri: Uri){
        val indexZipFile = File(context.cacheDir, BACKUP_FILENAME)
        val hashFile = File(context.cacheDir, HASH_FILENAME)
        val dbPath = context.getDatabasePath(MediaDatabase.DB_NAME)

        val embedStoreFiles = getMainEmbedStoreFiles(context) + getConceptEmbedStoreFiles(context)
        val filesToZip = listOf( hashFile, dbPath) + embedStoreFiles

        try {
            if(embedStoreFiles.none{it.exists()}) throw AppException.BackupException("Missing index file(s)")
            val hashes: List<String> = filesToZip.filter { it.exists() && it != hashFile }.map{hashFile(it)}
            hashFile.writeText(hashes.joinToString("\n") )
            zipFiles(indexZipFile, filesToZip)
            copyToUri(context, outputUri, indexZipFile)
        }catch (e: Exception){
            Log.e(TAG, "Unknow backup error", e)
            throw AppException.BackupException(cause = e)
        }
        finally {
            indexZipFile.delete()
            hashFile.delete()
        }

    }

    suspend fun restore(context: Context, uri: Uri){
        val indexZipFile = File(context.cacheDir, BACKUP_FILENAME)
        MediaDatabase.close()
        try {
            copyFromUri(context, uri, indexZipFile)
            val extractedFiles = unzipFiles(indexZipFile, context.filesDir)

            if(!isValidBackupFile(extractedFiles)){
                extractedFiles.forEach { it.delete() }
                throw AppException.RestoreException("Invalid backup file")
            }
        }
        catch (e: Exception){
            Log.e(TAG, "Unknow restore error", e)
            throw AppException.RestoreException(cause = e)
        }
        finally {
            indexZipFile.delete()
        }

    }

    fun checkCachedDb(context: Context): File?{
        val cachedDB = File(context.filesDir, MediaDatabase.DB_NAME)
        return if(cachedDB.exists()) cachedDB else null
    }

    fun restoreDbFromCache(context: Context, cachedDbFile: File){
        if(!cachedDbFile.exists()) return
        val dbPath = context.getDatabasePath(MediaDatabase.DB_NAME)
        Log.d(TAG, "Database cache found, restoring...")
        cachedDbFile.copyTo(dbPath, overwrite = true)
        cachedDbFile.delete()
    }

    private suspend fun isValidBackupFile(extractedFiles: List<File>): Boolean{
        val hashFile = extractedFiles.find { it.name == HASH_FILENAME }?: return false
        val hashesFromFile: List<String> = hashFile.readLines()
        if(hashesFromFile.isEmpty()) return false

        val otherFiles = extractedFiles.filterNot{it.name == HASH_FILENAME}
        val otherFileHashes = otherFiles.map{hashFile(it)}
        return hashesFromFile.toSet() == otherFileHashes.toSet()
    }

    private fun getMainEmbedStoreFiles(context: Context): List<File>{
        val imageEmbeddingStoreFile = File(context.filesDir, EmbeddingStoresFilesQuant.IMAGE)
        val videoEmbeddingStoreFile = File(context.filesDir,  EmbeddingStoresFilesQuant.VIDEO)
        val clusterEmbeddingStoreFile = File(context.filesDir, EmbeddingStoresFilesQuant.CLUSTER)
        return listOf(imageEmbeddingStoreFile, videoEmbeddingStoreFile, clusterEmbeddingStoreFile)
    }

    private fun getConceptEmbedStoreFiles(context: Context): List<File>{
        val imageConceptEmbeddingStoreFile = File(context.filesDir, EmbeddingStoresFilesQuant.IMAGE_CONCEPT)
        val videoConceptEmbeddingStoreFile = File(context.filesDir,  EmbeddingStoresFilesQuant.VIDEO_CONCEPT)
        val conceptEmbedStoreFile = File(context.filesDir, EmbeddingStoresFilesQuant.CONCEPT)
        return listOf(imageConceptEmbeddingStoreFile, videoConceptEmbeddingStoreFile, conceptEmbedStoreFile)
    }

}
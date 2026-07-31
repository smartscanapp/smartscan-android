package com.fpf.smartscan.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.fpf.smartscansdk.core.media.getScaledDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


fun getDirectoryName(context: Context, uri: Uri): String {
    val documentDir = DocumentFile.fromTreeUri(context, uri)
    return documentDir?.name.toString()
}

fun canOpenUri(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { }
        true
    } catch (e: Exception) {
        false
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val name = if (cursor != null && nameIndex != null && nameIndex != -1 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)
        } else null
        cursor?.close()
        return name
    }catch (e: Exception){
        Log.e("getFileName", "${e.message}")
        return null
    }
}

suspend fun zipFiles(outputFile: File, files: List<File>) = withContext(Dispatchers.IO){
    val filteredFiles = files.filter { it.exists() }

    if (filteredFiles.isEmpty()) error("No valid files")

    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use{zipOutputStream ->
        for(file in filteredFiles){
            FileInputStream(file).use { fileInputStream ->
                val entry = ZipEntry(file.name)
                zipOutputStream.putNextEntry(entry)
                fileInputStream.copyTo(zipOutputStream)
                zipOutputStream.closeEntry()
            }
        }
    }
}

suspend fun unzipFiles(zipFile: File, targetDir: File): List<File> = withContext(Dispatchers.IO){
    if(!zipFile.name.endsWith(".zip")) error ("Invalid zip file")
    if(!targetDir.exists()) targetDir.mkdirs()

    val extractedFiles = mutableListOf<File>()

    ZipInputStream(FileInputStream(zipFile)).use {zipInputStream ->
        var entry = zipInputStream.nextEntry
        while(entry != null){
            val entryFile = File(targetDir, entry.name)
            extractedFiles.add(entryFile)
            FileOutputStream(entryFile).use { outputStream ->
                zipInputStream.copyTo(outputStream)
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        }
    }
    extractedFiles
}

suspend fun copyFromUri(context: Context, uri: Uri, outputFile: File) = withContext(Dispatchers.IO){
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        FileOutputStream(outputFile).use { outputStream -> inputStream.copyTo(outputStream) }
    }
}

suspend fun copyToUri(context: Context, outputUri: Uri, file: File) = withContext(Dispatchers.IO) {
    context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
        FileInputStream(file).use { inputStream -> inputStream.copyTo(outputStream) }
    }
}

suspend fun hashFile(file: File, algorithm: String = "SHA-256"): String = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance(algorithm)
    file.inputStream().use { fis ->
        val buffer = ByteArray(1024)
        var read: Int
        while (fis.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

fun uriToBase64(context: Context, uri: Uri, maxSize: Int = 1024): String{
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val bitmap =  ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val (w, h) = getScaledDimensions(info.size.width, info.size.height, maxSize)
        decoder.setTargetSize(w, h)
    }
    val outputStream = ByteArrayOutputStream()
    val mime = context.contentResolver.getType(uri)?: "image/png"

    val format = when (mime) {
        "image/jpeg" -> Bitmap.CompressFormat.JPEG
        "image/webp" -> Bitmap.CompressFormat.WEBP
        else -> Bitmap.CompressFormat.PNG
    }
    bitmap.compress(format, 100, outputStream)
    val byteArray = outputStream.toByteArray()
    return "data:$mime;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
}

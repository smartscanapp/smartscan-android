package com.fpf.smartscan.media

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

fun getImageUriFromId(id: Long): Uri {
    return ContentUris.withAppendedId(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        id
    )
}

fun openImageInGallery(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}


fun getVideoUriFromId(id: Long): Uri {
    return ContentUris.withAppendedId(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        id
    )
}

fun openVideoInGallery(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/*")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}


fun shareMedia(context: Context, uri: Uri){
    val mime = context.contentResolver.getType(uri)
    val shareIntent: Intent = Intent().apply {
        this.action = Intent.ACTION_SEND
        this.putExtra(Intent.EXTRA_STREAM, uri)
        this.type = mime
    }
    if(!mime.isNullOrBlank()){
        context.startActivity(Intent.createChooser(shareIntent, null))
    }
}

fun shareMediaMulti(context: Context, uris: List<Uri>){
    val mime = context.contentResolver.getType(uris[0])?.substringBefore("/")?.plus( "/*")
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = mime
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }
    if(!mime.isNullOrBlank()){
        context.startActivity(Intent.createChooser(shareIntent, null))
    }
}

fun mediaIdToUri(id: Long, mediaType: MediaType): Uri {
    return when (mediaType) {
        MediaType.IMAGE -> getImageUriFromId(id)
        MediaType.VIDEO -> getVideoUriFromId(id)
    }
}

fun toMediaItem(id: Long, type: MediaType): MediaItem{
    return MediaItem(
        id = id,
        type = type
    )
}
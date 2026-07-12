package com.fpf.smartscan.media

import android.content.Context
import android.content.Intent
import android.net.Uri

fun openImageInGallery(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
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
package com.example.galleryapp.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStoreRepo {

    // Query all external images, newest first. Runs on IO dispatcher.
    suspend fun queryAllPhotos(context: Context): List<PhotoRow> = withContext(Dispatchers.IO) {
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.ORIENTATION
        )
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(uri, projection, null, null, sort)?.use { c ->
            val idxId = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val idxW  = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val idxH  = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val idxO  = c.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
            buildList {
                while (c.moveToNext()) {
                    add(
                        PhotoRow(
                            id = c.getLong(idxId),
                            width = c.getInt(idxW),
                            height = c.getInt(idxH),
                            orientation = c.getInt(idxO)
                        )
                    )
                }
            }
        } ?: emptyList()
    }

    // Helper to build a content:// URI for a row id.
    fun contentUriFor(id: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
}

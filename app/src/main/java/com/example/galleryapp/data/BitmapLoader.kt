package com.example.galleryapp.data

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.provider.MediaStore
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BitmapLoader {

    // 16MB cache for thumbnails
    private val cache = object : LruCache<Long, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }

    // Load a low res thumbnail for a MediaStore row on IO dispatcher.
    suspend fun loadThumb(cr: ContentResolver, row: PhotoRow, targetPx: Int = 300): Bitmap? =
        withContext(Dispatchers.IO) {
            cache.get(row.id) ?: runCatching {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, row.id
                )

                //bounds pass
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                cr.openInputStream(uri)?.use { ins ->
                    BitmapFactory.decodeStream(ins, null, bounds)
                }

                //compute sampling
                val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, targetPx)

                //decode with sampled
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val decoded = cr.openInputStream(uri)?.use { ins ->
                    BitmapFactory.decodeStream(ins, null, opts)
                }

                //rotate if needed
                val rotated = rotateIfNeeded(decoded, row.orientation)
                rotated?.also { cache.put(row.id, it) }
            }.getOrNull()
        }

    // High res decode for the viewer
    suspend fun loadFull(
        cr: ContentResolver,
        id: Long,
        orientation: Int,
        maxDim: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

            // bounds pass
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            // compute sample to keep longest side
            val longest = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var inSample = 1
            while ((longest / inSample) > maxDim) inSample *= 2

            // decode with sampling
            val opts = BitmapFactory.Options().apply { inSampleSize = inSample }
            val decoded = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }

            // rotate if needed
            rotateIfNeeded(decoded, orientation)
        }.getOrNull()
    }

    private fun computeInSampleSize(w: Int, h: Int, target: Int): Int {
        var s = 1
        var halfW = w / 2
        var halfH = h / 2
        while ((halfW / s) >= target && (halfH / s) >= target) s *= 2
        return s
    }

    private fun rotateIfNeeded(src: Bitmap?, degrees: Int): Bitmap? {
        if (src == null || (degrees % 360) == 0) return src
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
            .also { if (it !== src) src.recycle() }
    }
}

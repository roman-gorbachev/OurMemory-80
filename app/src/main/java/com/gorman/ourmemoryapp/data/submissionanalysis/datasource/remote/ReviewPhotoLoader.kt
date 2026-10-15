package com.gorman.ourmemoryapp.data.submissionanalysis.datasource.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.URL

class ReviewPhotoLoader {

    fun load(urls: List<String>): List<Bitmap> = urls.mapNotNull { url ->
        runCatching { decodeScaled(URL(url).openStream().use { it.readBytes() }) }.getOrNull()
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = ORIGINAL_SAMPLE_SIZE
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * SAMPLE_STEP) >= MAX_PHOTO_SIDE) {
            sampleSize *= SAMPLE_STEP
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    companion object {
        private const val MAX_PHOTO_SIDE = 1600
        private const val ORIGINAL_SAMPLE_SIZE = 1
        private const val SAMPLE_STEP = 2
    }
}

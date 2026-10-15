package com.gorman.ourmemoryapp.ui.scan.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

fun ImageProxy.toScaledJpeg(): ByteArray {
    val source = toBitmap()
    val scale = MAX_PHOTO_SIDE.toFloat() / maxOf(source.width, source.height)
    val matrix = Matrix().apply {
        postRotate(imageInfo.rotationDegrees.toFloat())
        if (scale < ORIGINAL_SCALE) postScale(scale, scale)
    }
    val photo = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    return ByteArrayOutputStream().use { stream ->
        photo.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        stream.toByteArray()
    }
}

private const val MAX_PHOTO_SIDE = 1600
private const val ORIGINAL_SCALE = 1f
private const val JPEG_QUALITY = 85

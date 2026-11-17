package com.triplog.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ReceiptImage {
    const val MAX_EDGE_PX = 1600
    const val JPEG_QUALITY = 80

    /**
     * Shrinks a receipt photo in place so its longest side is at most [MAX_EDGE_PX] pixels,
     * re-encodes it as JPEG and bakes in the EXIF rotation. Receipts stay readable at this
     * size and upload in a fraction of the time. If anything goes wrong the original file is
     * left untouched.
     */
    fun downscale(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return false

            val rotation = when (
                ExifInterface(file.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            val scale = MAX_EDGE_PX.toFloat() / max(decoded.width, decoded.height)
            val matrix = Matrix().apply {
                if (scale < 1f) postScale(scale, scale)
                if (rotation != 0f) postRotate(rotation)
            }
            val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

            val temp = File(file.parentFile, file.name + ".tmp")
            FileOutputStream(temp).use { out ->
                result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (result !== decoded) decoded.recycle()
            result.recycle()

            temp.copyTo(file, overwrite = true)
            temp.delete()
            true
        } catch (e: Exception) {
            false
        } catch (e: OutOfMemoryError) {
            false
        }
    }

    /** Largest power of two that keeps the decoded image at least [MAX_EDGE_PX] on its long side. */
    internal fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        val longest = max(width, height)
        while (longest / (sample * 2) >= MAX_EDGE_PX) sample *= 2
        return sample
    }
}

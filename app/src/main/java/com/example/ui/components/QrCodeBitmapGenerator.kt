package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

object QrCodeBitmapGenerator {

    /**
     * Generates a square QR Code [Bitmap] for a given content string.
     */
    fun createQrBitmap(
        content: String,
        size: Int = 512,
        darkColor: Color = Color.Black,
        lightColor: Color = Color.White
    ): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val pixels = IntArray(size * size)
            val darkArgb = darkColor.toArgb()
            val lightArgb = lightColor.toArgb()

            for (y in 0 until size) {
                val offset = y * size
                for (x in 0 until size) {
                    pixels[offset + x] = if (bitMatrix[x, y]) darkArgb else lightArgb
                }
            }

            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, size, 0, 0, size, size)
            }
        } catch (_: Exception) {
            null
        }
    }
}

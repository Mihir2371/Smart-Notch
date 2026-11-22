package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SampleAssetProvider {

    suspend fun getSampleImageUri(context: Context): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "drag_share").apply { mkdirs() }
            val outFile = File(dir, "aurora_sample.jpg")
            if (!outFile.exists()) {
                val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_sample_photo_1795341179984)
                outFile.outputStream().use { stream ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                }
                bmp.recycle()
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        }.getOrNull()
    }

    suspend fun getSamplePdfUri(context: Context): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "drag_share").apply { mkdirs() }
            val outFile = File(dir, "Aura_Island_Guide.pdf")
            if (!outFile.exists()) {
                // Minimal valid single-page PDF header/body so external PDF viewers & share targets accept it cleanly
                val minimalPdf = """
                    %PDF-1.4
                    1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
                    2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
                    3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 300 144] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj
                    4 0 obj << /Length 55 >> stream
                    BT /F1 16 Tf 24 80 Td (Aura Island Drag & Go PDF) Tj ET
                    endstream endobj
                    5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj
                    xref
                    0 6
                    0000000000 65535 f 
                    0000000009 00000 n 
                    0000000058 00000 n 
                    0000000115 00000 n 
                    0000000241 00000 n 
                    0000000346 00000 n 
                    trailer << /Size 6 /Root 1 0 R >>
                    startxref
                    414
                    %%EOF
                """.trimIndent()
                outFile.writeText(minimalPdf)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        }.getOrNull()
    }
}

package com.gatemaster.app.ui.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

/** Aspect ratio of one page, used to size placeholders before it renders. */
data class PdfPageSize(val width: Int, val height: Int) {
    val aspectRatio: Float get() = if (height == 0) 1f else width.toFloat() / height
}

/**
 * A PDF opened from the bundled assets.
 *
 * Uses the framework [PdfRenderer] rather than androidx.pdf, which is still at
 * alpha and changes shape between releases. Previous-year papers are a headline
 * feature, so they run on a stable API; everything PDF-specific lives in this
 * file and [PdfReader] so swapping later is contained.
 *
 * [PdfRenderer] allows only one open page at a time, hence the mutex.
 */
class PdfDocument private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    val pageSizes: List<PdfPageSize>,
) : Closeable {

    private val mutex = Mutex()
    private var closed = false

    val pageCount: Int get() = pageSizes.size

    suspend fun renderPage(index: Int, targetWidthPx: Int): ImageBitmap? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (closed || index !in 0 until pageCount) return@withLock null
                val size = pageSizes[index]
                val width = targetWidthPx.coerceIn(MIN_RENDER_WIDTH, MAX_RENDER_WIDTH)
                val height = (width / size.aspectRatio).toInt().coerceAtLeast(1)

                runCatching {
                    renderer.openPage(index).use { page ->
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        // PdfRenderer composites onto transparency; papers are
                        // scanned text, so give them an opaque white sheet.
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap.asImageBitmap()
                    }
                }.getOrNull()
            }
        }

    override fun close() {
        closed = true
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }

    companion object {
        private const val MIN_RENDER_WIDTH = 320
        private const val MAX_RENDER_WIDTH = 2048

        /**
         * Assets live inside the APK and have no file descriptor, so the
         * document is copied into the cache directory once and reused.
         */
        suspend fun openFromAsset(context: Context, assetPath: String): Result<PdfDocument> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val cached = cacheFileFor(context, assetPath)
                    if (!cached.exists() || cached.length() == 0L) {
                        cached.parentFile?.mkdirs()
                        val tmp = File(cached.absolutePath + ".part")
                        context.assets.open(assetPath).use { input ->
                            tmp.outputStream().use { output -> input.copyTo(output) }
                        }
                        if (!tmp.renameTo(cached)) {
                            tmp.delete()
                            error("Could not stage $assetPath for reading")
                        }
                    }

                    val fd = ParcelFileDescriptor.open(
                        cached,
                        ParcelFileDescriptor.MODE_READ_ONLY,
                    )
                    val renderer = PdfRenderer(fd)
                    val sizes = (0 until renderer.pageCount).map { i ->
                        renderer.openPage(i).use { PdfPageSize(it.width, it.height) }
                    }
                    PdfDocument(fd, renderer, sizes)
                }
            }

        private fun cacheFileFor(context: Context, assetPath: String): File {
            val safeName = assetPath.replace(Regex("[^A-Za-z0-9._-]"), "_")
            return File(File(context.cacheDir, "pdf"), safeName)
        }
    }
}

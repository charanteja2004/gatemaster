package com.gatemaster.app.ui.reader

import android.content.res.AssetManager
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.Collections

/**
 * Serves the reader stylesheet with the palette the *app* has chosen.
 *
 * The obvious approach — a `@media (prefers-color-scheme: dark)` block — hands
 * the decision to the system, so a reader who picks Light still gets dark notes
 * whenever their phone is in dark mode. Since every request already passes
 * through [WebViewAssetLoader], the stylesheet is assembled here instead:
 * `reader.css` carries the light palette, and `reader-dark.css` is appended only
 * when the app is dark.
 *
 * Doing it here rather than with injected JavaScript keeps the WebView's script
 * engine switched off, and avoids the flash that comes from restyling a page
 * after it has already painted.
 */
class ReaderCssHandler(
    private val assets: AssetManager,
    private val isDark: () -> Boolean,
) : WebViewAssetLoader.PathHandler {

    override fun handle(path: String): WebResourceResponse? {
        if (path != STYLESHEET) return null

        return runCatching {
            val base: InputStream = assets.open(STYLESHEET)
            val stream: InputStream = if (isDark()) {
                val dark = assets.open(DARK_STYLESHEET).readBytes()
                SequenceInputStream(
                    Collections.enumeration(
                        listOf(base, ByteArrayInputStream(SEPARATOR + dark)),
                    ),
                )
            } else {
                base
            }

            WebResourceResponse("text/css", "utf-8", stream)
        }.getOrNull()
    }

    private companion object {
        const val STYLESHEET = "reader.css"
        const val DARK_STYLESHEET = "reader-dark.css"
        val SEPARATOR = "\n\n/* --- app theme: dark --- */\n".toByteArray()
    }
}

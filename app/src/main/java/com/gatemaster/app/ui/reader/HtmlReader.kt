package com.gatemaster.app.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.gatemaster.app.R
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature

/**
 * Renders a bundled HTML article.
 *
 * Served through [WebViewAssetLoader] over https://appassets.androidplatform.net
 * rather than a `file://` URL. That lets us keep file access switched off
 * entirely while relative links between articles — of which the content has
 * many — still resolve.
 *
 * JavaScript stays disabled: every bundled document is static prose, so there
 * is nothing for it to do and no reason to widen the attack surface. Reading
 * progress therefore comes from the WebView's own scroll position rather than
 * from a script injected into the page.
 */
@Composable
fun HtmlReader(
    assetPath: String,
    modifier: Modifier = Modifier,
    textZoom: Int = 100,
    isDarkTheme: Boolean = false,
    onProgress: (Float) -> Unit = {},
) {
    val context = LocalContext.current

    // Read through a holder so the handler always sees the current theme
    // without rebuilding the loader (and reloading the page) on every change.
    val darkState = rememberUpdatedState(isDarkTheme)

    val assetLoader = remember {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", ReaderCssHandler(context.assets) { darkState.value })
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                ReaderWebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )

                    settings.apply {
                        javaScriptEnabled = false
                        allowFileAccess = false
                        allowContentAccess = false
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    // Algorithmic darkening stays OFF: the stylesheet already
                    // carries a hand-tuned dark palette, and letting WebView
                    // auto-invert on top of it washes out diagrams and code.
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)
                    }

                    isVerticalScrollBarEnabled = false

                    setOnScrollChangeListener { _, _, scrollY, _, _ ->
                        onProgress(readingFraction(scrollY))
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest,
                        ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val url = request.url
                            // Cross-links between bundled articles stay in the
                            // reader; anything else opens in the browser.
                            if (url.host == ASSET_HOST) return false
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, url))
                            }
                            return true
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            // A short article fits on one screen and can never
                            // be scrolled, so waiting for a scroll event would
                            // leave it permanently unread. Once layout has
                            // settled, decide from the actual content height.
                            view.postDelayed(
                                { onProgress((view as ReaderWebView).readingFraction(view.scrollY)) },
                                LAYOUT_SETTLE_MS,
                            )
                        }
                    }
                }
            },
            update = { webView ->
                if (webView.settings.textZoom != textZoom) {
                    webView.settings.textZoom = textZoom
                }
                val target = assetUrl(assetPath)
                if (webView.url != target) {
                    webView.loadUrl(target)
                } else if (webView.getTag(R.id.reader_theme_tag) != isDarkTheme) {
                    // The stylesheet is chosen at request time, so a theme
                    // change needs a reload to pick up the other palette.
                    webView.reload()
                }
                webView.setTag(R.id.reader_theme_tag, isDarkTheme)
            },
            onRelease = { webView ->
                webView.setOnScrollChangeListener(null)
                webView.stopLoading()
                webView.destroy()
            },
        )
    }
}

/**
 * WebView's scroll metrics are protected, so a subclass exposes the two values
 * needed to turn a scroll position into a reading-progress fraction. Using them
 * rather than `contentHeight * scale` avoids the deprecated `scale` property and
 * is correct when the user has pinch-zoomed.
 */
@SuppressLint("ViewConstructor")
private class ReaderWebView(context: Context) : WebView(context) {
    private val verticalScrollRange: Int get() = computeVerticalScrollRange()
    private val verticalScrollExtent: Int get() = computeVerticalScrollExtent()

    /**
     * How much of the article has been seen, 0f..1f.
     *
     * Content that fits entirely on screen counts as fully read: there is
     * nothing left to scroll to, so anything else would mark short topics
     * unread forever.
     */
    fun readingFraction(scrollY: Int): Float {
        val range = verticalScrollRange
        if (range <= 0) return 0f
        val scrollable = range - verticalScrollExtent
        return if (scrollable > 0) {
            (scrollY / scrollable.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }
    }
}

private const val LAYOUT_SETTLE_MS = 250L

private const val ASSET_HOST = "appassets.androidplatform.net"

private fun assetUrl(assetPath: String): String {
    val encoded = Uri.encode(assetPath, "/")
    return "https://$ASSET_HOST/assets/$encoded"
}

/** Kept for callers that need the raw Uri form. */
fun assetUri(assetPath: String): Uri = assetUrl(assetPath).toUri()

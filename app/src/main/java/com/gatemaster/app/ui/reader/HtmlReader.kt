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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
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
    onProgress: (Float) -> Unit = {},
) {
    val context = LocalContext.current

    val assetLoader = remember {
        WebViewAssetLoader.Builder()
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

                    // Lets the bundled CSS respond to the system dark theme.
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
                    }

                    isVerticalScrollBarEnabled = false

                    setOnScrollChangeListener { _, _, scrollY, _, _ ->
                        val scrollable = verticalScrollRange - verticalScrollExtent
                        onProgress(
                            if (scrollable > 0) scrollY / scrollable.toFloat() else 0f,
                        )
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
                            onProgress(0f)
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
                }
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
    val verticalScrollRange: Int get() = computeVerticalScrollRange()
    val verticalScrollExtent: Int get() = computeVerticalScrollExtent()
}

private const val ASSET_HOST = "appassets.androidplatform.net"

private fun assetUrl(assetPath: String): String {
    val encoded = Uri.encode(assetPath, "/")
    return "https://$ASSET_HOST/assets/$encoded"
}

/** Kept for callers that need the raw Uri form. */
fun assetUri(assetPath: String): Uri = assetUrl(assetPath).toUri()

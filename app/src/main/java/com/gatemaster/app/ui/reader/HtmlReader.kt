package com.gatemaster.app.ui.reader

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
 * is nothing for it to do and no reason to widen the attack surface.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlReader(
    assetPath: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var loading by remember(assetPath) { mutableStateOf(true) }

    val assetLoader = remember {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )

                    settings.apply {
                        javaScriptEnabled = false
                        allowFileAccess = false
                        allowContentAccess = false
                        // The content is authored at a fixed width; let the
                        // user zoom without the floating +/- controls.
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        // Honour the reader's viewport meta tag.
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        // textZoom is deliberately left at its default so the
                        // reader still honours the system font-size setting.
                    }

                    // Lets the bundled CSS respond to the system dark theme.
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
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
                            loading = false
                        }
                    }
                }
            },
            update = { webView ->
                val target = assetUrl(assetPath)
                if (webView.url != target) {
                    loading = true
                    webView.loadUrl(target)
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            },
        )

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private const val ASSET_HOST = "appassets.androidplatform.net"

private fun assetUrl(assetPath: String): String {
    val encoded = Uri.encode(assetPath, "/")
    return "https://$ASSET_HOST/assets/$encoded"
}

/** Kept for callers that need the raw Uri form. */
fun assetUri(assetPath: String): Uri = assetUrl(assetPath).toUri()

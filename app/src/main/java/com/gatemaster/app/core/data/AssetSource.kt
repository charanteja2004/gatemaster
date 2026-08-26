package com.gatemaster.app.core.data

import android.content.res.AssetManager
import java.io.InputStream

/**
 * Where bundled study material is read from.
 *
 * The repositories take this rather than [AssetManager] directly. AssetManager
 * cannot be constructed off-device, so depending on it would push every
 * repository test onto an emulator — for no gain, since none of this logic is
 * about Android. The production implementation is one line; tests hand over a
 * map of paths to contents.
 */
interface AssetSource {

    /** @throws java.io.FileNotFoundException when [path] is not bundled. */
    fun open(path: String): InputStream

    /**
     * Whether [path] is actually in the APK. Opening and closing is the only
     * reliable way to ask an AssetManager, and it is cheap: nothing is read.
     */
    fun exists(path: String): Boolean = runCatching { open(path).close() }.isSuccess
}

fun AssetManager.asAssetSource(): AssetSource = object : AssetSource {
    override fun open(path: String): InputStream = this@asAssetSource.open(path)
}

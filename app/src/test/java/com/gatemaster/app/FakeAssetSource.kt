package com.gatemaster.app

import com.gatemaster.app.core.data.AssetSource
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * An [AssetSource] backed by a map, so repository tests run on the JVM.
 *
 * Missing paths throw [FileNotFoundException] exactly as AssetManager does.
 * The repositories' failure paths depend on that, so a fake that returned null
 * instead would be testing something the app never does.
 */
class FakeAssetSource(private val files: Map<String, String>) : AssetSource {

    override fun open(path: String): InputStream =
        files[path]?.let { ByteArrayInputStream(it.toByteArray()) }
            ?: throw FileNotFoundException(path)
}

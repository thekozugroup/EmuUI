package com.swordfish.lemuroid.app.shared.covers

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SecureThumbnailInterceptorTest {
    @Test
    fun upgradesPersistedCoverAndPreservesEncodedTitle() {
        val url = "http://thumbnails.libretro.com/Nintendo%20DS/Named_Boxarts/QA%20%26%20Test.png".toHttpUrl()
        val secure = SecureThumbnailInterceptor.secureUrl(url)
        assertEquals("https", secure.scheme)
        assertEquals(443, secure.port)
        assertEquals(url.encodedPath, secure.encodedPath)
    }

    @Test
    fun doesNotRewriteOtherHostsOrPorts() {
        listOf(
            "https://thumbnails.libretro.com/image.png",
            "http://thumbnails.libretro.com.example.org/image.png",
            "http://example.org/image.png",
            "http://thumbnails.libretro.com:8080/image.png",
        ).forEach {
            val url = it.toHttpUrl()
            assertSame(url, SecureThumbnailInterceptor.secureUrl(url))
        }
    }
}

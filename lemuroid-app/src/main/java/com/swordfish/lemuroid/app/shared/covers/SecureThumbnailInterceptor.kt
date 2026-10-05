package com.swordfish.lemuroid.app.shared.covers

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/** Upgrade persisted cover URLs before opening a connection, without rewriting library/save data. */
object SecureThumbnailInterceptor : Interceptor {
    internal fun secureUrl(url: HttpUrl): HttpUrl =
        if (url.scheme == "http" && url.host == "thumbnails.libretro.com" && url.port == 80) {
            url.newBuilder().scheme("https").build()
        } else {
            url
        }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(request.newBuilder().url(secureUrl(request.url)).build())
    }
}

package com.shapeshed.kiosk

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.shapeshed.kiosk.data.HnApi
import com.shapeshed.kiosk.data.HnRepository
import com.shapeshed.kiosk.data.SettingsStore
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Owns the app's few long-lived singletons. No DI framework — the same deliberately plain wiring
 * as the rest of the app; ViewModels reach [repository] via the application instance. Also serves
 * as Coil's image-loader factory so favicons load through the shared OkHttp client.
 */
class KioskApp : Application(), SingletonImageLoader.Factory {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            // A feed page fans out ~25 item requests at once, all to the HN host. OkHttp's default
            // cap of 5 requests/host would serialise them into 5 rounds; lift it so a page loads in
            // roughly one round-trip (HN speaks HTTP/2, so these multiplex over one connection).
            .dispatcher(Dispatcher().apply { maxRequestsPerHost = 32 })
            // Disk cache so relaunches and stories shared across feeds come from disk, not network.
            .cache(Cache(File(cacheDir, "http_cache"), CACHE_SIZE_BYTES))
            .addNetworkInterceptor(hnCacheControlInterceptor)
            .build()
    }

    val repository: HnRepository by lazy { HnRepository(HnApi(okHttpClient)) }

    val settings: SettingsStore by lazy { SettingsStore(this) }

    override fun newImageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
            .build()

    private companion object {
        const val CACHE_SIZE_BYTES = 20L * 1024 * 1024

        /**
         * The HN API sends no cache headers, so responses are never stored. Rewrite them: item JSON
         * is effectively immutable for our list (title/host), so cache it briefly; feed id lists
         * must stay fresh so pull-to-refresh is real. Only the HN host is touched — favicon/image
         * requests keep their own headers and Coil's cache.
         */
        val hnCacheControlInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (request.url.host != "hacker-news.firebaseio.com") return@Interceptor response
            val cacheControl = if (request.url.encodedPath.contains("/item/")) {
                "public, max-age=600"
            } else {
                "public, no-cache"
            }
            response.newBuilder()
                .header("Cache-Control", cacheControl)
                .removeHeader("Pragma")
                .build()
        }
    }
}

package peekniko.okhttp

import peekniko.Peekniko
import okhttp3.Interceptor
import okhttp3.Response

/** An OkHttp interceptor that forwards requests without observing or modifying them. */
internal class PeeknikoInterceptor : Interceptor {
    public override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}

public val Peekniko.interceptor: Interceptor by lazy { PeeknikoInterceptor() }

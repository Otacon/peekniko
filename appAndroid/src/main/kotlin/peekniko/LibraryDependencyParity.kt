package peekniko

import android.app.Application
import peekniko.ktor.plugin
import peekniko.okhttp.interceptor
import peekniko.storage.sqldelight.sqlDelightStorage
import io.ktor.client.HttpClient
import okhttp3.OkHttpClient

@Suppress("unused")
internal object LibraryDependencyParity {

    private lateinit var context: Application

    fun configure() {
        Peekniko.configure {
            inMemoryStorage(maxApiCalls = 100)
            sqlDelightStorage(name = "peekniko.db")
        }

        HttpClient {
            install(Peekniko.plugin)
        }
        OkHttpClient.Builder()
            .addInterceptor(Peekniko.interceptor)

        Peekniko.startPeeknikoActivity(context)
    }
}

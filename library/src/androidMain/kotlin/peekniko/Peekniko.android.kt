package peekniko

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.startup.Initializer
import peekniko.ui.networkcalls.PeeknikoActivity

@Volatile
private var contextInternal: Application? = null

internal val Peekniko.context: Context
    get() = contextInternal!!

public val Peekniko.Configuration.context: Context
    get() = contextInternal!!

public class PeeknikoInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        contextInternal = context.applicationContext as Application
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

/** Opens the Peekniko inspector in its own activity. */
public fun Peekniko.open(context: Context) {
    val intent = Intent(context, PeeknikoActivity::class.java)
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
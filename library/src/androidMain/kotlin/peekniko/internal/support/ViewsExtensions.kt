package peekniko.internal.support

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import kotlin.properties.ReadOnlyProperty

internal fun <T : View> Activity.viewById(id: Int): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    findViewById<T>(id) ?: error("View with ID $id was not found in the Activity's content view")
}

internal fun <T : View> Fragment.viewById(id: Int): ReadOnlyProperty<Fragment, T> = ReadOnlyProperty { fragment, _ ->
    fragment.requireView().findViewById<T>(id) ?: error("View with ID $id was not found in the Fragment's content view")
}

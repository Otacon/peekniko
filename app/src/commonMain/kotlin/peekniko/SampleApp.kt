package peekniko

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

@Composable
public fun SampleApp(onOpenPeekniko: (() -> Unit)? = null) {
    val factory = remember { SampleViewModelFactory() }

    SampleAppTheme {
        SampleScreen(
            viewModelFactory = factory,
            onOpenPeekniko = onOpenPeekniko,
        )
    }
}

@Composable
public fun SampleAppTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme, content = content)
}

private class SampleViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        return SampleViewModel() as T
    }
}

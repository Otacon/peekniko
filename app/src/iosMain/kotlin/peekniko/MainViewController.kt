package peekniko

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

private val sampleViewModel: SampleViewModel by lazy {
    AppModule.initialize()
    SampleViewModel()
}

@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    val viewModel = sampleViewModel
    return ComposeUIViewController {
        SampleApp(viewModel = viewModel, onOpenPeekniko = { Peekniko.open() })
    }
}

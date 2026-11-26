package peekniko

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// Lives as long as the app, so requests keep running while Peekniko is presented.
private val sampleViewModel: SampleViewModel by lazy {
    AppModule.initialize()
    SampleViewModel()
}

/** Entry point of the shared Compose sample UI on iOS. The `appiOS` project uses the Swift package instead. */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    val viewModel = sampleViewModel
    return ComposeUIViewController {
        SampleApp(viewModel = viewModel, onOpenPeekniko = { Peekniko.open() })
    }
}

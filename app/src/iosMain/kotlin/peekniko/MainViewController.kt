package peekniko

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

private var isAppModuleInitialized = false

/** Entry point of the shared sample UI, hosted by the Xcode project in `appiOS`. */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    if (!isAppModuleInitialized) {
        AppModule.initialize()
        isAppModuleInitialized = true
    }
    return ComposeUIViewController {
        SampleApp(onOpenPeekniko = { Peekniko.present() })
    }
}

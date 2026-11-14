package peekniko

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.res.painterResource
import peekniko.storage.sqldelight.sqlDelightStorage
import peekniko.ui.openPeeknikoWindow as openLibraryWindow
import java.awt.Taskbar
import javax.imageio.ImageIO

fun main() {
    Peekniko.configure {
        sqlDelightStorage()
    }
    AppModule.initialize()
    setMacTaskbarIcon()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sample Peekniko app",
            icon = painterResource("icon.png"),
        ) {
            SampleApp(onOpenPeekniko = { openLibraryWindow() })
        }
    }
}

private fun setMacTaskbarIcon() {
    runCatching {
        val taskbar = Taskbar.getTaskbar()
        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            val image = Taskbar::class.java.getResourceAsStream("/icon.png")
                ?.use(ImageIO::read)
            if (image != null) taskbar.setIconImage(image)
        }
    }
}

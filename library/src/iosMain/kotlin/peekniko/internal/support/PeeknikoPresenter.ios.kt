package peekniko.internal.support

import peekniko.Peekniko
import peekniko.viewController
import platform.UIKit.UIApplication
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

@OptIn(ExperimentalNativeApi::class)
internal object PeeknikoPresenter {
    private var presented: WeakReference<UIViewController>? = null

    fun present(from: UIViewController?) {
        if (presented?.value?.presentingViewController != null) return
        val presenter = from ?: topViewController() ?: return
        val controller = Peekniko.viewController(onClose = ::dismiss)
        controller.modalPresentationStyle = UIModalPresentationFullScreen
        presented = WeakReference(controller)
        presenter.presentViewController(controller, animated = true, completion = null)
    }

    private fun dismiss() {
        presented?.value?.dismissViewControllerAnimated(true, completion = null)
        presented = null
    }

    private fun topViewController(): UIViewController? {
        val scenes = UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>()
        val scene = scenes.firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
            ?: scenes.firstOrNull()
        val windows = scene?.windows.orEmpty().filterIsInstance<UIWindow>()
        val window = windows.firstOrNull { it.isKeyWindow() } ?: windows.firstOrNull()
        var top = window?.rootViewController ?: return null
        while (true) {
            top = top.presentedViewController ?: return top
        }
    }
}

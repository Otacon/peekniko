import SwiftUI
import Peekniko

@main
struct PeeknikoApp: App {
    init() {
        Peekniko.shared.configure { configuration in
            configuration.sqlDelightStorage(name: "peekniko.db")
        }
        precondition(Peekniko.shared.registerUrlProtocol(), "Failed to register Peekniko URLProtocol")
    }

    var body: some Scene {
        WindowGroup {
            SampleAppView()
                .ignoresSafeArea()
        }
    }
}

/// Hosts the shared Compose sample UI from the `app` module.
private struct SampleAppView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

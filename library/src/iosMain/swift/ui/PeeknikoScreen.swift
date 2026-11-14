import Foundation
import SwiftUI
import UIKit
#if canImport(PeeknikoCore)
import PeeknikoCore
#elseif canImport(App)
import App
#endif

public struct PeeknikoScreen: View {
    @StateObject private var viewModel = NetworkCallListViewModel()
    @State private var selectedCallId: String?

    public init() {}

    public var body: some View {
        Group {
            if UIDevice.current.userInterfaceIdiom == .pad {
                NavigationSplitView {
                    NetworkCallListView(
                        viewModel: viewModel,
                        selectedCallId: $selectedCallId
                    )
                    .navigationTitle("Peekniko")
                } detail: {
                    if let callId = selectedCallId {
                        NetworkCallDetailView(callId: callId)
                            .id(callId)
                    } else {
                        Text("Select a network call")
                            .foregroundStyle(.secondary)
                    }
                }
            } else {
                NavigationStack {
                    NetworkCallListView(
                        viewModel: viewModel,
                        selectedCallId: $selectedCallId
                    )
                    .navigationTitle("Peekniko")
                    .navigationDestination(for: String.self) { callId in
                        NetworkCallDetailView(callId: callId)
                    }
                }
            }
        }
    }
}

@objc(PeeknikoHostingViewController)
public final class PeeknikoHostingViewController: UIViewController {
    public convenience init() {
        self.init(nibName: nil, bundle: nil)
    }

    @objc override public init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override public func viewDidLoad() {
        super.viewDidLoad()
        let hostingController = UIHostingController(rootView: PeeknikoScreen())
        addChild(hostingController)
        hostingController.view.frame = view.bounds
        hostingController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(hostingController.view)
        hostingController.didMove(toParent: self)
    }
}

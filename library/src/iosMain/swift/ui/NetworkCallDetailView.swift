import Foundation
import SwiftUI
import UIKit
#if canImport(PeeknikoCore)
import PeeknikoCore
#elseif canImport(App)
import App
#endif

@MainActor
final class NetworkCallDetailObserver: ObservableObject {
    @Published var detail: PeeknikoCallDetailItem?
    private var token: PeeknikoObservationToken?

    init(callId: String) {
        token = PeeknikoIosUiBridge.shared.observeCallDetail(callId: callId) { [weak self] item in
            self?.detail = item
        }
    }

    deinit {
        token?.cancel()
    }
}

struct NetworkCallDetailView: View {
    @StateObject private var observer: NetworkCallDetailObserver
    @State private var selectedTab: Int = 0
    @State private var toastMessage: String?

    init(callId: String) {
        _observer = StateObject(wrappedValue: NetworkCallDetailObserver(callId: callId))
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selectedTab) {
                SummaryTabView(detail: observer.detail, onToast: showToast)
                    .tabItem {
                        Label("Overview", systemImage: "doc.text.magnifyingglass")
                    }
                    .tag(0)

                RequestTabView(detail: observer.detail, onToast: showToast)
                    .tabItem {
                        Label("Request", systemImage: "arrow.up.doc")
                    }
                    .tag(1)

                ResponseTabView(detail: observer.detail, onToast: showToast)
                    .tabItem {
                        Label("Response", systemImage: "arrow.down.doc")
                    }
                    .tag(2)
            }

            if let toastMessage {
                Text(toastMessage)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 9)
                    .background(Color.black.opacity(0.85))
                    .clipShape(Capsule())
                    .padding(.bottom, 64)
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
        }
        .navigationTitle(observer.detail?.title ?? "Loading transaction…")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button("cURL") {
                    copyCurl()
                }
                Button {
                    copyActiveBody()
                } label: {
                    Image(systemName: "doc.on.doc")
                }
                .accessibilityLabel("Copy Body")
            }
        }
    }

    private func copyCurl() {
        guard let curl = observer.detail?.curl, !curl.isEmpty else {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showToast("No transaction to copy")
            return
        }
        UIPasteboard.general.string = curl
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        showToast("cURL copied")
    }

    private func copyActiveBody() {
        guard let detail = observer.detail else {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showToast("No body to copy")
            return
        }
        let pair: (String?, String)
        switch selectedTab {
        case 1:
            pair = (detail.rawRequestBody, "Request body")
        case 2:
            pair = (detail.rawResponseBody, "Response body")
        default:
            if let resp = detail.rawResponseBody, !resp.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                pair = (resp, "Response body")
            } else {
                pair = (detail.rawRequestBody, "Request body")
            }
        }
        if let body = pair.0, !body.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            UIPasteboard.general.string = body
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            showToast("\(pair.1) copied")
        } else {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showToast("No body to copy")
        }
    }

    private func showToast(_ message: String) {
        withAnimation(.easeInOut(duration: 0.2)) {
            toastMessage = message
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
            withAnimation(.easeInOut(duration: 0.25)) {
                if toastMessage == message {
                    toastMessage = nil
                }
            }
        }
    }
}

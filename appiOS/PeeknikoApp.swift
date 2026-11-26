import SwiftUI
// The "Peekniko Debug" target links PeeknikoKit and sets the PEEKNIKO compilation
// condition; "Peekniko Release" links PeeknikoKitNoop. Both modules have the same API.
#if PEEKNIKO
import PeeknikoCore
#else
import PeeknikoCoreNoop
#endif

@main
struct PeeknikoApp: App {
    @State private var viewModel: SampleViewModel

    init() {
        Peekniko.shared.configure { configuration in
            configuration.sqlDelightStorage(name: "peekniko.db")
        }
        precondition(Peekniko.shared.registerUrlProtocol(), "Failed to register Peekniko URLProtocol")
        _viewModel = State(initialValue: SampleViewModel())
    }

    var body: some Scene {
        WindowGroup {
            SampleScreen(viewModel: viewModel)
        }
    }
}

// MARK: - Screen (mirrors SampleScreen.kt in the app module)

private struct SampleScreen: View {
    let viewModel: SampleViewModel

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 8) {
                Text(viewModel.status)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                SampleButton("Open Peekniko") {
                    Peekniko.shared.open(from: nil)
                }
                SampleButton("Run custom URLSession requests", enabled: !viewModel.isLoading) {
                    viewModel.onRunCustomUrlSession()
                }
                SampleButton("Run URLSession requests", enabled: !viewModel.isLoading) {
                    viewModel.onRunUrlSession()
                }
            }
            .padding(16)
            .navigationTitle("Sample Peekniko app")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

private struct SampleButton: View {
    let title: String
    let enabled: Bool
    let action: () -> Void

    init(_ title: String, enabled: Bool = true, action: @escaping () -> Void) {
        self.title = title
        self.enabled = enabled
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(title).frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .disabled(!enabled)
    }
}

// MARK: - State and actions (mirrors SampleViewModel.kt)

@MainActor
@Observable
private final class SampleViewModel {
    private static let baseUrl = "https://httpbin.org"

    private(set) var status = "Choose a client to run the scenarios."
    private(set) var isLoading = false

    /// Requests through `URLSession.shared`, captured via `registerUrlProtocol()`.
    private let sharedClient = PlaygroundClient(session: .shared)

    /// Requests through a session with its own configuration, which needs
    /// `addUrlProtocol(configuration:)`. Stands in for the Ktor client of the Kotlin sample,
    /// which a Swift-only app can't use.
    private let customClient: PlaygroundClient = {
        let configuration = URLSessionConfiguration.default
        Peekniko.shared.addUrlProtocol(configuration: configuration)
        return PlaygroundClient(session: URLSession(configuration: configuration))
    }()

    func onRunCustomUrlSession() { runScenarios("custom URLSession", sharedClient: false) }

    func onRunUrlSession() { runScenarios("URLSession", sharedClient: true) }

    private func runScenarios(_ clientName: String, sharedClient useShared: Bool) {
        let client = useShared ? sharedClient : customClient
        isLoading = true
        status = "Running \(clientName) requests..."
        Task {
            for request in playgroundRequests {
                // As in the Kotlin sample, one failing scenario doesn't stop the others.
                _ = try? await client.execute(baseUrl: Self.baseUrl, request: request)
            }
            status = "Requests completed"
            isLoading = false
        }
    }
}

// MARK: - HTTP client (mirrors IosUrlSessionPlaygroundClient.kt)

private struct PlaygroundClient {
    let session: URLSession

    func execute(baseUrl: String, request: PlaygroundRequest) async throws -> (statusCode: Int, body: String) {
        guard let url = URL(string: baseUrl + request.path) else { throw URLError(.badURL) }
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = request.method
        for header in request.headers {
            urlRequest.setValue(header.value, forHTTPHeaderField: header.key)
        }
        urlRequest.httpBody = request.body.map { Data($0.utf8) }
        let (data, response) = try await session.data(for: urlRequest)
        let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 0
        return (statusCode, String(decoding: data, as: UTF8.self))
    }
}

// MARK: - Scenarios (same as PlaygroundScenarios.kt)

private struct PlaygroundRequest {
    let method: String
    let path: String
    let headers: KeyValuePairs<String, String>
    var body: String? = nil
}

private let playgroundRequests: [PlaygroundRequest] = [
    PlaygroundRequest(
        method: "GET",
        path: "/get?scenario=list-items",
        headers: ["Accept": "application/json", "X-Request-Id": "list-items"]
    ),
    PlaygroundRequest(
        method: "POST",
        path: "/post",
        headers: [
            "Accept": "application/json",
            "Content-Type": "application/json; charset=utf-8",
            "X-Request-Id": "create-item",
        ],
        body: #"{"name":"created from playground","active":true}"#
    ),
    PlaygroundRequest(
        method: "PATCH",
        path: "/patch",
        headers: [
            "Accept": "application/json",
            "Content-Type": "application/json; charset=utf-8",
            "X-Request-Id": "patch-item",
        ],
        body: #"{"name":"updated from playground"}"#
    ),
    PlaygroundRequest(
        method: "PUT",
        path: "/put",
        headers: [
            "Accept": "application/json",
            "Content-Type": "application/x-www-form-urlencoded",
            "X-Request-Id": "update-form",
        ],
        body: "name=playground+item&source=android-app"
    ),
    PlaygroundRequest(
        method: "POST",
        path: "/post",
        headers: [
            "Accept": "application/json",
            "Content-Type": "application/x-www-form-urlencoded; charset=utf-8",
            "X-Request-Id": "form-echo",
        ],
        body: "name=playground+user&status=ready"
    ),
    PlaygroundRequest(
        method: "GET",
        path: "/status/500",
        headers: ["Accept": "application/json", "X-Request-Id": "server-error"]
    ),
    PlaygroundRequest(
        method: "POST",
        path: "/status/422",
        headers: [
            "Accept": "application/json",
            "Content-Type": "application/json; charset=utf-8",
            "X-Request-Id": "validation-error",
        ],
        body: #"{"name":""}"#
    ),
    PlaygroundRequest(
        method: "GET",
        path: "/xml",
        headers: ["Accept": "application/xml", "X-Request-Id": "xml-catalog"]
    ),
    PlaygroundRequest(
        method: "GET",
        path: "/html",
        headers: ["Accept": "text/html", "X-Request-Id": "html-page"]
    ),
    PlaygroundRequest(
        method: "GET",
        path: "/robots.txt",
        headers: ["Accept": "text/plain", "X-Request-Id": "plain-text"]
    ),
    PlaygroundRequest(
        method: "GET",
        path: "/image/png",
        headers: ["Accept": "image/png", "X-Request-Id": "image-png"]
    ),
    PlaygroundRequest(
        method: "GET",
        path: "/image/jpeg",
        headers: ["Accept": "image/jpeg", "X-Request-Id": "image-jpeg"]
    ),
    PlaygroundRequest(
        method: "GET",
        path: "/image/webp",
        headers: ["Accept": "image/webp", "X-Request-Id": "image-webp"]
    ),
    PlaygroundRequest(
        method: "GET",
        path: "/image/svg",
        headers: ["Accept": "image/svg+xml", "X-Request-Id": "image-svg"]
    ),
    PlaygroundRequest(
        method: "GET",
        path: "/delay/1",
        headers: ["Accept": "application/json", "X-Request-Id": "delayed-response"]
    ),
]

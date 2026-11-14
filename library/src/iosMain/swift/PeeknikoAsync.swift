#if canImport(PeeknikoCore)
import PeeknikoCore

@MainActor
private final class ObservationCancellation {
    private var token: PeeknikoObservationToken?

    init(_ token: PeeknikoObservationToken) {
        self.token = token
    }

    func cancel() {
        token?.cancel()
        token = nil
    }
}

@MainActor
public enum PeeknikoAsync {
    public static func calls() -> AsyncStream<[PeeknikoCallRowItem]> {
        AsyncStream { continuation in
            let token = PeeknikoIosUiBridge.shared.observeCalls { calls in
                continuation.yield(calls)
            }
            let cancellation = ObservationCancellation(token)
            continuation.onTermination = { _ in
                Task { @MainActor in
                    cancellation.cancel()
                }
            }
        }
    }

    public static func callDetail(id: String) -> AsyncStream<PeeknikoCallDetailItem?> {
        AsyncStream { continuation in
            let token = PeeknikoIosUiBridge.shared.observeCallDetail(callId: id) { detail in
                continuation.yield(detail)
            }
            let cancellation = ObservationCancellation(token)
            continuation.onTermination = { _ in
                Task { @MainActor in
                    cancellation.cancel()
                }
            }
        }
    }
}
#endif

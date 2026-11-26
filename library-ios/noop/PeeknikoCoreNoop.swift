import Foundation
import UIKit
import UserNotifications

public final class Peekniko {
    public static let shared = Peekniko()

    private init() {}

    /// Does nothing: `configure` is never called.
    public func configure(configure: @escaping (Configuration) -> Void) {}

    public final class Configuration {
        private init() {}

        public func inMemoryStorage(maxApiCalls: Int32) {}

        public func sqlDelightStorage(name: String) {}
    }
}

public extension Peekniko {
    func open(from: UIViewController?) {}

    func isPeeknikoNotification(notification: UNNotification) -> Bool { false }

    func handleNotificationResponse(response: UNNotificationResponse) -> Bool { false }

    func registerUrlProtocol() -> Bool { true }

    func unregisterUrlProtocol() {}

    func addUrlProtocol(configuration: URLSessionConfiguration) {}
}

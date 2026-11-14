import Foundation
#if canImport(PeeknikoCore)
import PeeknikoCore
#elseif canImport(App)
import App
#endif

public extension Peekniko {
    var urlProtocol: AnyClass {
        return PeeknikoURLProtocol.self
    }
}

nonisolated private final class PeeknikoURLProtocol: URLProtocol, @unchecked Sendable {
    private static let handledKey = "PeeknikoURLProtocolHandledKey"

    private static let recordingQueue = DispatchQueue(
        label: "com.peekniko.urlprotocol.recording",
        qos: .utility
    )

    private static let internalSession: URLSession = {
        let config = URLSessionConfiguration.default
        if let existingProtocols = config.protocolClasses {
            config.protocolClasses = existingProtocols.filter { $0 != PeeknikoURLProtocol.self }
        }
        return URLSession(configuration: config)
    }()

    private var activeTask: URLSessionDataTask?

    override class func canInit(with request: URLRequest) -> Bool {
        guard let url = request.url, ["http", "https"].contains(url.scheme?.lowercased()) else {
            return false
        }
        if URLProtocol.property(forKey: handledKey, in: request) != nil {
            return false
        }
        return true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        return request
    }

    override func startLoading() {
        guard let mutableRequest = (request as NSURLRequest).mutableCopy() as? NSMutableURLRequest else { return }
        URLProtocol.setProperty(true, forKey: Self.handledKey, in: mutableRequest)
        if mutableRequest.httpBody == nil, let streamData = Self.extractBodyData(from: request) {
            mutableRequest.httpBody = streamData
        }
        let actualRequest = mutableRequest as URLRequest

        let currentCallId = UUID().uuidString
        let currentStartTimestamp = Int64(Date().timeIntervalSince1970 * 1000)
        Self.recordingQueue.async {
            Self.recordRequest(actualRequest, callId: currentCallId, startTimestamp: currentStartTimestamp)
        }
        self.activeTask = Self.internalSession.dataTask(with: actualRequest) { [weak self] data, response, error in
            guard let self = self else { return }

            if let error = error {
                Self.recordingQueue.async {
                    Self.recordError(error, callId: currentCallId)
                }
                self.client?.urlProtocol(self, didFailWithError: error)
                return
            }

            if let httpResponse = response as? HTTPURLResponse {
                let endTimestamp = Int64(Date().timeIntervalSince1970 * 1000)
                Self.recordingQueue.async {
                    Self.recordResponse(
                        httpResponse,
                        data: data,
                        callId: currentCallId,
                        endTimestamp: endTimestamp
                    )
                }
                self.client?.urlProtocol(self, didReceive: httpResponse, cacheStoragePolicy: .notAllowed)
            }
            if let data = data {
                self.client?.urlProtocol(self, didLoad: data)
            }
            self.client?.urlProtocolDidFinishLoading(self)
        }
        self.activeTask?.resume()
    }

    override func stopLoading() {
        activeTask?.cancel()
    }

    private static func extractBodyData(from request: URLRequest) -> Data? {
        if let body = request.httpBody {
            return body
        }
        guard let stream = request.httpBodyStream else {
            return nil
        }
        stream.open()
        defer { stream.close() }
        var data = Data()
        let bufferSize = 4096
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer { buffer.deallocate() }
        while stream.hasBytesAvailable {
            let readCount = stream.read(buffer, maxLength: bufferSize)
            if readCount > 0 {
                data.append(buffer, count: readCount)
            } else {
                break
            }
        }
        return data.isEmpty ? nil : data
    }

    private static func recordRequest(_ request: URLRequest, callId: String, startTimestamp: Int64) {
        guard let url = request.url, let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return
        }
        let bodyData = extractBodyData(from: request)

        let headers = (request.allHTTPHeaderFields ?? [:]).map { key, value in
            NetworkHeader(name: key, value: value)
        }

        let method = NetworkMethod.entries.first {
            $0.name.caseInsensitiveCompare(request.httpMethod ?? "GET") == .orderedSame
        } ?? .unknown

        let networkRequest = NetworkRequest.companion.create(
            scheme: components.scheme ?? "",
            host: components.host ?? "",
            port: components.port.map { KotlinInt(value: Int32($0)) },
            endpoint: components.percentEncodedPath.isEmpty ? "/" : components.percentEncodedPath,
            queryParams: components.percentEncodedQuery ?? "",
            method: method.name,
            headers: headers,
            body: bodyData,
            requestTimestampEpochMillis: startTimestamp
        )
        _ = Peekniko.shared.networkRepository.insert(id: callId, request: networkRequest)
    }

    private static func recordResponse(_ response: HTTPURLResponse, data: Data?, callId: String, endTimestamp: Int64) {
        let headers = response.allHeaderFields.compactMap { key, value -> NetworkHeader? in
            let headerName = String(describing: key)
            let headerValue = String(describing: value)
            return NetworkHeader(name: headerName, value: headerValue)
        }

        let networkResponse = NetworkResponseResponse.companion.create(
            body: data,
            headers: headers,
            code: Int32(response.statusCode),
            responseTimestampEpochMillis: endTimestamp
        )
        _ = Peekniko.shared.networkRepository.insert(id: callId, response: networkResponse)
    }

    private static func recordError(_ error: Error, callId: String) {
        let networkResponse = NetworkResponseError.companion.create(
            exceptionMessage: error.localizedDescription,
            stackTrace: Thread.callStackSymbols.joined(separator: "\n"),
            responseTimestampEpochMillis: Int64(Date().timeIntervalSince1970 * 1000)
        )
        _ = Peekniko.shared.networkRepository.insert(id: callId, response: networkResponse)
    }

}

import Foundation
#if canImport(PeeknikoCore)
import PeeknikoCore
#elseif canImport(App)
import App
#endif

enum BodyFormatter {
    static func formatBody(_ raw: String, contentType: BodyContentType) -> String {
        switch contentType {
        case .json:
            return prettyFormatJson(raw) ?? raw
        case .xml, .html:
            return prettyFormatMarkup(raw)
        case .formUrlEncoded:
            return prettyFormatFormUrlEncoded(raw)
        default:
            return raw
        }
    }

    static func prettyFormatJson(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.first == "{" || trimmed.first == "[" else { return nil }
        guard let data = trimmed.data(using: .utf8),
              let jsonObject = try? JSONSerialization.jsonObject(with: data, options: [.fragmentsAllowed]),
              let prettyData = try? JSONSerialization.data(
                  withJSONObject: jsonObject,
                  options: [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes]
              ),
              let prettyString = String(data: prettyData, encoding: .utf8) else {
            return nil
        }
        return prettyString
    }

    static func prettyFormatFormUrlEncoded(_ raw: String) -> String {
        let pairs = raw.components(separatedBy: "&")
        guard pairs.count > 1 || raw.contains("=") else { return raw }
        return pairs.map { pair in
            guard let eqIndex = pair.firstIndex(of: "=") else {
                return pair.replacingOccurrences(of: "+", with: " ")
            }
            let key = pair[..<eqIndex].replacingOccurrences(of: "+", with: " ")
            let value = pair[pair.index(after: eqIndex)...].replacingOccurrences(of: "+", with: " ")
            return "\(key) = \(value)"
        }.joined(separator: "\n")
    }

    static func prettyFormatMarkup(_ raw: String) -> String {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.contains("<") && trimmed.contains(">") else { return raw }
        // Only auto-indent if payload is mostly single-line or compact
        if trimmed.components(separatedBy: "\n").count > 4 {
            return raw
        }
        let voidTags: Set<String> = [
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
        ]
        var output: [String] = []
        var depth = 0
        var index = trimmed.startIndex

        while index < trimmed.endIndex {
            if trimmed[index] == "<" {
                let tagEnd = trimmed[index...].firstIndex(of: ">") ?? trimmed.index(before: trimmed.endIndex)
                let token = String(trimmed[index...tagEnd]).trimmingCharacters(in: .whitespacesAndNewlines)
                if token.hasPrefix("</") {
                    depth = max(0, depth - 1)
                }
                let indent = String(repeating: "  ", count: depth)
                output.append(indent + token)
                if !token.hasPrefix("</") && !token.hasSuffix("/>") && !token.hasPrefix("<!") && !token.hasPrefix("<?") {
                    let tagName = token.dropFirst()
                        .prefix(while: { $0.isLetter || $0.isNumber || $0 == "-" || $0 == ":" || $0 == "_" })
                        .lowercased()
                    if !voidTags.contains(tagName) {
                        depth += 1
                    }
                }
                index = trimmed.index(after: tagEnd)
            } else {
                let nextTag = trimmed[index...].firstIndex(of: "<") ?? trimmed.endIndex
                let textNode = String(trimmed[index..<nextTag]).trimmingCharacters(in: .whitespacesAndNewlines)
                if !textNode.isEmpty {
                    let indent = String(repeating: "  ", count: depth)
                    output.append(indent + textNode)
                }
                index = nextTag
            }
        }
        return output.isEmpty ? raw : output.joined(separator: "\n")
    }
}

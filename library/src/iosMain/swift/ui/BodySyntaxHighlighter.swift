import SwiftUI
#if canImport(PeeknikoCore)
import PeeknikoCore
#elseif canImport(App)
import App
#endif

enum BodySyntaxHighlighter {
    static func highlightLine(
        _ text: String,
        contentType: BodyContentType,
        palette: SwiftBodyPalette = .default
    ) -> AttributedString {
        switch contentType {
        case .json:
            return highlightJsonLine(text, palette: palette)
        case .xml, .html:
            return highlightMarkupLine(text, palette: palette)
        case .formUrlEncoded:
            return highlightFormUrlEncodedLine(text, palette: palette)
        default:
            var attr = AttributedString(text)
            attr.foregroundColor = palette.text
            return attr
        }
    }

    static func applySearchHighlights(
        to attr: inout AttributedString,
        lineText: String,
        searchQuery: String,
        isActiveMatch: Bool
    ) {
        guard !searchQuery.isEmpty else { return }
        let lowerLine = lineText.lowercased()
        let lowerQuery = searchQuery.lowercased()
        var searchStart = lowerLine.startIndex
        while let range = lowerLine.range(of: lowerQuery, range: searchStart..<lowerLine.endIndex) {
            if let attrRange = Range(range, in: attr) {
                attr[attrRange].backgroundColor = isActiveMatch ? .orange.opacity(0.65) : .yellow.opacity(0.45)
                attr[attrRange].foregroundColor = .black
            }
            searchStart = range.upperBound
        }
    }

    private static func highlightJsonLine(_ text: String, palette: SwiftBodyPalette) -> AttributedString {
        var result = AttributedString(text)
        result.foregroundColor = palette.separator
        let trimmed = text.trimmingCharacters(in: .whitespaces)
        if trimmed.isEmpty { return result }

        if let colonIndex = text.range(of: "\":") {
            let keyPartRange = text.startIndex..<colonIndex.lowerBound
            if let firstQuote = text[keyPartRange].firstIndex(of: "\"") {
                let keySpan = firstQuote...colonIndex.lowerBound
                if let attrKeyRange = Range(keySpan, in: result) {
                    result[attrKeyRange].foregroundColor = palette.key
                }
            }
            let valueStart = colonIndex.upperBound
            let valueSlice = text[valueStart...]
            colorJsonValue(in: &result, fullText: text, slice: valueSlice, palette: palette)
        } else {
            colorJsonValue(in: &result, fullText: text, slice: text[text.startIndex...], palette: palette)
        }
        return result
    }

    private static func colorJsonValue(
        in result: inout AttributedString,
        fullText: String,
        slice: Substring,
        palette: SwiftBodyPalette
    ) {
        let trimmed = slice.trimmingCharacters(in: .whitespaces)
        let content = trimmed.hasSuffix(",") ? String(trimmed.dropLast()) : trimmed
        guard !content.isEmpty,
              let rangeInFull = fullText.range(of: content, range: slice.startIndex..<slice.endIndex),
              let attrRange = Range(rangeInFull, in: result) else {
            return
        }

        if content.hasPrefix("\"") && content.hasSuffix("\"") {
            result[attrRange].foregroundColor = palette.string
        } else if content == "true" || content == "false" {
            result[attrRange].foregroundColor = palette.boolean
        } else if content == "null" {
            result[attrRange].foregroundColor = palette.nullValue
        } else if Double(content) != nil {
            result[attrRange].foregroundColor = palette.number
        }
    }

    private static func highlightFormUrlEncodedLine(_ text: String, palette: SwiftBodyPalette) -> AttributedString {
        var result = AttributedString(text)
        result.foregroundColor = palette.text

        if let eqRange = text.range(of: "=") {
            let keyRange = text.startIndex..<eqRange.lowerBound
            let valRange = eqRange.upperBound..<text.endIndex
            if let attrKey = Range(keyRange, in: result) {
                result[attrKey].foregroundColor = palette.key
            }
            if let attrEq = Range(eqRange, in: result) {
                result[attrEq].foregroundColor = palette.separator
            }
            if let attrVal = Range(valRange, in: result) {
                result[attrVal].foregroundColor = palette.attributeValue
            }
        }
        return result
    }

    private static func highlightMarkupLine(_ text: String, palette: SwiftBodyPalette) -> AttributedString {
        var result = AttributedString(text)
        result.foregroundColor = palette.text
        let trimmed = text.trimmingCharacters(in: .whitespaces)
        if trimmed.isEmpty { return result }

        if trimmed.hasPrefix("<!--") {
            result.foregroundColor = palette.comment
            return result
        }
        if trimmed.uppercased().hasPrefix("<!DOCTYPE") {
            result.foregroundColor = palette.doctype
            return result
        }

        var searchIndex = text.startIndex
        while searchIndex < text.endIndex,
              let openIdx = text[searchIndex...].firstIndex(of: "<") {
            let closeIdx = text[openIdx...].firstIndex(of: ">") ?? text.index(before: text.endIndex)
            colorSingleMarkupTag(in: &result, fullText: text, tagStart: openIdx, tagEnd: closeIdx, palette: palette)
            searchIndex = text.index(after: closeIdx)
        }
        return result
    }

    private static func colorSingleMarkupTag(
        in result: inout AttributedString,
        fullText: String,
        tagStart: String.Index,
        tagEnd: String.Index,
        palette: SwiftBodyPalette
    ) {
        let tagSlice = fullText[tagStart...tagEnd]
        if tagSlice.hasPrefix("<!--") {
            if let r = Range(tagStart...tagEnd, in: result) {
                result[r].foregroundColor = palette.comment
            }
            return
        }
        if tagSlice.uppercased().hasPrefix("<!DOCTYPE") {
            if let r = Range(tagStart...tagEnd, in: result) {
                result[r].foregroundColor = palette.doctype
            }
            return
        }

        var cursor = tagStart
        // '<' and optional '/' or '?'
        let bracketEnd = fullText.index(after: cursor)
        if let r = Range(cursor..<bracketEnd, in: result) {
            result[r].foregroundColor = palette.separator
        }
        cursor = bracketEnd
        if cursor <= tagEnd && (fullText[cursor] == "/" || fullText[cursor] == "?") {
            let next = fullText.index(after: cursor)
            if let r = Range(cursor..<next, in: result) {
                result[r].foregroundColor = palette.separator
            }
            cursor = next
        }

        // Tag name
        let nameStart = cursor
        while cursor <= tagEnd && !fullText[cursor].isWhitespace && fullText[cursor] != "/" && fullText[cursor] != ">" && fullText[cursor] != "?" {
            cursor = fullText.index(after: cursor)
        }
        if cursor > nameStart, let r = Range(nameStart..<cursor, in: result) {
            result[r].foregroundColor = palette.tag
        }

        // Attributes and closing bracket
        while cursor <= tagEnd {
            let ch = fullText[cursor]
            if ch.isWhitespace {
                cursor = fullText.index(after: cursor)
            } else if ch == ">" || ch == "/" || ch == "?" || ch == "=" {
                let next = fullText.index(after: cursor)
                if let r = Range(cursor..<next, in: result) {
                    result[r].foregroundColor = palette.separator
                }
                cursor = next
            } else if ch == "\"" || ch == "'" {
                let quote = ch
                let valStart = cursor
                cursor = fullText.index(after: cursor)
                while cursor <= tagEnd && fullText[cursor] != quote {
                    cursor = fullText.index(after: cursor)
                }
                if cursor <= tagEnd {
                    cursor = fullText.index(after: cursor)
                }
                if let r = Range(valStart..<cursor, in: result) {
                    result[r].foregroundColor = palette.attributeValue
                }
            } else {
                let attrStart = cursor
                while cursor <= tagEnd && !fullText[cursor].isWhitespace && fullText[cursor] != "=" && fullText[cursor] != "/" && fullText[cursor] != ">" && fullText[cursor] != "?" {
                    cursor = fullText.index(after: cursor)
                }
                if cursor > attrStart, let r = Range(attrStart..<cursor, in: result) {
                    result[r].foregroundColor = palette.attributeName
                } else {
                    cursor = fullText.index(after: cursor)
                }
            }
        }
    }
}

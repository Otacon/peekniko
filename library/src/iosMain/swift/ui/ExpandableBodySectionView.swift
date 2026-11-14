import Foundation
import SwiftUI
import UIKit
#if canImport(PeeknikoCore)
import PeeknikoCore
#elseif canImport(App)
import App
#endif

extension BodyContentType {
    var label: String {
        switch self {
        case .json: return "JSON"
        case .xml: return "XML"
        case .html: return "HTML"
        case .formUrlEncoded: return "FORM"
        case .plainText: return "TEXT"
        default: return "BODY"
        }
    }
}

struct ExpandableBodySectionView: View {
    let sectionTitle: String
    let badge: String?
    let contentType: BodyContentType
    let rawBody: String?
    let attributedBody: NSAttributedString?
    let emptyMessage: String
    let onToast: (String) -> Void

    @State private var isExpanded: Bool
    @State private var copied: Bool = false
    @State private var isSearchVisible: Bool = false
    @State private var searchQuery: String = ""
    @State private var currentMatchIndex: Int = 0
    @State private var preparedLines: [BodyLineItem] = []
    @State private var formattedFullText: String = ""

    init(
        sectionTitle: String,
        badge: String?,
        contentType: BodyContentType = .plainText,
        rawBody: String?,
        attributedBody: NSAttributedString? = nil,
        emptyMessage: String,
        initiallyExpanded: Bool = true,
        onToast: @escaping (String) -> Void
    ) {
        self.sectionTitle = sectionTitle
        self.badge = badge
        self.contentType = contentType
        self.rawBody = rawBody
        self.attributedBody = attributedBody
        self.emptyMessage = emptyMessage
        self._isExpanded = State(initialValue: initiallyExpanded)
        self.onToast = onToast
    }

    private var sourceKey: String {
        let attrLen = attributedBody?.length ?? 0
        let rawLen = rawBody?.count ?? 0
        let prefix = (rawBody ?? "").prefix(32)
        return "\(contentType.name)-\(attrLen)-\(rawLen)-\(prefix)"
    }

    private var isBodyEmpty: Bool {
        let attrEmpty = (attributedBody?.string.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true)
        let rawEmpty = (rawBody?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true)
        return attrEmpty && rawEmpty
    }

    private var matchingLineIndices: [Int] {
        let query = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return [] }
        return preparedLines.compactMap { item in
            item.text.localizedCaseInsensitiveContains(query) ? item.number : nil
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header Bar
            HStack(spacing: 10) {
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        isExpanded.toggle()
                    }
                } label: {
                    HStack(spacing: 6) {
                        Text(sectionTitle)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(.primary)

                        if let badge, !badge.isEmpty {
                            Text(badge)
                                .font(.system(size: 13))
                                .foregroundStyle(.secondary)
                        }

                        if !isBodyEmpty {
                            Text(contentType.label)
                                .font(.system(size: 10, weight: .semibold, design: .monospaced))
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.blue.opacity(0.12))
                                .foregroundColor(.blue)
                                .clipShape(RoundedRectangle(cornerRadius: 4))
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                if !isBodyEmpty {
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            if !isExpanded {
                                isExpanded = true
                            }
                            isSearchVisible.toggle()
                            if !isSearchVisible {
                                searchQuery = ""
                                currentMatchIndex = 0
                            }
                        }
                    } label: {
                        Image(systemName: isSearchVisible ? "magnifyingglass.circle.fill" : "magnifyingglass")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(isSearchVisible ? .blue : .secondary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Search in body")

                    Button {
                        copyEntireBody()
                    } label: {
                        Text(copied ? "Copied!" : "Copy")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(copied ? .green : .blue)
                    }
                    .buttonStyle(.plain)
                }

                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        isExpanded.toggle()
                    }
                } label: {
                    Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(.tertiary)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 14)
            .frame(height: 44)

            if isExpanded {
                Divider()

                if isBodyEmpty {
                    Text(emptyMessage)
                        .font(.system(size: 13).italic())
                        .foregroundStyle(.secondary)
                        .padding(14)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                } else {
                    ScrollViewReader { proxy in
                        VStack(alignment: .leading, spacing: 0) {
                            if isSearchVisible {
                                BodySearchBar(
                                    searchQuery: $searchQuery,
                                    matchCount: matchingLineIndices.count,
                                    currentMatchIndex: currentMatchIndex,
                                    onPrevious: {
                                        navigateMatch(delta: -1, proxy: proxy)
                                    },
                                    onNext: {
                                        navigateMatch(delta: 1, proxy: proxy)
                                    },
                                    onClear: {
                                        searchQuery = ""
                                        currentMatchIndex = 0
                                    }
                                )
                                .onChange(of: searchQuery) { _, _ in
                                    currentMatchIndex = 0
                                    if let firstLine = matchingLineIndices.first {
                                        withAnimation(.easeInOut(duration: 0.2)) {
                                            proxy.scrollTo(firstLine, anchor: .center)
                                        }
                                    }
                                }

                                Divider()
                            }

                            LazyBodyLinesView(
                                lines: preparedLines,
                                contentType: contentType,
                                searchQuery: searchQuery.trimmingCharacters(in: .whitespacesAndNewlines),
                                activeMatchLineNumber: activeMatchLineNumber
                            )
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: isExpanded ? .infinity : nil, alignment: .top)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color(uiColor: .separator), lineWidth: 0.5)
        )
        .task(id: sourceKey) {
            await prepareBodyLines()
        }
    }

    private var activeMatchLineNumber: Int? {
        let matches = matchingLineIndices
        guard !matches.isEmpty else { return nil }
        let safeIndex = min(max(0, currentMatchIndex), matches.count - 1)
        return matches[safeIndex]
    }

    private func navigateMatch(delta: Int, proxy: ScrollViewProxy) {
        let matches = matchingLineIndices
        guard !matches.isEmpty else { return }
        let count = matches.count
        currentMatchIndex = (currentMatchIndex + delta + count) % count
        let targetLine = matches[currentMatchIndex]
        withAnimation(.easeInOut(duration: 0.2)) {
            proxy.scrollTo(targetLine, anchor: .center)
        }
    }

    private func prepareBodyLines() async {
        let attrString = attributedBody?.string ?? ""
        let raw = rawBody ?? ""
        let detectedType = contentType
        if attrString.isEmpty && raw.isEmpty {
            preparedLines = []
            formattedFullText = ""
            return
        }

        let computed = await Self.computeBodyLines(
            attrString: attrString,
            raw: raw,
            contentType: detectedType
        )

        formattedFullText = computed.0
        preparedLines = computed.1
    }

    nonisolated private static func computeBodyLines(
        attrString: String,
        raw: String,
        contentType: BodyContentType
    ) async -> (String, [BodyLineItem]) {
        let baseText: String
        if !attrString.isEmpty {
            baseText = attrString
        } else {
            baseText = BodyFormatter.formatBody(raw, contentType: contentType)
        }

        let rawLines = baseText.components(separatedBy: "\n")
        var items: [BodyLineItem] = []
        items.reserveCapacity(rawLines.count)
        for (idx, lineText) in rawLines.enumerated() {
            items.append(BodyLineItem(number: idx + 1, text: lineText))
        }
        return (baseText, items)
    }

    private func copyEntireBody() {
        let text = !formattedFullText.isEmpty ? formattedFullText : (rawBody ?? "")
        guard !text.isEmpty else { return }
        UIPasteboard.general.string = text
        onToast("Body copied")
        copied = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            copied = false
        }
    }
}

struct BodyLineItem: Identifiable {
    let number: Int
    let text: String
    var id: Int { number }
}

private struct BodySearchBar: View {
    @Binding var searchQuery: String
    let matchCount: Int
    let currentMatchIndex: Int
    let onPrevious: () -> Void
    let onNext: () -> Void
    let onClear: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 13))
                .foregroundStyle(.secondary)

            TextField("Search in body…", text: $searchQuery)
                .font(.system(size: 13))
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)

            if !searchQuery.isEmpty {
                Text(matchCount > 0 ? "\(currentMatchIndex + 1)/\(matchCount)" : "0/0")
                    .font(.system(size: 12, design: .monospaced))
                    .foregroundColor(matchCount > 0 ? .secondary : .red)

                Button(action: onPrevious) {
                    Image(systemName: "chevron.up")
                        .font(.system(size: 12, weight: .semibold))
                        .frame(width: 24, height: 24)
                }
                .buttonStyle(.plain)
                .disabled(matchCount == 0)

                Button(action: onNext) {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 12, weight: .semibold))
                        .frame(width: 24, height: 24)
                }
                .buttonStyle(.plain)
                .disabled(matchCount == 0)

                Button(action: onClear) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 14))
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(uiColor: .tertiarySystemGroupedBackground))
    }
}

private struct LazyBodyLinesView: View {
    let lines: [BodyLineItem]
    let contentType: BodyContentType
    let searchQuery: String
    let activeMatchLineNumber: Int?

    private var gutterWidth: CGFloat {
        let digits = max(2, String(lines.count).count)
        return CGFloat(digits) * 8.5 + 16.0
    }

    var body: some View {
        ScrollView(.vertical, showsIndicators: true) {
            LazyVStack(alignment: .leading, spacing: 0) {
                ForEach(lines) { line in
                    BodySingleLineRow(
                        line: line,
                        contentType: contentType,
                        gutterWidth: gutterWidth,
                        searchQuery: searchQuery,
                        isActiveMatch: activeMatchLineNumber == line.number
                    )
                }
            }
            .padding(.vertical, 6)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

private struct BodySingleLineRow: View {
    let line: BodyLineItem
    let contentType: BodyContentType
    let gutterWidth: CGFloat
    let searchQuery: String
    let isActiveMatch: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            Text("\(line.number)")
                .font(.system(size: 12, design: .monospaced))
                .foregroundStyle(.secondary)
                .frame(width: gutterWidth, alignment: .trailing)
                .padding(.vertical, 1.5)
                .padding(.trailing, 8)
                .background(Color(uiColor: .tertiarySystemGroupedBackground))

            Divider()

            Text(highlightedLineAttributedString)
                .font(.system(size: 12.5, design: .monospaced))
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 1.5)
                .padding(.horizontal, 10)
        }
        .background(isActiveMatch ? Color.yellow.opacity(0.18) : Color.clear)
    }

    private var highlightedLineAttributedString: AttributedString {
        let displayLine = line.text.count > 4000 ? String(line.text.prefix(4000)) + "…" : line.text
        var attr = BodySyntaxHighlighter.highlightLine(displayLine, contentType: contentType)
        BodySyntaxHighlighter.applySearchHighlights(
            to: &attr,
            lineText: displayLine,
            searchQuery: searchQuery,
            isActiveMatch: isActiveMatch
        )
        return attr
    }
}

import Foundation
import SwiftUI
import UIKit
#if canImport(PeeknikoCore)
import PeeknikoCore
#elseif canImport(App)
import App
#endif

struct ExpandableHeadersView: View {
    let sectionTitle: String
    let headers: [NetworkHeader]
    let onToast: (String) -> Void

    @State private var isExpanded: Bool
    @State private var copiedAll: Bool = false
    @State private var highlightedIndex: Int?

    init(
        sectionTitle: String,
        headers: [NetworkHeader],
        initiallyExpanded: Bool = false,
        onToast: @escaping (String) -> Void
    ) {
        self.sectionTitle = sectionTitle
        self.headers = headers
        self._isExpanded = State(initialValue: initiallyExpanded)
        self.onToast = onToast
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        isExpanded.toggle()
                    }
                } label: {
                    HStack(spacing: 6) {
                        Text(sectionTitle)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(.primary)

                        if !headers.isEmpty {
                            Text("(\(headers.count))")
                                .font(.system(size: 13))
                                .foregroundStyle(.secondary)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                if !headers.isEmpty {
                    Button {
                        copyAllHeaders()
                    } label: {
                        Text(copiedAll ? "Copied!" : "Copy All")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(copiedAll ? .green : .blue)
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

                if headers.isEmpty {
                    Text("No headers")
                        .font(.system(size: 13).italic())
                        .foregroundStyle(.secondary)
                        .padding(14)
                } else {
                    ViewThatFits(in: .vertical) {
                        headersListContent

                        ScrollView(.vertical, showsIndicators: true) {
                            headersListContent
                        }
                        .frame(maxHeight: 220)
                    }
                }
            }
        }
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color(uiColor: .separator), lineWidth: 0.5)
        )
    }

    private var headersListContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(headers.enumerated()), id: \.offset) { index, header in
                Button {
                    copySingleHeader(header, index: index)
                } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(header.name)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(.primary)

                            Spacer()

                            Image(systemName: "doc.on.doc")
                                .font(.system(size: 12))
                                .foregroundStyle(.tertiary)
                        }

                        Text(header.value)
                            .font(.system(size: 12, design: .monospaced))
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(highlightedIndex == index ? Color(uiColor: .systemGray4) : Color.clear)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                if index < headers.count - 1 {
                    Divider()
                        .padding(.leading, 14)
                }
            }
        }
    }

    private func copyAllHeaders() {
        guard !headers.isEmpty else { return }
        let allText = headers.map { "\($0.name): \($0.value)" }.joined(separator: "\n")
        UIPasteboard.general.string = allText
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        onToast("All headers copied")
        copiedAll = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            copiedAll = false
        }
    }

    private func copySingleHeader(_ header: NetworkHeader, index: Int) {
        let lineText = "\(header.name): \(header.value)"
        UIPasteboard.general.string = lineText
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        onToast("\(header.name) copied")
        withAnimation(.easeInOut(duration: 0.12)) {
            highlightedIndex = index
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
            withAnimation(.easeInOut(duration: 0.2)) {
                if highlightedIndex == index {
                    highlightedIndex = nil
                }
            }
        }
    }
}

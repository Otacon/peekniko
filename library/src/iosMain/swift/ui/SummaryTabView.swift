import Foundation
import SwiftUI
#if canImport(PeeknikoCore)
import PeeknikoCore
#elseif canImport(App)
import App
#endif

struct SummaryTabView: View {
    let detail: PeeknikoCallDetailItem?
    let onToast: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if let detail {
                    OverviewHighlightCard(detail: detail)

                    ExpandableHeadersView(
                        sectionTitle: "OVERVIEW",
                        headers: detail.overviewHeaders,
                        initiallyExpanded: true,
                        onToast: onToast
                    )
                } else {
                    Text("Transaction removed")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
    }
}

private struct OverviewHighlightCard: View {
    let detail: PeeknikoCallDetailItem

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Text(detail.method.isEmpty ? "CALL" : detail.method)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(methodColor(detail.method))
                    .clipShape(RoundedRectangle(cornerRadius: 6))

                Text(detail.statusBadge)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(statusColor)
                    .clipShape(RoundedRectangle(cornerRadius: 6))

                Spacer()
            }

            Text(detail.fullUrl)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(.primary)
                .textSelection(.enabled)

            Text("Duration: \(detail.durationText)")
                .font(.system(size: 13))
                .foregroundStyle(.secondary)

            Text("Request: \(detail.requestSizeText) · Response: \(detail.responseSizeText)")
                .font(.system(size: 13))
                .foregroundStyle(.secondary)
        }
        .padding(14)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(uiColor: .separator), lineWidth: 0.5)
        )
    }

    private var statusColor: Color {
        if detail.exceptionMessage != nil {
            return Color(red: 198.0 / 255.0, green: 40.0 / 255.0, blue: 40.0 / 255.0)
        }
        if let code = detail.statusCode?.intValue {
            switch code {
            case 200...299:
                return Color(red: 46.0 / 255.0, green: 125.0 / 255.0, blue: 50.0 / 255.0)
            case 300...399:
                return Color(red: 21.0 / 255.0, green: 101.0 / 255.0, blue: 192.0 / 255.0)
            case 400...499:
                return Color(red: 239.0 / 255.0, green: 108.0 / 255.0, blue: 0.0 / 255.0)
            case 500...:
                return Color(red: 198.0 / 255.0, green: 40.0 / 255.0, blue: 40.0 / 255.0)
            default:
                return .gray
            }
        }
        return .gray
    }

    private func methodColor(_ method: String) -> Color {
        switch method {
        case "GET":
            return Color(red: 46.0 / 255.0, green: 125.0 / 255.0, blue: 50.0 / 255.0)
        case "POST":
            return Color(red: 21.0 / 255.0, green: 101.0 / 255.0, blue: 192.0 / 255.0)
        case "PUT", "PATCH":
            return Color(red: 239.0 / 255.0, green: 108.0 / 255.0, blue: 0.0 / 255.0)
        case "DELETE":
            return Color(red: 198.0 / 255.0, green: 40.0 / 255.0, blue: 40.0 / 255.0)
        default:
            return .gray
        }
    }
}

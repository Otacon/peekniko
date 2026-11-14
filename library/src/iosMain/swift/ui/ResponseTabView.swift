import Foundation
import SwiftUI
#if canImport(PeeknikoCore)
import PeeknikoCore
#elseif canImport(App)
import App
#endif

struct ResponseTabView: View {
    let detail: PeeknikoCallDetailItem?
    let onToast: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            ExpandableHeadersView(
                sectionTitle: "HEADERS",
                headers: detail?.responseHeaders ?? [],
                initiallyExpanded: false,
                onToast: onToast
            )

            ExpandableBodySectionView(
                sectionTitle: "BODY",
                badge: detail?.responseBadge,
                contentType: detail?.responseContentType ?? .plainText,
                rawBody: detail?.rawResponseBody,
                emptyMessage: "No response body",
                initiallyExpanded: true,
                onToast: onToast
            )
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(Color(uiColor: .systemGroupedBackground))
    }
}

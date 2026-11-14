import Foundation
import SwiftUI
#if canImport(PeeknikoCore)
import PeeknikoCore
#elseif canImport(App)
import App
#endif

struct RequestTabView: View {
    let detail: PeeknikoCallDetailItem?
    let onToast: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            ExpandableHeadersView(
                sectionTitle: "HEADERS",
                headers: detail?.requestHeaders ?? [],
                initiallyExpanded: false,
                onToast: onToast
            )

            ExpandableBodySectionView(
                sectionTitle: "BODY",
                badge: detail?.requestBadge,
                contentType: detail?.requestContentType ?? .plainText,
                rawBody: detail?.rawRequestBody,
                emptyMessage: "No request body",
                initiallyExpanded: true,
                onToast: onToast
            )
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(Color(uiColor: .systemGroupedBackground))
    }
}

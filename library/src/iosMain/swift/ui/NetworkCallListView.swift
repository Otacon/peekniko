import Foundation
import SwiftUI
import UIKit
#if canImport(PeeknikoCore)
import PeeknikoCore
#elseif canImport(App)
import App
#endif

@MainActor
final class NetworkCallListViewModel: ObservableObject {
    @Published var items: [PeeknikoCallRowItem] = []
    @Published var searchText: String = "" {
        didSet {
            if searchText != oldValue {
                controller.setSearchQuery(query: searchText)
            }
        }
    }
    @Published var selectedMethod: NetworkMethod? = nil
    @Published var selectedStatus: CallStatusFilter? = nil
    @Published var totalCount: Int = 0
    @Published var filteredCount: Int = 0

    var hasActiveFilter: Bool {
        !searchText.trimmingCharacters(in: .whitespaces).isEmpty || selectedMethod != nil || selectedStatus != nil
    }

    private let controller: PeeknikoCallsController
    private var token: PeeknikoObservationToken?

    init() {
        let ctrl = PeeknikoIosUiBridge.shared.createCallsController()
        self.controller = ctrl
        self.token = ctrl.observeState { [weak self] state in
            self?.items = state.calls
            self?.totalCount = Int(state.totalCount)
            self?.filteredCount = Int(state.filteredCount)
        }
    }

    func selectMethod(_ method: NetworkMethod?) {
        selectedMethod = method
        controller.setMethodFilter(method: method)
    }

    func selectStatus(_ status: CallStatusFilter) {
        selectedStatus = status
        controller.setStatusFilter(status: status)
    }

    func clearCalls() {
        controller.clearCalls()
    }

    func resetFilters() {
        searchText = ""
        selectedMethod = nil
        selectedStatus = nil
        controller.clearFilters()
    }

    deinit {
        token?.cancel()
        controller.close()
    }
}

struct NetworkCallListView: View {
    @ObservedObject var viewModel: NetworkCallListViewModel
    @Binding var selectedCallId: String?
    @State private var showClearConfirmation = false

    private var methods: [(label: String, value: NetworkMethod?)] {
        [("All Methods", nil)] + NetworkMethod.entries.filter { $0 != .unknown }.map { ($0.name, $0) }
    }

    private var statuses: [(label: String, value: CallStatusFilter)] {
        let labels = ["Success (2xx)", "Non-success (non-2xx)", "Request exceptions"]
        return CallStatusFilter.entries.enumerated().map { index, status in
            (labels[index], status)
        }
    }

    var body: some View {
        Group {
            if viewModel.items.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: viewModel.hasActiveFilter ? "line.3.horizontal.decrease.circle" : "network")
                        .font(.system(size: 40))
                        .foregroundStyle(.secondary)
                    Text(viewModel.totalCount == 0 ? "No network calls recorded yet" : "No calls match current filters")
                        .font(.headline)
                        .foregroundStyle(.secondary)
                    if viewModel.hasActiveFilter {
                        Button("Reset Filters") {
                            viewModel.resetFilters()
                        }
                        .buttonStyle(.bordered)
                        .padding(.top, 4)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(viewModel.items, id: \.id) { item in
                    if UIDevice.current.userInterfaceIdiom == .pad {
                        Button {
                            selectedCallId = item.id
                        } label: {
                            NetworkCallRowView(item: item)
                        }
                        .buttonStyle(.plain)
                    } else {
                        NavigationLink(value: item.id) {
                            NetworkCallRowView(item: item)
                        }
                    }
                }
                .listStyle(.plain)
            }
        }
        .searchable(text: $viewModel.searchText, prompt: "Search URL...")
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Menu {
                    Menu {
                        ForEach(methods.indices, id: \.self) { index in
                            let method = methods[index]
                            Button {
                                viewModel.selectMethod(method.value)
                            } label: {
                                HStack {
                                    Text(method.label)
                                    if viewModel.selectedMethod == method.value {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        Label(
                            "Method: \(viewModel.selectedMethod?.name ?? "All")",
                            systemImage: "network"
                        )
                    }

                    Menu {
                        ForEach(statuses.indices, id: \.self) { index in
                            let status = statuses[index]
                            Button {
                                viewModel.selectStatus(status.value)
                            } label: {
                                HStack {
                                    Text(status.label)
                                    if viewModel.selectedStatus == status.value {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        Label(
                            "Status: \(statuses.first(where: { $0.value == viewModel.selectedStatus })?.label ?? "All")",
                            systemImage: "number"
                        )
                    }

                    if viewModel.hasActiveFilter {
                        Divider()
                        Button(role: .destructive) {
                            viewModel.resetFilters()
                        } label: {
                            Label("Reset Filters", systemImage: "arrow.counterclockwise")
                        }
                    }
                } label: {
                    Image(systemName: viewModel.hasActiveFilter ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                }
            }

            ToolbarItem(placement: .topBarTrailing) {
                Button(role: .destructive) {
                    showClearConfirmation = true
                } label: {
                    Image(systemName: "trash")
                }
                .disabled(viewModel.totalCount == 0)
            }
        }
        .confirmationDialog("Clear all recorded calls?", isPresented: $showClearConfirmation, titleVisibility: .visible) {
            Button("Clear All", role: .destructive) {
                viewModel.clearCalls()
            }
            Button("Cancel", role: .cancel) {}
        }
    }
}

struct NetworkCallRowView: View {
    let item: PeeknikoCallRowItem

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Text(item.method)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(Color(red: 38.0 / 255.0, green: 50.0 / 255.0, blue: 56.0 / 255.0))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(methodChipColor(item.method))
                    .clipShape(RoundedRectangle(cornerRadius: 5))

                Text(item.urlPath)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)

                Spacer(minLength: 4)

                Text(item.statusBadge)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(statusForegroundColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(statusBackgroundColor)
                    .clipShape(RoundedRectangle(cornerRadius: 5))
            }

            Text(item.subtitle)
                .font(.system(size: 12))
                .foregroundColor(item.exceptionMessage != nil ? Color(red: 176.0 / 255.0, green: 0, blue: 32.0 / 255.0) : .secondary)
                .lineLimit(1)
        }
        .padding(.vertical, 4)
    }

    private var statusForegroundColor: Color {
        if item.exceptionMessage != nil {
            return Color(red: 136.0 / 255.0, green: 14.0 / 255.0, blue: 79.0 / 255.0)
        }
        return Color(red: 38.0 / 255.0, green: 50.0 / 255.0, blue: 56.0 / 255.0)
    }

    private var statusBackgroundColor: Color {
        if item.exceptionMessage != nil {
            return Color(red: 248.0 / 255.0, green: 187.0 / 255.0, blue: 208.0 / 255.0)
        }
        if let code = item.responseCode?.intValue {
            switch code / 100 {
            case 2:
                return Color(red: 165.0 / 255.0, green: 214.0 / 255.0, blue: 167.0 / 255.0)
            case 3:
                return Color(red: 144.0 / 255.0, green: 202.0 / 255.0, blue: 249.0 / 255.0)
            case 4:
                return Color(red: 255.0 / 255.0, green: 204.0 / 255.0, blue: 128.0 / 255.0)
            case 5:
                return Color(red: 239.0 / 255.0, green: 154.0 / 255.0, blue: 154.0 / 255.0)
            default:
                return Color(red: 179.0 / 255.0, green: 157.0 / 255.0, blue: 219.0 / 255.0)
            }
        }
        return Color(red: 238.0 / 255.0, green: 238.0 / 255.0, blue: 238.0 / 255.0)
    }

    private func methodChipColor(_ method: String) -> Color {
        switch method {
        case "GET":
            return Color(red: 165.0 / 255.0, green: 214.0 / 255.0, blue: 167.0 / 255.0)
        case "POST":
            return Color(red: 144.0 / 255.0, green: 202.0 / 255.0, blue: 249.0 / 255.0)
        case "PUT", "PATCH":
            return Color(red: 255.0 / 255.0, green: 204.0 / 255.0, blue: 128.0 / 255.0)
        case "DELETE":
            return Color(red: 239.0 / 255.0, green: 154.0 / 255.0, blue: 154.0 / 255.0)
        case "OPTIONS":
            return Color(red: 128.0 / 255.0, green: 203.0 / 255.0, blue: 196.0 / 255.0)
        default:
            return Color(red: 179.0 / 255.0, green: 157.0 / 255.0, blue: 219.0 / 255.0)
        }
    }
}

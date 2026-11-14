import Foundation
import SwiftUI
import Peekniko
import Observation

@main
struct PeeknikoApp: App {
    init() {
        Peekniko.shared.configure { configuration in
            configuration.sqlDelightStorage(name: "peekniko.db")
        }
        precondition(
            URLProtocol.registerClass(Peekniko.shared.urlProtocol),
            "Failed to register Peekniko URLProtocol",
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

private struct ContentView: View {
    @State private var isShowingLibrary = false
    @State private var viewModel = SampleViewViewModel()
    
    var body: some View {
        VStack(spacing: 16) {
            Button {
                viewModel.launchKtor()
            } label: {
                Text("KTOR")
            }
            .buttonStyle(.borderedProminent)

            Button {
                viewModel.launchURLSession()
            } label: {
                Text("URLSession")
            }
            .buttonStyle(.borderedProminent)
            
            Button {
                isShowingLibrary.toggle()
            } label: {
                Text("Open inspector")
            }
            .buttonStyle(.borderedProminent)
        }
        .ignoresSafeArea()
        .sheet(isPresented: $isShowingLibrary) {
           PeeknikoScreen()
        }
    }
}


@Observable
final class SampleViewViewModel {
    
    @ObservationIgnored
    let commonViewModel = SampleViewModelBridge()
    
    var isLoading = false
    var showUrlSessionButton = true
    
    init() {
        commonViewModel.observeState { [weak self] state in
            guard let self else { return }
            self.isLoading = state.isLoading
            self.showUrlSessionButton = state.showUrlSessionButton
        }
    }
    
    func launchKtor() {
        commonViewModel.launchKtor()
    }
    
    func launchURLSession() {
        commonViewModel.launchUrlSession()
    }
    
    deinit {
        commonViewModel.clear()
    }
}


#if DEBUG
#Preview {
    ContentView()
}
#endif

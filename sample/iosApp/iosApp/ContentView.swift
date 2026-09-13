import SwiftUI
import InkletSample

struct ComposeView: UIViewControllerRepresentable {
    var onDarkChanged: (Bool) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(onDarkChanged: { dark in
            onDarkChanged(dark.boolValue)
        })
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var dark = false

    var body: some View {
        ComposeView(onDarkChanged: { dark = $0 })
            .ignoresSafeArea(edges: .all)
            .preferredColorScheme(dark ? .dark : .light)
    }
}

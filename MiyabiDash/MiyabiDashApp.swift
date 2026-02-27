import SwiftUI

@main
struct MiyabiDashApp: App {
    @StateObject private var apiService = OpenClawAPIService()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(apiService)
        }
    }
}

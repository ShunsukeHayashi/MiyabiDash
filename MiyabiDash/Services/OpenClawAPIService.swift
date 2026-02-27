import Foundation
import Combine

class OpenClawAPIService: ObservableObject {
    @Published var status: OpenClawStatus?
    @Published var isLoading = false
    @Published var error: String?
    @Published var lastUpdated: Date?
    
    private var timer: Timer?
    
    // Default to Tailscale IP - configurable in Settings
    @AppStorage("apiEndpoint") var apiEndpoint = "http://100.86.157.40:18795/status"
    @AppStorage("refreshInterval") var refreshInterval: TimeInterval = 30
    
    init() {
        startAutoRefresh()
    }
    
    func fetchStatus() async {
        await MainActor.run { isLoading = true; error = nil }
        
        guard let url = URL(string: apiEndpoint) else {
            await MainActor.run { error = "Invalid URL"; isLoading = false }
            return
        }
        
        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            
            guard let httpResponse = response as? HTTPURLResponse,
                  httpResponse.statusCode == 200 else {
                await MainActor.run { error = "Server error"; isLoading = false }
                return
            }
            
            let decoded = try JSONDecoder().decode(OpenClawStatus.self, from: data)
            
            await MainActor.run {
                self.status = decoded
                self.lastUpdated = Date()
                self.isLoading = false
            }
        } catch {
            await MainActor.run {
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }
    
    func startAutoRefresh() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: refreshInterval, repeats: true) { [weak self] _ in
            Task { await self?.fetchStatus() }
        }
        Task { await fetchStatus() }
    }
    
    func stopAutoRefresh() {
        timer?.invalidate()
        timer = nil
    }
}

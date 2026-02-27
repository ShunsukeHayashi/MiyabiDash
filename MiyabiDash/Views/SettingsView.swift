import SwiftUI

struct SettingsView: View {
    @AppStorage("apiEndpoint") var apiEndpoint = "http://100.86.157.40:18795/status"
    @AppStorage("refreshInterval") var refreshInterval: TimeInterval = 30
    @EnvironmentObject var api: OpenClawAPIService
    @State private var testResult: String?
    
    var body: some View {
        Form {
            Section("API Configuration") {
                TextField("Endpoint URL", text: $apiEndpoint)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                
                Picker("Refresh Interval", selection: $refreshInterval) {
                    Text("10 seconds").tag(TimeInterval(10))
                    Text("30 seconds").tag(TimeInterval(30))
                    Text("1 minute").tag(TimeInterval(60))
                    Text("5 minutes").tag(TimeInterval(300))
                }
                
                Button("Test Connection") {
                    Task {
                        await api.fetchStatus()
                        testResult = api.status != nil ? "✅ Connected" : "❌ \(api.error ?? "Failed")"
                    }
                }
                
                if let result = testResult {
                    Text(result)
                        .font(.caption)
                        .foregroundStyle(result.hasPrefix("✅") ? .green : .red)
                }
            }
            
            Section("About") {
                LabeledContent("App", value: "MiyabiDash 🌸")
                LabeledContent("Version", value: "1.0.0")
                LabeledContent("Host", value: api.status?.host ?? "—")
                LabeledContent("API Version", value: api.status?.version ?? "—")
            }
        }
        .navigationTitle("Settings")
    }
}

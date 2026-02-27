import SwiftUI

struct ContentView: View {
    @EnvironmentObject var api: OpenClawAPIService
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Header
                    HeaderView()
                    
                    // Status Cards
                    if let status = api.status {
                        StatusCardsView(status: status)
                        AgentListView(agents: status.agentList)
                        StatsView(status: status)
                    } else if api.isLoading {
                        ProgressView("Connecting...")
                            .padding(40)
                    } else if let error = api.error {
                        ErrorView(message: error) {
                            Task { await api.fetchStatus() }
                        }
                    }
                }
                .padding()
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("🌸 Miyabi")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink(destination: SettingsView()) {
                        Image(systemName: "gearshape")
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        Task { await api.fetchStatus() }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .refreshable {
                await api.fetchStatus()
            }
        }
    }
}

// MARK: - Header

struct HeaderView: View {
    var body: some View {
        VStack(spacing: 4) {
            Text("OpenClaw Dashboard")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}

// MARK: - Status Cards

struct StatusCardsView: View {
    let status: OpenClawStatus
    
    var body: some View {
        HStack(spacing: 12) {
            StatusCard(
                title: "Gateway",
                value: status.gatewayStatus,
                icon: status.isGatewayOnline ? "checkmark.circle.fill" : "xmark.circle.fill",
                color: status.isGatewayOnline ? .green : .red,
                subtitle: status.gatewayLatencyMs.map { "\($0)ms" }
            )
            
            StatusCard(
                title: "Telegram",
                value: status.telegramStatus,
                icon: status.isTelegramOnline ? "checkmark.circle.fill" : "xmark.circle.fill",
                color: status.isTelegramOnline ? .green : .red
            )
        }
    }
}

struct StatusCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    var subtitle: String? = nil
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .foregroundStyle(color)
                Text(title)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            
            Text(value)
                .font(.title3)
                .fontWeight(.semibold)
            
            if let subtitle {
                Text(subtitle)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - Agent List

struct AgentListView: View {
    let agents: [AgentInfo]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Agents (\(agents.count))")
                .font(.headline)
            
            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 8) {
                ForEach(agents) { agent in
                    AgentCard(agent: agent)
                }
            }
        }
    }
}

struct AgentCard: View {
    let agent: AgentInfo
    
    var body: some View {
        HStack {
            Circle()
                .fill(agent.isHeartbeatEnabled ? .green : .gray.opacity(0.3))
                .frame(width: 8, height: 8)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(agent.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                if agent.isHeartbeatEnabled {
                    Text("⏱ \(agent.heartbeat)")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            
            Spacer()
        }
        .padding(10)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}

// MARK: - Stats

struct StatsView: View {
    let status: OpenClawStatus
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Stats")
                .font(.headline)
            
            HStack(spacing: 12) {
                StatItem(label: "Sessions", value: "\(status.sessions)")
                StatItem(label: "Memory", value: "\(status.memoryChunks)")
                StatItem(label: "Tasks", value: "\(status.heartbeatTasks)")
            }
            
            if let time = status.updatedTime.isEmpty ? nil : status.updatedTime {
                Text("Last updated: \(time) JST")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
    }
}

struct StatItem: View {
    let label: String
    let value: String
    
    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundStyle(.primary)
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Error

struct ErrorView: View {
    let message: String
    let retry: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "wifi.exclamationmark")
                .font(.largeTitle)
                .foregroundStyle(.red)
            
            Text("Connection Error")
                .font(.headline)
            
            Text(message)
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            
            Button("Retry", action: retry)
                .buttonStyle(.borderedProminent)
        }
        .padding(40)
    }
}

import WidgetKit
import SwiftUI

// MARK: - Timeline Provider

struct MiyabiProvider: TimelineProvider {
    func placeholder(in context: Context) -> MiyabiEntry {
        MiyabiEntry(date: Date(), status: .placeholder)
    }
    
    func getSnapshot(in context: Context, completion: @escaping (MiyabiEntry) -> Void) {
        Task {
            let status = await fetchStatus()
            completion(MiyabiEntry(date: Date(), status: status))
        }
    }
    
    func getTimeline(in context: Context, completion: @escaping (Timeline<MiyabiEntry>) -> Void) {
        Task {
            let status = await fetchStatus()
            let entry = MiyabiEntry(date: Date(), status: status)
            let nextUpdate = Calendar.current.date(byAdding: .minute, value: 15, to: Date())!
            let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))
            completion(timeline)
        }
    }
    
    private func fetchStatus() async -> WidgetStatus {
        let endpoint = UserDefaults(suiteName: "group.com.miyabi.dash")?.string(forKey: "apiEndpoint")
            ?? "http://100.86.157.40:18795/status"
        
        guard let url = URL(string: endpoint) else { return .offline }
        
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let decoded = try JSONDecoder().decode(OpenClawStatus.self, from: data)
            return WidgetStatus(
                gatewayOnline: decoded.isGatewayOnline,
                telegramOnline: decoded.isTelegramOnline,
                agents: decoded.agents,
                sessions: decoded.sessions,
                latencyMs: decoded.gatewayLatencyMs,
                updatedTime: decoded.updatedTime
            )
        } catch {
            return .offline
        }
    }
}

// MARK: - Entry

struct MiyabiEntry: TimelineEntry {
    let date: Date
    let status: WidgetStatus
}

struct WidgetStatus {
    let gatewayOnline: Bool
    let telegramOnline: Bool
    let agents: Int
    let sessions: Int
    let latencyMs: Int?
    let updatedTime: String
    
    static let placeholder = WidgetStatus(
        gatewayOnline: true, telegramOnline: true,
        agents: 11, sessions: 20, latencyMs: 28, updatedTime: "20:00"
    )
    
    static let offline = WidgetStatus(
        gatewayOnline: false, telegramOnline: false,
        agents: 0, sessions: 0, latencyMs: nil, updatedTime: "--:--"
    )
}

// MARK: - Widget Views

struct MiyabiWidgetSmallView: View {
    let status: WidgetStatus
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("🌸 Miyabi")
                    .font(.caption)
                    .fontWeight(.bold)
                Spacer()
            }
            
            HStack(spacing: 4) {
                Circle()
                    .fill(status.gatewayOnline ? .green : .red)
                    .frame(width: 8, height: 8)
                Text("GW")
                    .font(.caption2)
                
                Circle()
                    .fill(status.telegramOnline ? .green : .red)
                    .frame(width: 8, height: 8)
                Text("TG")
                    .font(.caption2)
            }
            
            Spacer()
            
            HStack {
                VStack(alignment: .leading) {
                    Text("\(status.agents)")
                        .font(.title2)
                        .fontWeight(.bold)
                    Text("Agents")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                
                Spacer()
                
                VStack(alignment: .trailing) {
                    Text("\(status.sessions)")
                        .font(.title2)
                        .fontWeight(.bold)
                    Text("Sessions")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            
            Text(status.updatedTime)
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
        .containerBackground(.fill.tertiary, for: .widget)
    }
}

struct MiyabiWidgetMediumView: View {
    let status: WidgetStatus
    
    var body: some View {
        HStack(spacing: 16) {
            // Left: Status
            VStack(alignment: .leading, spacing: 8) {
                Text("🌸 Miyabi")
                    .font(.subheadline)
                    .fontWeight(.bold)
                
                HStack(spacing: 6) {
                    StatusDot(online: status.gatewayOnline, label: "Gateway")
                    StatusDot(online: status.telegramOnline, label: "Telegram")
                }
                
                Spacer()
                
                if let ms = status.latencyMs {
                    Text("\(ms)ms latency")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            
            Divider()
            
            // Right: Numbers
            HStack(spacing: 20) {
                NumberColumn(value: status.agents, label: "Agents")
                NumberColumn(value: status.sessions, label: "Sessions")
            }
        }
        .containerBackground(.fill.tertiary, for: .widget)
    }
}

struct StatusDot: View {
    let online: Bool
    let label: String
    
    var body: some View {
        HStack(spacing: 3) {
            Circle()
                .fill(online ? .green : .red)
                .frame(width: 6, height: 6)
            Text(label)
                .font(.caption2)
        }
    }
}

struct NumberColumn: View {
    let value: Int
    let label: String
    
    var body: some View {
        VStack(spacing: 2) {
            Text("\(value)")
                .font(.title)
                .fontWeight(.bold)
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
}

// MARK: - Lock Screen Widget

struct MiyabiLockScreenView: View {
    let status: WidgetStatus
    
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: status.gatewayOnline ? "checkmark.circle" : "xmark.circle")
            Text("\(status.agents)A \(status.sessions)S")
                .font(.caption)
        }
    }
}

// MARK: - Widget Configuration

struct MiyabiDashWidget: Widget {
    let kind = "MiyabiDashWidget"
    
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: MiyabiProvider()) { entry in
            switch entry.status {
            default:
                MiyabiWidgetSmallView(status: entry.status)
            }
        }
        .configurationDisplayName("🌸 Miyabi Status")
        .description("OpenClaw gateway and agent status")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryInline, .accessoryCircular])
    }
}

@main
struct MiyabiWidgetBundle: WidgetBundle {
    var body: some Widget {
        MiyabiDashWidget()
    }
}

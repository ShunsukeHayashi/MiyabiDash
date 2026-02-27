import Foundation

struct OpenClawStatus: Codable {
    let summary: String
    let gateway: String
    let gatewayStatus: String
    let gatewayLatencyMs: Int?
    let telegram: String
    let telegramStatus: String
    let agents: Int
    let sessions: Int
    let memoryChunks: Int
    let updateAvailable: Bool
    let agentList: [AgentInfo]
    let heartbeatTasks: Int
    let updatedAt: String
    let updatedAtJST: String
    let updatedTime: String
    let host: String
    let version: String
    
    var isGatewayOnline: Bool { gateway == "🟢" }
    var isTelegramOnline: Bool { telegram == "🟢" }
}

struct AgentInfo: Codable, Identifiable {
    let id: String
    let name: String
    let heartbeat: String
    
    var isHeartbeatEnabled: Bool { heartbeat != "disabled" }
}

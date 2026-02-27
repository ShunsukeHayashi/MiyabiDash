package com.shunsukehayashi.miyabidash.data.models

data class OpenClawStatus(
    val status: String? = null,
    val healthy: Boolean? = null,
    val agents: List<AgentStatus> = emptyList(),
    val sessions: List<SessionStatus> = emptyList(),
    val memory: MemoryStatus? = null,
    val error: String? = null,
    val summary: String? = null,
    val gateway: String? = null,
    val gatewayStatus: String? = null,
    val gatewayLatencyMs: Int? = null,
    val telegram: String? = null,
    val telegramStatus: String? = null,
    val heartbeatTasks: Int? = null,
    val memoryChunks: Int? = null,
    val updateAvailable: Boolean? = null,
    val agentList: List<AgentStatus> = emptyList(),
    val agentCount: Int? = null,
    val sessionCount: Int? = null,
) {
    val displayStatus: String
        get() = status ?: summary ?: "unknown"

    val isHealthy: Boolean
        get() = healthy ?: (gateway == "🟢")

    val displayAgents: List<AgentStatus>
        get() = when {
            agentList.isNotEmpty() -> agentList
            agents.isNotEmpty() -> agents
            else -> emptyList()
        }

    val displayAgentCount: Int
        get() = when {
            displayAgents.isNotEmpty() -> displayAgents.size
            else -> (agentCount ?: 0)
        }

    val displaySessionCount: Int
        get() = when {
            sessions.isNotEmpty() -> sessions.size
            else -> (sessionCount ?: 0)
        }

    val isSyntheticResponse: Boolean
        get() = status == null && summary != null
}

data class AgentStatus(
    val id: String? = null,
    val name: String? = null,
    val status: String? = null,
    val lastSeen: String? = null
)

data class SessionStatus(
    val id: String? = null,
    val name: String? = null,
    val state: String? = null
)

data class MemoryStatus(
    val usedBytes: Long? = null,
    val totalBytes: Long? = null
) {
    val usagePercent: Double?
        get() = if (usedBytes != null && totalBytes != null && totalBytes > 0) {
            usedBytes.toDouble() / totalBytes.toDouble() * 100.0
        } else {
            null
        }
}

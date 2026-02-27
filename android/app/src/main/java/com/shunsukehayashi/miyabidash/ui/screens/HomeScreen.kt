package com.shunsukehayashi.miyabidash.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shunsukehayashi.miyabidash.data.models.AgentStatus
import com.shunsukehayashi.miyabidash.data.models.OpenClawStatus
import com.shunsukehayashi.miyabidash.ui.MiyabiDashboardUiState

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: MiyabiDashboardUiState,
    onRefresh: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        when (state) {
            MiyabiDashboardUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.padding(12.dp))
                    Text(
                        "読み込み中",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        is MiyabiDashboardUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text(
                        text = "エラー: ${state.message}",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.padding(16.dp))
                    Button(
                        onClick = onRefresh
                    ) {
                        Text("再試行", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            is MiyabiDashboardUiState.Success -> {
                StatusContent(
                    status = state.status,
                    onRefresh = onRefresh
                )
            }
        }
    }
}

@Composable
private fun StatusContent(
    status: OpenClawStatus,
    onRefresh: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "全体ステータス: ${status.displayStatus}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "健全性: ${status.isHealthy}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "エージェント数: ${status.displayAgentCount}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "セッション数: ${status.displaySessionCount}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (status.isSyntheticResponse) {
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text(
                            "注: ゲートウェイのダッシュボードHTMLを変換して表示しています",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("更新", color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.padding(12.dp))
        }

        if (status.displayAgents.isNotEmpty()) {
            item {
                Text(
                    "エージェント",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.padding(8.dp))
            }
            items(status.displayAgents, key = { it.id ?: it.hashCode().toString() }) { agent ->
                AgentRow(agent)
                Spacer(modifier = Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
private fun AgentRow(agent: AgentStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = agent.name ?: "unknown",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "ID: ${agent.id ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "状態: ${agent.status ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "最終確認: ${agent.lastSeen ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

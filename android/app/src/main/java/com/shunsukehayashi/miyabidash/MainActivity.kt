package com.shunsukehayashi.miyabidash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shunsukehayashi.miyabidash.data.repository.OpenClawRepository
import com.shunsukehayashi.miyabidash.data.settings.AppSettingValues
import com.shunsukehayashi.miyabidash.data.settings.AppSettings
import com.shunsukehayashi.miyabidash.ui.screens.HomeScreen
import com.shunsukehayashi.miyabidash.ui.screens.SettingsScreen
import com.shunsukehayashi.miyabidash.ui.theme.MiyabiDashTheme
import com.shunsukehayashi.miyabidash.ui.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = AppSettings(applicationContext)
        val repository = OpenClawRepository()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(repository, settings) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        setContent {
            MiyabiDashTheme {
                AppContent(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startPolling()
    }

    override fun onStop() {
        viewModel.stopPolling()
        super.onStop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settingsState.collectAsState(AppSettingValues())
    var showSettings by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("MiyabiDash") },
                    actions = {
                        Button(onClick = { showSettings = !showSettings }) {
                            Text(if (showSettings) "ダッシュボード" else "設定")
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (showSettings) {
                SettingsScreen(
                    modifier = androidx.compose.ui.Modifier.padding(innerPadding),
                    initial = settings,
                    onSave = {
                        viewModel.saveSettings(it)
                        showSettings = false
                    }
                )
            } else {
                HomeScreen(
                    modifier = androidx.compose.ui.Modifier.padding(innerPadding),
                    state = uiState,
                    onRefresh = { viewModel.refresh() }
                )
            }
        }
    }
}

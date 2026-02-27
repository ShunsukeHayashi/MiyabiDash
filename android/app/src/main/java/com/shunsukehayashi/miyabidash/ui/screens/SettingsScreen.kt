package com.shunsukehayashi.miyabidash.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shunsukehayashi.miyabidash.data.settings.AppSettingValues

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    initial: AppSettingValues,
    onSave: (AppSettingValues) -> Unit
) {
    var baseUrl by remember { mutableStateOf(initial.baseUrl) }
    var apiToken by remember { mutableStateOf(initial.apiToken) }
    var updateIntervalSeconds by remember { mutableLongStateOf(initial.updateIntervalSeconds) }

    Column(modifier = modifier.padding(16.dp)) {
        Text("設定", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(8.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(8.dp))

        OutlinedTextField(
            value = apiToken,
            onValueChange = { apiToken = it },
            label = { Text("API Token") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(8.dp))

        OutlinedTextField(
            value = updateIntervalSeconds.toString(),
            onValueChange = { value ->
                updateIntervalSeconds = value.toLongOrNull() ?: initial.updateIntervalSeconds
            },
            label = { Text("Update Interval (seconds)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.padding(16.dp))

        Button(
            onClick = {
                onSave(
                    AppSettingValues(
                        baseUrl = baseUrl.trim(),
                        apiToken = apiToken.trim(),
                        updateIntervalSeconds = updateIntervalSeconds.coerceAtLeast(5L)
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存")
        }
    }
}

package com.digitalwellbeing.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import com.digitalwellbeing.ui.CogniTrackTheme
import com.digitalwellbeing.ui.DashboardScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private val repository by lazy { AppModule.provideRepository(this) }
    private val dashboardViewModel by lazy {
        ViewModelProvider(this, DashboardViewModelFactory(repository))[DashboardViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

            CogniTrackTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    when (val state = uiState) {
                        DashboardUiState.Loading -> LoadingScreen()
                        DashboardUiState.MissingPermission -> PermissionScreen(
                            onOpenUsageAccess = {
                                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        )
                        is DashboardUiState.Ready -> DashboardScreen(
                            state = state.state,
                            statusLine = state.status,
                            onRefresh = dashboardViewModel::refresh
                        )
                        is DashboardUiState.Error -> ErrorScreen(
                            message = state.message,
                            onRetry = dashboardViewModel::refresh
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dashboardViewModel.refresh()
    }
}

@androidx.compose.runtime.Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(
            text = "Importing device activity…",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@androidx.compose.runtime.Composable
private fun PermissionScreen(
    onOpenUsageAccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Usage Access Needed", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "CogniTrack reads Android UsageStats to turn your phone activity into real timeline, intensity, and app-flow metrics.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
        )
        Button(onClick = onOpenUsageAccess, modifier = Modifier.fillMaxWidth()) {
            Text("Open Usage Access")
        }
    }
}

@androidx.compose.runtime.Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Import failed", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
        )
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Retry")
        }
    }
}

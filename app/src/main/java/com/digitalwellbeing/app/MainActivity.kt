package com.digitalwellbeing.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.digitalwellbeing.ui.CogniTrackTheme
import com.digitalwellbeing.ui.DashboardScreen
import com.digitalwellbeing.ui.sampleDashboardState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CogniTrackTheme {
                DashboardScreen(sampleDashboardState())
            }
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SonicFlowApp
import com.example.ui.viewmodel.SonicFlowViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val appContainer = (application as SonicFlowApplication).container
        setContent {
            val vm: SonicFlowViewModel = viewModel(factory = SonicFlowViewModel.Factory(appContainer))
            SonicFlowApp(viewModel = vm)
        }
    }
}

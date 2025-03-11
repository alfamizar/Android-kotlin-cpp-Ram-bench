package com.compucode.rambench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.compucode.rambench.presentation.ui.RAMBenchmarkApp
import com.compucode.rambench.presentation.viewmodels.BenchmarkViewModel
import com.compucode.rambench.ui.theme.RAMBenchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: BenchmarkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("benchmark")
        enableEdgeToEdge()
        setContent {
            RAMBenchTheme {
                RAMBenchmarkApp(viewModel)
            }
        }
    }
}
package com.compucode.rambench.presentation.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.compucode.rambench.presentation.viewmodels.BenchmarkViewModel

@Composable
fun RAMBenchmarkApp(viewModel: BenchmarkViewModel) {
    val view = LocalView.current
    LaunchedEffect(viewModel.keepScreenOn, viewModel.isRunning) {
        val window = view.context.getActivity()?.window
        if (viewModel.keepScreenOn && viewModel.isRunning) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val scrollState = remember { ScrollState(0) }
    val isEnabled = !viewModel.isRunning // Disable UI when running tests

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("RAM Speed Benchmark", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))

                var expandedImpl by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Implementation: ")
                    Button(
                        onClick = { expandedImpl = true },
                        enabled = isEnabled
                    ) {
                        Text(if (viewModel.useNative) "Native" else "Kotlin")
                    }
                    DropdownMenu(
                        expanded = expandedImpl,
                        onDismissRequest = { expandedImpl = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Native") },
                            onClick = {
                                viewModel.useNative = true
                                if (viewModel.selectedSize > 2048) viewModel.selectedSize = 256
                                expandedImpl = false
                            },
                            enabled = isEnabled
                        )
                        DropdownMenuItem(
                            text = { Text("Kotlin") },
                            onClick = {
                                viewModel.useNative = false
                                if (viewModel.selectedSize > 128) viewModel.selectedSize = 128
                                expandedImpl = false
                            },
                            enabled = isEnabled
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                var expandedSize by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Size (MB): ")
                    Button(
                        onClick = { expandedSize = true },
                        enabled = isEnabled
                    ) {
                        Text("${viewModel.selectedSize}")
                    }
                    DropdownMenu(
                        expanded = expandedSize,
                        onDismissRequest = { expandedSize = false }
                    ) {
                        viewModel.getAvailableSizes().forEach { size ->
                            DropdownMenuItem(
                                text = { Text("$size") },
                                onClick = { viewModel.selectedSize = size; expandedSize = false },
                                enabled = isEnabled
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Column {
                    Text("Iterations: ${viewModel.iterations}")
                    Slider(
                        value = viewModel.iterations.toFloat(),
                        onValueChange = { viewModel.iterations = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8,
                        enabled = isEnabled
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = viewModel.keepScreenOn,
                        onCheckedChange = { viewModel.keepScreenOn = it },
                        enabled = isEnabled
                    )
                    Text("Keep Screen On")
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.startBenchmark() },
                        enabled = isEnabled
                    ) {
                        Text("Start")
                    }
                    Button(
                        onClick = { viewModel.cancelBenchmark() },
                        enabled = viewModel.isRunning
                    ) {
                        Text("Cancel")
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (viewModel.isRunning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { viewModel.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(viewModel.currentTest)
                    }
                } else if (viewModel.readSpeed != 0.0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (viewModel.readSpeed < 0) {
                            Text("Error: Benchmark failed")
                        } else {
                            Text("Read: ${"%.2f".format(viewModel.readSpeed)} MB/s")
                            Spacer(Modifier.height(4.dp))
                            Text("Write: ${"%.2f".format(viewModel.writeSpeed)} MB/s")
                            Spacer(Modifier.height(4.dp))
                            Text("Copy: ${"%.2f".format(viewModel.copySpeed)} MB/s")
                        }
                    }
                }
            }
        }
    )
}

fun android.content.Context.getActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.getActivity()
    else -> null
}
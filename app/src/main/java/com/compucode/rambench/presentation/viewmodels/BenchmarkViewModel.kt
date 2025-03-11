package com.compucode.rambench.presentation.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compucode.rambench.domain.BenchmarkService
import com.compucode.rambench.domain.BenchmarkType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BenchmarkViewModel @Inject constructor(
    private val benchmarkService: BenchmarkService
) : ViewModel() {
    var readSpeed by mutableDoubleStateOf(0.0)
    var writeSpeed by mutableDoubleStateOf(0.0)
    var copySpeed by mutableDoubleStateOf(0.0)
    var isRunning by mutableStateOf(false)
    var useNative by mutableStateOf(true)
    var selectedSize by mutableIntStateOf(256)
    var iterations by mutableIntStateOf(5)
    var keepScreenOn by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var currentTest by mutableStateOf("")

    private var benchmarkJob: Job? = null

    fun startBenchmark() {
        if (isRunning) return
        isRunning = true
        benchmarkJob = viewModelScope.launch {
            try {
                val totalTests = 3f // Number of test types (read, write, copy)
                val testFraction = 1f / totalTests // 1/3 per test

                val (read, write, copy) = benchmarkService.runBenchmarks(
                    useNative = useNative,
                    sizeMb = selectedSize,
                    iterations = iterations,
                    onProgress = { test, iteration ->
                        currentTest = "$test (Iteration ${iteration + 1}/$iterations)"
                        when (test) {
                            BenchmarkType.READ -> {
                                progress = if (iteration == iterations - 1) {
                                    1f / totalTests // Exactly 1/3
                                } else {
                                    testFraction * (iteration + 1) / iterations
                                }
                            }
                            BenchmarkType.WRITE -> {
                                progress = if (iteration == iterations - 1) {
                                    2f / totalTests // Exactly 2/3
                                } else {
                                    (1f / totalTests) + (testFraction * (iteration + 1) / iterations)
                                }
                            }
                            BenchmarkType.COPY -> {
                                progress = if (iteration == iterations - 1) {
                                    3f / totalTests // Exactly 1.0
                                } else {
                                    (2f / totalTests) + (testFraction * (iteration + 1) / iterations)
                                }
                            }
                        }
                    }
                )
                readSpeed = read
                writeSpeed = write
                copySpeed = copy
            } catch (e: Exception) {
                readSpeed = -1.0
                writeSpeed = -1.0
                copySpeed = -1.0
            } finally {
                isRunning = false
                progress = 0f
                currentTest = ""
            }
        }
    }

    fun cancelBenchmark() {
        benchmarkJob?.cancel()
        isRunning = false
        progress = 0f
        currentTest = ""
    }

    fun getAvailableSizes(): List<Int> = if (useNative) {
        listOf(64, 128, 256, 512, 1024, 2048)
    } else {
        listOf(64, 128)
    }
}
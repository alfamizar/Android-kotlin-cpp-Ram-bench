package com.compucode.rambench.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.system.measureNanoTime

class BenchmarkService {
    private val benchmarkDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private external fun benchmarkRead(size: Int): Double
    private external fun benchmarkWrite(size: Int): Double
    private external fun benchmarkCopy(size: Int): Double

    suspend fun runBenchmarks(
        useNative: Boolean,
        sizeMb: Int,
        iterations: Int,
        onProgress: (BenchmarkType, Int) -> Unit
    ): Triple<Double, Double, Double> = withContext(Dispatchers.Default) {
        val sizeBytes = sizeMb * 1024 * 1024
        val totalSteps = iterations * 3
        var currentStep = 0

        // Run all read iterations
        var readTotal = 0.0
        repeat(iterations) { i ->
            onProgress(BenchmarkType.READ, i)
            readTotal += if (useNative) benchmarkRead(sizeBytes) else benchmarkReadKotlin(sizeBytes)
            currentStep++
            onProgress(BenchmarkType.READ, i) // Update progress after each iteration
        }

        // Run all write iterations
        var writeTotal = 0.0
        repeat(iterations) { i ->
            onProgress(BenchmarkType.WRITE, i)
            writeTotal += if (useNative) benchmarkWrite(sizeBytes) else benchmarkWriteKotlin(sizeBytes)
            currentStep++
            onProgress(BenchmarkType.WRITE, i)
        }

        // Run all copy iterations
        var copyTotal = 0.0
        repeat(iterations) { i ->
            onProgress(BenchmarkType.COPY, i)
            copyTotal += if (useNative) benchmarkCopy(sizeBytes) else benchmarkCopyKotlin(sizeBytes)
            currentStep++
            onProgress(BenchmarkType.COPY, i)
        }

        Triple(readTotal / iterations, writeTotal / iterations, copyTotal / iterations)
    }

    private fun benchmarkReadKotlin(size: Int): Double {
        val buffer = ByteBuffer.allocateDirect(size)
        for (i in 0 until size) buffer.put(i, 1)
        buffer.clear()
        var sum: Long = 0
        val timeNs = measureNanoTime {
            for (i in 0 until size) sum += buffer.get(i).toLong()
        }
        val elapsedSeconds = timeNs / 1_000_000_000.0
        return (size / (1024.0 * 1024.0)) / elapsedSeconds
    }

    private fun benchmarkWriteKotlin(size: Int): Double {
        val buffer = ByteBuffer.allocateDirect(size)
        val timeNs = measureNanoTime {
            for (i in 0 until size) buffer.put(i, (i % 128).toByte())
        }
        val elapsedSeconds = timeNs / 1_000_000_000.0
        return (size / (1024.0 * 1024.0)) / elapsedSeconds
    }

    private fun benchmarkCopyKotlin(size: Int): Double {
        val src = ByteBuffer.allocateDirect(size)
        val dst = ByteBuffer.allocateDirect(size)
        for (i in 0 until size) src.put(i, 1)
        src.clear()
        val timeNs = measureNanoTime {
            dst.put(src)
            src.clear()
            dst.clear()
        }
        val elapsedSeconds = timeNs / 1_000_000_000.0
        return (size / (1024.0 * 1024.0)) / elapsedSeconds
    }
}
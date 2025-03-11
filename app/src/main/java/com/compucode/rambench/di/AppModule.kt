package com.compucode.rambench.di

/**
 *
 *Created by Max on 23.02.2025
 *
 */
import com.compucode.rambench.domain.BenchmarkService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideBenchmarkService(): BenchmarkService = BenchmarkService()
}
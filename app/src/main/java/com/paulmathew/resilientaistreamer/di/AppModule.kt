package com.paulmathew.resilientaistreamer.di

import com.paulmathew.resilientaistreamer.data.ml.TextExtractorImpl
import com.paulmathew.resilientaistreamer.domain.repository.TextExtractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTextExtractor(
        implementation: TextExtractorImpl,
    ): TextExtractor
}
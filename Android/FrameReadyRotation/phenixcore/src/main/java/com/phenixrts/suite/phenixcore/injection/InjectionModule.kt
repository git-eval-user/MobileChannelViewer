/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.phenixcore.injection

import android.app.Application
import com.phenixrts.suite.phenixcore.debugmenu.common.FileWriterDebugTree
import com.phenixrts.suite.phenixcore.repositories.core.PhenixCoreRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal class InjectionModule(private val context: Application) {

    @Provides
    @Singleton
    fun provideFileWriterDebugTree() = FileWriterDebugTree(context)

    @Singleton
    @Provides
    internal fun providePhenixCoreRepository(debugTree: FileWriterDebugTree) = PhenixCoreRepository(context, debugTree)

}

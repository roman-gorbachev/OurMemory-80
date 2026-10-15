package com.gorman.ourmemoryapp.appcheck

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckProviders {
    fun factory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}

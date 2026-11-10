package com.developers.panapp

import android.app.Application
import com.developers.core.text.TextResources

class PanAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TextResources.initialize(this)
    }
}

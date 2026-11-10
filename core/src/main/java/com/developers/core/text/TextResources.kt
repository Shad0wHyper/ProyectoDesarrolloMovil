package com.developers.core.text

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Resolves text outside composition (callbacks, ViewModels and notifications).
 * Never changes the process locale or translates server values.
 */
object TextResources {
    private lateinit var applicationContext: Context

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun context(): Context {
        val base = applicationContext
        if (Build.VERSION.SDK_INT >= 33) return base
        val requested = AppCompatDelegate.getApplicationLocales()
        val locales = if (requested.isEmpty) {
            LocaleListCompat.wrap(Resources.getSystem().configuration.locales)
        } else requested
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocales(android.os.LocaleList.forLanguageTags(locales.toLanguageTags()))
        return base.createConfigurationContext(configuration)
    }
}

fun localizedText(@StringRes id: Int, vararg args: Any): String =
    if (args.isEmpty()) TextResources.context().getString(id)
    else TextResources.context().getString(id, *args)

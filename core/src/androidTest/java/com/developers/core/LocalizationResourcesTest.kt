package com.developers.core

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalizationResourcesTest {
    private fun context(tags: String) =
        InstrumentationRegistry.getInstrumentation().targetContext.let { base ->
            val configuration = Configuration(base.resources.configuration)
            configuration.setLocales(LocaleList.forLanguageTags(tags))
            base.createConfigurationContext(configuration)
        }

    @Test
    fun selectedLanguageWinsOverSystemFallback() {
        assertEquals("Language", context("en,es-MX").getString(R.string.language_title))
        assertEquals("Idioma", context("es-MX,en").getString(R.string.language_title))
        assertEquals("Voltar", context("pt-PT,es-MX").getString(R.string.pan_back))
    }

    @Test
    fun unsupportedLanguageFallsBackToEnglish() {
        assertEquals("Back", context("ja-JP").getString(R.string.pan_back))
    }

    @Test
    fun formattingPreservesServerValuesAndLineBreaks() {
        val name = "Bolillo / Pão 100%"
        assertEquals(
            "Olá, encomenda de $name:\n- Farinha: 12.5kg",
            context("pt-PT").getString(R.string.pan_ai_order_message, name, "12.5")
        )
        assertEquals("+12% vs yesterday", context("en").getString(R.string.pan_sales_trend))
    }

    @Test
    fun allChoicesAreAvailableInEveryLanguage() {
        for (tag in listOf("en", "es-MX", "pt-PT")) {
            val resources = context(tag)
            assertEquals("Español (México)", resources.getString(R.string.language_spanish_mexico))
            assertEquals("English", resources.getString(R.string.language_english))
            assertEquals("Português (Portugal)", resources.getString(R.string.language_portuguese_portugal))
        }
    }
}

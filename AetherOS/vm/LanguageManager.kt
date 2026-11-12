package com.excp.podroid.data.repository

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        const val LANGUAGE_AUTO = "auto"
        const val LANGUAGE_ZH = "zh"
        const val LANGUAGE_EN = "en"
        const val LANGUAGE_RU = "ru"
        private const val LANG_CACHE_FILE = "aether_lang_cache"

        fun getSavedLanguage(ctx: Context): String {
            return try {
                val file = File(ctx.filesDir, LANG_CACHE_FILE)
                val raw = if (file.exists()) file.readText().trim() else LANGUAGE_RU
                when (raw) {
                    LANGUAGE_ZH -> LANGUAGE_ZH
                    LANGUAGE_EN -> LANGUAGE_EN
                    else -> LANGUAGE_RU
                }
            } catch (_: Exception) {
                LANGUAGE_RU
            }
        }

        fun persistLanguage(ctx: Context, language: String) {
            runCatching { File(ctx.filesDir, LANG_CACHE_FILE).writeText(language) }
                .onFailure { android.util.Log.w("AetherLanguage", "Failed to save language", it) }
        }

        fun wrapContextForLocale(base: Context, language: String): Context {
            val locale = when (language) {
                LANGUAGE_ZH -> Locale("zh", "CN")
                LANGUAGE_EN -> Locale.ENGLISH
                else -> Locale("ru", "RU")
            }
            val config = Configuration(base.resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale)
                config.setLocales(android.os.LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            return base.createConfigurationContext(config)
        }
    }

    private fun getSystemLanguage(): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return when (locale?.language) {
            "zh" -> LANGUAGE_ZH
            "en" -> LANGUAGE_EN
            else -> LANGUAGE_RU
        }
    }

    val language: Flow<String> = settingsRepository.language.map { prefLanguage ->
        when (prefLanguage) {
            LANGUAGE_ZH -> LANGUAGE_ZH
            LANGUAGE_EN -> LANGUAGE_EN
            LANGUAGE_RU -> LANGUAGE_RU
            else -> getSystemLanguage().let { if (it == LANGUAGE_EN || it == LANGUAGE_ZH) it else LANGUAGE_RU }
        }
    }

    suspend fun setLanguage(language: String) {
        settingsRepository.setLanguage(language)
        persistLanguage(context, language)
    }
}

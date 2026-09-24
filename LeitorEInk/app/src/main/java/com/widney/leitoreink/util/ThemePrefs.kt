/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Tema de leitura escolhido pelo usuário: sempre um dos dois (nunca "segue o sistema"),
 * porque o visual e-ink É a proposta do app — assim como um Kindle não muda de tema
 * sozinho, aqui quem decide é o usuário, num botão dentro do próprio app.
 */
enum class AppTheme { LIGHT, DARK }

object ThemePrefs {
    private const val PREFS_NAME = "leitor_eink_prefs"
    private const val KEY_THEME = "theme_mode"
    private const val VALUE_DARK = "dark"
    private const val VALUE_LIGHT = "light"

    fun current(context: Context): AppTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.getString(KEY_THEME, VALUE_LIGHT) == VALUE_DARK) {
            AppTheme.DARK
        } else {
            AppTheme.LIGHT
        }
    }

    fun isDark(context: Context): Boolean = current(context) == AppTheme.DARK

    /** Aplica o tema salvo ao modo dia/noite do AppCompat (independente do tema do sistema). */
    fun apply(context: Context) {
        val mode = if (isDark(context)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun setTheme(context: Context, theme: AppTheme) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, if (theme == AppTheme.DARK) VALUE_DARK else VALUE_LIGHT).apply()
        apply(context)
    }
}

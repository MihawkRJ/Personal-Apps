/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.util

import android.content.Context

/**
 * Guarda as preferências de leitura (tamanho de letra e modo de exibição do PDF)
 * para que a escolha do usuário valha para todos os livros e continue valendo
 * depois de fechar o app — importante para quem depende de letra grande.
 */
object ReaderPrefs {
    private const val PREFS_NAME = "leitor_eink_prefs"
    private const val KEY_PDF_TEXT_MODE = "pdf_text_mode"
    private const val KEY_PDF_TEXT_FONT = "pdf_text_font_sp"
    private const val KEY_TXT_FONT = "txt_font_sp"
    private const val KEY_EPUB_FONT = "epub_font_percent"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPdfTextMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PDF_TEXT_MODE, false)

    fun setPdfTextMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PDF_TEXT_MODE, enabled).apply()
    }

    fun pdfTextFontSp(context: Context): Float =
        prefs(context).getFloat(KEY_PDF_TEXT_FONT, 18f)

    fun setPdfTextFontSp(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_PDF_TEXT_FONT, value).apply()
    }

    fun txtFontSp(context: Context): Float =
        prefs(context).getFloat(KEY_TXT_FONT, 17f)

    fun setTxtFontSp(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_TXT_FONT, value).apply()
    }

    fun epubFontPercent(context: Context): Int =
        prefs(context).getInt(KEY_EPUB_FONT, 100)

    fun setEpubFontPercent(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_EPUB_FONT, value).apply()
    }
}

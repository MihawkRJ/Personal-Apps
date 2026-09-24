/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.widney.leitoreink.data.AppDatabase
import com.widney.leitoreink.util.ThemePrefs

class EInkReaderApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        // Aplica o tema (claro/escuro) escolhido pelo usuário antes de qualquer
        // Activity ser criada.
        ThemePrefs.apply(this)

        // Necessário para a extração de texto de PDFs (modo "Texto" do leitor).
        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (e: Exception) {
            // Sem extração de texto o app continua funcionando no modo página.
        }
    }
}

/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.reader

/**
 * JavaScript injetado em cada capítulo do EPUB (via WebView.evaluateJavascript)
 * depois que a página termina de carregar, para forçar o visual "papel e-ink"
 * mesmo em EPUBs que vêm com seu próprio CSS colorido.
 */
object EpubEInkScript {

    fun build(fontSizePercent: Int, isDark: Boolean = false): String {
        val background = if (isDark) "#121110" else "#F7F6F1"
        val text = if (isDark) "#E5E0D3" else "#1A1A18"
        val border = if (isDark) "#3A382F" else "#D8D5CC"
        val imageFilter = if (isDark) {
            "filter: grayscale(100%) contrast(1.05) invert(1);"
        } else {
            "filter: grayscale(100%) contrast(1.05);"
        }

        return """
            (function() {
                var style = document.getElementById('eink-override-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'eink-override-style';
                    document.head.appendChild(style);
                }
                style.innerHTML =
                    'html, body { background-color: $background !important; color: $text !important; ' +
                    'font-family: serif !important; line-height: 1.6 !important; ' +
                    'font-size: $fontSizePercent% !important; } ' +
                    '* { background-color: transparent !important; color: $text !important; ' +
                    'border-color: $border !important; } ' +
                    'body { padding: 16px !important; box-sizing: border-box !important; } ' +
                    'img, svg { max-width: 100% !important; height: auto !important; $imageFilter }';
            })();
        """.trimIndent()
    }
}

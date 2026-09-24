/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.util

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

object EInkColorMatrix {

    /**
     * Filtro que converte qualquer imagem para tons de cinza com contraste
     * levemente realçado, imitando a aparência de uma tela de tinta eletrônica.
     * Aplicado como colorFilter de um ImageView (não altera os pixels originais).
     *
     * Quando [invert] é true (tema escuro), inverte as cores depois de aplicar o
     * cinza/contraste, transformando o fundo branco típico de um PDF num fundo
     * escuro com texto claro — para páginas de PDF acompanharem o tema noturno.
     */
    fun createFilter(contrast: Float = 1.12f, invert: Boolean = false): ColorMatrixColorFilter {
        val matrix = ColorMatrix().apply { setSaturation(0f) }

        val translate = (-0.5f * contrast + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(contrastMatrix)

        if (invert) {
            val invertMatrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            matrix.postConcat(invertMatrix)
        }

        return ColorMatrixColorFilter(matrix)
    }
}

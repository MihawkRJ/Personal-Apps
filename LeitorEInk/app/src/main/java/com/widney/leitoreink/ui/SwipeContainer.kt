/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * FrameLayout que detecta o gesto de arrastar o dedo na horizontal por cima do seu
 * conteúdo — usado na tela de leitura para virar a página, como num livro.
 *
 * Em vez de "roubar" o toque do filho (o que quebrava a rolagem da WebView/ScrollView
 * e fazia o gesto nunca ser reconhecido), aqui apenas OBSERVAMOS os eventos em
 * dispatchTouchEvent: eles continuam chegando normalmente na view de baixo, e no
 * momento em que o dedo é levantado verificamos se o movimento foi um arrasto
 * horizontal grande o bastante para virar a página.
 */
class SwipeContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    /** Chamado quando o usuário arrasta o dedo para a esquerda (avançar página). */
    var onSwipeLeft: (() -> Unit)? = null

    /** Chamado quando o usuário arrasta o dedo para a direita (voltar página). */
    var onSwipeRight: (() -> Unit)? = null

    /** Permite desligar o swipe em certas situações (ex: PDF com zoom aplicado). */
    var swipeEnabled: () -> Boolean = { true }

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var gestureConsumed = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                downTime = ev.eventTime
                gestureConsumed = false
            }
            // Dois dedos na tela (pinça/zoom) não contam como virar página.
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> {
                gestureConsumed = true
            }
            MotionEvent.ACTION_UP -> {
                if (!gestureConsumed && swipeEnabled()) {
                    val dx = ev.x - downX
                    val dy = ev.y - downY
                    val elapsed = ev.eventTime - downTime
                    val minDistance = (width * 0.18f).coerceAtLeast(MIN_DISTANCE_PX)

                    val isHorizontal = abs(dx) > abs(dy) * 1.3f
                    if (isHorizontal && abs(dx) >= minDistance && elapsed <= MAX_DURATION_MS) {
                        gestureConsumed = true
                        if (dx < 0) onSwipeLeft?.invoke() else onSwipeRight?.invoke()
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    companion object {
        private const val MIN_DISTANCE_PX = 90f
        private const val MAX_DURATION_MS = 1500L
    }
}

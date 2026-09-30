package com.esp8266.reverseproxy

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/** Efectos visuales reutilizables. Respetan "Escala de duracion de animacion = 0" del sistema. */
object Fx {
    fun enabled(ctx: Context): Boolean =
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
}

/** Entrada: sube desde abajo con fundido, escalonada por indice. */
fun View.enter(index: Int) {
    if (!Fx.enabled(context)) return
    alpha = 0f
    translationY = 90f
    animate().alpha(1f).translationY(0f)
        .setStartDelay(160L * index).setDuration(600)
        .setInterpolator(DecelerateInterpolator(2f)).start()
}

/** Sacudida horizontal (errores). */
fun View.shake() {
    if (!Fx.enabled(context)) return
    ObjectAnimator.ofFloat(this, View.TRANSLATION_X, 0f, -16f, 16f, -11f, 11f, -6f, 6f, 0f)
        .setDuration(420).start()
}

/** Parpadeo tipo monitor CRT (cambio de estado). */
fun View.flicker() {
    if (!Fx.enabled(context)) return
    ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0.15f, 1f, 0.4f, 1f, 0.7f, 1f)
        .setDuration(320).start()
}

/** El borde/fondo del panel "respira" como neon. Devuelve el animador para cancelarlo luego. */
fun View.pulseBorder(delayMs: Long): Animator? {
    if (!Fx.enabled(context)) return null
    val d = background?.mutate() ?: return null
    return ValueAnimator.ofInt(150, 255).apply {
        duration = 1700
        startDelay = delayMs
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { d.alpha = it.animatedValue as Int }
        start()
    }
}

/** Pulso de opacidad infinito (p. ej. la linea neon del encabezado). */
fun View.pulseAlpha(): Animator? {
    if (!Fx.enabled(context)) return null
    return ObjectAnimator.ofFloat(this, View.ALPHA, 0.45f, 1f).apply {
        duration = 1100
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        start()
    }
}

/** Boton: se hunde al tocar, rebota al soltar y vibra levemente. */
@SuppressLint("ClickableViewAccessibility")
fun View.neonPress() {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(280)
                    .setInterpolator(OvershootInterpolator(3.5f)).start()
            }
        }
        false
    }
}

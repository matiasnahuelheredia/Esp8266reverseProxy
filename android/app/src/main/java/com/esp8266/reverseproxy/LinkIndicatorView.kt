package com.esp8266.reverseproxy

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min
import kotlin.math.sin

/** Indicador de enlace: ondas de radar verdes si hay conexion, parpadeo magenta si no. */
class LinkIndicatorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var connected = false
    private var p = 0f

    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            p = it.animatedValue as Float
            invalidate()
        }
    }

    fun setConnected(c: Boolean) {
        connected = c
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Fx.enabled(context)) anim.start()
    }

    override fun onDetachedFromWindow() {
        anim.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val maxR = min(width, height) / 2f - density
        val core = maxR * 0.32f
        val color = if (connected) Color.rgb(57, 255, 20) else Color.rgb(255, 43, 214)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * density
        paint.color = color
        val rings = if (connected) 2 else 1
        for (i in 0 until rings) {
            val q = (p + i * 0.5f) % 1f
            paint.alpha = ((1f - q) * 210).toInt()
            canvas.drawCircle(cx, cy, core + (maxR - core) * q, paint)
        }

        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = if (connected) 255 else (110 + 145 * (0.5f + 0.5f * sin(p * 6.2831855f))).toInt()
        canvas.drawCircle(cx, cy, core, paint)
    }
}

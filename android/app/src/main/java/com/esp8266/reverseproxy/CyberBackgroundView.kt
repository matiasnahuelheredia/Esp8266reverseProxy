package com.esp8266.reverseproxy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import java.util.Random

/**
 * Fondo animado: lluvia de codigo, grilla synthwave en perspectiva que avanza,
 * resplandor de horizonte, scanlines CRT y una barra de barrido.
 */
class CyberBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val animated = Fx.enabled(context)
    private val t0 = System.nanoTime()

    private val bgPaint = Paint()
    private val glowPaint = Paint()
    private val sweepPaint = Paint()
    private val scanPaint = Paint().apply { color = Color.argb(28, 0, 0, 0); strokeWidth = density }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = Color.rgb(255, 43, 214)
    }
    private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textSize = 13f * density
        color = Color.rgb(57, 255, 20)
    }

    private val glyphs = "01ABCDEF<>/\\{}#\$%&*=+".toCharArray()
    private val one = CharArray(1)
    private val trail = 9
    private var colCount = 0
    private var colX = FloatArray(0)
    private var speed = FloatArray(0)
    private var phase = FloatArray(0)
    private var scanLines = FloatArray(0)
    private var horizon = 0f
    private val glowH = 70f * density
    private val bandH = 120f * density

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val wf = w.toFloat()
        val hf = h.toFloat()
        horizon = hf * 0.62f
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, hf,
            intArrayOf(Color.rgb(10, 10, 18), Color.rgb(24, 8, 44), Color.rgb(10, 10, 18)),
            floatArrayOf(0f, 0.72f, 1f), Shader.TileMode.CLAMP
        )
        glowPaint.shader = LinearGradient(
            0f, horizon - glowH, 0f, horizon,
            Color.argb(0, 0, 240, 255), Color.argb(120, 0, 240, 255), Shader.TileMode.CLAMP
        )
        sweepPaint.shader = LinearGradient(
            0f, 0f, 0f, bandH,
            Color.argb(0, 0, 240, 255), Color.argb(46, 0, 240, 255), Shader.TileMode.CLAMP
        )

        val step = 26f * density
        colCount = (wf / step).toInt() + 1
        val rnd = Random(7)
        colX = FloatArray(colCount) { it * step + 4f * density }
        speed = FloatArray(colCount) { (45f + rnd.nextFloat() * 110f) * density }
        phase = FloatArray(colCount) { rnd.nextFloat() * (hf + trail * rainPaint.textSize) }

        val n = (hf / (3f * density)).toInt()
        scanLines = FloatArray(n * 4)
        for (i in 0 until n) {
            val y = i * 3f * density
            scanLines[i * 4] = 0f
            scanLines[i * 4 + 1] = y
            scanLines[i * 4 + 2] = wf
            scanLines[i * 4 + 3] = y
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val t = (System.nanoTime() - t0) / 1_000_000_000f
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Lluvia de codigo
        val cell = rainPaint.textSize
        val total = h + trail * cell
        val tick = (t * 6f).toInt()
        for (c in 0 until colCount) {
            val headY = (t * speed[c] + phase[c]) % total
            for (r in 0 until trail) {
                val y = headY - r * cell
                if (y < 0f || y > h) continue
                rainPaint.alpha = (80 * (1f - r / trail.toFloat())).toInt()
                one[0] = glyphs[(tick + c * 7 + r * 13) % glyphs.size]
                canvas.drawText(one, 0, 1, colX[c], y, rainPaint)
            }
        }

        // Horizonte + grilla en perspectiva
        canvas.drawRect(0f, horizon - glowH, w, horizon, glowPaint)
        val cx = w / 2f
        gridPaint.alpha = 120
        for (k in -8..8) {
            canvas.drawLine(cx + k * (w / 70f), horizon, cx + k * (w / 5f), h, gridPaint)
        }
        val n = 10
        val shift = (t * 0.5f) % 1f
        for (j in 0 until n) {
            val z = (j + shift) / n
            val y = horizon + (h - horizon) * z * z
            gridPaint.alpha = (40 + 190 * z).toInt().coerceAtMost(255)
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // Scanlines CRT y barra de barrido
        canvas.drawLines(scanLines, scanPaint)
        val sy = ((t / 5f) % 1f) * (h + bandH) - bandH
        canvas.save()
        canvas.translate(0f, sy)
        canvas.drawRect(0f, 0f, w, bandH, sweepPaint)
        canvas.restore()

        if (animated) postInvalidateOnAnimation()
    }
}

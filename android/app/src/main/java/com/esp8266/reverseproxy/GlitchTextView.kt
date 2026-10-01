package com.esp8266.reverseproxy

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import java.util.Random

/** TextView que se "glitchea" (tiembla, cambia de sombra cian/magenta y parpadea) al azar. */
class GlitchTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyle) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val rnd = Random()
    private val baseR = shadowRadius
    private val baseDx = shadowDx
    private val baseDy = shadowDy
    private val baseColor = shadowColor
    private val fxEnabled = Fx.enabled(context)

    private val loop = object : Runnable {
        override fun run() {
            burst()
            mainHandler.postDelayed(this, 2500L + rnd.nextInt(3500))
        }
    }

    /** Rafaga de glitch de [frames] cuadros. */
    fun burst(frames: Int = 9) {
        if (!fxEnabled) return
        var i = 0
        val step = object : Runnable {
            override fun run() {
                if (i < frames) {
                    translationX = (rnd.nextFloat() - 0.5f) * 18f
                    translationY = (rnd.nextFloat() - 0.5f) * 5f
                    alpha = 0.55f + rnd.nextFloat() * 0.45f
                    setShadowLayer(
                        1f, (rnd.nextFloat() - 0.5f) * 18f, (rnd.nextFloat() - 0.5f) * 7f,
                        if (rnd.nextBoolean()) Color.rgb(255, 43, 214) else Color.rgb(0, 240, 255)
                    )
                    i++
                    mainHandler.postDelayed(this, 35)
                } else {
                    translationX = 0f
                    translationY = 0f
                    alpha = 1f
                    setShadowLayer(baseR, baseDx, baseDy, baseColor)
                }
            }
        }
        mainHandler.post(step)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (fxEnabled) mainHandler.postDelayed(loop, 1200L)
    }

    override fun onDetachedFromWindow() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }
}
